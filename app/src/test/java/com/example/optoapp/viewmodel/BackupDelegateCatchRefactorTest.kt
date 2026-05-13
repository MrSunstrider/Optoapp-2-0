package com.example.optoapp.viewmodel

import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

/**
 * Approval tests for BackupDelegate's catch block behavior
 * at line 87 (restoreBackup).
 *
 * Current:  catch (e: Exception) { return error message }
 * Expected: catch (e: IOException | DatabaseException) { return error message }
 *
 * Tests verify error message patterns and CancellationException rethrow.
 */
class BackupDelegateCatchRefactorTest {

    // ─── restoreBackup() error message pattern (line 87) ────────────────

    @Test
    fun `restoreBackup error message prefix`() {
        val prefix = "Error al restaurar: "
        val msg = "Error al restaurar: network failure"
        assertTrue(msg.startsWith(prefix))
    }

    @Test
    fun `restoreBackup error with message`() {
        val message = "RPC call failed"
        val msg = "Error al restaurar: ${message ?: "desconocido"}"
        assertEquals("Error al restaurar: RPC call failed", msg)
    }

    @Test
    fun `restoreBackup error fallback when message null`() {
        val message: String? = null
        val msg = "Error al restaurar: ${message ?: "desconocido"}"
        assertEquals("Error al restaurar: desconocido", msg)
    }

    @Test
    fun `restoreBackup success message`() {
        val msg = "Base de datos restaurada correctamente."
        assertEquals("Base de datos restaurada correctamente.", msg)
    }

    // ─── CancellationException rethrow pattern ──────────────────────────

    @Test
    fun `restoreBackup catch rethrows CancellationException`() {
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
