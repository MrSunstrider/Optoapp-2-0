package com.example.optoapp.viewmodel

import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.domain.CalcularMontoPagadoUseCase
import com.example.optoapp.domain.CancelDispensacionUseCase
import com.example.optoapp.domain.PagoEffect
import com.example.optoapp.domain.ReclaimDispensacionUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DispensacionStockHelper
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * La validación de abonos debe compartir semántica con la UI (`PagosSectionState`)
 * y con los agregados ledger: reembolsos y reversos restan.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DispensacionViewModelPagoValidationTest {

    private val testDate = LocalDate.of(2026, 8, 14)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun pago(id: String, tipo: String, monto: Double) = Pago(
        id = id,
        dispensacionId = "disp-1",
        fecha = testDate,
        tipo = tipo,
        monto = monto,
        metodoPago = "Efectivo",
        opticaId = "optica-test",
    )

    private fun buildViewModel(): DispensacionViewModel {
        val repository = mockk<OptoRepository>(relaxed = true)
        val sessionManager = mockk<SessionManager>()
        every { sessionManager.opticaId } returns MutableStateFlow("optica-test")
        every { repository.getAllPagosFlowForOptica(any()) } returns flowOf(emptyList())

        return DispensacionViewModel(
            repository,
            sessionManager,
            mockk<PostSaveSyncScheduler>(relaxed = true),
            mockk<DispensacionStockHelper>(relaxed = true),
            mockk<CalcularMontoPagadoUseCase>(relaxed = true),
            mockk<CancelDispensacionUseCase>(relaxed = true),
            mockk<ReclaimDispensacionUseCase>(relaxed = true),
            mockk<CostoProductoDao>(relaxed = true),
            mockk<CostoBiseladoDao>(relaxed = true),
        )
    }

    @Test
    fun `abono neto con reembolso queda bajo el total`() {
        val pagos = listOf(
            pago("p1", "Abono", 200.0),
            pago("p2", "Reembolso", 50.0),
            pago("p3", "Abono", 40.0),
        )

        val neto = pagos.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }

        assertEquals(190.0, neto, 0.001)
    }

    @Test
    fun `saveDispensacion rechaza abonos netos sobre el total`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.addPago(pago("p1", "Abono", 150.0))
        viewModel.addPago(pago("p2", "Abono", 120.0))
        viewModel.updateUiState { it.copy(ot = "OT-1", montoTotal = "200") }

        viewModel.saveDispensacion("pac-1", null) {}

        assertEquals(
            FinanzasRemoteDefaults.Messages.ABONO_MAYOR_QUE_TOTAL,
            viewModel.uiState.value.error,
        )
    }
}
