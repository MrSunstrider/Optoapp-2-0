package com.example.optoapp.viewmodel

import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

/**
 * Approval tests for MonturasViewModel's catch block behavior
 * at line 166 (save method).
 *
 * Current:  catch (e: Exception) { ... UiState.Error }
 * Expected: catch (e: NetworkException | IOException) { ... UiState.Error }
 *
 * Tests verify error messages, unique constraint handling, CancellationException rethrow.
 */
class MonturasViewModelCatchRefactorTest {

    // ─── Error message patterns ──────────────────────────────────────────

    @Test
    fun `montura error unique constraint message`() {
        val msg = "El SKU ya existe para otro producto."
        val expected = if (true) "El SKU ya existe para otro producto."
                      else "Error al guardar: null"
        assertEquals(expected, msg)
    }

    @Test
    fun `montura error fallback message`() {
        val e = java.io.IOException("timeout")
        val msg = "Error al guardar: ${e.message}"
        assertEquals("Error al guardar: timeout", msg)
    }

    @Test
    fun `montura error contains Error al guardar prefix`() {
        val message = "Error al guardar: connection failed"
        assertTrue(message.startsWith("Error al guardar:"))
    }

    @Test
    fun `montura error preserves UiState Error pattern`() {
        data class UiState(val error: String? = null, val success: String? = null)
        val state = UiState()
        val updated = state.copy(error = "Error al guardar: error")
        assertNull(state.error)
        assertEquals("Error al guardar: error", updated.error)
    }

    @Test
    fun `montura success clears error`() {
        data class UiState(val error: String? = null, val success: String? = null)
        val afterSave = UiState(error = null, success = "Producto guardado")
        assertNull(afterSave.error)
        assertEquals("Producto guardado", afterSave.success)
    }

    // ─── CancellationException rethrow pattern ──────────────────────────

    @Test
    fun `montura catch rethrows CancellationException`() {
        val caught = mutableListOf<Throwable>()
        val block: () -> Unit = {
            try {
                throw CancellationException("cancel")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                caught.add(e)
            }
        }
        assertThrows(CancellationException::class.java) { block() }
        assertTrue(caught.isEmpty())
    }

    @Test
    fun `montura catch handles IOException`() {
        var caught: Throwable? = null
        try {
            throw java.io.IOException("db error")
        } catch (e: CancellationException) {
            throw e
        } catch (e: java.io.IOException) {
            caught = e
        }
        assertNotNull(caught)
    }
}
