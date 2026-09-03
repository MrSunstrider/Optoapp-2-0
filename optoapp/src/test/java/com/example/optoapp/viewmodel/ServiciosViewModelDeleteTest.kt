package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.CancelServicioExtraUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DispensacionStockHelper
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
    private lateinit var cancelServicioExtraUseCase: CancelServicioExtraUseCase
    private lateinit var stockHelper: DispensacionStockHelper
    private lateinit var viewModel: ServiciosViewModel

    private val opticaIdFlow = MutableStateFlow("optica-test")
    private val testDispatcher = StandardTestDispatcher()
    private val testDate = LocalDate.of(2026, 7, 10)
    private val servId = "serv-delete-1"

    private val testServicio = ServicioExtra(
        id = servId, ot = "SERV-001", descripcion = "Limpieza de lentes",
        montoTotal = 200.0, aCuenta = 100.0, estado = "Pendiente",
        fecha = testDate, pacienteId = "pac-1",
        metodoPago = "", opticaId = "optica-test",
    )

    private val testPagos = listOf(
        Pago(
            id = "pago-serv-1",
            fecha = testDate,
            tipo = "Efectivo",
            monto = 50.0,
            opticaId = "optica-test",
            servicioExtraId = servId,
        ),
        Pago(
            id = "pago-serv-2",
            fecha = testDate,
            tipo = "Transferencia",
            monto = 50.0,
            opticaId = "optica-test",
            servicioExtraId = servId,
        ),
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
        cancelServicioExtraUseCase = mockk(relaxed = true)
        stockHelper = mockk(relaxed = true)

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
        viewModel = ServiciosViewModel(repository, sessionManager, postSaveSyncScheduler, cancelServicioExtraUseCase, stockHelper)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showDeleteConfirmation(testServicio)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { cancelServicioExtraUseCase(servId, "optica-test") }
        coVerify(inverse = true) { repository.deleteServicio(any()) }
    }

    @Test
    fun `confirmDelete creates inverse pagos for each existing pago`() = runTest {
        viewModel = ServiciosViewModel(repository, sessionManager, postSaveSyncScheduler, cancelServicioExtraUseCase, stockHelper)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showDeleteConfirmation(testServicio)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { cancelServicioExtraUseCase(servId, "optica-test") }
        coVerify(exactly = 0) { repository.insertPago(match { it.tipo == "Anulación" }) }
    }
}
