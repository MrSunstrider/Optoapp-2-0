package com.example.optoapp.sync

import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

/**
 * TDD: RED → GREEN cycle for null-safety in [PostSaveSyncScheduler].
 *
 * RED  (current code):  `!!` on null use case throws NPE → caught by catch block
 *                        → "Error inesperado" is logged. These tests FAIL because
 *                        the error log IS present.
 *
 * GREEN (fixed code):    Safe call + early return logs "useCase no inyectado" warning.
 *                        No "Error inesperado" log → tests PASS.
 *
 * Each test overrides [PostSaveSyncScheduler.ensureSessionForPostSaveSync]
 * to return `true` so execution reaches the use-case call site, and overrides
 * [PostSaveSyncScheduler.scheduleDebounced] to run the block synchronously.
 */
@RunWith(RobolectricTestRunner::class)
class PostSaveSyncSchedulerNullUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val fakeSupabase = createSupabaseClient(
        supabaseUrl = "https://test.supabase.co",
        supabaseKey = "test-key"
    ) { }

    @After
    fun tearDown() {
        ShadowLog.clear()
    }

    private fun createSchedulerWithNullUseCases(): PostSaveSyncScheduler {
        return object : PostSaveSyncScheduler(
            applicationScope = testScope,
            syncGate = SyncGate(),
            supabase = fakeSupabase,
            syncPacientesUseCase = null,
            syncHistorialUseCase = null,
            syncFinanzasUseCase = null,
            syncInventarioUseCase = null,
            syncProveedoresUseCase = null,
            syncOrdenesCompraUseCase = null,
            syncInventarioFisicoUseCase = null,
            syncInventoryKpisUseCase = null
        ) {
            override suspend fun ensureSessionForPostSaveSync(stage: String): Boolean = true

            override fun scheduleDebounced(
                key: String,
                delayMs: Long,
                block: suspend () -> Unit
            ) {
                kotlinx.coroutines.runBlocking { block() }
            }
        }
    }

    @Test
    fun `schedulePacientesSync does not log unexpected error when useCase is null`() = runTest(testDispatcher) {
        val scheduler = createSchedulerWithNullUseCases()
        scheduler.schedulePacientesSync("test-optica")

        val errorLogs = ShadowLog.getLogsForTag("PostSaveSync").filter {
            it.msg.contains("Error inesperado")
        }
        assertTrue(
            "Expected no 'Error inesperado' log; useCase should be null-checked safely",
            errorLogs.isEmpty()
        )
    }

    @Test
    fun `scheduleHistorialSync does not log unexpected error when useCase is null`() = runTest(testDispatcher) {
        val scheduler = createSchedulerWithNullUseCases()
        scheduler.scheduleHistorialSync("test-optica")

        val errorLogs = ShadowLog.getLogsForTag("PostSaveSync").filter {
            it.msg.contains("Error inesperado")
        }
        assertTrue(
            "Expected no 'Error inesperado' log; useCase should be null-checked safely",
            errorLogs.isEmpty()
        )
    }

    @Test
    fun `scheduleFinanzasSync does not log unexpected error when useCase is null`() = runTest(testDispatcher) {
        val scheduler = createSchedulerWithNullUseCases()
        scheduler.scheduleFinanzasSync("test-optica")

        val errorLogs = ShadowLog.getLogsForTag("PostSaveSync").filter {
            it.msg.contains("Error inesperado")
        }
        assertTrue(
            "Expected no 'Error inesperado' log; useCase should be null-checked safely",
            errorLogs.isEmpty()
        )
    }

    @Test
    fun `scheduleInventarioSync does not log unexpected error when useCase is null`() = runTest(testDispatcher) {
        val scheduler = createSchedulerWithNullUseCases()
        scheduler.scheduleInventarioSync("test-optica")

        val errorLogs = ShadowLog.getLogsForTag("PostSaveSync").filter {
            it.msg.contains("Error inesperado")
        }
        assertTrue(
            "Expected no 'Error inesperado' log; useCase should be null-checked safely",
            errorLogs.isEmpty()
        )
    }

    @Test
    fun `scheduleOrdenCompraSync does not log unexpected error when useCase is null`() = runTest(testDispatcher) {
        val scheduler = createSchedulerWithNullUseCases()
        scheduler.scheduleOrdenCompraSync("test-optica")

        val errorLogs = ShadowLog.getLogsForTag("PostSaveSync").filter {
            it.msg.contains("Error inesperado")
        }
        assertTrue(
            "Expected no 'Error inesperado' log; useCase should be null-checked safely",
            errorLogs.isEmpty()
        )
    }

    @Test
    fun `scheduleProveedoresSync does not log unexpected error when useCase is null`() = runTest(testDispatcher) {
        val scheduler = createSchedulerWithNullUseCases()
        scheduler.scheduleProveedoresSync("test-optica")

        val errorLogs = ShadowLog.getLogsForTag("PostSaveSync").filter {
            it.msg.contains("Error inesperado")
        }
        assertTrue(
            "Expected no 'Error inesperado' log; useCase should be null-checked safely",
            errorLogs.isEmpty()
        )
    }

    @Test
    fun `scheduleInventoryKpisSync does not log unexpected error when useCase is null`() = runTest(testDispatcher) {
        val scheduler = createSchedulerWithNullUseCases()
        scheduler.scheduleInventoryKpisSync("test-optica")

        val errorLogs = ShadowLog.getLogsForTag("PostSaveSync").filter {
            it.msg.contains("Error inesperado")
        }
        assertTrue(
            "Expected no 'Error inesperado' log; useCase should be null-checked safely",
            errorLogs.isEmpty()
        )
    }

    @Test
    fun `scheduleInventarioFisicoSync does not log unexpected error when useCase is null`() = runTest(testDispatcher) {
        val scheduler = createSchedulerWithNullUseCases()
        scheduler.scheduleInventarioFisicoSync("test-optica")

        val errorLogs = ShadowLog.getLogsForTag("PostSaveSync").filter {
            it.msg.contains("Error inesperado")
        }
        assertTrue(
            "Expected no 'Error inesperado' log; useCase should be null-checked safely",
            errorLogs.isEmpty()
        )
    }
}
