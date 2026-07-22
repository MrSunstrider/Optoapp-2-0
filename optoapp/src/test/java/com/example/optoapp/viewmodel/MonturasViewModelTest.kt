package com.example.optoapp.viewmodel

import com.example.optoapp.data.Montura
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `sortedMonturas sorts by name when sortBy is name`() = runTest(testDispatcher) {
        val monturaA = Montura(id = "a", sku = "SKU-A", marca = "Zeiss", modelo = "Alpha", stockActual = 5, stockMinimo = 3)
        val monturaB = Montura(id = "b", sku = "SKU-B", marca = "Nikon", modelo = "Beta", stockActual = 10, stockMinimo = 5)
        every { repository.getMonturasByOptica(opticaId) } returns flowOf(listOf(monturaA, monturaB))

        viewModel = MonturasViewModel(repository, sessionManager)
        viewModel.setSortBy("name")
        advanceUntilIdle()

        val sorted = viewModel.sortedMonturas.value
        assertEquals(2, sorted.size)
        // Sorted by marca then modelo: Nikon < Zeiss
        assertEquals("Nikon", sorted[0].marca)
        assertEquals("Zeiss", sorted[1].marca)
    }

    @Test
    fun `sortedMonturas sorts by stock descending when sortBy is stock_desc`() = runTest(testDispatcher) {
        val lowStock = Montura(id = "c", sku = "SKU-C", marca = "A", modelo = "X", stockActual = 3, stockMinimo = 5)
        val highStock = Montura(id = "d", sku = "SKU-D", marca = "B", modelo = "Y", stockActual = 20, stockMinimo = 5)
        every { repository.getMonturasByOptica(opticaId) } returns flowOf(listOf(lowStock, highStock))

        viewModel = MonturasViewModel(repository, sessionManager)
        viewModel.setSortBy("stock_desc")
        advanceUntilIdle()

        val sorted = viewModel.sortedMonturas.value
        assertEquals(2, sorted.size)
        // High stock first
        assertEquals("d", sorted[0].id)
        assertEquals("c", sorted[1].id)
    }

    @Test
    fun `sortedMonturas sorts by precio descending when sortBy is precio_desc`() = runTest(testDispatcher) {
        val cheap = Montura(id = "e", sku = "SKU-E", marca = "A", modelo = "X", precio = 50.0, stockActual = 1, stockMinimo = 1)
        val expensive = Montura(id = "f", sku = "SKU-F", marca = "B", modelo = "Y", precio = 200.0, stockActual = 1, stockMinimo = 1)
        every { repository.getMonturasByOptica(opticaId) } returns flowOf(listOf(cheap, expensive))

        viewModel = MonturasViewModel(repository, sessionManager)
        viewModel.setSortBy("precio_desc")
        advanceUntilIdle()

        val sorted = viewModel.sortedMonturas.value
        assertEquals(2, sorted.size)
        // Expensive first
        assertEquals("f", sorted[0].id)
        assertEquals("e", sorted[1].id)
    }

    @Test
    fun `porReponerMonturas returns only active low-stock items sorted by urgency`() = runTest(testDispatcher) {
        val activeLow = Montura(id = "g", sku = "SKU-G", marca = "A", modelo = "X", stockActual = 2, stockMinimo = 5, activo = true)
        val inactiveLow = Montura(id = "h", sku = "SKU-H", marca = "B", modelo = "Y", stockActual = 1, stockMinimo = 3, activo = false)
        val activeOk = Montura(id = "i", sku = "SKU-I", marca = "C", modelo = "Z", stockActual = 10, stockMinimo = 5, activo = true)
        every { repository.getMonturasByOptica(opticaId) } returns flowOf(listOf(activeLow, inactiveLow, activeOk))

        viewModel = MonturasViewModel(repository, sessionManager)
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

        viewModel = MonturasViewModel(repository, sessionManager)
        viewModel.onQueryChange("Zeiss")
        viewModel.setSortBy("name")
        advanceUntilIdle()

        val sorted = viewModel.sortedMonturas.value
        assertEquals(2, sorted.size)
        assertTrue(sorted.all { it.marca == "Zeiss" })
        // Sorted by marca then modelo: Zeiss A < Zeiss B
        assertEquals("A", sorted[0].modelo)
        assertEquals("B", sorted[1].modelo)
    }
}
