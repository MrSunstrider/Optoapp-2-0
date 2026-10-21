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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CostosYGastosViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: OptoRepository
    private lateinit var costoProductoDao: CostoProductoDao
    private lateinit var costoBiseladoDao: CostoBiseladoDao
    private lateinit var sessionManager: SessionManager
    private lateinit var scheduler: PostSaveSyncScheduler
    private lateinit var syncFinanzas: SyncFinanzasUseCase
    private lateinit var syncStateTracker: SyncStateTracker
    private lateinit var viewModel: CostosYGastosViewModel

    private val opticaId = "optica-test"
    private val testDate = LocalDate.of(2026, 7, 16)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // android.util.Log throws in plain JVM tests, and caught exceptions are logged.
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0

        mockkObject(DateUtils)
        every { DateUtils.today() } returns testDate
        every { DateUtils.toIso(testDate) } returns "2026-07-16"

        repository = mockk(relaxed = true)
        costoProductoDao = mockk(relaxed = true)
        costoBiseladoDao = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        syncFinanzas = mockk(relaxed = true)
        syncStateTracker = mockk(relaxed = true)

        every { sessionManager.opticaId } returns flowOf(opticaId)
        every { sessionManager.opticaRol } returns flowOf("admin")
        every { repository.getGastosOperativos(opticaId) } returns emptyFlow()
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        coEvery { costoProductoDao.getByBloque(any(), any()) } returns flowOf(emptyList())
    }

    private fun buildViewModel() = CostosYGastosViewModel(
        repository,
        costoProductoDao,
        costoBiseladoDao,
        mockk<CostoLcDao>(relaxed = true),
        sessionManager,
        scheduler,
        syncFinanzas,
        syncStateTracker,
        mockk<MatrizVigenciaUseCase>(),
        SavedStateHandle(),
    )

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun saveCosto_createsEntityWithUuidOpticaIdAndVigenteDesde_callsUpsertAll() = runTest(testDispatcher) {
        coEvery { costoProductoDao.getByBloque(opticaId, "stock") } returns flowOf(emptyList())

        viewModel = buildViewModel()

        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        viewModel.showNewCosto()
        viewModel.updateCostoMaterial("CR39")
        viewModel.updateCostoTipoLente("Monofocal")
        viewModel.updateCostoCostoUnitario("150.00")

        viewModel.saveCosto()
        advanceUntilIdle()

        coVerify {
            costoProductoDao.upsertAll(
                withArg { entities ->
                    assertEquals(1, entities.size)
                    val entity = entities[0]
                    assertEquals(opticaId, entity.opticaId)
                    assertEquals("CR39", entity.material)
                    assertEquals("Monofocal", entity.tipoLente)
                    assertEquals(150.0, entity.costoUnitario, 0.001)
                    assertEquals("2026-07-16", entity.vigenteDesde)
                    assertNotNull("Entity must have a generated UUID", entity.id)
                    assertTrue("Entity ID must not be blank", entity.id.isNotBlank())
                    assertNotNull("A new price must carry its edit time for last-write-wins", entity.updatedAt)
                },
            )
        }

        coVerify(atLeast = 1) { costoProductoDao.getByBloque(opticaId, "stock") }
        coVerify { syncStateTracker.markLocallyModified(opticaId, "costo_producto", any()) }

        coVerify { scheduler.scheduleFinanzasSync(opticaId) }

        assertFalse("Dialog must be dismissed after successful save", viewModel.uiState.value.isCostoDialogVisible)
        assertNull("No error expected after successful save", viewModel.uiState.value.costoSaveError)
    }

    @Test
    fun saveCosto_rejectsEmptyMaterial_setsCostoSaveError() = runTest(testDispatcher) {
        viewModel = buildViewModel()

        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        viewModel.showNewCosto()
        viewModel.updateCostoTipoLente("Monofocal")
        viewModel.updateCostoCostoUnitario("150.00")

        viewModel.saveCosto()

        assertEquals("Selecciona un material", viewModel.uiState.value.costoSaveError)
        assertTrue("Dialog must remain visible when validation fails", viewModel.uiState.value.isCostoDialogVisible)
    }

    @Test
    fun saveCosto_rejectsEmptyTipoLente_setsCostoSaveError() = runTest(testDispatcher) {
        viewModel = buildViewModel()

        // Fabricación Resina does not prefill tipo — Stock Monofocal would.
        viewModel.loadBlock("Fabricación Resina")
        advanceUntilIdle()

        viewModel.showNewCosto()
        viewModel.updateCostoMaterial("CR39")
        viewModel.updateCostoCostoUnitario("150.00")

        viewModel.saveCosto()

        assertEquals("Selecciona un tipo de lente", viewModel.uiState.value.costoSaveError)
        assertTrue("Dialog must remain visible when validation fails", viewModel.uiState.value.isCostoDialogVisible)
    }

    @Test
    fun saveCosto_rejectsInvalidCostoUnitario_setsCostoSaveError() = runTest(testDispatcher) {
        viewModel = buildViewModel()

        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        viewModel.showNewCosto()
        viewModel.updateCostoMaterial("CR39")
        viewModel.updateCostoTipoLente("Monofocal")
        viewModel.updateCostoCostoUnitario("0")

        viewModel.saveCosto()

        assertEquals("Ingresa un costo unitario válido", viewModel.uiState.value.costoSaveError)
        assertTrue("Dialog must remain visible when validation fails", viewModel.uiState.value.isCostoDialogVisible)
    }

    @Test
    fun saveCosto_rejectsNegativeCostoUnitario_setsCostoSaveError() = runTest(testDispatcher) {
        viewModel = buildViewModel()

        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        viewModel.showNewCosto()
        viewModel.updateCostoMaterial("CR39")
        viewModel.updateCostoTipoLente("Monofocal")
        viewModel.updateCostoCostoUnitario("-50")

        viewModel.saveCosto()

        assertEquals("Ingresa un costo unitario válido", viewModel.uiState.value.costoSaveError)
    }

    @Test
    fun saveCosto_rejectsNonNumericCostoUnitario_setsCostoSaveError() = runTest(testDispatcher) {
        viewModel = buildViewModel()

        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        viewModel.showNewCosto()
        viewModel.updateCostoMaterial("CR39")
        viewModel.updateCostoTipoLente("Monofocal")
        viewModel.updateCostoCostoUnitario("abc")

        viewModel.saveCosto()

        assertEquals("Ingresa un costo unitario válido", viewModel.uiState.value.costoSaveError)
    }

    @Test
    fun deleteCosto_setsVigenteHasta_callsUpsertAll_refreshesBlock_schedulesSync() = runTest(testDispatcher) {
        val existing = CostoProductoEntity(
            id = "costo-1",
            opticaId = opticaId,
            material = "CR39",
            tipoLente = "Monofocal",
            stockOFabricacion = "stock",
            costoUnitario = 150.0,
            vigenteDesde = "2026-01-01",
            vigenteHasta = null,
        )

        coEvery { costoProductoDao.getByBloque(opticaId, "stock") } returns flowOf(listOf(existing))

        viewModel = buildViewModel()

        viewModel.loadBlock("Stock Monofocal")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.costosDelBloque.size)

        viewModel.confirmDeleteCosto(existing)
        assertNotNull("deletingCosto must be set after confirmDeleteCosto", viewModel.uiState.value.deletingCosto)

        viewModel.deleteCosto()
        advanceUntilIdle()

        coVerify {
            costoProductoDao.upsertAll(
                withArg { entities ->
                    assertEquals(1, entities.size)
                    val deleted = entities[0]
                    assertEquals("costo-1", deleted.id)
                    assertEquals("2026-07-16", deleted.vigenteHasta)
                    assertNotNull(deleted.updatedAt)
                },
            )
        }

        coVerify(atLeast = 1) { costoProductoDao.getByBloque(opticaId, "stock") }

        coVerify { syncStateTracker.markLocallyModified(opticaId, "costo_producto", "costo-1") }
        coVerify { scheduler.scheduleFinanzasSync(opticaId) }

        assertNull("deletingCosto must be cleared after delete", viewModel.uiState.value.deletingCosto)
    }

    @Test
    fun deleteCosto_whenDaoFails_setsCostoSaveError() = runTest(testDispatcher) {
        val existing = CostoProductoEntity(
            id = "costo-2",
            opticaId = opticaId,
            material = "Policarbonato",
            tipoLente = "Progresivo",
            stockOFabricacion = "fabricacion",
            costoUnitario = 250.0,
            vigenteDesde = "2026-01-01",
            vigenteHasta = null,
        )

        coEvery { costoProductoDao.getByBloque(opticaId, "fabricacion") } returns flowOf(listOf(existing))
        coEvery { costoProductoDao.upsertAll(any()) } throws IOException("DB error")

        viewModel = buildViewModel()

        viewModel.loadBlock("Fabricación Resina")
        advanceUntilIdle()

        viewModel.confirmDeleteCosto(existing)
        viewModel.deleteCosto()
        advanceUntilIdle()

        val error = viewModel.uiState.value.costoSaveError
        assertNotNull("Error must be set after delete failure", error)
        assertTrue("Error must mention the problem", error!!.contains("eliminar", ignoreCase = true))
        coVerify(exactly = 0) { syncStateTracker.markLocallyModified(any(), any(), any()) }
    }
}
