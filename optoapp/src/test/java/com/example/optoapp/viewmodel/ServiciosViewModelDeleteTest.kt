package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.CancelServicioExtraUseCase
import com.example.optoapp.domain.LifecycleOutcome
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
import kotlinx.coroutines.launch
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
class ServiciosViewModelDeleteTest {

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var cancelServicioExtraUseCase: CancelServicioExtraUseCase
    private lateinit var viewModel: ServiciosViewModel

    private val opticaRolFlow = MutableStateFlow("gerente")
    private val testDispatcher = StandardTestDispatcher()
    private val servId = "serv-delete-1"

    private val testServicio = ServicioExtra(
        id = servId, ot = "SERV-001", descripcion = "Limpieza de lentes",
        montoTotal = 200.0, aCuenta = 100.0, estado = "Pendiente",
        fecha = LocalDate.of(2026, 7, 10), pacienteId = "pac-1",
        metodoPago = "", opticaId = "optica-test",
    )

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0

        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        sessionManager = mockk()
        cancelServicioExtraUseCase = mockk()
        coEvery { cancelServicioExtraUseCase(any(), any(), any()) } returns LifecycleOutcome.Applied

        every { sessionManager.opticaId } returns MutableStateFlow("optica-test")
        every { sessionManager.opticaRol } returns opticaRolFlow
        every { sessionManager.userTimeZone } returns flowOf(null)
        every { repository.getAllServiciosForOptica(any()) } returns flowOf(
            listOf(
                testServicio,
                testServicio.copy(id = "serv-entregado", estado = "Entregado"),
                testServicio.copy(id = "serv-anulado", estado = "Anulado"),
                testServicio.copy(id = "serv-reclamado", estado = "Reclamada"),
            ),
        )
        every { repository.getAllPagosFlowForOptica(any()) } returns flowOf(emptyList())

        viewModel = ServiciosViewModel(
            repository, sessionManager, mockk<PostSaveSyncScheduler>(relaxed = true),
            cancelServicioExtraUseCase, mockk<DispensacionStockHelper>(relaxed = true),
        )
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.showDeleteConfirmation(testServicio)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun confirm(motivo: String) {
        viewModel.confirmAnular(motivo)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `confirmAnular cancels with the reason and closes the dialog`() = runTest(testDispatcher) {
        confirm("Error de registro")

        coVerify(exactly = 1) { cancelServicioExtraUseCase(servId, "optica-test", "Error de registro") }
        assertFalse(viewModel.showDeleteDialog.value)
        assertNull(viewModel.servicioToDelete.value)
        assertNull(viewModel.deleteError.value)
    }

    @Test
    fun `confirmAnular rejects asesor without invoking the use case`() = runTest(testDispatcher) {
        opticaRolFlow.value = "asesor"

        confirm("Error de registro")

        coVerify(exactly = 0) { cancelServicioExtraUseCase(any(), any(), any()) }
        assertTrue(viewModel.deleteError.value!!.contains("anular servicio"))
        assertTrue(viewModel.showDeleteDialog.value)
    }

    @Test
    fun `confirmAnular rejects a blank reason without invoking the use case`() = runTest(testDispatcher) {
        confirm("   ")

        coVerify(exactly = 0) { cancelServicioExtraUseCase(any(), any(), any()) }
        assertEquals("El motivo es obligatorio.", viewModel.deleteError.value)
        assertTrue(viewModel.showDeleteDialog.value)
    }

    @Test
    fun `confirmAnular on an already cancelled servicio informs the user and stays on screen`() = runTest(testDispatcher) {
        coEvery { cancelServicioExtraUseCase(any(), any(), any()) } returns LifecycleOutcome.AlreadyTerminal("Anulado")
        var completions = 0

        viewModel.confirmAnular("Error de registro") { completions++ }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Este servicio ya fue anulado", viewModel.infoMessage.value)
        assertEquals(0, completions)
        assertNull(viewModel.deleteError.value)
        assertFalse(viewModel.showDeleteDialog.value)
        assertNull(viewModel.servicioToDelete.value)
        assertFalse(viewModel.anulando.value)
    }

    @Test
    fun `confirmAnular on an already cancelled servicio reloads the open form so it turns read-only`() = runTest(testDispatcher) {
        every { repository.getPagosByServicioExtra(servId, "optica-test") } returns flowOf(emptyList())
        coEvery { repository.getServicioById(servId, "optica-test") } returns Resource.Success(testServicio)
        viewModel.loadServicio(servId)
        testDispatcher.scheduler.advanceUntilIdle()
        coEvery { repository.getServicioById(servId, "optica-test") } returns
            Resource.Success(testServicio.copy(estado = "Anulado"))
        coEvery { cancelServicioExtraUseCase(any(), any(), any()) } returns LifecycleOutcome.AlreadyTerminal("Anulado")

        confirm("Error de registro")

        assertEquals("Anulado", viewModel.uiState.value.estado)
        assertTrue(viewModel.uiState.value.isReadOnly)
    }

    @Test
    fun `clearInfoMessage resets the info message`() = runTest(testDispatcher) {
        coEvery { cancelServicioExtraUseCase(any(), any(), any()) } returns LifecycleOutcome.AlreadyTerminal("Anulado")
        confirm("Error de registro")

        viewModel.clearInfoMessage()

        assertNull(viewModel.infoMessage.value)
    }

    @Test
    fun `applied cancel completes without an info message`() = runTest(testDispatcher) {
        var completions = 0

        viewModel.confirmAnular("Error de registro") { completions++ }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, completions)
        assertNull(viewModel.infoMessage.value)
    }

    @Test
    fun `confirmAnular surfaces a restock failure and keeps the dialog open`() = runTest(testDispatcher) {
        coEvery { cancelServicioExtraUseCase(any(), any(), any()) } throws IllegalStateException("Stock insuficiente")

        confirm("Error de registro")

        assertEquals("Stock insuficiente", viewModel.deleteError.value)
        assertTrue(viewModel.showDeleteDialog.value)
    }

    @Test
    fun `cancelableServicioIds lists only active servicios for a gerente`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.cancelableServicioIds.collect {} }
        testDispatcher.scheduler.runCurrent()

        assertEquals(setOf(servId, "serv-entregado"), viewModel.cancelableServicioIds.value)
    }

    @Test
    fun `cancelableServicioIds empties when the role cannot cancel`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.cancelableServicioIds.collect {} }
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, viewModel.cancelableServicioIds.value.size)

        opticaRolFlow.value = "asesor"
        testDispatcher.scheduler.runCurrent()

        assertEquals(emptySet<String>(), viewModel.cancelableServicioIds.value)
    }

    @Test
    fun `double tap on the cancel confirmation cancels once`() = runTest(testDispatcher) {
        var completions = 0

        viewModel.confirmAnular("Error de registro") { completions++ }
        viewModel.confirmAnular("Error de registro") { completions++ }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { cancelServicioExtraUseCase(servId, "optica-test", "Error de registro") }
        assertEquals(1, completions)
        assertFalse(viewModel.anulando.value)
    }

    @Test
    fun `a failed cancel releases the guard and skips completion`() = runTest(testDispatcher) {
        coEvery { cancelServicioExtraUseCase(any(), any(), any()) } throws IllegalStateException("Stock insuficiente")
        var completions = 0

        viewModel.confirmAnular("Error de registro") { completions++ }
        assertTrue(viewModel.anulando.value)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, completions)
        assertFalse(viewModel.anulando.value)
    }

    @Test
    fun `dismissDeleteDialog clears the pending servicio and error`() = runTest(testDispatcher) {
        confirm("   ")

        viewModel.dismissDeleteDialog()

        assertFalse(viewModel.showDeleteDialog.value)
        assertNull(viewModel.servicioToDelete.value)
        assertNull(viewModel.deleteError.value)
    }
}
