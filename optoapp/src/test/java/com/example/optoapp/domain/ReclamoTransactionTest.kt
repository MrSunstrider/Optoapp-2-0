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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReclamoTransactionTest {
    private val opticaId = "o1"
    private val origId = "d1"
    private lateinit var db: OptoDatabase
    private lateinit var repository: OptoRepository
    private val claimScheduler = mockk<PostSaveSyncScheduler>(relaxed = true)
    private val today = DateUtils.today()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repoSchedulerLazy = mockk<Lazy<PostSaveSyncScheduler>>()
        every { repoSchedulerLazy.get() } returns mockk(relaxed = true)
        repository = OptoRepository(
            database = db,
            syncStateTracker = mockk(relaxed = true),
            postSaveSyncScheduler = repoSchedulerLazy,
            pacienteRepo = PacienteRepository(db.pacienteDao(), db.evaluacionDao()),
            dispensacionRepo = DispensacionRepository(db.dispensacionDao(), db.dispensacionItemDao(), db.pagoDao(), db.servicioExtraDao()),
            syncRepo = SyncRepository(mockk(relaxed = true), db.monturaDao(), db.monturaMovimientoDao()),
            snapshotCoordinator = mockk<SyncSnapshotCoordinator>(relaxed = true),
            backupCoordinator = mockk<BackupRestoreCoordinator>(relaxed = true),
            monturaCoordinator = MonturaInventoryCoordinator(db.monturaDao(), db.monturaMovimientoDao(), repoSchedulerLazy),
            gastoOperativoDao = db.gastoOperativoDao(),
            supabase = mockk<SupabaseClient>(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun claim(
        total: Double,
        metodoReembolso: String = "Efectivo",
        motivo: String = " Lente rayado ",
        repo: OptoRepository = repository,
    ) = ReclamarDispensacionUseCase(
        repo, db.pagoDao(), DispensacionStockHelper(repository.monturaCoordinator, RoomTransactionRunner(db)), claimScheduler,
        CalcularMontoPagadoUseCase(db.pagoDao()),
    )(origId, opticaId, motivo, total, metodoReembolso)

    private fun recordingRepository(inserted: MutableList<Pago>): OptoRepository {
        val spy = spyk(repository)
        coEvery { spy.insertPago(any()) } coAnswers {
            inserted += firstArg<Pago>()
            callOriginal()
        }
        return spy
    }

    private fun lowestRunningNet(startNet: Double, rows: List<Pago>): Double =
        rows.runningFold(startNet) { net, pago -> net + PagoEffect.signedAmount(pago.tipo, pago.monto) }.min()

    private suspend fun seedOriginal(estado: String = "Entregado") {
        db.pacienteDao().insertPaciente(
            Paciente(id = "pac", nombreCompleto = "Paciente", edad = 30, telefono = "1", fechaCreacion = today, opticaId = opticaId),
        )
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = origId, ot = "2026-0042", pacienteId = "pac", fecha = today.minusDays(40), opticaId = opticaId,
                montoTotal = 200.0, estadoEntrega = estado, tipoLente = "Progresivo",
                fechaEntrega = today.minusDays(30), fechaVencimientoGarantia = today.plusDays(335),
            ),
        )
    }

    private suspend fun seedPago(id: String, tipo: String, monto: Double, metodo: String) = db.pagoDao().insertPago(
        Pago(id = id, dispensacionId = origId, fecha = today.minusDays(35), tipo = tipo, monto = monto, metodoPago = metodo, opticaId = opticaId),
    )

    private suspend fun original() = db.dispensacionDao().getDispensacionById(origId, opticaId)!!
    private suspend fun replacement() = db.dispensacionDao().getByReclamoOrigenId(origId, opticaId)
    private suspend fun pagosOf(parentId: String) = db.pagoDao().getPagosByParent(parentId, opticaId)
    private suspend fun net(parentId: String) = db.pagoDao().sumMontoByDispensacion(parentId, opticaId)
    private suspend fun claimDayPagos(): List<Pago> = db.pagoDao().getPagosListByOptica(opticaId).filter { it.fecha == today }
    private suspend fun cashDeltaBy(metodo: String) =
        claimDayPagos().filter { it.metodoPago == metodo }.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }

    @Test
    fun totalEqualToPaid_transfersOneAbonoPerMethodAndNetsCashToZero() = runTest {
        seedOriginal()
        seedPago("a1", "Abono", 120.0, "Efectivo")
        seedPago("a2", "Abono", 80.0, "Tarjeta")

        val outcome = claim(total = 200.0)

        val repl = replacement()!!
        assertEquals(ReclamoOutcome.Created(repl.id, "2026-0042-R1"), outcome)
        val orig = original()
        assertEquals("Reclamada", orig.estadoEntrega)
        assertEquals("Lente rayado", orig.motivoAnulacion)
        assertEquals(today, orig.fechaAnulacion)
        assertEquals(0.0, orig.montoPagado, 0.001)
        assertEquals(0.0, net(origId), 0.001)
        val reversos = pagosOf(origId).filter { it.tipo == "Reverso" }
        assertEquals(setOf("a1", "a2"), reversos.map { it.reversaPagoId }.toSet())
        assertTrue(reversos.all { it.fecha == today })

        assertNotEquals(origId, repl.id)
        assertEquals("2026-0042-R1", repl.ot)
        assertEquals("Pendiente", repl.estadoEntrega)
        assertEquals("pac", repl.pacienteId)
        assertEquals("Progresivo", repl.tipoLente)
        assertEquals(200.0, repl.montoTotal, 0.001)
        assertEquals(200.0, repl.montoPagado, 0.001)
        assertEquals(today, repl.fecha)
        assertNull(repl.fechaEntrega)
        assertNull(repl.fechaVencimientoGarantia)
        assertNull(repl.motivoAnulacion)
        assertNull(repl.fechaAnulacion)
        val transfer = pagosOf(repl.id)
        assertEquals(setOf("Abono" to 120.0 to "Efectivo", "Abono" to 80.0 to "Tarjeta"), transfer.map { it.tipo to it.monto to it.metodoPago }.toSet())
        assertTrue(transfer.all { it.fecha == today && it.ventaId == "v_disp_${repl.id}" && it.nota == "Crédito por reclamo de OT 2026-0042" })
        assertEquals(0.0, cashDeltaBy("Efectivo"), 0.001)
        assertEquals(0.0, cashDeltaBy("Tarjeta"), 0.001)
        coVerify(exactly = 1) { claimScheduler.scheduleFinanzasSync(opticaId) }
        coVerify(exactly = 1) { claimScheduler.scheduleInventarioSync(opticaId) }
    }

    @Test
    fun totalLowerThanPaid_refundsTheExcessOnTheReplacementWithTheChosenMethod() = runTest {
        seedOriginal()
        seedPago("a1", "Abono", 120.0, "Efectivo")
        seedPago("a2", "Abono", 80.0, "Tarjeta")

        claim(total = 150.0, metodoReembolso = "Tarjeta")

        val repl = replacement()!!
        val reembolso = pagosOf(repl.id).single { it.tipo == "Reembolso" }
        assertEquals(50.0, reembolso.monto, 0.001)
        assertEquals("Tarjeta", reembolso.metodoPago)
        assertEquals("Reembolso por reclamo de OT 2026-0042", reembolso.nota)
        assertEquals("v_disp_${repl.id}", reembolso.ventaId)
        assertEquals(today, reembolso.fecha)
        assertEquals(150.0, net(repl.id), 0.001)
        assertEquals(150.0, repl.montoPagado, 0.001)
        assertEquals(-50.0, claimDayPagos().sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }, 0.001)
        assertTrue(pagosOf(origId).none { it.tipo == "Reembolso" })
    }

    @Test
    fun refundMethodYape_isHonoredAndOnlyYapeCashMoves() = runTest {
        seedOriginal()
        seedPago("a1", "Abono", 120.0, "Efectivo")
        seedPago("a2", "Abono", 80.0, "Tarjeta")

        claim(total = 150.0, metodoReembolso = "Yape")

        val repl = replacement()!!
        assertEquals(listOf(50.0 to "Yape"), pagosOf(repl.id).filter { it.tipo == "Reembolso" }.map { it.monto to it.metodoPago })
        assertEquals(-50.0, cashDeltaBy("Yape"), 0.001)
        assertEquals(0.0, cashDeltaBy("Efectivo"), 0.001)
        assertEquals(0.0, cashDeltaBy("Tarjeta"), 0.001)
    }

    @Test
    fun mixedMethodOriginal_transfersEachMethodAndRefundsWithoutNegativeParents() = runTest {
        seedOriginal()
        seedPago("a1", "Abono", 150.0, "Efectivo")
        seedPago("a2", "Abono", 50.0, "Yape")
        val inserted = mutableListOf<Pago>()

        claim(total = 120.0, metodoReembolso = "Tarjeta", repo = recordingRepository(inserted))

        val repl = replacement()!!
        assertEquals(
            setOf(Triple("Abono", 150.0, "Efectivo"), Triple("Abono", 50.0, "Yape"), Triple("Reembolso", 80.0, "Tarjeta")),
            pagosOf(repl.id).map { Triple(it.tipo, it.monto, it.metodoPago) }.toSet(),
        )
        assertEquals(120.0, net(repl.id), 0.001)
        assertEquals(0.0, net(origId), 0.001)
        assertEquals(5, inserted.size)
        assertTrue(lowestRunningNet(200.0, inserted.filter { it.dispensacionId == origId }) >= -0.001)
        assertTrue(lowestRunningNet(0.0, inserted.filter { it.dispensacionId == repl.id }) >= -0.001)
        assertEquals(-80.0, claimDayPagos().sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }, 0.001)
    }

    @Test
    fun totalHigherThanPaid_leavesTheDifferenceAsSaldoWithoutRefund() = runTest {
        seedOriginal()
        seedPago("a1", "Abono", 120.0, "Efectivo")
        seedPago("a2", "Abono", 80.0, "Tarjeta")

        claim(total = 260.0)

        val repl = replacement()!!
        assertTrue(pagosOf(repl.id).none { it.tipo == "Reembolso" })
        assertEquals(200.0, repl.montoPagado, 0.001)
        assertEquals(60.0, repl.montoTotal - repl.montoPagado, 0.001)
    }

    @Test
    fun zeroTotalWithPayments_refundsEverythingWithTheChosenMethod() = runTest {
        seedOriginal()
        seedPago("a1", "Abono", 200.0, "Tarjeta")

        claim(total = 0.0, metodoReembolso = "Efectivo")

        val repl = replacement()!!
        assertEquals(listOf(200.0 to "Efectivo"), pagosOf(repl.id).filter { it.tipo == "Reembolso" }.map { it.monto to it.metodoPago })
        assertEquals(0.0, net(repl.id), 0.001)
        assertEquals(0.0, repl.montoPagado, 0.001)
        assertEquals(-200.0, claimDayPagos().sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }, 0.001)
    }

    @Test
    fun noPayments_createsReplacementWithoutPagosAndFullSaldo() = runTest {
        listOf(0.0, 100.0).forEachIndexed { index, total ->
            val id = "d$index"
            db.pacienteDao().insertPaciente(
                Paciente(id = "pac", nombreCompleto = "Paciente", edad = 30, telefono = "1", fechaCreacion = today, opticaId = opticaId),
            )
            db.dispensacionDao().insertDispensacion(
                DispensacionOptica(id = id, ot = "2026-005$index", pacienteId = "pac", fecha = today, opticaId = opticaId, estadoEntrega = "Entregado"),
            )

            val outcome = ReclamarDispensacionUseCase(
                repository, db.pagoDao(), DispensacionStockHelper(repository.monturaCoordinator, RoomTransactionRunner(db)), claimScheduler,
                CalcularMontoPagadoUseCase(db.pagoDao()),
            )(id, opticaId, "Garantía", total, "Efectivo") as ReclamoOutcome.Created

            assertEquals("2026-005$index-R1", outcome.replacementOt)
            assertTrue(pagosOf(outcome.replacementId).isEmpty())
            val repl = db.dispensacionDao().getDispensacionById(outcome.replacementId, opticaId)!!
            assertEquals(total, repl.montoTotal - repl.montoPagado, 0.001)
        }
    }

    @Test
    fun alreadyReclamada_schedulesNoSyncAndCreatesNoSecondReplacement() = runTest {
        seedOriginal()
        seedPago("a1", "Abono", 200.0, "Efectivo")
        val first = claim(total = 200.0) as ReclamoOutcome.Created

        val second = claim(total = 200.0)

        assertEquals(ReclamoOutcome.AlreadyTerminal("Reclamada"), second)
        assertEquals(listOf(first.replacementId), db.dispensacionDao().getDispensacionesListByOptica(opticaId).map { it.id }.filter { it != origId })
        coVerify(exactly = 1) { claimScheduler.scheduleFinanzasSync(opticaId) }
    }

    private fun creditsFirst(rows: List<Pago>) = rows.sortedBy { PagoEffect.signedAmount(it.tipo, it.monto) < 0 }

    private fun List<Pago>.summary() = map { Triple(it.tipo, it.monto, it.metodoPago) }.toSet()

    private suspend fun seedLegacySameMethodOriginal() {
        seedOriginal()
        seedPago("a1", "Abono", 120.0, "Efectivo")
        seedPago("a2", "Abono", 80.0, "Tarjeta")
        seedPago("e1", "Reembolso", 50.0, "Efectivo")
    }

    @Test
    fun legacyRefundWithoutDifference_transfersNetPerMethodAndMovesNoCash() = runTest {
        seedLegacySameMethodOriginal()

        claim(total = 150.0)

        val origNew = pagosOf(origId).filter { it.fecha == today }
        assertEquals(
            setOf(Triple("Reverso", 120.0, "Efectivo"), Triple("Reverso", 80.0, "Tarjeta"), Triple("Abono", 50.0, "Efectivo")),
            origNew.summary(),
        )
        assertTrue(origNew.single { it.tipo == "Abono" }.nota.startsWith("Compensación de Reembolso"))
        assertEquals(0.0, net(origId), 0.001)
        val repl = replacement()!!
        assertEquals(setOf(Triple("Abono", 70.0, "Efectivo"), Triple("Abono", 80.0, "Tarjeta")), pagosOf(repl.id).summary())
        assertEquals(150.0, repl.montoPagado, 0.001)
        assertEquals(0.0, cashDeltaBy("Efectivo"), 0.001)
        assertEquals(0.0, cashDeltaBy("Tarjeta"), 0.001)
    }

    @Test
    fun legacyRefundWithDifference_refundsOnlyTheDifference() = runTest {
        seedLegacySameMethodOriginal()

        claim(total = 100.0)

        val repl = replacement()!!
        assertEquals(listOf(50.0 to "Efectivo"), pagosOf(repl.id).filter { it.tipo == "Reembolso" }.map { it.monto to it.metodoPago })
        assertEquals(100.0, net(repl.id), 0.001)
        assertEquals(0.0, net(origId), 0.001)
        assertEquals(-50.0, claimDayPagos().sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }, 0.001)
        assertEquals(-50.0, cashDeltaBy("Efectivo"), 0.001)
        assertEquals(0.0, cashDeltaBy("Tarjeta"), 0.001)
    }

    @Test
    fun crossMethodLegacyRefund_addsAdjustmentReembolsoExcludedFromCashDelta() = runTest {
        seedOriginal()
        seedPago("a1", "Abono", 100.0, "Efectivo")
        seedPago("e1", "Reembolso", 30.0, "Yape")
        val inserted = mutableListOf<Pago>()

        val outcome = claim(total = 50.0, metodoReembolso = "Efectivo", repo = recordingRepository(inserted))

        assertTrue(outcome is ReclamoOutcome.Created)
        assertEquals(setOf(Triple("Reverso", 100.0, "Efectivo"), Triple("Abono", 30.0, "Yape")), pagosOf(origId).filter { it.fecha == today }.summary())
        assertEquals(0.0, net(origId), 0.001)
        val repl = replacement()!!
        val replPagos = pagosOf(repl.id)
        assertEquals(
            setOf(Triple("Abono", 100.0, "Efectivo"), Triple("Reembolso", 30.0, "Yape"), Triple("Reembolso", 20.0, "Efectivo")),
            replPagos.summary(),
        )
        val adjustment = replPagos.single { it.metodoPago == "Yape" }
        assertEquals("Ajuste de crédito por reclamo de OT 2026-0042", adjustment.nota)
        assertEquals("v_disp_${repl.id}", adjustment.ventaId)
        assertEquals(today, adjustment.fecha)
        assertEquals(50.0, net(repl.id), 0.001)
        assertEquals(0.0, repl.montoTotal - repl.montoPagado, 0.001)
        assertEquals(-20.0, claimDayPagos().sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }, 0.001)
        assertEquals(-20.0, cashDeltaBy("Efectivo"), 0.001)
        assertEquals(0.0, cashDeltaBy("Yape"), 0.001)
        assertEquals(5, inserted.size)
        assertTrue(lowestRunningNet(70.0, creditsFirst(inserted.filter { it.dispensacionId == origId })) >= -0.001)
        assertTrue(lowestRunningNet(0.0, inserted.filter { it.dispensacionId == repl.id }) >= -0.001)
    }

    @Test
    fun crossMethodOrphanReverso_withoutDifference_movesNoCash() = runTest {
        seedOriginal()
        seedPago("a1", "Abono", 100.0, "Efectivo")
        seedPago("r0", "Reverso", 30.0, "Yape")

        claim(total = 70.0)

        val repl = replacement()!!
        assertEquals(setOf(Triple("Abono", 100.0, "Efectivo"), Triple("Reembolso", 30.0, "Yape")), pagosOf(repl.id).summary())
        assertEquals(70.0, net(repl.id), 0.001)
        assertEquals(0.0, net(origId), 0.001)
        assertEquals(0.0, cashDeltaBy("Efectivo"), 0.001)
        assertEquals(0.0, cashDeltaBy("Yape"), 0.001)
    }

    private suspend fun seedMontura(id: String, stock: Int) =
        db.monturaDao().insertMontura(Montura(id = id, sku = "sku-$id", stockActual = stock, opticaId = opticaId))

    private suspend fun stock(id: String) = db.monturaDao().getMonturaByIdForOptica(id, opticaId)!!.stockActual
    private suspend fun movimientos(): List<MonturaMovimiento> = db.monturaMovimientoDao().getMovimientosListByOptica(opticaId)

    private suspend fun seedStoreItemAndRegalo(frameStock: Int) {
        seedMontura("M1", frameStock)
        seedMontura("P1", stock = 5)
        db.dispensacionItemDao().insertItem(
            DispensacionItem(id = "i1", dispensacionId = origId, monturaId = "M1", origenMontura = "Tienda", descripcionMontura = "Ray-Ban 5154", opticaId = opticaId),
        )
        db.regaloDispensacionDao().insert(
            RegaloDispensacionEntity(
                id = "r1", dispensacionId = origId, productoId = "P1", cantidad = 1,
                costoUnitario = 5.0, descripcion = "Estuche", motivo = "", opticaId = opticaId,
            ),
        )
    }

    @Test
    fun copiedStoreFrame_isConsumedByTheReplacementWhileRegalosAndOriginalStockStay() = runTest {
        seedOriginal()
        seedStoreItemAndRegalo(frameStock = 3)
        seedPago("a1", "Abono", 200.0, "Efectivo")

        val created = claim(total = 200.0) as ReclamoOutcome.Created

        val copied = db.dispensacionItemDao().getItemsListByDispensacion(created.replacementId, opticaId).single()
        assertEquals("M1", copied.monturaId)
        assertNotEquals("i1", copied.id)
        assertTrue(db.regaloDispensacionDao().getByDispensacionId(created.replacementId, opticaId).isEmpty())
        assertEquals(2, stock("M1"))
        assertEquals(5, stock("P1"))
        val salida = movimientos().single()
        assertEquals("SALIDA_VENTA" to created.replacementId, salida.tipo to salida.referenciaId)
        assertEquals("M1", salida.monturaId)
    }

    @Test
    fun legacyHeaderStoreFrame_isConsumedByTheReplacement() = runTest {
        seedOriginal()
        db.dispensacionDao().updateDispensacion(original().copy(monturaId = "M1", origenMontura = "Tienda"))
        seedMontura("M1", stock = 1)

        val created = claim(total = 200.0) as ReclamoOutcome.Created

        assertEquals(0, stock("M1"))
        assertEquals(listOf(created.replacementId), movimientos().map { it.referenciaId })
    }

    @Test
    fun legacyNuevaDeTiendaHeaderFrame_isConsumedByTheReplacement() = runTest {
        seedOriginal()
        db.dispensacionDao().updateDispensacion(original().copy(monturaId = "M1", origenMontura = "Nueva de Tienda"))
        seedMontura("M1", stock = 1)

        val created = claim(total = 200.0) as ReclamoOutcome.Created

        assertEquals(0, stock("M1"))
        assertEquals(listOf(created.replacementId), movimientos().map { it.referenciaId })
    }

    @Test
    fun outOfStockCopiedFrame_failsTheWholeClaimWithTypedError() = runTest {
        seedOriginal()
        seedStoreItemAndRegalo(frameStock = 0)
        seedPago("a1", "Abono", 200.0, "Efectivo")

        val error = runCatching { claim(total = 200.0) }.exceptionOrNull()

        assertTrue(error is ReclamoStockInsuficienteException)
        assertEquals("M1", (error as ReclamoStockInsuficienteException).monturaId)
        assertTrue(error.message!!.contains("Ray-Ban 5154"))
        val orig = original()
        assertEquals("Entregado", orig.estadoEntrega)
        assertNull(orig.motivoAnulacion)
        assertNull(orig.fechaAnulacion)
        assertNull(replacement())
        assertEquals(listOf("a1"), pagosOf(origId).map { it.id })
        assertTrue(movimientos().isEmpty())
        assertEquals(0, stock("M1"))
        coVerify(exactly = 0) { claimScheduler.scheduleInventarioSync(any()) }
    }

    @Test
    fun failureAfterReversos_rollsBackTheWholeClaim() = runTest {
        seedOriginal()
        seedStoreItemAndRegalo(frameStock = 3)
        seedPago("a1", "Abono", 200.0, "Efectivo")
        val failingRepo = spyk(repository)
        coEvery { failingRepo.insertPago(match { it.nota.startsWith("Crédito por reclamo") }) } throws IllegalStateException("insert failed")

        val error = runCatching {
            ReclamarDispensacionUseCase(
                failingRepo, db.pagoDao(), DispensacionStockHelper(repository.monturaCoordinator, RoomTransactionRunner(db)), claimScheduler,
                CalcularMontoPagadoUseCase(db.pagoDao()),
            )(origId, opticaId, "Lente rayado", 200.0, "Efectivo")
        }.exceptionOrNull()

        assertEquals("insert failed", error?.message)
        assertEquals("Entregado", original().estadoEntrega)
        assertNull(replacement())
        assertEquals(listOf("a1"), pagosOf(origId).map { it.id })
        assertEquals(3, stock("M1"))
        assertTrue(movimientos().isEmpty())
    }

    @Test
    fun negativeNetPaid_throwsWithoutWrites() = runTest {
        seedOriginal()
        seedPago("e1", "Reembolso", 50.0, "Efectivo")

        val error = runCatching { claim(total = 100.0) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("Saldo pagado inconsistente en la orden original; sincroniza y reintenta.", error!!.message)
        assertEquals("Entregado", original().estadoEntrega)
        assertNull(replacement())
        assertEquals(listOf("e1"), pagosOf(origId).map { it.id })
    }
}
