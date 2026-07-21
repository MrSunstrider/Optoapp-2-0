package com.example.optoapp.domain.sync

import com.example.optoapp.data.Resource
import com.example.optoapp.domain.SyncPacientesUseCase
import com.example.optoapp.sync.SyncGate
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test

class SyncOrchestratorTest {

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        SyncOrchestrator.syncTimeoutMs = 100L
    }

    @After
    fun tearDown() {
        SyncOrchestrator.syncTimeoutMs = 300_000L
        unmockkAll()
    }

    @Test
    fun `executeModules returns true when mutex is locked and timeout fires`() = runBlocking {
        val syncGate = SyncGate()

        // Lock the mutex from background
        launch {
            syncGate.mutex.withLock {
                delay(10_000L)
            }
        }
        delay(10L) // let lock acquire

        val pacientesUseCase = mockk<SyncPacientesUseCase>(relaxed = true)
        coEvery { pacientesUseCase(any(), any(), any()) } returns Resource.Success(mockk())

        val result = SyncOrchestrator(
            syncPacientesUseCase = pacientesUseCase,
            syncHistorialUseCase = mockk(relaxed = true),
            syncFinanzasUseCase = mockk(relaxed = true),
            syncInventarioUseCase = mockk(relaxed = true),
            syncProveedoresUseCase = mockk(relaxed = true),
            syncOrdenesCompraUseCase = mockk(relaxed = true),
            syncInventarioFisicoUseCase = mockk(relaxed = true),
            syncInventoryKpisUseCase = mockk(relaxed = true),
            syncGate = syncGate,
        ).executeModules("optica-test", false)

        assertEquals(
            "executeModules should return true (hasErrors) when timeout fires",
            true, result,
        )
    }
}
