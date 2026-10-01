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
import kotlinx.coroutines.CancellationException
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
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ServiciosViewModelTerminalGuardTest {

    private lateinit var repository: OptoRepository
    private lateinit var stockHelper: DispensacionStockHelper
    private val testDispatcher = UnconfinedTestDispatcher()
    private val opticaId = "optica-test"
    private val servicioId = "serv-1"

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        stockHelper = mockk(relaxed = true)
        coEvery { stockHelper.adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any()) } returns Result.success(1)
        every { repository.getAllServiciosForOptica(any()) } returns flowOf(emptyList())
        every { repository.pacientesFlowForOptica(any()) } returns flowOf(emptyList())
        every { repository.getAllPagosFlowForOptica(any()) } returns flowOf(emptyList())
        coEvery { repository.getServicioExtraItems(servicioId, opticaId) } returns listOf(
            ServicioExtraItem(id = "item-1", servicioExtraId = servicioId, monturaId = "m-old", descripcion = "Montura", monto = 50.0, opticaId = opticaId),
        )
        coEvery { repository.getRegalosByServicioExtraId(any(), any()) } returns emptyList()
        coEvery { repository.withTransaction(any<suspend () -> Any>()) } coAnswers { firstArg<suspend () -> Any>()() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun givenPersistedEstado(estado: String) {
        coEvery { repository.getServicioById(servicioId, opticaId) } returns Resource.Success(
            ServicioExtra(
                id = servicioId, ot = "S-1", monturaId = "m-old", descripcion = "Montura", montoTotal = 50.0,
                estado = estado, fecha = LocalDate.of(2026, 9, 2), opticaId = opticaId,
            ),
        )
    }

    private fun editWithNewMontura(): ServiciosViewModel {
        val sessionManager = mockk<SessionManager>()
        every { sessionManager.opticaId } returns MutableStateFlow(opticaId)
        val viewModel = ServiciosViewModel(
            repository, sessionManager, mockk<PostSaveSyncScheduler>(relaxed = true),
            mockk<CancelServicioExtraUseCase>(relaxed = true), stockHelper,
        )
        viewModel.updateUiState {
            it.copy(
                id = servicioId, ot = "S-1", isEdit = true, estado = "Entregado",
                items = listOf(ServicioExtraItemUi(id = "item-1", monturaId = "m-new", descripcion = "Montura", montoDraft = "50")),
            )
        }
        return viewModel
    }

    @Test
    fun `edit save on Anulado servicio writes nothing and emits error`() = runTest(testDispatcher) {
        givenPersistedEstado("Anulado")
        val viewModel = editWithNewMontura()

        var saved = false
        viewModel.saveServicio { saved = true }
        advanceUntilIdle()

        assertEquals(false, saved)
        assertEquals("La orden está anulada y no se puede editar el servicio.", viewModel.uiState.value.error)
        coVerify(exactly = 0) { stockHelper.adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.updateServicio(any()) }
        coVerify(exactly = 0) { repository.deleteServicioExtraItemsByServicioId(any(), any()) }
        coVerify(exactly = 0) { repository.insertServicioExtraItem(any()) }
        coVerify(exactly = 0) { repository.insertPago(any()) }
    }

    @Test
    fun `cancelled save is not reported as an edit error`() = runTest(testDispatcher) {
        givenPersistedEstado("Entregado")
        coEvery { repository.withTransaction(any<suspend () -> Any>()) } throws CancellationException("screen closed")
        val viewModel = editWithNewMontura()

        var saved = false
        viewModel.saveServicio { saved = true }
        advanceUntilIdle()

        assertEquals(false, saved)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `edit save on Entregado servicio persists the change`() = runTest(testDispatcher) {
        givenPersistedEstado("Entregado")
        val viewModel = editWithNewMontura()

        var saved = false
        viewModel.saveServicio { saved = true }
        advanceUntilIdle()

        assertEquals(true, saved)
        assertNull(viewModel.uiState.value.error)
        coVerify(exactly = 1) { repository.updateServicio(match { it.id == servicioId && it.monturaId == "m-new" }) }
    }
}
