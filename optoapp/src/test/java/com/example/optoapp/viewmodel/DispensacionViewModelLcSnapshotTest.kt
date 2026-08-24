package com.example.optoapp.viewmodel

import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Montura
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costobiselado.CostoBiseladoEntity
import com.example.optoapp.data.costolc.CostoLcDao
import com.example.optoapp.data.costolc.CostoLcEntity
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.domain.CalcularMontoPagadoUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DispensacionStockHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DispensacionViewModelLcSnapshotTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val opticaId = "optica-test"
    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var costoProductoDao: CostoProductoDao
    private lateinit var costoBiseladoDao: CostoBiseladoDao
    private lateinit var costoLcDao: CostoLcDao

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        costoProductoDao = mockk(relaxed = true)
        costoBiseladoDao = mockk(relaxed = true)
        costoLcDao = mockk(relaxed = true)
        every { sessionManager.opticaId } returns MutableStateFlow(opticaId)
        every { repository.getMonturasByOptica(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm() = DispensacionViewModel(
        repository,
        sessionManager,
        mockk<PostSaveSyncScheduler>(relaxed = true),
        mockk<DispensacionStockHelper>(relaxed = true),
        mockk<CalcularMontoPagadoUseCase>(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        costoProductoDao,
        costoBiseladoDao,
        costoLcDao,
    )

    @Test
    fun mapTipoLc_mapsCosmeticMedidaGraduadoTerapeutico() {
        assertEquals("cosmetico", mapTipoLc("Lentes de Contacto Cosmético"))
        assertEquals("graduado", mapTipoLc("Lentes de Contacto a Medida"))
        assertEquals("graduado", mapTipoLc("Lentes de Contacto Graduado"))
        assertEquals("terapeutico", mapTipoLc("Lentes de Contacto Terapéutico"))
    }

    @Test
    fun mapTipoLc_unknownLabel_defaultsToGraduado() {
        assertEquals("graduado", mapTipoLc("Bifocal"))
        assertEquals("graduado", mapTipoLc("Natural"))
        assertEquals("graduado", mapTipoLc(""))
        assertEquals("graduado", mapTipoLc("algún tipo desconocido"))
    }

    @Test
    fun calculateCosts_lcNullCost_looksUpCostoLcDao_notLookupLc() = runTest(testDispatcher) {
        val eval = mockk<EvaluacionClinica>(relaxed = true)
        every { eval.lcMaterial } returns "hidrogel"
        every { eval.lcLaboratorio } returns "lab-1"
        every { eval.recetaOdEsf } returns null
        every { eval.recetaOiEsf } returns null
        coEvery { repository.getEvaluacionById("eval-1", opticaId) } returns Resource.Success(eval)
        coEvery {
            costoLcDao.lookup(opticaId, "cosmetico", "hidrogel", "mensual", "lab-1")
        } returns CostoLcEntity(
            id = "cl1",
            opticaId = opticaId,
            tipoLc = "cosmetico",
            materialLc = "hidrogel",
            modalidad = "mensual",
            laboratorioId = "lab-1",
            costoUnitario = 33.0,
            vigenteDesde = "2026-01-01",
        )

        val vm = createVm()
        vm.setEvaluacionId("eval-1")
        vm.updateItem(
            0,
            vm.uiState.value.items[0].copy(
                tipoLente = "Lentes de Contacto Cosmético",
                materialLente = "hidrogel",
                costoRealLc = null,
            ),
        )
        vm.calculateCosts(0)
        advanceUntilIdle()

        assertEquals(33.0, vm.uiState.value.items[0].costoRealLc!!, 0.001)
        coVerify(exactly = 1) {
            costoLcDao.lookup(opticaId, "cosmetico", "hidrogel", "mensual", "lab-1")
        }
        coVerify(exactly = 0) { costoProductoDao.lookupLc(any(), any(), any(), any(), any()) }
    }

    @Test
    fun calculateCosts_lcNonNullCost_preservesOverride() = runTest(testDispatcher) {
        val eval = mockk<EvaluacionClinica>(relaxed = true)
        every { eval.lcMaterial } returns "silicona"
        every { eval.lcLaboratorio } returns null
        every { eval.recetaOdEsf } returns null
        every { eval.recetaOiEsf } returns null
        coEvery { repository.getEvaluacionById("eval-2", opticaId) } returns Resource.Success(eval)
        coEvery {
            costoLcDao.lookup(any(), any(), any(), any(), any())
        } returns CostoLcEntity(
            id = "cl2",
            opticaId = opticaId,
            tipoLc = "graduado",
            materialLc = "silicona",
            modalidad = "mensual",
            costoUnitario = 50.0,
            vigenteDesde = "2026-01-01",
        )

        val vm = createVm()
        vm.setEvaluacionId("eval-2")
        vm.updateItem(
            0,
            vm.uiState.value.items[0].copy(
                tipoLente = "Lentes de Contacto a Medida",
                materialLente = "silicona",
                costoRealLc = 99.0,
            ),
        )
        vm.calculateCosts(0)
        advanceUntilIdle()

        assertEquals(99.0, vm.uiState.value.items[0].costoRealLc!!, 0.001)
    }

    @Test
    fun calculateCosts_lcNoMatch_leavesNull() = runTest(testDispatcher) {
        val eval = mockk<EvaluacionClinica>(relaxed = true)
        every { eval.lcMaterial } returns "hidrogel"
        every { eval.lcLaboratorio } returns null
        every { eval.recetaOdEsf } returns null
        every { eval.recetaOiEsf } returns null
        coEvery { repository.getEvaluacionById("eval-3", opticaId) } returns Resource.Success(eval)
        coEvery { costoLcDao.lookup(any(), any(), any(), any(), any()) } returns null

        val vm = createVm()
        vm.setEvaluacionId("eval-3")
        vm.updateItem(
            0,
            vm.uiState.value.items[0].copy(
                tipoLente = "Lentes de Contacto Cosmético",
                costoRealLc = null,
            ),
        )
        vm.calculateCosts(0)
        advanceUntilIdle()

        assertNull(vm.uiState.value.items[0].costoRealLc)
    }

    // ── S7: mapModalidadLcOrNull ─────────────────────────────────────────────

    @Test
    fun mapModalidadLcOrNull_returnsMatchForEachCatalogKeyword() {
        assertEquals("diario", mapModalidadLcOrNull("diario"))
        assertEquals("diario", mapModalidadLcOrNull("uso Diario"))
        assertEquals("quincenal", mapModalidadLcOrNull("Quincenal reemplazo"))
        assertEquals("mensual", mapModalidadLcOrNull("Mensual"))
        assertEquals("anual", mapModalidadLcOrNull("lente anual"))
    }

    @Test
    fun mapModalidadLcOrNull_returnsNullForUnknownStrings() {
        assertNull(mapModalidadLcOrNull(""))
        assertNull(mapModalidadLcOrNull("graduado"))
        assertNull(mapModalidadLcOrNull("Cosmético"))
        assertNull(mapModalidadLcOrNull("silicona"))
    }

    @Test
    fun mapModalidadLc_defaultsToMensualWhenNoMatch() {
        assertEquals("mensual", mapModalidadLc(""))
        assertEquals("mensual", mapModalidadLc("Cosmético"))
    }

    @Test
    fun calculateCosts_lcModalidadDerivedFromLcTipoLente() = runTest(testDispatcher) {
        val eval = mockk<EvaluacionClinica>(relaxed = true)
        every { eval.lcTipoLente } returns "lente diario"
        every { eval.lcMaterial } returns "hidrogel"
        every { eval.lcLaboratorio } returns null
        every { eval.recetaOdEsf } returns null
        every { eval.recetaOiEsf } returns null
        coEvery { repository.getEvaluacionById("eval-s7", opticaId) } returns Resource.Success(eval)
        coEvery {
            costoLcDao.lookup(opticaId, any(), "hidrogel", "diario", null)
        } returns CostoLcEntity(
            id = "cl-s7",
            opticaId = opticaId,
            tipoLc = "graduado",
            materialLc = "hidrogel",
            modalidad = "diario",
            costoUnitario = 15.0,
            vigenteDesde = "2026-01-01",
        )

        val vm = createVm()
        vm.setEvaluacionId("eval-s7")
        vm.updateItem(
            0,
            vm.uiState.value.items[0].copy(
                tipoLente = "Lentes de Contacto Graduado",
                materialLente = "hidrogel",
                costoRealLc = null,
            ),
        )
        vm.calculateCosts(0)
        advanceUntilIdle()

        assertEquals(15.0, vm.uiState.value.items[0].costoRealLc!!, 0.001)
        coVerify(exactly = 1) {
            costoLcDao.lookup(opticaId, any(), "hidrogel", "diario", null)
        }
    }

    // ── S8: biselado serie derived from cilindro ─────────────────────────────

    @Test
    fun calculateCosts_biseladoSerieDerivedFromOdCil() = runTest(testDispatcher) {
        val eval = mockk<EvaluacionClinica>(relaxed = true)
        every { eval.lcTipoLente } returns null
        every { eval.recetaOdEsf } returns "-3.00"
        every { eval.recetaOdCil } returns "-3.00"
        every { eval.recetaOiEsf } returns null
        every { eval.recetaOiCil } returns null
        coEvery { repository.getEvaluacionById("eval-s8", opticaId) } returns Resource.Success(eval)
        coEvery { costoProductoDao.lookup(any(), any(), any(), any(), any(), any()) } returns null
        coEvery { repository.getMonturaById("mon-1", opticaId) } returns Resource.Success(
            Montura(id = "mon-1"),
        )
        coEvery {
            costoBiseladoDao.lookup(opticaId, any(), any(), any(), 2, null)
        } returns CostoBiseladoEntity(
            id = "bis-s8",
            opticaId = opticaId,
            material = "Resina",
            tipoAro = "aro_completo",
            stockOFabricacion = "stock",
            serie = 2,
            altoIndice = null,
            costoPorPar = 22.0,
            vigenteDesde = "2026-01-01",
        )

        val vm = createVm()
        vm.setEvaluacionId("eval-s8")
        vm.updateItem(
            0,
            vm.uiState.value.items[0].copy(
                tipoLente = "Monofocal",
                materialLente = "Resina",
                origenMontura = "Tienda",
                monturaId = "mon-1",
                tipoAro = "Aro Completo",
                materialMontura = "Resina",
                costoRealBiselado = null,
            ),
        )
        vm.calculateCosts(0)
        advanceUntilIdle()

        assertEquals(22.0, vm.uiState.value.items[0].costoRealBiselado!!, 0.001)
        coVerify(exactly = 1) {
            costoBiseladoDao.lookup(opticaId, any(), any(), any(), 2, null)
        }
    }

    @Test
    fun calculateCosts_biseladoSerieNullWhenNoCylData() = runTest(testDispatcher) {
        val eval = mockk<EvaluacionClinica>(relaxed = true)
        every { eval.lcTipoLente } returns null
        every { eval.recetaOdEsf } returns "-2.00"
        every { eval.recetaOdCil } returns null
        every { eval.recetaOiEsf } returns null
        every { eval.recetaOiCil } returns null
        coEvery { repository.getEvaluacionById("eval-s8b", opticaId) } returns Resource.Success(eval)
        coEvery { costoProductoDao.lookup(any(), any(), any(), any(), any(), any()) } returns null
        coEvery { repository.getMonturaById("mon-2", opticaId) } returns Resource.Success(
            Montura(id = "mon-2"),
        )
        coEvery {
            costoBiseladoDao.lookup(opticaId, any(), any(), any(), null, null)
        } returns null

        val vm = createVm()
        vm.setEvaluacionId("eval-s8b")
        vm.updateItem(
            0,
            vm.uiState.value.items[0].copy(
                tipoLente = "Monofocal",
                materialLente = "Resina",
                origenMontura = "Tienda",
                monturaId = "mon-2",
                tipoAro = "Aro Completo",
                materialMontura = "Resina",
                costoRealBiselado = null,
            ),
        )
        vm.calculateCosts(0)
        advanceUntilIdle()

        assertNull(vm.uiState.value.items[0].costoRealBiselado)
        coVerify(exactly = 1) {
            costoBiseladoDao.lookup(opticaId, any(), any(), any(), null, null)
        }
    }
}
