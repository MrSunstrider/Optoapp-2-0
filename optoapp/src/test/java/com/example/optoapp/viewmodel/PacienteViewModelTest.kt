package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.sync.PostSaveSyncScheduler
import io.github.jan.supabase.SupabaseClient
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PacienteViewModelTest {

    private lateinit var viewModel: PacienteViewModel
    private val repository = mockk<OptoRepository>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private val postSaveSyncScheduler = mockk<PostSaveSyncScheduler>(relaxed = true)
    private val opticaIdFlow = MutableStateFlow("test-optica")
    private val opticaRolFlow = MutableStateFlow("admin")

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { sessionManager.opticaId } returns opticaIdFlow
        every { sessionManager.opticaRol } returns opticaRolFlow
        coEvery { postSaveSyncScheduler.schedulePacientesSync(any()) } just Runs
        viewModel = PacienteViewModel(
            repository = repository,
            sessionManager = sessionManager,
            postSaveSyncScheduler = postSaveSyncScheduler,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isLoading becomes false when pacientes flow emits non-empty list`() = runTest(testDispatcher) {
        val pacientesFlow = MutableStateFlow(
            listOf(
                Paciente(
                    id = "b2-1", nombreCompleto = "Flow Test", edad = 30,
                    telefono = "111", fechaCreacion = LocalDate.parse("2026-01-01"),
                    opticaId = "test-optica",
                ),
            ),
        )
        every { repository.pacientesFlowForOptica(any()) } returns pacientesFlow

        viewModel = PacienteViewModel(
            repository = repository,
            sessionManager = sessionManager,
            postSaveSyncScheduler = postSaveSyncScheduler,
        )

        advanceUntilIdle()

        assertFalse("isLoading should become false without artificial delay", viewModel.isLoading.value)
    }

    @Test
    fun `savePaciente with duplicate HO throws IllegalArgumentException`() = runBlocking {
        coEvery { repository.existsDuplicateHistoriaOptometrica(any(), any(), any()) } returns true
        val paciente = Paciente(
            id = "c2-dup-ho", nombreCompleto = "Dup HO", edad = 30,
            telefono = "111", fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "test-optica", historiaOptometrica = "HO-2026-0001",
        )

        try {
            viewModel.savePaciente(paciente)
            fail("Expected IllegalArgumentException for duplicate HO")
        } catch (e: IllegalArgumentException) {
            assertEquals(
                "Ya existe una historia optometrica con ese numero en esta optica.",
                e.message,
            )
        }
    }

    @Test
    fun `savePaciente without HO does not check duplicate`() = runBlocking {
        val paciente = Paciente(
            id = "c2-no-ho", nombreCompleto = "No HO", edad = 30,
            telefono = "111", fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "test-optica", historiaOptometrica = null,
        )

        viewModel.savePaciente(paciente)

        coVerify { repository.insertPaciente(any()) }
    }

    @Test
    fun `savePaciente with admin role succeeds`() = runBlocking {
        val paciente = Paciente(
            id = "save-1", nombreCompleto = "Admin Save", edad = 30,
            telefono = "111", fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "test-optica",
        )
        coEvery { repository.existsDuplicateHistoriaOptometrica(any(), any(), any()) } returns false

        viewModel.savePaciente(paciente)

        coVerify { repository.insertPaciente(any()) }
    }

    @Test
    fun `savePaciente with vendedor role throws unauthorized`() = runBlocking {
        opticaRolFlow.value = "vendedor"
        val paciente = Paciente(
            id = "save-2", nombreCompleto = "Vendedor Save", edad = 25,
            telefono = "222", fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "test-optica",
        )

        try {
            viewModel.savePaciente(paciente)
            fail("Expected IllegalArgumentException for role vendedor")
        } catch (_: IllegalArgumentException) {
            // Expected — authorization denied
        }
    }

    @Test
    fun deletePacienteResultSuccess_holdsRemainingDeletes() {
        val result = DeletePacienteResult.Success(remainingDeletesToday = 7)
        assertEquals(7, (result as DeletePacienteResult.Success).remainingDeletesToday)
    }

    @Test
    fun deletePacienteResultError_holdsMessage() {
        val result = DeletePacienteResult.Error("No tienes permisos")
        assertEquals("No tienes permisos", (result as DeletePacienteResult.Error).message)
    }

    @Test
    fun deletePacienteResultSuccess_isNotError() {
        val result: DeletePacienteResult = DeletePacienteResult.Success(remainingDeletesToday = 5)
        assertFalse(result is DeletePacienteResult.Error)
    }

    @Test
    fun deletePacienteResultError_isNotSuccess() {
        val result: DeletePacienteResult = DeletePacienteResult.Error("Error")
        assertFalse(result is DeletePacienteResult.Success)
    }

    @Test
    fun deletePacienteResultSuccess_remainingDeletesZero() {
        val result = DeletePacienteResult.Success(remainingDeletesToday = 0)
        assertEquals(0, (result as DeletePacienteResult.Success).remainingDeletesToday)
    }

    @Test
    fun deletePacienteResultSuccess_dataClass() {
        val r1 = DeletePacienteResult.Success(3)
        val r2 = DeletePacienteResult.Success(3)
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun deletePacienteResultError_dataClass() {
        val r1 = DeletePacienteResult.Error("msg")
        val r2 = DeletePacienteResult.Error("msg")
        assertEquals(r1, r2)
    }

    @Test
    fun deletePacienteResultError_differentMessages_notEqual() {
        val r1 = DeletePacienteResult.Error("msg1")
        val r2 = DeletePacienteResult.Error("msg2")
        assertNotEquals(r1, r2)
    }

    // T2: RED — getPaciente return type changed to Resource<Paciente>

    @Test
    fun `getPaciente returns Resource Error on DB failure`() = runBlocking {
        val pacienteId = "p-fail"
        coEvery { repository.getPacienteByIdScoped(pacienteId, "test-optica") } returns
            Resource.Error("DB connection failed")

        val result = viewModel.getPaciente(pacienteId)

        assertTrue("Expected Resource.Error but got $result", result is Resource.Error)
        val err = result as Resource.Error
        assertEquals("DB connection failed", err.message)
    }

    @Test
    fun `getPaciente returns Resource Success with patient data`() = runBlocking {
        val pacienteId = "p-success"
        val expected = Paciente(
            id = pacienteId, nombreCompleto = "Test Success", edad = 30,
            telefono = "111", fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "test-optica",
        )
        coEvery { repository.getPacienteByIdScoped(pacienteId, "test-optica") } returns
            Resource.Success(expected)

        val result = viewModel.getPaciente(pacienteId)

        assertTrue("Expected Resource.Success but got $result", result is Resource.Success)
        val success = result as Resource.Success
        assertEquals(expected, success.data)
    }

    @Test
    fun `savePaciente does not schedule PacientesSync`() = runBlocking {
        val paciente = Paciente(
            id = "save-no-sync", nombreCompleto = "No Sync", edad = 30,
            telefono = "111", fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "test-optica",
        )
        coEvery { repository.existsDuplicateHistoriaOptometrica(any(), any(), any()) } returns false

        viewModel.savePaciente(paciente)

        coVerify(exactly = 0) { postSaveSyncScheduler.schedulePacientesSync(any()) }
    }
}
