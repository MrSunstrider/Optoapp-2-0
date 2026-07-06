package com.example.optoapp.viewmodel

import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.AnalisisMensual
import com.example.optoapp.domain.Deudor
import com.example.optoapp.domain.GenerarRecomendacionesUseCase
import com.example.optoapp.domain.MargenCategoria
import com.example.optoapp.domain.ObtenerAnalisisMensualUseCase
import com.example.optoapp.domain.ObtenerDeudoresUseCase
import com.example.optoapp.domain.Prioridad
import com.example.optoapp.domain.Recomendacion
import com.example.optoapp.domain.RecomendacionTipo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
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
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: AnalisisNegocioViewModel

    private val opticaId = "optica-test-1"
    private val currentMonth = LocalDate.of(2026, 7, 1)

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
        sessionManager = mockk(relaxed = true)

        every { sessionManager.opticaId } returns flowOf(opticaId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun createAnalisis(
        ventasMes: Double = 10000.0,
        cobrosMes: Double = 8000.0,
        margenNetoPct: Double = 35.0,
        ventasMesAnterior: Double = 9000.0,
        variacionVentasPct: Double? = 11.1,
        esOffline: Boolean = false,
        gastosMes: Double = 2000.0
    ): AnalisisMensual = AnalisisMensual(
        ventasMes = ventasMes,
        cobrosMes = cobrosMes,
        margenNetoPct = margenNetoPct,
        margenPorCategoria = listOf(
            MargenCategoria("Monturas", 5000.0, 3000.0, 40.0),
            MargenCategoria("Lentes de Contacto", 3000.0, 2000.0, 33.3)
        ),
        deudores = com.example.optoapp.domain.DeudoresResumen(5, 2500.0),
        proyeccionCaja = com.example.optoapp.domain.ProyeccionCaja(12000.0, 4000.0, 8000.0),
        stockEstancado = listOf(
            com.example.optoapp.domain.StockEstancadoItem("m1", "SKU001", "Modelo A", 150.0, 3, "2026-05-01", 60)
        ),
        valorInventario = 50000.0,
        ventasMesAnterior = ventasMesAnterior,
        variacionVentasPct = variacionVentasPct,
        gastosMes = gastosMes,
        esOffline = esOffline
    )

    private fun createDeudores(): List<Deudor> = listOf(
        Deudor("Juan Pérez", "999888777", "v1", LocalDate.of(2026, 6, 15), 500.0, 200.0, 300.0, 15),
        Deudor("María López", "999888776", "v2", LocalDate.of(2026, 5, 1), 1200.0, 0.0, 1200.0, 60)
    )

    private fun createRecomendaciones(): List<Recomendacion> = listOf(
        Recomendacion("r1", RecomendacionTipo.COBRAR, "Cobranza pendiente",
            "Deuda total de S/ 1,500 en 2 deudores.", prioridad = Prioridad.ALTA),
        Recomendacion("r2", RecomendacionTipo.MEJORAR_PRECIO, "Mejorar precio de Monturas",
            "Margen bajo del 8%.", prioridad = Prioridad.ALTA),
        Recomendacion("r3", RecomendacionTipo.LIQUIDAR_STOCK, "Liquidar stock estancado",
            "1 item sin venderse.", prioridad = Prioridad.MEDIA)
    )

    private fun primeUseCases(
        analisis: Resource<AnalisisMensual> = Resource.Success(createAnalisis()),
        deudores: Resource<List<Deudor>> = Resource.Success(createDeudores()),
        recomendaciones: Resource<List<Recomendacion>> = Resource.Success(createRecomendaciones())
    ) {
        coEvery { obtenerAnalisisMensual(opticaId, any()) } returns analisis
        coEvery { obtenerDeudores(opticaId) } returns deudores
        coEvery { generarRecomendaciones(opticaId, any()) } returns recomendaciones
    }

    private val saldoEsperado = 10000.0 - 8000.0 // ventasMes - cobrosMes

    // ── Tests ─────────────────────────────────────────────────────────

    @Test
    fun `init loads data from all 3 use cases and populates state`() = runTest(testDispatcher) {
        primeUseCases()

        viewModel = AnalisisNegocioViewModel(
            obtenerAnalisisMensual, obtenerDeudores, generarRecomendaciones, sessionManager
        )
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

        viewModel = AnalisisNegocioViewModel(
            obtenerAnalisisMensual, obtenerDeudores, generarRecomendaciones, sessionManager
        )
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

        viewModel = AnalisisNegocioViewModel(
            obtenerAnalisisMensual, obtenerDeudores, generarRecomendaciones, sessionManager
        )
        advanceUntilIdle()

        viewModel.navigateMonth(-1)
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("mesSeleccionado should be July - 1 month = June", currentMonth.minusMonths(1), state.mesSeleccionado)
    }

    @Test
    fun `isLoading is false after successful load completion`() = runTest(testDispatcher) {
        primeUseCases()

        viewModel = AnalisisNegocioViewModel(
            obtenerAnalisisMensual, obtenerDeudores, generarRecomendaciones, sessionManager
        )
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("isLoading should be false after load completes", false, state.isLoading)
        assertNull("error should be null after successful load", state.error)
    }

    @Test
    fun `error state when analisis use case fails`() = runTest(testDispatcher) {
        primeUseCases(
            analisis = Resource.Error("Error en analisis mensual")
        )

        viewModel = AnalisisNegocioViewModel(
            obtenerAnalisisMensual, obtenerDeudores, generarRecomendaciones, sessionManager
        )
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("error should be set", "Error en analisis mensual", state.error)
        assertNull("analisis should be null on error", state.analisis)
        assertEquals("deudores should still load", 2, state.deudores.size)
        assertEquals("recomendaciones should still load", 3, state.recomendaciones.size)
    }

    @Test
    fun `offline detection propagates esOffline flag`() = runTest(testDispatcher) {
        primeUseCases(
            analisis = Resource.Success(createAnalisis(esOffline = true))
        )

        viewModel = AnalisisNegocioViewModel(
            obtenerAnalisisMensual, obtenerDeudores, generarRecomendaciones, sessionManager
        )
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertNotNull("analisis should be present", state.analisis)
        assertEquals("esOffline should be true", true, state.analisis!!.esOffline)
    }

    @Test
    fun `all use cases failing produces combined error`() = runTest(testDispatcher) {
        primeUseCases(
            analisis = Resource.Error("Error A"),
            deudores = Resource.Error("Error B"),
            recomendaciones = Resource.Error("Error C")
        )

        viewModel = AnalisisNegocioViewModel(
            obtenerAnalisisMensual, obtenerDeudores, generarRecomendaciones, sessionManager
        )
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertNotNull("error should be set", state.error)
        assertTrue("error should contain analisis error", state.error!!.contains("Error A"))
    }

    @Test
    fun `refresh re-invokes all use cases`() = runTest(testDispatcher) {
        primeUseCases()

        viewModel = AnalisisNegocioViewModel(
            obtenerAnalisisMensual, obtenerDeudores, generarRecomendaciones, sessionManager
        )
        advanceUntilIdle()

        // Change to error state
        coEvery { obtenerAnalisisMensual(opticaId, any()) } returns Resource.Error("Refresh error")

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("error should reflect refresh failure", "Refresh error", state.error)
    }
}
