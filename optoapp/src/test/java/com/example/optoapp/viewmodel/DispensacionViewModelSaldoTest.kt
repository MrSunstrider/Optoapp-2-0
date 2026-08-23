package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costolc.CostoLcDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.domain.CalcularMontoPagadoUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DispensacionStockHelper
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class DispensacionViewModelSaldoTest {

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var postSaveSyncScheduler: PostSaveSyncScheduler
    private lateinit var stockHelper: DispensacionStockHelper
    private lateinit var calcularMontoPagadoUseCase: CalcularMontoPagadoUseCase
    private lateinit var costoProductoDao: CostoProductoDao
    private lateinit var costoBiseladoDao: CostoBiseladoDao
    private lateinit var costoLcDao: CostoLcDao

    private val opticaIdFlow = MutableStateFlow("optica-test")
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testDate = LocalDate.of(2026, 7, 10)

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
        stockHelper = mockk(relaxed = true)
        calcularMontoPagadoUseCase = mockk()
        costoProductoDao = mockk(relaxed = true)
        costoBiseladoDao = mockk(relaxed = true)
        costoLcDao = mockk(relaxed = true)

        every { sessionManager.opticaId } returns opticaIdFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `pagosSumByDispensacion nets Abono with Reverso via PagoEffect`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", dispensacionId = "d1", tipo = "Abono", monto = 100.0, opticaId = "optica-test", fecha = testDate),
            Pago(id = "p2", dispensacionId = "d1", tipo = "Reverso", monto = 100.0, opticaId = "optica-test", fecha = testDate, reversaPagoId = "p1"),
            Pago(id = "p3", dispensacionId = "d1", tipo = "Anulación", monto = 100.0, opticaId = "optica-test", fecha = testDate),
        )
        every { repository.getAllPagosFlowForOptica("optica-test") } returns flowOf(pagos)

        val viewModel = DispensacionViewModel(
            repository, sessionManager, postSaveSyncScheduler, stockHelper,
            calcularMontoPagadoUseCase, mockk<com.example.optoapp.domain.CancelDispensacionUseCase>(relaxed = true), mockk<com.example.optoapp.domain.ReclaimDispensacionUseCase>(relaxed = true), costoProductoDao, costoBiseladoDao, costoLcDao,
        )
        advanceUntilIdle()

        val result = viewModel.pagosSumByDispensacion.first()
        assertEquals("Abono 100 + Reverso 100 + Anulación 0 = 0", 0.0, result["d1"] ?: 0.0, 0.001)
    }

    @Test
    fun `pagosSumByDispensacion handles multiple dispensaciones with Reverso`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", dispensacionId = "d1", tipo = "Abono", monto = 100.0, opticaId = "optica-test", fecha = testDate),
            Pago(id = "p2", dispensacionId = "d2", tipo = "Abono", monto = 200.0, opticaId = "optica-test", fecha = testDate),
            Pago(id = "p3", dispensacionId = "d1", tipo = "Reverso", monto = 100.0, opticaId = "optica-test", fecha = testDate, reversaPagoId = "p1"),
        )
        every { repository.getAllPagosFlowForOptica("optica-test") } returns flowOf(pagos)

        val viewModel = DispensacionViewModel(
            repository, sessionManager, postSaveSyncScheduler, stockHelper,
            calcularMontoPagadoUseCase, mockk<com.example.optoapp.domain.CancelDispensacionUseCase>(relaxed = true), mockk<com.example.optoapp.domain.ReclaimDispensacionUseCase>(relaxed = true), costoProductoDao, costoBiseladoDao, costoLcDao,
        )
        advanceUntilIdle()

        val result = viewModel.pagosSumByDispensacion.first()
        assertEquals("d1 nets to 0", 0.0, result["d1"] ?: 0.0, 0.001)
        assertEquals("d2 sum stays 200.0", 200.0, result["d2"] ?: 0.0, 0.001)
    }

    @Test
    fun `aCuentaSumByServicio nets Abono with Reverso via PagoEffect`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", servicioExtraId = "s1", tipo = "Abono", monto = 100.0, opticaId = "optica-test", fecha = testDate),
            Pago(id = "p2", servicioExtraId = "s1", tipo = "Reverso", monto = 100.0, opticaId = "optica-test", fecha = testDate, reversaPagoId = "p1"),
        )
        every { repository.getAllPagosFlowForOptica("optica-test") } returns flowOf(pagos)

        val viewModel = DispensacionViewModel(
            repository, sessionManager, postSaveSyncScheduler, stockHelper,
            calcularMontoPagadoUseCase, mockk<com.example.optoapp.domain.CancelDispensacionUseCase>(relaxed = true), mockk<com.example.optoapp.domain.ReclaimDispensacionUseCase>(relaxed = true), costoProductoDao, costoBiseladoDao, costoLcDao,
        )
        advanceUntilIdle()

        val result = viewModel.aCuentaSumByServicio.first()
        assertEquals("Abono + Reverso nets to 0", 0.0, result["s1"] ?: 0.0, 0.001)
    }

    @Test
    fun `aCuentaSumByServicio handles multiple servicios with Reembolso`() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(id = "p1", servicioExtraId = "s1", tipo = "Abono", monto = 50.0, opticaId = "optica-test", fecha = testDate),
            Pago(id = "p2", servicioExtraId = "s2", tipo = "Abono", monto = 75.0, opticaId = "optica-test", fecha = testDate),
            Pago(id = "p3", servicioExtraId = "s1", tipo = "Reembolso", monto = 50.0, opticaId = "optica-test", fecha = testDate),
        )
        every { repository.getAllPagosFlowForOptica("optica-test") } returns flowOf(pagos)

        val viewModel = DispensacionViewModel(
            repository, sessionManager, postSaveSyncScheduler, stockHelper,
            calcularMontoPagadoUseCase, mockk<com.example.optoapp.domain.CancelDispensacionUseCase>(relaxed = true), mockk<com.example.optoapp.domain.ReclaimDispensacionUseCase>(relaxed = true), costoProductoDao, costoBiseladoDao, costoLcDao,
        )
        advanceUntilIdle()

        val result = viewModel.aCuentaSumByServicio.first()
        assertEquals("s1 Abono+Reembolso → 0", 0.0, result["s1"] ?: 0.0, 0.001)
        assertEquals("s2 sum stays 75.0", 75.0, result["s2"] ?: 0.0, 0.001)
    }
}
