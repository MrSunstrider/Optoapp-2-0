package com.example.optoapp.viewmodel

import com.example.optoapp.data.arqueo.ArqueoCaja
import com.example.optoapp.data.arqueo.IArqueoCajaRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

// ---------------------------------------------------------------------------
// Fake repository used only inside this test file.
// Implements IArqueoCajaRepo so that no production abstraction is needed.
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
// ArqueoCajaViewModelTest — 6 test cases (RED state when written,
// GREEN after ArqueoCajaViewModel is created with matching IArqueoCajaRepo).
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class ArqueoCajaViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeArqueoCajaRepository
    private lateinit var viewModel: ArqueoCajaViewModel

    private val testDate = LocalDate.of(2026, 6, 17)
    private val testOpticaId = "optica-1"
    private val testUserId = "user@test.com"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeArqueoCajaRepository()
        viewModel = ArqueoCajaViewModel(
            repo = fakeRepo,
            currentUserId = testUserId
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -----------------------------------------------------------------------
    // Test 1: diferenciaEfectivo = efectivoContado - efectivoCobrado
    // spec: efectivoContado=180.0, sistemaTotals["efectivo"]=200.0 → diferenciaEfectivo=-20.0
    // -----------------------------------------------------------------------
    @Test
    fun diferenciaEfectivo_equals_contado_minus_cobrado() = runTest(testDispatcher) {
        viewModel.setEfectivoContado(180.0)

        viewModel.cerrarDia(
            fecha = testDate,
            opticaId = testOpticaId,
            systemTotals = mapOf("efectivo" to 200.0)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = fakeRepo.getStoredArqueo(testDate, testOpticaId)
        assertNotNull("Arqueo should have been persisted", stored)
        assertEquals(-20.0, stored!!.diferenciaEfectivo, 0.001)
    }

    // -----------------------------------------------------------------------
    // Test 2: diferenciaTotal = sum of all per-method diferencias
    // spec: -20, 0, 5, 0 → diferenciaTotal=-15.0
    // -----------------------------------------------------------------------
    @Test
    fun diferenciaTotal_equals_sum_of_all_diferencias() = runTest(testDispatcher) {
        viewModel.setEfectivoContado(180.0)     // cobrado=200 → diferencia=-20
        viewModel.setTarjetaContado(150.0)      // cobrado=150 → diferencia=0
        viewModel.setTransferenciaContado(55.0) // cobrado=50  → diferencia=+5
        viewModel.setMovilContado(0.0)          // cobrado=0   → diferencia=0

        viewModel.cerrarDia(
            fecha = testDate,
            opticaId = testOpticaId,
            systemTotals = mapOf(
                "efectivo" to 200.0,
                "tarjeta" to 150.0,
                "transferencia" to 50.0,
                "movil" to 0.0
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
            systemTotals = mapOf("efectivo" to 100.0)
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
    // Stored efectivoCobrado stays frozen even after external pago changes.
    // -----------------------------------------------------------------------
    @Test
    fun cerrarDia_snapshot_invariant() = runTest(testDispatcher) {
        viewModel.setEfectivoContado(100.0)

        viewModel.cerrarDia(
            fecha = testDate,
            opticaId = testOpticaId,
            systemTotals = mapOf("efectivo" to 100.0)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // External pago change does NOT call repo.insertArqueo again
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
            systemTotals = mapOf("efectivo" to 100.0)
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
                "efectivo" to 0.0,
                "tarjeta" to 0.0,
                "transferencia" to 0.0,
                "movil" to 0.0
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("validationErrors must be empty when all contado values are 0.0", state.validationErrors.isEmpty())
        assertEquals("insertArqueo must be called once when validation passes", 1, fakeRepo.insertCallCount)
    }
}
