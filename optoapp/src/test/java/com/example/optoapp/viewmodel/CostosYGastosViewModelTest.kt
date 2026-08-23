package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.costoproducto.CostoProductoEntity
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CostosYGastosViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: OptoRepository
    private lateinit var costoProductoDao: CostoProductoDao
    private lateinit var costoBiseladoDao: CostoBiseladoDao
    private lateinit var sessionManager: SessionManager
    private lateinit var scheduler: PostSaveSyncScheduler
    private lateinit var syncFinanzas: SyncFinanzasUseCase
    private lateinit var viewModel: CostosYGastosViewModel

    private val opticaId = "optica-test"
    private val testDate = LocalDate.of(2026, 7, 16)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock android.util.Log to avoid RuntimeException in caught exceptions
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0

        mockkObject(DateUtils)
        every { DateUtils.today() } returns testDate
        every { DateUtils.toIso(testDate) } returns "2026-07-16"

        repository = mockk(relaxed = true)
        costoProductoDao = mockk(relaxed = true)
        costoBiseladoDao = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        syncFinanzas = mockk(relaxed = true)

        every { sessionManager.opticaId } returns flowOf(opticaId)
        every { repository.getGastosOperativos(opticaId) } returns emptyFlow()
        // Default: getByBloque returns empty so loadBlock doesn't crash
        coEvery { costoProductoDao.getByBloque(any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun saveCosto_createsEntityWithUuidOpticaIdAndVigenteDesde_callsUpsertAll() = runTest(testDispatcher) {
        // Mock DAO refresh to return empty after upsert
        coEvery { costoProductoDao.getByBloque(opticaId, "stock") } returns flowOf(emptyList())

        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            costoBiseladoDao,
            sessionManager,
            scheduler,
            syncFinanzas,
        )

        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        viewModel.showNewCosto()
        viewModel.updateCostoMaterial("CR39")
        viewModel.updateCostoTipoLente("Monofocal")
        viewModel.updateCostoCostoUnitario("150.00")

        viewModel.saveCosto()
        advanceUntilIdle()

        // Verify upsertAll was called with one entity containing expected fields
        coVerify {
            costoProductoDao.upsertAll(
                withArg { entities ->
                    assertEquals(1, entities.size)
                    val entity = entities[0]
                    assertEquals(opticaId, entity.opticaId)
                    assertEquals("CR39", entity.material)
                    assertEquals("Monofocal", entity.tipoLente)
                    assertEquals(150.0, entity.costoUnitario, 0.001)
                    assertEquals("2026-07-16", entity.vigenteDesde)
                    assertNotNull("Entity must have a generated UUID", entity.id)
                    assertTrue("Entity ID must not be blank", entity.id.isNotBlank())
                },
            )
        }

        // Verify block was refreshed after save
        coVerify(atLeast = 1) { costoProductoDao.getByBloque(opticaId, "stock") }

        // Verify sync was scheduled
        coVerify { scheduler.scheduleFinanzasSync(opticaId) }

        // Verify dialog dismissed and no error
        assertFalse("Dialog must be dismissed after successful save", viewModel.uiState.value.isCostoDialogVisible)
        assertNull("No error expected after successful save", viewModel.uiState.value.costoSaveError)
    }

    @Test
    fun saveCosto_rejectsEmptyMaterial_setsCostoSaveError() = runTest(testDispatcher) {
        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            costoBiseladoDao,
            sessionManager,
            scheduler,
            syncFinanzas,
        )

        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        viewModel.showNewCosto()
        // Fill other fields but leave material empty
        viewModel.updateCostoTipoLente("Monofocal")
        viewModel.updateCostoCostoUnitario("150.00")

        viewModel.saveCosto()

        assertEquals("Selecciona un material", viewModel.uiState.value.costoSaveError)
        assertTrue("Dialog must remain visible when validation fails", viewModel.uiState.value.isCostoDialogVisible)
    }

    @Test
    fun saveCosto_rejectsEmptyTipoLente_setsCostoSaveError() = runTest(testDispatcher) {
        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            costoBiseladoDao,
            sessionManager,
            scheduler,
            syncFinanzas,
        )

        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        viewModel.showNewCosto()
        viewModel.updateCostoMaterial("CR39")
        // Leave tipoLente empty
        viewModel.updateCostoCostoUnitario("150.00")

        viewModel.saveCosto()

        assertEquals("Selecciona un tipo de lente", viewModel.uiState.value.costoSaveError)
        assertTrue("Dialog must remain visible when validation fails", viewModel.uiState.value.isCostoDialogVisible)
    }

    @Test
    fun saveCosto_rejectsInvalidCostoUnitario_setsCostoSaveError() = runTest(testDispatcher) {
        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            costoBiseladoDao,
            sessionManager,
            scheduler,
            syncFinanzas,
        )

        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        viewModel.showNewCosto()
        viewModel.updateCostoMaterial("CR39")
        viewModel.updateCostoTipoLente("Monofocal")
        viewModel.updateCostoCostoUnitario("0") // <= 0

        viewModel.saveCosto()

        assertEquals("Ingresa un costo unitario válido", viewModel.uiState.value.costoSaveError)
        assertTrue("Dialog must remain visible when validation fails", viewModel.uiState.value.isCostoDialogVisible)
    }

    @Test
    fun saveCosto_rejectsNegativeCostoUnitario_setsCostoSaveError() = runTest(testDispatcher) {
        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            costoBiseladoDao,
            sessionManager,
            scheduler,
            syncFinanzas,
        )

        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        viewModel.showNewCosto()
        viewModel.updateCostoMaterial("CR39")
        viewModel.updateCostoTipoLente("Monofocal")
        viewModel.updateCostoCostoUnitario("-50")

        viewModel.saveCosto()

        assertEquals("Ingresa un costo unitario válido", viewModel.uiState.value.costoSaveError)
    }

    @Test
    fun saveCosto_rejectsNonNumericCostoUnitario_setsCostoSaveError() = runTest(testDispatcher) {
        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            costoBiseladoDao,
            sessionManager,
            scheduler,
            syncFinanzas,
        )

        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        viewModel.showNewCosto()
        viewModel.updateCostoMaterial("CR39")
        viewModel.updateCostoTipoLente("Monofocal")
        viewModel.updateCostoCostoUnitario("abc")

        viewModel.saveCosto()

        assertEquals("Ingresa un costo unitario válido", viewModel.uiState.value.costoSaveError)
    }

    @Test
    fun deleteCosto_setsVigenteHasta_callsUpsertAll_refreshesBlock_schedulesSync() = runTest(testDispatcher) {
        val existing = CostoProductoEntity(
            id = "costo-1",
            opticaId = opticaId,
            material = "CR39",
            tipoLente = "Monofocal",
            stockOFabricacion = "stock",
            costoUnitario = 150.0,
            vigenteDesde = "2026-01-01",
            vigenteHasta = null,
        )

        coEvery { costoProductoDao.getByBloque(opticaId, "stock") } returns flowOf(listOf(existing))

        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            costoBiseladoDao,
            sessionManager,
            scheduler,
            syncFinanzas,
        )

        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        // Verify entity is in the list
        assertEquals(1, viewModel.uiState.value.costosDelBloque.size)

        // Trigger delete confirmation and delete
        viewModel.confirmDeleteCosto(existing)
        assertNotNull("deletingCosto must be set after confirmDeleteCosto", viewModel.uiState.value.deletingCosto)

        viewModel.deleteCosto()
        advanceUntilIdle()

        // Verify upsertAll was called with entity that has vigenteHasta set
        coVerify {
            costoProductoDao.upsertAll(
                withArg { entities ->
                    assertEquals(1, entities.size)
                    val deleted = entities[0]
                    assertEquals("costo-1", deleted.id)
                    assertEquals("2026-07-16", deleted.vigenteHasta)
                },
            )
        }

        // Verify block was refreshed
        coVerify(atLeast = 1) { costoProductoDao.getByBloque(opticaId, "stock") }

        // Verify sync was scheduled
        coVerify { scheduler.scheduleFinanzasSync(opticaId) }

        // Verify deletingCosto is cleared
        assertNull("deletingCosto must be cleared after delete", viewModel.uiState.value.deletingCosto)
    }

    @Test
    fun deleteCosto_whenDaoFails_setsCostoSaveError() = runTest(testDispatcher) {
        val existing = CostoProductoEntity(
            id = "costo-2",
            opticaId = opticaId,
            material = "Policarbonato",
            tipoLente = "Progresivo",
            stockOFabricacion = "fabricacion",
            costoUnitario = 250.0,
            vigenteDesde = "2026-01-01",
            vigenteHasta = null,
        )

        coEvery { costoProductoDao.getByBloque(opticaId, "fabricacion") } returns flowOf(listOf(existing))
        coEvery { costoProductoDao.upsertAll(any()) } throws IOException("DB error")

        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            costoBiseladoDao,
            sessionManager,
            scheduler,
            syncFinanzas,
        )

        viewModel.loadBlock("Fabricación Resina")
        advanceUntilIdle()

        viewModel.confirmDeleteCosto(existing)
        viewModel.deleteCosto()
        advanceUntilIdle()

        val error = viewModel.uiState.value.costoSaveError
        assertNotNull("Error must be set after delete failure", error)
        assertTrue("Error must mention the problem", error!!.contains("eliminar", ignoreCase = true))
    }

    @Test
    fun autoGenerarSiFalta_whenDueTemplateMissing_upsertsAndShowsMaterialized() = runTest(testDispatcher) {
        val template = GastoOperativoEntity(
            id = "tpl-1",
            opticaId = opticaId,
            categoria = "alquiler",
            monto = BigDecimal.valueOf(800.0),
            fecha = LocalDate.of(2026, 6, 1),
            isRecurring = true,
        )
        every { repository.getGastosOperativos(opticaId) } returns flowOf(listOf(template))
        coEvery { repository.upsertGastoOperativo(any()) } returns Unit

        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            costoBiseladoDao,
            sessionManager,
            scheduler,
            syncFinanzas,
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.upsertGastoOperativo(any()) }
        coVerify { scheduler.scheduleFinanzasSync(opticaId) }
        val gastos = viewModel.uiState.value.gastosOperativos
        assertEquals(2, gastos.size)
        assertTrue(gastos.any { it.id == "tpl-1" })
        assertTrue(
            gastos.any {
                !it.isRecurring &&
                    it.categoria == "alquiler" &&
                    it.fecha == LocalDate.of(2026, 7, 1)
            },
        )
    }

    @Test
    fun autoGenerarSiFalta_whenCopyAlreadyExists_doesNotUpsert() = runTest(testDispatcher) {
        val template = GastoOperativoEntity(
            id = "tpl-1",
            opticaId = opticaId,
            categoria = "personal",
            monto = BigDecimal.valueOf(2500.0),
            fecha = LocalDate.of(2026, 6, 1),
            isRecurring = true,
        )
        val copia = GastoOperativoEntity(
            id = "cop-1",
            opticaId = opticaId,
            categoria = "personal",
            monto = BigDecimal.valueOf(2500.0),
            fecha = LocalDate.of(2026, 7, 1),
            isRecurring = false,
        )
        every { repository.getGastosOperativos(opticaId) } returns flowOf(listOf(template, copia))

        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            costoBiseladoDao,
            sessionManager,
            scheduler,
            syncFinanzas,
        )
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.upsertGastoOperativo(any()) }
        assertEquals(2, viewModel.uiState.value.gastosOperativos.size)
    }

    @Test
    fun clampTab_initialTab3_isGastosOperativos() {
        assertEquals(3, CostosYGastosViewModel.TAB_GASTOS)
        assertEquals(3, CostosYGastosViewModel.clampTab(3))
    }

    @Test
    fun clampTab_outOfRange_clampsTo0Through3() {
        assertEquals(0, CostosYGastosViewModel.clampTab(-1))
        assertEquals(0, CostosYGastosViewModel.clampTab(0))
        assertEquals(2, CostosYGastosViewModel.clampTab(2))
        assertEquals(3, CostosYGastosViewModel.clampTab(3))
        assertEquals(3, CostosYGastosViewModel.clampTab(99))
    }

    @Test
    fun selectTab_initialTab3_setsGastosSelected() = runTest(testDispatcher) {
        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            costoBiseladoDao,
            sessionManager,
            scheduler,
            syncFinanzas,
        )
        viewModel.selectTab(3)
        assertEquals(3, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun selectTab_clampsOutOfRange() = runTest(testDispatcher) {
        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            costoBiseladoDao,
            sessionManager,
            scheduler,
            syncFinanzas,
        )
        viewModel.selectTab(-5)
        assertEquals(0, viewModel.uiState.value.selectedTab)
        viewModel.selectTab(10)
        assertEquals(3, viewModel.uiState.value.selectedTab)
    }

    // ── WU2: Gastos tab loading/empty/error triad ──

    @Test
    fun gastosTriad_loading_hidesEmpty() {
        val t = CostosGastosUiPolicy.resolveGastosTriad(true, 0, null)
        assertTrue(t.showsLoading)
        assertFalse(t.showsEmpty)
        assertFalse(t.showsError)
    }

    @Test
    fun gastosTriad_emptyAfterLoad_showsEmptyHidesLoading() {
        val t = CostosGastosUiPolicy.resolveGastosTriad(false, 0, null)
        assertFalse(t.showsLoading)
        assertTrue(t.showsEmpty)
        assertFalse(t.showsError)
    }

    @Test
    fun gastosTriad_error_showsErrorAndRetry() {
        val t = CostosGastosUiPolicy.resolveGastosTriad(false, 0, "Error al cargar gastos")
        assertFalse(t.showsEmpty)
        assertTrue(t.showsError && t.showsRetry)
        assertFalse(t.showsLoading)
    }

    @Test
    fun gastosTriad_withData_hidesEmptyAndLoading() {
        val t = CostosGastosUiPolicy.resolveGastosTriad(false, 2, null)
        assertFalse(t.showsLoading || t.showsEmpty || t.showsError)
    }

    @Test
    fun costosAccess_nullRol_isRestricted() {
        val access = CostosGastosUiPolicy.resolveAccess(null)
        assertTrue(access.isRestricted)
    }

    @Test
    fun costosAccess_adminRol_isNotRestricted() {
        val access = CostosGastosUiPolicy.resolveAccess("admin")
        assertFalse(access.isRestricted)
    }

    @Test
    fun costosAccess_gerenteRol_isNotRestricted() {
        val access = CostosGastosUiPolicy.resolveAccess("gerente")
        assertFalse(access.isRestricted)
    }

    @Test
    fun costosAccess_asesorRol_isRestricted() {
        val access = CostosGastosUiPolicy.resolveAccess("asesor")
        assertTrue(access.isRestricted)
    }

    @Test
    fun costosAccess_especialistaRol_isRestricted() {
        val access = CostosGastosUiPolicy.resolveAccess("especialista")
        assertTrue(access.isRestricted)
    }

    @Test
    fun gastosLoading_staysTrueUntilFirstEmission() = runTest(testDispatcher) {
        val neverEmits = kotlinx.coroutines.flow.MutableSharedFlow<List<GastoOperativoEntity>>()
        every { repository.getGastosOperativos(opticaId) } returns neverEmits

        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            costoBiseladoDao,
            sessionManager,
            scheduler,
            syncFinanzas,
        )
        advanceUntilIdle()

        assertTrue(
            "gastosLoading must stay true until first Flow emission",
            viewModel.uiState.value.gastosLoading,
        )
        assertNull(viewModel.uiState.value.gastosError)
    }

    @Test
    fun gastosLoading_falseAfterEmptyEmission() = runTest(testDispatcher) {
        every { repository.getGastosOperativos(opticaId) } returns flowOf(emptyList())

        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            costoBiseladoDao,
            sessionManager,
            scheduler,
            syncFinanzas,
        )
        advanceUntilIdle()

        assertFalse(
            "empty after load: gastosLoading off once flow emits",
            viewModel.uiState.value.gastosLoading,
        )
        assertNull(viewModel.uiState.value.gastosError)
    }

    @Test
    fun gastosFlowFailure_setsGastosErrorAndClearsLoading() = runTest(testDispatcher) {
        every { repository.getGastosOperativos(opticaId) } returns kotlinx.coroutines.flow.flow {
            throw IOException("gastos db down")
        }

        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            costoBiseladoDao,
            sessionManager,
            scheduler,
            syncFinanzas,
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.gastosLoading)
        assertNotNull(viewModel.uiState.value.gastosError)
        assertTrue(
            viewModel.uiState.value.gastosError!!.contains("gastos db down"),
        )
    }
}
