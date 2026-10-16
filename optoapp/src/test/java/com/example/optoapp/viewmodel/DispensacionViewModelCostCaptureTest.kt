package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.domain.AnularDispensacionUseCase
import com.example.optoapp.domain.CalcularCostoItemUseCase
import com.example.optoapp.domain.CalcularMontoPagadoUseCase
import com.example.optoapp.domain.ComponenteCosto
import com.example.optoapp.domain.CostoItemCalculado
import com.example.optoapp.domain.OrigenComponente
import com.example.optoapp.domain.ReclamarDispensacionUseCase
import com.example.optoapp.domain.ResumenRecalcMarker
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DispensacionStockHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DispensacionViewModelCostCaptureTest {

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var calcularCostoItem: CalcularCostoItemUseCase
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fechaOt = LocalDate.of(2026, 9, 15)
    private val tracker = mockk<SyncStateTracker>(relaxed = true)
    @Volatile private var enTransaccion = false
    private val marcadoEnTransaccion = java.util.Collections.synchronizedList(mutableListOf<Boolean>())

    private val evaluacion = EvaluacionClinica(
        id = "eval-1",
        pacienteId = "pac-1",
        fecha = LocalDate.of(2026, 9, 1),
        recetaOdEsf = "-1,00",
        recetaOdCil = "-0,50",
        recetaOiEsf = "-1,25",
    )

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0

        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        sessionManager = mockk()
        calcularCostoItem = mockk()
        every { sessionManager.opticaId } returns MutableStateFlow("optica-test")
        every { sessionManager.opticaRol } returns MutableStateFlow("admin")
        every { repository.runInTransaction(any()) } answers {
            enTransaccion = true
            try {
                firstArg<() -> Unit>().invoke()
            } finally {
                enTransaccion = false
            }
        }
        coEvery { tracker.markPendingRecalc(any(), any(), any(), any()) } answers { marcadoEnTransaccion += enTransaccion }
        coEvery { repository.getEvaluacionById("eval-1", "optica-test") } returns Resource.Success(evaluacion)
        coEvery { calcularCostoItem(any(), any(), any(), any()) } returns
            CostoItemCalculado(mapOf(ComponenteCosto.OD to 18.0, ComponenteCosto.OI to 18.0))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = DispensacionViewModel(
        repository,
        sessionManager,
        mockk<PostSaveSyncScheduler>(relaxed = true),
        mockk<DispensacionStockHelper>(relaxed = true),
        mockk<CalcularMontoPagadoUseCase>(relaxed = true),
        mockk<AnularDispensacionUseCase>(relaxed = true),
        mockk<ReclamarDispensacionUseCase>(relaxed = true),
        calcularCostoItem,
        mockk(relaxed = true),
        ResumenRecalcMarker(tracker),
        mockk(relaxed = true))

    // Awaits in real time because the save transaction runs on Dispatchers.IO, outside the test scheduler.
    private suspend fun DispensacionViewModel.guardar(dispensacionId: String?) {
        val guardado = CompletableDeferred<Unit>()
        saveDispensacion("pac-1", dispensacionId) { guardado.complete(Unit) }
        withContext(Dispatchers.Default) { withTimeout(10_000) { guardado.await() } }
    }

    private val itemUi = DispensacionItemUi(id = "item-1", tipoLente = "Monofocal", materialLente = "Resina")

    private fun DispensacionViewModel.prepararOt() = updateUiState {
        it.copy(ot = "OT-2026-0001", fecha = fechaOt, evaluacionId = "eval-1", items = listOf(itemUi), montoTotal = "100")
    }

    @Test
    fun `saving a new order captures the matrix cost without pressing the calculate button`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.prepararOt()

        viewModel.guardar(null)

        coVerify {
            repository.insertDispensacionItem(
                withArg { item ->
                    assertEquals(18.0, item.costoRealOd!!, 0.001)
                    assertEquals(18.0, item.costoRealOi!!, 0.001)
                    assertEquals("matriz", item.costoOrigen)
                },
            )
        }
        coVerify {
            calcularCostoItem(
                "optica-test",
                any(),
                withArg { receta -> assertEquals(-1.0, receta!!.odEsf!!, 0.001) },
                fechaOt,
            )
        }
    }

    @Test
    fun `a session override is saved as manual instead of the matrix value`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.prepararOt()
        viewModel.setCostoOverride(0, ComponenteCosto.OD, 25.0)

        viewModel.guardar(null)

        coVerify {
            repository.insertDispensacionItem(
                withArg { item ->
                    assertEquals(25.0, item.costoRealOd!!, 0.001)
                    assertEquals(18.0, item.costoRealOi!!, 0.001)
                    assertEquals("mixto", item.costoOrigen)
                },
            )
        }
    }

    @Test
    fun `editing with unchanged inputs keeps the persisted snapshot`() = runTest(testDispatcher) {
        val persistido = DispensacionItem(
            id = "item-1",
            dispensacionId = "disp-1",
            tipoLente = "Monofocal",
            materialLente = "Resina",
            costoRealOd = 15.0,
            costoRealOi = 15.0,
            costoOrigen = "matriz",
        )
        coEvery { repository.getDispensacionItemsByDispensacion("disp-1", "optica-test") } returns listOf(persistido)
        coEvery { repository.getDispensacionById("disp-1", "optica-test") } returns Resource.Success(
            DispensacionOptica(
                id = "disp-1", ot = "OT-2026-0001", pacienteId = "pac-1", fecha = fechaOt,
                opticaId = "optica-test", estadoEntrega = "Pendiente", evaluacionId = "eval-1",
            ),
        )
        val viewModel = createViewModel()
        viewModel.prepararOt()

        viewModel.guardar("disp-1")

        coVerify {
            repository.insertDispensacionItem(
                withArg { item ->
                    assertEquals(15.0, item.costoRealOd!!, 0.001)
                    assertEquals("matriz", item.costoOrigen)
                },
            )
        }
        coVerify(exactly = 0) { calcularCostoItem(any(), any(), any(), any()) }
    }

    @Test
    fun `the cost preview uses the same capture as the save`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.prepararOt()
        viewModel.setCostoOverride(0, ComponenteCosto.OI, 30.0)

        viewModel.calculateCosts(0)
        advanceUntilIdle()

        val item = viewModel.uiState.value.items.first()
        assertEquals(18.0, item.costoRealOd!!, 0.001)
        assertEquals(30.0, item.costoRealOi!!, 0.001)
    }

    @Test
    fun `the cost preview records the origin of each component`() = runTest(testDispatcher) {
        coEvery { calcularCostoItem(any(), any(), any(), any()) } returns
            CostoItemCalculado(mapOf(ComponenteCosto.OD to 18.0, ComponenteCosto.BISELADO to null))
        val viewModel = createViewModel()
        viewModel.prepararOt()

        viewModel.calculateCosts(0)
        advanceUntilIdle()

        val origenes = viewModel.uiState.value.items.first().costoOrigenes
        assertEquals(OrigenComponente.MATRIZ, origenes[ComponenteCosto.OD])
        assertEquals(OrigenComponente.FALTANTE, origenes[ComponenteCosto.BISELADO])
    }

    @Test
    fun `clearing an override restores the matrix value and origin`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.prepararOt()
        viewModel.setCostoOverride(0, ComponenteCosto.OD, 20.0)
        viewModel.calculateCosts(0)
        advanceUntilIdle()

        viewModel.clearCostoOverride(0, ComponenteCosto.OD)
        advanceUntilIdle()

        val item = viewModel.uiState.value.items.first()
        assertTrue(item.costoOverrides.isEmpty())
        assertEquals(18.0, item.costoRealOd!!, 0.001)
        assertEquals(OrigenComponente.MATRIZ, item.costoOrigenes[ComponenteCosto.OD])
    }

    @Test
    fun `a loaded order keeps the saved cost origin of each item`() = runTest(testDispatcher) {
        val persistido = DispensacionItem(id = "item-1", dispensacionId = "disp-1", costoRealOd = 15.0, costoOrigen = "faltante")
        coEvery { repository.getDispensacionById("disp-1", "optica-test") } returns Resource.Success(
            DispensacionOptica(
                id = "disp-1", ot = "OT-2026-0001", pacienteId = "pac-1", fecha = fechaOt,
                opticaId = "optica-test", estadoEntrega = "Pendiente",
            ),
        )
        coEvery { repository.getDispensacionItemsByDispensacion("disp-1", "optica-test") } returns listOf(persistido)
        coEvery { repository.getPagosByDispensacion("disp-1", "optica-test") } returns flowOf(emptyList())
        coEvery { repository.getRegalosByDispensacionId("disp-1", "optica-test") } returns emptyList()
        val viewModel = createViewModel()

        viewModel.loadDispensacion("disp-1")

        val item = viewModel.uiState.first { !it.isLoading && it.ot.isNotEmpty() }.items.single()
        assertEquals("faltante", item.costoOrigen)
    }

    private fun persistirOtPasada() {
        val persistido = DispensacionItem(
            id = "item-1", dispensacionId = "disp-1", tipoLente = "Monofocal", materialLente = "Resina",
            costoRealOd = 15.0, costoRealOi = 15.0, costoOrigen = "matriz",
        )
        coEvery { repository.getDispensacionItemsByDispensacion("disp-1", "optica-test") } returns listOf(persistido)
        coEvery { repository.getDispensacionById("disp-1", "optica-test") } returns Resource.Success(
            DispensacionOptica(
                id = "disp-1", ot = "OT-2026-0001", pacienteId = "pac-1", fecha = fechaOt,
                opticaId = "optica-test", estadoEntrega = "Pendiente", evaluacionId = "eval-1",
            ),
        )
    }

    @Test
    fun `a past order whose cost and date change marks both dates inside the save transaction`() = runTest(testDispatcher) {
        persistirOtPasada()
        val nuevaFecha = fechaOt.plusDays(5)
        val viewModel = createViewModel()
        viewModel.prepararOt()
        viewModel.updateUiState { it.copy(fecha = nuevaFecha) }
        viewModel.setCostoOverride(0, ComponenteCosto.OD, 25.0)

        viewModel.guardar("disp-1")

        coVerify(exactly = 1) { tracker.markPendingRecalc("optica-test", fechaOt, "dispensacion", "disp-1") }
        coVerify(exactly = 1) { tracker.markPendingRecalc("optica-test", nuevaFecha, "dispensacion", "disp-1") }
        assertEquals(listOf(true, true), marcadoEnTransaccion)
    }

    @Test
    fun `a past order saved with the same cost and date marks nothing`() = runTest(testDispatcher) {
        persistirOtPasada()
        val viewModel = createViewModel()
        viewModel.prepararOt()

        viewModel.guardar("disp-1")

        coVerify(exactly = 0) { tracker.markPendingRecalc(any(), any(), any(), any()) }
    }
}
