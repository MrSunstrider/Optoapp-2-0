package com.example.optoapp.viewmodel

import com.example.optoapp.data.Montura
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.inventario.InventarioItemKind
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonturasViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: MonturasViewModel

    private val opticaId = "optica-test"
    private val opticaIdFlow = MutableStateFlow(opticaId)
    private val opticaRolFlow = MutableStateFlow("admin")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0

        repository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)

        every { sessionManager.opticaId } returns opticaIdFlow
        every { sessionManager.opticaRol } returns opticaRolFlow
        every { repository.getMonturasByOptica(opticaId) } returns flowOf(emptyList())
        coEvery { repository.getMonturaById(any(), any()) } returns Resource.Error("missing")
        coEvery { repository.insertMontura(any()) } returns Unit
        coEvery { repository.updateMontura(any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createVm() {
        viewModel = MonturasViewModel(repository, sessionManager)
    }

    @Test
    fun `sortedMonturas sorts by name when sortBy is name`() = runTest(testDispatcher) {
        val monturaA = Montura(id = "a", sku = "SKU-A", marca = "Zeiss", modelo = "Alpha", stockActual = 5, stockMinimo = 3)
        val monturaB = Montura(id = "b", sku = "SKU-B", marca = "Nikon", modelo = "Beta", stockActual = 10, stockMinimo = 5)
        every { repository.getMonturasByOptica(opticaId) } returns flowOf(listOf(monturaA, monturaB))

        createVm()
        viewModel.setSortBy("name")
        advanceUntilIdle()

        val sorted = viewModel.sortedMonturas.value
        assertEquals(2, sorted.size)
        assertEquals("Nikon", sorted[0].marca)
        assertEquals("Zeiss", sorted[1].marca)
    }

    @Test
    fun `sortedMonturas sorts by stock descending when sortBy is stock_desc`() = runTest(testDispatcher) {
        val lowStock = Montura(id = "c", sku = "SKU-C", marca = "A", modelo = "X", stockActual = 3, stockMinimo = 5)
        val highStock = Montura(id = "d", sku = "SKU-D", marca = "B", modelo = "Y", stockActual = 20, stockMinimo = 5)
        every { repository.getMonturasByOptica(opticaId) } returns flowOf(listOf(lowStock, highStock))

        createVm()
        viewModel.setSortBy("stock_desc")
        advanceUntilIdle()

        val sorted = viewModel.sortedMonturas.value
        assertEquals(2, sorted.size)
        assertEquals("d", sorted[0].id)
        assertEquals("c", sorted[1].id)
    }

    @Test
    fun `sortedMonturas sorts by precio descending when sortBy is precio_desc`() = runTest(testDispatcher) {
        val cheap = Montura(id = "e", sku = "SKU-E", marca = "A", modelo = "X", precio = 50.0, stockActual = 1, stockMinimo = 1)
        val expensive = Montura(id = "f", sku = "SKU-F", marca = "B", modelo = "Y", precio = 200.0, stockActual = 1, stockMinimo = 1)
        every { repository.getMonturasByOptica(opticaId) } returns flowOf(listOf(cheap, expensive))

        createVm()
        viewModel.setSortBy("precio_desc")
        advanceUntilIdle()

        val sorted = viewModel.sortedMonturas.value
        assertEquals(2, sorted.size)
        assertEquals("f", sorted[0].id)
        assertEquals("e", sorted[1].id)
    }

    @Test
    fun `porReponerMonturas returns only active low-stock items sorted by urgency`() = runTest(testDispatcher) {
        val activeLow = Montura(id = "g", sku = "SKU-G", marca = "A", modelo = "X", stockActual = 2, stockMinimo = 5, activo = true)
        val inactiveLow = Montura(id = "h", sku = "SKU-H", marca = "B", modelo = "Y", stockActual = 1, stockMinimo = 3, activo = false)
        val activeOk = Montura(id = "i", sku = "SKU-I", marca = "C", modelo = "Z", stockActual = 10, stockMinimo = 5, activo = true)
        every { repository.getMonturasByOptica(opticaId) } returns flowOf(listOf(activeLow, inactiveLow, activeOk))

        createVm()
        advanceUntilIdle()

        val porReponer = viewModel.porReponerMonturas.value
        assertEquals(1, porReponer.size)
        assertEquals("g", porReponer[0].id)
    }

    @Test
    fun `sortedMonturas applies query filter then sort`() = runTest(testDispatcher) {
        val zeissA = Montura(id = "z1", sku = "ZE-1", marca = "Zeiss", modelo = "A", stockActual = 5, stockMinimo = 3)
        val nikonB = Montura(id = "n1", sku = "NI-1", marca = "Nikon", modelo = "B", stockActual = 10, stockMinimo = 5)
        val zeissB = Montura(id = "z2", sku = "ZE-2", marca = "Zeiss", modelo = "B", stockActual = 8, stockMinimo = 3)
        every { repository.getMonturasByOptica(opticaId) } returns flowOf(listOf(zeissA, nikonB, zeissB))

        createVm()
        viewModel.onQueryChange("Zeiss")
        viewModel.setSortBy("name")
        advanceUntilIdle()

        val sorted = viewModel.sortedMonturas.value
        assertEquals(2, sorted.size)
        assertTrue(sorted.all { it.marca == "Zeiss" })
        assertEquals("A", sorted[0].modelo)
        assertEquals("B", sorted[1].modelo)
    }

    @Test
    fun `sortedMonturas matches color in query`() = runTest(testDispatcher) {
        val black = Montura(id = "1", sku = "S1", marca = "X", modelo = "M", color = "Negro", stockActual = 1, stockMinimo = 0)
        val red = Montura(id = "2", sku = "S2", marca = "Y", modelo = "N", color = "Rojo", stockActual = 1, stockMinimo = 0)
        every { repository.getMonturasByOptica(opticaId) } returns flowOf(listOf(black, red))

        createVm()
        viewModel.onQueryChange("rojo")
        advanceUntilIdle()

        assertEquals(listOf("2"), viewModel.sortedMonturas.value.map { it.id })
    }

    @Test
    fun `save montura without tipoAro sets error and does not insert`() = runTest(testDispatcher) {
        createVm()
        viewModel.startCreate()
        viewModel.updateForm {
            it.copy(
                tipoItem = InventarioItemKind.MONTURA,
                sku = "SKU-1",
                marca = "Ray",
                modelo = "Ban",
                tipoAro = "",
                materialMontura = "Acetato",
                stockActual = "1",
                stockMinimo = "0",
            )
        }
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Tipo de aro es obligatorio.", viewModel.uiState.value.error)
        coVerify(exactly = 0) { repository.insertMontura(any()) }
    }

    @Test
    fun `save accesorio without aro inserts with categoria ACCESORIO`() = runTest(testDispatcher) {
        createVm()
        viewModel.startCreate()
        viewModel.updateForm {
            it.copy(
                tipoItem = InventarioItemKind.ACCESORIO,
                sku = "LIQ-1",
                marca = "Liq1",
                modelo = "Liquido Limpiador",
                tipoAro = "",
                materialMontura = "",
                costo = "3.75",
                precio = "10",
                stockActual = "5",
                stockMinimo = "1",
            )
        }
        viewModel.save()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals("Accesorio guardado", viewModel.uiState.value.success)
        val slot = slot<Montura>()
        coVerify(exactly = 1) { repository.insertMontura(capture(slot)) }
        assertEquals(InventarioItemKind.ACCESORIO, slot.captured.categoria)
        assertEquals("", slot.captured.tipoAro)
        assertEquals("", slot.captured.materialMontura)
    }
}
