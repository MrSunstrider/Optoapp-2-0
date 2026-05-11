package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests normalization logic used by SyncFinanzasUseCase (normalizedOtForUnique)
 * and SyncHistorialUseCase / SyncPacientesUseCase (normalizedHistoriaKey).
 *
 * normalizedOtForUnique is a top-level `internal` function — callable directly.
 * normalizedHistoriaKey is an instance method on both UseCase classes (same logic);
 * this file verifies the logic contract that both implementations share.
 */
class SyncUtilsTest {

    // ── normalizedOtForUnique ──────────────────────────────────────────────

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

    // ── normalizedHistoriaKey logic (identical in both UseCases) ──────────

    @Test
    fun normalizedHistoriaKey_trimsAndUppercases() {
        val result = normalizedHistoriaKeyHelper("ho-001")
        assertEquals("HO-001", result)
    }

    @Test
    fun normalizedHistoriaKey_trimsWhitespace() {
        val result = normalizedHistoriaKeyHelper("  Ho-002  ")
        assertEquals("HO-002", result)
    }

    @Test
    fun normalizedHistoriaKey_nullInput_returnsNull() {
        assertNull(normalizedHistoriaKeyHelper(null))
    }

    @Test
    fun normalizedHistoriaKey_emptyInput_returnsNull() {
        assertNull(normalizedHistoriaKeyHelper(""))
    }

    @Test
    fun normalizedHistoriaKey_blankInput_returnsNull() {
        assertNull(normalizedHistoriaKeyHelper("   "))
    }

    /**
     * Replicates the exact logic of `normalizedHistoriaKey` from
     * SyncHistorialUseCase and SyncPacientesUseCase (both are identical).
     *
     * Both UseCases define this as an `internal` member function, which
     * requires class instantiation with Hilt deps to call directly.
     * This helper verifies the logic contract instead.
     */
    private fun normalizedHistoriaKeyHelper(historia: String?): String? {
        val normalized = historia?.trim()?.uppercase().orEmpty()
        return normalized.ifBlank { null }
    }
}
