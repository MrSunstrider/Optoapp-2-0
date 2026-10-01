package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.ReclamarDispensacionUseCase
import com.example.optoapp.domain.ReclamoOutcome
import com.example.optoapp.domain.ReclamoStockInsuficienteException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DispensacionViewModelReclamoTest {
    private val testDispatcher = StandardTestDispatcher()
    private val originalId = "disp-original"
    private val opticaId = "optica-test"
    private val rolFlow = MutableStateFlow("admin")
    private lateinit var repository: OptoRepository
    private lateinit var reclamar: ReclamarDispensacionUseCase
    private lateinit var viewModel: DispensacionViewModel

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        reclamar = mockk()
        val sessionManager = mockk<SessionManager>()
        every { sessionManager.opticaId } returns MutableStateFlow(opticaId)
        every { sessionManager.opticaRol } returns rolFlow
        viewModel = DispensacionViewModel(
            repository,
            sessionManager,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            reclamar,
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

    private fun reclamarAndAwait(total: Double = 250.0, metodo: String = "Tarjeta"): String? {
        var navigatedTo: String? = null
        viewModel.crearReclamo(originalId, "Lente rayado", total, metodo) { navigatedTo = it }
        testDispatcher.scheduler.advanceUntilIdle()
        return navigatedTo
    }

    @Test
    fun `asesor is rejected without invoking the use case`() = runTest {
        rolFlow.value = "asesor"

        val navigatedTo = reclamarAndAwait()

        assertNull(navigatedTo)
        assertTrue(viewModel.uiState.value.error!!.contains("reclamar dispensación"))
        assertFalse(viewModel.uiState.value.isLoading)
        confirmVerified(reclamar)
    }

    @Test
    fun `created claim delegates motivo total and refund method and navigates to the replacement`() = runTest {
        rolFlow.value = "gerente"
        coEvery { reclamar(originalId, opticaId, "Lente rayado", 150.0, "Yape") } returns
            ReclamoOutcome.Created("repl-1", "OT-1-R1")

        val navigatedTo = reclamarAndAwait(total = 150.0, metodo = "Yape")

        assertEquals("repl-1", navigatedTo)
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 1) { reclamar(originalId, opticaId, "Lente rayado", 150.0, "Yape") }
    }

    @Test
    fun `claim on an already Reclamada order informs the user and does not navigate`() = runTest {
        coEvery { reclamar(originalId, opticaId, any(), any(), any()) } returns ReclamoOutcome.AlreadyTerminal("Reclamada")

        val navigatedTo = reclamarAndAwait()

        assertNull(navigatedTo)
        assertEquals("Esta orden ya fue reclamada", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `out of stock copied frame surfaces the typed error message`() = runTest {
        val error = ReclamoStockInsuficienteException("mon-1", "Ray-Ban RB2140")
        coEvery { reclamar(originalId, opticaId, any(), any(), any()) } throws error

        val navigatedTo = reclamarAndAwait()

        assertNull(navigatedTo)
        assertEquals(error.message, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `isLoading is true while the claim runs and ignores a second request`() = runTest {
        val gate = CompletableDeferred<ReclamoOutcome>()
        coEvery { reclamar(originalId, opticaId, any(), any(), any()) } coAnswers { gate.await() }

        viewModel.crearReclamo(originalId, "Lente rayado", 250.0, "Tarjeta") {}
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.crearReclamo(originalId, "Lente rayado", 250.0, "Tarjeta") {}
        testDispatcher.scheduler.advanceUntilIdle()

        gate.complete(ReclamoOutcome.Created("repl-1", "OT-1-R1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 1) { reclamar(originalId, opticaId, any(), any(), any()) }
    }

    @Test
    fun `suggested refund method is the last credit payment method of the original`() = runTest {
        every { repository.getPagosByDispensacion(originalId, opticaId) } returns flowOf(
            listOf(
                pago("a1", "Abono", "Efectivo", LocalDate.of(2026, 7, 1)),
                pago("a2", "Abono", "Tarjeta", LocalDate.of(2026, 7, 5)),
                pago("r1", "Reembolso", "Yape", LocalDate.of(2026, 7, 9)),
            ),
        )

        assertEquals("Tarjeta", viewModel.metodoReembolsoSugerido(originalId))
    }

    @Test
    fun `suggested refund method falls back to Efectivo without credit payments`() = runTest {
        every { repository.getPagosByDispensacion(originalId, opticaId) } returns flowOf(emptyList())

        assertEquals("Efectivo", viewModel.metodoReembolsoSugerido(originalId))
    }

    private fun pago(id: String, tipo: String, metodo: String, fecha: LocalDate) = Pago(
        id = id, dispensacionId = originalId, fecha = fecha, tipo = tipo, monto = 50.0, metodoPago = metodo, opticaId = opticaId,
    )
}
