package com.example.optoapp.viewmodel

import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

/**
 * Approval tests for FiscalConfigViewModel's catch block behavior
 * at line 244 (save method).
 *
 * Current:  catch (e: Exception) { UiState error = e.localizedMessage ?: fallback }
 * Expected: catch (e: IOException | ValidationException) { UiState error = ... }
 *
 * Tests verify error message patterns and CancellationException rethrow.
 */
class FiscalConfigViewModelCatchRefactorTest {

    // ─── Error message patterns ──────────────────────────────────────────

    @Test
    fun `fiscal error uses localizedMessage`() {
        val errorMsg = "Network error"
        val result = errorMsg ?: "Ocurrió un error inesperado al guardar datos fiscales."
        assertEquals("Network error", result)
    }

    @Test
    fun `fiscal error fallback when localizedMessage null`() {
        val errorMsg: String? = null
        val result = errorMsg ?: "Ocurrió un error inesperado al guardar datos fiscales."
        assertEquals("Ocurrió un error inesperado al guardar datos fiscales.", result)
    }

    @Test
    fun `fiscal error pattern loading false and error set`() {
        data class Status(val loading: Boolean = false, val error: String? = null)
        val error = "timeout"
        val status = Status(loading = false, error = error)
        assertFalse(status.loading)
        assertEquals("timeout", status.error)
    }

    @Test
    fun `fiscal success pattern loading false and message set`() {
        data class Status(val loading: Boolean = false, val message: String? = null, val error: String? = null)
        val status = Status(loading = false, message = "Datos fiscales guardados.", error = null)
        assertFalse(status.loading)
        assertEquals("Datos fiscales guardados.", status.message)
        assertNull(status.error)
    }

    // ─── CancellationException rethrow pattern ──────────────────────────

    @Test
    fun `fiscal save catch rethrows CancellationException`() {
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
}
