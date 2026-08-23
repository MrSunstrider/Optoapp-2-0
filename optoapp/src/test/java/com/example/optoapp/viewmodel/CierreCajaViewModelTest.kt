package com.example.optoapp.viewmodel

import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests para [CierreCajaViewModel] usando mockk puro (sin Robolectric ni Room).
 *
 * Cubre todos los escenarios del delta spec fix-cierre-caja:
 * REQ-CIERRE-001 a REQ-CIERRE-006.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CierreCajaViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager

    private val today = LocalDate.of(2026, 6, 17)
    private val yesterday = LocalDate.of(2026, 6, 16)
    private val tomorrow = LocalDate.of(2026, 6, 18)
    private val opticaId = "optica-1"
    private val otherOpticaId = "optica-2"

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
        every { sessionManager.opticaRol } returns flowOf("admin")
        every { sessionManager.userTimeZone } returns flowOf(null)
        // Default: empty data for all queries
        every { repository.getPagosByDateRangeForOptica(any(), any(), any()) } returns flowOf(emptyList())
        every { repository.getDispensacionesByDateRangeForOptica(any(), any(), any()) } returns flowOf(emptyList())
        every { repository.getServiciosByDateRangeForOptica(any(), any(), any()) } returns flowOf(emptyList())
        every { repository.pacientesFlowForOptica(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    /**
     * Creates the ViewModel, sets fecha to today, and waits for first non-loading emission.
     */
    private suspend fun createViewModel(): CierreCajaViewModel {
        val vm = CierreCajaViewModel(repository, sessionManager)
        vm.setFecha(today)
        vm.uiState.first { !it.isLoading }
        return vm
    }
    // REQ-CIERRE-001: Saldo Pendiente Correcto

    @Test
    fun `saldoPendiente full historical payment yields zero`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(
                id = "d1",
                pacienteId = "pac1",
                fecha = today,
                montoTotal = 300.0,
                montoPagado = 300.0,
                opticaId = opticaId,
            ),
        )
        val servicios = listOf(
            ServicioExtra(
                id = "s1",
                descripcion = "Servicio",
                montoTotal = 150.0,
                aCuenta = 150.0,
                estado = "Entregado",
                fecha = today,
                opticaId = opticaId,
            ),
        )
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(servicios)

        val vm = createViewModel()

        assertEquals(0.0, vm.uiState.value.saldoPendiente, 0.001)
    }

    @Test
    fun `saldoPendiente partial payment from prior day`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(
                id = "d1",
                pacienteId = "pac1",
                fecha = today,
                montoTotal = 300.0,
                montoPagado = 200.0,
                opticaId = opticaId,
            ),
        )
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)

        val vm = createViewModel()

        assertEquals(100.0, vm.uiState.value.saldoPendiente, 0.001)
    }

    @Test
    fun `saldoPendiente same day full payment`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(
                id = "d1",
                pacienteId = "pac1",
                fecha = today,
                montoTotal = 300.0,
                montoPagado = 300.0,
                opticaId = opticaId,
            ),
        )
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)

        val vm = createViewModel()

        assertEquals(0.0, vm.uiState.value.saldoPendiente, 0.001)
    }

    @Test
    fun `saldoPendiente empty data returns zero`() = runTest(testDispatcher) {
        val vm = createViewModel()

        assertEquals(0.0, vm.uiState.value.saldoPendiente, 0.001)
        assertEquals(0.0, vm.uiState.value.totalGeneral, 0.001)
    }

    @Test
    fun `saldoPendiente includes entity tracked payments for disp and serv`() = runTest(testDispatcher) {
        // S/300 disp, montoPagado=100 -> pendiente 200
        // S/100 serv, aCuenta=75 -> pendiente 25
        val dispensaciones = listOf(
            DispensacionOptica(
                id = "d1",
                pacienteId = "pac1",
                fecha = today,
                montoTotal = 300.0,
                montoPagado = 100.0,
                opticaId = opticaId,
            ),
        )
        val servicios = listOf(
            ServicioExtra(
                id = "s1",
                descripcion = "Servicio",
                montoTotal = 100.0,
                aCuenta = 75.0,
                estado = "Entregado",
                fecha = today,
                opticaId = opticaId,
            ),
        )
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(servicios)

        val vm = createViewModel()

        assertEquals(225.0, vm.uiState.value.saldoPendiente, 0.001)
    }

    @Test
    fun `saldoPendiente and totalGeneral exclude anulated dispensaciones`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(
                id = "d1",
                pacienteId = "pac1",
                fecha = today,
                montoTotal = 300.0,
                montoPagado = 300.0,
                estadoEntrega = "Anulado",
                opticaId = opticaId,
            ),
            DispensacionOptica(
                id = "d2",
                pacienteId = "pac2",
                fecha = today,
                montoTotal = 200.0,
                montoPagado = 50.0,
                opticaId = opticaId,
            ),
        )
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)

        val vm = createViewModel()

        assertEquals(150.0, vm.uiState.value.saldoPendiente, 0.001)
        assertEquals(200.0, vm.uiState.value.totalGeneral, 0.001)
        assertEquals(1, vm.uiState.value.dispensacionesHoy.size)
    }
    // Ventas Hoy and Cobros Atrasados

    @Test
    fun `ventasHoy sums pagos collected today for today dispensaciones`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = today, tipo = "Abono", monto = 50.0, opticaId = opticaId, dispensacionId = "d1"),
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, opticaId = opticaId),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)

        val vm = createViewModel()

        assertEquals(150.0, vm.uiState.value.ventasHoy, 0.001)
    }

    @Test
    fun `cobrosAtrasados sums pagos from older dispensaciones collected today`() = runTest(testDispatcher) {
        // Batch fetch: pago d1 references disp outside date range -> ViewModel fetches via getDispensacionesByIds
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 75.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = today, tipo = "Abono", monto = 100.0, opticaId = opticaId, dispensacionId = "d2"),
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d2", pacienteId = "pac1", fecha = today, opticaId = opticaId),
        )
        val extraDisp = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac2", fecha = yesterday, opticaId = opticaId),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)
        coEvery { repository.getDispensacionesByIds(listOf("d1"), opticaId) } returns extraDisp

        val vm = createViewModel()

        assertEquals(75.0, vm.uiState.value.cobrosAtrasados, 0.001)
        assertEquals(100.0, vm.uiState.value.ventasHoy, 0.001)
    }

    @Test
    fun `totalDispensacionesHoy sums only dispensaciones`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, montoTotal = 300.0, opticaId = opticaId),
        )
        val servicios = listOf(
            ServicioExtra(
                id = "s1",
                descripcion = "Servicio",
                montoTotal = 150.0,
                aCuenta = 50.0,
                estado = "Entregado",
                fecha = today,
                opticaId = opticaId,
            ),
        )
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(servicios)

        val vm = createViewModel()

        assertEquals("totalDispensacionesHoy must remain disp-only", 300.0, vm.uiState.value.totalDispensacionesHoy, 0.001)
    }

    @Test
    fun `totalServiciosExtra includes today servicios`() = runTest(testDispatcher) {
        val servicios = listOf(
            ServicioExtra(
                id = "s1",
                descripcion = "Servicio 1",
                montoTotal = 100.0,
                aCuenta = 50.0,
                estado = "Entregado",
                fecha = today,
                opticaId = opticaId,
            ),
        )
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(servicios)

        val vm = createViewModel()

        assertEquals("totalServiciosExtra must sum today's servicios", 100.0, vm.uiState.value.totalServiciosExtra, 0.001)
        assertEquals("serviciosExtraHoy must contain only today's servicios", 1, vm.uiState.value.serviciosExtraHoy.size)
    }

    @Test
    fun `totalGeneral equals totalDispensacionesHoy plus totalServiciosExtra`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, montoTotal = 300.0, opticaId = opticaId),
        )
        val servicios = listOf(
            ServicioExtra(
                id = "s1",
                descripcion = "Servicio",
                montoTotal = 150.0,
                aCuenta = 50.0,
                estado = "Entregado",
                fecha = today,
                opticaId = opticaId,
            ),
        )
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(servicios)

        val vm = createViewModel()

        assertEquals("totalGeneral must be sum of both totals", 450.0, vm.uiState.value.totalGeneral, 0.001)
    }
    // Pago classification: dispensacion-linked and servicio-linked

    @Test
    fun `ventasHoy includes pago linked to today servicio extra`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 75.0, opticaId = opticaId, servicioExtraId = "s1"),
        )
        val servicios = listOf(
            ServicioExtra(
                id = "s1",
                descripcion = "Servicio",
                montoTotal = 100.0,
                aCuenta = 75.0,
                estado = "Entregado",
                fecha = today,
                opticaId = opticaId,
            ),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(servicios)

        val vm = createViewModel()

        assertEquals("Payment for today's servicio extra must go to ventasHoy", 75.0, vm.uiState.value.ventasHoy, 0.001)
        assertEquals("No cobros atrasados for same-day servicio payment", 0.0, vm.uiState.value.cobrosAtrasados, 0.001)
    }

    @Test
    fun `cobrosAtrasados includes pago linked to older servicio extra`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 75.0, opticaId = opticaId, servicioExtraId = "s1"),
        )
        val servicios = listOf(
            ServicioExtra(
                id = "s1",
                descripcion = "Old Servicio",
                montoTotal = 100.0,
                aCuenta = 75.0,
                estado = "Entregado",
                fecha = yesterday,
                opticaId = opticaId,
            ),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(servicios)

        val vm = createViewModel()

        assertEquals("Payment for older servicio extra must go to cobrosAtrasados", 75.0, vm.uiState.value.cobrosAtrasados, 0.001)
        assertEquals("ventasHoy must not include older servicio payment", 0.0, vm.uiState.value.ventasHoy, 0.001)
    }

    @Test
    fun `orphan pago contributes to ventasHoy`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(
                id = "p1",
                fecha = today,
                tipo = "Abono",
                monto = 80.0,
                opticaId = opticaId,
                dispensacionId = null,
                servicioExtraId = null,
            ),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)

        val vm = createViewModel()

        assertEquals("Orphan payment must stay in ventasHoy", 80.0, vm.uiState.value.ventasHoy, 0.001)
        assertEquals("Orphan payment must not be a cobro atrasado", 0.0, vm.uiState.value.cobrosAtrasados, 0.001)
    }

    @Test
    fun `totalRecaudado equals ventasHoy plus cobrosAtrasados`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = today, tipo = "Abono", monto = 50.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p3", fecha = today, tipo = "Abono", monto = 50.0, opticaId = opticaId, dispensacionId = "d2"),
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, opticaId = opticaId),
        )
        val extraDisp = listOf(
            DispensacionOptica(id = "d2", pacienteId = "pac2", fecha = yesterday, opticaId = opticaId),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)
        coEvery { repository.getDispensacionesByIds(listOf("d2"), opticaId) } returns extraDisp

        val vm = createViewModel()

        val state = vm.uiState.value
        val totalRecaudado = state.ventasHoy + state.cobrosAtrasados
        assertEquals(200.0, totalRecaudado, 0.001)
    }
    // REQ-CIERRE-005: Future-date classification

    @Test
    fun `future dated dispensacion pago excluded from ventasHoy`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
        )
        // Dispensacion with future fecha — not returned by date-range query, fetched via batch
        val extraDisp = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = tomorrow, opticaId = opticaId),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(emptyList())
        coEvery { repository.getDispensacionesByIds(listOf("d1"), opticaId) } returns extraDisp

        val vm = createViewModel()

        assertEquals("ventasHoy must exclude future-dated disp payment", 0.0, vm.uiState.value.ventasHoy, 0.001)
    }

    @Test
    fun `future dated servicio pago excluded from ventasHoy`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 100.0, opticaId = opticaId, servicioExtraId = "s1"),
        )
        val extraServ = listOf(
            ServicioExtra(
                id = "s1",
                descripcion = "Futuro",
                montoTotal = 200.0,
                aCuenta = 0.0,
                estado = "Pendiente",
                fecha = tomorrow,
                opticaId = opticaId,
            ),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(emptyList())
        coEvery { repository.getServiciosByIds(listOf("s1"), opticaId) } returns extraServ

        val vm = createViewModel()

        assertEquals("ventasHoy must exclude future-dated servicio payment", 0.0, vm.uiState.value.ventasHoy, 0.001)
    }
    // Anulacion pagos

    @Test
    fun `Reverso pago offsets ventasHoy same day via PagoEffect`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = today, tipo = "Reverso", monto = 100.0, opticaId = opticaId, dispensacionId = "d1", reversaPagoId = "p1"),
            Pago(id = "p3", fecha = today, tipo = "Anulación", monto = 50.0, opticaId = opticaId, dispensacionId = "d1"),
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, montoTotal = 100.0, opticaId = opticaId),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)

        val vm = createViewModel()

        assertEquals("ventasHoy must be 0 (Abono+Reverso; Anulación effect 0)", 0.0, vm.uiState.value.ventasHoy, 0.001)
    }
    // getTotalesPorMetodo

    @Test
    fun `getTotalesPorMetodo groups pagos by metodoPago`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 100.0, opticaId = opticaId, metodoPago = "Efectivo"),
            Pago(id = "p2", fecha = today, tipo = "Abono", monto = 50.0, opticaId = opticaId, metodoPago = "Efectivo"),
            Pago(id = "p3", fecha = today, tipo = "Abono", monto = 200.0, opticaId = opticaId, metodoPago = "Tarjeta"),
            Pago(id = "p4", fecha = today, tipo = "Abono", monto = 75.0, opticaId = opticaId, metodoPago = "Móvil"),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)

        val vm = createViewModel()

        val totales = vm.getTotalesPorMetodo()
        assertEquals("Efectivo total must be 150.0", 150.0, totales["Efectivo"] ?: 0.0, 0.001)
        assertEquals("Tarjeta total must be 200.0", 200.0, totales["Tarjeta"] ?: 0.0, 0.001)
        assertEquals("Móvil total must be 75.0", 75.0, totales["Móvil"] ?: 0.0, 0.001)
    }

    @Test
    fun `getTotalesPorMetodo empty pagos returns empty map`() = runTest(testDispatcher) {
        val vm = createViewModel()

        assertTrue(
            "getTotalesPorMetodo must return empty map when no pagos exist",
            vm.getTotalesPorMetodo().isEmpty(),
        )
    }

    @Test
    fun `getTotalesPorMetodo normalizes Sin especificar into empty string`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 100.0, metodoPago = "Efectivo", opticaId = opticaId),
            Pago(id = "p2", fecha = today, tipo = "Abono", monto = 50.0, metodoPago = "Sin especificar", opticaId = opticaId),
            Pago(id = "p3", fecha = today, tipo = "Abono", monto = 30.0, metodoPago = "", opticaId = opticaId),
            Pago(id = "p4", fecha = today, tipo = "Abono", monto = 200.0, metodoPago = "Tarjeta", opticaId = opticaId),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)

        val vm = createViewModel()

        val totales = vm.getTotalesPorMetodo()
        assertEquals(80.0, totales[""] ?: 0.0, 0.001)
        assertEquals(100.0, totales["Efectivo"] ?: 0.0, 0.001)
        assertEquals(200.0, totales["Tarjeta"] ?: 0.0, 0.001)
        assertEquals("getTotalesPorMetodo should have exactly 3 keys", 3, totales.size)
    }
    // setFecha and empty state

    @Test
    fun `setFecha updates fecha in uiState`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.setFecha(yesterday)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(yesterday, vm.uiState.value.fecha)
    }

    @Test
    fun `empty pagos results in zero amounts and empty pagos list`() = runTest(testDispatcher) {
        val vm = createViewModel()

        val state = vm.uiState.value
        assertEquals(0.0, state.ventasHoy, 0.001)
        assertEquals(0.0, state.cobrosAtrasados, 0.001)
        assertTrue("pagos must be empty when no pagos returned", state.pagos.isEmpty())
    }
    // REQ-CIERRE-006: Multi-optica isolation

    @Test
    fun `cross optica isolation returns empty for other optica`() = runTest(testDispatcher) {
        // Seed data for optica A repos, but session asks for optica B
        val dispensaciones = listOf(
            DispensacionOptica(
                id = "d1",
                pacienteId = "pac1",
                fecha = today,
                montoTotal = 300.0,
                montoPagado = 100.0,
                opticaId = opticaId,
            ),
        )
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)
        // For optica B, return empty
        every { repository.getDispensacionesByDateRangeForOptica(today, today, otherOpticaId) } returns flowOf(emptyList())
        every { repository.getServiciosByDateRangeForOptica(today, today, otherOpticaId) } returns flowOf(emptyList())
        every { repository.getPagosByDateRangeForOptica(today, today, otherOpticaId) } returns flowOf(emptyList())

        // Switch session to other optica
        every { sessionManager.opticaId } returns flowOf(otherOpticaId)

        val vm = CierreCajaViewModel(repository, sessionManager)
        vm.setFecha(today)
        vm.uiState.first { !it.isLoading }

        assertEquals("dispensacionesHoy must be empty for other optica", 0, vm.uiState.value.dispensacionesHoy.size)
        assertEquals("serviciosExtraHoy must be empty for other optica", 0, vm.uiState.value.serviciosExtraHoy.size)
        assertEquals("totalGeneral must be 0 for other optica", 0.0, vm.uiState.value.totalGeneral, 0.001)
        assertEquals("saldoPendiente must be 0 for other optica", 0.0, vm.uiState.value.saldoPendiente, 0.001)
    }

    @Test
    fun `same optica returns correct data excluding other optica`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(
                id = "d1",
                pacienteId = "pac1",
                fecha = today,
                montoTotal = 300.0,
                montoPagado = 100.0,
                opticaId = opticaId,
            ),
        )
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)

        val vm = createViewModel()

        assertEquals("Only optica A dispensaciones should be visible", 1, vm.uiState.value.dispensacionesHoy.size)
        assertEquals("d1", vm.uiState.value.dispensacionesHoy[0].id)
        assertEquals("totalGeneral must reflect only optica A data", 300.0, vm.uiState.value.totalGeneral, 0.001)
    }
    // REQ-CIERRE-002: Per-item Pagado from entity fields

    @Test
    fun `dispensacion per-item shows montoPagado as Pagado and correct Saldo`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(
                id = "d1",
                pacienteId = "pac1",
                fecha = today,
                montoTotal = 300.0,
                montoPagado = 250.0,
                opticaId = opticaId,
            ),
        )
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)

        val vm = createViewModel()

        val disp = vm.uiState.value.dispensacionesHoy.find { it.id == "d1" }
        assertNotNull(disp)
        assertEquals(250.0, disp!!.montoPagado, 0.001)
    }

    @Test
    fun `servicio per-item shows aCuenta as Pagado and correct Saldo`() = runTest(testDispatcher) {
        val servicios = listOf(
            ServicioExtra(
                id = "s1",
                descripcion = "Pagado",
                montoTotal = 80.0,
                aCuenta = 80.0,
                estado = "Entregado",
                fecha = today,
                opticaId = opticaId,
            ),
        )
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(servicios)

        val vm = createViewModel()

        val serv = vm.uiState.value.serviciosExtraHoy.find { it.id == "s1" }
        assertNotNull(serv)
        assertEquals(80.0, serv!!.aCuenta, 0.001)
    }
    // Anulado ServicioExtra exclusion from totals

    @Test
    fun `anulado servicio excluido de totalServiciosExtra y saldoPendiente`() = runTest(testDispatcher) {
        val servicios = listOf(
            ServicioExtra(
                id = "s1",
                descripcion = "Anulado",
                montoTotal = 200.0,
                aCuenta = 100.0,
                estado = "Anulado",
                fecha = today,
                opticaId = opticaId,
            ),
            ServicioExtra(
                id = "s2",
                descripcion = "Activo",
                montoTotal = 150.0,
                aCuenta = 50.0,
                estado = "Entregado",
                fecha = today,
                opticaId = opticaId,
            ),
        )
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(servicios)

        val vm = createViewModel()

        assertEquals(150.0, vm.uiState.value.totalServiciosExtra, 0.001)
        assertEquals(100.0, vm.uiState.value.saldoPendiente, 0.001)
        assertEquals(1, vm.uiState.value.serviciosExtraHoy.size)
    }
    // Error handling

    @Test
    fun `error en batch fetch emite errorMessage y isLoading false`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(
                id = "p1",
                fecha = today,
                tipo = "Abono",
                monto = 50.0,
                opticaId = opticaId,
                dispensacionId = "d1",
            ),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(emptyList())
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(emptyList())
        coEvery { repository.getDispensacionesByIds(any(), any()) } throws RuntimeException("DB corrupted")

        val vm = CierreCajaViewModel(repository, sessionManager)
        vm.setFecha(today)
        vm.uiState.first { !it.isLoading }

        assertNotNull("errorMessage must be set on crash", vm.uiState.value.errorMessage)
        assertTrue(
            "errorMessage must contain error text",
            vm.uiState.value.errorMessage!!.contains("DB corrupted"),
        )
        assertFalse("isLoading must be false after error", vm.uiState.value.isLoading)
    }
    // Pagos futuros metric

    @Test
    fun `pagosFuturos tracks future-dated payments`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(
                id = "p1",
                fecha = today,
                tipo = "Abono",
                monto = 100.0,
                opticaId = opticaId,
                dispensacionId = "d1",
            ),
            Pago(
                id = "p2",
                fecha = today,
                tipo = "Abono",
                monto = 50.0,
                opticaId = opticaId,
                dispensacionId = "d2",
            ),
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d2", pacienteId = "pac1", fecha = today, opticaId = opticaId),
        )
        val extraDisp = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac2", fecha = tomorrow, opticaId = opticaId),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)
        coEvery { repository.getDispensacionesByIds(listOf("d1"), opticaId) } returns extraDisp

        val vm = createViewModel()

        assertEquals("ventasHoy must only include today's disp payment", 50.0, vm.uiState.value.ventasHoy, 0.001)
        assertEquals("pagosFuturos must track future-dated payment", 100.0, vm.uiState.value.pagosFuturos, 0.001)
    }

    @Test
    fun `pagosFuturos tracks future-dated servicio payments`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(
                id = "p1",
                fecha = today,
                tipo = "Abono",
                monto = 100.0,
                opticaId = opticaId,
                servicioExtraId = "s1",
            ),
        )
        val extraServ = listOf(
            ServicioExtra(
                id = "s1",
                descripcion = "Futuro",
                montoTotal = 200.0,
                aCuenta = 0.0,
                estado = "Pendiente",
                fecha = tomorrow,
                opticaId = opticaId,
            ),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(emptyList())
        coEvery { repository.getServiciosByIds(listOf("s1"), opticaId) } returns extraServ

        val vm = createViewModel()

        assertEquals("ventasHoy must exclude future-dated servicio payment", 0.0, vm.uiState.value.ventasHoy, 0.001)
        assertEquals("pagosFuturos must track future-dated servicio payment", 100.0, vm.uiState.value.pagosFuturos, 0.001)
    }

    @Test
    fun `pago with both dispensacionId and servicioExtraId classifies by disp first`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(
                id = "p1",
                fecha = today,
                tipo = "Abono",
                monto = 100.0,
                opticaId = opticaId,
                dispensacionId = "d1",
                servicioExtraId = "s1",
            ),
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, opticaId = opticaId),
        )
        val servicios = listOf(
            ServicioExtra(
                id = "s1",
                descripcion = "Serv",
                montoTotal = 100.0,
                aCuenta = 0.0,
                estado = "Entregado",
                fecha = yesterday,
                opticaId = opticaId,
            ),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(servicios)

        val vm = createViewModel()

        // dispFecha=today (ventasHoy), servFecha=yesterday (cobrosAtrasados). Disp wins.
        assertEquals("Dual-linked pago must classify by disp date first", 100.0, vm.uiState.value.ventasHoy, 0.001)
        assertEquals(0.0, vm.uiState.value.cobrosAtrasados, 0.001)
    }

    @Test
    fun `getTotalesPorMetodo applies PagoEffect matrix`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(
                id = "p1",
                fecha = today,
                tipo = "Abono",
                monto = 100.0,
                metodoPago = "Efectivo",
                opticaId = opticaId,
                dispensacionId = "d1",
            ),
            Pago(
                id = "p2",
                fecha = today,
                tipo = "Reverso",
                monto = 40.0,
                metodoPago = "Efectivo",
                opticaId = opticaId,
                dispensacionId = "d1",
                reversaPagoId = "p1",
            ),
            Pago(
                id = "p3",
                fecha = today,
                tipo = "Reembolso",
                monto = 10.0,
                metodoPago = "Tarjeta",
                opticaId = opticaId,
                dispensacionId = "d1",
            ),
            Pago(
                id = "p4",
                fecha = today,
                tipo = "Anulación",
                monto = 50.0,
                metodoPago = "Efectivo",
                opticaId = opticaId,
                dispensacionId = "d1",
            ),
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, montoTotal = 100.0, opticaId = opticaId),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)

        val vm = createViewModel()

        val totales = vm.getTotalesPorMetodo()
        assertEquals("Efectivo = Abono 100 + Reverso -40 + Anulación 0", 60.0, totales["Efectivo"] ?: 0.0, 0.001)
        assertEquals("Tarjeta = Reembolso -10", -10.0, totales["Tarjeta"] ?: 0.0, 0.001)
    }
    // getCobradoHoy PagoEffect matrix

    @Test
    fun `getCobradoHoy sums PagoEffect for all pagos`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = today, tipo = "Abono", monto = 50.0, opticaId = opticaId, dispensacionId = "d1"),
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, opticaId = opticaId),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)

        val vm = createViewModel()

        assertEquals(150.0, vm.getCobradoHoy(), 0.001)
    }

    @Test
    fun `getCobradoHoy nets Abono plus Reverso via PagoEffect`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = today, tipo = "Reverso", monto = 40.0, opticaId = opticaId, dispensacionId = "d1", reversaPagoId = "p1"),
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, opticaId = opticaId),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)

        val vm = createViewModel()

        assertEquals("Abono 100 - Reverso 40 = 60", 60.0, vm.getCobradoHoy(), 0.001)
    }

    @Test
    fun `getCobradoHoy Anulacion contributes zero via PagoEffect`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = today, tipo = "Anulación", monto = 50.0, opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p3", fecha = today, tipo = "Reverso", monto = 40.0, opticaId = opticaId, dispensacionId = "d1", reversaPagoId = "p1"),
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, montoTotal = 100.0, opticaId = opticaId),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)

        val vm = createViewModel()

        assertEquals("Abono 100 + Anulación 0 + Reverso -40 = 60", 60.0, vm.getCobradoHoy(), 0.001)
    }

    @Test
    fun `getCobradoHoy equals sum of getTotalesPorMetodo values`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 100.0, metodoPago = "Efectivo", opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = today, tipo = "Reverso", monto = 30.0, metodoPago = "Efectivo", opticaId = opticaId, dispensacionId = "d1", reversaPagoId = "p1"),
            Pago(id = "p3", fecha = today, tipo = "Abono", monto = 200.0, metodoPago = "Tarjeta", opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p4", fecha = today, tipo = "Anulación", monto = 50.0, metodoPago = "Efectivo", opticaId = opticaId, dispensacionId = "d1"),
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", fecha = today, montoTotal = 300.0, opticaId = opticaId),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)

        val vm = createViewModel()

        assertEquals(vm.getCobradoHoy(), vm.getTotalesPorMetodo().values.sum(), 0.001)
    }

    @Test
    fun `getCobradoHoy returns zero for empty pagos`() = runTest(testDispatcher) {
        val vm = createViewModel()

        assertEquals(0.0, vm.getCobradoHoy(), 0.001)
    }
    // pagosDisplay

    @Test
    fun `pagosDisplay uses OT label for dispensacion pago`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 100.0, opticaId = opticaId, dispensacionId = "d1"),
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac1", ot = "2026-0042", fecha = today, opticaId = opticaId),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)

        val vm = createViewModel()

        val display = vm.uiState.value.pagosDisplay
        assertEquals(1, display.size)
        assertEquals("OT 2026-0042", display[0].label)
        assertEquals("Dispensación", display[0].tipoEntidad)
        assertEquals("pac1", display[0].pacienteId)
        assertEquals("d1", display[0].dispensacionId)
        assertFalse(display[0].esCobroAtrasado)
    }

    @Test
    fun `pagosDisplay marks cobro atrasado for older dispensacion`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 75.0, opticaId = opticaId, dispensacionId = "d1"),
        )
        val extraDisp = listOf(
            DispensacionOptica(id = "d1", pacienteId = "pac2", ot = "2026-0040", fecha = yesterday, opticaId = opticaId),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(emptyList())
        coEvery { repository.getDispensacionesByIds(listOf("d1"), opticaId) } returns extraDisp

        val vm = createViewModel()

        val display = vm.uiState.value.pagosDisplay
        assertEquals(1, display.size)
        assertTrue(display[0].esCobroAtrasado)
        assertEquals("OT 2026-0040", display[0].label)
    }

    @Test
    fun `pagosDisplay uses servicio descripcion as label`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Abono", monto = 50.0, opticaId = opticaId, servicioExtraId = "s1"),
        )
        val servicios = listOf(
            ServicioExtra(
                id = "s1",
                descripcion = "Reparación bisel",
                montoTotal = 50.0,
                aCuenta = 50.0,
                estado = "Entregado",
                fecha = today,
                pacienteId = "pac-serv",
                opticaId = opticaId,
            ),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(servicios)

        val vm = createViewModel()

        val display = vm.uiState.value.pagosDisplay
        assertEquals(1, display.size)
        assertEquals("Reparación bisel", display[0].label)
        assertEquals("Servicio Extra", display[0].tipoEntidad)
        assertEquals("s1", display[0].servicioExtraId)
        assertEquals("pac-serv", display[0].pacienteId)
        assertFalse(display[0].esCobroAtrasado)
    }
    // pacienteNombres

    @Test
    fun `pacienteNombres populated from pacientesFlowForOptica`() = runTest(testDispatcher) {
        val paciente = com.example.optoapp.data.Paciente(
            id = "pac1",
            nombreCompleto = "María García",
            edad = 30,
            telefono = "999999999",
            fechaCreacion = today,
            opticaId = opticaId,
        )
        every { repository.pacientesFlowForOptica(opticaId) } returns flowOf(listOf(paciente))

        val vm = createViewModel()

        assertEquals("María García", vm.uiState.value.pacienteNombres["pac1"])
    }

    @Test
    fun `saldoPendiente prefers same-day PagoEffect over doubled montoPagado cache`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(
                id = "d1",
                pacienteId = "pac1",
                fecha = today,
                montoTotal = 170.0,
                montoPagado = 200.0,
                opticaId = opticaId,
            ),
        )
        val pagos = listOf(
            Pago(
                id = "p1",
                fecha = today,
                tipo = "Abono",
                monto = 100.0,
                metodoPago = "Efectivo",
                opticaId = opticaId,
                dispensacionId = "d1",
            ),
        )
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(dispensaciones)
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)

        val vm = createViewModel()

        assertEquals(70.0, vm.uiState.value.saldoPendiente, 0.001)
        assertEquals(100.0, vm.uiState.value.pagadoLedgerByDispensacion["d1"] ?: 0.0, 0.001)
    }

    @Test
    fun `saldoPendiente prefers same-day PagoEffect over doubled aCuenta cache`() = runTest(testDispatcher) {
        val servicios = listOf(
            ServicioExtra(
                id = "s1",
                descripcion = "Brazos",
                montoTotal = 25.0,
                aCuenta = 50.0,
                estado = "Entregado",
                fecha = today,
                opticaId = opticaId,
            ),
        )
        val pagos = listOf(
            Pago(
                id = "p1",
                fecha = today,
                tipo = "Abono",
                monto = 25.0,
                metodoPago = "Transferencia",
                opticaId = opticaId,
                servicioExtraId = "s1",
            ),
        )
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(servicios)
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)

        val vm = createViewModel()

        assertEquals(0.0, vm.uiState.value.saldoPendiente, 0.001)
        assertEquals(25.0, vm.uiState.value.pagadoLedgerByServicio["s1"] ?: 0.0, 0.001)
    }

    // ── WU2: Cierre loading/empty/error triad polish (PagoEffect/role unchanged) ──

    @Test
    fun cierreTriad_loading_hidesEmpty() {
        val t = CierreCajaUiPolicy.resolveTriad(true, false, null)
        assertTrue(t.showsLoading)
        assertFalse(t.showsEmpty || t.showsError || t.showsRetry)
    }

    @Test
    fun cierreTriad_emptyAfterLoad_showsEmptyHidesLoading() {
        val t = CierreCajaUiPolicy.resolveTriad(false, false, null)
        assertFalse(t.showsLoading)
        assertTrue(t.showsEmpty)
        assertFalse(t.showsError)
    }

    @Test
    fun cierreTriad_errorMessage_showsErrorAndRetry() {
        val t = CierreCajaUiPolicy.resolveTriad(false, false, "Error al cargar datos: boom")
        assertTrue(t.showsError && t.showsRetry)
        assertFalse(t.showsEmpty || t.showsLoading)
    }

    @Test
    fun cierreTriad_withMovements_hidesEmpty() {
        val t = CierreCajaUiPolicy.resolveTriad(false, true, null)
        assertFalse(t.showsEmpty || t.showsLoading || t.showsError)
    }

    @Test
    fun failCanViewCierreCaja_yieldsRestrictedAccess() {
        assertFalse(AppRoles.canViewCierreCaja("asesor"))
        assertTrue(CierreCajaUiPolicy.resolveAccess("asesor").isRestricted)
    }

    @Test
    fun passingCanViewCierreCaja_allowsAccess() {
        assertTrue(AppRoles.canViewCierreCaja("admin"))
        assertFalse(CierreCajaUiPolicy.resolveAccess("admin").isRestricted)
    }

    @Test
    fun retry_reloadsCurrentFechaClearingError() = runTest(testDispatcher) {
        val attempts = AtomicInteger(0)
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(
            listOf(
                Pago(
                    id = "p1",
                    fecha = today,
                    tipo = "Abono",
                    monto = 50.0,
                    opticaId = opticaId,
                    dispensacionId = "missing-d1",
                ),
            ),
        )
        every { repository.getDispensacionesByDateRangeForOptica(today, today, opticaId) } returns flowOf(emptyList())
        every { repository.getServiciosByDateRangeForOptica(today, today, opticaId) } returns flowOf(emptyList())
        coEvery { repository.getDispensacionesByIds(any(), any()) } answers {
            if (attempts.getAndIncrement() == 0) {
                throw RuntimeException("transient failure")
            } else {
                emptyList()
            }
        }

        val vm = CierreCajaViewModel(repository, sessionManager)
        vm.setFecha(today)
        vm.uiState.first { !it.isLoading && it.errorMessage != null }
        assertNotNull(vm.uiState.value.errorMessage)

        vm.retry()
        vm.uiState.first { !it.isLoading && it.errorMessage == null }

        assertNull("retry must clear error on successful reload", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }
}
