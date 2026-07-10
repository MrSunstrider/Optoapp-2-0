package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.MovimientoFinanciero
import com.example.optoapp.domain.Origen
import com.example.optoapp.domain.TipoMovimiento
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
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ReportesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: ReportesViewModel

    private val opticaId = "optica-rf-1"
    private val today = LocalDate.of(2026, 7, 1)
    private val tomorrow = today.plusDays(1)
    private val dayAfter = today.plusDays(2)

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
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(emptyList())
        every { repository.getAllPagosFlowForOptica(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `totalVendido from dispensaciones and servicios sums all in period`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 100.0, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p2", fecha = today, montoTotal = 200.0, opticaId = opticaId)
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Srv", montoTotal = 50.0, aCuenta = 50.0, estado = "Entregado", fecha = today, opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)

        viewModel = ReportesViewModel(repository, sessionManager)
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        // totalVendido from movimientos created from dispensaciones+servicios, filtered by period
        assertEquals("totalVendido must sum all movimientos in period", 350.0,
            viewModel.totalVendido.first(), 0.001)
    }

    @Test
    fun `totalVendido includes both dispensaciones and servicios`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 100.0, opticaId = opticaId)
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Srv", montoTotal = 80.0, aCuenta = 50.0, estado = "Entregado", fecha = today, opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)

        viewModel = ReportesViewModel(repository, sessionManager)
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("totalVendido must sum from both origins", 180.0,
            viewModel.totalVendido.first(), 0.001)
    }

    @Test
    fun `empty period returns zero totalVendido`() = runTest(testDispatcher) {
        viewModel = ReportesViewModel(repository, sessionManager)
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("totalVendido must be 0 for empty period", 0.0,
            viewModel.totalVendido.first(), 0.001)
    }

    @Test
    fun `totalTransacciones counts movimientos in period`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 100.0, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p2", fecha = today, montoTotal = 200.0, opticaId = opticaId)
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Srv", montoTotal = 50.0, aCuenta = 50.0, estado = "Entregado", fecha = today, opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)

        viewModel = ReportesViewModel(repository, sessionManager)
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("totalTransacciones must count all movimientos in period", 3,
            viewModel.totalTransacciones.first())
    }
}
