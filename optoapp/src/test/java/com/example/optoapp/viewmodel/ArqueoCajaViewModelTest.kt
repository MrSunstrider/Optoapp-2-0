package com.example.optoapp.viewmodel

import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.arqueo.ArqueoCaja
import com.example.optoapp.data.arqueo.IArqueoCajaRepo
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

// ---------------------------------------------------------------------------
// Fake repository — implements IArqueoCajaRepo for test isolation.
// ---------------------------------------------------------------------------

private class FakeArqueoCajaRepository : IArqueoCajaRepo {

    private val store = mutableMapOf<Pair<LocalDate, String>, ArqueoCaja>()

    var insertCallCount = 0
        private set

    override suspend fun insertArqueo(arqueo: ArqueoCaja) {
        insertCallCount++
        store[arqueo.fecha to arqueo.opticaId] = arqueo
    }

    fun getStoredArqueo(fecha: LocalDate, opticaId: String): ArqueoCaja? =
        store[fecha to opticaId]
}

// ---------------------------------------------------------------------------
// ArqueoCajaViewModelTest
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class ArqueoCajaViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeArqueoCajaRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: ArqueoCajaViewModel

    private val testDate = LocalDate.of(2026, 6, 17)
    private val testOpticaId = "optica-1"
    private val testUserId = "user@test.com"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeArqueoCajaRepository()
        sessionManager = mockk(relaxed = true)
        every { sessionManager.userEmail } returns flowOf(testUserId)
        viewModel = ArqueoCajaViewModel(
            repo = fakeRepo,
            sessionManager = sessionManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -----------------------------------------------------------------------
    // Test 1: diferenciaEfectivo = efectivoContado - efectivoCobrado
    // -----------------------------------------------------------------------
    @Test
    fun diferenciaEfectivo_equals_contado_minus_cobrado() = runTest(testDispatcher) {
        viewModel.setEfectivoContado(180.0)

        viewModel.cerrarDia(
            fecha = testDate,
            opticaId = testOpticaId,
            systemTotals = mapOf("Efectivo" to 200.0)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = fakeRepo.getStoredArqueo(testDate, testOpticaId)
        assertNotNull("Arqueo should have been persisted", stored)
        assertEquals(-20.0, stored!!.diferenciaEfectivo, 0.001)
    }

    // -----------------------------------------------------------------------
    // Test 2: diferenciaTotal = sum of all per-method diferencias
    // -----------------------------------------------------------------------
    @Test
    fun diferenciaTotal_equals_sum_of_all_diferencias() = runTest(testDispatcher) {
        viewModel.setEfectivoContado(180.0)
        viewModel.setTarjetaContado(150.0)
        viewModel.setTransferenciaContado(55.0)
        viewModel.setMovilContado(0.0)

        viewModel.cerrarDia(
            fecha = testDate,
            opticaId = testOpticaId,
            systemTotals = mapOf(
                "Efectivo" to 200.0,
                "Tarjeta" to 150.0,
                "Transferencia" to 50.0,
                "Móvil" to 0.0
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = fakeRepo.getStoredArqueo(testDate, testOpticaId)
        assertNotNull(stored)
        assertEquals(-15.0, stored!!.diferenciaTotal, 0.001)
    }

    // -----------------------------------------------------------------------
    // Test 3: negative contado emits validation error AND DAO is never called
    // -----------------------------------------------------------------------
    @Test
    fun negative_contado_emits_validation_error() = runTest(testDispatcher) {
        viewModel.setEfectivoContado(-5.0)

        viewModel.cerrarDia(
            fecha = testDate,
            opticaId = testOpticaId,
            systemTotals = mapOf("Efectivo" to 100.0)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(
            "validationErrors[efectivoContado] must be non-null for negative contado",
            state.validationErrors["efectivoContado"]
        )
        assertEquals("insertArqueo must not be called when validation fails", 0, fakeRepo.insertCallCount)
    }

    // -----------------------------------------------------------------------
    // Test 4: cerrarDia snapshot invariant
    // -----------------------------------------------------------------------
    @Test
    fun cerrarDia_snapshot_invariant() = runTest(testDispatcher) {
        viewModel.setEfectivoContado(100.0)

        viewModel.cerrarDia(
            fecha = testDate,
            opticaId = testOpticaId,
            systemTotals = mapOf("Efectivo" to 100.0)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val storedRow = fakeRepo.getStoredArqueo(testDate, testOpticaId)
        assertNotNull(storedRow)
        assertEquals(
            "efectivoCobrado must remain frozen at 100.0 regardless of external pago changes",
            100.0,
            storedRow!!.efectivoCobrado,
            0.001
        )
    }

    // -----------------------------------------------------------------------
    // Test 5: cerrarDia sets sellado = true
    // -----------------------------------------------------------------------
    @Test
    fun cerrarDia_sets_sellado_true() = runTest(testDispatcher) {
        viewModel.setEfectivoContado(100.0)

        viewModel.cerrarDia(
            fecha = testDate,
            opticaId = testOpticaId,
            systemTotals = mapOf("Efectivo" to 100.0)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = fakeRepo.getStoredArqueo(testDate, testOpticaId)
        assertNotNull(stored)
        assertTrue("sellado must be true after cerrarDia", stored!!.sellado)
    }

    // -----------------------------------------------------------------------
    // Test 6: zero contado passes validation → no errors, cerrarDia succeeds
    // -----------------------------------------------------------------------
    @Test
    fun zero_contado_passes_validation() = runTest(testDispatcher) {
        viewModel.cerrarDia(
            fecha = testDate,
            opticaId = testOpticaId,
            systemTotals = mapOf(
                "Efectivo" to 0.0,
                "Tarjeta" to 0.0,
                "Transferencia" to 0.0,
                "Móvil" to 0.0
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("validationErrors must be empty when all contado values are 0.0", state.validationErrors.isEmpty())
        assertEquals("insertArqueo must be called once when validation passes", 1, fakeRepo.insertCallCount)
    }

    // -----------------------------------------------------------------------
    // Test 7: cerrarDia uses title-case keys to look up systemTotals (Bug #4)
    // -----------------------------------------------------------------------
    @Test
    fun totalesUseTitleCaseKeys() = runTest(testDispatcher) {
        viewModel.setEfectivoContado(500.0)

        viewModel.cerrarDia(
            fecha = testDate,
            opticaId = testOpticaId,
            systemTotals = mapOf(
                "Efectivo" to 500.0,
                "Tarjeta" to 100.0,
                "Transferencia" to 50.0,
                "Móvil" to 25.0
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = fakeRepo.getStoredArqueo(testDate, testOpticaId)
        assertNotNull("Arqueo should have been persisted", stored)
        assertEquals(
            "efectivoCobrado must be non-zero when Efectivo key is title-case",
            500.0, stored!!.efectivoCobrado, 0.001
        )
        assertEquals(
            "tarjetaCobrado must be non-zero when Tarjeta key is title-case",
            100.0, stored.tarjetaCobrado, 0.001
        )
        assertEquals(
            "transferenciaCobrado must be non-zero when Transferencia key is title-case",
            50.0, stored.transferenciaCobrado, 0.001
        )
        assertEquals(
            "movilCobrado must be non-zero when Móvil key is title-case",
            25.0, stored.movilCobrado, 0.001
        )
    }

    // -----------------------------------------------------------------------
    // Test 8: userId is resolved async from SessionManager in init (Bug #3)
    // The ViewModel must populate currentUserId from the userEmail flow
    // without blocking the constructor thread.
    // -----------------------------------------------------------------------
    @Test
    fun userIdResolvedAsyncWithoutBlocking() = runTest(testDispatcher) {
        // ViewModel was constructed in setUp with SessionManager returning testUserId.
        // After the coroutine in init completes, currentUserId must equal the email.
        advanceUntilIdle()

        // Trigger cerrarDia to see the userId stamped on the stored arqueo.
        viewModel.cerrarDia(
            fecha = testDate,
            opticaId = testOpticaId,
            systemTotals = mapOf("Efectivo" to 100.0)
        )
        advanceUntilIdle()

        val stored = fakeRepo.getStoredArqueo(testDate, testOpticaId)
        assertNotNull("Arqueo should have been persisted", stored)
        assertEquals(
            "cerradoPor must be the email resolved async from SessionManager",
            testUserId,
            stored!!.cerradoPor
        )
    }

    // -----------------------------------------------------------------------
    // Test 9: cerrarDia guard — does NOT persist when userId has not yet resolved
    // -----------------------------------------------------------------------
    @Test
    fun cerrarDia_empty_userId_does_not_persist() = runTest(testDispatcher) {
        val neverEmittingSession = mockk<SessionManager>(relaxed = true)
        every { neverEmittingSession.userEmail } returns MutableSharedFlow()

        val vmNoUser = ArqueoCajaViewModel(repo = fakeRepo, sessionManager = neverEmittingSession)
        testDispatcher.scheduler.advanceUntilIdle()

        vmNoUser.cerrarDia(
            fecha = testDate,
            opticaId = testOpticaId,
            systemTotals = mapOf("Efectivo" to 100.0)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("insertArqueo must NOT be called when userId is empty", 0, fakeRepo.insertCallCount)
    }

    // -----------------------------------------------------------------------
    // Test 10: setFondoCaja value flows through to persisted ArqueoCaja.fondoCaja
    // -----------------------------------------------------------------------
    @Test
    fun setFondoCaja_value_is_persisted_in_arqueo() = runTest(testDispatcher) {
        viewModel.setFondoCaja(350.0)

        viewModel.cerrarDia(
            fecha = testDate,
            opticaId = testOpticaId,
            systemTotals = mapOf("Efectivo" to 100.0)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = fakeRepo.getStoredArqueo(testDate, testOpticaId)
        assertNotNull("Arqueo must be persisted", stored)
        assertEquals("fondoCaja must reflect the value set via setFondoCaja", 350.0, stored!!.fondoCaja, 0.001)
    }

    // -----------------------------------------------------------------------
    // Test 11: cerrarDia called twice inserts twice (VM-level idempotency)
    // DAO-level REPLACE prevents duplicate-key error; VM makes no attempt to deduplicate.
    // -----------------------------------------------------------------------
    @Test
    fun cerrarDia_called_twice_calls_insertArqueo_twice() = runTest(testDispatcher) {
        viewModel.cerrarDia(fecha = testDate, opticaId = testOpticaId, systemTotals = emptyMap())
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.cerrarDia(fecha = testDate, opticaId = testOpticaId, systemTotals = emptyMap())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Two cerrarDia calls must each call insertArqueo", 2, fakeRepo.insertCallCount)
    }

    // -----------------------------------------------------------------------
    // Tests 12–15: badgeColorFor companion — badge color thresholds
    // -----------------------------------------------------------------------
    @Test
    fun badgeColorFor_green_when_contado_equals_cobrado() {
        assertEquals(BadgeColor.GREEN, ArqueoCajaViewModel.badgeColorFor(200.0, 200.0))
    }

    @Test
    fun badgeColorFor_yellow_when_within_5_percent() {
        // |204 - 200| / 200 = 2% ≤ 5%
        assertEquals(BadgeColor.YELLOW, ArqueoCajaViewModel.badgeColorFor(204.0, 200.0))
    }

    @Test
    fun badgeColorFor_red_when_above_5_percent() {
        // |215 - 200| / 200 = 7.5% > 5%
        assertEquals(BadgeColor.RED, ArqueoCajaViewModel.badgeColorFor(215.0, 200.0))
    }

    @Test
    fun badgeColorFor_red_when_cobrado_is_zero_and_contado_nonzero() {
        assertEquals(BadgeColor.RED, ArqueoCajaViewModel.badgeColorFor(100.0, 0.0))
    }
}
