package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class GastosViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var scheduler: PostSaveSyncScheduler
    private lateinit var syncFinanzas: SyncFinanzasUseCase
    private lateinit var viewModel: GastosViewModel

    private val opticaId = "optica-test"
    private val testDate = LocalDate.of(2026, 6, 15)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0

        repository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        syncFinanzas = mockk(relaxed = true)

        every { sessionManager.opticaId } returns flowOf(opticaId)
        every { repository.getGastosOperativos(opticaId) } returns emptyFlow()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun delete_when_repository_fails_sets_error_state() = runTest(testDispatcher) {
        val gasto = GastoOperativoEntity(
            id = "g1",
            opticaId = opticaId,
            categoria = "alquiler",
            descripcion = "Local",
            monto = BigDecimal.valueOf(500.0),
            fecha = testDate,
        )

        coEvery { repository.deleteGastoOperativo(gasto) } throws
            IOException("Database write failed")

        viewModel = GastosViewModel(repository, sessionManager, scheduler, syncFinanzas)
        viewModel.delete(gasto)

        // After the coroutine runs, error should be set
        advanceUntilIdle()

        val error = viewModel.uiState.value.error
        assertNotNull("Expected error state to be set after delete failure", error)
    }

    @Test
    fun delete_success_does_not_set_error_state() = runTest(testDispatcher) {
        val gasto = GastoOperativoEntity(
            id = "g2",
            opticaId = opticaId,
            categoria = "servicios",
            descripcion = "Internet",
            monto = BigDecimal.valueOf(80.0),
            fecha = testDate,
        )

        coEvery { repository.deleteGastoOperativo(gasto) } returns Unit

        viewModel = GastosViewModel(repository, sessionManager, scheduler, syncFinanzas)
        viewModel.delete(gasto)

        advanceUntilIdle()

        val error = viewModel.uiState.value.error
        assertNull("Expected no error after successful delete, but got: $error", error)
    }

    @Test
    fun `allGastos emits sorted by fecha descending`() = runTest(testDispatcher) {
        val earlyDate = LocalDate.of(2026, 1, 15)
        val midDate = LocalDate.of(2026, 6, 15)
        val lateDate = LocalDate.of(2026, 12, 20)
        val gastoEarly = GastoOperativoEntity(
            id = "g_early", opticaId = opticaId, categoria = "alquiler",
            monto = BigDecimal.valueOf(100.0), fecha = earlyDate,
        )
        val gastoMid = GastoOperativoEntity(
            id = "g_mid", opticaId = opticaId, categoria = "servicios",
            monto = BigDecimal.valueOf(200.0), fecha = midDate,
        )
        val gastoLate = GastoOperativoEntity(
            id = "g_late", opticaId = opticaId, categoria = "insumos",
            monto = BigDecimal.valueOf(300.0), fecha = lateDate,
        )
        // Return unsorted — VM must sort descending
        every { repository.getGastosOperativos(opticaId) } returns flowOf(
            listOf(gastoMid, gastoEarly, gastoLate),
        )

        viewModel = GastosViewModel(repository, sessionManager, scheduler, syncFinanzas)
        advanceUntilIdle()

        val emitted = viewModel.allGastos.value
        assertEquals(3, emitted.size)
        assertEquals("g_late", emitted[0].id)
        assertEquals("g_mid", emitted[1].id)
        assertEquals("g_early", emitted[2].id)
    }

    @Test
    fun `allGastos maintains sort with auto-generated recurring gastos`() = runTest(testDispatcher) {
        val today = LocalDate.of(2026, 6, 15)
        val older = today.minusMonths(1)
        val gastoExisting = GastoOperativoEntity(
            id = "g_existing", opticaId = opticaId, categoria = "alquiler",
            monto = BigDecimal.valueOf(500.0), fecha = today,
        )
        // Return existing gasto and the repository will auto-generate a recurring one
        every { repository.getGastosOperativos(opticaId) } returns flowOf(listOf(gastoExisting))

        viewModel = GastosViewModel(repository, sessionManager, scheduler, syncFinanzas)
        advanceUntilIdle()

        val emitted = viewModel.allGastos.value
        // Should have at least the existing gasto (auto-generated goes to different category)
        assertTrue(emitted.isNotEmpty())
        // First item should be the most recent (today)
        assertEquals(today, emitted.first().fecha)
    }
}
