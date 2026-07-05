package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.venta.Venta
import com.example.optoapp.data.venta.VentaDao
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
    private lateinit var ventaDao: VentaDao
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
        ventaDao = mockk(relaxed = true)
        every { sessionManager.opticaId } returns flowOf(opticaId)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getMonturasByOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getMovimientosMonturaByOptica(opticaId) } returns flowOf(emptyList())
        every { ventaDao.getVentasByOpticaAndDateRange(opticaId, any(), any()) } returns flowOf(emptyList())
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
        val ventas = listOf(
            Venta(id = "v1", opticaId = opticaId, origen = "dispensacion", origenId = "d1",
                pacienteId = "p1", fecha = today, montoTotal = 500.0, estado = "Completado"),
            Venta(id = "v2", opticaId = opticaId, origen = "servicio_extra", origenId = "s1",
                pacienteId = "p1", fecha = today, montoTotal = 120.0, estado = "Completado")
        )
        every { repository.countEvaluacionesInRangeForOptica(any(), any(), opticaId) } returns flowOf(0)
        every { repository.getDispensacionesByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(dispensaciones)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(emptyList())
        every { ventaDao.getVentasByOpticaAndDateRange(opticaId, any(), any()) } returns flowOf(ventas)

        viewModel = BIViewModel(repository, sessionManager, ventaDao)
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
        val ventas = listOf(
            Venta(id = "v1", opticaId = opticaId, origen = "dispensacion", origenId = "d1",
                pacienteId = "p1", fecha = today, montoTotal = 500.0, estado = "Completado")
        )
        mockEmptyPeriod()
        every { repository.getDispensacionesByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(dispensaciones)
        every { ventaDao.getVentasByOpticaAndDateRange(opticaId, any(), any()) } returns flowOf(ventas)

        viewModel = BIViewModel(repository, sessionManager, ventaDao)
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
        val ventasMes = listOf(
            Venta(id = "v1", opticaId = opticaId, origen = "dispensacion", origenId = "d1",
                pacienteId = "p1", fecha = today, montoTotal = 100.0, estado = "Completado")
        )
        val ventasAnio = listOf(
            Venta(id = "v1", opticaId = opticaId, origen = "dispensacion", origenId = "d1",
                pacienteId = "p1", fecha = today, montoTotal = 100.0, estado = "Completado"),
            Venta(id = "v2", opticaId = opticaId, origen = "dispensacion", origenId = "d2",
                pacienteId = "p2", fecha = today.minusMonths(3), montoTotal = 200.0, estado = "Completado")
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
        every { ventaDao.getVentasByOpticaAndDateRange(opticaId, any(), any()) } returns flowOf(ventasMes) andThen flowOf(ventasAnio)

        viewModel = BIViewModel(repository, sessionManager, ventaDao)
        advanceUntilIdle()
        assertEquals("Mes actual should show current month dispensations", 100.0, viewModel.uiState.value.recaudacionProyectada, 0.001)

        every { repository.getDispensacionesByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(dispensacionesAnio)
        viewModel.setPeriodo(Periodo.ANIO)
        advanceUntilIdle()

        assertEquals("Año should show all year dispensations", 300.0, viewModel.uiState.value.recaudacionProyectada, 0.001)
    }

    // ── Phase 4: ventas-based recaudacionProyectada (BI-1-a/b/c) ──────────

    @Test
    fun `BI-1-a recaudacionProyectada from ventas sums all ventas in period`() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val ventas = listOf(
            Venta(id = "v1", opticaId = opticaId, origen = "dispensacion", origenId = "d1",
                pacienteId = "p1", fecha = today, montoTotal = 500.0, estado = "Completado"),
            Venta(id = "v2", opticaId = opticaId, origen = "dispensacion", origenId = "d2",
                pacienteId = "p2", fecha = today, montoTotal = 120.0, estado = "Completado"),
            Venta(id = "v3", opticaId = opticaId, origen = "servicio_extra", origenId = "s1",
                pacienteId = "p3", fecha = today, montoTotal = 30.0, estado = "Completado")
        )
        every { repository.countEvaluacionesInRangeForOptica(any(), any(), opticaId) } returns flowOf(0)
        every { repository.getDispensacionesByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(emptyList())
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(emptyList())
        every { ventaDao.getVentasByOpticaAndDateRange(opticaId, any(), any()) } returns flowOf(ventas)

        viewModel = BIViewModel(repository, sessionManager, ventaDao)
        advanceUntilIdle()

        assertEquals("recaudacionProyectada must sum all ventas in period", 650.0,
            viewModel.uiState.value.recaudacionProyectada, 0.001)
    }

    @Test
    fun `BI-1-b recaudacionProyectada includes mixed origins from ventas`() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val ventas = listOf(
            Venta(id = "v1", opticaId = opticaId, origen = "dispensacion", origenId = "d1",
                pacienteId = "p1", fecha = today, montoTotal = 500.0, estado = "Completado"),
            Venta(id = "v2", opticaId = opticaId, origen = "servicio_extra", origenId = "s1",
                pacienteId = "p2", fecha = today, montoTotal = 120.0, estado = "Completado")
        )
        every { repository.countEvaluacionesInRangeForOptica(any(), any(), opticaId) } returns flowOf(0)
        every { repository.getDispensacionesByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(emptyList())
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(emptyList())
        every { ventaDao.getVentasByOpticaAndDateRange(opticaId, any(), any()) } returns flowOf(ventas)

        viewModel = BIViewModel(repository, sessionManager, ventaDao)
        advanceUntilIdle()

        assertEquals("recaudacionProyectada must include both origins from ventas", 620.0,
            viewModel.uiState.value.recaudacionProyectada, 0.001)
    }

    @Test
    fun `BI-1-c recaudacionProyectada is zero for empty ventas`() = runTest(testDispatcher) {
        every { repository.countEvaluacionesInRangeForOptica(any(), any(), opticaId) } returns flowOf(0)
        every { repository.getDispensacionesByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(emptyList())
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(emptyList())
        every { ventaDao.getVentasByOpticaAndDateRange(opticaId, any(), any()) } returns flowOf(emptyList())

        viewModel = BIViewModel(repository, sessionManager, ventaDao)
        advanceUntilIdle()

        assertEquals("recaudacionProyectada must be 0 for empty ventas", 0.0,
            viewModel.uiState.value.recaudacionProyectada, 0.001)
    }
}
