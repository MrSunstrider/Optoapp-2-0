package com.example.optoapp.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.DispensacionRepository
import com.example.optoapp.data.Montura
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.PacienteRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.RoomTransactionRunner
import com.example.optoapp.data.SyncRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.backup.BackupRestoreCoordinator
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.data.sync.SyncSnapshotCoordinator
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.DispensacionStockHelper
import dagger.Lazy
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscardLosingClaimUseCaseTest {
    private val opticaId = "o1"
    private val origId = "d1"
    private lateinit var db: OptoDatabase
    private lateinit var repository: OptoRepository
    private lateinit var tracker: SyncStateTracker
    private lateinit var stockHelper: DispensacionStockHelper
    private val today = DateUtils.today()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val schedulerLazy = mockk<Lazy<PostSaveSyncScheduler>>()
        every { schedulerLazy.get() } returns mockk(relaxed = true)
        tracker = SyncStateTracker(db.syncEntityStateDao(), db)
        repository = OptoRepository(
            database = db,
            syncStateTracker = tracker,
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
        stockHelper = DispensacionStockHelper(repository.monturaCoordinator, RoomTransactionRunner(db))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun discarder(repo: OptoRepository = repository) = DiscardLosingClaimUseCase(
        repo, db.pagoDao(), db.monturaMovimientoDao(), repository.monturaCoordinator, stockHelper, tracker,
    )

    private suspend fun seedClaimedOriginal(frameStock: Int = 3): String {
        db.pacienteDao().insertPaciente(
            Paciente(id = "pac", nombreCompleto = "Paciente", edad = 30, telefono = "1", fechaCreacion = today, opticaId = opticaId),
        )
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = origId, ot = "2026-0042", pacienteId = "pac", fecha = today.minusDays(40), opticaId = opticaId,
                montoTotal = 200.0, estadoEntrega = "Entregado", fechaEntrega = today.minusDays(30),
            ),
        )
        db.monturaDao().insertMontura(Montura(id = "M1", sku = "sku-M1", stockActual = frameStock, opticaId = opticaId))
        db.dispensacionItemDao().insertItem(
            DispensacionItem(id = "i1", dispensacionId = origId, monturaId = "M1", origenMontura = "Tienda", opticaId = opticaId),
        )
        listOf(Triple("a1", 120.0, "Efectivo"), Triple("a2", 80.0, "Tarjeta")).forEach { (id, monto, metodo) ->
            db.pagoDao().insertPago(
                Pago(id = id, dispensacionId = origId, fecha = today.minusDays(35), tipo = "Abono", monto = monto, metodoPago = metodo, opticaId = opticaId),
            )
            tracker.markSynced(opticaId, "pago", id)
        }
        val created = ReclamarDispensacionUseCase(
            repository, db.pagoDao(), stockHelper, mockk(relaxed = true), CalcularMontoPagadoUseCase(db.pagoDao()),
        )(origId, opticaId, "Lente rayado", 150.0, "Tarjeta") as ReclamoOutcome.Created
        tracker.markError(opticaId, "dispensacion", created.replacementId, "quarantine:reclamo_duplicate:$origId")
        return created.replacementId
    }

    private suspend fun pagosOf(parentId: String) = db.pagoDao().getPagosByParent(parentId, opticaId)
    private suspend fun stock() = db.monturaDao().getMonturaByIdForOptica("M1", opticaId)!!.stockActual
    private suspend fun movimientos() = db.monturaMovimientoDao().getMovimientosListByOptica(opticaId)
    private suspend fun states() = db.syncEntityStateDao().getByStatus(opticaId, "error") +
        db.syncEntityStateDao().getByStatus(opticaId, "awaiting_remote")

    @Test
    fun discard_removesTheLosingClaimAndRestoresTheLedgerAndStock() = runTest {
        val replId = seedClaimedOriginal()
        assertEquals(2, stock())

        val discarded = discarder()(opticaId)

        assertEquals(1, discarded)
        assertNull(db.dispensacionDao().getDispensacionById(replId, opticaId))
        assertTrue(pagosOf(replId).isEmpty())
        assertTrue(db.dispensacionItemDao().getItemsListByDispensacion(replId, opticaId).isEmpty())
        assertEquals(setOf("a1", "a2"), pagosOf(origId).map { it.id }.toSet())
        assertEquals(200.0, db.pagoDao().sumMontoByDispensacion(origId, opticaId), 0.001)
        assertEquals(3, stock())
        assertTrue(movimientos().isEmpty())
        assertTrue(tracker.quarantineReasons(opticaId, "dispensacion").isEmpty())
    }

    @Test
    fun discard_holdsTheOriginalForTheWinnerAndTellsTheUserOnce() = runTest {
        val replId = seedClaimedOriginal()

        discarder()(opticaId)

        val awaiting = db.syncEntityStateDao().getByStatus(opticaId, "awaiting_remote").single()
        assertEquals("dispensacion" to origId, awaiting.entityType to awaiting.entityId)
        val notice = db.syncEntityStateDao().getByStatus(opticaId, "error").single()
        assertEquals("reclamo_descartado" to replId, notice.entityType to notice.entityId)
        assertEquals(
            "Otro dispositivo ya registró el reclamo de la OT 2026-0042; se descartó el reclamo local.",
            notice.lastError,
        )
        assertFalse(notice.lastError.startsWith("quarantine:"))
    }

    @Test
    fun discard_compensatesAnAlreadyUploadedSaleWithAnIdempotentRestock() = runTest {
        val replId = seedClaimedOriginal()
        val sale = movimientos().single()
        tracker.markSynced(opticaId, "montura_movimiento", sale.id)

        discarder()(opticaId)

        assertEquals(3, stock())
        val restock = movimientos().single { it.id != sale.id }
        assertEquals("AJUSTE" to "$replId:descarte:${sale.id}", restock.tipo to restock.referenciaId)
        assertEquals(1, restock.cantidad)
        assertNotNull(movimientos().singleOrNull { it.id == sale.id })
    }

    @Test
    fun discard_removesOnlyTheClaimReversosAndClearsTheirQuarantine() = runTest {
        seedClaimedOriginal()
        val reversos = pagosOf(origId).filter { it.tipo == "Reverso" }
        assertEquals(2, reversos.size)
        tracker.markError(opticaId, "pago", reversos.first().id, "quarantine:reverso_duplicate:a1")

        discarder()(opticaId)

        assertEquals(setOf("a1", "a2"), pagosOf(origId).map { it.id }.toSet())
        assertTrue(states().none { it.entityType == "pago" })
    }

    @Test
    fun failureMidDiscard_rollsBackEverything() = runTest {
        val replId = seedClaimedOriginal()
        val failing = spyk(repository)
        coEvery { failing.deleteDispensacionById(replId, opticaId) } throws IllegalStateException("delete failed")

        val error = runCatching { discarder(failing)(opticaId) }.exceptionOrNull()

        assertEquals("delete failed", error?.message)
        assertNotNull(db.dispensacionDao().getDispensacionById(replId, opticaId))
        assertEquals(2, pagosOf(origId).count { it.tipo == "Reverso" })
        assertEquals(2, stock())
        assertEquals(1, movimientos().size)
        assertEquals(
            mapOf(replId to "quarantine:reclamo_duplicate:$origId"),
            tracker.quarantineReasons(opticaId, "dispensacion"),
        )
    }

    @Test
    fun withoutALosingClaim_discardIsANoOp() = runTest {
        db.pacienteDao().insertPaciente(
            Paciente(id = "pac", nombreCompleto = "Paciente", edad = 30, telefono = "1", fechaCreacion = today, opticaId = opticaId),
        )
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(id = origId, ot = "2026-0042", pacienteId = "pac", fecha = today, opticaId = opticaId),
        )
        tracker.markError(opticaId, "dispensacion", origId, "quarantine:constraint:23514")

        assertEquals(0, discarder()(opticaId))
        assertNotNull(db.dispensacionDao().getDispensacionById(origId, opticaId))
    }
}
