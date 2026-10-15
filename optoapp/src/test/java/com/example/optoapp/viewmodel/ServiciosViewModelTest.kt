package com.example.optoapp.viewmodel

import com.example.optoapp.data.Montura
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.servicio.ServicioExtraItem
import com.example.optoapp.data.serviciotipo.ServicioTipoDao
import com.example.optoapp.data.serviciotipo.ServicioTipoEntity
import com.example.optoapp.domain.CostoServicioResolver
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ServiciosViewModelTest {

    private val opticaId = "optica-test"
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private val servicioTipoDao = mockk<ServicioTipoDao>(relaxed = true)
    private val insertedItems = mutableListOf<ServicioExtraItem>()
    private val savedServicios = mutableListOf<ServicioExtra>()

    private val biselado = ServicioTipoEntity(
        id = "t1", opticaId = opticaId, nombre = "Biselado", costoPropio = 5.0, costoTercero = 12.0,
    )

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        sessionManager = mockk()
        every { sessionManager.opticaId } returns MutableStateFlow(opticaId)
        every { sessionManager.opticaRol } returns flowOf("admin")
        every { repository.getAllServiciosForOptica(any()) } returns flowOf(emptyList())
        every { repository.pacientesFlowForOptica(any()) } returns flowOf(emptyList())
        every { repository.getAllPagosFlowForOptica(any()) } returns flowOf(emptyList())
        coEvery { repository.getServicioExtraItems(any(), any()) } returns emptyList()
        coEvery { repository.getRegalosByServicioExtraId(any(), any()) } returns emptyList()
        coEvery { repository.getPagosByServicioExtra(any(), any()) } returns flowOf(emptyList())
        coEvery { repository.withTransaction(any<suspend () -> Any>()) } coAnswers { firstArg<suspend () -> Any>()() }
        coEvery { repository.insertServicioExtraItem(any()) } answers { insertedItems += firstArg<ServicioExtraItem>() }
        coEvery { repository.insertServicio(any()) } answers { savedServicios += firstArg<ServicioExtra>() }
        coEvery { repository.updateServicio(any()) } answers { savedServicios += firstArg<ServicioExtra>() }
        stubMontura("m1", costo = 45.0)
        stubMontura("m2", costo = 50.0)
        stubMontura("m-free", costo = 0.0)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic("android.util.Log")
    }

    private fun stubMontura(id: String, costo: Double) {
        coEvery { repository.getMonturaById(id, opticaId) } returns
            Resource.Success(Montura(id = id, costo = costo, stockActual = 5, opticaId = opticaId))
    }

    private fun buildViewModel() = ServiciosViewModel(
        repository, sessionManager, mockk(relaxed = true), mockk(relaxed = true),
        mockk(relaxed = true) { coEvery { adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any()) } returns Result.success(1) },
        servicioTipoDao,
    )

    private fun storedServicio(costoReal: Double = 0.0) = ServicioExtra(
        id = "s1", ot = "S-001", descripcion = "Armazón", montoTotal = 100.0, aCuenta = 0.0, estado = "Pendiente",
        fecha = LocalDate.of(2026, 9, 1), opticaId = opticaId, servicioTipoId = "t1",
        realizadoPor = CostoServicioResolver.TERCERO, realizadoPorNombre = "Laboratorio Sur", costoReal = costoReal,
    )

    private fun storedItem(monturaId: String, costoReal: Double) = ServicioExtraItem(
        id = "i1", servicioExtraId = "s1", monturaId = monturaId, descripcion = "Armazón", monto = 100.0,
        opticaId = opticaId, costoReal = costoReal,
    )

    private fun ServiciosViewModel.setItem(monturaId: String?) {
        updateItem(0, ServicioExtraItemUi(id = "i1", monturaId = monturaId, descripcion = "Armazón", montoDraft = "100"))
    }

    @Test
    fun newItemSnapshotsTheCurrentFrameCost() = runTest(testDispatcher) {
        val vm = buildViewModel()
        vm.setItem("m1")

        vm.saveServicio {}
        advanceUntilIdle()

        assertEquals(45.0, insertedItems.single().costoReal, 0.0)
    }

    @Test
    fun aFreeFrameAndAnItemWithoutFrameSnapshotZero() = runTest(testDispatcher) {
        val vm = buildViewModel()
        vm.setItem("m-free")
        vm.addItem()
        vm.updateItem(1, ServicioExtraItemUi(id = "i2", descripcion = "Ajuste", montoDraft = "10"))

        vm.saveServicio {}
        advanceUntilIdle()

        assertEquals(listOf(0.0, 0.0), insertedItems.map { it.costoReal })
    }

    @Test
    fun editingWithTheSameFrameKeepsTheStoredItemCost() = runTest(testDispatcher) {
        coEvery { repository.getServicioById("s1", opticaId) } returns Resource.Success(storedServicio())
        coEvery { repository.getServicioExtraItems("s1", opticaId) } returns listOf(storedItem("m1", costoReal = 30.0))
        val vm = buildViewModel()
        vm.loadServicio("s1")
        advanceUntilIdle()

        vm.saveServicio {}
        advanceUntilIdle()

        assertEquals(30.0, insertedItems.single().costoReal, 0.0)
    }

    @Test
    fun editingToAnotherFrameTakesItsCurrentCost() = runTest(testDispatcher) {
        coEvery { repository.getServicioById("s1", opticaId) } returns Resource.Success(storedServicio())
        coEvery { repository.getServicioExtraItems("s1", opticaId) } returns listOf(storedItem("m1", costoReal = 30.0))
        val vm = buildViewModel()
        vm.loadServicio("s1")
        advanceUntilIdle()
        vm.setItem("m2")

        vm.saveServicio {}
        advanceUntilIdle()

        assertEquals(50.0, insertedItems.single().costoReal, 0.0)
    }

    @Test
    fun saveSnapshotsTheExecutorAndItsCost() = runTest(testDispatcher) {
        val vm = buildViewModel()
        vm.setItem(null)
        vm.seleccionarRealizadoPor(CostoServicioResolver.TERCERO)
        vm.seleccionarTipoServicio(biselado)
        vm.updateRealizadoPorNombre("Laboratorio Sur")

        vm.saveServicio {}
        advanceUntilIdle()

        val saved = savedServicios.single()
        assertEquals("t1", saved.servicioTipoId)
        assertEquals(CostoServicioResolver.TERCERO, saved.realizadoPor)
        assertEquals("Laboratorio Sur", saved.realizadoPorNombre)
        assertEquals(12.0, saved.costoReal, 0.0)
    }

    @Test
    fun aNewServicioWithoutTypeStoresYoAndZeroCost() = runTest(testDispatcher) {
        val vm = buildViewModel()
        vm.setItem(null)

        vm.saveServicio {}
        advanceUntilIdle()

        val saved = savedServicios.single()
        assertEquals(null, saved.servicioTipoId)
        assertEquals(CostoServicioResolver.PROPIO, saved.realizadoPor)
        assertEquals(null, saved.realizadoPorNombre)
        assertEquals(0.0, saved.costoReal, 0.0)
    }

    @Test
    fun realizadoPorStartsAsYoAndATypePreloadsItsOwnCost() = runTest(testDispatcher) {
        val vm = buildViewModel()

        val inicial = vm.uiState.value.costo.realizadoPor
        vm.seleccionarTipoServicio(biselado)

        assertEquals(CostoServicioResolver.PROPIO, inicial)
        assertEquals("5", vm.uiState.value.costo.costoDraft)
    }

    @Test
    fun aManualCostSurvivesSwitchingToOtroOptico() = runTest(testDispatcher) {
        val vm = buildViewModel()
        vm.seleccionarTipoServicio(biselado)

        vm.updateCostoServicio("8")
        vm.seleccionarRealizadoPor(CostoServicioResolver.TERCERO)

        assertEquals("8", vm.uiState.value.costo.costoDraft)
    }

    @Test
    fun theThirdPartyNameOnlyLivesWhileOtroOpticoIsSelected() = runTest(testDispatcher) {
        val vm = buildViewModel()
        vm.updateRealizadoPorNombre("Ignorado")
        val conYo = vm.uiState.value.costo.realizadoPorNombre
        vm.seleccionarRealizadoPor(CostoServicioResolver.TERCERO)
        vm.updateRealizadoPorNombre("Laboratorio Sur")
        val conOtro = vm.uiState.value.costo.realizadoPorNombre

        vm.seleccionarRealizadoPor(CostoServicioResolver.PROPIO)

        assertEquals("", conYo)
        assertEquals("Laboratorio Sur", conOtro)
        assertEquals("", vm.uiState.value.costo.realizadoPorNombre)
    }

    @Test
    fun aTypeWithZeroCostLeavesAnEmptyDraft() = runTest(testDispatcher) {
        val vm = buildViewModel()

        vm.seleccionarTipoServicio(biselado.copy(costoPropio = 0.0))

        assertEquals("", vm.uiState.value.costo.costoDraft)
    }

    @Test
    fun aNegativeServiceCostBlocksTheSave() = runTest(testDispatcher) {
        val vm = buildViewModel()
        vm.setItem(null)
        vm.updateCostoServicio("-3")

        vm.saveServicio {}
        advanceUntilIdle()

        assertEquals(ServiciosViewModel.ERROR_COSTO_SERVICIO, vm.uiState.value.error)
        assertEquals(emptyList<ServicioExtra>(), savedServicios)
    }

    @Test
    fun anUnreadableServiceCostBlocksTheSave() = runTest(testDispatcher) {
        val vm = buildViewModel()
        vm.setItem(null)
        vm.updateCostoServicio("abc")

        vm.saveServicio {}
        advanceUntilIdle()

        assertEquals(ServiciosViewModel.ERROR_COSTO_SERVICIO, vm.uiState.value.error)
        assertEquals(emptyList<ServicioExtra>(), savedServicios)
    }

    @Test
    fun theSelectorOffersOnlyActiveTypes() = runTest(testDispatcher) {
        every { servicioTipoDao.observeActivosByOptica(opticaId) } returns flowOf(listOf(biselado))
        val vm = buildViewModel()
        backgroundScope.launch { vm.tiposServicio.collect {} }
        advanceUntilIdle()

        assertEquals(listOf(biselado), vm.tiposServicio.value)
    }

    @Test
    fun aSavedServicioStillShowsItsDeactivatedTypeName() = runTest(testDispatcher) {
        coEvery { servicioTipoDao.getById("t1", opticaId) } returns biselado.copy(activo = false)
        coEvery { repository.getServicioById("s1", opticaId) } returns Resource.Success(storedServicio(costoReal = 12.0))
        val vm = buildViewModel()

        vm.loadServicio("s1")
        advanceUntilIdle()

        assertEquals("Biselado", vm.uiState.value.costo.tipo?.nombre)
        assertEquals("12", vm.uiState.value.costo.costoDraft)
    }

    @Test
    fun aCatalogCostChangeNeverRewritesASavedServicio() = runTest(testDispatcher) {
        coEvery { repository.getServicioById("s1", opticaId) } returns Resource.Success(storedServicio(costoReal = 12.0))
        coEvery { repository.getServicioExtraItems("s1", opticaId) } returns listOf(storedItem("m1", costoReal = 30.0))
        val vm = buildViewModel()
        vm.loadServicio("s1")
        advanceUntilIdle()

        vm.saveServicio {}
        advanceUntilIdle()

        val saved = savedServicios.single()
        assertEquals(12.0, saved.costoReal, 0.0)
        assertEquals("t1", saved.servicioTipoId)
        assertEquals("Laboratorio Sur", saved.realizadoPorNombre)
        assertEquals("12", vm.uiState.value.costo.costoDraft)
    }
}
