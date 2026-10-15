package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CostosYGastosViewModelGastosTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val opticaId = "optica-test"
    private val hoy = LocalDate.of(2026, 7, 16)

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var scheduler: PostSaveSyncScheduler

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        mockkObject(DateUtils)
        every { DateUtils.today() } returns hoy

        repository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        every { sessionManager.opticaId } returns flowOf(opticaId)
        every { sessionManager.opticaRol } returns flowOf("admin")
        every { repository.getGastosOperativos(opticaId) } returns emptyFlow()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun gasto(
        id: String,
        categoria: String,
        monto: Double,
        fecha: LocalDate,
        isRecurring: Boolean = false,
    ) = GastoOperativoEntity(
        id = id,
        opticaId = opticaId,
        categoria = categoria,
        monto = BigDecimal.valueOf(monto),
        fecha = fecha,
        isRecurring = isRecurring,
    )

    private fun buildViewModel() = CostosYGastosViewModel(
        repository,
        mockk<CostoProductoDao>(relaxed = true),
        mockk<CostoBiseladoDao>(relaxed = true),
        sessionManager,
        scheduler,
        mockk<SyncFinanzasUseCase>(relaxed = true),
    )

    @Test
    fun gastosAreExposedNewestFirst() = runTest(testDispatcher) {
        every { repository.getGastosOperativos(opticaId) } returns flowOf(
            listOf(
                gasto("g_mid", "servicios", 200.0, LocalDate.of(2026, 6, 15)),
                gasto("g_early", "alquiler", 100.0, LocalDate.of(2026, 1, 15)),
                gasto("g_late", "insumos", 300.0, LocalDate.of(2026, 7, 10)),
            ),
        )

        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(
            listOf("g_late", "g_mid", "g_early"),
            viewModel.uiState.value.gastosOperativos.map { it.id },
        )
    }

    @Test
    fun recurringTemplateFromAPreviousMonthIsCopiedIntoTheCurrentMonth() = runTest(testDispatcher) {
        val plantilla = gasto("tpl", "alquiler", 800.0, LocalDate.of(2026, 6, 1), isRecurring = true)
        every { repository.getGastosOperativos(opticaId) } returns flowOf(listOf(plantilla))

        val viewModel = buildViewModel()
        advanceUntilIdle()

        coVerify {
            repository.upsertGastoOperativo(
                match { it.categoria == "alquiler" && it.fecha == LocalDate.of(2026, 7, 1) && !it.isRecurring },
            )
        }
        coVerify { scheduler.scheduleFinanzasSync(opticaId) }
        assertEquals(LocalDate.of(2026, 7, 1), viewModel.uiState.value.gastosOperativos.first().fecha)
    }

    @Test
    fun saveGastoPersistsAndSchedulesSync() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.showNewGasto()
        viewModel.updateMonto("150")
        viewModel.saveGasto()
        advanceUntilIdle()

        coVerify { repository.upsertGastoOperativo(match { it.monto.compareTo(BigDecimal("150")) == 0 }) }
        coVerify { scheduler.scheduleFinanzasSync(opticaId) }
        assertEquals(false, viewModel.uiState.value.isDialogVisible)
    }
}
