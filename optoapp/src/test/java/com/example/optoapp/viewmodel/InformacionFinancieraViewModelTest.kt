package com.example.optoapp.viewmodel

import com.example.optoapp.data.ContextoFinanciero
import com.example.optoapp.data.DispensacionFinancieraRepository
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class InformacionFinancieraViewModelTest {

    private lateinit var repository: DispensacionFinancieraRepository
    private lateinit var optoRepository: OptoRepository
    private lateinit var stockHelper: DispensacionStockHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var postSaveSyncScheduler: PostSaveSyncScheduler
    private lateinit var viewModel: InformacionFinancieraViewModel

    private val opticaIdFlow = MutableStateFlow("optica-test")
    private val testDate = LocalDate.of(2026, 7, 4)
    private val testDispatcher = StandardTestDispatcher()
    private val dispId = "disp-1"

    private val testContexto = ContextoFinanciero(
        ot = "OT-2026-0001",
        pacienteNombre = "Juan Perez",
        pacienteId = "pac-1",
        fecha = testDate,
        descripcion = "Monofocal",
    )

    private val testPagos = listOf(
        Pago(id = "p-1", dispensacionId = dispId, fecha = testDate, tipo = "Abono", monto = 50.0, metodoPago = "Efectivo", opticaId = "optica-test"),
        Pago(id = "p-2", dispensacionId = dispId, fecha = testDate, tipo = "Abono", monto = 30.0, metodoPago = "Tarjeta", opticaId = "optica-test"),
    )

    private val testDispensacion = DispensacionOptica(
        id = dispId,
        ot = "OT-2026-0001",
        pacienteId = "pac-1",
        fecha = testDate,
        opticaId = "optica-test",
        tipoLente = "Monofocal",
        montoTotal = 150.0,
        estadoEntrega = "Pendiente",
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
        optoRepository = mockk(relaxed = true)
        stockHelper = mockk(relaxed = true)
        every { repository.runInTransaction(any()) } answers {
            (firstArg() as () -> Unit).invoke()
        }
        sessionManager = mockk()
        postSaveSyncScheduler = mockk(relaxed = true)

        every { sessionManager.opticaId } returns opticaIdFlow

        coEvery { repository.obtenerContexto(dispId) } returns testContexto
        coEvery { repository.obtenerDispensacion(dispId) } returns Resource.Success(testDispensacion)
        coEvery { repository.agregarPago(any()) } returns Unit
        coEvery { repository.editarPago(any()) } returns Unit
        coEvery { repository.eliminarPago(any(), any()) } returns Unit
        coEvery { repository.actualizarMontoTotal(any(), any(), any()) } returns Unit
        coEvery { repository.actualizarMontoPagado(any(), any(), any()) } returns Unit
        coEvery { repository.actualizarEstado(any(), any(), any(), any()) } returns Unit
        coEvery { optoRepository.getRegalosByDispensacionId(any()) } returns emptyList()
        coEvery { repository.withTransaction(any<suspend () -> Unit>()) } coAnswers {
            val block = arg<suspend () -> Unit>(0)
            block()
        }
        every { optoRepository.getMonturasByOptica(any()) } returns flowOf(emptyList())
    }

    private fun createViewModel(): InformacionFinancieraViewModel =
        InformacionFinancieraViewModel(repository, optoRepository, stockHelper, sessionManager, postSaveSyncScheduler)

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadFinanciera loads contexto pagos and montoTotal`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.loadFinanciera(dispId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("OT-2026-0001", state.contexto?.ot)
        assertEquals("Juan Perez", state.contexto?.pacienteNombre)
        assertEquals("150.0", state.montoTotal)
        assertEquals("Pendiente", state.estadoEntrega)
    }

    @Test
    fun `loadFinanciera loads pagos from repository`() = runTest {
        coEvery { repository.obtenerPagos(dispId) } returns testPagos

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.loadFinanciera(dispId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, vm.uiState.value.pagos.size)
    }

    @Test
    fun `saldoRestante is calculated from montoTotal minus pagos`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.loadFinanciera(dispId)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.addPago(testPagos[0])
        vm.addPago(testPagos[1])
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(150.0 - 80.0, state.saldoRestante, 0.001)
    }

    @Test
    fun `saldoRestante is zero when no montoTotal`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(0.0, state.saldoRestante, 0.001)
    }

    @Test
    fun `addPago appends pago to list`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.addPago(testPagos[0])
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.pagos.size)
        assertEquals("p-1", vm.uiState.value.pagos[0].id)
    }

    @Test
    fun `updatePago replaces existing pago`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.addPago(testPagos[0])
        testDispatcher.scheduler.advanceUntilIdle()

        val modified = testPagos[0].copy(monto = 100.0)
        vm.updatePago(modified)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(100.0, vm.uiState.value.pagos[0].monto, 0.001)
    }

    @Test
    fun `removePago removes and tracks deletion`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.addPago(testPagos[0])
        testDispatcher.scheduler.advanceUntilIdle()

        vm.removePago(testPagos[0])
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, vm.uiState.value.pagos.size)
        assertEquals(1, vm.uiState.value.pagosToDelete.size)
    }

    @Test
    fun `updateEstado changes estadoEntrega`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.loadFinanciera(dispId)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.updateEstado("Entregado")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Entregado", vm.uiState.value.estadoEntrega)
        assertNotNull(vm.uiState.value.fechaEntrega)
    }

    @Test
    fun `updateEstado to Pendiente clears fechaEntrega`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.loadFinanciera(dispId)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.updateEstado("Entregado")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.updateEstado("Pendiente")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Pendiente", vm.uiState.value.estadoEntrega)
        assertEquals(null, vm.uiState.value.fechaEntrega)
    }

    @Test
    fun `save syncs montoPagado from pagos excluding anulacion`() = runTest {
        val unconfined = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfined)

        coEvery { repository.obtenerPagos(dispId) } returns testPagos
        coEvery { repository.withTransaction(any<suspend () -> Unit>()) } coAnswers {
            val block = arg<suspend () -> Unit>(0)
            block()
        }

        val vm = createViewModel()
        vm.loadFinanciera(dispId)
        advanceUntilIdle()

        var completed = false
        vm.save { completed = true }
        advanceUntilIdle()

        assertTrue(completed)
        coVerify { repository.actualizarMontoPagado(dispId, 80.0, "optica-test") }

        Dispatchers.setMain(testDispatcher)
    }
}
