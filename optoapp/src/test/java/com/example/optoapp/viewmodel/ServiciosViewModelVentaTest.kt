package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.venta.Venta
import com.example.optoapp.sync.PostSaveSyncScheduler
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
 * Tests that saveServicio() upserts a Venta locally
 * BEFORE scheduleFinanzasSync, following offline-first design.
 */
class ServiciosViewModelVentaTest {

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var postSaveSyncScheduler: PostSaveSyncScheduler

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

        every { sessionManager.opticaId } returns opticaIdFlow
        coEvery { repository.reassignLegacyMiOpticaBaseTo(any()) } returns Unit
        // Mock flows for init block
        every { repository.getAllServiciosForOptica(any()) } returns flowOf(emptyList())
        every { repository.pacientesFlowForOptica(any()) } returns flowOf(emptyList())
        every { repository.getMonturasByOptica(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveServicio upserts Venta with v_serv prefix before scheduleFinanzasSync`() = runTest {
        val ventaSlot = slot<Venta>()

        val vm = ServiciosViewModel(repository, sessionManager, postSaveSyncScheduler)
        testDispatcher.scheduler.advanceUntilIdle()

        // Set up valid UI state for save
        vm.updateUiState { s ->
            s.copy(
                ot = "OT-SERV-001",
                descripcion = "Reparacion de montura",
                montoTotal = "75.0",
                fecha = testDate,
                pacienteId = "paciente-1",
                estado = "Pendiente"
            )
        }

        var successCalled = false
        vm.saveServicio { successCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            repository.upsertVenta(capture(ventaSlot))
            postSaveSyncScheduler.scheduleFinanzasSync("optica-test")
        }

        val captured = ventaSlot.captured
        assertTrue(captured.id.startsWith("v_serv_"))
        assertEquals("servicio_extra", captured.origen)
        assertEquals("optica-test", captured.opticaId)
        assertEquals(75.0, captured.montoTotal, 0.001)
        assertEquals("Pendiente", captured.estado)
        assertEquals("paciente-1", captured.pacienteId)
        assertEquals(testDate, captured.fecha)
        assertTrue(successCalled)
    }

    @Test
    fun `saveServicio Venta uses empty pacienteId when null`() = runTest {
        val ventaSlot = slot<Venta>()

        val vm = ServiciosViewModel(repository, sessionManager, postSaveSyncScheduler)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.updateUiState { s ->
            s.copy(
                ot = "OT-SERV-002",
                descripcion = "Consulta",
                montoTotal = "50.0",
                fecha = testDate,
                pacienteId = null,
                estado = "Pendiente"
            )
        }

        var successCalled = false
        vm.saveServicio { successCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.upsertVenta(capture(ventaSlot)) }

        val captured = ventaSlot.captured
        assertEquals("", captured.pacienteId)
        assertTrue(successCalled)
    }

    @Test
    fun `saveServicio does NOT upsert Venta when validation fails`() = runTest {
        val vm = ServiciosViewModel(repository, sessionManager, postSaveSyncScheduler)

        // Empty description + empty montoTotal should fail validation
        vm.updateUiState { s -> s.copy(descripcion = "", montoTotal = "") }

        var successCalled = false
        vm.saveServicio { successCalled = false }

        coVerify(exactly = 0) { repository.upsertVenta(any<Venta>()) }
        coVerify(exactly = 0) { postSaveSyncScheduler.scheduleFinanzasSync(any()) }
    }
}
