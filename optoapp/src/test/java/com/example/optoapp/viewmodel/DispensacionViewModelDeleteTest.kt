package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.domain.CalcularMontoPagadoUseCase
import com.example.optoapp.domain.EliminarDispensacionUseCase
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
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class DispensacionViewModelDeleteTest {

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var postSaveSyncScheduler: PostSaveSyncScheduler
    private lateinit var stockHelper: DispensacionStockHelper
    private lateinit var calcularMontoPagadoUseCase: CalcularMontoPagadoUseCase
    private lateinit var costoProductoDao: CostoProductoDao
    private lateinit var costoBiseladoDao: CostoBiseladoDao
    private lateinit var eliminar: EliminarDispensacionUseCase
    private lateinit var viewModel: DispensacionViewModel

    private val opticaIdFlow = MutableStateFlow("optica-test")
    private val opticaRolFlow = MutableStateFlow("admin")
    private val testDispatcher = StandardTestDispatcher()
    private val dispId = "disp-delete-1"

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0

        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        sessionManager = mockk()
        postSaveSyncScheduler = mockk(relaxed = true)
        stockHelper = mockk(relaxed = true)
        calcularMontoPagadoUseCase = mockk()
        costoProductoDao = mockk(relaxed = true)
        costoBiseladoDao = mockk(relaxed = true)
        eliminar = mockk(relaxed = true)

        every { sessionManager.opticaId } returns opticaIdFlow
        every { sessionManager.opticaRol } returns opticaRolFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = DispensacionViewModel(
        repository,
        sessionManager,
        postSaveSyncScheduler,
        stockHelper,
        calcularMontoPagadoUseCase,
        mockk<com.example.optoapp.domain.AnularDispensacionUseCase>(relaxed = true),
        mockk<com.example.optoapp.domain.ReclamarDispensacionUseCase>(relaxed = true),
        costoProductoDao,
        costoBiseladoDao,
        eliminar,
    ).also { testDispatcher.scheduler.advanceUntilIdle() }

    @Test
    fun `deleteDispensacion delegates to the eliminar use case without restocking`() = runTest(testDispatcher) {
        viewModel = createViewModel()

        var completed = false
        viewModel.deleteDispensacion(dispId) { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { eliminar(dispId, "optica-test") }
        assertTrue(completed)
        assertNull(viewModel.uiState.value.error)
        coVerify(exactly = 0) { stockHelper.adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.deleteDispensacion(any()) }
        coVerify(exactly = 0) { repository.insertPago(any<Pago>()) }
    }

    @Test
    fun `deleteDispensacion rejects asesor without invoking the use case`() = runTest(testDispatcher) {
        opticaRolFlow.value = "asesor"
        viewModel = createViewModel()

        var completed = false
        viewModel.deleteDispensacion(dispId) { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { eliminar(any(), any()) }
        assertFalse(completed)
        assertTrue(viewModel.uiState.value.error!!.contains("eliminar dispensación"))
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `deleteDispensacion surfaces the use case rejection for orders with trace`() = runTest(testDispatcher) {
        coEvery { eliminar(dispId, any()) } throws IllegalStateException("La orden tiene pagos o movimientos de stock. Usa Anular.")
        viewModel = createViewModel()

        var completed = false
        viewModel.deleteDispensacion(dispId) { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(completed)
        assertEquals("La orden tiene pagos o movimientos de stock. Usa Anular.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
