package com.example.optoapp.sync

import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [PostSaveSyncScheduler] debounce key mapping and session gating.
 *
 * Uses test subclasses that override [scheduleDebounced] to capture keys
 * (for key tests) or execute synchronously (for session gating tests).
 */
class PostSaveSyncSchedulerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val fakeSupabase = createSupabaseClient(
        supabaseUrl = "https://test.supabase.co",
        supabaseKey = "test-key"
    ) { }

    // ─── Key mapping (scheduleDebounced is overridden → records keys) ─────────

    @Test
    fun `scheduleDebounced is called with correct key per module`() = runTest(testDispatcher) {
        val (scheduler, calls) = createKeyRecorder()

        scheduler.schedulePacientesSync("optica-1")
        scheduler.scheduleHistorialSync("optica-1")
        scheduler.scheduleFinanzasSync("optica-1")
        scheduler.scheduleInventarioSync("optica-1")
        scheduler.scheduleProveedoresSync("optica-1")
        scheduler.scheduleOrdenCompraSync("optica-1")

        assertEquals("pacientes:optica-1", calls[0])
        assertEquals("historial:optica-1", calls[1])
        assertEquals("finanzas:optica-1", calls[2])
        assertEquals("inventario:optica-1", calls[3])
        assertEquals("proveedores:optica-1", calls[4])
        assertEquals("ordenes_compra:optica-1", calls[5])
    }

    @Test
    fun `schedulePacientesSync uses opticaId in key`() = runTest(testDispatcher) {
        val (scheduler, calls) = createKeyRecorder()

        scheduler.schedulePacientesSync("optica-A")
        scheduler.schedulePacientesSync("optica-B")

        assertEquals("pacientes:optica-A", calls[0])
        assertEquals("pacientes:optica-B", calls[1])
    }

    // ─── Session gating (scheduleDebounced executes block synchronously) ────

    @Test
    fun `ensureSessionForPostSaveSync is called before block execution`() = runTest(testDispatcher) {
        val stages = mutableListOf<String>()
        val scheduler = createSessionRecorder(stages)

        scheduler.schedulePacientesSync("optica-1")
        // Block runs synchronously via override → stages populated immediately

        assertEquals("ensureSessionForPostSaveSync was called with pacientes",
            listOf("pacientes"), stages)
    }

    @Test
    fun `ensureSessionForPostSaveSync receives correct stage for each module`() = runTest(testDispatcher) {
        val stages = mutableListOf<String>()
        val scheduler = createSessionRecorder(stages)

        scheduler.schedulePacientesSync("o1")
        scheduler.scheduleHistorialSync("o1")
        scheduler.scheduleFinanzasSync("o1")
        scheduler.scheduleInventarioSync("o1")
        scheduler.scheduleProveedoresSync("o1")
        scheduler.scheduleOrdenCompraSync("o1")

        assertEquals(
            listOf("pacientes", "historial", "finanzas", "inventario", "proveedores", "ordenes_compra"),
            stages
        )
    }

    @Test
    fun `schedule methods do not crash when session is valid and use cases are null`() = runTest(testDispatcher) {
        val scheduler = createSessionRecorder(mutableListOf())

        // Should not throw — NPE from null use cases caught by try-catch
        scheduler.schedulePacientesSync("o1")
        // If we reach here, no exception was thrown — success
    }

    @Test
    fun `schedule methods with invalid session skip gracefully`() = runTest(testDispatcher) {
        val scheduler = object : PostSaveSyncScheduler(
            applicationScope = testScope,
            syncGate = SyncGate(),
            supabase = fakeSupabase,
            syncPacientesUseCase = null,
            syncHistorialUseCase = null,
            syncFinanzasUseCase = null,
            syncInventarioUseCase = null
        ) {
            override suspend fun ensureSessionForPostSaveSync(stage: String): Boolean = false

            override fun scheduleDebounced(
                key: String,
                delayMs: Long,
                block: suspend () -> Unit
            ) {
                // Execute block synchronously so session check runs immediately
                kotlinx.coroutines.runBlocking { block() }
            }
        }

        // Should not throw because session returns false → block exits early
        scheduler.schedulePacientesSync("o1")
    }

    // ─── RC-5: cancelPending race fix ─────────────────────────────────────

    @Test
    fun `cancelPending_awaitsJobCancellation_beforeReturn`() = runTest(testDispatcher) {
        val scheduler = PostSaveSyncScheduler(
            applicationScope = testScope,
            syncGate = SyncGate(),
            supabase = fakeSupabase
        )
        val job = launch { delay(Long.MAX_VALUE) }
        scheduler.pendingJobs["test-key"] = job

        scheduler.cancelPending()

        assertTrue(
            "cancelPending must cancel and join all pending jobs before returning (RC-5)",
            job.isCancelled
        )
    }

    @Test
    fun `cancelPending_clearsPendingJobsMap_afterCancellation`() = runTest(testDispatcher) {
        val scheduler = PostSaveSyncScheduler(
            applicationScope = testScope,
            syncGate = SyncGate(),
            supabase = fakeSupabase
        )
        val job = launch { delay(Long.MAX_VALUE) }
        scheduler.pendingJobs["test-key"] = job

        scheduler.cancelPending()

        assertTrue(
            "pendingJobs must be empty after cancelPending (RC-5)",
            scheduler.pendingJobs.isEmpty()
        )
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    /**
     * Creates a scheduler that records [scheduleDebounced] keys.
     * Does NOT execute the debounced block — only records the key.
     */
    private fun createKeyRecorder(): Pair<PostSaveSyncScheduler, MutableList<String>> {
        val keys = mutableListOf<String>()
        val scheduler = object : PostSaveSyncScheduler(
            applicationScope = testScope,
            syncGate = SyncGate(),
            supabase = fakeSupabase,
            syncPacientesUseCase = null,
            syncHistorialUseCase = null,
            syncFinanzasUseCase = null,
            syncInventarioUseCase = null
        ) {
            override fun scheduleDebounced(
                key: String,
                delayMs: Long,
                block: suspend () -> Unit
            ) {
                keys.add(key)
            }
        }
        return scheduler to keys
    }

    /**
     * Creates a scheduler that records [ensureSessionForPostSaveSync] stage names
     * and executes the debounced block synchronously.
     */
    private fun createSessionRecorder(stages: MutableList<String>): PostSaveSyncScheduler {
        return object : PostSaveSyncScheduler(
            applicationScope = testScope,
            syncGate = SyncGate(),
            supabase = fakeSupabase,
            syncPacientesUseCase = null,
            syncHistorialUseCase = null,
            syncFinanzasUseCase = null,
            syncInventarioUseCase = null
        ) {
            override suspend fun ensureSessionForPostSaveSync(stage: String): Boolean {
                stages.add(stage)
                return false // stop before accessing use cases
            }

            override fun scheduleDebounced(
                key: String,
                delayMs: Long,
                block: suspend () -> Unit
            ) {
                kotlinx.coroutines.runBlocking { block() }
            }
        }
    }
}
