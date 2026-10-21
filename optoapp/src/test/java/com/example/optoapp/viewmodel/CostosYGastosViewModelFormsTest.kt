package com.example.optoapp.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costobiselado.CostoBiseladoEntity
import com.example.optoapp.data.costolc.CostoLcDao
import com.example.optoapp.data.costolc.CostoLcEntity
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.domain.MatrizVigenciaUseCase
import com.example.optoapp.domain.ResultadoVigencia
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.ui.navigation.CostosTab
import com.example.optoapp.util.DateUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
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
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CostosYGastosViewModelFormsTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val opticaId = "optica-forms"
    private val hoy = LocalDate.of(2026, 7, 16)

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var scheduler: PostSaveSyncScheduler
    private lateinit var costoProductoDao: CostoProductoDao
    private lateinit var costoBiseladoDao: CostoBiseladoDao
    private lateinit var costoLcDao: CostoLcDao
    private lateinit var syncStateTracker: SyncStateTracker
    private lateinit var syncFinanzasUseCase: SyncFinanzasUseCase
    private lateinit var matrizVigenciaUseCase: MatrizVigenciaUseCase

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
        costoProductoDao = mockk(relaxed = true)
        costoBiseladoDao = mockk(relaxed = true)
        costoLcDao = mockk(relaxed = true)
        syncStateTracker = mockk(relaxed = true)
        syncFinanzasUseCase = mockk(relaxed = true)
        matrizVigenciaUseCase = mockk()
        every { sessionManager.opticaId } returns flowOf(opticaId)
        every { sessionManager.opticaRol } returns flowOf("admin")
        every { repository.getGastosOperativos(opticaId) } returns emptyFlow()
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        coEvery { costoBiseladoDao.getByOpticaIdList(opticaId) } returns emptyList()
        coEvery { costoLcDao.getByOpticaIdList(opticaId) } returns emptyList()
        every { costoProductoDao.getByBloque(any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun buildViewModel() = CostosYGastosViewModel(
        repository,
        costoProductoDao,
        costoBiseladoDao,
        costoLcDao,
        sessionManager,
        scheduler,
        syncFinanzasUseCase,
        syncStateTracker,
        matrizVigenciaUseCase,
        SavedStateHandle(),
    )

    private fun costo(id: String, tipoLente: String = "Monofocal", serie: Int? = 1) = CostoProductoEntity(
        id = id,
        opticaId = opticaId,
        material = "Resina",
        tipoLente = tipoLente,
        stockOFabricacion = "stock",
        serie = serie,
        costoUnitario = 20.0,
        vigenteDesde = "2026-01-01",
    )

    @Test
    fun everyGastoFieldEditedInTheFormReachesTheSavedRow() = runTest(testDispatcher) {
        val guardado = slot<GastoOperativoEntity>()
        coEvery { repository.upsertGastoOperativo(capture(guardado)) } returns Unit
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.showNewGasto()
        viewModel.updateCategoria("marketing")
        viewModel.updateDescripcion("Volantes")
        viewModel.updateMonto("350.50")
        viewModel.updateFecha(LocalDate.of(2026, 7, 3))
        viewModel.updateNota("Campaña de invierno")
        viewModel.toggleRecurrente()
        viewModel.saveGasto()
        advanceUntilIdle()

        with(guardado.captured) {
            assertEquals(this@CostosYGastosViewModelFormsTest.opticaId, opticaId)
            assertEquals("marketing", categoria)
            assertEquals("Volantes", descripcion)
            assertEquals(0, BigDecimal("350.50").compareTo(monto))
            assertEquals(LocalDate.of(2026, 7, 3), fecha)
            assertEquals("Campaña de invierno", nota)
            assertTrue(isRecurring)
            assertEquals("variable", tipoGasto)
        }
        coVerify { scheduler.scheduleFinanzasSync(opticaId) }
        assertFalse(viewModel.uiState.value.isDialogVisible)
    }

    @Test
    fun blankDescriptionAndNoteAreStoredAsNull() = runTest(testDispatcher) {
        val guardado = slot<GastoOperativoEntity>()
        coEvery { repository.upsertGastoOperativo(capture(guardado)) } returns Unit
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.showNewGasto()
        viewModel.updateDescripcion("   ")
        viewModel.updateNota("")
        viewModel.updateMonto("100")
        viewModel.saveGasto()
        advanceUntilIdle()

        assertNull(guardado.captured.descripcion)
        assertNull(guardado.captured.nota)
    }

    @Test
    fun aNonPositiveAmountIsRejectedWithoutPersisting() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.showNewGasto()
        viewModel.updateMonto("0")
        viewModel.saveGasto()
        advanceUntilIdle()

        assertEquals("Ingresa un monto válido", viewModel.uiState.value.gastoFormError)
        coVerify(exactly = 0) { repository.upsertGastoOperativo(any()) }
    }

    @Test
    fun dismissingTheGastoDialogDropsTheEditAndItsError() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.showNewGasto()
        viewModel.updateMonto("abc")
        viewModel.saveGasto()

        viewModel.dismissDialog()

        val estado = viewModel.uiState.value
        assertFalse(estado.isDialogVisible)
        assertNull(estado.editingGasto)
        assertNull(estado.gastoFormError)
    }

    @Test
    fun refreshDownloadsFinanzasWithoutUploadingAndSwallowsFailures() = runTest(testDispatcher) {
        coEvery { syncFinanzasUseCase(opticaId, downloadAfterUpload = true, skipUpload = true) } throws IllegalStateException("offline")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.refreshGastos()
        advanceUntilIdle()

        coVerify(exactly = 1) { syncFinanzasUseCase(opticaId, downloadAfterUpload = true, skipUpload = true) }
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun newCostoPreselectsTheModalityOfTheOpenBlock() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.loadBlock("Fabricación Resina")
        advanceUntilIdle()

        viewModel.showNewCosto()

        val estado = viewModel.uiState.value
        assertTrue(estado.isCostoDialogVisible)
        assertTrue(estado.creatingCosto)
        assertEquals("fabricacion", estado.costoStockOFabricacion)
    }

    @Test
    fun newCostoValidatesMaterialThenLensTypeThenUnitCost() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        // Fabricación Resina prefills modality only — tipo stays empty for the full chain.
        viewModel.loadBlock("Fabricación Resina")
        viewModel.showNewCosto()

        viewModel.saveCosto()
        assertEquals("Selecciona un material", viewModel.uiState.value.costoSaveError)

        viewModel.updateCostoMaterial("Resina")
        viewModel.saveCosto()
        assertEquals("Selecciona un tipo de lente", viewModel.uiState.value.costoSaveError)

        viewModel.updateCostoTipoLente("Monofocal")
        viewModel.updateCostoCostoUnitario("-3")
        viewModel.saveCosto()
        assertEquals("Ingresa un costo unitario válido", viewModel.uiState.value.costoSaveError)

        coVerify(exactly = 0) { costoProductoDao.upsertAll(any()) }
    }

    @Test
    fun newCostoOnStockMonofocalPrefillsTipoAndSkipsTipoValidation() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.loadBlock("Stock Monofocal")
        viewModel.showNewCosto()
        assertEquals("Monofocal", viewModel.uiState.value.costoTipoLente)

        viewModel.saveCosto()
        assertEquals("Selecciona un material", viewModel.uiState.value.costoSaveError)

        viewModel.updateCostoMaterial("Resina")
        viewModel.saveCosto()
        assertEquals("Ingresa un costo unitario válido", viewModel.uiState.value.costoSaveError)
    }

    @Test
    fun savingANewCostoMarksItForUploadAndResetsTheForm() = runTest(testDispatcher) {
        val filas = slot<List<CostoProductoEntity>>()
        coEvery { costoProductoDao.upsertAll(capture(filas)) } returns Unit
        val refrescado = listOf(costo("c-1"))
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()
        every { costoProductoDao.getByBloque(opticaId, "stock") } returns flowOf(refrescado)

        viewModel.showNewCosto()
        viewModel.updateCostoMaterial("Resina")
        viewModel.updateCostoTipoLente("Monofocal")
        viewModel.updateCostoTratamiento("")
        viewModel.updateCostoSerie("2")
        viewModel.updateCostoCostoUnitario("18.5")
        viewModel.saveCosto()
        advanceUntilIdle()

        val fila = filas.captured.single()
        assertEquals("stock", fila.stockOFabricacion)
        assertNull(fila.tratamiento)
        assertEquals(2, fila.serie)
        assertEquals(18.5, fila.costoUnitario, 0.0)
        assertEquals("2026-07-16", fila.vigenteDesde)
        assertNotNull(fila.updatedAt)
        coVerify { syncStateTracker.markLocallyModified(opticaId, "costo_producto", fila.id) }
        coVerify { scheduler.scheduleFinanzasSync(opticaId) }
        val estado = viewModel.uiState.value
        assertFalse(estado.isCostoDialogVisible)
        assertEquals(refrescado, estado.costosDelBloque)
        assertEquals("", estado.costoMaterial)
        assertEquals("", estado.costoCostoUnitario)
    }

    @Test
    fun aFailedCostoSaveKeepsTheDialogWithTheError() = runTest(testDispatcher) {
        coEvery { costoProductoDao.upsertAll(any()) } throws IllegalStateException("disk full")
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.loadBlock("Stock Monofocal")
        viewModel.showNewCosto()
        viewModel.updateCostoMaterial("Resina")
        viewModel.updateCostoTipoLente("Monofocal")
        viewModel.updateCostoCostoUnitario("10")

        viewModel.saveCosto()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCostoDialogVisible)
        assertEquals("Error al guardar: disk full", viewModel.uiState.value.costoSaveError)
        coVerify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
    }

    @Test
    fun dismissingTheCostoDialogClosesItAndClearsTheError() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.showNewCosto()
        viewModel.saveCosto()

        viewModel.dismissCostoDialog()

        val estado = viewModel.uiState.value
        assertFalse(estado.isCostoDialogVisible)
        assertFalse(estado.creatingCosto)
        assertNull(estado.costoSaveError)
    }

    @Test
    fun deletingACostoClosesItsValidityTodayInsteadOfRemovingTheRow() = runTest(testDispatcher) {
        val filas = slot<List<CostoProductoEntity>>()
        coEvery { costoProductoDao.upsertAll(capture(filas)) } returns Unit
        val objetivo = costo("c-del")
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        viewModel.confirmDeleteCosto(objetivo)
        assertEquals(objetivo, viewModel.uiState.value.deletingCosto)
        viewModel.deleteCosto()
        advanceUntilIdle()

        assertEquals("2026-07-16", filas.captured.single().vigenteHasta)
        coVerify { syncStateTracker.markLocallyModified(opticaId, "costo_producto", "c-del") }
        coVerify { scheduler.scheduleFinanzasSync(opticaId) }
        assertNull(viewModel.uiState.value.deletingCosto)
    }

    @Test
    fun dismissingTheDeleteConfirmationKeepsTheCosto() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.confirmDeleteCosto(costo("c-keep"))

        viewModel.dismissDeleteDialog()
        viewModel.deleteCosto()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.deletingCosto)
        coVerify(exactly = 0) { costoProductoDao.upsertAll(any()) }
    }

    @Test
    fun stockMonofocalBlockHidesOtherTiposAndSeriesFilterNarrowsFurther() = runTest(testDispatcher) {
        val mono1 = costo("m1", "Monofocal", 1)
        val mono2 = costo("m2", "Monofocal", 2)
        val bifo1 = costo("b1", "Bifocal", 1)
        every { costoProductoDao.getByBloque(opticaId, "stock") } returns flowOf(listOf(mono1, mono2, bifo1))
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        assertEquals(listOf(mono1, mono2), viewModel.uiState.value.costosVisibles)

        viewModel.updateFiltroSerie(2)
        assertEquals(listOf(mono2), viewModel.uiState.value.costosVisibles)

        viewModel.updateFiltroSerie(1)
        assertEquals(listOf(mono1), viewModel.uiState.value.costosVisibles)
    }

    @Test
    fun aFailingMatrixLoadShowsAnErrorAndStopsLoading() = runTest(testDispatcher) {
        coEvery { costoBiseladoDao.getByOpticaIdList(opticaId) } throws IllegalStateException("db closed")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.selectTab(CostosTab.BISELADO.ordinal)
        advanceUntilIdle()

        assertEquals("No se pudo cargar la matriz de biselado.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun biseladoFormRejectsInvalidRingModalitySeriesAndDuplicates() = runTest(testDispatcher) {
        val abierta = CostoBiseladoEntity(
            id = "b-open", opticaId = opticaId, material = "Resina", tipoAro = "aro_completo",
            stockOFabricacion = "stock", serie = 1, costoPorPar = 8.0, vigenteDesde = "2026-01-01",
        )
        coEvery { costoBiseladoDao.getByOpticaIdList(opticaId) } returns listOf(abierta)
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.selectTab(CostosTab.BISELADO.ordinal)
        advanceUntilIdle()
        viewModel.showNewBiselado()

        viewModel.updateFormBiselado { it.copy(material = "Resina", tipoAro = "redondo") }
        viewModel.saveBiselado()
        assertEquals("Selecciona el tipo de aro", viewModel.uiState.value.formBiselado?.error)

        viewModel.updateFormBiselado { it.copy(tipoAro = "aro_completo", modalidad = "otro") }
        viewModel.saveBiselado()
        assertEquals("Selecciona stock o fabricación", viewModel.uiState.value.formBiselado?.error)

        viewModel.updateFormBiselado { it.copy(modalidad = "stock", serie = "uno", costo = "9") }
        viewModel.saveBiselado()
        assertEquals("La serie debe ser un número entero", viewModel.uiState.value.formBiselado?.error)

        viewModel.updateFormBiselado { it.copy(serie = "1") }
        viewModel.saveBiselado()
        assertTrue(viewModel.uiState.value.formBiselado?.error.orEmpty().startsWith("Ya existe un precio vigente"))

        coVerify(exactly = 0) { costoBiseladoDao.upsertAll(any()) }
    }

    @Test
    fun aFailedBiseladoSaveKeepsTheFormWithARetryMessage() = runTest(testDispatcher) {
        coEvery { costoBiseladoDao.upsertAll(any()) } throws IllegalStateException("constraint")
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.showNewBiselado()
        viewModel.updateFormBiselado { it.copy(material = "Resina", tipoAro = "semi_aire", modalidad = "stock", costo = "12") }

        viewModel.saveBiselado()
        advanceUntilIdle()

        assertEquals("No se pudo guardar el precio. Inténtalo de nuevo.", viewModel.uiState.value.formBiselado?.error)
        coVerify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }

        viewModel.dismissFormBiselado()
        assertNull(viewModel.uiState.value.formBiselado)
    }

    @Test
    fun lcFormRejectsInvalidTypeMaterialModalityCostAndDuplicates() = runTest(testDispatcher) {
        val abierta = CostoLcEntity(
            id = "lc-open", opticaId = opticaId, tipoLc = "graduado", materialLc = "Hidrogel",
            modalidad = "mensual", costoUnitario = 15.0, vigenteDesde = "2026-01-01",
        )
        coEvery { costoLcDao.getByOpticaIdList(opticaId) } returns listOf(abierta)
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.selectTab(CostosTab.LC.ordinal)
        advanceUntilIdle()
        viewModel.showNewLc()

        viewModel.saveLc()
        assertEquals("Selecciona el tipo de lente de contacto", viewModel.uiState.value.formLc?.error)

        viewModel.updateFormLc { it.copy(tipoLc = "graduado", material = " ") }
        viewModel.saveLc()
        assertEquals("Ingresa el material", viewModel.uiState.value.formLc?.error)

        viewModel.updateFormLc { it.copy(material = "Hidrogel", modalidad = "semanal") }
        viewModel.saveLc()
        assertEquals("Selecciona la modalidad", viewModel.uiState.value.formLc?.error)

        viewModel.updateFormLc { it.copy(modalidad = "mensual", costo = "0") }
        viewModel.saveLc()
        assertEquals("Ingresa un costo válido", viewModel.uiState.value.formLc?.error)

        viewModel.updateFormLc { it.copy(material = " Hidrogel ", costo = "16") }
        viewModel.saveLc()
        assertTrue(viewModel.uiState.value.formLc?.error.orEmpty().startsWith("Ya existe un precio vigente"))

        coVerify(exactly = 0) { costoLcDao.upsertAll(any()) }
    }

    @Test
    fun aFailedLcSaveKeepsTheFormWithARetryMessage() = runTest(testDispatcher) {
        coEvery { costoLcDao.upsertAll(any()) } throws IllegalStateException("constraint")
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.showNewLc()
        viewModel.updateFormLc { it.copy(tipoLc = "cosmetico", material = "Silicona", modalidad = "diario", costo = "4") }

        viewModel.saveLc()
        advanceUntilIdle()

        assertEquals("No se pudo guardar el precio. Inténtalo de nuevo.", viewModel.uiState.value.formLc?.error)

        viewModel.dismissFormLc()
        assertNull(viewModel.uiState.value.formLc)
    }

    @Test
    fun aPriceEditWithAnInvalidValueNeverReachesTheUseCase() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.showEditCosto(costo("c-edit"))
        viewModel.updateNuevoCostoUnitario("cero")

        viewModel.saveCostoEdit()
        advanceUntilIdle()

        assertEquals("Ingresa un costo válido", viewModel.uiState.value.error)
        coVerify(exactly = 0) { matrizVigenciaUseCase.editarProducto(any(), any(), any(), any()) }
    }

    @Test
    fun aPriceThatStoppedBeingCurrentClosesTheDialogAndExplainsWhy() = runTest(testDispatcher) {
        val fila = costo("c-stale")
        coEvery { matrizVigenciaUseCase.editarProducto(fila, 25.0, any(), hoy) } returns ResultadoVigencia.YaNoVigente
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()
        viewModel.showEditCosto(fila)
        viewModel.updateNuevoCostoUnitario("25")
        viewModel.updateVigenteDesde(LocalDate.of(2026, 7, 20))

        viewModel.saveCostoEdit()
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertNull(estado.editingCosto)
        assertEquals("Este precio ya no está vigente. Revisa la lista actualizada.", estado.error)
        coVerify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
    }

    @Test
    fun aFailingPriceEditKeepsTheDialogOpenWithARetryMessage() = runTest(testDispatcher) {
        val fila = CostoLcEntity(
            id = "lc-edit", opticaId = opticaId, tipoLc = "graduado", materialLc = "Hidrogel",
            modalidad = "mensual", costoUnitario = 15.0, vigenteDesde = "2026-01-01",
        )
        coEvery { matrizVigenciaUseCase.editarLc(fila, 17.0, any(), hoy) } throws IllegalStateException("io")
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.showEditLc(fila)
        viewModel.updateNuevoCostoUnitario("17")

        viewModel.saveLcEdit()
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(fila, estado.editingLc)
        assertEquals("No se pudo guardar el precio. Inténtalo de nuevo.", estado.error)
    }
}
