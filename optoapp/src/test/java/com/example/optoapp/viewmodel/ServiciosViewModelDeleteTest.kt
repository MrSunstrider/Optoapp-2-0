package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.Resource
import com.example.optoapp.sync.PostSaveSyncScheduler
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ServiciosViewModelDeleteTest {

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var postSaveSyncScheduler: PostSaveSyncScheduler
    private lateinit var viewModel: ServiciosViewModel

    private val opticaIdFlow = MutableStateFlow("optica-test")
    private val testDispatcher = StandardTestDispatcher()
    private val testDate = LocalDate.of(2026, 7, 10)
    private val servId = "serv-delete-1"

    private val testServicio = ServicioExtra(
        id = servId, ot = "SERV-001", descripcion = "Limpieza de lentes",
        montoTotal = 200.0, aCuenta = 100.0, estado = "Pendiente",
        fecha = testDate, pacienteId = "pac-1",
        metodoPago = "", opticaId = "optica-test"
    )

    private val testPagos = listOf(
        Pago(id = "pago-serv-1", fecha = testDate, tipo = "Efectivo", monto = 50.0,
            opticaId = "optica-test", servicioExtraId = servId),
        Pago(id = "pago-serv-2", fecha = testDate, tipo = "Transferencia", monto = 50.0,
            opticaId = "optica-test", servicioExtraId = servId)
    )

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0

        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        sessionManager = mockk()
        postSaveSyncScheduler = mockk(relaxed = true)

        every { sessionManager.opticaId } returns opticaIdFlow
        every { sessionManager.userTimeZone } returns flowOf(null)
        every { repository.getAllServiciosForOptica(any()) } returns flowOf(listOf(testServicio))
        every { repository.getAllPagosFlowForOptica(any()) } returns flowOf(testPagos)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `confirmDelete anula en vez de hard-delete`() = runTest {
        viewModel = ServiciosViewModel(repository, sessionManager, postSaveSyncScheduler)
        testDispatcher.scheduler.advanceUntilIdle()

        // Set up the servicio to delete
        viewModel.showDeleteConfirmation(testServicio)
        testDispatcher.scheduler.advanceUntilIdle()

        // Mock pagos for the servicio
        coEvery { repository.getPagosByServicioExtra(servId) } returns flowOf(testPagos)

        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify anulación: updateServicio called with estado="Anulado"
        coVerify { repository.updateServicio(match { it.id == servId && it.estado == "Anulado" }) }
        // should NOT call deleteServicio
        coVerify(inverse = true) { repository.deleteServicio(any()) }
    }

    @Test
    fun `confirmDelete creates inverse pagos for each existing pago`() = runTest {
        viewModel = ServiciosViewModel(repository, sessionManager, postSaveSyncScheduler)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showDeleteConfirmation(testServicio)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.getPagosByServicioExtra(servId) } returns flowOf(testPagos)

        val pagoSlot = slot<Pago>()
        // Count how many times insertPago is called
        var insertCount = 0
        coEvery { repository.insertPago(capture(pagoSlot)) } answers { insertCount++; Unit }

        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Should have created 2 inverse Pagos (one per existing pago)
        assertEquals(2, insertCount)
        assertEquals("Anulación", pagoSlot.captured.tipo)
        assertEquals(-50.0, pagoSlot.captured.monto, 0.001)
    }
}
