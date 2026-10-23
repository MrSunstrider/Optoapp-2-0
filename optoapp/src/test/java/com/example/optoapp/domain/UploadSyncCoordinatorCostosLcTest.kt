package com.example.optoapp.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.costolc.CostoLcEntity
import com.example.optoapp.domain.sync.RemoteTimestamp
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

// Robolectric because Room.inMemoryDatabaseBuilder needs an Android Context (AGENTS.md real-Room exception).
@RunWith(RobolectricTestRunner::class)
class UploadSyncCoordinatorCostosLcTest {

    private val opticaId = "o1"
    private val t1 = "2026-07-01T10:00:00Z"
    private val t2 = "2026-07-10T10:00:00Z"
    private val t3 = "2026-07-20T10:00:00Z"
    private lateinit var db: OptoDatabase
    private lateinit var tracker: SyncStateTracker
    private val networkRetryHelper = mockk<NetworkRetryHelper>()
    private val remoteTimestamps = mutableMapOf<String, String?>()
    private var remoteFetchError: Exception? = null
    private var remoteFetchCalls = 0
    private val uploaded = mutableListOf<CostoLcRemoto>()
    private lateinit var upload: UploadSyncCoordinator

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tracker = SyncStateTracker(db.syncEntityStateDao(), db)
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } coAnswers { secondArg<suspend () -> Unit>().invoke() }
        upload = object : UploadSyncCoordinator(
            repository = mockk<OptoRepository>(relaxed = true),
            supabase = mockk<SupabaseClient>(relaxed = true),
            database = db,
            syncStateTracker = tracker,
            mergeHandler = mockk(relaxed = true),
            networkRetryHelper = networkRetryHelper,
            costoProductoDao = db.costoProductoDao(),
            costoBiseladoDao = db.costoBiseladoDao(),
            costoLcDao = db.costoLcDao(),
            servicioTipoDao = db.servicioTipoDao(),
            configuracionFinancieraDao = db.configuracionFinancieraDao(),
        ) {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()

            override suspend fun selectRemoteUpdatedAtChunk(tableName: String, opticaId: String, ids: List<String>): List<RemoteTimestamp> {
                assertEquals("costos_lc", tableName)
                remoteFetchCalls++
                remoteFetchError?.let { throw it }
                return ids.filter { it in remoteTimestamps }.map { RemoteTimestamp(it, remoteTimestamps[it]) }
            }

            override suspend fun upsertCostosLcChunk(chunk: List<CostoLcRemoto>) {
                uploaded += chunk
            }
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun lc(id: String, costo: Double, hasta: String? = null, updatedAt: String? = t1) = CostoLcEntity(
        id = id,
        opticaId = opticaId,
        tipoLc = "graduado",
        materialLc = "Hydrogel",
        modalidad = "mensual",
        radioBase = "8.6",
        diametro = "14.2",
        laboratorioId = "lab1",
        costoUnitario = costo,
        vigenteDesde = "2026-01-01",
        vigenteHasta = hasta,
        updatedAt = updatedAt,
    )

    private suspend fun seed(entity: CostoLcEntity, synced: Boolean) {
        db.costoLcDao().upsertAll(listOf(entity))
        if (synced) {
            tracker.markSynced(opticaId, "costo_lc", entity.id)
        } else {
            tracker.markLocallyModified(opticaId, "costo_lc", entity.id)
        }
    }

    @Test
    fun `a stale device with only downloaded LC rows sends no request`() = runTest {
        seed(lc("P", 45.0), synced = true)

        assertEquals(0, upload.uploadCostosLc(opticaId))

        assertEquals(0, remoteFetchCalls)
        assertTrue(uploaded.isEmpty())
        assertTrue(tracker.isSynced(opticaId, "upload_costos_lc", "batch"))
    }

    @Test
    fun `a closed and a new LC version upload with every column and are marked synced`() = runTest {
        seed(lc("P", 45.0, hasta = "2026-07-19", updatedAt = t3), synced = false)
        seed(lc("Q", 50.0, updatedAt = t3), synced = false)
        remoteTimestamps["P"] = t1

        assertEquals(2, upload.uploadCostosLc(opticaId))

        val closed = uploaded.single { it.id == "P" }
        assertEquals(
            CostoLcRemoto(
                id = "P",
                opticaId = opticaId,
                tipoLc = "graduado",
                materialLc = "Hydrogel",
                modalidad = "mensual",
                radioBase = "8.6",
                diametro = "14.2",
                laboratorioId = "lab1",
                costoUnitario = 45.0,
                vigenteDesde = "2026-01-01",
                vigenteHasta = "2026-07-19",
                updatedAt = t3,
            ),
            closed,
        )
        assertEquals(setOf("P", "Q"), tracker.syncedIds(opticaId, "costo_lc"))
    }

    @Test
    fun `a pending LC edit older than the remote copy is not uploaded`() = runTest {
        seed(lc("P", 47.0, updatedAt = t2), synced = false)
        remoteTimestamps["P"] = t3

        assertEquals(0, upload.uploadCostosLc(opticaId))

        assertTrue(uploaded.isEmpty())
        assertFalse(tracker.isSynced(opticaId, "costo_lc", "P"))
    }

    @Test
    fun `a legacy LC row without sync state uploads with a non-null updated_at`() = runTest {
        db.costoLcDao().upsertAll(listOf(lc("L", 40.0, updatedAt = null)))

        assertEquals(1, upload.uploadCostosLc(opticaId))

        assertNotNull(uploaded.single().updatedAt)
    }

    @Test
    fun `a remote timestamp failure fails the LC table and keeps rows pending`() = runTest {
        seed(lc("P", 47.0, updatedAt = t3), synced = false)
        remoteFetchError = IOException("timeout")

        val error = runCatching { upload.uploadCostosLc(opticaId) }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(uploaded.isEmpty())
        assertFalse(tracker.isSynced(opticaId, "costo_lc", "P"))
    }
}
