package com.example.optoapp.domain.sync

import com.example.optoapp.data.Resource
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncInventarioFisicoUseCase
import com.example.optoapp.domain.SyncInventarioUseCase
import com.example.optoapp.domain.SyncInventoryKpisUseCase
import com.example.optoapp.domain.SyncOrdenesCompraUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import com.example.optoapp.domain.SyncProveedoresUseCase
import com.example.optoapp.sync.SyncGate
import com.example.optoapp.util.BackgroundErrorCollector
import io.mockk.coEvery
import io.mockk.coVerify
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
        SyncOrchestrator.syncTimeoutMs = 100_000L
    }

    @After
    fun tearDown() {
        SyncOrchestrator.syncTimeoutMs = 300_000L
        unmockkAll()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> success(): Resource<T> = Resource.Success(mockk<Any>()) as Resource<T>

    @Test
    fun `executeModules returns true when mutex is locked and timeout fires`() = runBlocking {
        val syncGate = SyncGate()

        launch {
            syncGate.mutex.withLock {
                delay(10_000L)
            }
        }
        delay(10L)

        val pacientesUseCase = mockk<SyncPacientesUseCase>(relaxed = true)
        coEvery { pacientesUseCase(any(), any(), any()) } returns success()
        val bgErrorCollector = mockk<BackgroundErrorCollector>(relaxed = true)

        // WHY: Mutex held for 10s — timeout must fire before mutex is released to test the timeout path
        SyncOrchestrator.syncTimeoutMs = 50L

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
            bgErrorCollector = bgErrorCollector,
        ).executeModules("optica-test", false)

        assertEquals(true, result)
    }

    @Test
    fun `executeModules records BEC for single module error`() = runBlocking {
        val bgErrorCollector = mockk<BackgroundErrorCollector>(relaxed = true)
        val pacientesUseCase = mockk<SyncPacientesUseCase>(relaxed = true)
        coEvery { pacientesUseCase(any(), any(), any()) } returns Resource.Error("timeout")

        val historial = mockk<SyncHistorialUseCase>(relaxed = true)
        coEvery { historial(any(), any(), any()) } returns success()
        val finanzas = mockk<SyncFinanzasUseCase>(relaxed = true)
        coEvery { finanzas(any(), any(), any()) } returns success()
        val inventario = mockk<SyncInventarioUseCase>(relaxed = true)
        coEvery { inventario(any(), any(), any()) } returns success()
        val proveedores = mockk<SyncProveedoresUseCase>(relaxed = true)
        coEvery { proveedores(any(), any(), any()) } returns success()
        val ordenesCompra = mockk<SyncOrdenesCompraUseCase>(relaxed = true)
        coEvery { ordenesCompra(any(), any(), any()) } returns success()
        val inventarioFisico = mockk<SyncInventarioFisicoUseCase>(relaxed = true)
        coEvery { inventarioFisico(any(), any(), any()) } returns success()
        val inventoryKpis = mockk<SyncInventoryKpisUseCase>(relaxed = true)
        coEvery { inventoryKpis(any()) } returns success()

        SyncOrchestrator(
            syncPacientesUseCase = pacientesUseCase,
            syncHistorialUseCase = historial,
            syncFinanzasUseCase = finanzas,
            syncInventarioUseCase = inventario,
            syncProveedoresUseCase = proveedores,
            syncOrdenesCompraUseCase = ordenesCompra,
            syncInventarioFisicoUseCase = inventarioFisico,
            syncInventoryKpisUseCase = inventoryKpis,
            syncGate = SyncGate(),
            bgErrorCollector = bgErrorCollector,
        ).executeModules("optica-test", false)

        coVerify(exactly = 1) { bgErrorCollector.record("sync:pacientes", "timeout") }
    }

    @Test
    fun `executeModules records BEC for each erroring module`() = runBlocking {
        val bgErrorCollector = mockk<BackgroundErrorCollector>(relaxed = true)
        val pacientesUseCase = mockk<SyncPacientesUseCase>(relaxed = true)
        coEvery { pacientesUseCase(any(), any(), any()) } returns Resource.Error("timeout")
        val finanzasUseCase = mockk<SyncFinanzasUseCase>(relaxed = true)
        coEvery { finanzasUseCase(any(), any(), any()) } returns Resource.Error("network")

        val historial = mockk<SyncHistorialUseCase>(relaxed = true)
        coEvery { historial(any(), any(), any()) } returns success()
        val inventario = mockk<SyncInventarioUseCase>(relaxed = true)
        coEvery { inventario(any(), any(), any()) } returns success()
        val proveedores = mockk<SyncProveedoresUseCase>(relaxed = true)
        coEvery { proveedores(any(), any(), any()) } returns success()
        val ordenesCompra = mockk<SyncOrdenesCompraUseCase>(relaxed = true)
        coEvery { ordenesCompra(any(), any(), any()) } returns success()
        val inventarioFisico = mockk<SyncInventarioFisicoUseCase>(relaxed = true)
        coEvery { inventarioFisico(any(), any(), any()) } returns success()
        val inventoryKpis = mockk<SyncInventoryKpisUseCase>(relaxed = true)
        coEvery { inventoryKpis(any()) } returns success()

        SyncOrchestrator(
            syncPacientesUseCase = pacientesUseCase,
            syncHistorialUseCase = historial,
            syncFinanzasUseCase = finanzasUseCase,
            syncInventarioUseCase = inventario,
            syncProveedoresUseCase = proveedores,
            syncOrdenesCompraUseCase = ordenesCompra,
            syncInventarioFisicoUseCase = inventarioFisico,
            syncInventoryKpisUseCase = inventoryKpis,
            syncGate = SyncGate(),
            bgErrorCollector = bgErrorCollector,
        ).executeModules("optica-test", false)

        coVerify(exactly = 1) { bgErrorCollector.record("sync:pacientes", "timeout") }
        coVerify(exactly = 1) { bgErrorCollector.record("sync:finanzas", "network") }
    }

    @Test
    fun `executeSilentModules records BEC on module error`() = runBlocking {
        val bgErrorCollector = mockk<BackgroundErrorCollector>(relaxed = true)
        val onResult: suspend (String, Resource<*>) -> Unit = { _, _ -> }

        val finanzasUseCase = mockk<SyncFinanzasUseCase>(relaxed = true)
        coEvery { finanzasUseCase(any(), any(), any()) } returns Resource.Error("timeout")

        val pacientes = mockk<SyncPacientesUseCase>(relaxed = true)
        coEvery { pacientes(any(), any(), any()) } returns success()
        val historial = mockk<SyncHistorialUseCase>(relaxed = true)
        coEvery { historial(any(), any(), any()) } returns success()
        val inventario = mockk<SyncInventarioUseCase>(relaxed = true)
        coEvery { inventario(any(), any(), any()) } returns success()
        val proveedores = mockk<SyncProveedoresUseCase>(relaxed = true)
        coEvery { proveedores(any(), any(), any()) } returns success()
        val ordenesCompra = mockk<SyncOrdenesCompraUseCase>(relaxed = true)
        coEvery { ordenesCompra(any(), any(), any()) } returns success()
        val inventarioFisico = mockk<SyncInventarioFisicoUseCase>(relaxed = true)
        coEvery { inventarioFisico(any(), any(), any()) } returns success()
        val inventoryKpis = mockk<SyncInventoryKpisUseCase>(relaxed = true)
        coEvery { inventoryKpis(any()) } returns success()

        SyncOrchestrator(
            syncPacientesUseCase = pacientes,
            syncHistorialUseCase = historial,
            syncFinanzasUseCase = finanzasUseCase,
            syncInventarioUseCase = inventario,
            syncProveedoresUseCase = proveedores,
            syncOrdenesCompraUseCase = ordenesCompra,
            syncInventarioFisicoUseCase = inventarioFisico,
            syncInventoryKpisUseCase = inventoryKpis,
            syncGate = SyncGate(),
            bgErrorCollector = bgErrorCollector,
        ).executeSilentModules("optica-test", onResult)

        coVerify(exactly = 1) { bgErrorCollector.record("sync:finanzas", "timeout") }
    }
}
