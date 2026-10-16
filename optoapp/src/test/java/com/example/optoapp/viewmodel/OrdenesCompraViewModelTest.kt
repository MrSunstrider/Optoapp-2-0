package com.example.optoapp.viewmodel

import android.util.Log
import com.example.optoapp.data.OrdenCompraRepository
import com.example.optoapp.data.SessionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class OrdenesCompraViewModelTest {
    private val repository: OrdenCompraRepository = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk()
    private lateinit var viewModel: OrdenesCompraViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { sessionManager.opticaId } returns flowOf("o1")
        viewModel = OrdenesCompraViewModel(repository, sessionManager)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    @Test
    fun receiveItems_success_reportsReceiptForTheCurrentOptica() = runTest {
        viewModel.viewDetail("oc1")
        viewModel.receiveItems("oc1", mapOf("i1" to 10))

        val state = viewModel.uiState.first { it.success != null || it.error != null }
        assertEquals("Recepción registrada", state.success)
        assertNull(state.error)
        assertNull(state.viewingId)
        assertFalse(state.receiving)
        coVerify(exactly = 1) { repository.receiveItems("oc1", "o1", mapOf("i1" to 10)) }
    }

    @Test
    fun receiveItems_transactionFailure_surfacesErrorInsteadOfSuccess() = runTest {
        coEvery { repository.receiveItems(any(), any(), any()) } throws IllegalStateException("No se pudo ajustar el stock")

        viewModel.viewDetail("oc1")
        viewModel.receiveItems("oc1", mapOf("i1" to 10))

        val state = viewModel.uiState.first { it.success != null || it.error != null }
        assertEquals("No se pudo registrar la recepción. Inténtalo de nuevo.", state.error)
        assertNull(state.success)
        assertEquals("oc1", state.viewingId)
        assertFalse(state.receiving)
    }

    @Test
    fun receiveItems_secondCallWhileInFlight_doesNotCallRepositoryTwice() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.receiveItems(any(), any(), any()) } coAnswers {
            gate.await()
        }

        viewModel.viewDetail("oc1")
        viewModel.receiveItems("oc1", mapOf("i1" to 10))
        assertTrue(viewModel.uiState.value.receiving)

        viewModel.receiveItems("oc1", mapOf("i1" to 10))
        gate.complete(Unit)

        val state = viewModel.uiState.first { it.success != null || it.error != null }
        assertEquals("Recepción registrada", state.success)
        assertFalse(state.receiving)
        coVerify(exactly = 1) { repository.receiveItems("oc1", "o1", mapOf("i1" to 10)) }
    }
}
