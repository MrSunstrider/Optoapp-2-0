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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CierreCajaViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: CierreCajaViewModel

    private val today = LocalDate.of(2026, 6, 17)
    private val yesterday = LocalDate.of(2026, 6, 16)
    private val pastDate = LocalDate.of(2026, 6, 15)
    private val opticaId = "optica-1"

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
        every { sessionManager.userTimeZone } returns flowOf(null)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // -----------------------------------------------------------------------
    // Test 1: ventasHoy sums pagos collected on 'today' for the optica
    // spec: two pagos on 2026-06-17 for optica-1 (100+50), one on 2026-06-16 → ventasHoy=150.0
    // -----------------------------------------------------------------------
    @Test
    fun ventasHoy_sums_pagos_cobrados_for_date_and_optica() = runTest(testDispatcher) {
        val pagosHoy = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 100.0, opticaId = opticaId),
            Pago(id = "p2", fecha = today, tipo = "Tarjeta",  monto = 50.0,  opticaId = opticaId)
        )
        // Dispensaciones created today — all pagos go to ventasHoy
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, opticaId = opticaId)
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagosHoy)
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(150.0, viewModel.uiState.value.ventasHoy, 0.001)
    }

    // -----------------------------------------------------------------------
    // Test 2: cobrosAtrasados sums pagos from older dispensaciones collected today
    // -----------------------------------------------------------------------
    @Test
    fun cobrosAtrasados_sums_pagos_from_older_dispensaciones_collected_today() = runTest(testDispatcher) {
        val pagos = listOf(
            // Pago collected today, dispensacion from yesterday → cobrosAtrasados
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 75.0, opticaId = opticaId, dispensacionId = "d1"),
            // Pago collected today, dispensacion from today → ventasHoy
            Pago(id = "p2", fecha = today, tipo = "Efectivo", monto = 100.0, opticaId = opticaId, dispensacionId = "d2")
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = yesterday, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "pac2", fecha = today,     opticaId = opticaId)
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(75.0, viewModel.uiState.value.cobrosAtrasados, 0.001)
    }

    // -----------------------------------------------------------------------
    // Test 3: saldoPendiente = totalDispensaciones - ventasHoy
    // spec: dispensacion montoTotal=300, only 150 paid → saldoPendiente=150.0
    // -----------------------------------------------------------------------
    @Test
    fun saldoPendiente_equals_totalGeneral_minus_ventasHoy() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = today, tipo = "Tarjeta",  monto = 50.0,  opticaId = opticaId, dispensacionId = "d1")
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, montoTotal = 300.0, opticaId = opticaId)
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(150.0, viewModel.uiState.value.saldoPendiente, 0.001)
    }

    // -----------------------------------------------------------------------
    // Servicios Extra desglose and dual-ID classification
    // -----------------------------------------------------------------------

    @Test
    fun totalVentasHoy_sums_only_dispensaciones() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, montoTotal = 300.0, opticaId = opticaId)
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Servicio", montoTotal = 150.0, aCuenta = 50.0, estado = "Entregado", fecha = today, opticaId = opticaId)
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(emptyList())
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("totalVentasHoy must remain disp-only", 300.0, viewModel.uiState.value.totalVentasHoy, 0.001)
    }

    @Test
    fun totalServiciosExtra_includes_today_servicios() = runTest(testDispatcher) {
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Servicio 1", montoTotal = 100.0, aCuenta = 50.0, estado = "Entregado", fecha = today, opticaId = opticaId),
            ServicioExtra(id = "s2", descripcion = "Servicio 2", montoTotal = 50.0, aCuenta = 25.0, estado = "Entregado", fecha = yesterday, opticaId = opticaId)
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(emptyList())
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("totalServiciosExtra must sum today's servicios", 100.0, viewModel.uiState.value.totalServiciosExtra, 0.001)
        assertEquals("serviciosExtraHoy must contain only today's servicios", 1, viewModel.uiState.value.serviciosExtraHoy.size)
    }

    @Test
    fun totalGeneral_equals_totalVentasHoy_plus_totalServiciosExtra() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, montoTotal = 300.0, opticaId = opticaId)
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Servicio", montoTotal = 150.0, aCuenta = 50.0, estado = "Entregado", fecha = today, opticaId = opticaId)
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(emptyList())
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("totalGeneral must be sum of both totals", 450.0, viewModel.uiState.value.totalGeneral, 0.001)
    }

    @Test
    fun saldoPendiente_includes_serviciosExtra_in_totalGeneral() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 200.0, opticaId = opticaId, dispensacionId = "d1")
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, montoTotal = 300.0, opticaId = opticaId)
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Servicio", montoTotal = 150.0, aCuenta = 50.0, estado = "Entregado", fecha = today, opticaId = opticaId)
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("saldoPendiente must be totalGeneral - ventasHoy", 250.0, viewModel.uiState.value.saldoPendiente, 0.001)
    }

    @Test
    fun ventasHoy_includes_pago_linked_to_today_servicioExtra() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 75.0, opticaId = opticaId, servicioExtraId = "s1")
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Servicio", montoTotal = 100.0, aCuenta = 75.0, estado = "Entregado", fecha = today, opticaId = opticaId)
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Payment for today's servicio extra must go to ventasHoy", 75.0, viewModel.uiState.value.ventasHoy, 0.001)
        assertEquals("No cobros atrasados for same-day servicio payment", 0.0, viewModel.uiState.value.cobrosAtrasados, 0.001)
    }

    @Test
    fun cobrosAtrasados_includes_pago_linked_to_older_servicioExtra() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 75.0, opticaId = opticaId, servicioExtraId = "s1")
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Servicio", montoTotal = 100.0, aCuenta = 75.0, estado = "Entregado", fecha = yesterday, opticaId = opticaId)
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(servicios)

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Payment for older servicio extra must go to cobrosAtrasados", 75.0, viewModel.uiState.value.cobrosAtrasados, 0.001)
        assertEquals("ventasHoy must not include older servicio payment", 0.0, viewModel.uiState.value.ventasHoy, 0.001)
    }

    @Test
    fun orphan_pago_contributes_to_ventasHoy() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 80.0, opticaId = opticaId, dispensacionId = null, servicioExtraId = null)
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(emptyList())

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Orphan payment must stay in ventasHoy", 80.0, viewModel.uiState.value.ventasHoy, 0.001)
        assertEquals("Orphan payment must not be a cobro atrasado", 0.0, viewModel.uiState.value.cobrosAtrasados, 0.001)
    }

    // -----------------------------------------------------------------------
    // Test 6: totalRecaudado = ventasHoy + cobrosAtrasados
    // spec: ventasHoy=150.0, cobrosAtrasados=50.0 → totalRecaudado=200.0
    // -----------------------------------------------------------------------
    @Test
    fun totalRecaudado_equals_ventasHoy_plus_cobrosAtrasados() = runTest(testDispatcher) {
        val pagos = listOf(
            // ventasHoy: dispensacion created today
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = today, tipo = "Tarjeta",  monto = 50.0,  opticaId = opticaId, dispensacionId = "d1"),
            // cobrosAtrasados: dispensacion created yesterday
            Pago(id = "p3", fecha = today, tipo = "Efectivo", monto = 50.0,  opticaId = opticaId, dispensacionId = "d2")
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today,     opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "pac2", fecha = yesterday, opticaId = opticaId)
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val totalRecaudado = state.ventasHoy + state.cobrosAtrasados
        assertEquals(200.0, totalRecaudado, 0.001)
    }

    // -----------------------------------------------------------------------
    // Test 8: getTotalesPorMetodo groups pagos by metodoPago (title-case keys)
    // -----------------------------------------------------------------------
    @Test
    fun getTotalesPorMetodo_groups_pagos_by_metodoPago() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "abono", monto = 100.0, opticaId = opticaId, metodoPago = "Efectivo"),
            Pago(id = "p2", fecha = today, tipo = "abono", monto = 50.0,  opticaId = opticaId, metodoPago = "Efectivo"),
            Pago(id = "p3", fecha = today, tipo = "abono", monto = 200.0, opticaId = opticaId, metodoPago = "Tarjeta"),
            Pago(id = "p4", fecha = today, tipo = "abono", monto = 75.0,  opticaId = opticaId, metodoPago = "Móvil")
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        val totales = viewModel.getTotalesPorMetodo()
        assertEquals("Efectivo total must be 150.0", 150.0, totales["Efectivo"] ?: 0.0, 0.001)
        assertEquals("Tarjeta total must be 200.0", 200.0, totales["Tarjeta"] ?: 0.0, 0.001)
        assertEquals("Móvil total must be 75.0",    75.0,  totales["Móvil"]   ?: 0.0, 0.001)
    }

    // -----------------------------------------------------------------------
    // Test 9: getTotalesPorMetodo returns empty map when no pagos exist
    // -----------------------------------------------------------------------
    @Test
    fun getTotalesPorMetodo_empty_pagos_returns_empty_map() = runTest(testDispatcher) {
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(emptyList())
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("getTotalesPorMetodo must return empty map when no pagos exist",
            viewModel.getTotalesPorMetodo().isEmpty())
    }

    // -----------------------------------------------------------------------
    // Test 10: setFecha updates the fecha field in uiState
    // -----------------------------------------------------------------------
    @Test
    fun setFecha_updates_fecha_in_uiState() = runTest(testDispatcher) {
        every { repository.getPagosByDateRangeForOptica(any(), any(), any()) } returns flowOf(emptyList())
        every { repository.getAllDispensacionesForOptica(any()) } returns flowOf(emptyList())

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(yesterday)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("setFecha must update fecha in uiState", yesterday, viewModel.uiState.value.fecha)
    }

    // -----------------------------------------------------------------------
    // Test 11: empty pagos list results in zero amounts and an empty pagos list
    // -----------------------------------------------------------------------
    @Test
    fun empty_pagos_results_in_zero_amounts_and_empty_list() = runTest(testDispatcher) {
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(emptyList())
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0.0, state.ventasHoy, 0.001)
        assertEquals(0.0, state.cobrosAtrasados, 0.001)
        assertTrue("pagos must be empty when no pagos returned", state.pagos.isEmpty())
    }

    // -----------------------------------------------------------------------
    // Test 12: getTotalesPorMetodo normalizes "Sin especificar" into ""
    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    // Fix 1: Anulación pagos must be included (not skipped) so negative monto
    // offsets the original payment. ventasHoy should be 0 for same-day refund.
    // spec: dispensacion with pago 100 + Anulación -100 on same day → ventasHoy = 0.0
    // -----------------------------------------------------------------------
    @Test
    fun anulacion_pago_offsets_ventasHoy_same_day() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = today, tipo = "Anulación", monto = -100.0, opticaId = opticaId, dispensacionId = "d1")
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, montoTotal = 100.0, opticaId = opticaId)
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("ventasHoy must be 0 (100 + -100)", 0.0, viewModel.uiState.value.ventasHoy, 0.001)
    }

    @Test
    fun getTotalesPorMetodo_groupsSinEspecificarWithEmptyString() = runTest(testDispatcher) {
        val pagosHoy = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 100.0, metodoPago = "Efectivo", opticaId = opticaId),
            Pago(id = "p2", fecha = today, tipo = "Efectivo", monto = 50.0,  metodoPago = "Sin especificar", opticaId = opticaId),
            Pago(id = "p3", fecha = today, tipo = "Efectivo", monto = 30.0,  metodoPago = "", opticaId = opticaId),
            Pago(id = "p4", fecha = today, tipo = "Tarjeta", monto = 200.0, metodoPago = "Tarjeta", opticaId = opticaId)
        )
        val dispensaciones = emptyList<DispensacionOptica>()

        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagosHoy)
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = CierreCajaViewModel(repository, sessionManager)
        viewModel.setFecha(today)
        testDispatcher.scheduler.advanceUntilIdle()

        val totales = viewModel.getTotalesPorMetodo()

        // "Sin especificar" (50) + "" (30) should be grouped together under ""
        assertEquals(80.0, totales[""] ?: 0.0, 0.001)
        assertEquals(100.0, totales["Efectivo"] ?: 0.0, 0.001)
        assertEquals(200.0, totales["Tarjeta"] ?: 0.0, 0.001)
        assertEquals("getTotalesPorMetodo should have exactly 3 keys", 3, totales.size)
    }
}
