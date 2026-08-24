package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import com.example.optoapp.data.AppRoles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.junit.Assert.assertTrue
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
            DispensacionOptica(id = "d2", pacienteId = "p2", fecha = today, montoTotal = 200.0, opticaId = opticaId),
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Srv", montoTotal = 50.0, aCuenta = 50.0, estado = "Entregado", fecha = today, opticaId = opticaId),
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)

        viewModel = ReportesViewModel(repository, sessionManager)
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        // totalVendido from movimientos created from dispensaciones+servicios, filtered by period
        assertEquals(
            "totalVendido must sum all movimientos in period",
            350.0,
            viewModel.totalVendido.first(),
            0.001,
        )
    }

    @Test
    fun `totalVendido includes both dispensaciones and servicios`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 100.0, opticaId = opticaId),
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Srv", montoTotal = 80.0, aCuenta = 50.0, estado = "Entregado", fecha = today, opticaId = opticaId),
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)

        viewModel = ReportesViewModel(repository, sessionManager)
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals(
            "totalVendido must sum from both origins",
            180.0,
            viewModel.totalVendido.first(),
            0.001,
        )
    }

    @Test
    fun `empty period returns zero totalVendido`() = runTest(testDispatcher) {
        viewModel = ReportesViewModel(repository, sessionManager)
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals(
            "totalVendido must be 0 for empty period",
            0.0,
            viewModel.totalVendido.first(),
            0.001,
        )
    }
    @Test
    fun `cobrosPeriodo uses PagoEffect excluding Anulacion contributing zero`() = runTest(testDispatcher) {
        val yesterday = today.minusDays(1)
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = yesterday, montoTotal = 100.0, opticaId = opticaId),
        )
        val pagosEnPeriodo = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = today, tipo = "Anulación", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p3", fecha = today, tipo = "Reverso", monto = 25.0, opticaId = opticaId, dispensacionId = "d1", reversaPagoId = "p1"),
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagosEnPeriodo)

        viewModel = ReportesViewModel(repository, sessionManager)
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals(
            "cobrosPeriodo = Abono 100 + Reverso -25 + Anulación 0 = 75",
            75.0,
            viewModel.cobrosPeriodo.first(),
            0.001,
        )
    }
    @Test
    fun `cobrosPeriodo excludes dual-reference pago with servicio in period`() = runTest(testDispatcher) {
        val yesterday = today.minusDays(1)
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = yesterday, montoTotal = 100.0, opticaId = opticaId),
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Srv", montoTotal = 50.0, aCuenta = 50.0, estado = "Entregado", fecha = today, opticaId = opticaId),
        )
        // Pago with BOTH disp AND serv refs: disp is old, servicio is current
        val pagosEnPeriodo = listOf(
            Pago(
                id = "p1",
                fecha = today,
                tipo = "Abono",
                monto = 75.0,
                opticaId = opticaId,
                dispensacionId = "d1",
                servicioExtraId = "s1",
            ),
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagosEnPeriodo)

        viewModel = ReportesViewModel(repository, sessionManager)
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        // The servicio (s1) is in period, so this pago should NOT count as cobrosPeriodo
        assertEquals(
            "cobrosPeriodo must be 0 because servicio ref is in period",
            0.0,
            viewModel.cobrosPeriodo.first(),
            0.001,
        )
    }

    @Test
    fun `totalTransacciones counts movimientos in period`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 100.0, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p2", fecha = today, montoTotal = 200.0, opticaId = opticaId),
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Srv", montoTotal = 50.0, aCuenta = 50.0, estado = "Entregado", fecha = today, opticaId = opticaId),
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)

        viewModel = ReportesViewModel(repository, sessionManager)
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals(
            "totalTransacciones must count all movimientos in period",
            3,
            viewModel.totalTransacciones.first(),
        )
    }

    // ── pagosSumByDispensacion: reactive sum (abonos only, exclude Anulación) ──

    @Test
    fun `pagosSumByDispensacion nets via PagoEffect Reverso`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", dispensacionId = "d1", tipo = "Abono", monto = 100.0, opticaId = opticaId, fecha = today),
            Pago(id = "p2", dispensacionId = "d1", tipo = "Reverso", monto = 100.0, opticaId = opticaId, fecha = today, reversaPagoId = "p1"),
            Pago(id = "p3", dispensacionId = "d1", tipo = "Anulación", monto = 50.0, opticaId = opticaId, fecha = today),
        )
        every { repository.getAllPagosFlowForOptica(opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager)
        advanceUntilIdle()

        val result = viewModel.pagosSumByDispensacion.first()
        assertEquals("Abono+Reverso+Anulación effect = 0", 0.0, result["d1"] ?: 0.0, 0.001)
    }

    @Test
    fun `pagosSumByDispensacion only abono yields correct sum`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", dispensacionId = "d1", tipo = "Abono", monto = 100.0, opticaId = opticaId, fecha = today),
        )
        every { repository.getAllPagosFlowForOptica(opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager)
        advanceUntilIdle()

        val result = viewModel.pagosSumByDispensacion.first()
        assertEquals("d1 sum should be 100", 100.0, result["d1"] ?: 0.0, 0.001)
    }

    // ── aCuentaSumByServicio: reactive sum (abonos only, exclude Anulación) ──

    @Test
    fun `aCuentaSumByServicio nets via PagoEffect Reverso`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", servicioExtraId = "s1", tipo = "Abono", monto = 100.0, opticaId = opticaId, fecha = today),
            Pago(id = "p2", servicioExtraId = "s1", tipo = "Reverso", monto = 100.0, opticaId = opticaId, fecha = today, reversaPagoId = "p1"),
        )
        every { repository.getAllPagosFlowForOptica(opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager)
        advanceUntilIdle()

        val result = viewModel.aCuentaSumByServicio.first()
        assertEquals("Abono+Reverso effect = 0", 0.0, result["s1"] ?: 0.0, 0.001)
    }

    @Test
    fun `aCuentaSumByServicio only abono yields correct sum`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", servicioExtraId = "s1", tipo = "Abono", monto = 100.0, opticaId = opticaId, fecha = today),
        )
        every { repository.getAllPagosFlowForOptica(opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager)
        advanceUntilIdle()

        val result = viewModel.aCuentaSumByServicio.first()
        assertEquals("s1 sum should be 100", 100.0, result["s1"] ?: 0.0, 0.001)
    }

    // ── WU1: unique porCobrar KPI, Total chrome, honest loading, role gate ──

    @Test
    fun `headline KPIs include porCobrar exactly once without Pendiente`() {
        val kpis = ReportesUiPolicy.headlineKpiIds
        assertEquals("porCobrar must appear once", 1, kpis.count { it == "porCobrar" })
        assertFalse("Pendiente duplicate must be removed", "pendiente" in kpis)
        assertTrue("vendido required", "vendido" in kpis)
        assertTrue("cobrado required", "cobrado" in kpis)
    }

    @Test
    fun `headline KPIs stay unique when porCobrar is zero`() {
        val kpis = ReportesUiPolicy.headlineKpiIds
        assertEquals(1, kpis.count { it == "porCobrar" })
        assertFalse("pendiente" in kpis)
    }

    @Test
    fun `TOTAL_RANGE uses ISO-safe bounds not LocalDate MAX`() {
        val (start, end) = ReportesUiPolicy.TOTAL_RANGE
        assertEquals(LocalDate.of(1900, 1, 1), start)
        assertEquals(LocalDate.of(9999, 12, 31), end)
        assertTrue("end year must be 4-digit ISO-safe", end.year in 1..9999)
    }

    @Test
    fun `Total hides period chrome Diario shows it`() = runTest(testDispatcher) {
        viewModel = ReportesViewModel(repository, sessionManager)
        advanceUntilIdle()

        viewModel.setPeriodo("Total")
        advanceUntilIdle()
        assertFalse("Total must hide prev/next/picker chrome", viewModel.showsPeriodChrome.value)

        viewModel.setPeriodo("Diario")
        advanceUntilIdle()
        assertTrue("Diario must show period chrome", viewModel.showsPeriodChrome.value)
    }

    @Test
    fun `isLoading stays true until first data emission not delay alone`() = runTest(testDispatcher) {
        val neverEmits = MutableSharedFlow<List<DispensacionOptica>>()
        every { repository.getAllDispensacionesForOptica(opticaId) } returns neverEmits

        viewModel = ReportesViewModel(repository, sessionManager)
        advanceUntilIdle()

        assertTrue(
            "isLoading must stay true until first Flow emission (not delay-only clear)",
            viewModel.isLoading.value,
        )
    }

    @Test
    fun `isLoading false after first empty emission`() = runTest(testDispatcher) {
        viewModel = ReportesViewModel(repository, sessionManager)
        advanceUntilIdle()

        assertFalse(
            "empty after load: isLoading off once flows emit",
            viewModel.isLoading.value,
        )
    }

    @Test
    fun `failing canViewBiAndReports yields restricted without totals`() {
        assertFalse(AppRoles.canViewBiAndReports("asesor"))
        val access = ReportesUiPolicy.resolveAccess("asesor")
        assertTrue("unauthorized must be restricted", access.isRestricted)
        assertFalse("unauthorized must not show totals", access.showTotals)
    }

    @Test
    fun `passing canViewBiAndReports allows totals`() {
        assertTrue(AppRoles.canViewBiAndReports("admin"))
        val access = ReportesUiPolicy.resolveAccess("admin")
        assertFalse(access.isRestricted)
        assertTrue(access.showTotals)
    }

    @Test
    fun `porCobrar sums saldo pendiente coerced at least zero from allMovimientosDelPeriodo`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 100.0, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p2", fecha = today, montoTotal = 200.0, opticaId = opticaId),
        )
        // Pago covers 60 of d1; d2 has no payment
        val pagos = listOf(
            Pago(id = "pago1", dispensacionId = "d1", tipo = "Abono", monto = 60.0, opticaId = opticaId, fecha = today),
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getAllPagosFlowForOptica(opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager)
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        // d1: 100 - 60 = 40, d2: 200 - 0 = 200 → total 240
        assertEquals(
            "porCobrar must equal sum of (montoTotal - montoPagado).coerceAtLeast(0)",
            240.0,
            viewModel.porCobrar.first(),
            0.001,
        )
    }
}
