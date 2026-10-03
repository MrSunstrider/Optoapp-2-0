package com.example.optoapp.viewmodel

import com.example.optoapp.data.ContextoFinanciero
import com.example.optoapp.data.DispensacionFinancieraRepository
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.CalcularMontoPagadoUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DispensacionStockHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class InformacionFinancieraViewModelTest {

    private lateinit var repository: DispensacionFinancieraRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var postSaveSyncScheduler: PostSaveSyncScheduler
    private lateinit var calcularMontoPagado: CalcularMontoPagadoUseCase
    private lateinit var stockHelper: DispensacionStockHelper

    private val opticaIdFlow = MutableStateFlow("optica-test")
    private val testDate = LocalDate.of(2026, 7, 4)
    private val testDispatcher = UnconfinedTestDispatcher()
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

    private fun createViewModel() = InformacionFinancieraViewModel(
        repository, sessionManager, postSaveSyncScheduler, calcularMontoPagado, stockHelper,
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
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            firstArg<suspend () -> Any?>().invoke()
        }
        sessionManager = mockk()
        postSaveSyncScheduler = mockk(relaxed = true)
        calcularMontoPagado = mockk()
        stockHelper = mockk(relaxed = true)

        every { sessionManager.opticaId } returns opticaIdFlow

        coEvery { repository.obtenerContexto(dispId, any()) } returns testContexto
        coEvery { repository.obtenerDispensacion(dispId, any()) } returns Resource.Success(testDispensacion)
        coEvery { repository.obtenerPagos(dispId, any()) } returns emptyList()
        coEvery { repository.obtenerRegalos(dispId, any()) } returns emptyList()
        coEvery { repository.agregarPago(any()) } returns Unit
        coEvery { repository.editarPago(any()) } returns Unit
        coEvery { repository.eliminarPago(any(), any()) } returns Unit
        coEvery { repository.actualizarMontoTotal(any(), any(), any()) } returns Unit
        coEvery { repository.actualizarEstado(any(), any(), any(), any()) } returns Unit
        coEvery { repository.actualizarMontoPagado(any(), any(), any()) } returns Unit
        coEvery { repository.insertarRegalo(any()) } returns Unit
        coEvery { repository.eliminarRegalosByDispensacionId(any(), any()) } returns Unit
        coEvery { calcularMontoPagado(any(), any()) } returns 0.0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadFinanciera loads contexto pagos and montoTotal`() = runTest {
        val vm = createViewModel()

        vm.loadFinanciera(dispId)

        val state = vm.uiState.value
        assertEquals("OT-2026-0001", state.contexto?.ot)
        assertEquals("Juan Perez", state.contexto?.pacienteNombre)
        assertEquals("150", state.montoTotal)
        assertEquals("Pendiente", state.estadoEntrega)
    }

    @Test
    fun `loadFinanciera loads pagos from repository`() = runTest {
        coEvery { repository.obtenerPagos(dispId, any()) } returns testPagos

        val vm = createViewModel()

        vm.loadFinanciera(dispId)

        assertEquals(2, vm.uiState.value.pagos.size)
    }

    @Test
    fun `saldoRestante is calculated from montoTotal minus pagos`() = runTest {
        val vm = createViewModel()

        vm.loadFinanciera(dispId)

        vm.addPago(testPagos[0])
        vm.addPago(testPagos[1])

        val state = vm.uiState.value
        assertEquals(150.0 - 80.0, state.saldoRestante, 0.001)
    }

    @Test
    fun `saldoRestante is zero when no montoTotal`() = runTest {
        val vm = createViewModel()

        val state = vm.uiState.value
        assertEquals(0.0, state.saldoRestante, 0.001)
    }

    @Test
    fun `addPago appends pago to list`() = runTest {
        val vm = createViewModel()

        vm.addPago(testPagos[0])

        assertEquals(1, vm.uiState.value.pagos.size)
        assertEquals("p-1", vm.uiState.value.pagos[0].id)
    }

    @Test
    fun `updatePago replaces existing pago`() = runTest {
        val vm = createViewModel()

        vm.addPago(testPagos[0])

        val modified = testPagos[0].copy(monto = 100.0)
        vm.updatePago(modified)

        assertEquals(100.0, vm.uiState.value.pagos[0].monto, 0.001)
    }

    @Test
    fun `removePago removes and tracks deletion`() = runTest {
        val vm = createViewModel()

        vm.addPago(testPagos[0])

        vm.removePago(testPagos[0])

        assertEquals(0, vm.uiState.value.pagos.size)
        assertEquals(1, vm.uiState.value.pagosToDelete.size)
    }

    @Test
    fun `updateEstado changes estadoEntrega`() = runTest {
        val vm = createViewModel()

        vm.loadFinanciera(dispId)

        vm.updateEstado("Entregado")

        assertEquals("Entregado", vm.uiState.value.estadoEntrega)
        assertNotNull(vm.uiState.value.fechaEntrega)
    }

    @Test
    fun `updateEstado to Pendiente clears fechaEntrega`() = runTest {
        val vm = createViewModel()

        vm.loadFinanciera(dispId)

        vm.updateEstado("Entregado")
        vm.updateEstado("Pendiente")

        assertEquals("Pendiente", vm.uiState.value.estadoEntrega)
        assertEquals(null, vm.uiState.value.fechaEntrega)
    }

    @Test
    fun `save calls actualizarMontoPagado with effect-aware net after pago CRUD`() = runTest {
        coEvery { repository.obtenerPagos(dispId, any()) } returns testPagos
        coEvery { calcularMontoPagado(dispId, any()) } returns 150.0

        val vm = createViewModel()
        vm.loadFinanciera(dispId)

        var completed = false
        vm.save { completed = true }

        coVerify { repository.actualizarMontoPagado(dispId, 150.0, "optica-test") }
        assertEquals(true, completed)
    }

    @Test
    fun `save persists montoPagado AFTER pago insertions`() = runTest {
        coEvery { calcularMontoPagado(dispId, any()) } returns 200.0

        val vm = createViewModel()
        vm.loadFinanciera(dispId)
        val newPago = Pago(id = "p-new", dispensacionId = dispId, fecha = testDate, tipo = "Abono", monto = 200.0, metodoPago = "Efectivo", opticaId = "optica-test")
        vm.addPago(newPago)

        vm.save {}

        coVerifyOrder {
            repository.agregarPago(any())
            calcularMontoPagado(dispId, any())
            repository.actualizarMontoPagado(dispId, 200.0, "optica-test")
        }
    }

    @Test
    fun `save with abono and reembolso updates montoPagado correctly`() = runTest {
        val abono = Pago(id = "p-a", dispensacionId = dispId, fecha = testDate, tipo = "Abono", monto = 200.0, metodoPago = "Efectivo", opticaId = "optica-test")
        val reembolso = Pago(id = "p-r", dispensacionId = dispId, fecha = testDate, tipo = "Reembolso", monto = 50.0, metodoPago = "Efectivo", opticaId = "optica-test")
        coEvery { repository.obtenerPagos(dispId, any()) } returns listOf(abono, reembolso)
        coEvery { calcularMontoPagado(dispId, any()) } returns 150.0

        val vm = createViewModel()
        vm.loadFinanciera(dispId)

        vm.save {}

        coVerify { repository.actualizarMontoPagado(dispId, 150.0, "optica-test") }
    }

    @Test
    fun `save persists a zero total for a claim replacement`() = runTest {
        coEvery { repository.obtenerDispensacion(dispId, any()) } returns
            Resource.Success(testDispensacion.copy(montoTotal = 0.0, reclamoOrigenId = "disp-origen"))
        val vm = createViewModel()
        vm.loadFinanciera(dispId)
        vm.updateMontoTotal("0")

        var completed = false
        vm.save { completed = true }

        coVerify { repository.actualizarMontoTotal(dispId, 0.0, "optica-test") }
        assertEquals(true, completed)
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun `save rejects a zero total for an ordinary dispensacion and writes nothing`() = runTest {
        val vm = createViewModel()
        vm.loadFinanciera(dispId)
        vm.updateMontoTotal("0")

        var completed = false
        vm.save { completed = true }

        coVerify(exactly = 0) { repository.actualizarMontoTotal(any(), any(), any()) }
        assertEquals(false, completed)
        assertEquals(FinanzasRemoteDefaults.Messages.MONTO_TOTAL_MAYOR_A_CERO, vm.uiState.value.error)
    }

    @Test
    fun `save rejects a negative total for an ordinary dispensacion and writes nothing`() = runTest {
        val vm = createViewModel()
        vm.loadFinanciera(dispId)
        vm.updateMontoTotal("-1")

        var completed = false
        vm.save { completed = true }

        coVerify(exactly = 0) { repository.actualizarMontoTotal(any(), any(), any()) }
        assertEquals(false, completed)
        assertEquals(FinanzasRemoteDefaults.Messages.MONTO_TOTAL_MAYOR_A_CERO, vm.uiState.value.error)
    }

    @Test
    fun `save rejects a negative total for a claim replacement`() = runTest {
        coEvery { repository.obtenerDispensacion(dispId, any()) } returns
            Resource.Success(testDispensacion.copy(reclamoOrigenId = "disp-origen"))
        val vm = createViewModel()
        vm.loadFinanciera(dispId)
        vm.updateMontoTotal("-1")

        vm.save {}

        coVerify(exactly = 0) { repository.actualizarMontoTotal(any(), any(), any()) }
        assertEquals(FinanzasRemoteDefaults.Messages.MONTO_TOTAL_MAYOR_A_CERO, vm.uiState.value.error)
    }

    @Test
    fun `save on an order cancelled after loading writes nothing and emits error`() = runTest {
        val vm = createViewModel()
        vm.loadFinanciera(dispId)
        coEvery { repository.obtenerDispensacion(dispId, any()) } returns
            Resource.Success(testDispensacion.copy(estadoEntrega = "Anulado"))
        vm.addPago(testPagos[0])

        var completed = false
        vm.save { completed = true }

        coVerify(exactly = 0) { repository.agregarPago(any()) }
        coVerify(exactly = 0) { repository.actualizarMontoTotal(any(), any(), any()) }
        coVerify(exactly = 0) { repository.actualizarEstado(any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.actualizarMontoPagado(any(), any(), any()) }
        coVerify(exactly = 0) { postSaveSyncScheduler.scheduleFinanzasSync(any()) }
        assertEquals(false, completed)
        assertEquals("Error al guardar: La orden está anulada y no se puede modificar la información financiera.", vm.uiState.value.error)
    }

    @Test
    fun `save on Reclamada order cannot revert estado to Pendiente`() = runTest {
        coEvery { repository.obtenerDispensacion(dispId, any()) } returns
            Resource.Success(testDispensacion.copy(estadoEntrega = "Reclamada"))
        val vm = createViewModel()
        vm.loadFinanciera(dispId)

        vm.updateEstado("Pendiente")
        vm.save {}

        assertEquals("Reclamada", vm.uiState.value.estadoEntrega)
        coVerify(exactly = 0) { repository.actualizarEstado(any(), any(), any(), any()) }
        assertEquals("Error al guardar: La orden está reclamada y no se puede modificar la información financiera.", vm.uiState.value.error)
    }

    @Test
    fun `save on Entregado order persists the added pago`() = runTest {
        coEvery { repository.obtenerDispensacion(dispId, any()) } returns
            Resource.Success(testDispensacion.copy(estadoEntrega = "Entregado"))
        val vm = createViewModel()
        vm.loadFinanciera(dispId)
        vm.addPago(testPagos[0])

        var completed = false
        vm.save { completed = true }

        coVerify { repository.agregarPago(match { it.id == "p-1" && it.ventaId == "v_disp_$dispId" }) }
        assertEquals(true, completed)
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun `updateEstado ignores terminal targets on an active order`() = runTest {
        val vm = createViewModel()
        vm.loadFinanciera(dispId)

        vm.updateEstado("Anulado")
        vm.updateEstado("Reclamada")

        assertEquals("Pendiente", vm.uiState.value.estadoEntrega)
        assertEquals(null, vm.uiState.value.fechaEntrega)
    }

    @Test
    fun `updateEstado ignores any change on an Anulado order`() = runTest {
        coEvery { repository.obtenerDispensacion(dispId, any()) } returns
            Resource.Success(testDispensacion.copy(estadoEntrega = "Anulado"))
        val vm = createViewModel()
        vm.loadFinanciera(dispId)

        vm.updateEstado("Entregado")

        assertEquals("Anulado", vm.uiState.value.estadoEntrega)
        assertEquals(null, vm.uiState.value.fechaEntrega)
    }

    @Test
    fun `loading an Anulado order exposes it as read-only with its reason and date`() = runTest {
        coEvery { repository.obtenerDispensacion(dispId, any()) } returns Resource.Success(
            testDispensacion.copy(estadoEntrega = "Anulado", motivoAnulacion = "Cliente desistió", fechaAnulacion = testDate),
        )
        val vm = createViewModel()
        vm.loadFinanciera(dispId)

        val state = vm.uiState.value
        assertEquals(true, state.isReadOnly)
        assertEquals("Cliente desistió", state.motivoAnulacion)
        assertEquals(testDate, state.fechaAnulacion)
        assertEquals(listOf("Anulado"), state.selectableEstados)
    }

    @Test
    fun `an active order stays editable and offers only manual estados`() = runTest {
        val vm = createViewModel()
        vm.loadFinanciera(dispId)

        val state = vm.uiState.value
        assertEquals(false, state.isReadOnly)
        assertEquals(null, state.motivoAnulacion)
        assertEquals(listOf("Pendiente", "Entregado"), state.selectableEstados)
    }

    @Test
    fun `save rejects empty montoTotal`() = runTest {
        val vm = createViewModel()
        vm.loadFinanciera(dispId)
        vm.updateMontoTotal("")

        vm.save {}

        assertEquals(FinanzasRemoteDefaults.Messages.MONTO_TOTAL_MAYOR_A_CERO, vm.uiState.value.error)
    }
}
