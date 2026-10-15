package com.example.optoapp.domain

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.data.regaloservicio.RegaloServicioExtraEntity
import com.example.optoapp.data.servicio.ServicioExtraItem
import com.example.optoapp.util.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate

class UploadSyncCoordinatorChildBatchesTest {

    private val opticaId = "optica-upload"

    private val repository = mockk<OptoRepository>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val networkRetryHelper = mockk<NetworkRetryHelper>()

    private lateinit var coordinator: UploadSyncCoordinator

    private val gasto = GastoOperativoEntity(
        id = "g-1",
        opticaId = opticaId,
        categoria = "marketing",
        monto = BigDecimal("120.00"),
        fecha = LocalDate.of(2026, 7, 2),
        tipoGasto = "variable",
    )
    private val item = ServicioExtraItem(id = "si-1", servicioExtraId = "s-1", monturaId = "m-1", opticaId = opticaId)
    private val regalo = RegaloServicioExtraEntity(
        id = "rs-1",
        servicioExtraId = "s-1",
        productoId = "p-1",
        cantidad = 2,
        costoUnitario = 5.0,
        descripcion = "Estuche",
        opticaId = opticaId,
    )

    @Before
    fun setUp() {
        mockkObject(AppLogger)
        every { AppLogger.e(any(), any(), any()) } returns Unit
        coordinator = object : UploadSyncCoordinator(
            repository = repository,
            supabase = mockk<SupabaseClient>(relaxed = true),
            database = mockk(relaxed = true),
            syncStateTracker = syncStateTracker,
            mergeHandler = mockk(relaxed = true),
            networkRetryHelper = networkRetryHelper,
            costoProductoDao = mockk(relaxed = true),
            costoBiseladoDao = mockk(relaxed = true),
            costoLcDao = mockk(relaxed = true),
            servicioTipoDao = mockk(relaxed = true),
        ) {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun conFilas() {
        coEvery { repository.getGastosOperativosList(opticaId) } returns listOf(gasto, gasto)
        coEvery { repository.getServicioExtraItemsSnapshotForOptica(opticaId) } returns listOf(item)
        coEvery { repository.getRegalosServicioExtraSnapshotForOptica(opticaId) } returns listOf(regalo)
    }

    @Test
    fun nothingLocalCompletesEachBatchWithoutTouchingTheNetwork() = runTest {
        coEvery { repository.getGastosOperativosList(opticaId) } returns emptyList()
        coEvery { repository.getServicioExtraItemsSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { repository.getRegalosServicioExtraSnapshotForOptica(opticaId) } returns emptyList()

        assertEquals(0, coordinator.uploadGastosOperativos(opticaId))
        assertEquals(0, coordinator.uploadServicioExtraItems(opticaId))
        assertEquals(0, coordinator.uploadRegalosServicioExtra(opticaId))

        coVerify { syncStateTracker.markSynced(opticaId, "upload_gastos_operativos", "batch") }
        coVerify { syncStateTracker.markSynced(opticaId, "upload_servicio_extra_items", "batch") }
        coVerify { syncStateTracker.markSynced(opticaId, "upload_regalos_servicio_extra", "batch") }
        coVerify(exactly = 0) { networkRetryHelper.retryNetwork(any(), any()) }
    }

    @Test
    fun uploadedRowsAreMarkedSyncedSoTheResumenFlushCanTrustThem() = runTest {
        conFilas()
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit

        assertEquals(1, coordinator.uploadGastosOperativos(opticaId))
        assertEquals(1, coordinator.uploadServicioExtraItems(opticaId))
        assertEquals(1, coordinator.uploadRegalosServicioExtra(opticaId))

        coVerify(exactly = 1) { syncStateTracker.markSynced(opticaId, "gasto_operativo", "g-1") }
        coVerify { syncStateTracker.markSynced(opticaId, "servicio_extra_item", "si-1") }
        coVerify { syncStateTracker.markSynced(opticaId, "regalo_servicio_extra", "rs-1") }
        coVerify { syncStateTracker.markSynced(opticaId, "upload_servicio_extra_items", "batch") }
        coVerify { syncStateTracker.markSynced(opticaId, "upload_regalos_servicio_extra", "batch") }
    }

    @Test
    fun aNetworkFailureLeavesTheBatchInErrorAndReportsThePartialCount() = runTest {
        conFilas()
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } throws IOException("offline")

        listOf(
            "upload_gastos_operativos" to suspend { coordinator.uploadGastosOperativos(opticaId) },
            "upload_servicio_extra_items" to suspend { coordinator.uploadServicioExtraItems(opticaId) },
            "upload_regalos_servicio_extra" to suspend { coordinator.uploadRegalosServicioExtra(opticaId) },
        ).forEach { (lote, subir) ->
            val error = runCatching { subir() }.exceptionOrNull()

            assertEquals(lote, UploadPartialException::class, error?.let { it::class })
            assertEquals(lote, 0, (error as UploadPartialException).uploadedCount)
            coVerify { syncStateTracker.markError(opticaId, lote, "batch", "offline") }
            coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, lote, "batch") }
        }
    }

    @Test
    fun anUnexpectedFailureIsRecordedAndRethrown() = runTest {
        conFilas()
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } throws IllegalStateException("check violation")

        val error = runCatching { coordinator.uploadServicioExtraItems(opticaId) }.exceptionOrNull()

        assertEquals("check violation", error?.message)
        coVerify { syncStateTracker.markError(opticaId, "upload_servicio_extra_items", "batch", "check violation") }
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "servicio_extra_item", any()) }
    }

    @Test
    fun aBlankOpticaIsRefusedBeforeAnyUpload() = runTest {
        coEvery { repository.getGastosOperativosList("") } returns listOf(gasto)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { coordinator.uploadGastosOperativos("") }
        }
        coVerify(exactly = 0) { networkRetryHelper.retryNetwork(any(), any()) }
    }
}
