package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.venta.Venta
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DispensacionStockHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests that saveDispensacion() upserts a Venta locally
 * BEFORE scheduleFinanzasSync, following offline-first design.
 */
class DispensacionViewModelVentaTest {

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var postSaveSyncScheduler: PostSaveSyncScheduler
    private lateinit var stockHelper: DispensacionStockHelper

    private val opticaIdFlow = MutableStateFlow("optica-test")
    private val testDate = LocalDate.of(2026, 7, 4)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0

        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        sessionManager = mockk()
        postSaveSyncScheduler = mockk(relaxed = true)
        stockHelper = mockk(relaxed = true)

        every { sessionManager.opticaId } returns opticaIdFlow

        // Happy-path mocks for saveDispensacion
        coEvery { repository.suggestNextOt(any(), any()) } returns "OT-2026-0001"
        coEvery { repository.getDispensacionItemsByDispensacion(any()) } returns emptyList()
        coEvery { stockHelper.adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any()) } returns Result.success(0)
        coEvery { repository.getPagosByDispensacion(any()) } returns flowOf(emptyList())
        // Mock for init block
        every { repository.getMonturasByOptica(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveDispensacion upserts Venta with v_disp prefix before scheduleFinanzasSync`() = runTest {
        val ventaSlot = slot<Venta>()

        val vm = DispensacionViewModel(repository, sessionManager, postSaveSyncScheduler, stockHelper)
        // Let init block coroutine settle
        testDispatcher.scheduler.advanceUntilIdle()

        // Set up valid UI state
        vm.updateUiState { s ->
            s.copy(
                ot = "OT-2026-0001",
                montoTotal = "150.0",
                fecha = testDate,
                pacienteNombre = "Test Paciente",
                items = listOf(DispensacionItemUi(
                    tipoLente = "Monofocal",
                    distanciaLente = "Lejos",
                    altura = ""
                ))
            )
        }

        var completeCalled = false
        vm.saveDispensacion("paciente-1", null) { completeCalled = true }
        // Advance so the viewModelScope coroutine runs
        testDispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            repository.upsertVenta(capture(ventaSlot))
            postSaveSyncScheduler.scheduleFinanzasSync("optica-test")
        }

        val captured = ventaSlot.captured
        assertTrue(captured.id.startsWith("v_disp_"))
        assertEquals("dispensacion", captured.origen)
        assertEquals("optica-test", captured.opticaId)
        assertEquals(150.0, captured.montoTotal, 0.001)
        assertEquals("Pendiente", captured.estado)
        assertEquals("paciente-1", captured.pacienteId)
        assertEquals(testDate, captured.fecha)
        assertTrue(completeCalled)
    }

    @Test
    fun `saveDispensacion Venta includes fechaEntrega when set`() = runTest {
        val ventaSlot = slot<Venta>()
        val fechaEntrega = LocalDate.of(2026, 7, 15)

        val vm = DispensacionViewModel(repository, sessionManager, postSaveSyncScheduler, stockHelper)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.updateUiState { s ->
            s.copy(
                ot = "OT-2026-0002",
                montoTotal = "200.0",
                fecha = testDate,
                fechaEntrega = fechaEntrega,
                estadoEntrega = "Entregado",
                pacienteNombre = "Test",
                items = listOf(DispensacionItemUi(
                    tipoLente = "Monofocal",
                    distanciaLente = "Lejos",
                    altura = ""
                ))
            )
        }

        var completeCalled = false
        vm.saveDispensacion("paciente-2", null) { completeCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.upsertVenta(capture(ventaSlot)) }

        val captured = ventaSlot.captured
        assertEquals(fechaEntrega, captured.fechaEntrega)
        assertEquals("Entregado", captured.estado)
        assertTrue(completeCalled)
    }
}
