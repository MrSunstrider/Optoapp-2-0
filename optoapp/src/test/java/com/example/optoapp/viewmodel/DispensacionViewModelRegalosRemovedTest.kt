package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costolc.CostoLcDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.domain.CalcularMontoPagadoUseCase
import com.example.optoapp.domain.CancelDispensacionUseCase
import com.example.optoapp.domain.ReclaimDispensacionUseCase
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DispensacionViewModelRegalosRemovedTest {

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var postSaveSyncScheduler: PostSaveSyncScheduler
    private lateinit var stockHelper: DispensacionStockHelper
    private lateinit var calcularMontoPagadoUseCase: CalcularMontoPagadoUseCase

    private val opticaIdFlow = MutableStateFlow("optica-test")
    private val testDispatcher = UnconfinedTestDispatcher()

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

        every { sessionManager.opticaId } returns opticaIdFlow
        every { sessionManager.opticaRol } returns MutableStateFlow("admin")
        coEvery { calcularMontoPagadoUseCase(any(), any()) } returns 0.0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = DispensacionViewModel(
        repository, sessionManager, postSaveSyncScheduler, stockHelper,
        calcularMontoPagadoUseCase,
        mockk<CancelDispensacionUseCase>(relaxed = true),
        mockk<ReclaimDispensacionUseCase>(relaxed = true),
        mockk<CostoProductoDao>(relaxed = true),
        mockk<CostoBiseladoDao>(relaxed = true),
        mockk<CostoLcDao>(relaxed = true),
    )

    @Test
    fun `saveDispensacion does NOT call insertRegalo even when regalos in state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val regalo = RegaloDispensacionUi(
            id = "reg-1",
            productoId = "prod-1",
            descripcion = "Montura regalo",
            cantidad = 1,
            costoUnitario = 50.0,
            motivo = "Promo",
        )
        viewModel.updateUiState { it.copy(regalos = listOf(regalo), montoTotal = "100") }
        advanceUntilIdle()

        var completed = false
        viewModel.saveDispensacion("pac-1", null) { completed = true }
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.insertRegalo(any()) }
        coVerify(exactly = 0) { repository.deleteRegalosByDispensacionId(any(), any()) }
    }

    @Test
    fun `saveDispensacion does NOT call getRegalosByDispensacionId on edit`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateUiState { it.copy(montoTotal = "100") }
        advanceUntilIdle()

        var completed = false
        viewModel.saveDispensacion("pac-1", "disp-existing") { completed = true }
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.getRegalosByDispensacionId(any(), any()) }
    }
}
