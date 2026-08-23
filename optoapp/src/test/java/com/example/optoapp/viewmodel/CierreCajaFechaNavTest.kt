package com.example.optoapp.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.util.DateUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CierreCajaFechaNavTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private val today = LocalDate.of(2026, 8, 23)
    private val yesterday = LocalDate.of(2026, 8, 22)
    private val opticaId = "optica-1"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        mockkObject(DateUtils)
        every { DateUtils.today() } returns today
        repository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        every { sessionManager.opticaId } returns flowOf(opticaId)
        every { sessionManager.opticaRol } returns flowOf("admin")
        every { repository.getPagosByDateRangeForOptica(any(), any(), any()) } returns flowOf(emptyList())
        every { repository.getDispensacionesByDateRangeForOptica(any(), any(), any()) } returns flowOf(emptyList())
        every { repository.getServiciosByDateRangeForOptica(any(), any(), any()) } returns flowOf(emptyList())
        every { repository.pacientesFlowForOptica(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun resolveInitialFecha_parsesIsoArg() {
        assertEquals(yesterday, CierreCajaViewModel.resolveInitialFecha("2026-08-22", today))
    }

    @Test
    fun resolveInitialFecha_missingOrBlank_defaultsToToday() {
        assertEquals(today, CierreCajaViewModel.resolveInitialFecha(null, today))
        assertEquals(today, CierreCajaViewModel.resolveInitialFecha("", today))
        assertEquals(today, CierreCajaViewModel.resolveInitialFecha("  ", today))
    }

    @Test
    fun resolveInitialFecha_invalid_defaultsToToday() {
        assertEquals(today, CierreCajaViewModel.resolveInitialFecha("not-a-date", today))
    }

    @Test
    fun viewModel_appliesSavedStateHandleFecha() = runTest(testDispatcher) {
        val vm = CierreCajaViewModel(
            repository,
            sessionManager,
            SavedStateHandle(mapOf(CierreCajaViewModel.FECHA_ARG to "2026-08-22")),
        )
        val state = vm.uiState.first { !it.isLoading || it.fecha == yesterday }
        assertEquals(yesterday, state.fecha)
    }

    @Test
    fun viewModel_missingFechaArg_defaultsToToday() = runTest(testDispatcher) {
        val vm = CierreCajaViewModel(repository, sessionManager, SavedStateHandle())
        assertEquals(today, vm.uiState.value.fecha)
    }

    @Test
    fun cierreCajaRoute_withFecha_buildsQuery() {
        assertEquals("cierre_caja?fecha=2026-08-22", com.example.optoapp.ui.navigation.Route.CierreCaja.withFecha(yesterday))
        assertEquals("cierre_caja", com.example.optoapp.ui.navigation.Route.CierreCaja.routeWithoutFecha())
    }
}
