package com.example.optoapp.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.optoapp.data.DispensacionDao
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.AnalisisMensual
import com.example.optoapp.domain.DeudoresResumen
import com.example.optoapp.domain.FuenteDatos
import com.example.optoapp.domain.MarginFacts
import com.example.optoapp.domain.ObtenerAnalisisMensualUseCase
import com.example.optoapp.util.CurrentMonthProvider
import com.example.optoapp.util.MesArg
import io.mockk.coEvery
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CostosHeaderViewModelTest {

    private val opticaId = "optica-1"
    private val mayo = YearMonth.of(2026, 5)
    private val actual = YearMonth.of(2026, 7)

    private val analisisUseCase = mockk<ObtenerAnalisisMensualUseCase>()
    private val dispensacionDao = mockk<DispensacionDao>()
    private val sessionManager = mockk<SessionManager>()
    private val currentMonthProvider = mockk<CurrentMonthProvider>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { sessionManager.opticaId } returns flowOf(opticaId)
        every { currentMonthProvider.current() } returns actual
        coEvery { analisisUseCase(opticaId, any()) } returns Resource.Success(analisis(costo = 0.0, gastos = 0.0))
        every { dispensacionDao.countOtsSinCosto(opticaId, any(), any()) } returns flowOf(0)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun headerFiguresAndCtaFollowTheSelectedMonth() {
        coEvery { analisisUseCase(opticaId, mayo.atDay(1)) } returns
            Resource.Success(analisis(costo = 1200.0, gastos = 800.0))
        every {
            dispensacionDao.countOtsSinCosto(opticaId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31))
        } returns flowOf(3)
        val viewModel = buildViewModel()

        viewModel.seleccionarMes(mayo)

        val state = viewModel.uiState.value
        assertEquals(mayo, state.mes)
        assertEquals(1200.0, state.costoVentas, 0.001)
        assertEquals(800.0, state.gastos, 0.001)
        assertEquals(3, state.otsSinCosto)
        assertTrue(state.mostrarCtaOtsSinCosto)
        assertFalse(state.isLoading)
    }

    @Test
    fun ctaIsHiddenWhenNoOtLacksCost() {
        val viewModel = buildViewModel()

        viewModel.seleccionarMes(mayo)

        assertEquals(0, viewModel.uiState.value.otsSinCosto)
        assertFalse(viewModel.uiState.value.mostrarCtaOtsSinCosto)
    }

    @Test
    fun offlineKeepsRoomFiguresAndReplacesTheCtaWithTheCoverageNote() {
        coEvery { analisisUseCase(opticaId, mayo.atDay(1)) } returns
            Resource.Success(analisis(costo = 450.0, gastos = 300.0, fuente = FuenteDatos.OFFLINE))
        every { dispensacionDao.countOtsSinCosto(opticaId, any(), any()) } returns flowOf(2)
        val viewModel = buildViewModel()

        viewModel.seleccionarMes(mayo)

        val state = viewModel.uiState.value
        assertEquals(450.0, state.costoVentas, 0.001)
        assertEquals(300.0, state.gastos, 0.001)
        assertTrue(state.offline)
        assertFalse(state.mostrarCtaOtsSinCosto)
    }

    @Test
    fun monthArgumentSetsTheInitialMonth() {
        val viewModel = buildViewModel(SavedStateHandle(mapOf(MesArg.KEY to "2026-05")))

        assertEquals(mayo, viewModel.uiState.value.mes)
    }

    @Test
    fun futureMonthIsClampedToTheCurrentOne() {
        val viewModel = buildViewModel()

        viewModel.seleccionarMes(YearMonth.of(2026, 9))

        assertEquals(actual, viewModel.uiState.value.mes)
    }

    @Test
    fun analysisErrorHidesTheCtaAndReportsIt() {
        coEvery { analisisUseCase(opticaId, mayo.atDay(1)) } returns Resource.Error("No se pudieron cargar los datos del mes")
        every { dispensacionDao.countOtsSinCosto(opticaId, any(), any()) } returns flowOf(4)
        val viewModel = buildViewModel()

        viewModel.seleccionarMes(mayo)

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.mostrarCtaOtsSinCosto)
    }

    private fun buildViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        CostosHeaderViewModel(analisisUseCase, dispensacionDao, sessionManager, currentMonthProvider, savedStateHandle)

    private fun analisis(costo: Double, gastos: Double, fuente: FuenteDatos = FuenteDatos.SERVIDOR) = AnalisisMensual(
        ventasMes = 0.0,
        cobrosMes = 0.0,
        margenNetoPct = 0.0,
        margenPorCategoria = emptyList(),
        deudores = DeudoresResumen(0, 0.0),
        proyeccionCaja = null,
        stockEstancado = emptyList(),
        valorInventario = 0.0,
        ventasMesAnterior = 0.0,
        variacionVentasPct = null,
        gastosMes = gastos,
        esOffline = fuente == FuenteDatos.OFFLINE,
        costoMes = costo,
        marginFacts = MarginFacts(costoMes = costo, fuente = fuente),
    )
}
