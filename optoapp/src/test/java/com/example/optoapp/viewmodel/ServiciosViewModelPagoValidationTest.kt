package com.example.optoapp.viewmodel

import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.CancelServicioExtraUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DispensacionStockHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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
class ServiciosViewModelPagoValidationTest {

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var postSaveSyncScheduler: PostSaveSyncScheduler
    private lateinit var cancelServicioExtraUseCase: CancelServicioExtraUseCase
    private lateinit var stockHelper: DispensacionStockHelper

    private val opticaIdFlow = MutableStateFlow("optica-test")
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testDate = LocalDate.of(2026, 8, 14)

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
        every { repository.getAllServiciosForOptica(any()) } returns flowOf(emptyList())
        every { repository.getAllPagosFlowForOptica(any()) } returns flowOf(emptyList())
        coEvery { repository.withTransaction(any<suspend () -> Any>()) } coAnswers {
            firstArg<suspend () -> Any>()()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun pago(id: String, tipo: String, monto: Double) = Pago(
        id = id,
        servicioExtraId = "serv-1",
        fecha = testDate,
        tipo = tipo,
        monto = monto,
        metodoPago = "Efectivo",
        opticaId = "optica-test",
    )

    private fun buildViewModel() = ServiciosViewModel(
        repository,
        sessionManager,
        postSaveSyncScheduler,
        cancelServicioExtraUseCase,
        stockHelper,
    )

    @Test
    fun `saveServicio accepts abono plus reembolso within total via PagoEffect`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.updateUiState {
            it.copy(
                descripcion = "Limpieza",
                montoTotal = "200",
                pagos = listOf(
                    pago("p1", "Abono", 200.0),
                    pago("p2", "Reembolso", 50.0),
                    pago("p3", "Abono", 40.0),
                ),
            )
        }

        viewModel.saveServicio {}
        advanceUntilIdle()

        assertNull(
            "Neto 190 <= 200 no debe disparar ABONO_MAYOR_QUE_TOTAL",
            viewModel.uiState.value.error,
        )
    }

    @Test
    fun `saveServicio still rejects abonos above total`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.updateUiState {
            it.copy(
                descripcion = "Limpieza",
                montoTotal = "200",
                pagos = listOf(pago("p1", "Abono", 150.0), pago("p2", "Abono", 120.0)),
            )
        }

        viewModel.saveServicio {}
        advanceUntilIdle()

        assertEquals(
            FinanzasRemoteDefaults.Messages.ABONO_MAYOR_QUE_TOTAL,
            viewModel.uiState.value.error,
        )
    }
}
