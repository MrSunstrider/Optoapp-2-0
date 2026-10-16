package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
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

        viewModel = ReportesViewModel(repository, sessionManager,
        mockk(relaxed = true))
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

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

        viewModel = ReportesViewModel(repository, sessionManager,
        mockk(relaxed = true))
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
        viewModel = ReportesViewModel(repository, sessionManager,
        mockk(relaxed = true))
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
    fun `totalVendido excludes Anulado and Reclamada sales from allMovimientosDelPeriodo`() = runTest(testDispatcher) {
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(
            listOf(
                DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 100.0, opticaId = opticaId, estadoEntrega = "Pendiente"),
                DispensacionOptica(id = "d2", pacienteId = "p2", fecha = today, montoTotal = 200.0, opticaId = opticaId, estadoEntrega = "Anulado"),
                DispensacionOptica(id = "d3", pacienteId = "p3", fecha = today, montoTotal = 300.0, opticaId = opticaId, estadoEntrega = "Reclamada"),
            ),
        )
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(
            listOf(
                ServicioExtra(id = "s1", descripcion = "Srv", montoTotal = 50.0, aCuenta = 50.0, estado = "Entregado", fecha = today, opticaId = opticaId),
                ServicioExtra(id = "s2", descripcion = "Anulado", montoTotal = 80.0, aCuenta = 0.0, estado = "Anulado", fecha = today, opticaId = opticaId),
            ),
        )
        coEvery { repository.getDispensacionItemsSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { repository.getRegalosSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { repository.getServicioExtraItemsSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { repository.getRegalosServicioExtraSnapshotForOptica(opticaId) } returns emptyList()

        viewModel = ReportesViewModel(repository, sessionManager, mockk(relaxed = true))
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        val movs = viewModel.allMovimientosDelPeriodo.first()
        assertEquals(setOf("d1", "s1"), movs.map { it.origenId }.toSet())
        assertEquals(150.0, viewModel.totalVendido.first(), 0.001)
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

        viewModel = ReportesViewModel(repository, sessionManager,
        mockk(relaxed = true))
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

        viewModel = ReportesViewModel(repository, sessionManager,
        mockk(relaxed = true))
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

        viewModel = ReportesViewModel(repository, sessionManager,
        mockk(relaxed = true))
        viewModel.setPeriodo("Diario")
        viewModel.setFechaDiario(today)
        advanceUntilIdle()

        assertEquals(
            "totalTransacciones must count all movimientos in period",
            3,
            viewModel.totalTransacciones.first(),
        )
    }

    @Test
    fun `pagosSumByDispensacion nets via PagoEffect Reverso`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", dispensacionId = "d1", tipo = "Abono", monto = 100.0, opticaId = opticaId, fecha = today),
            Pago(id = "p2", dispensacionId = "d1", tipo = "Reverso", monto = 100.0, opticaId = opticaId, fecha = today, reversaPagoId = "p1"),
            Pago(id = "p3", dispensacionId = "d1", tipo = "Anulación", monto = 50.0, opticaId = opticaId, fecha = today),
        )
        every { repository.getAllPagosFlowForOptica(opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager,
        mockk(relaxed = true))
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

        viewModel = ReportesViewModel(repository, sessionManager,
        mockk(relaxed = true))
        advanceUntilIdle()

        val result = viewModel.pagosSumByDispensacion.first()
        assertEquals("d1 sum should be 100", 100.0, result["d1"] ?: 0.0, 0.001)
    }

    @Test
    fun `aCuentaSumByServicio nets via PagoEffect Reverso`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", servicioExtraId = "s1", tipo = "Abono", monto = 100.0, opticaId = opticaId, fecha = today),
            Pago(id = "p2", servicioExtraId = "s1", tipo = "Reverso", monto = 100.0, opticaId = opticaId, fecha = today, reversaPagoId = "p1"),
        )
        every { repository.getAllPagosFlowForOptica(opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager,
        mockk(relaxed = true))
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

        viewModel = ReportesViewModel(repository, sessionManager,
        mockk(relaxed = true))
        advanceUntilIdle()

        val result = viewModel.aCuentaSumByServicio.first()
        assertEquals("s1 sum should be 100", 100.0, result["s1"] ?: 0.0, 0.001)
    }

    private fun stubMensualConCostos(regalos: List<RegaloDispensacionEntity> = emptyList()) {
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(
            listOf(DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, montoTotal = 200.0, opticaId = opticaId)),
        )
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(
            listOf(
                ServicioExtra(
                    id = "s1", descripcion = "Srv", montoTotal = 100.0, estado = "Entregado", fecha = today,
                    opticaId = opticaId, realizadoPor = "tercero", costoReal = 60.0,
                ),
            ),
        )
        coEvery { repository.getDispensacionItemsSnapshotForOptica(opticaId) } returns listOf(
            DispensacionItem(id = "i1", dispensacionId = "d1", opticaId = opticaId, costoRealOd = 70.0, costoRealOi = 70.0, costoOrigen = "matriz"),
        )
        coEvery { repository.getRegalosSnapshotForOptica(opticaId) } returns regalos
        coEvery { repository.getServicioExtraItemsSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { repository.getRegalosServicioExtraSnapshotForOptica(opticaId) } returns emptyList()
    }

    private fun mensualViewModel(): ReportesViewModel = ReportesViewModel(repository, sessionManager,
        mockk(relaxed = true)).apply {
        setPeriodo("Mensual")
        setFechaDiario(today)
    }

    @Test
    fun `Mensual cost and margin use real item and executor costs`() = runTest(testDispatcher) {
        stubMensualConCostos()

        viewModel = mensualViewModel()
        advanceUntilIdle()

        assertEquals(200.0, viewModel.costoTotal.first(), 0.001)
        assertEquals(100.0, viewModel.margenTotal.first(), 0.001)
    }

    @Test
    fun `Mensual cost adds dispensacion gifts`() = runTest(testDispatcher) {
        stubMensualConCostos(
            regalos = listOf(
                RegaloDispensacionEntity(
                    id = "r1", dispensacionId = "d1", productoId = "pr1", cantidad = 2, costoUnitario = 5.0,
                    descripcion = "Estuche", opticaId = opticaId,
                ),
            ),
        )

        viewModel = mensualViewModel()
        advanceUntilIdle()

        assertEquals(210.0, viewModel.costoTotal.first(), 0.001)
        assertEquals(90.0, viewModel.margenTotal.first(), 0.001)
    }

    @Test
    fun `empty period has zero cost and margin`() = runTest(testDispatcher) {
        viewModel = mensualViewModel()
        advanceUntilIdle()

        assertEquals(0.0, viewModel.costoTotal.first(), 0.001)
        assertEquals(0.0, viewModel.margenTotal.first(), 0.001)
    }

    @Test
    fun `movimientosPorOrigenId flags a servicio without executor as incomplete cost`() = runTest(testDispatcher) {
        stubMensualConCostos()
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(
            listOf(ServicioExtra(id = "s1", descripcion = "Srv", montoTotal = 100.0, estado = "Entregado", fecha = today, opticaId = opticaId)),
        )

        viewModel = mensualViewModel()
        advanceUntilIdle()

        val movimientos = viewModel.movimientosPorOrigenId.first()
        assertTrue(movimientos.getValue("s1").costoIncompleto)
        assertFalse(movimientos.getValue("d1").costoIncompleto)
    }

    @Test
    fun `margenTotal and costoTotal exclude incomplete-cost rows`() = runTest(testDispatcher) {
        stubMensualConCostos()
        every { repository.getAllServiciosForOptica(opticaId) } returns flowOf(
            listOf(ServicioExtra(id = "s1", descripcion = "Srv", montoTotal = 100.0, estado = "Entregado", fecha = today, opticaId = opticaId)),
        )

        viewModel = mensualViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.movimientosPorOrigenId.first().getValue("s1").costoIncompleto)
        assertEquals(140.0, viewModel.costoTotal.first(), 0.001)
        assertEquals(60.0, viewModel.margenTotal.first(), 0.001)
    }
}
