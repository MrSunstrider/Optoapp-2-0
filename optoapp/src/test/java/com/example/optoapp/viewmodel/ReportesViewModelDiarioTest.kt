package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.venta.Venta
import com.example.optoapp.data.venta.VentaDao
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
 * Tests for ReportesViewModel — daily period ("Diario") logic.
 *
 * Coverage:
 * - allDispensaciones filtering by fechaDiario
 * - totalVendido, totalPagado, totalCobrado, cobrosPeriodo, ventasPeriodo
 * - Edge cases: empty data, pagos sin dispensación, changing dates
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportesViewModelDiarioTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var ventaDao: VentaDao
    private lateinit var viewModel: ReportesViewModel

    private val opticaId = "optica-test-1"
    private val today = LocalDate.of(2026, 6, 23)
    private val yesterday = LocalDate.of(2026, 6, 22)

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
        every { ventaDao.getVentasByOpticaAndDateRange(opticaId, any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    /**
     * Launches persistent background collectors for all StateFlows so
     * that WhileSubscribed(5000) keeps the upstream active throughout
     * the test. Must be called inside runTest.
     */
    private fun TestScope.activateFlows() {
        backgroundScope.launch(testDispatcher) { viewModel.allDispensaciones.collect { } }
        backgroundScope.launch(testDispatcher) { viewModel.allVentasDelPeriodo.collect { } }
        backgroundScope.launch(testDispatcher) { viewModel.totalVendido.collect { } }
        backgroundScope.launch(testDispatcher) { viewModel.totalPagado.collect { } }
        backgroundScope.launch(testDispatcher) { viewModel.totalCobrado.collect { } }
        backgroundScope.launch(testDispatcher) { viewModel.cobrosPeriodo.collect { } }
    }

    @Test
    fun `only today dispensaciones pass the Diario filter`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today,    montoTotal = 100.0, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p2", fecha = yesterday, montoTotal = 200.0, opticaId = opticaId),
            DispensacionOptica(id = "d3", pacienteId = "p3", fecha = today,    montoTotal = 50.0,  opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("Should return only today's dispensaciones", 2, viewModel.allDispensaciones.value.size)
        assertTrue("d1 should be included", viewModel.allDispensaciones.value.any { it.id == "d1" })
        assertTrue("d3 should be included", viewModel.allDispensaciones.value.any { it.id == "d3" })
    }

    @Test
    fun `no dispensaciones on the selected date returns empty`() = runTest(testDispatcher) {
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(
            listOf(
                DispensacionOptica(id = "d1", pacienteId = "p1", fecha = yesterday, montoTotal = 100.0, opticaId = opticaId)
            )
        )

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertTrue("Should be empty when no dispensaciones on the selected date",
            viewModel.allDispensaciones.value.isEmpty())
    }

    @Test
    fun `totalVendido sums montoTotal and totalPagado sums pagos monto of daily filtered`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 150.0, montoPagado = 100.0, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p2", fecha = today, montoTotal = 75.0,  montoPagado = 75.0,  opticaId = opticaId),
            DispensacionOptica(id = "d3", pacienteId = "p3", fecha = yesterday, montoTotal = 300.0, montoPagado = 0.0, opticaId = opticaId)
        )
        val ventas = listOf(
            Venta(id = "v1", opticaId = opticaId, origen = "dispensacion", origenId = "d1",
                pacienteId = "p1", fecha = today, montoTotal = 150.0, estado = "Completado"),
            Venta(id = "v2", opticaId = opticaId, origen = "dispensacion", origenId = "d2",
                pacienteId = "p2", fecha = today, montoTotal = 75.0, estado = "Completado")
        )
        val pagos = listOf(
            Pago(id = "pg1", fecha = today, tipo = "Efectivo", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "pg2", fecha = today, tipo = "Efectivo", monto = 75.0, opticaId = opticaId, dispensacionId = "d2")
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagos)
        every { ventaDao.getVentasByOpticaAndDateRange(opticaId, today, today) } returns flowOf(ventas)

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("totalVendido should be sum of today's montoTotal", 225.0, viewModel.totalVendido.value, 0.001)
        assertEquals("totalPagado should be sum of today's pagos monto", 175.0, viewModel.totalPagado.value, 0.001)
    }

    @Test
    fun `totalVendido and totalPagado are zero when no dispensaciones on date`() = runTest(testDispatcher) {
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(
            listOf(
                DispensacionOptica(id = "d1", pacienteId = "p1", fecha = yesterday, montoTotal = 100.0, opticaId = opticaId)
            )
        )
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(emptyList())

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("totalVendido should be 0", 0.0, viewModel.totalVendido.value, 0.001)
        assertEquals("totalPagado should be 0", 0.0, viewModel.totalPagado.value, 0.001)
    }

    @Test
    fun `totalCobrado only includes payments on the selected date`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 100.0, opticaId = opticaId)
        )
        val pagos = listOf(
            Pago(id = "p1", fecha = today,    tipo = "Efectivo", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = yesterday, tipo = "Efectivo", monto = 50.0,  opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p3", fecha = today,    tipo = "Tarjeta",  monto = 75.0,  opticaId = opticaId, dispensacionId = "d1")
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        // totalCobrado should sum p1 (100) + p3 (75) = 175, excluding p2 (yesterday)
        assertEquals("totalCobrado should sum payments on the selected date only",
            175.0, viewModel.totalCobrado.value, 0.001)
    }

    @Test
    fun `cobrosPeriodo counts payments for dispensaciones from other dates`() = runTest(testDispatcher) {
        val todasLasDispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today,    montoTotal = 100.0, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p2", fecha = yesterday, montoTotal = 200.0, opticaId = opticaId)
        )
        val pagos = listOf(
            // Payment for today's dispensacion → venta del período (excluded from cobrosPeriodo)
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
            // Payment for yesterday's dispensacion collected today → cobro atrasado
            Pago(id = "p2", fecha = today, tipo = "Efectivo", monto = 50.0,  opticaId = opticaId, dispensacionId = "d2")
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(todasLasDispensaciones)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        // cobrosPeriodo: p1 is for today's disp → excluded (0), p2 is for yesterday's → 50
        assertEquals("cobrosPeriodo should include only payments for non-today dispensaciones",
            50.0, viewModel.cobrosPeriodo.value, 0.001)
    }

    @Test
    fun `ventasPeriodo equals totalCobrado minus cobrosPeriodo`() = runTest(testDispatcher) {
        val todasLasDispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today,    montoTotal = 100.0, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p2", fecha = yesterday, montoTotal = 200.0, opticaId = opticaId)
        )
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = today, tipo = "Efectivo", monto = 50.0,  opticaId = opticaId, dispensacionId = "d2")
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(todasLasDispensaciones)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        val totalCobrado = viewModel.totalCobrado.value      // 150.0 (p1 100 + p2 50)
        val cobrosPeriodo = viewModel.cobrosPeriodo.value     // 50.0 (p2 is cobro atrasado)
        val ventasPeriodo = totalCobrado - cobrosPeriodo      // 100.0 (p1 only)

        assertEquals("ventasPeriodo should be totalCobrado - cobrosPeriodo",
            100.0, ventasPeriodo, 0.001)
    }

    @Test
    fun `pagos sin dispensacionId se clasifican como cobrosPeriodo`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 100.0, opticaId = opticaId)
        )
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 75.0, opticaId = opticaId, dispensacionId = null)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("totalCobrado should include the payment", 75.0, viewModel.totalCobrado.value, 0.001)
        assertEquals("Payment without dispensacionId goes to cobrosPeriodo",
            75.0, viewModel.cobrosPeriodo.value, 0.001)
    }

    @Test
    fun `changing fechaDiario switches data to the new date`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today,    montoTotal = 100.0, montoPagado = 50.0,  opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p2", fecha = yesterday, montoTotal = 200.0, montoPagado = 200.0, opticaId = opticaId)
        )
        val pagos = listOf(
            Pago(id = "p1", fecha = today,    tipo = "Efectivo", monto = 50.0,  opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = yesterday, tipo = "Efectivo", monto = 200.0, opticaId = opticaId, dispensacionId = "d2")
        )
        val ventasToday = listOf(
            Venta(id = "v1", opticaId = opticaId, origen = "dispensacion", origenId = "d1",
                pacienteId = "p1", fecha = today, montoTotal = 100.0, estado = "Completado")
        )
        val ventasYesterday = listOf(
            Venta(id = "v2", opticaId = opticaId, origen = "dispensacion", origenId = "d2",
                pacienteId = "p2", fecha = yesterday, montoTotal = 200.0, estado = "Completado")
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagos)
        every { ventaDao.getVentasByOpticaAndDateRange(opticaId, today, today) } returns flowOf(ventasToday)
        every { ventaDao.getVentasByOpticaAndDateRange(opticaId, yesterday, yesterday) } returns flowOf(ventasYesterday)

        // Create with today
        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("On today: 1 dispensacion", 1, viewModel.allDispensaciones.value.size)
        assertEquals("On today: totalVendido = 100", 100.0, viewModel.totalVendido.value, 0.001)
        assertEquals("On today: totalCobrado = 50", 50.0, viewModel.totalCobrado.value, 0.001)

        // Switch to yesterday — persistent subscription auto-reactivates the combine
        viewModel.setFechaDiario(yesterday)
        advanceUntilIdle()

        assertEquals("On yesterday: 1 dispensacion", 1, viewModel.allDispensaciones.value.size)
        assertEquals("On yesterday: totalVendido = 200", 200.0, viewModel.totalVendido.value, 0.001)
        assertEquals("On yesterday: totalCobrado = 200", 200.0, viewModel.totalCobrado.value, 0.001)
    }

    @Test
    fun `all values are zero with empty data on daily period`() = runTest(testDispatcher) {
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(emptyList())

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertTrue(viewModel.allDispensaciones.value.isEmpty())
        assertEquals(0.0, viewModel.totalVendido.value, 0.001)
        assertEquals(0.0, viewModel.totalPagado.value, 0.001)
        assertEquals(0.0, viewModel.totalCobrado.value, 0.001)
        assertEquals(0.0, viewModel.cobrosPeriodo.value, 0.001)
    }

    // ===================================================================
    // Servicios Extra inclusion in totals
    // ===================================================================

    @Test
    fun `totalVendido includes ServicioExtra montoTotal when on the selected date`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 100.0, opticaId = opticaId)
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Lente de contacto", montoTotal = 40.0, aCuenta = 20.0, estado = "Entregado", fecha = today, opticaId = opticaId)
        )
        val ventas = listOf(
            Venta(id = "v1", opticaId = opticaId, origen = "dispensacion", origenId = "d1",
                pacienteId = "p1", fecha = today, montoTotal = 100.0, estado = "Completado"),
            Venta(id = "v2", opticaId = opticaId, origen = "servicio_extra", origenId = "s1",
                pacienteId = "p1", fecha = today, montoTotal = 40.0, estado = "Completado")
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)
        every { ventaDao.getVentasByOpticaAndDateRange(opticaId, today, today) } returns flowOf(ventas)

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("totalVendido should include disp + servicio montoTotal", 140.0, viewModel.totalVendido.value, 0.001)
    }

    @Test
    fun `totalPagado sums pagos monto for disp and servicio extra in period`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 100.0, montoPagado = 60.0, opticaId = opticaId)
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Lente de contacto", montoTotal = 40.0, aCuenta = 20.0, estado = "Entregado", fecha = today, opticaId = opticaId)
        )
        val pagos = listOf(
            Pago(id = "pg1", fecha = today, tipo = "Efectivo", monto = 60.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "pg2", fecha = today, tipo = "Efectivo", monto = 20.0, opticaId = opticaId, servicioExtraId = "s1")
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("totalPagado should sum pagos monto in the period", 80.0, viewModel.totalPagado.value, 0.001)
    }

    @Test
    fun `cobrosPeriodo does NOT count pago linked to servicioExtraId from the selected date`() = runTest(testDispatcher) {
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Lente de contacto", montoTotal = 40.0, aCuenta = 20.0, estado = "Entregado", fecha = today, opticaId = opticaId)
        )
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 20.0, opticaId = opticaId, servicioExtraId = "s1")
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("Payment for today's servicio extra should not be a cobro atrasado", 0.0, viewModel.cobrosPeriodo.value, 0.001)
    }

    @Test
    fun `cobrosPeriodo DOES count pago linked to servicioExtraId from a different date`() = runTest(testDispatcher) {
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Lente de contacto", montoTotal = 40.0, aCuenta = 20.0, estado = "Entregado", fecha = yesterday, opticaId = opticaId)
        )
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 20.0, opticaId = opticaId, servicioExtraId = "s1")
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("Payment for a past servicio extra should be a cobro atrasado", 20.0, viewModel.cobrosPeriodo.value, 0.001)
    }

    @Test
    fun `cobrosPeriodo includes orphan pago with neither dispensacionId nor servicioExtraId`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 75.0, opticaId = opticaId, dispensacionId = null, servicioExtraId = null)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("Orphan payment should go to cobrosPeriodo", 75.0, viewModel.cobrosPeriodo.value, 0.001)
    }

    @Test
    fun `cobrosPeriodo dispensacion date wins when both ids are set`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 100.0, opticaId = opticaId)
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Lente de contacto", montoTotal = 40.0, aCuenta = 20.0, estado = "Entregado", fecha = yesterday, opticaId = opticaId)
        )
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 50.0, opticaId = opticaId, dispensacionId = "d1", servicioExtraId = "s1")
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("Dispensacion in-range date should win over out-of-range servicio", 0.0, viewModel.cobrosPeriodo.value, 0.001)
    }

    // ===================================================================
    // T3.1 (REQ-A1): totalCobrado is independent of allDispensaciones
    // ===================================================================

    @Test
    fun `totalCobrado does not change when allDispensaciones emits new value`() = runTest(testDispatcher) {
        val dispensacionesBatch1 = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 100.0, opticaId = opticaId)
        )
        val dispensacionesBatch2 = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 100.0, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p2", fecha = today, montoTotal = 200.0, opticaId = opticaId)
        )
        val dispensacionesFlow = MutableStateFlow(dispensacionesBatch1)
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 50.0, opticaId = opticaId, dispensacionId = "d1")
        )

        every { repository.getAllDispensacionesForOptica(opticaId) } returns dispensacionesFlow
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        val cobradoBefore = viewModel.totalCobrado.value
        assertEquals("totalCobrado should be 50.0 initially", 50.0, cobradoBefore, 0.001)

        // Emit new dispensaciones — totalCobrado MUST NOT change
        dispensacionesFlow.value = dispensacionesBatch2
        advanceUntilIdle()

        assertEquals(
            "totalCobrado must remain unchanged when only dispensaciones change",
            cobradoBefore, viewModel.totalCobrado.value, 0.001
        )
    }

    // ===================================================================
    // T3.3 (REQ-D1): Diario query passes exact (today, today) to DAO
    // ===================================================================

    @Test
    fun `Diario period calls getPagosByDateRangeForOptica with exact today-today range`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 100.0, opticaId = opticaId)
        )
        val startSlot = slot<LocalDate>()
        val endSlot = slot<LocalDate>()
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every {
            repository.getPagosByDateRangeForOptica(capture(startSlot), capture(endSlot), any())
        } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager, ventaDao)
        activateFlows()
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals("DAO start must be today", today, startSlot.captured)
        assertEquals("DAO end must be today", today, endSlot.captured)
    }
}
