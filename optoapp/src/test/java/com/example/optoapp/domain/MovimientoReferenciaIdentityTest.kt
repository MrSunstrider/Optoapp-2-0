package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the rule: referenciaId names the movement fact inside (tipo, monturaId).
 * Empty or shared-parent IDs make idx_movimientos_conflict reject a second event.
 */
class MovimientoReferenciaIdentityTest {

    @Test
    fun manualMovementUsesOwnIdAsReferencia() {
        val id = "mov-manual-1"
        val ref = movimientoReferenciaForManual(id)
        assertEquals(id, ref)
        assertTrue(ref.isNotBlank())
    }

    @Test
    fun regaloUsesRegaloIdNotDispensacionId() {
        val regaloId = "reg-1"
        val dispensacionId = "disp-1"
        val ref = movimientoReferenciaForRegalo(regaloId)
        assertEquals(regaloId, ref)
        assertNotEquals(dispensacionId, ref)
    }

    @Test
    fun ordenCompraEntradaUsesLineItemIdNotOrdenId() {
        val ocId = "oc-1"
        val itemId = "item-1"
        val ref = movimientoReferenciaForOrdenCompraItem(itemId)
        assertEquals(itemId, ref)
        assertNotEquals(ocId, ref)
    }

    @Test
    fun inventarioFisicoUsesDetalleIdNotSessionId() {
        val sessionId = "session-1"
        val detalleId = "det-1"
        val ref = movimientoReferenciaForInventarioDetalle(detalleId)
        assertEquals(detalleId, ref)
        assertNotEquals(sessionId, ref)
    }

    @Test
    fun twoManualMovesOnSameMonturaMustNotShareReferencia() {
        val a = movimientoReferenciaForManual("mov-a")
        val b = movimientoReferenciaForManual("mov-b")
        assertNotEquals(a, b)
    }
}
