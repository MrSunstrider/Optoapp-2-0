package com.example.optoapp.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for PacienteViewModel's data types and public API contracts.
 *
 * The ViewModel depends on OptoRepository, SessionManager, PostSaveSyncScheduler,
 * and SupabaseClient — all concrete (final) classes. Full ViewModel tests
 * require either making these classes open or adding a mocking library.
 *
 * These tests cover the sealed class behavior, data class contracts, and
 * state type contracts.
 */
class PacienteViewModelTest {

    // ─── DeletePacienteResult sealed class ────────────────────────────────

    @Test
    fun deletePacienteResultSuccess_holdsRemainingDeletes() {
        val result = DeletePacienteResult.Success(remainingDeletesToday = 7)
        assertEquals(7, (result as DeletePacienteResult.Success).remainingDeletesToday)
    }

    @Test
    fun deletePacienteResultError_holdsMessage() {
        val result = DeletePacienteResult.Error("No tienes permisos")
        assertEquals("No tienes permisos", (result as DeletePacienteResult.Error).message)
    }

    @Test
    fun deletePacienteResultSuccess_isNotError() {
        val result = DeletePacienteResult.Success(remainingDeletesToday = 5)
        assertFalse(result is DeletePacienteResult.Error)
    }

    @Test
    fun deletePacienteResultError_isNotSuccess() {
        val result = DeletePacienteResult.Error("Error")
        assertFalse(result is DeletePacienteResult.Success)
    }

    @Test
    fun deletePacienteResultSuccess_remainingDeletesZero() {
        val result = DeletePacienteResult.Success(remainingDeletesToday = 0)
        assertEquals(0, (result as DeletePacienteResult.Success).remainingDeletesToday)
    }

    // ─── Data class contracts ─────────────────────────────────────────────

    @Test
    fun deletePacienteResultSuccess_dataClass() {
        val r1 = DeletePacienteResult.Success(3)
        val r2 = DeletePacienteResult.Success(3)
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun deletePacienteResultError_dataClass() {
        val r1 = DeletePacienteResult.Error("msg")
        val r2 = DeletePacienteResult.Error("msg")
        assertEquals(r1, r2)
    }

    @Test
    fun deletePacienteResultError_differentMessages_notEqual() {
        val r1 = DeletePacienteResult.Error("msg1")
        val r2 = DeletePacienteResult.Error("msg2")
        assertNotEquals(r1, r2)
    }
}
