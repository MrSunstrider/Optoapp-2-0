package com.example.optoapp.viewmodel

import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
class DispensacionViewModelCreateSaveTest {

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var calcularMontoPagadoUseCase: CalcularMontoPagadoUseCase
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
        calcularMontoPagadoUseCase = mockk()
        every { sessionManager.opticaId } returns MutableStateFlow("optica-test")
        every { sessionManager.opticaRol } returns MutableStateFlow("admin")
        coEvery { calcularMontoPagadoUseCase(any(), any()) } returns 0.0
        every { repository.runInTransaction(any()) } answers {
            firstArg<() -> Unit>().invoke()
        }
    }

    @After
    fun tearDown() {
        runBlocking { delay(200) }
        Dispatchers.resetMain()
    }

    private fun createViewModel() = DispensacionViewModel(
        repository,
        sessionManager,
        mockk<PostSaveSyncScheduler>(relaxed = true),
        mockk<DispensacionStockHelper>(relaxed = true),
        calcularMontoPagadoUseCase,
        mockk<CancelDispensacionUseCase>(relaxed = true),
        mockk<ReclaimDispensacionUseCase>(relaxed = true),
        mockk<CostoProductoDao>(relaxed = true),
        mockk<CostoBiseladoDao>(relaxed = true),
    )

    private fun minimalItem() = DispensacionItemUi(tipoLente = "Monofocal")

    @Test
    fun `saveDispensacion on create allows empty montoTotal and forces Pendiente`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateUiState {
            it.copy(
                ot = "OT-2026-0001",
                items = listOf(minimalItem()),
                montoTotal = "",
                estadoEntrega = "Entregado",
            )
        }
        advanceUntilIdle()

        viewModel.saveDispensacion("pac-1", null) {}
        coVerify(timeout = 10_000) {
            repository.insertDispensacion(
                withArg { disp ->
                    assertEquals(0.0, disp.montoTotal, 0.001)
                    assertEquals("Pendiente", disp.estadoEntrega)
                    assertNull(disp.fechaEntrega)
                },
            )
        }
        runBlocking {
            withTimeout(5_000) {
                while (viewModel.uiState.value.isLoading) {
                    delay(10)
                }
            }
        }
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `saveDispensacion on edit still requires montoTotal greater than zero`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { repository.getDispensacionItemsByDispensacion("disp-existing", "optica-test") } returns emptyList()

        viewModel.updateUiState {
            it.copy(ot = "OT-2026-0001", items = listOf(minimalItem()), montoTotal = "")
        }
        advanceUntilIdle()

        viewModel.saveDispensacion("pac-1", "disp-existing") {}
        advanceUntilIdle()

        assertEquals(
            FinanzasRemoteDefaults.Messages.MONTO_TOTAL_MAYOR_A_CERO,
            viewModel.uiState.value.error,
        )
        coVerify(exactly = 0) { repository.updateDispensacion(any()) }
    }
}
