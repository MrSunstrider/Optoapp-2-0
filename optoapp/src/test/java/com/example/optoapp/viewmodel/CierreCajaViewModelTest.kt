package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.arqueo.ArqueoCaja
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

// ---------------------------------------------------------------------------
// CierreCajaViewModelTest
//
// Uses MockK to stub OptoRepository and SessionManager, matching the pattern
// used in SyncViewModelConflictResolutionTest and other ViewModel tests.
//
// Tests 1-3 + 6: verify computation logic (ventasHoy, cobrosAtrasados,
//   saldoPendiente, totalRecaudado) via the existing observePagos() flow.
// Tests 4-5: verify loadArqueoForDate() — a NEW method added in T-08.
//   These will fail to compile until T-08 (CierreCajaViewModel update) is done.
//   That is the expected RED state for those two test cases.
// ---------------------------------------------------------------------------

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
    // Test 3: saldoPendiente = totalVentasHoy - ventasHoy
    // spec: dispensacion montoTotal=300, only 150 paid → saldoPendiente=150.0
    // -----------------------------------------------------------------------
    @Test
    fun saldoPendiente_equals_totalVentas_minus_ventasHoy() = runTest(testDispatcher) {
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
    // Test 4: loadArqueoForDate returns sealed arqueo
    // NOTE: This test references CierreCajaViewModel.loadArqueoForDate() which
    // does NOT exist yet — RED state until T-08 is implemented.
    // -----------------------------------------------------------------------
    @Test
    fun loadArqueoForDate_returns_sealed_arqueo() = runTest(testDispatcher) {
        val sealedArqueo = ArqueoCaja(
            id = "arq-1",
            fecha = pastDate,
            opticaId = opticaId,
            fondoCaja = 500.0,
            efectivoContado = 200.0,
            tarjetaContado = 150.0,
            transferenciaContado = 0.0,
            movilContado = 50.0,
            efectivoCobrado = 200.0,
            tarjetaCobrado = 150.0,
            transferenciaCobrado = 0.0,
            movilCobrado = 50.0,
            diferenciaEfectivo = 0.0,
            diferenciaTarjeta = 0.0,
            diferenciaTransferencia = 0.0,
            diferenciaMovil = 0.0,
            diferenciaTotal = 0.0,
            cerradoPor = "user@test.com",
            sellado = true,
            createdAt = "2026-06-15T10:00:00Z",
            updatedAt = "2026-06-15T10:00:00Z"
        )
        every { repository.getArqueoByFecha(pastDate, opticaId) } returns flowOf(sealedArqueo)
        every { repository.getPagosByDateRangeForOptica(any(), any(), any()) } returns flowOf(emptyList())
        every { repository.getAllDispensacionesForOptica(any()) } returns flowOf(emptyList())

        viewModel = CierreCajaViewModel(repository, sessionManager)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.loadArqueoForDate(pastDate, opticaId).first()
        assertNotNull("loadArqueoForDate should return the sealed arqueo", result)
        assertEquals("arq-1", result!!.id)
        assertEquals(true, result.sellado)
    }

    // -----------------------------------------------------------------------
    // Test 5: loadArqueoForDate returns null when no arqueo exists
    // NOTE: RED state until T-08 is implemented.
    // -----------------------------------------------------------------------
    @Test
    fun loadArqueoForDate_returns_null_when_no_arqueo() = runTest(testDispatcher) {
        every { repository.getArqueoByFecha(today, opticaId) } returns flowOf(null)
        every { repository.getPagosByDateRangeForOptica(any(), any(), any()) } returns flowOf(emptyList())
        every { repository.getAllDispensacionesForOptica(any()) } returns flowOf(emptyList())

        viewModel = CierreCajaViewModel(repository, sessionManager)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.loadArqueoForDate(today, opticaId).first()
        assertNull("loadArqueoForDate should return null when no arqueo exists for the date", result)
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
}
