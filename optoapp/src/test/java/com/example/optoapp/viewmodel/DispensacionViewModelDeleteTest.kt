package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import com.example.optoapp.domain.CalcularMontoPagadoUseCase
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DispensacionViewModelDeleteTest {

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var postSaveSyncScheduler: PostSaveSyncScheduler
    private lateinit var stockHelper: DispensacionStockHelper
    private lateinit var calcularMontoPagadoUseCase: CalcularMontoPagadoUseCase
    private lateinit var costoProductoDao: CostoProductoDao
    private lateinit var costoBiseladoDao: CostoBiseladoDao
    private lateinit var viewModel: DispensacionViewModel

    private val opticaIdFlow = MutableStateFlow("optica-test")
    private val opticaRolFlow = MutableStateFlow("admin")
    private val testDispatcher = StandardTestDispatcher()
    private val dispId = "disp-delete-1"
    private val testDate = LocalDate.of(2026, 7, 10)

    private val testDispensacion = DispensacionOptica(
        id = dispId, ot = "OT-2026-0001", pacienteId = "pac-1", fecha = testDate,
        opticaId = "optica-test", tipoLente = "Monofocal", montoTotal = 300.0,
        montoPagado = 150.0, estadoEntrega = "Pendiente", metodoPago = "Efectivo",
    )

    private val testRegalos = listOf(
        RegaloDispensacionEntity(
            id = "reg-del-1",
            dispensacionId = dispId,
            productoId = "prod-1",
            cantidad = 2,
            costoUnitario = 10.0,
            descripcion = "Estuche",
            motivo = "Cortesia",
            opticaId = "optica-test",
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
        stockHelper = mockk(relaxed = true)
        calcularMontoPagadoUseCase = mockk()
        costoProductoDao = mockk(relaxed = true)
        costoBiseladoDao = mockk(relaxed = true)

        every { sessionManager.opticaId } returns opticaIdFlow
        every { sessionManager.opticaRol } returns opticaRolFlow

        coEvery { repository.getDispensacionById(dispId) } returns Resource.Success(testDispensacion)
        coEvery { calcularMontoPagadoUseCase(dispId) } returns 150.0
        coEvery { repository.getRegalosByDispensacionId(dispId) } returns testRegalos
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `deleteDispensacion hard-deletes without anulacion`() = runTest {
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
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var completed = false
        viewModel.deleteDispensacion(dispId) { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteDispensacion(testDispensacion) }
        coVerify(inverse = true) { repository.updateDispensacion(any()) }
        coVerify(inverse = true) { repository.insertPago(any<Pago>()) }
    }

    @Test
    fun `deleteDispensacion reverts regalo stock`() = runTest {
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
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var completed = false
        viewModel.deleteDispensacion(dispId) { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            stockHelper.adjustStockAndRegistrarMovimiento(
                "prod-1",
                "optica-test",
                2,
                "AJUSTE",
                "reg-del-1",
                "Devolución por borrado de dispensación",
            )
        }
    }
}
