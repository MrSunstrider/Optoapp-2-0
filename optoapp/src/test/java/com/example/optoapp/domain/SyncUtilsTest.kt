package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests normalization logic used by SyncFinanzasUseCase (normalizedOtForUnique)
 * and SyncHistorialUseCase / SyncPacientesUseCase (normalizedHistoriaKey).
 *
 * normalizedOtForUnique is a top-level `internal` function — callable directly.
 * normalizedHistoriaKey was extracted to a top-level `internal` function in
 * SyncHistorialUseCase.kt — callable directly from this package.
 */
class SyncUtilsTest {

    @Test
    fun normalizedOtForUnique_trimsAndUppercases() {
        assertEquals("OT-123", normalizedOtForUnique("ot-123"))
    }

    @Test
    fun normalizedOtForUnique_trimsWhitespace() {
        assertEquals("OT-456", normalizedOtForUnique("  Ot-456  "))
    }

    @Test
    fun normalizedOtForUnique_nullInput_returnsNull() {
        assertNull(normalizedOtForUnique(null))
    }

    @Test
    fun normalizedOtForUnique_emptyInput_returnsNull() {
        assertNull(normalizedOtForUnique(""))
    }

    @Test
    fun normalizedOtForUnique_blankInput_returnsNull() {
        assertNull(normalizedOtForUnique("   "))
    }

    @Test
    fun normalizedHistoriaKey_trimsAndUppercases() {
        assertEquals("HO-001", normalizedHistoriaKey("ho-001"))
    }

    @Test
    fun normalizedHistoriaKey_trimsWhitespace() {
        assertEquals("HO-002", normalizedHistoriaKey("  Ho-002  "))
    }

    @Test
    fun normalizedHistoriaKey_nullInput_returnsNull() {
        assertNull(normalizedHistoriaKey(null))
    }

    @Test
    fun normalizedHistoriaKey_emptyInput_returnsNull() {
        assertNull(normalizedHistoriaKey(""))
    }

    @Test
    fun normalizedHistoriaKey_blankInput_returnsNull() {
        assertNull(normalizedHistoriaKey("   "))
    }
}
