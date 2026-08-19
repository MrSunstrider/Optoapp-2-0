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
    fun `79 plus 1 partitions quarantine poison only`() {
        val valid = (1..79).map { i ->
            Triple("p$i", FinanzasUploadValidator.validatePago("Abono", 10.0, "d$i", null, null), i)
        }
        val poison = FinanzasUploadValidator.validatePago("Abono", -5.0, "d80", null, null)
        assertTrue(valid.all { it.second == null })
        assertEquals("quarantine:negative_monto", poison)
        assertEquals(79, valid.count { it.second == null })
    }
}
