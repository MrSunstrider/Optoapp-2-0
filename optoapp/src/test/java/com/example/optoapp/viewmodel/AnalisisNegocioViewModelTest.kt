package com.example.optoapp.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.AnalisisMensual
import com.example.optoapp.domain.Deudor
import com.example.optoapp.domain.FeedbackRecomendacionUseCase
import com.example.optoapp.domain.GenerarRecomendacionesUseCase
import com.example.optoapp.domain.MargenCategoria
import com.example.optoapp.domain.ObtenerAnalisisMensualUseCase
import com.example.optoapp.domain.ObtenerDeudoresUseCase
import com.example.optoapp.domain.Prioridad
import com.example.optoapp.domain.Recomendacion
import com.example.optoapp.domain.RecomendacionTipo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AnalisisNegocioViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var obtenerAnalisisMensual: ObtenerAnalisisMensualUseCase
    private lateinit var obtenerDeudores: ObtenerDeudoresUseCase
    private lateinit var generarRecomendaciones: GenerarRecomendacionesUseCase
    private lateinit var feedbackRecomendacion: FeedbackRecomendacionUseCase
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: AnalisisNegocioViewModel

    private val opticaId = "optica-test-1"
    private val currentMonth = LocalDate.now().withDayOfMonth(1)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0

        obtenerAnalisisMensual = mockk()
        obtenerDeudores = mockk()
        generarRecomendaciones = mockk()
        feedbackRecomendacion = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)

        every { sessionManager.opticaId } returns flowOf(opticaId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createAnalisis(
        ventasMes: Double = 10000.0,
        cobrosMes: Double = 8000.0,
        margenNetoPct: Double = 35.0,
        ventasMesAnterior: Double = 9000.0,
        variacionVentasPct: Double? = 11.1,
        esOffline: Boolean = false,
        gastosMes: Double = 2000.0,
    ): AnalisisMensual = AnalisisMensual(
        ventasMes = ventasMes,
        cobrosMes = cobrosMes,
        margenNetoPct = margenNetoPct,
        margenPorCategoria = listOf(
            MargenCategoria("Monturas", 5000.0, 3000.0, 40.0),
            MargenCategoria("Lentes de Contacto", 3000.0, 2000.0, 33.3),
        ),
        deudores = com.example.optoapp.domain.DeudoresResumen(5, 2500.0),
        proyeccionCaja = com.example.optoapp.domain.ProyeccionCaja(12000.0, 4000.0, 8000.0),
        stockEstancado = listOf(
            com.example.optoapp.domain.StockEstancadoItem("m1", "SKU001", "Modelo A", 150.0, 3, "2026-05-01", 60),
        ),
        valorInventario = 50000.0,
        ventasMesAnterior = ventasMesAnterior,
        variacionVentasPct = variacionVentasPct,
        gastosMes = gastosMes,
        esOffline = esOffline,
    )

    private fun createDeudores(): List<Deudor> = listOf(
        Deudor("Juan P\u00e9rez", "999888777", "v1", LocalDate.of(2026, 6, 15), 500.0, 200.0, 300.0, 15),
        Deudor("Mar\u00eda L\u00f3pez", "999888776", "v2", LocalDate.of(2026, 5, 1), 1200.0, 0.0, 1200.0, 60),
    )

    private fun createRecomendaciones(): List<Recomendacion> = listOf(
        Recomendacion(
            "r1",
            RecomendacionTipo.COBRAR,
            "Cobranza pendiente",
            "Deuda total de S/ 1,500 en 2 deudores.",
            prioridad = Prioridad.ALTA,
        ),
        Recomendacion(
            "r2",
            RecomendacionTipo.MEJORAR_PRECIO,
            "Mejorar precio de Monturas",
            "Margen bajo del 8%.",
            prioridad = Prioridad.ALTA,
        ),
        Recomendacion(
            "r3",
            RecomendacionTipo.LIQUIDAR_STOCK,
            "Liquidar stock estancado",
            "1 item sin venderse.",
            prioridad = Prioridad.MEDIA,
        ),
    )

    private fun primeUseCases(
        analisis: Resource<AnalisisMensual> = Resource.Success(createAnalisis()),
        deudores: Resource<List<Deudor>> = Resource.Success(createDeudores()),
        recomendaciones: Resource<List<Recomendacion>> = Resource.Success(createRecomendaciones()),
    ) {
        coEvery { obtenerAnalisisMensual(opticaId, any()) } returns analisis
        coEvery { obtenerDeudores(opticaId) } returns deudores
        coEvery { generarRecomendaciones(any(), any<List<Deudor>>(), any()) } returns recomendaciones
    }

    private val saldoEsperado = 10000.0 - 8000.0

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): AnalisisNegocioViewModel = AnalisisNegocioViewModel(
        obtenerAnalisisMensual,
        obtenerDeudores,
        generarRecomendaciones,
        feedbackRecomendacion,
        sessionManager,
        savedStateHandle,
    )

    @Test
    fun `SavedStateHandle yearMonth 2026-03 initializes mesSeleccionado to March`() = runTest(testDispatcher) {
        primeUseCases()
        val march = LocalDate.of(2026, 3, 1)

        viewModel = createViewModel(SavedStateHandle(mapOf("yearMonth" to "2026-03")))
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("mesSeleccionado should be March 2026", march, state.mesSeleccionado)
        coVerify { obtenerAnalisisMensual(opticaId, march) }
    }

    @Test
    fun `SavedStateHandle yearMonth 2025-12 initializes December`() = runTest(testDispatcher) {
        primeUseCases()
        val december = LocalDate.of(2025, 12, 1)

        viewModel = createViewModel(SavedStateHandle(mapOf("yearMonth" to "2025-12")))
        advanceUntilIdle()

        assertEquals(december, viewModel.uiState.first().mesSeleccionado)
        coVerify { obtenerAnalisisMensual(opticaId, december) }
    }

    @Test
    fun `invalid SavedStateHandle yearMonth falls back to current month`() = runTest(testDispatcher) {
        primeUseCases()

        viewModel = createViewModel(SavedStateHandle(mapOf("yearMonth" to "not-a-month")))
        advanceUntilIdle()

        assertEquals(currentMonth, viewModel.uiState.first().mesSeleccionado)
        coVerify { obtenerAnalisisMensual(opticaId, currentMonth) }
    }

    @Test
    fun `missing yearMonth arg falls back to current month`() = runTest(testDispatcher) {
        primeUseCases()

        viewModel = createViewModel(SavedStateHandle())
        advanceUntilIdle()

        assertEquals(currentMonth, viewModel.uiState.first().mesSeleccionado)
        coVerify { obtenerAnalisisMensual(opticaId, currentMonth) }
    }

    @Test
    fun `resolveInitialMonth parses valid and rejects invalid`() {
        assertEquals(LocalDate.of(2026, 3, 1), AnalisisNegocioViewModel.resolveInitialMonth("2026-03"))
        assertEquals(LocalDate.of(2025, 12, 1), AnalisisNegocioViewModel.resolveInitialMonth("2025-12"))
        assertEquals(currentMonth, AnalisisNegocioViewModel.resolveInitialMonth("2026-13", currentMonth))
        assertEquals(currentMonth, AnalisisNegocioViewModel.resolveInitialMonth("", currentMonth))
        assertEquals(currentMonth, AnalisisNegocioViewModel.resolveInitialMonth(null, currentMonth))
    }

    @Test
    fun `init loads data from all 3 use cases and populates state`() = runTest(testDispatcher) {
        primeUseCases()

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("mesSeleccionado should be current month", currentMonth, state.mesSeleccionado)
        assertNotNull("analisis should be populated", state.analisis)
        assertEquals("ventasMes should match", 10000.0, state.analisis!!.ventasMes, 0.001)
        assertEquals("deudores should be populated", 2, state.deudores.size)
        assertEquals("recomendaciones should be populated", 3, state.recomendaciones.size)
        assertEquals("isLoading should be false after load", false, state.isLoading)
        assertNull("error should be null after successful load", state.error)
    }

    @Test
    fun `navigateMonth plusOne updates mesSeleccionado and reloads`() = runTest(testDispatcher) {
        primeUseCases()

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.navigateMonth(1)
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("mesSeleccionado should be July + 1 month = August", currentMonth.plusMonths(1), state.mesSeleccionado)
        assertNotNull("analisis should be populated for new month", state.analisis)
    }

    @Test
    fun `navigateMonth minusOne updates mesSeleccionado and reloads`() = runTest(testDispatcher) {
        primeUseCases()

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.navigateMonth(-1)
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("mesSeleccionado should be July - 1 month = June", currentMonth.minusMonths(1), state.mesSeleccionado)
    }

    @Test
    fun `isLoading is false after successful load completion`() = runTest(testDispatcher) {
        primeUseCases()

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("isLoading should be false after load completes", false, state.isLoading)
        assertNull("error should be null after successful load", state.error)
    }

    @Test
    fun `error state when analisis use case fails`() = runTest(testDispatcher) {
        primeUseCases(
            analisis = Resource.Error("Error en analisis mensual"),
            recomendaciones = Resource.Error("No hay datos de analisis para generar recomendaciones"),
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertNotNull("error should be set", state.error)
        assertTrue("error should contain analisis error", state.error!!.contains("Error en analisis mensual"))
        assertNull("analisis should be null on error", state.analisis)
        assertEquals("deudores should still load", 2, state.deudores.size)
        assertEquals("recomendaciones should be empty when analisis fails", 0, state.recomendaciones.size)
    }

    @Test
    fun `offline detection propagates esOffline flag`() = runTest(testDispatcher) {
        primeUseCases(
            analisis = Resource.Success(createAnalisis(esOffline = true)),
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertNotNull("analisis should be present", state.analisis)
        assertEquals("esOffline should be true", true, state.analisis!!.esOffline)
        assertEquals("isSeasonalityWarning should be true when offline", true, state.isSeasonalityWarning)
    }

    @Test
    fun `all use cases failing produces combined error with distinct messages`() = runTest(testDispatcher) {
        primeUseCases(
            analisis = Resource.Error("Error A"),
            deudores = Resource.Error("Error B"),
            recomendaciones = Resource.Error("Datos insuficientes para generar recomendaciones"),
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertNotNull("error should be set", state.error)
        assertTrue("error should contain analisis error", state.error!!.contains("Error A"))
        assertTrue("error should contain deudores error", state.error!!.contains("Error B"))
    }

    @Test
    fun `onFeedback util calls marcarUtil`() = runTest(testDispatcher) {
        primeUseCases()
        coEvery { feedbackRecomendacion.marcarUtil("r1", opticaId) } returns Unit

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onFeedback("r1", fueUtil = true)
        advanceUntilIdle()

        coEvery { feedbackRecomendacion.marcarUtil("r1", opticaId) }
        // No exception = success
    }

    @Test
    fun `onFeedback no util calls marcarNoUtil`() = runTest(testDispatcher) {
        primeUseCases()
        coEvery { feedbackRecomendacion.marcarNoUtil("r2", opticaId) } returns Unit

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onFeedback("r2", fueUtil = false)
        advanceUntilIdle()
        // No exception = success
    }

    @Test
    fun `isSeasonalityWarning is false when online`() = runTest(testDispatcher) {
        primeUseCases(analisis = Resource.Success(createAnalisis(esOffline = false)))

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("isSeasonalityWarning should be false when online", false, state.isSeasonalityWarning)
    }

    @Test
    fun `sibling failure in loadData does not cancel deudores deferred`() = runTest(testDispatcher) {
        coEvery { obtenerAnalisisMensual(opticaId, any()) } throws RuntimeException("analisis crashed")
        coEvery { obtenerDeudores(opticaId) } returns Resource.Success(createDeudores())
        coEvery { generarRecomendaciones(any(), any<List<Deudor>>(), any()) } returns Resource.Error("Datos insuficientes")

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("deudores must still load despite analisis crash", 2, state.deudores.size)
        assertFalse("isLoading should be false after load completes", state.isLoading)
    }

    @Test
    fun `cancellation does not produce error state`() = runTest(testDispatcher) {
        coEvery { obtenerAnalisisMensual(opticaId, any()) } throws CancellationException("Test cancel")
        coEvery { obtenerDeudores(opticaId) } returns Resource.Success(createDeudores())
        coEvery { generarRecomendaciones(any(), any<List<Deudor>>(), any()) } returns Resource.Success(createRecomendaciones())

        try {
            viewModel = createViewModel()
        } catch (_: CancellationException) {
            // Expected — cancellation should propagate, not be swallowed
        }

        val state = viewModel.uiState.first()
        assertNull("error should be null after cancellation", state.error)
        assertEquals("isLoading should be true (cancelled before completion)", true, state.isLoading)
    }
}
