package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.servicio.ServicioExtraItem
import com.example.optoapp.domain.CancelServicioExtraUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
        coEvery { repository.getServicioExtraItems(any(), any()) } returns emptyList()
        coEvery { repository.getRegalosByServicioExtraId(any(), any()) } returns emptyList()
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

        viewModel.updateItem(
            0,
            ServicioExtraItemUi(
                id = "item-1",
                monturaId = "m-liquido",
                descripcion = "Líquido",
                montoDraft = "20",
            ),
        )

        viewModel.saveServicio {}
        advanceUntilIdle()

        coVerify(exactly = 1) {
            stockHelper.adjustStockAndRegistrarMovimiento(
                monturaId = "m-liquido",
                opticaId = "optica-test",
                delta = -1,
                tipo = "SALIDA_VENTA",
                referenciaId = "item-1",
                nota = "Salida por servicio extra",
            )
        }
        coVerify(exactly = 1) { repository.insertServicio(match { it.monturaId == "m-liquido" }) }
        coVerify(exactly = 1) { repository.insertServicioExtraItem(match { it.id == "item-1" }) }
    }

    @Test
    fun saveServicio_without_monturaId_skips_stock() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.updateItem(
            0,
            ServicioExtraItemUi(descripcion = "Reparación", montoDraft = "50"),
        )

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
        viewModel.updateItem(
            0,
            ServicioExtraItemUi(
                monturaId = "m-cofre",
                descripcion = "Cofre",
                montoDraft = "30",
            ),
        )

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
        coEvery { repository.getServicioExtraItems("serv-1", "optica-test") } returns listOf(
            ServicioExtraItem(
                id = "item-old",
                servicioExtraId = "serv-1",
                monturaId = "m-old",
                descripcion = "Viejo",
                monto = 20.0,
                opticaId = "optica-test",
            ),
        )
        coEvery {
            stockHelper.adjustStockAndRegistrarMovimiento(
                monturaId = "m-old",
                opticaId = "optica-test",
                delta = 1,
                tipo = "AJUSTE",
                referenciaId = "item-old",
                nota = any(),
            )
        } returns Result.success(1)
        coEvery {
            stockHelper.adjustStockAndRegistrarMovimiento(
                monturaId = "m-new",
                opticaId = "optica-test",
                delta = -1,
                tipo = "SALIDA_VENTA",
                referenciaId = "item-new",
                nota = any(),
            )
        } returns Result.success(1)

        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.updateUiState {
            it.copy(
                id = "serv-1",
                isEdit = true,
                items = listOf(
                    ServicioExtraItemUi(
                        id = "item-new",
                        monturaId = "m-new",
                        descripcion = "Nuevo",
                        montoDraft = "25",
                    ),
                ),
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
                referenciaId = "item-old",
                nota = any(),
            )
        }
        coVerify(exactly = 1) {
            stockHelper.adjustStockAndRegistrarMovimiento(
                monturaId = "m-new",
                opticaId = "optica-test",
                delta = -1,
                tipo = "SALIDA_VENTA",
                referenciaId = "item-new",
                nota = any(),
            )
        }
    }

    @Test
    fun saveServicio_multi_item_sums_montoTotal() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.updateItem(
            0,
            ServicioExtraItemUi(descripcion = "A", montoDraft = "40"),
        )
        viewModel.addItem()
        viewModel.updateItem(
            1,
            ServicioExtraItemUi(descripcion = "B", montoDraft = "25.5"),
        )

        viewModel.saveServicio {}
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.insertServicio(match { servicio ->
                servicio.montoTotal == 65.5 &&
                    servicio.descripcion == "A + B"
            })
        }
        coVerify(exactly = 2) { repository.insertServicioExtraItem(any()) }
    }

    @Test
    fun editServicio_unchanged_item_skips_stock_rewrite() = runTest(testDispatcher) {
        coEvery { repository.getServicioById("serv-1", any()) } returns Resource.Success(
            ServicioExtra(
                id = "serv-1",
                monturaId = "m-same",
                descripcion = "Producto",
                montoTotal = 20.0,
                estado = "Pendiente",
                fecha = testDate,
                opticaId = "optica-test",
            ),
        )
        coEvery { repository.getServicioExtraItems("serv-1", "optica-test") } returns listOf(
            ServicioExtraItem(
                id = "item-same",
                servicioExtraId = "serv-1",
                monturaId = "m-same",
                descripcion = "Producto",
                monto = 20.0,
                opticaId = "optica-test",
            ),
        )

        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.updateUiState {
            it.copy(
                id = "serv-1",
                isEdit = true,
                items = listOf(
                    ServicioExtraItemUi(
                        id = "item-same",
                        monturaId = "m-same",
                        descripcion = "Producto",
                        montoDraft = "20",
                    ),
                ),
            )
        }

        viewModel.saveServicio {}
        advanceUntilIdle()

        coVerify(exactly = 0) {
            stockHelper.adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun deriveHeader_formats_monto_without_forced_zero() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.montoTotal)
        assertTrue(viewModel.uiState.value.items.size == 1)
        assertEquals("", viewModel.uiState.value.items.first().montoDraft)
    }
}
