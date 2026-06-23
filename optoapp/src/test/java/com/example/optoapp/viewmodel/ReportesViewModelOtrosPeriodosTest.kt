package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.SessionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Tests for ReportesViewModel — non-daily periods ("Semanal", "Este mes",
 * "Este año", "Anual", "Todo").
 *
 * All tests use LocalDate.now() as reference so they pass regardless of
 * the actual current date. Semanal week boundaries are computed dynamically.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportesViewModelOtrosPeriodosTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: ReportesViewModel

    private val opticaId = "optica-test-1"

    /**
     * Monday of the current week (= fechaDiario will default to LocalDate.now(),
     * so the week displayed by "Semanal" is the current Mon-Sun).
     */
    private val currentMonday: LocalDate by lazy {
        val today = LocalDate.now()
        today.minusDays((today.dayOfWeek.value - 1).toLong())
    }

    /** Sunday of the current week */
    private val currentSunday: LocalDate by lazy { currentMonday.plusDays(6) }

    /** Day BEFORE Monday = previous Sunday, should be excluded from Semanal */
    private val prevSunday: LocalDate by lazy { currentMonday.minusDays(1) }

    /** Monday of next week = boundary, should be excluded */
    private val nextMonday: LocalDate by lazy { currentMonday.plusDays(7) }

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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    /** Launches persistent background collectors for all StateFlows. */
    private fun TestScope.activateFlows() {
        backgroundScope.launch(testDispatcher) { viewModel.allDispensaciones.collect { } }
        backgroundScope.launch(testDispatcher) { viewModel.totalVendido.collect { } }
        backgroundScope.launch(testDispatcher) { viewModel.totalPagado.collect { } }
        backgroundScope.launch(testDispatcher) { viewModel.totalCobrado.collect { } }
        backgroundScope.launch(testDispatcher) { viewModel.cobrosPeriodo.collect { } }
    }

    @Test
    fun `Semanal includes Mon through Sun of current week`() = runTest(testDispatcher) {
        val inWeek = listOf(
            DispensacionOptica(id = "mon", pacienteId = "p", fecha = currentMonday,  montoTotal = 10.0, opticaId = opticaId),
            DispensacionOptica(id = "wed", pacienteId = "p", fecha = currentMonday.plusDays(2), montoTotal = 20.0, opticaId = opticaId),
            DispensacionOptica(id = "sun", pacienteId = "p", fecha = currentSunday,  montoTotal = 30.0, opticaId = opticaId)
        )
        val outOfWeek = listOf(
            DispensacionOptica(id = "prev-sun", pacienteId = "p", fecha = prevSunday, montoTotal = 40.0, opticaId = opticaId),
            DispensacionOptica(id = "next-mon", pacienteId = "p", fecha = nextMonday, montoTotal = 50.0, opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(inWeek + outOfWeek)

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Semanal")
        advanceUntilIdle()

        assertEquals("Semanal should include Mon-Sun of the current week",
            3, viewModel.allDispensaciones.value.size)
        assertTrue("Monday should be included",
            viewModel.allDispensaciones.value.any { it.id == "mon" })
        assertTrue("Sunday should be included",
            viewModel.allDispensaciones.value.any { it.id == "sun" })
    }

    @Test
    fun `Semanal excludes prev Sunday and next Monday`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "prev-sun", pacienteId = "p", fecha = prevSunday, montoTotal = 10.0, opticaId = opticaId),
            DispensacionOptica(id = "next-mon", pacienteId = "p", fecha = nextMonday, montoTotal = 20.0, opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Semanal")
        advanceUntilIdle()

        assertTrue("Both dates should be excluded", viewModel.allDispensaciones.value.isEmpty())
    }

    @Test
    fun `Semanal totalVendido sums only week dispensaciones`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "in",  pacienteId = "p", fecha = currentMonday, montoTotal = 100.0, opticaId = opticaId),
            DispensacionOptica(id = "out", pacienteId = "p", fecha = prevSunday,    montoTotal = 200.0, opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Semanal")
        advanceUntilIdle()

        assertEquals("totalVendido should sum only week dispensaciones", 100.0, viewModel.totalVendido.value, 0.001)
    }

    @Test
    fun `Semanal cobrosPeriodo classifies across week boundary`() = runTest(testDispatcher) {
        val todasLasDispensaciones = listOf(
            DispensacionOptica(id = "in",  pacienteId = "p", fecha = currentMonday, montoTotal = 100.0, opticaId = opticaId),
            DispensacionOptica(id = "out", pacienteId = "p", fecha = prevSunday,    montoTotal = 100.0, opticaId = opticaId)
        )
        val pagos = listOf(
            // Pago for in-week dispensation → venta del período
            Pago(id = "p1", fecha = currentMonday, tipo = "Efectivo", monto = 100.0, opticaId = opticaId, dispensacionId = "in"),
            // Pago for out-of-week dispensation → cobro atrasado
            Pago(id = "p2", fecha = currentMonday, tipo = "Efectivo", monto = 50.0,  opticaId = opticaId, dispensacionId = "out")
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(todasLasDispensaciones)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Semanal")
        advanceUntilIdle()

        assertEquals("cobrosPeriodo should include payments from outside the week",
            50.0, viewModel.cobrosPeriodo.value, 0.001)
    }

    @Test
    fun `Este mes includes only dates in the current month`() = runTest(testDispatcher) {
        val now = LocalDate.now()
        val firstOfMonth = now.withDayOfMonth(1)
        val lastOfMonth = now.withDayOfMonth(now.lengthOfMonth())

        val inMonth = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p", fecha = firstOfMonth, montoTotal = 10.0, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p", fecha = now,          montoTotal = 20.0, opticaId = opticaId),
            DispensacionOptica(id = "d3", pacienteId = "p", fecha = lastOfMonth,  montoTotal = 30.0, opticaId = opticaId)
        )
        val outOfMonth = listOf(
            DispensacionOptica(id = "d4", pacienteId = "p", fecha = firstOfMonth.minusDays(1), montoTotal = 40.0, opticaId = opticaId),
            DispensacionOptica(id = "d5", pacienteId = "p", fecha = lastOfMonth.plusDays(1),   montoTotal = 50.0, opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(inMonth + outOfMonth)

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Este mes")
        advanceUntilIdle()

        assertEquals("Este mes should include only current month dates",
            3, viewModel.allDispensaciones.value.size)
    }

    @Test
    fun `Este mes totalVendido sums only current month`() = runTest(testDispatcher) {
        val now = LocalDate.now()
        val lastMonth = now.minusMonths(1)
        val dispensaciones = listOf(
            DispensacionOptica(id = "in",  pacienteId = "p", fecha = now,        montoTotal = 100.0, opticaId = opticaId),
            DispensacionOptica(id = "out", pacienteId = "p", fecha = lastMonth,  montoTotal = 500.0, opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Este mes")
        advanceUntilIdle()

        assertEquals("totalVendido should only reflect current month", 100.0, viewModel.totalVendido.value, 0.001)
    }

    @Test
    fun `Este año includes only dates in the current year`() = runTest(testDispatcher) {
        val now = LocalDate.now()
        val firstOfYear = now.withDayOfYear(1)
        val lastOfYear = now.withDayOfYear(now.lengthOfYear())

        val inYear = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p", fecha = firstOfYear,  montoTotal = 10.0, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p", fecha = now,          montoTotal = 20.0, opticaId = opticaId),
            DispensacionOptica(id = "d3", pacienteId = "p", fecha = lastOfYear,   montoTotal = 30.0, opticaId = opticaId)
        )
        val outOfYear = listOf(
            DispensacionOptica(id = "d4", pacienteId = "p", fecha = firstOfYear.minusDays(1), montoTotal = 40.0, opticaId = opticaId),
            DispensacionOptica(id = "d5", pacienteId = "p", fecha = lastOfYear.plusDays(1),   montoTotal = 50.0, opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(inYear + outOfYear)

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Este año")
        advanceUntilIdle()

        assertEquals("Este año should include only current year dates",
            3, viewModel.allDispensaciones.value.size)
    }

    @Test
    fun `Anual includes only dates from the selected year`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d2025", pacienteId = "p", fecha = LocalDate.of(2025, 6, 23), montoTotal = 10.0, opticaId = opticaId),
            DispensacionOptica(id = "d2026", pacienteId = "p", fecha = LocalDate.of(2026, 6, 23), montoTotal = 20.0, opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Anual")
        viewModel.setAnio("2025")
        advanceUntilIdle()

        assertEquals("Anual 2025", 1, viewModel.allDispensaciones.value.size)
        assertTrue("d2025 included", viewModel.allDispensaciones.value.any { it.id == "d2025" })

        viewModel.setAnio("2026")
        advanceUntilIdle()
        assertEquals("Anual 2026", 1, viewModel.allDispensaciones.value.size)
        assertTrue("d2026 included", viewModel.allDispensaciones.value.any { it.id == "d2026" })
    }

    @Test
    fun `Anual with no matching year returns empty`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d2025", pacienteId = "p", fecha = LocalDate.of(2025, 6, 23), montoTotal = 10.0, opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Anual")
        viewModel.setAnio("2030")
        advanceUntilIdle()

        assertTrue("Anual 2030 should be empty", viewModel.allDispensaciones.value.isEmpty())
    }

    @Test
    fun `Anual totalVendido reflects selected year`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d25", pacienteId = "p", fecha = LocalDate.of(2025, 1, 1), montoTotal = 500.0, opticaId = opticaId),
            DispensacionOptica(id = "d26", pacienteId = "p", fecha = LocalDate.of(2026, 6, 1), montoTotal = 300.0, opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Anual")
        viewModel.setAnio("2025")
        advanceUntilIdle()
        assertEquals("Anual 2025", 500.0, viewModel.totalVendido.value, 0.001)

        viewModel.setAnio("2026")
        advanceUntilIdle()
        assertEquals("Anual 2026", 300.0, viewModel.totalVendido.value, 0.001)
    }

    @Test
    fun `Todo includes all dispensaciones regardless of date`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p", fecha = LocalDate.of(2020, 1, 1),  montoTotal = 10.0, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p", fecha = LocalDate.now(),            montoTotal = 20.0, opticaId = opticaId),
            DispensacionOptica(id = "d3", pacienteId = "p", fecha = LocalDate.of(2030, 12, 31), montoTotal = 30.0, opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Todo")
        advanceUntilIdle()

        assertEquals("Todo should include 3 dispensaciones", 3, viewModel.allDispensaciones.value.size)
    }

    @Test
    fun `Todo totalVendido includes everything`() = runTest(testDispatcher) {
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p", fecha = LocalDate.of(2020, 1, 1),  montoTotal = 100.0, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p", fecha = LocalDate.now(),            montoTotal = 200.0, opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Todo")
        advanceUntilIdle()

        assertEquals("Todo totalVendido should include all", 300.0, viewModel.totalVendido.value, 0.001)
    }

    @Test
    fun `Todo cobrosPeriodo is always 0`() = runTest(testDispatcher) {
        val todasLasDispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p", fecha = LocalDate.of(2020, 1, 1), montoTotal = 100.0, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p", fecha = LocalDate.now(),           montoTotal = 200.0, opticaId = opticaId)
        )
        val pagos = listOf(
            Pago(id = "p1", fecha = LocalDate.now(), tipo = "Efectivo", monto = 50.0,  opticaId = opticaId, dispensacionId = "d1"),
            Pago(id = "p2", fecha = LocalDate.now(), tipo = "Efectivo", monto = 100.0, opticaId = opticaId, dispensacionId = "d2")
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(todasLasDispensaciones)
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(pagos)

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Todo")
        advanceUntilIdle()

        // In Todo, all dispensaciones are in-period → all payments are "ventas del período"
        assertEquals("cobrosPeriodo should be 0", 0.0, viewModel.cobrosPeriodo.value, 0.001)
        assertEquals("totalCobrado", 150.0, viewModel.totalCobrado.value, 0.001)
    }

    @Test
    fun `switching from Anual to Semanal recalculates filter`() = runTest(testDispatcher) {
        val now = LocalDate.now()
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p", fecha = now, montoTotal = 100.0, opticaId = opticaId),
            DispensacionOptica(id = "d2", pacienteId = "p", fecha = now.minusMonths(3), montoTotal = 200.0, opticaId = opticaId)
        )
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(dispensaciones)

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()

        // Anual 2026 should include both (both in 2026, since now is 2026)
        viewModel.setPeriodo("Anual")
        viewModel.setAnio(now.year.toString())
        advanceUntilIdle()
        assertEquals("Anual current year", 2, viewModel.allDispensaciones.value.size)

        // Semanal should only include today (the other is 3 months ago, different week)
        viewModel.setPeriodo("Semanal")
        advanceUntilIdle()
        assertEquals("Semanal from current week", 1, viewModel.allDispensaciones.value.size)
    }

    @Test
    fun `empty data across all periods returns zero`() = runTest(testDispatcher) {
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every { repository.getPagosByDateRangeForOptica(any(), any(), opticaId) } returns flowOf(emptyList())

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()

        for (periodo in listOf("Semanal", "Este mes", "Este año", "Anual", "Todo")) {
            viewModel.setPeriodo(periodo)
            viewModel.setAnio(LocalDate.now().year.toString())
            advanceUntilIdle()

            assertTrue("$periodo: dispensaciones should be empty", viewModel.allDispensaciones.value.isEmpty())
            assertEquals("$periodo: totalVendido should be 0", 0.0, viewModel.totalVendido.value, 0.001)
            assertEquals("$periodo: totalCobrado should be 0", 0.0, viewModel.totalCobrado.value, 0.001)
        }
    }

    // ===================================================================
    // T3.4–T3.8: DAO receives exact date range per period
    // ===================================================================

    @Test
    fun `Semanal period passes week range to DAO`() = runTest(testDispatcher) {
        val now = LocalDate.now()
        val startOfWeek = now.minusDays((now.dayOfWeek.value - 1).toLong())
        val endOfWeek = startOfWeek.plusDays(6)

        val startSlot = slot<LocalDate>()
        val endSlot = slot<LocalDate>()
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every {
            repository.getPagosByDateRangeForOptica(capture(startSlot), capture(endSlot), any())
        } returns flowOf(emptyList())

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Semanal")
        advanceUntilIdle()

        assertEquals("Semanal start must be Monday of current week", startOfWeek, startSlot.captured)
        assertEquals("Semanal end must be Sunday of current week", endOfWeek, endSlot.captured)
    }

    @Test
    fun `Este mes period passes month range to DAO`() = runTest(testDispatcher) {
        val now = LocalDate.now()
        val firstOfMonth = now.withDayOfMonth(1)
        val lastOfMonth = now.withDayOfMonth(now.lengthOfMonth())

        val startSlot = slot<LocalDate>()
        val endSlot = slot<LocalDate>()
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every {
            repository.getPagosByDateRangeForOptica(capture(startSlot), capture(endSlot), any())
        } returns flowOf(emptyList())

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Este mes")
        advanceUntilIdle()

        assertEquals("Este mes start must be first day of month", firstOfMonth, startSlot.captured)
        assertEquals("Este mes end must be last day of month", lastOfMonth, endSlot.captured)
    }

    @Test
    fun `Este año period passes year range to DAO`() = runTest(testDispatcher) {
        val now = LocalDate.now()
        val firstOfYear = now.withDayOfYear(1)
        val lastOfYear = now.withDayOfYear(now.lengthOfYear())

        val startSlot = slot<LocalDate>()
        val endSlot = slot<LocalDate>()
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every {
            repository.getPagosByDateRangeForOptica(capture(startSlot), capture(endSlot), any())
        } returns flowOf(emptyList())

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Este año")
        advanceUntilIdle()

        assertEquals("Este año start must be Jan 1", firstOfYear, startSlot.captured)
        assertEquals("Este año end must be Dec 31", lastOfYear, endSlot.captured)
    }

    @Test
    fun `Anual period passes selected year range to DAO`() = runTest(testDispatcher) {
        val startSlot = slot<LocalDate>()
        val endSlot = slot<LocalDate>()
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every {
            repository.getPagosByDateRangeForOptica(capture(startSlot), capture(endSlot), any())
        } returns flowOf(emptyList())

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Anual")
        viewModel.setAnio("2025")
        advanceUntilIdle()

        assertEquals("Anual 2025 start must be Jan 1 2025", LocalDate.of(2025, 1, 1), startSlot.captured)
        assertEquals("Anual 2025 end must be Dec 31 2025", LocalDate.of(2025, 12, 31), endSlot.captured)
    }

    @Test
    fun `Todo period passes MIN MAX to DAO`() = runTest(testDispatcher) {
        val startSlot = slot<LocalDate>()
        val endSlot = slot<LocalDate>()
        every { repository.getAllDispensacionesForOptica(opticaId) } returns flowOf(emptyList())
        every {
            repository.getPagosByDateRangeForOptica(capture(startSlot), capture(endSlot), any())
        } returns flowOf(emptyList())

        viewModel = ReportesViewModel(repository, sessionManager)
        activateFlows()
        viewModel.setPeriodo("Todo")
        advanceUntilIdle()

        assertEquals("Todo start must be MIN", LocalDate.MIN, startSlot.captured)
        assertEquals("Todo end must be MAX", LocalDate.MAX, endSlot.captured)
    }
}
