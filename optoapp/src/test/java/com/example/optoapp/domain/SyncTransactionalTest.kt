package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests for REQ-SYNC-003: Transactional markSynced on Upload.
 */
class SyncTransactionalTest {

    private val repository = mockk<OptoRepository>(relaxed = true)
    private val supabase = mockk<SupabaseClient>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val mergeHandler = mockk<DispensacionMergeHandler>(relaxed = true)
    private val networkRetryHelper = mockk<NetworkRetryHelper>(relaxed = true)
    private val costoProductoDao = mockk<CostoProductoDao>(relaxed = true)
    private val costoBiseladoDao = mockk<CostoBiseladoDao>(relaxed = true)
    private val configuracionFinancieraDao = mockk<ConfiguracionFinancieraDao>(relaxed = true)
    private lateinit var coordinator: UploadSyncCoordinator

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        // Create a test coordinator that runs transactions inline without a real DB
        val database = mockk<OptoDatabase>(relaxed = true)
        coordinator = object : UploadSyncCoordinator(
            repository = repository,
            supabase = supabase,
            database = database,
            syncStateTracker = syncStateTracker,
            mergeHandler = mergeHandler,
            networkRetryHelper = networkRetryHelper,
            costoProductoDao = costoProductoDao,
            costoBiseladoDao = costoBiseladoDao,
            configuracionFinancieraDao = configuracionFinancieraDao,
        ) {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `success path invokes per-entity markSynced and batch-level markSynced`() = runBlocking {
        val item = DispensacionItem(
            id = "item1", dispensacionId = "d1", opticaId = "optica-test",
        )
        coEvery { repository.getDispensacionItemsSnapshotForOptica("optica-test") } returns listOf(item)
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit

        coordinator.uploadDispensacionItems("optica-test")

        coVerify(exactly = 1) {
            syncStateTracker.markSynced("optica-test", "dispensacion_item", "item1")
        }
        coVerify(exactly = 1) {
            syncStateTracker.markSynced("optica-test", "upload_dispensacion_items", "batch")
        }
    }

    @Test
    fun `per-entity markSynced failure prevents batch-level markSynced`() = runBlocking {
        val item = DispensacionItem(
            id = "item1", dispensacionId = "d1", opticaId = "optica-test",
        )
        coEvery { repository.getDispensacionItemsSnapshotForOptica("optica-test") } returns listOf(item)
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit
        coEvery {
            syncStateTracker.markSynced("optica-test", "dispensacion_item", "item1")
        } throws RuntimeException("Simulated markSynced failure")

        var exceptionCaught = false
        try {
            coordinator.uploadDispensacionItems("optica-test")
        } catch (_: RuntimeException) {
            exceptionCaught = true
        }

        assertTrue("Exception must propagate when markSynced fails", exceptionCaught)
        coVerify(exactly = 0) {
            syncStateTracker.markSynced("optica-test", "upload_dispensacion_items", "batch")
        }
    }

    @Test
    fun `empty rows marks batch synced immediately without calling database`() = runBlocking {
        coEvery { repository.getDispensacionItemsSnapshotForOptica("optica-test") } returns emptyList()

        val result = coordinator.uploadDispensacionItems("optica-test")

        assertEquals(0, result)
        coVerify(exactly = 1) {
            syncStateTracker.markSynced("optica-test", "upload_dispensacion_items", "batch")
        }
        coVerify(exactly = 0) {
            syncStateTracker.markSynced(any(), "dispensacion_item", any())
        }
    }
}
