package com.example.optoapp.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.DispensacionRepository
import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.PacienteRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.RoomTransactionRunner
import com.example.optoapp.data.SyncRepository
import com.example.optoapp.data.backup.BackupRestoreCoordinator
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import com.example.optoapp.data.sync.SyncSnapshotCoordinator
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.DispensacionStockHelper
import dagger.Lazy
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnularDispensacionTransactionTest {
    private val opticaId = "o1"
    private val dispId = "d1"
    private lateinit var db: OptoDatabase
    private lateinit var repository: OptoRepository
    private lateinit var scheduler: PostSaveSyncScheduler
    private val today = DateUtils.today()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scheduler = mockk(relaxed = true)
        val schedulerLazy = mockk<Lazy<PostSaveSyncScheduler>>()
        every { schedulerLazy.get() } returns scheduler
        val pacienteRepo = PacienteRepository(db.pacienteDao(), db.evaluacionDao())
        val dispensacionRepo = DispensacionRepository(db.dispensacionDao(), db.dispensacionItemDao(), db.pagoDao(), db.servicioExtraDao())
        val syncRepo = SyncRepository(mockk(relaxed = true), db.monturaDao(), db.monturaMovimientoDao())
        repository = OptoRepository(
            database = db,
            syncStateTracker = mockk(relaxed = true),
            postSaveSyncScheduler = schedulerLazy,
            pacienteRepo = pacienteRepo,
            dispensacionRepo = dispensacionRepo,
            syncRepo = syncRepo,
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

    private fun useCase(repo: OptoRepository = repository) = AnularDispensacionUseCase(
        repo,
        db.pagoDao(),
        DispensacionStockHelper(repo.monturaCoordinator, RoomTransactionRunner(db)),
        scheduler,
        CalcularMontoPagadoUseCase(db.pagoDao()),
    )

    private suspend fun seedOrder(
        estado: String = "Pendiente",
        headerMonturaId: String = "",
        headerOrigen: String = "",
    ) {
        db.pacienteDao().insertPaciente(
            Paciente(id = "pac", nombreCompleto = "Paciente", edad = 30, telefono = "1", fechaCreacion = today, opticaId = opticaId),
        )
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = dispId, ot = "2026-0042", pacienteId = "pac", fecha = today.minusDays(5), opticaId = opticaId,
                montoTotal = 200.0, estadoEntrega = estado, monturaId = headerMonturaId, origenMontura = headerOrigen,
            ),
        )
    }

    private suspend fun seedMontura(id: String, stock: Int) =
        db.monturaDao().insertMontura(Montura(id = id, sku = "sku-$id", stockActual = stock, opticaId = opticaId))

    private suspend fun seedItem(id: String, monturaId: String, origen: String = "Tienda") =
        db.dispensacionItemDao().insertItem(
            DispensacionItem(id = id, dispensacionId = dispId, monturaId = monturaId, origenMontura = origen, opticaId = opticaId),
        )

    private suspend fun seedRegalo(id: String, productoId: String, cantidad: Int = 1) = db.regaloDispensacionDao().insert(
        RegaloDispensacionEntity(
            id = id, dispensacionId = dispId, productoId = productoId, cantidad = cantidad,
            costoUnitario = 5.0, descripcion = "Estuche", motivo = "", opticaId = opticaId,
        ),
    )

    private suspend fun seedPago(id: String, tipo: String, monto: Double, metodo: String, daysAgo: Long = 3, reversaPagoId: String? = null) =
        db.pagoDao().insertPago(
            Pago(
                id = id, dispensacionId = dispId, fecha = today.minusDays(daysAgo), tipo = tipo, monto = monto,
                metodoPago = metodo, opticaId = opticaId, reversaPagoId = reversaPagoId,
            ),
        )

    private suspend fun pagos() = db.pagoDao().getPagosByParent(dispId, opticaId)
    private suspend fun reversos() = pagos().filter { it.tipo == "Reverso" }
    private suspend fun stock(id: String) = db.monturaDao().getMonturaByIdForOptica(id, opticaId)!!.stockActual
    private suspend fun movimientos(): List<MonturaMovimiento> = db.monturaMovimientoDao().getMovimientosListByOptica(opticaId)
    private suspend fun order() = db.dispensacionDao().getDispensacionById(dispId, opticaId)!!
    private suspend fun netPaid() = db.pagoDao().sumMontoByDispensacion(dispId, opticaId)

    @Test
    fun cancelPendiente_reversesPagosRestocksFrameAndRegaloAndMarksAnulado() = runTest {
        seedOrder()
        seedMontura("M1", stock = 3)
        seedMontura("P1", stock = 5)
        seedItem("i1", "M1")
        seedRegalo("r1", "P1")
        seedPago("a1", "Abono", 100.0, "Efectivo", daysAgo = 3)
        seedPago("a2", "Abono", 50.0, "Yape", daysAgo = 1)

        val outcome = useCase()(dispId, opticaId, "  Cliente desistió ")

        assertEquals(LifecycleOutcome.Applied, outcome)
        val reversos = reversos().associateBy { it.reversaPagoId }
        assertEquals(setOf("a1", "a2"), reversos.keys)
        assertEquals(100.0, reversos.getValue("a1").monto, 0.001)
        assertEquals("Efectivo", reversos.getValue("a1").metodoPago)
        assertEquals(50.0, reversos.getValue("a2").monto, 0.001)
        assertEquals("Yape", reversos.getValue("a2").metodoPago)
        assertTrue(reversos.values.all { it.fecha == today })
        assertEquals(4, stock("M1"))
        assertEquals(6, stock("P1"))
        assertEquals(setOf("d1:anul:i1", "r1:anul"), movimientos().map { it.referenciaId }.toSet())
        val cancelled = order()
        assertEquals("Anulado", cancelled.estadoEntrega)
        assertEquals("Cliente desistió", cancelled.motivoAnulacion)
        assertEquals(today, cancelled.fechaAnulacion)
        assertEquals(0.0, cancelled.montoPagado, 0.001)
        assertEquals(0.0, netPaid(), 0.001)
        coVerify { scheduler.scheduleFinanzasSync(opticaId) }
        coVerify { scheduler.scheduleInventarioSync(opticaId) }
    }

    @Test
    fun cancelEntregado_isAReturnAndKeepsEarlierSaleAndEditMovimientos() = runTest {
        seedOrder(estado = "Entregado")
        seedMontura("M1", stock = 2)
        seedItem("i1", "M1")
        seedPago("a1", "Abono", 80.0, "Tarjeta", daysAgo = 60)
        val sale = MonturaMovimiento(id = "mv-sale", monturaId = "M1", tipo = "SALIDA_VENTA", cantidad = 1, stockPrevio = 3, stockNuevo = 2, referenciaId = dispId, opticaId = opticaId)
        val edit = MonturaMovimiento(id = "mv-edit", monturaId = "M1", tipo = "AJUSTE", cantidad = 1, stockPrevio = 2, stockNuevo = 3, referenciaId = dispId, opticaId = opticaId)
        db.monturaMovimientoDao().insertMovimiento(sale)
        db.monturaMovimientoDao().insertMovimiento(edit)

        val outcome = useCase()(dispId, opticaId, "Devolución")

        assertEquals(LifecycleOutcome.Applied, outcome)
        assertEquals(listOf("a1"), reversos().map { it.reversaPagoId })
        assertEquals(3, stock("M1"))
        assertEquals("Anulado", order().estadoEntrega)
        val byId = movimientos().associateBy { it.id }
        assertEquals(sale, byId["mv-sale"])
        assertEquals(edit, byId["mv-edit"])
        assertEquals(listOf("d1:anul:i1"), byId.values.filter { it.id !in setOf("mv-sale", "mv-edit") }.map { it.referenciaId })
    }

    @Test
    fun nonStoreFrameWithoutPagos_cancelsWithoutStockMovimientoOrPagoRows() = runTest {
        seedOrder()
        seedMontura("M1", stock = 3)
        seedItem("i1", "M1", origen = "Paciente")

        val outcome = useCase()(dispId, opticaId, "Error de pedido")

        assertEquals(LifecycleOutcome.Applied, outcome)
        assertEquals(3, stock("M1"))
        assertEquals(0, movimientos().size)
        assertEquals(0, pagos().size)
        assertEquals("Anulado", order().estadoEntrega)
    }

    @Test
    fun alreadyReversedPago_getsNoSecondReversoAndOnlyUnreversedOneIsReversed() = runTest {
        seedOrder()
        seedPago("a1", "Abono", 100.0, "Efectivo", daysAgo = 10)
        seedPago("rv-a1", "Reverso", 100.0, "Efectivo", daysAgo = 9, reversaPagoId = "a1")
        seedPago("a2", "Abono", 50.0, "Yape", daysAgo = 2)

        val outcome = useCase()(dispId, opticaId, "Cliente desistió")

        assertEquals(LifecycleOutcome.Applied, outcome)
        val newReversos = reversos().filter { it.id != "rv-a1" }
        assertEquals(listOf("a2"), newReversos.map { it.reversaPagoId })
        assertEquals(50.0, newReversos.single().monto, 0.001)
        assertEquals(today, newReversos.single().fecha)
        assertEquals(listOf("rv-a1"), reversos().filter { it.reversaPagoId == "a1" }.map { it.id })
        assertEquals(0.0, netPaid(), 0.001)
        assertEquals("Anulado", order().estadoEntrega)
    }

    @Test
    fun noPagosWithStoreFrame_restocksFrameWithoutPagoRows() = runTest {
        seedOrder()
        seedMontura("M1", stock = 2)
        seedItem("i1", "M1")

        val outcome = useCase()(dispId, opticaId, "Cliente desistió")

        assertEquals(LifecycleOutcome.Applied, outcome)
        assertEquals(0, pagos().size)
        assertEquals(3, stock("M1"))
        assertEquals(listOf("d1:anul:i1"), movimientos().map { it.referenciaId })
        assertEquals("Anulado", order().estadoEntrega)
    }

    @Test
    fun legacyHeaderStoreFrame_restocksWithHeaderReferencia() = runTest {
        seedOrder(headerMonturaId = "M1", headerOrigen = "Tienda")
        seedMontura("M1", stock = 0)

        useCase()(dispId, opticaId, "Cliente desistió")

        assertEquals(1, stock("M1"))
        assertEquals(listOf("d1:anul:h:M1"), movimientos().map { it.referenciaId })
        assertEquals(0, pagos().size)
    }

    @Test
    fun legacyNuevaDeTiendaHeaderFrame_restocksOnceAcrossRepeatedCancels() = runTest {
        seedOrder(headerMonturaId = "M1", headerOrigen = "Nueva de Tienda")
        seedMontura("M1", stock = 0)

        useCase()(dispId, opticaId, "Cliente desistió")
        val second = useCase()(dispId, opticaId, "Cliente desistió")

        assertEquals(LifecycleOutcome.AlreadyTerminal("Anulado"), second)
        assertEquals(1, stock("M1"))
        assertEquals(listOf("d1:anul:h:M1"), movimientos().map { it.referenciaId })
    }

    @Test
    fun legacyNuevaDeTiendaItemFrame_isRestocked() = runTest {
        seedOrder()
        seedMontura("M1", stock = 2)
        seedItem("i1", "M1", origen = "Nueva de Tienda")

        useCase()(dispId, opticaId, "Cliente desistió")

        assertEquals(3, stock("M1"))
        assertEquals(listOf("d1:anul:i1"), movimientos().map { it.referenciaId })
    }

    @Test
    fun secondCancel_returnsAlreadyTerminalAndChangesNothing() = runTest {
        seedOrder()
        seedMontura("M1", stock = 3)
        seedMontura("P1", stock = 5)
        seedItem("i1", "M1")
        seedRegalo("r1", "P1")
        seedPago("a1", "Abono", 100.0, "Efectivo")
        seedPago("a2", "Abono", 50.0, "Yape")
        useCase()(dispId, opticaId, "A")
        val pagosBefore = pagos()
        val movimientosBefore = movimientos()
        val orderBefore = order()

        val outcome = useCase()(dispId, opticaId, "B")

        assertEquals(LifecycleOutcome.AlreadyTerminal("Anulado"), outcome)
        assertEquals(pagosBefore, pagos())
        assertEquals(movimientosBefore, movimientos())
        assertEquals(orderBefore, order())
        assertEquals(4, stock("M1"))
        assertEquals(6, stock("P1"))
    }

    @Test
    fun redownloadedSyncedReversal_staysSingleAndRestockIsNotRepeated() = runTest {
        seedOrder()
        seedMontura("M1", stock = 3)
        seedItem("i1", "M1")
        useCase()(dispId, opticaId, "Cliente desistió")
        val synced = movimientos().single()

        repository.upsertMonturaMovimiento(synced.toRemoto().toEntity())
        repository.upsertMonturaMovimiento(synced.copy(id = "remote-other-device").toRemoto().toEntity())

        assertEquals(listOf("d1:anul:i1"), movimientos().map { it.referenciaId })
        assertEquals(4, stock("M1"))
        assertEquals(LifecycleOutcome.AlreadyTerminal("Anulado"), useCase()(dispId, opticaId, "Otra vez"))
        val restock = DispensacionStockHelper(repository.monturaCoordinator, RoomTransactionRunner(db))
            .restockOnce("M1", opticaId, 1, "d1:anul:i1", "Reintento")
        assertEquals(Result.success(false), restock)
        assertEquals(1, movimientos().size)
        assertEquals(4, stock("M1"))
    }

    @Test
    fun overlappingInvocations_applyExactlyOnce() = runTest {
        seedOrder()
        seedMontura("M1", stock = 3)
        seedItem("i1", "M1")
        seedPago("a1", "Abono", 100.0, "Efectivo")

        val outcomes = List(2) { async(Dispatchers.IO) { useCase()(dispId, opticaId, "Doble toque") } }.awaitAll()

        assertEquals(setOf(LifecycleOutcome.Applied, LifecycleOutcome.AlreadyTerminal("Anulado")), outcomes.toSet())
        assertEquals(1, reversos().size)
        assertEquals(4, stock("M1"))
    }

    @Test
    fun cancelOnReclamada_returnsAlreadyTerminalWithZeroWrites() = runTest {
        seedOrder(estado = "Reclamada")
        seedMontura("M1", stock = 3)
        seedItem("i1", "M1")
        seedPago("a1", "Abono", 100.0, "Efectivo")
        val orderBefore = order()

        val outcome = useCase()(dispId, opticaId, "No aplica")

        assertEquals(LifecycleOutcome.AlreadyTerminal("Reclamada"), outcome)
        assertEquals(1, pagos().size)
        assertEquals(3, stock("M1"))
        assertEquals(0, movimientos().size)
        assertEquals(orderBefore, order())
        coVerify(exactly = 0) { scheduler.scheduleInventarioSync(any()) }
    }

    @Test
    fun stockFailure_rollsBackReversosAndEstado() = runTest {
        seedOrder()
        seedItem("i1", "missing-montura")
        seedPago("a1", "Abono", 100.0, "Efectivo")

        val error = runCatching { useCase()(dispId, opticaId, "Cliente desistió") }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals(0, reversos().size)
        assertEquals("Pendiente", order().estadoEntrega)
        assertNull(order().motivoAnulacion)
        assertNull(order().fechaAnulacion)
    }

    @Test
    fun reversoInsertFailure_rollsBackEarlierReversoAndLeavesStockUntouched() = runTest {
        seedOrder()
        seedMontura("M1", stock = 3)
        seedItem("i1", "M1")
        seedPago("a1", "Abono", 100.0, "Efectivo")
        seedPago("a2", "Abono", 50.0, "Yape")
        val failingRepo = spyk(repository)
        var reversoInserts = 0
        coEvery { failingRepo.insertPago(match { it.tipo == "Reverso" }) } answers {
            if (++reversoInserts == 2) throw IllegalStateException("insert failed")
            callOriginal()
        }

        val error = runCatching { useCase(failingRepo)(dispId, opticaId, "Cliente desistió") }.exceptionOrNull()

        assertEquals("insert failed", error?.message)
        assertEquals(2, reversoInserts)
        assertEquals(0, reversos().size)
        assertEquals(3, stock("M1"))
        assertEquals(0, movimientos().size)
        assertEquals("Pendiente", order().estadoEntrega)
    }

    @Test
    fun invalidMotivo_isRejectedBeforeAnyWrite() = runTest {
        seedOrder()
        seedPago("a1", "Abono", 100.0, "Efectivo")

        val errors = listOf("", "   ", "x".repeat(501)).map { motivo ->
            runCatching { useCase()(dispId, opticaId, motivo) }.exceptionOrNull()
        }

        assertTrue(errors.all { it is IllegalArgumentException })
        assertEquals(1, pagos().size)
        assertEquals("Pendiente", order().estadoEntrega)
        assertNull(order().motivoAnulacion)
    }

    @Test
    fun legacyReembolso_isCompensatedSoNetIsZeroAndCashDropsByNetPaid() = runTest {
        seedOrder(estado = "Entregado")
        seedPago("a1", "Abono", 200.0, "Efectivo", daysAgo = 30)
        seedPago("e1", "Reembolso", 50.0, "Efectivo", daysAgo = 20)

        useCase()(dispId, opticaId, "Devolución")

        val created = pagos().filter { it.fecha == today }
        assertEquals(listOf("Reverso" to 200.0, "Abono" to 50.0), created.sortedBy { it.tipo != "Reverso" }.map { it.tipo to it.monto })
        assertEquals(0.0, netPaid(), 0.001)
        assertEquals(-150.0, created.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }, 0.001)
    }
}
