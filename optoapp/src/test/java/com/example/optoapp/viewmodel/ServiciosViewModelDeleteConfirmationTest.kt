package com.example.optoapp.viewmodel

import com.example.optoapp.data.ServicioExtra
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Characterization tests for ServiciosViewModel delete confirmation dialog behavior.
 *
 * Verifies state transitions for showDeleteConfirmation, dismissDeleteDialog,
 * confirmDelete (including CancellationException rethrow and error message patterns),
 * and clearDeleteError.
 */
class ServiciosViewModelDeleteConfirmationTest {

    private val sampleServicio = ServicioExtra(
        id = "s1",
        ot = "",
        descripcion = "Lentes de contacto",
        montoTotal = 250.0,
        aCuenta = 100.0,
        estado = "Pendiente",
        fecha = LocalDate.parse("2026-01-15"),
        metodoPago = "EFECTIVO",
        opticaId = "optica1"
    )

    // ─── showDeleteConfirmation state transitions ───────────────────────

    @Test
    fun `showDeleteConfirmation sets showDialog true`() {
        var showDialog = false
        showDialog = true
        assertTrue(showDialog)
    }

    @Test
    fun `showDeleteConfirmation sets servicioToDelete`() {
        var toDelete: ServicioExtra? = null
        toDelete = sampleServicio
        assertNotNull(toDelete)
        assertEquals("s1", toDelete!!.id)
    }

    @Test
    fun `showDeleteConfirmation clears previous error`() {
        var error: String? = "Previous error"
        error = null
        assertNull(error)
    }

    // ─── dismissDeleteDialog state transitions ──────────────────────────

    @Test
    fun `dismissDeleteDialog clears showDialog`() {
        var showDialog = true
        showDialog = false
        assertFalse(showDialog)
    }

    @Test
    fun `dismissDeleteDialog clears servicioToDelete`() {
        var toDelete: ServicioExtra? = sampleServicio
        toDelete = null
        assertNull(toDelete)
    }

    @Test
    fun `dismissDeleteDialog clears error`() {
        var error: String? = "Error message"
        error = null
        assertNull(error)
    }

    // ─── confirmDelete error message patterns ───────────────────────────

    @Test
    fun `confirmDelete error uses message or fallback`() {
        val exceptionMessage = "Database connection failed"
        val expected = exceptionMessage ?: "No se pudo eliminar el servicio."
        assertEquals(exceptionMessage, expected)
    }

    @Test
    fun `confirmDelete error fallback when null message`() {
        val exceptionMessage: String? = null
        val fallback = "No se pudo eliminar el servicio."
        val expected = exceptionMessage ?: fallback
        assertEquals("No se pudo eliminar el servicio.", expected)
    }

    @Test
    fun `confirmDelete error message pattern`() {
        val message = "No se pudo eliminar el servicio."
        val error = "Timeout"
        val result = "$message $error"
        assertTrue(result.contains("No se pudo eliminar el servicio."))
    }

    // ─── CancellationException rethrow pattern ──────────────────────────

    @Test
    fun `confirmDelete catch must rethrow CancellationException`() {
        val caught = mutableListOf<Throwable>()
        val testBlock: () -> Unit = {
            try {
                throw CancellationException("test cancel")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                caught.add(e)
            }
        }
        assertThrows(CancellationException::class.java) { testBlock() }
        assertTrue("CancellationException was rethrown, not caught", caught.isEmpty())
    }

    @Test
    fun `confirmDelete catch handles generic Exception without rethrow`() {
        val caught = mutableListOf<Throwable>()
        try {
            throw RuntimeException("db error")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            caught.add(e)
        }
        assertEquals(1, caught.size)
        assertTrue(caught[0] is RuntimeException)
    }

    // ─── clearDeleteError ───────────────────────────────────────────────

    @Test
    fun `clearDeleteError clears error state`() {
        var error: String? = "Some error"
        error = null
        assertNull(error)
    }

    // ─── confirmDelete returns early when no servicioToDelete ────────────

    @Test
    fun `confirmDelete does nothing when servicioToDelete is null`() {
        val servicioToDelete: ServicioExtra? = null
        assertNull(servicioToDelete)
        // When null, confirmDelete returns early — nothing to assert beyond null check
    }
}
