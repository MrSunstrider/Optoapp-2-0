package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import io.mockk.coEvery
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

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

    @Test
    fun deleteNeedsConfirmationAndNothingIsDeletedBeforeIt() = runTest(testDispatcher) {
        val objetivo = gasto("g1", "alquiler", 500.0, hoy)
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.confirmDeleteGasto(objetivo)
        advanceUntilIdle()

        assertEquals(objetivo, viewModel.uiState.value.deletingGasto)
        coVerify(exactly = 0) { repository.deleteGastoOperativo(any()) }

        viewModel.dismissDeleteGasto()

        assertNull(viewModel.uiState.value.deletingGasto)
        coVerify(exactly = 0) { repository.deleteGastoOperativo(any()) }
    }

    @Test
    fun successfulDeleteResetsLoadingAndAFollowingSaveProceeds() = runTest(testDispatcher) {
        val objetivo = gasto("g1", "alquiler", 500.0, hoy)
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.confirmDeleteGasto(objetivo)
        viewModel.deleteGasto()
        advanceUntilIdle()

        coVerify { repository.deleteGastoOperativo(objetivo) }
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.deletingGasto)

        viewModel.showNewGasto()
        viewModel.updateMonto("80")
        viewModel.saveGasto()
        advanceUntilIdle()

        coVerify { repository.upsertGastoOperativo(match { it.monto.compareTo(BigDecimal("80")) == 0 }) }
    }

    @Test
    fun failedDeleteShowsAStaticErrorScopedToTheConfirmationAndResetsLoading() = runTest(testDispatcher) {
        val objetivo = gasto("g1", "alquiler", 500.0, hoy)
        coEvery { repository.deleteGastoOperativo(objetivo) } throws IOException("disk full at /data/x")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.confirmDeleteGasto(objetivo)
        viewModel.deleteGasto()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(objetivo, state.deletingGasto)
        assertEquals("No se pudo eliminar el gasto. Inténtalo de nuevo.", state.gastoDeleteError)
        assertNull(state.gastoFormError)

        viewModel.dismissDeleteGasto()
        viewModel.showNewGasto()
        viewModel.updateMonto("80")
        viewModel.saveGasto()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.gastoDeleteError)
        coVerify { repository.upsertGastoOperativo(match { it.monto.compareTo(BigDecimal("80")) == 0 }) }
    }

    @Test
    fun invalidAmountSetsOnlyTheFormError() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.showNewGasto()
        viewModel.updateMonto("")
        viewModel.saveGasto()

        assertEquals("Ingresa un monto válido", viewModel.uiState.value.gastoFormError)
        assertNull(viewModel.uiState.value.gastoDeleteError)
        coVerify(exactly = 0) { repository.upsertGastoOperativo(any()) }
    }

    @Test
    fun editKeepsRecurrenceScheduledDateAndExplicitType() = runTest(testDispatcher) {
        val recurrente = gasto("g1", "alquiler", 2000.0, LocalDate.of(2026, 7, 1), isRecurring = true)
            .copy(fechaProgramada = LocalDate.of(2026, 8, 1), tipoGasto = "variable")
        every { repository.getGastosOperativos(opticaId) } returns flowOf(listOf(recurrente))
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.editGasto(recurrente)
        assertTrue(viewModel.uiState.value.isRecurring)
        viewModel.updateMonto("2100")
        viewModel.saveGasto()
        advanceUntilIdle()

        coVerify {
            repository.upsertGastoOperativo(
                match {
                    it.id == "g1" &&
                        it.isRecurring &&
                        it.fechaProgramada == LocalDate.of(2026, 8, 1) &&
                        it.tipoGasto == "variable" &&
                        it.monto.compareTo(BigDecimal("2100")) == 0
                },
            )
        }
    }

    @Test
    fun monthTotalAndGroupsFollowTheSelectedMonth() = runTest(testDispatcher) {
        every { repository.getGastosOperativos(opticaId) } returns flowOf(
            listOf(
                gasto("jun", "alquiler", 300.0, LocalDate.of(2026, 6, 10)),
                gasto("a1", "alquiler", 2000.0, LocalDate.of(2026, 7, 1)),
                gasto("a2", "alquiler", 100.0, LocalDate.of(2026, 7, 5), isRecurring = true),
                gasto("m1", "marketing", 500.0, LocalDate.of(2026, 7, 8)),
            ),
        )
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(0, BigDecimal("2600").compareTo(viewModel.uiState.value.totalGastosMes))

        viewModel.selectMesGastos(YearMonth.of(2026, 6))

        assertEquals(YearMonth.of(2026, 6), viewModel.uiState.value.mesGastos)
        assertEquals(0, BigDecimal("300").compareTo(viewModel.uiState.value.totalGastosMes))
        assertEquals(listOf("jun"), viewModel.uiState.value.gruposGastos.flatMap { g -> g.filas.map { it.gasto.id } })
    }

    @Test
    fun groupsAreSortedBySubtotalWithResolvedTypeAndRecurringFlag() = runTest(testDispatcher) {
        every { repository.getGastosOperativos(opticaId) } returns flowOf(
            listOf(
                gasto("o1", "otro", 50.0, LocalDate.of(2026, 7, 3)).copy(tipoGasto = "fijo"),
                gasto("m1", "marketing", 500.0, LocalDate.of(2026, 7, 8)),
                gasto("a1", "alquiler", 2000.0, LocalDate.of(2026, 7, 1)),
                gasto("a2", "alquiler", 100.0, LocalDate.of(2026, 7, 5), isRecurring = true),
            ),
        )
        val viewModel = buildViewModel()
        advanceUntilIdle()

        val grupos = viewModel.uiState.value.gruposGastos
        assertEquals(listOf("Alquiler", "Marketing", "Otro"), grupos.map { it.etiqueta })
        assertEquals(0, BigDecimal("2100").compareTo(grupos[0].subtotal))
        assertEquals(0, BigDecimal("500").compareTo(grupos[1].subtotal))
        assertEquals(listOf("fijo", "fijo"), grupos[0].filas.map { it.tipo })
        assertEquals(listOf(false, true), grupos[0].filas.sortedBy { it.gasto.id }.map { it.gasto.isRecurring })
        assertEquals("variable", grupos[1].filas.single().tipo)
        assertEquals("fijo", grupos[2].filas.single().tipo)
    }

    @Test
    fun roleWithoutWritePermissionCannotCreateEditOrDelete() = runTest(testDispatcher) {
        val existente = gasto("g1", "alquiler", 500.0, hoy)
        every { sessionManager.opticaRol } returns flowOf("asesor")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.puedeEditarGastos)

        viewModel.showNewGasto()
        viewModel.editGasto(existente)
        viewModel.confirmDeleteGasto(existente)
        viewModel.deleteGasto()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDialogVisible)
        assertNull(viewModel.uiState.value.deletingGasto)
        coVerify(exactly = 0) { repository.deleteGastoOperativo(any()) }
    }

    @Test
    fun managementRolesCanWriteGastos() = runTest(testDispatcher) {
        every { sessionManager.opticaRol } returns flowOf(" Gerente ")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.puedeEditarGastos)
    }
}
