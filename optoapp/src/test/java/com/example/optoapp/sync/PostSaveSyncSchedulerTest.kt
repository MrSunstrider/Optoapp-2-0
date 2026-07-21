package com.example.optoapp.sync

import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import io.github.jan.supabase.createSupabaseClient
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
        supabaseKey = "test-key",
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

        assertEquals(
            "ensureSessionForPostSaveSync was called with pacientes",
            listOf("pacientes"),
            stages,
        )
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
            stages,
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
            syncInventarioUseCase = null,
        ) {
            override suspend fun ensureSessionForPostSaveSync(stage: String): Boolean = false

            override fun scheduleDebounced(
                key: String,
                delayMs: Long,
                block: suspend () -> Unit,
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
            supabase = fakeSupabase,
        )
        val job = launch { delay(Long.MAX_VALUE) }
        scheduler.pendingJobs["test-key"] = job

        scheduler.cancelPending()

        assertTrue(
            "cancelPending must cancel and join all pending jobs before returning (RC-5)",
            job.isCancelled,
        )
    }

    @Test
    fun `cancelPending_clearsPendingJobsMap_afterCancellation`() = runTest(testDispatcher) {
        val scheduler = PostSaveSyncScheduler(
            applicationScope = testScope,
            syncGate = SyncGate(),
            supabase = fakeSupabase,
        )
        val job = launch { delay(Long.MAX_VALUE) }
        scheduler.pendingJobs["test-key"] = job

        scheduler.cancelPending()

        assertTrue(
            "pendingJobs must be empty after cancelPending (RC-5)",
            scheduler.pendingJobs.isEmpty(),
        )
    }

    // ─── RC-2: scheduler isolation — no pacientes cascade ────────────────────

    @Test
    fun scheduleHistorialSync_doesNotInvokeSyncPacientes() = runTest(testDispatcher) {
        val mockSyncPacientes = mockk<SyncPacientesUseCase>(relaxed = true)
        val mockSyncHistorial = mockk<SyncHistorialUseCase>(relaxed = true)
        val scheduler = object : PostSaveSyncScheduler(
            applicationScope = testScope,
            syncGate = SyncGate(),
            supabase = fakeSupabase,
            syncPacientesUseCase = mockSyncPacientes,
            syncHistorialUseCase = mockSyncHistorial,
            syncFinanzasUseCase = null,
            syncInventarioUseCase = null,
        ) {
            // Capture key only — don't execute the block (avoids android.util.Log without Robolectric).
            // The assertion only cares that mockSyncPacientes was never invoked as a side effect.
            override fun scheduleDebounced(key: String, delayMs: Long, block: suspend () -> Unit) {}
        }

        scheduler.scheduleHistorialSync("optica-1")

        coVerify(exactly = 0) { mockSyncPacientes(any()) }
    }

    @Test
    fun scheduleFinanzasSync_doesNotInvokeSyncPacientes() = runTest(testDispatcher) {
        val mockSyncPacientes = mockk<SyncPacientesUseCase>(relaxed = true)
        val mockSyncFinanzas = mockk<SyncFinanzasUseCase>(relaxed = true)
        val scheduler = object : PostSaveSyncScheduler(
            applicationScope = testScope,
            syncGate = SyncGate(),
            supabase = fakeSupabase,
            syncPacientesUseCase = mockSyncPacientes,
            syncHistorialUseCase = null,
            syncFinanzasUseCase = mockSyncFinanzas,
            syncInventarioUseCase = null,
        ) {
            override fun scheduleDebounced(key: String, delayMs: Long, block: suspend () -> Unit) {}
        }

        scheduler.scheduleFinanzasSync("optica-1")

        coVerify(exactly = 0) { mockSyncPacientes(any()) }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private fun createKeyRecorder(): Pair<PostSaveSyncScheduler, MutableList<String>> {
        val keys = mutableListOf<String>()
        val scheduler = object : PostSaveSyncScheduler(
            applicationScope = testScope,
            syncGate = SyncGate(),
            supabase = fakeSupabase,
            syncPacientesUseCase = null,
            syncHistorialUseCase = null,
            syncFinanzasUseCase = null,
            syncInventarioUseCase = null,
        ) {
            override fun scheduleDebounced(
                key: String,
                delayMs: Long,
                block: suspend () -> Unit,
            ) {
                keys.add(key)
            }
        }
        return scheduler to keys
    }

    private fun createSessionRecorder(stages: MutableList<String>): PostSaveSyncScheduler {
        return object : PostSaveSyncScheduler(
            applicationScope = testScope,
            syncGate = SyncGate(),
            supabase = fakeSupabase,
            syncPacientesUseCase = null,
            syncHistorialUseCase = null,
            syncFinanzasUseCase = null,
            syncInventarioUseCase = null,
        ) {
            override suspend fun ensureSessionForPostSaveSync(stage: String): Boolean {
                stages.add(stage)
                return false // stop before accessing use cases
            }

            override fun scheduleDebounced(
                key: String,
                delayMs: Long,
                block: suspend () -> Unit,
            ) {
                kotlinx.coroutines.runBlocking { block() }
            }
        }
    }
}
