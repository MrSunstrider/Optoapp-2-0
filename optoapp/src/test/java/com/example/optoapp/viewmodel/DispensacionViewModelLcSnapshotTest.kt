package com.example.optoapp.viewmodel

import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
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
}
