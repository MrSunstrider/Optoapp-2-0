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
import io.mockk.unmockkAll
import io.mockk.verify
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CostosYGastosViewModelMatricesTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val opticaId = "optica-test"
    private val hoy = LocalDate.of(2026, 7, 20)

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var costoProductoDao: CostoProductoDao
    private lateinit var costoBiseladoDao: CostoBiseladoDao
    private lateinit var costoLcDao: CostoLcDao
    private lateinit var scheduler: PostSaveSyncScheduler
    private lateinit var syncStateTracker: SyncStateTracker
    private lateinit var matrizVigencia: MatrizVigenciaUseCase

    private fun lente(id: String, material: String, tipoLente: String, serie: Int?) = CostoProductoEntity(
        id = id, opticaId = opticaId, material = material, tipoLente = tipoLente,
        stockOFabricacion = "stock", serie = serie, costoUnitario = 10.0, vigenteDesde = "2026-01-01",
    )

    private val biselado = CostoBiseladoEntity(
        id = "b2", opticaId = opticaId, material = "Resina", tipoAro = "semi_aire",
        stockOFabricacion = "fabricacion", serie = 2, altoIndice = "1.67", costoPorPar = 12.5,
        vigenteDesde = "2026-03-01",
    )

    private val lc = CostoLcEntity(
        id = "l2", opticaId = opticaId, tipoLc = "graduado", materialLc = "Hydrogel",
        modalidad = "mensual", costoUnitario = 45.0, vigenteDesde = "2026-03-01",
    )

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
        costoProductoDao = mockk(relaxed = true)
        costoBiseladoDao = mockk(relaxed = true)
        costoLcDao = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        syncStateTracker = mockk(relaxed = true)
        matrizVigencia = mockk()

        every { sessionManager.opticaId } returns flowOf(opticaId)
        every { sessionManager.opticaRol } returns flowOf("admin")
        every { repository.getGastosOperativos(opticaId) } returns emptyFlow()
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        coEvery { costoBiseladoDao.getByOpticaIdList(opticaId) } returns emptyList()
        coEvery { costoLcDao.getByOpticaIdList(opticaId) } returns emptyList()
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
        mockk<SyncFinanzasUseCase>(relaxed = true),
        syncStateTracker,
        matrizVigencia,
        SavedStateHandle(),
    )

    @Test
    fun filteringTheLensBlockByMaterialDropsTheOtherMaterials() = runTest(testDispatcher) {
        every { costoProductoDao.getByBloque(opticaId, "stock") } returns flowOf(
            listOf(lente("r1", "Resina", "Monofocal", 1), lente("c1", "Cristal", "Monofocal", 1)),
        )
        val viewModel = buildViewModel()
        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        viewModel.updateFiltroMaterial("Resina")

        assertEquals(listOf("r1"), viewModel.uiState.value.costosVisibles.map { it.id })
    }

    @Test
    fun lensFiltersCombineMaterialTipoAndSerieAndNullMeansAll() {
        val costos = listOf(
            lente("a", "Resina", "Monofocal", 1),
            lente("b", "Resina", "Bifocal", 1),
            lente("c", "Resina", "Monofocal", 2),
            lente("d", "Cristal", "Monofocal", 1),
        )

        assertEquals(listOf("a"), filtrarCostos(costos, "Resina", "Monofocal", 1).map { it.id })
        assertEquals(listOf("a", "c"), filtrarCostos(costos, "Resina", "Monofocal", null).map { it.id })
        assertEquals(costos, filtrarCostos(costos, null, null, null))
    }

    @Test
    fun editableBlocksAreTheFiveLensBlocksPlusBiseladoAndLcWithoutMonturas() {
        assertEquals(7, MATRIZ_BLOQUES_EDITABLES.size)
        assertEquals(COST_BLOCKS, MATRIZ_BLOQUES_EDITABLES.take(5))
        assertTrue(MATRIZ_BLOQUES_EDITABLES.containsAll(listOf("Biselado", "Lentes de Contacto")))
        assertTrue(MATRIZ_BLOQUES_EDITABLES.none { it.contains("montura", ignoreCase = true) })
    }

    @Test
    fun openingTheBiseladoTabLoadsTheOpenRowsWithEveryDisplayedField() = runTest(testDispatcher) {
        coEvery { costoBiseladoDao.getByOpticaIdList(opticaId) } returns listOf(biselado)
        val viewModel = buildViewModel()

        viewModel.selectTab(CostosTab.BISELADO.ordinal)
        advanceUntilIdle()

        val fila = viewModel.uiState.value.costosBiselado.single()
        assertEquals("Resina", fila.material)
        assertEquals("semi_aire", fila.tipoAro)
        assertEquals("fabricacion", fila.stockOFabricacion)
        assertEquals(2, fila.serie)
        assertEquals("1.67", fila.altoIndice)
        assertEquals(12.5, fila.costoPorPar, 0.001)
    }

    @Test
    fun openingTheLcTabLoadsTheOpenRowsWithEveryDisplayedField() = runTest(testDispatcher) {
        coEvery { costoLcDao.getByOpticaIdList(opticaId) } returns listOf(lc)
        val viewModel = buildViewModel()

        viewModel.selectTab(CostosTab.LC.ordinal)
        advanceUntilIdle()

        val fila = viewModel.uiState.value.costosLc.single()
        assertEquals("graduado", fila.tipoLc)
        assertEquals("Hydrogel", fila.materialLc)
        assertEquals("mensual", fila.modalidad)
        assertEquals(45.0, fila.costoUnitario, 0.001)
    }

    @Test
    fun anEmptyMatrixOffersTheAddPriceActionToWriters() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.selectTab(CostosTab.BISELADO.ordinal)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.costosBiselado.isEmpty())
        assertTrue(viewModel.uiState.value.puedeEditarMatrices)
        viewModel.showNewBiselado()
        assertNotNull(viewModel.uiState.value.formBiselado)

        viewModel.selectTab(CostosTab.LC.ordinal)
        viewModel.showNewLc()
        assertNotNull(viewModel.uiState.value.formLc)
    }

    @Test
    fun rolesWithoutWritePermissionSeeTheMatricesReadOnly() = runTest(testDispatcher) {
        every { sessionManager.opticaRol } returns flowOf("vendedor")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.showNewBiselado()
        viewModel.showNewLc()
        viewModel.showNewCosto()
        viewModel.showEditBiselado(biselado)
        viewModel.showEditLc(lc)
        viewModel.showEditCosto(lente("r1", "Resina", "Monofocal", 1))

        val state = viewModel.uiState.value
        assertFalse(state.puedeEditarMatrices)
        assertNull(state.formBiselado)
        assertNull(state.formLc)
        assertFalse(state.isCostoDialogVisible)
        assertNull(state.editingBiselado)
        assertNull(state.editingLc)
        assertNull(state.editingCosto)
    }

    @Test
    fun aNewBiseladoPriceStoresTheCanonicalAltoIndiceSoRowsStayComparable() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.selectTab(CostosTab.BISELADO.ordinal)
        viewModel.showNewBiselado()
        viewModel.updateFormBiselado {
            it.copy(material = "Resina", tipoAro = "aro_completo", modalidad = "stock", serie = "1", altoIndice = "1,7", costo = "15")
        }

        viewModel.saveBiselado()
        advanceUntilIdle()

        coVerify {
            costoBiseladoDao.upsertAll(
                withArg { filas ->
                    val fila = filas.single()
                    assertEquals("1.70", fila.altoIndice)
                    assertEquals("aro_completo", fila.tipoAro)
                    assertEquals("stock", fila.stockOFabricacion)
                    assertEquals(1, fila.serie)
                    assertEquals(15.0, fila.costoPorPar, 0.001)
                    assertEquals("2026-07-20", fila.vigenteDesde)
                    assertNotNull(fila.updatedAt)
                },
            )
        }
        coVerify { syncStateTracker.markLocallyModified(opticaId, "costo_biselado", any()) }
        verify { scheduler.scheduleFinanzasSync(opticaId) }
        assertNull(viewModel.uiState.value.formBiselado)
    }

    @Test
    fun aNewBiseladoPriceWithoutTipoDeAroIsRejected() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.showNewBiselado()
        viewModel.updateFormBiselado { it.copy(material = "Resina", modalidad = "stock", costo = "15") }

        viewModel.saveBiselado()
        advanceUntilIdle()

        assertEquals("Selecciona el tipo de aro", viewModel.uiState.value.formBiselado?.error)
        coVerify(exactly = 0) { costoBiseladoDao.upsertAll(any()) }
    }

    @Test
    fun aNewBiseladoPriceForAnOpenCombinationIsRejectedSoTheLookupStaysUnambiguous() = runTest(testDispatcher) {
        coEvery { costoBiseladoDao.getByOpticaIdList(opticaId) } returns listOf(biselado)
        val viewModel = buildViewModel()
        viewModel.selectTab(CostosTab.BISELADO.ordinal)
        advanceUntilIdle()
        viewModel.showNewBiselado()
        viewModel.updateFormBiselado {
            it.copy(material = "Resina", tipoAro = "semi_aire", modalidad = "fabricacion", serie = "2", altoIndice = "1,67", costo = "20")
        }

        viewModel.saveBiselado()
        advanceUntilIdle()

        assertEquals(
            "Ya existe un precio vigente para esta combinación. Ábralo en la lista para editarlo.",
            viewModel.uiState.value.formBiselado?.error,
        )
        coVerify(exactly = 0) { costoBiseladoDao.upsertAll(any()) }
    }

    @Test
    fun aNewLcPriceOutsideTheServerModalidadesIsRejected() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.showNewLc()
        viewModel.updateFormLc { it.copy(tipoLc = "graduado", material = "Hydrogel", modalidad = "semanal", costo = "40") }

        viewModel.saveLc()
        advanceUntilIdle()

        assertEquals("Selecciona la modalidad", viewModel.uiState.value.formLc?.error)
        coVerify(exactly = 0) { costoLcDao.upsertAll(any()) }
    }

    @Test
    fun aNewLcPriceIsMarkedForUploadAndSchedulesSync() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.selectTab(CostosTab.LC.ordinal)
        viewModel.showNewLc()
        viewModel.updateFormLc { it.copy(tipoLc = "graduado", material = "Hydrogel", modalidad = "mensual", costo = "40") }

        viewModel.saveLc()
        advanceUntilIdle()

        coVerify {
            costoLcDao.upsertAll(
                withArg { filas ->
                    val fila = filas.single()
                    assertEquals("graduado", fila.tipoLc)
                    assertEquals("Hydrogel", fila.materialLc)
                    assertEquals("mensual", fila.modalidad)
                    assertEquals(40.0, fila.costoUnitario, 0.001)
                },
            )
        }
        coVerify { syncStateTracker.markLocallyModified(opticaId, "costo_lc", any()) }
        verify { scheduler.scheduleFinanzasSync(opticaId) }
        assertNull(viewModel.uiState.value.formLc)
    }

    @Test
    fun editingABiseladoPriceShowsItsHistoryAndSchedulesSyncOnSave() = runTest(testDispatcher) {
        val anterior = biselado.copy(id = "b1", costoPorPar = 10.0, vigenteDesde = "2026-01-01", vigenteHasta = "2026-02-28")
        coEvery {
            costoBiseladoDao.getHistorial(opticaId, "Resina", "semi_aire", "fabricacion", 2, "1.67")
        } returns listOf(biselado, anterior)
        val desde = LocalDate.of(2026, 8, 1)
        coEvery { matrizVigencia.editarBiselado(biselado, 14.0, desde, hoy) } returns ResultadoVigencia.Aplicado("b3")
        val viewModel = buildViewModel()

        viewModel.showEditBiselado(biselado)
        advanceUntilIdle()
        assertEquals(hoy, viewModel.uiState.value.vigenteDesde)
        assertEquals(
            listOf(VersionPrecio("b2", 12.5, "2026-03-01", null), VersionPrecio("b1", 10.0, "2026-01-01", "2026-02-28")),
            viewModel.uiState.value.historialPrecios,
        )

        viewModel.updateNuevoCostoUnitario("14")
        viewModel.updateVigenteDesde(desde)
        viewModel.saveBiseladoEdit()
        advanceUntilIdle()

        coVerify { matrizVigencia.editarBiselado(biselado, 14.0, desde, hoy) }
        verify { scheduler.scheduleFinanzasSync(opticaId) }
        assertNull(viewModel.uiState.value.editingBiselado)
    }

    @Test
    fun editingAnLcPriceWithAnEarlierDateShowsTheFieldErrorAndSchedulesNothing() = runTest(testDispatcher) {
        coEvery { costoLcDao.getHistorial(opticaId, "graduado", "Hydrogel", "mensual", null) } returns listOf(lc)
        val antes = LocalDate.of(2026, 7, 1)
        coEvery { matrizVigencia.editarLc(lc, 50.0, antes, hoy) } returns ResultadoVigencia.Rechazado(hoy)
        val viewModel = buildViewModel()

        viewModel.showEditLc(lc)
        viewModel.updateNuevoCostoUnitario("50")
        viewModel.updateVigenteDesde(antes)
        viewModel.saveLcEdit()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.editingLc)
        assertEquals("La fecha debe ser el 20/07/2026 o posterior", viewModel.uiState.value.vigenteDesdeError)
        verify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
    }

    @Test
    fun anAppliedLcEditSchedulesSyncAndRefreshesTheTab() = runTest(testDispatcher) {
        val desde = LocalDate.of(2026, 8, 1)
        coEvery { matrizVigencia.editarLc(lc, 50.0, desde, hoy) } returns ResultadoVigencia.Aplicado("l3")
        val viewModel = buildViewModel()
        viewModel.selectTab(CostosTab.LC.ordinal)
        advanceUntilIdle()

        viewModel.showEditLc(lc)
        viewModel.updateNuevoCostoUnitario("50")
        viewModel.updateVigenteDesde(desde)
        viewModel.saveLcEdit()
        advanceUntilIdle()

        verify { scheduler.scheduleFinanzasSync(opticaId) }
        coVerify(atLeast = 2) { costoLcDao.getByOpticaIdList(opticaId) }
        assertNull(viewModel.uiState.value.editingLc)
    }
}
