package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.regaloservicio.RegaloServicioExtraEntity
import com.example.optoapp.data.servicio.ServicioExtraItem
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.DispensacionStockHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CancelLedgerUseCasesTest {
    private val repository = mockk<OptoRepository>(relaxed = true)
    private val pagoDao = mockk<PagoDao>(relaxed = true)
    private val scheduler = mockk<PostSaveSyncScheduler>(relaxed = true)
    private val stockHelper = mockk<DispensacionStockHelper>(relaxed = true)
    private val date = LocalDate.of(2026, 8, 14)
    private var inTransaction = false
    private val writesOutsideTransaction = mutableListOf<String>()

    init {
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            inTransaction = true
            try {
                firstArg<suspend () -> Any?>().invoke()
            } finally {
                inTransaction = false
            }
        }
    }

    private fun recordWrite(kind: String) {
        if (!inTransaction) writesOutsideTransaction += kind
    }

    private fun servicio(estado: String = "Pendiente", monturaId: String? = null) = ServicioExtra(
        id = "s1", descripcion = "x", montoTotal = 100.0, estado = estado, fecha = date, monturaId = monturaId,
    )

    private fun servicioItem(id: String, monturaId: String) = ServicioExtraItem(
        id = id, servicioExtraId = "s1", monturaId = monturaId, descripcion = id, monto = 20.0, opticaId = "o1",
    )

    private fun stubServicio(
        servicio: ServicioExtra,
        items: List<ServicioExtraItem> = emptyList(),
        regalos: List<RegaloServicioExtraEntity> = emptyList(),
        pagos: List<Pago> = emptyList(),
    ): MutableList<ServicioExtra> {
        coEvery { repository.getServicioById("s1", "o1") } returns Resource.Success(servicio)
        coEvery { repository.getServicioExtraItems("s1", "o1") } returns items
        coEvery { repository.getRegalosByServicioExtraId("s1", "o1") } returns regalos
        coEvery { pagoDao.getPagosByParent("s1", "o1") } returns pagos
        coEvery { repository.insertPago(any()) } answers { recordWrite("pago") }
        coEvery { stockHelper.restockOnce(any(), any(), any(), any(), any()) } answers {
            recordWrite("stock")
            Result.success(true)
        }
        val updates = mutableListOf<ServicioExtra>()
        coEvery { repository.updateServicio(capture(updates)) } answers { recordWrite("servicio") }
        return updates
    }

    private suspend fun cancelServicio(motivo: String = "Error de registro") =
        CancelServicioExtraUseCase(repository, pagoDao, scheduler, stockHelper)("s1", "o1", motivo)

    @Test
    fun cancelServicio_reversesCreditsRestocksAndStoresMetadataInOneTransaction() = runTest {
        val credit = Pago(
            id = "p1", servicioExtraId = "s1", fecha = date.minusDays(3),
            tipo = "Abono", monto = 80.0, metodoPago = "Efectivo", opticaId = "o1",
        )
        val updates = stubServicio(
            servicio(),
            items = listOf(servicioItem("item-a", "m-a"), servicioItem("item-b", "m-b")),
            regalos = listOf(
                RegaloServicioExtraEntity(
                    id = "reg-1", servicioExtraId = "s1", productoId = "P1", cantidad = 2,
                    costoUnitario = 5.0, descripcion = "Estuche", opticaId = "o1",
                ),
            ),
            pagos = listOf(credit),
        )
        val reverso = slot<Pago>()
        coEvery { repository.insertPago(capture(reverso)) } answers { recordWrite("pago") }

        val outcome = cancelServicio("  Error de registro ")

        assertEquals(LifecycleOutcome.Applied, outcome)
        assertEquals("Reverso", reverso.captured.tipo)
        assertEquals("p1", reverso.captured.reversaPagoId)
        assertEquals(80.0, reverso.captured.monto, 0.001)
        assertEquals(DateUtils.today(), reverso.captured.fecha)
        coVerify(exactly = 1) { stockHelper.restockOnce("m-a", "o1", 1, "item-a:anul", any()) }
        coVerify(exactly = 1) { stockHelper.restockOnce("m-b", "o1", 1, "item-b:anul", any()) }
        coVerify(exactly = 1) { stockHelper.restockOnce("P1", "o1", 2, "reg-1:anul", any()) }
        val saved = updates.single()
        assertEquals("Anulado", saved.estado)
        assertEquals("Error de registro", saved.motivoAnulacion)
        assertEquals(DateUtils.today(), saved.fechaAnulacion)
        assertEquals(emptyList<String>(), writesOutsideTransaction)
        coVerify(exactly = 1) { scheduler.scheduleFinanzasSync("o1") }
        coVerify(exactly = 1) { scheduler.scheduleInventarioSync("o1") }
    }

    @Test
    fun cancelServicio_legacyHeaderFrameKeepsServicioReversoReferencia() = runTest {
        stubServicio(servicio(monturaId = "m-liquido"))

        cancelServicio()

        coVerify(exactly = 1) {
            stockHelper.restockOnce("m-liquido", "o1", 1, movimientoReferenciaForServicioExtraReverso("s1", "m-liquido"), any())
        }
    }

    @Test
    fun cancelServicio_withItemsNeverRestocksTheHeaderFrame() = runTest {
        stubServicio(servicio(monturaId = "m-header"), items = listOf(servicioItem("item-a", "m-a")))

        cancelServicio()

        coVerify(exactly = 0) { stockHelper.restockOnce("m-header", any(), any(), any(), any()) }
    }

    @Test
    fun cancelServicio_withoutFramesOrRegalosSkipsRestock() = runTest {
        stubServicio(servicio())

        cancelServicio()

        coVerify(exactly = 0) { stockHelper.restockOnce(any(), any(), any(), any(), any()) }
    }

    @Test
    fun cancelServicio_blankMotivoIsRejectedBeforeAnyWrite() = runTest {
        stubServicio(servicio(monturaId = "m-1"))

        val error = runCatching { cancelServicio("   ") }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        coVerify(exactly = 0) { repository.updateServicio(any()) }
        coVerify(exactly = 0) { repository.insertPago(any()) }
        coVerify(exactly = 0) { stockHelper.restockOnce(any(), any(), any(), any(), any()) }
    }

    @Test
    fun cancelServicio_stockFailureLeavesServicioActiveWithoutMetadata() = runTest {
        val updates = stubServicio(servicio(), items = listOf(servicioItem("item-1", "m-1")))
        coEvery { stockHelper.restockOnce("m-1", "o1", 1, "item-1:anul", any()) } returns
            Result.failure(IllegalStateException("restock failed"))

        val error = runCatching { cancelServicio() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals(emptyList<ServicioExtra>(), updates)
        coVerify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
    }

    @Test
    fun cancelServicio_alreadyAnuladoIsANoOpThatKeepsMetadata() = runTest {
        stubServicio(servicio(estado = " Anulado "))

        val outcome = cancelServicio("Otro motivo")

        assertEquals(LifecycleOutcome.AlreadyTerminal("Anulado"), outcome)
        coVerify(exactly = 0) { repository.insertPago(any()) }
        coVerify(exactly = 0) { repository.updateServicio(any()) }
        coVerify(exactly = 0) { stockHelper.restockOnce(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
    }

    @Test
    fun reclaim_positiveReembolsoWithoutReversaLink() = runTest {
        coEvery { repository.getDispensacionById("d1", any()) } returns Resource.Success(
            DispensacionOptica(
                id = "d1", pacienteId = "pac", fecha = date, opticaId = "o1",
                estadoEntrega = "Pendiente", metodoPago = "Efectivo", ot = "OT-1",
            ),
        )
        val slot = slot<Pago>()
        coEvery { repository.insertPago(capture(slot)) } returns Unit

        ReclaimDispensacionUseCase(repository, scheduler)("d1", "o1", 50.0, "Efectivo", "OT-1")

        assertEquals("Reembolso", slot.captured.tipo)
        assertEquals(50.0, slot.captured.monto, 0.001)
        assertNull(slot.captured.reversaPagoId)
        coVerify { repository.updateDispensacion(match { it.estadoEntrega == "Reclamada" }) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun reclaim_rejectsNegativeMonto() = runTest {
        ReclaimDispensacionUseCase(repository, scheduler)("d1", "o1", -1.0, "Efectivo", "OT-1")
    }

    private fun ledgerPago(
        id: String,
        tipo: String,
        monto: Double,
        metodo: String,
        reversaPagoId: String? = null,
        ventaId: String? = null,
    ) = Pago(
        id = id, dispensacionId = "d1", fecha = date, tipo = tipo, monto = monto,
        metodoPago = metodo, opticaId = "o1", reversaPagoId = reversaPagoId, ventaId = ventaId,
    )

    private fun stubLedger(pagos: List<Pago>): MutableList<Pago> {
        val inserted = mutableListOf<Pago>()
        coEvery { pagoDao.getPagosByParent("d1", "o1") } returns pagos
        coEvery { repository.insertPago(capture(inserted)) } returns Unit
        return inserted
    }

    @Test
    fun reverseLedgerFully_legacyReembolso_reversesCreditThenCompensatesDebitToNetZero() = runTest {
        val original = listOf(
            ledgerPago("abono-200", "Abono", 200.0, "Efectivo", ventaId = "v-1"),
            ledgerPago("reembolso-50", "Reembolso", 50.0, "Efectivo", ventaId = "v-2"),
        )
        val inserted = stubLedger(original)

        val snapshot = reverseLedgerFully(repository, pagoDao, "d1", "o1", forDispensacion = true, contexto = "anulación")

        assertEquals(listOf("Reverso", "Abono"), inserted.map { it.tipo })
        val reverso = inserted[0]
        assertEquals("abono-200", reverso.reversaPagoId)
        assertEquals(200.0, reverso.monto, 0.001)
        val compensation = inserted[1]
        assertEquals(50.0, compensation.monto, 0.001)
        assertEquals("Efectivo", compensation.metodoPago)
        assertEquals("v-2", compensation.ventaId)
        assertEquals("d1", compensation.dispensacionId)
        assertNull(compensation.reversaPagoId)
        assertEquals("Compensación de Reembolso reembols por anulación", compensation.nota)
        assertEquals(listOf(DateUtils.today(), DateUtils.today()), inserted.map { it.fecha })
        assertEquals(0.0, (original + inserted).sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }, 0.001)
        assertEquals(150.0, snapshot.netPaid, 0.001)
    }

    @Test
    fun reverseLedgerFully_whitespacePaddedTipos_reversesEachUnreversedCreditOnceToNetZero() = runTest {
        val original = listOf(
            ledgerPago("padded-abono", " Abono", 100.0, "Efectivo"),
            ledgerPago("abono-t", "Abono", 50.0, "Tarjeta"),
            ledgerPago("padded-reverso", " Reverso ", 50.0, "Tarjeta", reversaPagoId = "abono-t"),
        )
        val inserted = stubLedger(original)

        reverseLedgerFully(repository, pagoDao, "d1", "o1", forDispensacion = true, contexto = "anulación")

        assertEquals(listOf("padded-abono"), inserted.map { it.reversaPagoId })
        assertEquals("Reverso", inserted.single().tipo)
        assertEquals(100.0, inserted.single().monto, 0.001)
        assertEquals(0.0, (original + inserted).sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }, 0.001)
    }

    @Test
    fun reverseLedgerFully_servicioParent_alreadyReversedCreditGetsNoSecondReverso() = runTest {
        val inserted = stubLedger(
            listOf(
                ledgerPago("a1", "Abono", 100.0, "Efectivo"),
                ledgerPago("r1", "Reverso", 100.0, "Efectivo", reversaPagoId = "a1"),
                ledgerPago("a2", "Pago completo", 30.0, "Yape"),
            ),
        )

        val snapshot = reverseLedgerFully(repository, pagoDao, "d1", "o1", forDispensacion = false, contexto = "anulación")

        assertEquals(listOf("a2"), inserted.map { it.reversaPagoId })
        assertEquals("d1", inserted.single().servicioExtraId)
        assertNull(inserted.single().dispensacionId)
        assertEquals(mapOf("Yape" to 30.0), snapshot.netByMetodo)
    }
}
