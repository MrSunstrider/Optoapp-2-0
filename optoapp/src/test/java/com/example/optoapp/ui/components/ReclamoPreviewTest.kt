package com.example.optoapp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReclamoPreviewTest {

    @Test
    fun `new total lower than the transferred credit refunds the difference`() {
        val preview = reclamoPreview(creditoTransferido = 200.0, nuevoTotalInput = "150")

        assertEquals(150.0, preview.nuevoTotal!!, 0.001)
        assertEquals(50.0, preview.reembolso, 0.001)
        assertEquals(0.0, preview.saldoReemplazo, 0.001)
        assertTrue(preview.muestraMetodoReembolso)
    }

    @Test
    fun `new total equal to the transferred credit refunds nothing and hides the refund method`() {
        val preview = reclamoPreview(creditoTransferido = 200.0, nuevoTotalInput = "200")

        assertEquals(0.0, preview.reembolso, 0.001)
        assertEquals(0.0, preview.saldoReemplazo, 0.001)
        assertFalse(preview.muestraMetodoReembolso)
    }

    @Test
    fun `new total higher than the transferred credit leaves a balance on the replacement`() {
        val preview = reclamoPreview(creditoTransferido = 200.0, nuevoTotalInput = "260,50")

        assertEquals(260.5, preview.nuevoTotal!!, 0.001)
        assertEquals(0.0, preview.reembolso, 0.001)
        assertEquals(60.5, preview.saldoReemplazo, 0.001)
        assertFalse(preview.muestraMetodoReembolso)
    }

    @Test
    fun `new total zero refunds the whole transferred credit`() {
        val preview = reclamoPreview(creditoTransferido = 200.0, nuevoTotalInput = "0")

        assertEquals(0.0, preview.nuevoTotal!!, 0.001)
        assertEquals(200.0, preview.reembolso, 0.001)
        assertTrue(preview.muestraMetodoReembolso)
    }

    @Test
    fun `new total zero on an unpaid original refunds nothing`() {
        val preview = reclamoPreview(creditoTransferido = 0.0, nuevoTotalInput = "0")

        assertEquals(0.0, preview.reembolso, 0.001)
        assertFalse(preview.muestraMetodoReembolso)
    }

    @Test
    fun `negative or unparsable new total is invalid and refunds nothing`() {
        listOf("-10", "abc", "").forEach { input ->
            val preview = reclamoPreview(creditoTransferido = 200.0, nuevoTotalInput = input)

            assertNull(input, preview.nuevoTotal)
            assertEquals(input, 0.0, preview.reembolso, 0.001)
            assertFalse(input, preview.muestraMetodoReembolso)
        }
    }

    @Test
    fun `negative net paid never yields a negative transferred credit`() {
        val preview = reclamoPreview(creditoTransferido = -30.0, nuevoTotalInput = "100")

        assertEquals(0.0, preview.creditoTransferido, 0.001)
        assertEquals(100.0, preview.saldoReemplazo, 0.001)
    }

    @Test
    fun `claim can be confirmed only with a motivo and a valid total while idle`() {
        val valid = reclamoPreview(200.0, "150")
        val invalid = reclamoPreview(200.0, "-1")

        assertTrue(canConfirmReclamo("Lente rayado", valid, "Efectivo", submitting = false))
        assertFalse(canConfirmReclamo("  ", valid, "Efectivo", submitting = false))
        assertFalse(canConfirmReclamo("Lente rayado", invalid, "Efectivo", submitting = false))
        assertFalse(canConfirmReclamo("Lente rayado", valid, "Efectivo", submitting = true))
    }

    @Test
    fun `non finite new total is invalid and refunds nothing`() {
        listOf("Infinity", "1e309", "NaN").forEach { input ->
            val preview = reclamoPreview(creditoTransferido = 200.0, nuevoTotalInput = input)

            assertNull(input, preview.nuevoTotal)
            assertEquals(input, 0.0, preview.reembolso, 0.001)
            assertFalse(input, preview.muestraMetodoReembolso)
        }
    }

    @Test
    fun `excess within the ledger epsilon shows no refund like the use case`() {
        val preview = reclamoPreview(creditoTransferido = 200.0, nuevoTotalInput = "199.997")

        assertEquals(199.997, preview.nuevoTotal!!, 0.0001)
        assertEquals(0.0, preview.reembolso, 0.0)
        assertFalse(preview.muestraMetodoReembolso)
    }

    @Test
    fun `claim with a shown refund requires a refund method`() {
        val conReembolso = reclamoPreview(200.0, "150")
        val sinReembolso = reclamoPreview(200.0, "200")

        assertFalse(canConfirmReclamo("Lente rayado", conReembolso, "", submitting = false))
        assertFalse(canConfirmReclamo("Lente rayado", conReembolso, "  ", submitting = false))
        assertTrue(canConfirmReclamo("Lente rayado", sinReembolso, "", submitting = false))
    }

    @Test
    fun `refund method options keep a non standard suggested method selectable`() {
        assertEquals(listOf("Efectivo", "Tarjeta", "Transferencia", "Yape"), metodosReembolso("Yape"))
        assertEquals(listOf("Efectivo", "Tarjeta", "Transferencia"), metodosReembolso("Tarjeta"))
    }
}
