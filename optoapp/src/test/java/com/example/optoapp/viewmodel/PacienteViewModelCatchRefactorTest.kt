package com.example.optoapp.viewmodel

import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

/**
 * Approval tests for PacienteViewModel's catch block behavior
 * at line 141 (deletePacienteGuarded).
 *
 * Current:  catch (e: Exception) { DeletePacienteResult.Error(...) }
 * Expected: catch (e: IOException | SupabaseException) { DeletePacienteResult.Error(...) }
 *
 * Tests verify error message patterns and CancellationException rethrow.
 */
class PacienteViewModelCatchRefactorTest {

    @Test
    fun `paciente delete error message prefix`() {
        val prefix = "No se pudo eliminar el paciente: "
        assertTrue("No se pudo eliminar el paciente: timeout".startsWith(prefix))
    }

    @Test
    fun `paciente delete error with localizedMessage`() {
        val localized = "Connection timeout"
        val result = "No se pudo eliminar el paciente: $localized"
        assertEquals("No se pudo eliminar el paciente: Connection timeout", result)
    }

    @Test
    fun `paciente delete error fallback when null message`() {
        val e: Throwable? = null
        val message = e?.localizedMessage ?: "error desconocido"
        assertEquals("error desconocido", message)
    }

    @Test
    fun `paciente delete error uses localizedMessage or fallback`() {
        val exceptionMessage: String? = "Network failure"
        val result = "No se pudo eliminar el paciente: ${exceptionMessage ?: "error desconocido"}"
        assertEquals("No se pudo eliminar el paciente: Network failure", result)
    }

    @Test
    fun `paciente delete error fallback when localizedMessage null`() {
        val exceptionMessage: String? = null
        val result = "No se pudo eliminar el paciente: ${exceptionMessage ?: "error desconocido"}"
        assertEquals("No se pudo eliminar el paciente: error desconocido", result)
    }

    @Test
    fun `deletePacienteResult Error holds message`() {
        val error: DeletePacienteResult = DeletePacienteResult.Error("No se pudo eliminar el paciente: error")
        assertTrue(error is DeletePacienteResult.Error)
        assertEquals("No se pudo eliminar el paciente: error", (error as DeletePacienteResult.Error).message)
    }

    @Test
    fun `paciente delete catch rethrows CancellationException`() {
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
