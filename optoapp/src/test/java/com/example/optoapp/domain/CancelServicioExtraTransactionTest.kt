package com.example.optoapp.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.DispensacionRepository
import com.example.optoapp.data.Montura
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.PacienteRepository
import com.example.optoapp.data.RoomTransactionRunner
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SyncRepository
import com.example.optoapp.data.backup.BackupRestoreCoordinator
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.data.servicio.ServicioExtraItem
import com.example.optoapp.data.sync.SyncSnapshotCoordinator
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.DispensacionStockHelper
import dagger.Lazy
import io.github.jan.supabase.SupabaseClient
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CancelServicioExtraTransactionTest {
    private val opticaId = "o1"
    private val servicioId = "s1"
    private lateinit var db: OptoDatabase
    private lateinit var repository: OptoRepository
    private lateinit var scheduler: PostSaveSyncScheduler

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scheduler = mockk(relaxed = true)
        val schedulerLazy = Lazy { scheduler }
        repository = OptoRepository(
            database = db,
            syncStateTracker = mockk(relaxed = true),
            postSaveSyncScheduler = schedulerLazy,
            pacienteRepo = PacienteRepository(db.pacienteDao(), db.evaluacionDao()),
            dispensacionRepo = DispensacionRepository(db.dispensacionDao(), db.dispensacionItemDao(), db.pagoDao(), db.servicioExtraDao()),
            syncRepo = SyncRepository(mockk(relaxed = true), db.monturaDao(), db.monturaMovimientoDao()),
            snapshotCoordinator = mockk<SyncSnapshotCoordinator>(relaxed = true),
            backupCoordinator = mockk<BackupRestoreCoordinator>(relaxed = true),
            monturaCoordinator = MonturaInventoryCoordinator(db.monturaDao(), db.monturaMovimientoDao(), schedulerLazy),
            gastoOperativoDao = db.gastoOperativoDao(),
            supabase = mockk<SupabaseClient>(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun useCase() = CancelServicioExtraUseCase(
        repository,
        db.pagoDao(),
        scheduler,
        DispensacionStockHelper(repository.monturaCoordinator, RoomTransactionRunner(db)),
    )

    private suspend fun stock(id: String) = db.monturaDao().getMonturaByIdForOptica(id, opticaId)!!.stockActual
    private suspend fun pagos() = db.pagoDao().getPagosByParent(servicioId, opticaId)

    @Test
    fun overlappingInvocations_applyExactlyOnce() = runTest {
        db.monturaDao().insertMontura(Montura(id = "M1", sku = "sku-M1", stockActual = 3, opticaId = opticaId))
        db.servicioExtraDao().insertServicio(
            ServicioExtra(id = servicioId, descripcion = "Montaje", montoTotal = 80.0, estado = "Pendiente", fecha = DateUtils.today(), opticaId = opticaId),
        )
        db.servicioExtraItemDao().insert(ServicioExtraItem(id = "it1", servicioExtraId = servicioId, monturaId = "M1", opticaId = opticaId))
        db.pagoDao().insertPago(
            Pago(id = "a1", servicioExtraId = servicioId, fecha = DateUtils.today(), tipo = "Abono", monto = 80.0, metodoPago = "Efectivo", opticaId = opticaId),
        )

        val outcomes = List(2) { async(Dispatchers.IO) { useCase()(servicioId, opticaId, "Doble toque") } }.awaitAll()

        assertEquals(setOf(LifecycleOutcome.Applied, LifecycleOutcome.AlreadyTerminal("Anulado")), outcomes.toSet())
        assertEquals(1, pagos().count { it.tipo == "Reverso" })
        assertEquals(4, stock("M1"))
        assertEquals(1, db.monturaMovimientoDao().getMovimientosListByOptica(opticaId).size)
        assertEquals("Anulado", db.servicioExtraDao().getServicioById(servicioId, opticaId)!!.estado)
    }
}
