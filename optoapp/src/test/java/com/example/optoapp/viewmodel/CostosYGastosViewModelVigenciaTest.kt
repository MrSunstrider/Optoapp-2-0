package com.example.optoapp.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costolc.CostoLcDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import com.example.optoapp.domain.MatrizVigenciaUseCase
import com.example.optoapp.domain.ResultadoVigencia
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CostosYGastosViewModelVigenciaTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val opticaId = "optica-test"
    private val hoy = LocalDate.of(2026, 7, 20)

    private lateinit var costoProductoDao: CostoProductoDao
    private lateinit var scheduler: PostSaveSyncScheduler
    private lateinit var matrizVigencia: MatrizVigenciaUseCase
    private lateinit var viewModel: CostosYGastosViewModel

    private val vigente = CostoProductoEntity(
        id = "p2", opticaId = opticaId, material = "Resina", tipoLente = "Monofocal",
        stockOFabricacion = "stock", serie = 1, costoUnitario = 18.0, vigenteDesde = "2026-03-01",
    )
    private val anterior = vigente.copy(id = "p1", costoUnitario = 15.0, vigenteDesde = "2026-01-01", vigenteHasta = "2026-02-28")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        mockkObject(DateUtils)
        every { DateUtils.today() } returns hoy

        val repository = mockk<OptoRepository>(relaxed = true)
        val sessionManager = mockk<SessionManager>(relaxed = true)
        costoProductoDao = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        matrizVigencia = mockk()
        every { sessionManager.opticaId } returns flowOf(opticaId)
        every { sessionManager.opticaRol } returns flowOf("admin")
        every { repository.getGastosOperativos(opticaId) } returns emptyFlow()
        every { costoProductoDao.getByBloque(opticaId, "stock") } returns flowOf(listOf(vigente))
        coEvery {
            costoProductoDao.getHistorial(opticaId, "Resina", "Monofocal", "stock", null, 1, null)
        } returns listOf(vigente, anterior)

        viewModel = CostosYGastosViewModel(
            repository,
            costoProductoDao,
            mockk<CostoBiseladoDao>(relaxed = true),
            mockk<CostoLcDao>(relaxed = true),
            sessionManager,
            scheduler,
            mockk<SyncFinanzasUseCase>(relaxed = true),
            mockk<SyncStateTracker>(relaxed = true),
            matrizVigencia,
            SavedStateHandle(),
        )
        viewModel.loadBlock("Stock Monofocal")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun openingTheEditPreloadsTheEarliestAllowedDateAndTheHistoryNewestFirst() = runTest(testDispatcher) {
        viewModel.showEditCosto(vigente)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(hoy, state.vigenteDesde)
        assertEquals(
            listOf(VersionPrecio("p2", 18.0, "2026-03-01", null), VersionPrecio("p1", 15.0, "2026-01-01", "2026-02-28")),
            state.historialPrecios,
        )
    }

    @Test
    fun aSuccessfulEditVersionsTheRowSchedulesSyncAndClosesTheDialog() = runTest(testDispatcher) {
        val desde = LocalDate.of(2026, 8, 1)
        coEvery { matrizVigencia.editarProducto(vigente, 20.0, desde, hoy) } returns ResultadoVigencia.Aplicado("p3")

        viewModel.showEditCosto(vigente)
        viewModel.updateNuevoCostoUnitario("20.0")
        viewModel.updateVigenteDesde(desde)
        viewModel.saveCostoEdit()
        advanceUntilIdle()

        coVerify { matrizVigencia.editarProducto(vigente, 20.0, desde, hoy) }
        verify { scheduler.scheduleFinanzasSync(opticaId) }
        assertNull(viewModel.uiState.value.editingCosto)
        assertNull(viewModel.uiState.value.vigenteDesdeError)
    }

    @Test
    fun aRejectedDateShowsAnInlineErrorAndSchedulesNothing() = runTest(testDispatcher) {
        val desde = LocalDate.of(2026, 7, 19)
        coEvery { matrizVigencia.editarProducto(vigente, 20.0, desde, hoy) } returns ResultadoVigencia.Rechazado(hoy)

        viewModel.showEditCosto(vigente)
        viewModel.updateNuevoCostoUnitario("20.0")
        viewModel.updateVigenteDesde(desde)
        viewModel.saveCostoEdit()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.editingCosto)
        assertEquals("La fecha debe ser el 20/07/2026 o posterior", viewModel.uiState.value.vigenteDesdeError)
        verify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
    }

    @Test
    fun aRowClosedMeanwhileRefreshesTheBlockAndExplainsWhy() = runTest(testDispatcher) {
        coEvery { matrizVigencia.editarProducto(any(), any(), any(), any()) } returns ResultadoVigencia.YaNoVigente

        viewModel.showEditCosto(vigente)
        viewModel.updateNuevoCostoUnitario("20.0")
        viewModel.saveCostoEdit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingCosto)
        assertEquals("Este precio ya no está vigente. Revisa la lista actualizada.", viewModel.uiState.value.error)
        verify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
    }
}
