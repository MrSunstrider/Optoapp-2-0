package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.CalcularMontoPagadoUseCase
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DispensacionViewModelReclamoTest {

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var postSaveSyncScheduler: PostSaveSyncScheduler
    private lateinit var stockHelper: DispensacionStockHelper
    private lateinit var calcularMontoPagadoUseCase: CalcularMontoPagadoUseCase
    private lateinit var viewModel: DispensacionViewModel

    private val opticaIdFlow = MutableStateFlow("optica-test")
    private val testDispatcher = StandardTestDispatcher()
    private val originalId = "disp-original"
    private val testDate = LocalDate.of(2026, 7, 10)

    private val originalDispensacion = DispensacionOptica(
        id = originalId, ot = "OT-2026-0001", pacienteId = "pac-1", fecha = testDate,
        opticaId = "optica-test", tipoLente = "Monofocal", montoTotal = 300.0,
        montoPagado = 200.0, estadoEntrega = "Pendiente", metodoPago = "Efectivo"
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
        stockHelper = mockk(relaxed = true)
        calcularMontoPagadoUseCase = mockk()

        every { sessionManager.opticaId } returns opticaIdFlow
        coEvery { repository.getDispensacionById(originalId) } returns Resource.Success(originalDispensacion)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `crearReclamo marks original as Reclamada`() = runTest {
        coEvery { calcularMontoPagadoUseCase(originalId) } returns 200.0
        viewModel = DispensacionViewModel(
            repository, sessionManager, postSaveSyncScheduler, stockHelper, calcularMontoPagadoUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var completed = false
        viewModel.crearReclamo(originalId, 250.0) { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.updateDispensacion(match { it.estadoEntrega == "Reclamada" }) }
        assertTrue(completed)
    }

    @Test
    fun `crearReclamo creates new dispensacion with reclamoOrigenId`() = runTest {
        coEvery { calcularMontoPagadoUseCase(originalId) } returns 200.0
        viewModel = DispensacionViewModel(
            repository, sessionManager, postSaveSyncScheduler, stockHelper, calcularMontoPagadoUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val dispSlot = slot<DispensacionOptica>()
        coEvery { repository.insertDispensacion(capture(dispSlot)) } returns Unit

        var completed = false
        viewModel.crearReclamo(originalId, 250.0) { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(originalId, dispSlot.captured.reclamoOrigenId)
        assertEquals("Pendiente", dispSlot.captured.estadoEntrega)
        assertEquals(250.0, dispSlot.captured.montoTotal, 0.001)
    }

    @Test
    fun `crearReclamo diff greater than zero does not create refund Pago`() = runTest {
        coEvery { calcularMontoPagadoUseCase(originalId) } returns 200.0
        viewModel = DispensacionViewModel(
            repository, sessionManager, postSaveSyncScheduler, stockHelper, calcularMontoPagadoUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var completed = false
        // nuevoMontoTotal = 250, totalPagado = 200, diff = 50 > 0
        viewModel.crearReclamo(originalId, 250.0) { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()

        // Should NOT have inserted a refund pago
        coVerify(inverse = true) { repository.insertPago(match { it.monto < 0 }) }
        assertTrue(completed)
    }

    @Test
    fun `crearReclamo diff less than zero creates refund Pago with negative monto`() = runTest {
        coEvery { calcularMontoPagadoUseCase(originalId) } returns 200.0
        viewModel = DispensacionViewModel(
            repository, sessionManager, postSaveSyncScheduler, stockHelper, calcularMontoPagadoUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val pagoSlot = slot<Pago>()
        coEvery { repository.insertPago(capture(pagoSlot)) } returns Unit

        var completed = false
        // nuevoMontoTotal = 150, totalPagado = 200, diff = -50 < 0
        viewModel.crearReclamo(originalId, 150.0) { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(-50.0, pagoSlot.captured.monto, 0.001)
        assertEquals("Anulación", pagoSlot.captured.tipo)
        assertEquals(originalId, pagoSlot.captured.dispensacionId)
        assertTrue(pagoSlot.captured.nota.contains("Reembolso"))
        assertTrue(completed)
    }

    @Test
    fun `crearReclamo diff equals zero does not create any Pago`() = runTest {
        coEvery { calcularMontoPagadoUseCase(originalId) } returns 200.0
        viewModel = DispensacionViewModel(
            repository, sessionManager, postSaveSyncScheduler, stockHelper, calcularMontoPagadoUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var completed = false
        // nuevoMontoTotal = 200, totalPagado = 200, diff = 0
        viewModel.crearReclamo(originalId, 200.0) { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(inverse = true) { repository.insertPago(any()) }
        assertTrue(completed)
    }
}
