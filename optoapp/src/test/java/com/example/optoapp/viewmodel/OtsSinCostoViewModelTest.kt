package com.example.optoapp.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.optoapp.data.DispensacionDao
import com.example.optoapp.data.OtSinCostoRow
import com.example.optoapp.data.SessionManager
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.util.CurrentMonthProvider
import com.example.optoapp.util.MesArg
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class OtsSinCostoViewModelTest {

    private val opticaId = "optica-1"
    private val actual = YearMonth.of(2026, 7)
    private val fila = OtSinCostoRow("d1", "OT-2026-0001", "p1", "Ana Perez", LocalDate.of(2026, 5, 9), 350.0, 2)

    private val dispensacionDao = mockk<DispensacionDao>()
    private val sessionManager = mockk<SessionManager>()
    private val currentMonthProvider = mockk<CurrentMonthProvider>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { sessionManager.opticaId } returns flowOf(opticaId)
        every { currentMonthProvider.current() } returns actual
        every { dispensacionDao.observeOtsSinCosto(opticaId, any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun monthArgumentSelectsTheListedMonth() {
        every {
            dispensacionDao.observeOtsSinCosto(opticaId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31))
        } returns flowOf(listOf(fila))

        val state = buildViewModel("2026-05").uiState.value

        assertEquals(YearMonth.of(2026, 5), state.mes)
        assertEquals(listOf(fila), state.filas)
        assertFalse(state.isLoading)
    }

    @Test
    fun malformedOrMissingMonthFallsBackToTheCurrentOne() {
        assertEquals(actual, buildViewModel("mayo").uiState.value.mes)
        assertEquals(actual, buildViewModel(null).uiState.value.mes)
    }

    @Test
    fun tappingARowOpensTheOtOnItsCostSection() {
        val ruta = buildViewModel("2026-05").rutaEdicion(fila)

        assertEquals(Route.EditarDispensacion("p1", "d1", Route.EditarDispensacion.SECCION_COSTO).route, ruta)
        assertEquals("editarDispensacion/p1/d1?seccion=costo", ruta)
    }

    private fun buildViewModel(mes: String?) = OtsSinCostoViewModel(
        dispensacionDao,
        sessionManager,
        currentMonthProvider,
        SavedStateHandle(listOfNotNull(mes?.let { MesArg.KEY to it }).toMap()),
    )
}
