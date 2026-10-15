package com.example.optoapp.domain

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costolc.CostoLcDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import com.example.optoapp.data.serviciotipo.ServicioTipoDao
import com.example.optoapp.util.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class DownloadSyncCoordinatorFetchTest {

    private val opticaId = "optica-fetch"

    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val networkRetryHelper = mockk<NetworkRetryHelper>()
    private val repository = mockk<OptoRepository>(relaxed = true)
    private val resumenDiarioDao = mockk<ResumenDiarioDao>(relaxed = true)
    private val configuracionFinancieraDao = mockk<ConfiguracionFinancieraDao>(relaxed = true)

    private lateinit var coordinator: DownloadSyncCoordinator

    private val descargas: List<Pair<String, suspend (String) -> Int>> by lazy {
        listOf(
            "costo_producto" to coordinator::downloadCostosProductos,
            "costo_biselado" to coordinator::downloadCostosBiselado,
            "costo_lc" to coordinator::downloadCostosLc,
            "servicio_tipo" to coordinator::downloadServicioTipos,
            "gasto_operativo" to coordinator::downloadGastosOperativos,
            "servicio_extra" to coordinator::downloadServicios,
            "servicio_extra_item" to coordinator::downloadServicioExtraItems,
            "regalo_servicio_extra" to coordinator::downloadRegalosServicioExtra,
            "dispensacion" to coordinator::downloadDispensaciones,
            "dispensacion_item" to coordinator::downloadDispensacionItems,
            "regalo_dispensacion" to coordinator::downloadRegalos,
            "pago" to coordinator::downloadPagos,
        )
    }

    @Before
    fun setUp() {
        mockkObject(AppLogger)
        every { AppLogger.e(any(), any(), any()) } returns Unit
        every { AppLogger.w(any(), any(), any()) } returns Unit
        coordinator = DownloadSyncCoordinator(
            repository = repository,
            supabase = mockk<SupabaseClient>(relaxed = true),
            syncStateTracker = syncStateTracker,
            deletionSyncHelper = mockk(relaxed = true),
            networkRetryHelper = networkRetryHelper,
            resumenDiarioDao = resumenDiarioDao,
            configuracionFinancieraDao = configuracionFinancieraDao,
            costoProductoDao = mockk<CostoProductoDao>(relaxed = true),
            costoBiseladoDao = mockk<CostoBiseladoDao>(relaxed = true),
            costoLcDao = mockk<CostoLcDao>(relaxed = true),
            servicioTipoDao = mockk<ServicioTipoDao>(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun aNetworkFailureMarksTheTableBatchAsErrorAndPersistsNothing() = runTest {
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } throws IOException("offline")

        descargas.forEach { (entidad, descargar) ->
            clearMocks(syncStateTracker, answers = false)

            assertEquals(entidad, 0, descargar(opticaId))

            coVerify { syncStateTracker.markError(opticaId, "download_$entidad", "batch", "offline") }
            coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "download_$entidad", "batch") }
        }
        coVerify(exactly = 0) { repository.withTransaction(any<suspend () -> Any?>()) }
    }

    @Test
    fun anUnexpectedFetchFailureIsAlsoRecordedOnTheBatch() = runTest {
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } throws IllegalStateException("bad payload")

        descargas.forEach { (entidad, descargar) ->
            clearMocks(syncStateTracker, answers = false)

            assertEquals(entidad, 0, descargar(opticaId))

            coVerify { syncStateTracker.markError(opticaId, "download_$entidad", "batch", "bad payload") }
        }
    }

    @Test
    fun anEmptyRemoteTableCompletesTheBatchAndClearsStaleMarkers() = runTest {
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit

        descargas.forEach { (entidad, descargar) ->
            clearMocks(syncStateTracker, answers = false)

            assertEquals(entidad, 0, descargar(opticaId))

            coVerify { syncStateTracker.clear(opticaId, "download_$entidad", "batch") }
            coVerify { syncStateTracker.clear(opticaId, "download_${entidad}_blocked", "batch") }
            coVerify { syncStateTracker.markSynced(opticaId, "download_$entidad", "batch") }
            coVerify(exactly = 0) { syncStateTracker.markError(any(), any(), any(), any()) }
        }
    }

    @Test
    fun resumenAndConfigurationDownloadsNeverAbortTheSyncOnFailure() = runTest {
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } throws IOException("offline")

        assertEquals(0, coordinator.downloadResumenDiario(opticaId))
        assertEquals(0, coordinator.downloadConfiguracionFinanciera(opticaId))

        coVerify(exactly = 0) { resumenDiarioDao.upsert(any()) }
        coVerify(exactly = 0) { configuracionFinancieraDao.upsert(any()) }
    }

    @Test
    fun emptyResumenAndConfigurationDownloadsStoreNothing() = runTest {
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit

        assertEquals(0, coordinator.downloadResumenDiario(opticaId))
        assertEquals(0, coordinator.downloadConfiguracionFinanciera(opticaId))

        coVerify(exactly = 0) { resumenDiarioDao.upsert(any()) }
        coVerify(exactly = 0) { configuracionFinancieraDao.upsert(any()) }
    }
}
