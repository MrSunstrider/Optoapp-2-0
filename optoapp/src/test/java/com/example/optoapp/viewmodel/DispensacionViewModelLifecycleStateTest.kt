package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.EliminarDispensacionUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DispensacionViewModelLifecycleStateTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispId = "disp-lifecycle"
    private val opticaId = "optica-test"
    private val rolFlow = MutableStateFlow("admin")
    private lateinit var repository: OptoRepository
    private lateinit var eliminar: EliminarDispensacionUseCase
    private lateinit var viewModel: DispensacionViewModel

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        eliminar = mockk(relaxed = true)
        val sessionManager = mockk<SessionManager>()
        every { sessionManager.opticaId } returns MutableStateFlow(opticaId)
        every { sessionManager.opticaRol } returns rolFlow
        every { repository.getPagosByDispensacion(dispId, opticaId) } returns flowOf(emptyList())
        viewModel = DispensacionViewModel(
            repository,
            sessionManager,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            eliminar,
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun load(estado: String, hasTrace: Boolean = true): OrderLifecycleState {
        coEvery { repository.getDispensacionById(dispId, opticaId) } returns Resource.Success(
            DispensacionOptica(id = dispId, pacienteId = "pac-1", fecha = LocalDate.of(2026, 9, 1), estadoEntrega = estado),
        )
        coEvery { eliminar.hasTrace(dispId, opticaId) } returns hasTrace
        viewModel.loadDispensacion(dispId)
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel.lifecycle.value
    }

    @Test
    fun `a new order exposes no lifecycle action`() {
        assertEquals(OrderLifecycleState(), viewModel.lifecycle.value)
    }

    @Test
    fun `Pendiente can be cancelled but not claimed`() {
        val state = load("Pendiente")

        assertEquals(
            OrderLifecycleState(canCancel = true, canClaim = false, canHardDelete = false, isReadOnly = false),
            state,
        )
    }

    @Test
    fun `Entregado can be cancelled and claimed`() {
        rolFlow.value = "gerente"

        val state = load(" Entregado ")

        assertEquals(
            OrderLifecycleState(canCancel = true, canClaim = true, canHardDelete = false, isReadOnly = false),
            state,
        )
    }

    @Test
    fun `Anulado is read-only without actions even when it left no trace`() {
        assertEquals(OrderLifecycleState(isReadOnly = true), load("Anulado", hasTrace = false))
    }

    @Test
    fun `Reclamada is read-only without actions`() {
        assertEquals(OrderLifecycleState(isReadOnly = true), load("Reclamada"))
    }

    @Test
    fun `non privileged role gets no action on an editable order`() {
        rolFlow.value = "asesor"

        assertEquals(OrderLifecycleState(), load("Entregado", hasTrace = false))
    }

    @Test
    fun `hard delete is offered only when the order has no pagos or stock movements`() {
        assertEquals(true, load("Pendiente", hasTrace = false).canHardDelete)
        assertEquals(false, load("Pendiente", hasTrace = true).canHardDelete)
    }
}
