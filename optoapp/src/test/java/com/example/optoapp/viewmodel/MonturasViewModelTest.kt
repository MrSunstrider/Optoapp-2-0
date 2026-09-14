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
import java.io.IOException

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
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0

        repository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)

        every { sessionManager.opticaId } returns opticaIdFlow
        every { sessionManager.opticaRol } returns opticaRolFlow
        every { repository.getMonturasByOptica(opticaId) } returns flowOf(emptyList())
        coEvery { repository.getMonturaById(any(), any()) } returns Resource.Error("missing")
        coEvery { repository.insertMontura(any()) } returns Unit
        coEvery { repository.insertMonturas(any()) } returns Unit
        coEvery { repository.updateMontura(any()) } returns Unit
        coEvery { repository.updateMonturaAndInsertSiblings(any(), any()) } returns Unit
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

    @Test
    fun `save montura create with two tipos inserts two rows with per-type stock`() = runTest(testDispatcher) {
        createVm()
        viewModel.startCreate()
        viewModel.updateForm {
            it.copy(
                tipoItem = InventarioItemKind.MONTURA,
                sku = "RAY-2140",
                marca = "Ray-Ban",
                modelo = "Aviator",
                materialMontura = "Metal",
                selectedTiposAro = setOf("Aro Completo", "Semi al aire"),
                stockPorTipoAro = mapOf(
                    "Aro Completo" to "5",
                    "Semi al aire" to "3",
                ),
                stockMinimo = "1",
            )
        }
        viewModel.save()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals("Se crearon 2 variantes", viewModel.uiState.value.success)
        val batch = slot<List<Montura>>()
        coVerify(exactly = 1) { repository.insertMonturas(capture(batch)) }
        coVerify(exactly = 0) { repository.insertMontura(any()) }
        assertEquals(2, batch.captured.size)
        assertEquals(setOf("Aro Completo", "Semi al aire"), batch.captured.map { it.tipoAro }.toSet())
        assertEquals(5, batch.captured.first { it.tipoAro == "Aro Completo" }.stockActual)
        assertEquals(3, batch.captured.first { it.tipoAro == "Semi al aire" }.stockActual)
        assertTrue(batch.captured.all { it.sku == "RAY-2140" })
        assertTrue(batch.captured.map { it.id }.distinct().size == 2)
    }

    @Test
    fun `save montura create with Aluminio material persists material`() = runTest(testDispatcher) {
        createVm()
        viewModel.startCreate()
        viewModel.updateForm {
            it.copy(
                tipoItem = InventarioItemKind.MONTURA,
                sku = "ALU-1",
                marca = "X",
                modelo = "Y",
                materialMontura = "Aluminio",
                selectedTiposAro = setOf("Aro Completo"),
                stockPorTipoAro = mapOf("Aro Completo" to "2"),
            )
        }
        viewModel.save()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        val slot = slot<Montura>()
        coVerify(exactly = 1) { repository.insertMontura(capture(slot)) }
        assertEquals("Aluminio", slot.captured.materialMontura)
        assertEquals(2, slot.captured.stockActual)
    }

    @Test
    fun `edit save keeps selected tipoAro and Aluminio material`() = runTest(testDispatcher) {
        val existing = Montura(
            id = "edit-1",
            sku = "RAY-2140",
            marca = "Ray-Ban",
            modelo = "Aviator",
            tipoAro = "Aro Completo",
            materialMontura = "Metal",
            stockActual = 5,
            stockMinimo = 1,
            opticaId = opticaId,
        )
        coEvery { repository.getMonturaById("edit-1", opticaId) } returns Resource.Success(existing)

        createVm()
        viewModel.startEdit(existing)
        viewModel.updateForm {
            it.copy(
                tipoAro = "Semi al aire",
                materialMontura = "Aluminio",
                stockActual = "5",
                stockMinimo = "1",
            )
        }
        viewModel.save()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        val slot = slot<Montura>()
        coVerify(exactly = 1) { repository.updateMontura(capture(slot)) }
        assertEquals("Semi al aire", slot.captured.tipoAro)
        assertEquals("Aluminio", slot.captured.materialMontura)
    }

    @Test
    fun `edit save accesorio keeps empty tipoAro and material`() = runTest(testDispatcher) {
        val existing = Montura(
            id = "acc-1",
            sku = "LIQ-1",
            marca = "Opti",
            modelo = "Limpiador",
            categoria = InventarioItemKind.ACCESORIO,
            tipoAro = "",
            materialMontura = "",
            stockActual = 3,
            stockMinimo = 1,
            opticaId = opticaId,
        )
        coEvery { repository.getMonturaById("acc-1", opticaId) } returns Resource.Success(existing)

        createVm()
        viewModel.startEdit(existing)
        viewModel.updateForm {
            it.copy(
                tipoItem = InventarioItemKind.ACCESORIO,
                modelo = "Limpiador Pro",
                stockActual = "4",
                stockMinimo = "1",
            )
        }
        viewModel.save()
        advanceUntilIdle()

        val slot = slot<Montura>()
        coVerify(exactly = 1) { repository.updateMontura(capture(slot)) }
        assertEquals("", slot.captured.tipoAro)
        assertEquals("", slot.captured.materialMontura)
        assertEquals(InventarioItemKind.ACCESORIO, slot.captured.categoria)
    }

    @Test
    fun `soft delete sets activo false via softDeleteMontura and shows desactivado`() = runTest(testDispatcher) {
        val active = Montura(
            id = "del-1",
            sku = "SKU-D",
            marca = "A",
            modelo = "B",
            activo = true,
            opticaId = opticaId,
        )
        coEvery { repository.softDeleteMontura(any()) } returns Unit

        createVm()
        viewModel.delete(active)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.softDeleteMontura(active) }
        coVerify(exactly = 0) { repository.deleteMontura(any()) }
        assertEquals("Producto desactivado", viewModel.uiState.value.success)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `soft delete unauthorized role surfaces Spanish error and does not call repo`() = runTest(testDispatcher) {
        opticaRolFlow.value = "vendedor"
        val active = Montura(id = "del-2", sku = "S", marca = "A", modelo = "B", opticaId = opticaId)

        createVm()
        viewModel.delete(active)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.softDeleteMontura(any()) }
        coVerify(exactly = 0) { repository.deleteMontura(any()) }
        assertTrue(viewModel.uiState.value.error!!.contains("permiso", ignoreCase = true))
        assertNull(viewModel.uiState.value.success)
    }

    @Test
    fun `soft delete persistence failure surfaces Spanish error`() = runTest(testDispatcher) {
        coEvery { repository.softDeleteMontura(any()) } throws IOException("disk")

        createVm()
        viewModel.delete(Montura(id = "del-3", sku = "S", marca = "A", modelo = "B", opticaId = opticaId))
        advanceUntilIdle()

        assertEquals("Error inesperado. Reintente más tarde.", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.success)
    }

    @Test
    fun `sortedMonturas hides inactive rows`() = runTest(testDispatcher) {
        val active = Montura(id = "a1", sku = "A", marca = "Zeiss", modelo = "X", activo = true)
        val inactive = Montura(id = "i1", sku = "I", marca = "Nikon", modelo = "Y", activo = false)
        every { repository.getMonturasByOptica(opticaId) } returns flowOf(listOf(active, inactive))

        createVm()
        advanceUntilIdle()

        val sorted = viewModel.sortedMonturas.value
        assertEquals(listOf("a1"), sorted.map { it.id })
    }

    @Test
    fun `edit sibling spawn inserts shared attrs with new tipo and stock`() = runTest(testDispatcher) {
        val existing = Montura(
            id = "sib-1",
            sku = "RAY-2140",
            marca = "Ray-Ban",
            modelo = "Aviator",
            color = "Negro",
            talla = "58",
            costo = 40.0,
            precio = 120.0,
            stockActual = 5,
            stockMinimo = 1,
            tipoAro = "Aro Completo",
            materialMontura = "Metal",
            opticaId = opticaId,
        )
        coEvery { repository.getMonturaById("sib-1", opticaId) } returns Resource.Success(existing)

        createVm()
        viewModel.startEdit(existing)
        viewModel.updateForm {
            it.copy(
                stockActual = "5",
                stockMinimo = "1",
                siblingTiposAro = setOf("Al aire"),
                siblingStockPorTipo = mapOf("Al aire" to "4"),
            )
        }
        viewModel.save()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        coVerify(exactly = 0) { repository.updateMontura(any()) }
        coVerify(exactly = 0) { repository.insertMonturas(any()) }
        val monturaSlot = slot<Montura>()
        val batch = slot<List<Montura>>()
        coVerify(exactly = 1) {
            repository.updateMonturaAndInsertSiblings(capture(monturaSlot), capture(batch))
        }
        assertEquals("sib-1", monturaSlot.captured.id)
        assertEquals(1, batch.captured.size)
        val sibling = batch.captured.single()
        assertEquals("RAY-2140", sibling.sku)
        assertEquals("Ray-Ban", sibling.marca)
        assertEquals("Aviator", sibling.modelo)
        assertEquals("Negro", sibling.color)
        assertEquals("58", sibling.talla)
        assertEquals(40.0, sibling.costo, 0.0)
        assertEquals(120.0, sibling.precio, 0.0)
        assertEquals(1, sibling.stockMinimo)
        assertEquals("Metal", sibling.materialMontura)
        assertEquals("Al aire", sibling.tipoAro)
        assertEquals(4, sibling.stockActual)
        assertTrue(sibling.id != existing.id)
        assertTrue(viewModel.uiState.value.success!!.contains("1"))
    }

    @Test
    fun `edit sibling UNIQUE conflict shows sku tipo error`() = runTest(testDispatcher) {
        val existing = Montura(
            id = "sib-2",
            sku = "RAY-2140",
            marca = "Ray-Ban",
            modelo = "Aviator",
            stockActual = 5,
            stockMinimo = 1,
            tipoAro = "Aro Completo",
            materialMontura = "Metal",
            opticaId = opticaId,
        )
        coEvery { repository.getMonturaById("sib-2", opticaId) } returns Resource.Success(existing)
        coEvery { repository.updateMonturaAndInsertSiblings(any(), any()) } throws
            Exception("UNIQUE constraint failed")

        createVm()
        viewModel.startEdit(existing)
        viewModel.updateForm {
            it.copy(
                stockActual = "5",
                stockMinimo = "1",
                siblingTiposAro = setOf("Semi al aire"),
                siblingStockPorTipo = mapOf("Semi al aire" to "2"),
            )
        }
        viewModel.save()
        advanceUntilIdle()

        assertEquals("El SKU ya existe para ese tipo de aro.", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.success)
        coVerify(exactly = 0) { repository.updateMontura(any()) }
        coVerify(exactly = 0) { repository.insertMonturas(any()) }
        coVerify(exactly = 1) { repository.updateMonturaAndInsertSiblings(any(), any()) }
    }

    @Test
    fun `edit sibling negative stock skips all repository writes`() = runTest(testDispatcher) {
        val existing = Montura(
            id = "sib-3",
            sku = "RAY-2140",
            marca = "Ray-Ban",
            modelo = "Aviator",
            stockActual = 5,
            stockMinimo = 1,
            tipoAro = "Aro Completo",
            materialMontura = "Metal",
            opticaId = opticaId,
        )
        coEvery { repository.getMonturaById("sib-3", opticaId) } returns Resource.Success(existing)

        createVm()
        viewModel.startEdit(existing)
        viewModel.updateForm {
            it.copy(
                stockActual = "5",
                stockMinimo = "1",
                siblingTiposAro = setOf("Semi al aire"),
                siblingStockPorTipo = mapOf("Semi al aire" to "-1"),
            )
        }
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Stock actual y mínimo no pueden ser negativos.", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.success)
        coVerify(exactly = 0) { repository.updateMontura(any()) }
        coVerify(exactly = 0) { repository.updateMonturaAndInsertSiblings(any(), any()) }
        coVerify(exactly = 0) { repository.insertMonturas(any()) }
    }
}
