package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.domain.AnularDispensacionUseCase
import com.example.optoapp.domain.CalcularMontoPagadoUseCase
import com.example.optoapp.domain.ReclamarDispensacionUseCase
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DispensacionViewModelTerminalGuardTest {

    private lateinit var repository: OptoRepository
    private lateinit var stockHelper: DispensacionStockHelper
    private val testDispatcher = UnconfinedTestDispatcher()
    private val opticaId = "optica-test"
    private val dispId = "disp-1"
    private val fechaAnulacion = LocalDate.of(2026, 9, 28)

    private val persisted = DispensacionOptica(
        id = dispId, ot = "2026-0042", pacienteId = "pac-1", fecha = LocalDate.of(2026, 9, 1), opticaId = opticaId,
        montoTotal = 200.0, estadoEntrega = "Reclamada", monturaId = "M-old", origenMontura = "Tienda",
        motivoAnulacion = "Lente rayado", fechaAnulacion = fechaAnulacion,
    )

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        stockHelper = mockk(relaxed = true)
        every { repository.runInTransaction(any()) } answers { firstArg<() -> Unit>().invoke() }
        coEvery { repository.getDispensacionItemsByDispensacion(dispId, opticaId) } returns listOf(
            DispensacionItem(id = "i1", dispensacionId = dispId, tipoLente = "Monofocal", monturaId = "M-old", origenMontura = "Tienda", opticaId = opticaId),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): DispensacionViewModel {
        val sessionManager = mockk<SessionManager>()
        every { sessionManager.opticaId } returns MutableStateFlow(opticaId)
        every { sessionManager.opticaRol } returns MutableStateFlow("admin")
        val calcularMontoPagado = mockk<CalcularMontoPagadoUseCase>()
        coEvery { calcularMontoPagado(any(), any()) } returns 0.0
        return DispensacionViewModel(
            repository, sessionManager, mockk<PostSaveSyncScheduler>(relaxed = true), stockHelper, calcularMontoPagado,
            mockk<AnularDispensacionUseCase>(relaxed = true), mockk<ReclamarDispensacionUseCase>(relaxed = true),
            mockk<CostoProductoDao>(relaxed = true), mockk<CostoBiseladoDao>(relaxed = true), mockk(relaxed = true),
        )
    }

    private suspend fun saveEdit(viewModel: DispensacionViewModel, monturaId: String, estado: String): Boolean {
        viewModel.updateUiState {
            it.copy(
                ot = "2026-0042", montoTotal = "200", estadoEntrega = estado,
                items = listOf(DispensacionItemUi(id = "i1", tipoLente = "Monofocal", monturaId = monturaId, origenMontura = "Tienda")),
            )
        }
        var completed = false
        viewModel.saveDispensacion("pac-1", dispId) { completed = true }
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { while (viewModel.uiState.value.isLoading) delay(10) }
        }
        return completed
    }

    @Test
    fun `editing items of a Reclamada order changes no item stock movimiento or metadata`() = runTest(testDispatcher) {
        coEvery { repository.getDispensacionById(dispId, opticaId) } returns Resource.Success(persisted)
        val viewModel = createViewModel()

        val completed = saveEdit(viewModel, monturaId = "M-new", estado = "Reclamada")

        assertEquals(false, completed)
        assertEquals("La orden está reclamada y no se puede editar la dispensación.", viewModel.uiState.value.error)
        coVerify(exactly = 0) { stockHelper.adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.updateDispensacion(any()) }
        coVerify(exactly = 0) { repository.deleteItemsByDispensacionId(any(), any()) }
        coVerify(exactly = 0) { repository.insertDispensacionItem(any()) }
        coVerify(exactly = 0) { repository.insertPago(any()) }
    }

    @Test
    fun `editing an active order keeps claim linkage and cancellation metadata of the persisted row`() = runTest(testDispatcher) {
        val active = persisted.copy(estadoEntrega = "Entregado", reclamoOrigenId = "orig-1")
        coEvery { repository.getDispensacionById(dispId, opticaId) } returns Resource.Success(active)
        val saved = mutableListOf<DispensacionOptica>()
        coEvery { repository.updateDispensacion(capture(saved)) } returns Unit
        val viewModel = createViewModel()

        val completed = saveEdit(viewModel, monturaId = "M-old", estado = "Entregado")

        assertEquals(true, completed)
        assertNull(viewModel.uiState.value.error)
        assertEquals(2, saved.size)
        saved.forEach { row ->
            assertEquals("orig-1", row.reclamoOrigenId)
            assertEquals("Lente rayado", row.motivoAnulacion)
            assertEquals(fechaAnulacion, row.fechaAnulacion)
        }
    }
}
