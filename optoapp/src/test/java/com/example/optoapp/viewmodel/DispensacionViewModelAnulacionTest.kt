package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.AnularDispensacionUseCase
import com.example.optoapp.domain.LifecycleOutcome
import com.example.optoapp.util.DispensacionStockHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
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
class DispensacionViewModelAnulacionTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispId = "disp-1"
    private val opticaId = "optica-test"
    private val rolFlow = MutableStateFlow("gerente")
    private lateinit var repository: OptoRepository
    private lateinit var stockHelper: DispensacionStockHelper
    private lateinit var anular: AnularDispensacionUseCase
    private lateinit var viewModel: DispensacionViewModel

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        stockHelper = mockk(relaxed = true)
        anular = mockk()
        val sessionManager = mockk<SessionManager>()
        every { sessionManager.opticaId } returns MutableStateFlow(opticaId)
        every { sessionManager.opticaRol } returns rolFlow
        viewModel = DispensacionViewModel(
            repository,
            sessionManager,
            mockk(relaxed = true),
            stockHelper,
            mockk(relaxed = true),
            anular,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun anularAndAwait(): Boolean {
        var completed = false
        viewModel.anularDispensacion(dispId, "Cliente desistió") { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()
        return completed
    }

    @Test
    fun `asesor is rejected without invoking the use case`() = runTest {
        rolFlow.value = "asesor"

        val completed = anularAndAwait()

        assertFalse(completed)
        assertTrue(viewModel.uiState.value.error!!.contains("anular dispensación"))
        assertFalse(viewModel.uiState.value.isLoading)
        confirmVerified(anular)
    }

    @Test
    fun `applied cancel completes and the view model no longer restocks regalos itself`() = runTest {
        coEvery { anular(dispId, opticaId, "Cliente desistió") } returns LifecycleOutcome.Applied

        val completed = anularAndAwait()

        assertTrue(completed)
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 0) { stockHelper.adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.getRegalosByDispensacionId(any(), any()) }
    }

    @Test
    fun `cancel on a Reclamada order shows an error and does not complete`() = runTest {
        coEvery { anular(dispId, opticaId, any()) } returns LifecycleOutcome.AlreadyTerminal("Reclamada")

        val completed = anularAndAwait()

        assertFalse(completed)
        assertEquals("No se puede anular una orden reclamada", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `cancel on an already Anulado order informs the user and completes to refresh the order`() = runTest {
        coEvery { anular(dispId, opticaId, any()) } returns LifecycleOutcome.AlreadyTerminal("Anulado")

        val completed = anularAndAwait()

        assertTrue(completed)
        assertEquals("Esta orden ya fue anulada", viewModel.uiState.value.infoMessage)
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `applied cancel shows no info message`() = runTest {
        coEvery { anular(dispId, opticaId, any()) } returns LifecycleOutcome.Applied

        anularAndAwait()

        assertNull(viewModel.uiState.value.infoMessage)
    }

    @Test
    fun `use case failure surfaces its message`() = runTest {
        coEvery { anular(dispId, opticaId, any()) } throws IllegalStateException("Stock insuficiente")

        val completed = anularAndAwait()

        assertFalse(completed)
        assertEquals("Stock insuficiente", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
