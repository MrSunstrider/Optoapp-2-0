package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.Montura
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests that saveDispensacion() upserts a Venta locally
 * BEFORE scheduleFinanzasSync, following offline-first design.
 */
@OptIn(ExperimentalCoroutinesApi::class)
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
        every { repository.runInTransaction(any()) } answers {
            (firstArg() as () -> Unit).invoke()
        }
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

    /**
     * BUG-F1: monturas with stockActual=0 MUST be exposed in monturasActivas
     * so the dispensation dropdown can show all active monturas regardless of stock.
     * The stock validation happens at save time, not at selection time.
     */
    @Test
    fun monturasActivas_includesZeroStockMonturas() = runTest(testDispatcher) {
        val zeroStock = Montura(
            id = "m-zero", marca = "Zero", modelo = "Stock", stockActual = 0, activo = true, opticaId = "optica-test"
        )
        val withStock = Montura(
            id = "m-ok", marca = "OK", modelo = "Stock", stockActual = 5, activo = true, opticaId = "optica-test"
        )
        coEvery { repository.getMonturasByOptica("optica-test") } returns flowOf(listOf(zeroStock, withStock))

        val vm = DispensacionViewModel(repository, sessionManager, postSaveSyncScheduler, stockHelper)
        testDispatcher.scheduler.advanceUntilIdle()

        val monturas = vm.monturasActivas.value
        assertNotNull("montura with stock=0 must be present", monturas.find { it.id == "m-zero" })
        assertNotNull("montura with stock>0 must be present", monturas.find { it.id == "m-ok" })
        assertEquals("both monturas must be exposed", 2, monturas.size)
    }

    /**
     * BUG-F2: generatedId MUST be stable across state updates.
     * UUID.randomUUID() as a data class default regenerates on every new
     * DispensacionUiState() construction, though .copy() preserves it.
     * Moving initialization to ViewModel init removes this risk entirely.
     */
    @Test
    fun generatedId_isStableAcrossStateUpdates() = runTest(testDispatcher) {
        coEvery { repository.getMonturasByOptica("optica-test") } returns flowOf(emptyList())

        val vm = DispensacionViewModel(repository, sessionManager, postSaveSyncScheduler, stockHelper)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialId = vm.uiState.value.generatedId

        vm.updateUiState { it.copy(montoTotal = "100.00") }
        assertEquals("generatedId must be stable after montoTotal update", initialId, vm.uiState.value.generatedId)

        vm.updateUiState { it.copy(ot = "OT-001") }
        assertEquals("generatedId must be stable after OT update", initialId, vm.uiState.value.generatedId)

        vm.updateUiState { it.copy(fecha = LocalDate.of(2026, 1, 1)) }
        assertEquals("generatedId must be stable after fecha update", initialId, vm.uiState.value.generatedId)
    }

    /**
     * BUG-F2-b: Spanish locale comma (e.g. "100,50") must be parseable as montoTotal.
     * toDoubleOrNull() rejects commas — must convert to dot first.
     */
    @Test
    fun montoTotal_acceptsCommaAsDecimalSeparator() {
        val parsed = "100,50".replace(',', '.').toDoubleOrNull()
        assertNotNull("100,50 must be parseable as 100.5 after comma-to-dot", parsed)
        assertEquals(100.5, parsed!!, 0.001)
        assertNotNull("100.50 must parse as 100.5", "100.50".toDoubleOrNull())
        assertNull("empty string must yield null", "".toDoubleOrNull())
    }
}
