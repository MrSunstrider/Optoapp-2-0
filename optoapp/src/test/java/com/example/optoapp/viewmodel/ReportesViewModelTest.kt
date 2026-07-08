package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.venta.Venta
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
    private val periodStart = LocalDate.of(2026, 7, 1)
    private val periodEnd = LocalDate.of(2026, 7, 31)

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
        every { repository.getVentasByOpticaAndDateRange(opticaId, any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `RF-1-a totalVendido from ventas sums all ventas in period`() = runTest(testDispatcher) {
        val ventas = listOf(
            Venta(id = "v1", opticaId = opticaId, origen = "dispensacion", origenId = "d1",
                pacienteId = "p1", fecha = today, montoTotal = 100.0, estado = "Completado"),
            Venta(id = "v2", opticaId = opticaId, origen = "dispensacion", origenId = "d2",
                pacienteId = "p2", fecha = today.plusDays(1), montoTotal = 200.0, estado = "Completado"),
            Venta(id = "v3", opticaId = opticaId, origen = "servicio_extra", origenId = "s1",
                pacienteId = "p3", fecha = today.plusDays(2), montoTotal = 50.0, estado = "Completado")
        )
        every { repository.getVentasByOpticaAndDateRange(opticaId, any(), any()) } returns flowOf(ventas)
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(emptyList())

        viewModel = ReportesViewModel(repository, sessionManager)
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        // totalVendido should be 350.0 from ventas, overriding the 0 from empty dispensaciones+servicios
        assertEquals("totalVendido must sum ventas.montoTotal in period", 350.0,
            viewModel.totalVendido.first(), 0.001)
    }

    @Test
    fun `RF-1-a totalVendido includes both origins`() = runTest(testDispatcher) {
        val ventas = listOf(
            Venta(id = "v1", opticaId = opticaId, origen = "dispensacion", origenId = "d1",
                pacienteId = "p1", fecha = today, montoTotal = 100.0, estado = "Completado"),
            Venta(id = "v2", opticaId = opticaId, origen = "servicio_extra", origenId = "s1",
                pacienteId = "p2", fecha = today, montoTotal = 80.0, estado = "Completado")
        )
        every { repository.getVentasByOpticaAndDateRange(opticaId, any(), any()) } returns flowOf(ventas)
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(emptyList())

        viewModel = ReportesViewModel(repository, sessionManager)
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("totalVendido must sum ventas from both origins", 180.0,
            viewModel.totalVendido.first(), 0.001)
    }

    @Test
    fun `RF-1-b empty period returns zero totalVendido`() = runTest(testDispatcher) {
        every { repository.getVentasByOpticaAndDateRange(opticaId, any(), any()) } returns flowOf(emptyList())

        viewModel = ReportesViewModel(repository, sessionManager)
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("totalVendido must be 0 for empty period", 0.0,
            viewModel.totalVendido.first(), 0.001)
    }

    @Test
    fun `allVentasDelPeriodo emits ventas from VentaDao`() = runTest(testDispatcher) {
        val ventas = listOf(
            Venta(id = "v1", opticaId = opticaId, origen = "dispensacion", origenId = "d1",
                pacienteId = "p1", fecha = today, montoTotal = 100.0, estado = "Completado"),
            Venta(id = "v2", opticaId = opticaId, origen = "servicio_extra", origenId = "s1",
                pacienteId = "p2", fecha = today, montoTotal = 80.0, estado = "Completado")
        )
        every { repository.getVentasByOpticaAndDateRange(opticaId, any(), any()) } returns flowOf(ventas)

        viewModel = ReportesViewModel(repository, sessionManager)
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        val result = viewModel.allVentasDelPeriodo.first()
        assertEquals("allVentasDelPeriodo must emit 2 ventas", 2, result.size)
    }
}
