package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costolc.CostoLcDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
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
    private lateinit var costoProductoDao: CostoProductoDao
    private lateinit var costoBiseladoDao: CostoBiseladoDao
    private lateinit var costoLcDao: CostoLcDao
    private lateinit var viewModel: DispensacionViewModel

    private val opticaIdFlow = MutableStateFlow("optica-test")
    private val testDispatcher = StandardTestDispatcher()
    private val originalId = "disp-original"
    private val testDate = LocalDate.of(2026, 7, 10)

    private val originalDispensacion = DispensacionOptica(
        id = originalId, ot = "OT-2026-0001", pacienteId = "pac-1", fecha = testDate,
        opticaId = "optica-test", tipoLente = "Monofocal", montoTotal = 300.0,
        montoPagado = 200.0, estadoEntrega = "Pendiente", metodoPago = "Efectivo",
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
        costoProductoDao = mockk(relaxed = true)
        costoBiseladoDao = mockk(relaxed = true)
        costoLcDao = mockk(relaxed = true)

        every { sessionManager.opticaId } returns opticaIdFlow
        coEvery { repository.getDispensacionById(originalId, any()) } returns Resource.Success(originalDispensacion)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `crearReclamo marks original as Reclamada`() = runTest {
        coEvery { calcularMontoPagadoUseCase(originalId, any()) } returns 200.0
        val reclaim = mockk<com.example.optoapp.domain.ReclaimDispensacionUseCase>(relaxed = true)
        viewModel = DispensacionViewModel(
            repository,
            sessionManager,
            postSaveSyncScheduler,
            stockHelper,
            calcularMontoPagadoUseCase,
            mockk(relaxed = true),
            reclaim,
            costoProductoDao,
            costoBiseladoDao,
            costoLcDao,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var completed = false
        viewModel.crearReclamo(originalId, 250.0) { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            reclaim(
                dispensacionId = originalId,
                opticaId = "optica-test",
                refundMonto = 0.0,
                metodoPago = "Efectivo",
                ot = "OT-2026-0001",
            )
        }
        assertTrue(completed)
    }

    @Test
    fun `crearReclamo creates new dispensacion with reclamoOrigenId`() = runTest {
        coEvery { calcularMontoPagadoUseCase(originalId, any()) } returns 200.0
        viewModel = DispensacionViewModel(
            repository,
            sessionManager,
            postSaveSyncScheduler,
            stockHelper,
            calcularMontoPagadoUseCase,
            mockk<com.example.optoapp.domain.CancelDispensacionUseCase>(relaxed = true),
            mockk<com.example.optoapp.domain.ReclaimDispensacionUseCase>(relaxed = true),
            costoProductoDao,
            costoBiseladoDao,
            costoLcDao,
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
        coEvery { calcularMontoPagadoUseCase(originalId, any()) } returns 200.0
        viewModel = DispensacionViewModel(
            repository,
            sessionManager,
            postSaveSyncScheduler,
            stockHelper,
            calcularMontoPagadoUseCase,
            mockk<com.example.optoapp.domain.CancelDispensacionUseCase>(relaxed = true),
            mockk<com.example.optoapp.domain.ReclaimDispensacionUseCase>(relaxed = true),
            costoProductoDao,
            costoBiseladoDao,
            costoLcDao,
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
        coEvery { calcularMontoPagadoUseCase(originalId, any()) } returns 200.0
        val reclaim = mockk<com.example.optoapp.domain.ReclaimDispensacionUseCase>(relaxed = true)
        viewModel = DispensacionViewModel(
            repository,
            sessionManager,
            postSaveSyncScheduler,
            stockHelper,
            calcularMontoPagadoUseCase,
            mockk(relaxed = true),
            reclaim,
            costoProductoDao,
            costoBiseladoDao,
            costoLcDao,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var completed = false
        // nuevoMontoTotal = 150, totalPagado = 200 → refund 50
        viewModel.crearReclamo(originalId, 150.0) { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            reclaim(
                dispensacionId = originalId,
                opticaId = "optica-test",
                refundMonto = 50.0,
                metodoPago = "Efectivo",
                ot = "OT-2026-0001",
            )
        }
        coVerify(exactly = 0) { repository.insertPago(match { it.tipo == "Anulación" }) }
        assertTrue(completed)
    }

    @Test
    fun `crearReclamo diff equals zero does not create any Pago`() = runTest {
        coEvery { calcularMontoPagadoUseCase(originalId, any()) } returns 200.0
        viewModel = DispensacionViewModel(
            repository,
            sessionManager,
            postSaveSyncScheduler,
            stockHelper,
            calcularMontoPagadoUseCase,
            mockk<com.example.optoapp.domain.CancelDispensacionUseCase>(relaxed = true),
            mockk<com.example.optoapp.domain.ReclaimDispensacionUseCase>(relaxed = true),
            costoProductoDao,
            costoBiseladoDao,
            costoLcDao,
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
