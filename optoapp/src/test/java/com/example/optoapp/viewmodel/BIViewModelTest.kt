package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class BIViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: BIViewModel

    private val opticaId = "optica-bi-1"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        repository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        every { sessionManager.opticaId } returns flowOf(opticaId)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getMonturasByOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getMovimientosMonturaByOptica(opticaId) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun mockEmptyPeriod() {
        every { repository.countEvaluacionesInRangeForOptica(any(), any(), opticaId) } returns flowOf(0)
        every { repository.getDispensacionesByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(emptyList())
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(emptyList())
    }

    @Test
    fun `recaudacionProyectada includes ServicioExtra montoTotal`() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val dispensaciones = listOf(
            DispensacionOptica(
                id = "d1",
                pacienteId = "p1",
                fecha = today,
                montoTotal = 500.0,
                opticaId = opticaId
            )
        )
        val servicios = listOf(
            ServicioExtra(
                id = "s1",
                descripcion = "Servicio",
                montoTotal = 120.0,
                aCuenta = 60.0,
                estado = "Entregado",
                fecha = today,
                opticaId = opticaId
            )
        )
        every { repository.countEvaluacionesInRangeForOptica(any(), any(), opticaId) } returns flowOf(0)
        every { repository.getDispensacionesByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(dispensaciones)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(emptyList())
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)

        viewModel = BIViewModel(repository, sessionManager)
        advanceUntilIdle()

        assertEquals("recaudacionProyectada must include dispensaciones + servicios extra",
            620.0, viewModel.uiState.value.recaudacionProyectada, 0.001)
    }

    @Test
    fun `recaudacionProyectada with no servicios extra matches dispensaciones only`() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val dispensaciones = listOf(
            DispensacionOptica(
                id = "d1",
                pacienteId = "p1",
                fecha = today,
                montoTotal = 500.0,
                opticaId = opticaId
            )
        )
        mockEmptyPeriod()
        every { repository.getDispensacionesByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(dispensaciones)

        viewModel = BIViewModel(repository, sessionManager)
        advanceUntilIdle()

        assertEquals("recaudacionProyectada should match dispensaciones when no servicios extra",
            500.0, viewModel.uiState.value.recaudacionProyectada, 0.001)
    }

    @Test
    fun `changing period re-triggers recaudacionProyectada calculation`() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val dispensacionesMes = listOf(
            DispensacionOptica(
                id = "d1",
                pacienteId = "p1",
                fecha = today,
                montoTotal = 100.0,
                opticaId = opticaId
            )
        )
        val dispensacionesAnio = listOf(
            DispensacionOptica(
                id = "d1",
                pacienteId = "p1",
                fecha = today,
                montoTotal = 100.0,
                opticaId = opticaId
            ),
            DispensacionOptica(
                id = "d2",
                pacienteId = "p2",
                fecha = today.minusMonths(3),
                montoTotal = 200.0,
                opticaId = opticaId
            )
        )
        every { repository.countEvaluacionesInRangeForOptica(any(), any(), opticaId) } returns flowOf(0)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(emptyList())
        every { repository.getDispensacionesByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(dispensacionesMes)

        viewModel = BIViewModel(repository, sessionManager)
        advanceUntilIdle()
        assertEquals("Mes actual should show current month dispensations", 100.0, viewModel.uiState.value.recaudacionProyectada, 0.001)

        every { repository.getDispensacionesByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(dispensacionesAnio)
        viewModel.setPeriodo(Periodo.ANIO)
        advanceUntilIdle()

        assertEquals("Año should show all year dispensations", 300.0, viewModel.uiState.value.recaudacionProyectada, 0.001)
    }
}
