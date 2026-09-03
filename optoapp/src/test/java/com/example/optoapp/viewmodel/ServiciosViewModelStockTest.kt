package com.example.optoapp.viewmodel

import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.CancelServicioExtraUseCase
import com.example.optoapp.domain.movimientoReferenciaForServicioExtraReverso
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DispensacionStockHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ServiciosViewModelStockTest {

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var postSaveSyncScheduler: PostSaveSyncScheduler
    private lateinit var cancelServicioExtraUseCase: CancelServicioExtraUseCase
    private lateinit var stockHelper: DispensacionStockHelper

    private val opticaIdFlow = MutableStateFlow("optica-test")
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testDate = LocalDate.of(2026, 9, 2)

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0

        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        sessionManager = mockk()
        postSaveSyncScheduler = mockk(relaxed = true)
        cancelServicioExtraUseCase = mockk(relaxed = true)
        stockHelper = mockk(relaxed = true)

        every { sessionManager.opticaId } returns opticaIdFlow
        coEvery { repository.reassignLegacyMiOpticaBaseTo(any()) } returns Unit
        every { repository.getAllServiciosForOptica(any()) } returns flowOf(emptyList())
        every { repository.pacientesFlowForOptica(any()) } returns flowOf(emptyList())
        every { repository.getAllPagosFlowForOptica(any()) } returns flowOf(emptyList())
        coEvery { repository.withTransaction(any<suspend () -> Any>()) } coAnswers {
            firstArg<suspend () -> Any>()()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = ServiciosViewModel(
        repository,
        sessionManager,
        postSaveSyncScheduler,
        cancelServicioExtraUseCase,
        stockHelper,
    )

    @Test
    fun saveServicio_with_monturaId_registers_SALIDA_VENTA() = runTest(testDispatcher) {
        coEvery {
            stockHelper.adjustStockAndRegistrarMovimiento(
                monturaId = "m-liquido",
                opticaId = "optica-test",
                delta = -1,
                tipo = "SALIDA_VENTA",
                referenciaId = any(),
                nota = "Salida por servicio extra",
            )
        } returns Result.success(1)

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.updateUiState {
            it.copy(
                descripcion = "Líquido",
                montoTotal = "20",
                monturaId = "m-liquido",
            )
        }

        viewModel.saveServicio {}
        advanceUntilIdle()

        coVerify(exactly = 1) {
            stockHelper.adjustStockAndRegistrarMovimiento(
                monturaId = "m-liquido",
                opticaId = "optica-test",
                delta = -1,
                tipo = "SALIDA_VENTA",
                referenciaId = any(),
                nota = "Salida por servicio extra",
            )
        }
        coVerify(exactly = 1) { repository.insertServicio(match { it.monturaId == "m-liquido" }) }
    }

    @Test
    fun saveServicio_without_monturaId_skips_stock() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.updateUiState {
            it.copy(descripcion = "Reparación", montoTotal = "50")
        }

        viewModel.saveServicio {}
        advanceUntilIdle()

        coVerify(exactly = 0) {
            stockHelper.adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun saveServicio_insufficient_stock_sets_error_and_skips_insert() = runTest(testDispatcher) {
        coEvery {
            stockHelper.adjustStockAndRegistrarMovimiento(
                monturaId = "m-cofre",
                opticaId = "optica-test",
                delta = -1,
                tipo = "SALIDA_VENTA",
                referenciaId = any(),
                nota = any(),
            )
        } returns Result.failure(IllegalStateException("Stock insuficiente: actual=0, delta=-1"))

        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.updateUiState {
            it.copy(descripcion = "Cofre", montoTotal = "30", monturaId = "m-cofre")
        }

        viewModel.saveServicio {}
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.insertServicio(any()) }
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun editServicio_changing_montura_restock_old_and_sale_new() = runTest(testDispatcher) {
        coEvery { repository.getServicioById(any(), any()) } returns Resource.Success(
            ServicioExtra(
                id = "serv-1",
                monturaId = "m-old",
                descripcion = "Viejo",
                montoTotal = 20.0,
                estado = "Pendiente",
                fecha = testDate,
                opticaId = "optica-test",
            ),
        )
        coEvery {
            stockHelper.adjustStockAndRegistrarMovimiento(
                monturaId = "m-old",
                opticaId = "optica-test",
                delta = 1,
                tipo = "AJUSTE",
                referenciaId = movimientoReferenciaForServicioExtraReverso("serv-1", "m-old"),
                nota = any(),
            )
        } returns Result.success(1)
        coEvery {
            stockHelper.adjustStockAndRegistrarMovimiento(
                monturaId = "m-new",
                opticaId = "optica-test",
                delta = -1,
                tipo = "SALIDA_VENTA",
                referenciaId = "serv-1",
                nota = any(),
            )
        } returns Result.success(1)

        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.updateUiState {
            it.copy(
                id = "serv-1",
                isEdit = true,
                descripcion = "Nuevo",
                montoTotal = "25",
                monturaId = "m-new",
            )
        }

        viewModel.saveServicio {}
        advanceUntilIdle()

        coVerify(exactly = 1) {
            stockHelper.adjustStockAndRegistrarMovimiento(
                monturaId = "m-old",
                opticaId = "optica-test",
                delta = 1,
                tipo = "AJUSTE",
                referenciaId = movimientoReferenciaForServicioExtraReverso("serv-1", "m-old"),
                nota = any(),
            )
        }
        coVerify(exactly = 1) {
            stockHelper.adjustStockAndRegistrarMovimiento(
                monturaId = "m-new",
                opticaId = "optica-test",
                delta = -1,
                tipo = "SALIDA_VENTA",
                referenciaId = "serv-1",
                nota = any(),
            )
        }
    }
}
