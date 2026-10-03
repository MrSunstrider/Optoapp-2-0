package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanzasUploadValidatorTest {

    @Test
    fun `valid Abono passes`() {
        assertNull(
            FinanzasUploadValidator.validatePago("Abono", 100.0, "d1", null, null),
        )
    }

    @Test
    fun `negative monto quarantines`() {
        val reason = FinanzasUploadValidator.validatePago("Abono", -1.0, "d1", null, null)
        assertEquals("quarantine:negative_monto", reason)
    }

    @Test
    fun `unknown tipo quarantines`() {
        val reason = FinanzasUploadValidator.validatePago("Efectivo", 10.0, "d1", null, null)
        assertTrue(reason!!.startsWith("quarantine:invalid_tipo"))
    }

    @Test
    fun `xor origen required`() {
        assertEquals(
            "quarantine:xor_origen",
            FinanzasUploadValidator.validatePago("Abono", 10.0, null, null, null),
        )
        assertEquals(
            "quarantine:xor_origen",
            FinanzasUploadValidator.validatePago("Abono", 10.0, "d1", "s1", null),
        )
    }

    @Test
    fun `Reverso requires link and non-Reverso forbids it`() {
        assertEquals(
            "quarantine:reverso_missing_link",
            FinanzasUploadValidator.validatePago("Reverso", 10.0, "d1", null, null),
        )
        assertNull(FinanzasUploadValidator.validatePago("Reverso", 10.0, "d1", null, "p1"))
        assertEquals(
            "quarantine:reversa_on_non_reverso",
            FinanzasUploadValidator.validatePago("Abono", 10.0, "d1", null, "p1"),
        )
    }

    @Test
    fun `estado domains accept Anulado and Reclamada`() {
        assertNull(FinanzasUploadValidator.validateDispensacionEstado("Anulado"))
        assertNull(FinanzasUploadValidator.validateDispensacionEstado("Reclamada"))
        assertNull(FinanzasUploadValidator.validateServicioEstado("Anulado"))
        assertNotNull(FinanzasUploadValidator.validateDispensacionEstado("Cancelado"))
        assertNotNull(FinanzasUploadValidator.validateServicioEstado("Cancelado"))
    }

    @Test
    fun `cancelled and claimed ledgers pass upload validation`() {
        assertNull(FinanzasUploadValidator.validateDispensacionEstado(" Anulado "))
        assertNull(FinanzasUploadValidator.validateServicioEstado(" Anulado "))
        assertNull(FinanzasUploadValidator.validatePago("Reverso", 100.0, "d1", null, "p1"))
        assertNull(FinanzasUploadValidator.validatePago("Reverso", 80.0, null, "s1", "ps1"))
        assertNull(FinanzasUploadValidator.validatePago("Abono", 120.0, "d1-R1", null, null))
        assertNull(FinanzasUploadValidator.validatePago("Reembolso", 50.0, "d1-R1", null, null))
        assertEquals(0.0, FinanzasUploadValidator.safeParentBalanceForUpload(0.0), 0.0)
    }

    @Test
    fun `constraint detection for 23514`() {
        assertTrue(FinanzasUploadValidator.isConstraintViolation("ERROR: 23514 new row violates check"))
        assertFalse(FinanzasUploadValidator.isConstraintViolation("network timeout"))
    }

    @Test
    fun `RLS 42501 is isolatable so leftover PKs do not fail the whole batch`() {
        val rls = "new row violates row-level security policy for table \"dispensaciones\" Code: 42501"
        assertTrue(FinanzasUploadValidator.isIsolatableUploadFailure(rls))
        assertFalse(FinanzasUploadValidator.isIsolatableUploadFailure("network timeout"))
        assertTrue(FinanzasUploadValidator.isIsolatableUploadFailure("ERROR: 23514 check"))
    }

    @Test
    fun `safeParentBalanceForUpload floors negative net to zero`() {
        assertEquals(0.0, FinanzasUploadValidator.safeParentBalanceForUpload(-50.0), 0.001)
        assertEquals(150.0, FinanzasUploadValidator.safeParentBalanceForUpload(150.0), 0.001)
        assertEquals(0.0, FinanzasUploadValidator.safeParentBalanceForUpload(-0.001), 0.001)
    }

    @Test
    fun `79 plus 1 partitions quarantine poison only`() {
        val valid = (1..79).map { i ->
            Triple("p$i", FinanzasUploadValidator.validatePago("Abono", 10.0, "d$i", null, null), i)
        }
        val poison = FinanzasUploadValidator.validatePago("Abono", -5.0, "d80", null, null)
        assertTrue(valid.all { it.second == null })
        assertEquals("quarantine:negative_monto", poison)
        assertEquals(79, valid.count { it.second == null })
    }

    @Test
    fun `only claim and reverso unique violations are isolatable among 23505`() {
        val claim = "duplicate key value violates unique constraint \"dispensaciones_reclamo_origen_uidx\" Code: 23505"
        val reverso = "duplicate key value violates unique constraint \"pagos_reversa_pago_id_uidx\" Code: 23505"
        val other = "duplicate key value violates unique constraint \"dispensaciones_ot_key\" Code: 23505"

        assertTrue(FinanzasUploadValidator.isIsolatableUploadFailure(claim))
        assertTrue(FinanzasUploadValidator.isIsolatableUploadFailure(reverso))
        assertFalse(FinanzasUploadValidator.isIsolatableUploadFailure(other))
        assertEquals(FinanzasUploadValidator.CLAIM_UNIQUE_VIOLATION, FinanzasUploadValidator.poisonReason(claim))
        assertEquals(FinanzasUploadValidator.REVERSO_UNIQUE_VIOLATION, FinanzasUploadValidator.poisonReason(reverso))
    }

    @Test
    fun `reclamo duplicate reason round trips the original id`() {
        val reason = FinanzasUploadValidator.reclamoDuplicateReason("orig-1")

        assertEquals("quarantine:reclamo_duplicate:orig-1", reason)
        assertEquals("orig-1", FinanzasUploadValidator.reclamoOrigenIdOf(reason))
        assertNull(FinanzasUploadValidator.reclamoOrigenIdOf("quarantine:parent_missing:dispensacion:orig-1"))
    }
}
