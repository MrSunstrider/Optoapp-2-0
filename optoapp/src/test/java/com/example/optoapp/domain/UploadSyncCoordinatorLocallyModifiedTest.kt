package com.example.optoapp.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.costobiselado.CostoBiseladoEntity
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
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

// Real-Room DAO test: in-memory Room needs Robolectric for its Android context, the AGENTS.md exception.
@RunWith(RobolectricTestRunner::class)
class UploadSyncCoordinatorLocallyModifiedTest {

    private val opticaId = "o1"
    private val t1 = "2026-07-01T10:00:00Z"
    private val t2 = "2026-07-10T10:00:00Z"
    private val t3 = "2026-07-20T10:00:00Z"
    private lateinit var db: OptoDatabase
    private lateinit var tracker: SyncStateTracker
    private val repository = mockk<OptoRepository>(relaxed = true)
    private val networkRetryHelper = mockk<NetworkRetryHelper>()
    private val remoteTimestamps = mutableMapOf<String, String?>()
    private var remoteFetchError: Exception? = null
    private var remoteFetchCalls = 0
    private val uploadedProductos = mutableListOf<CostoProductoRemoto>()
    private val uploadedBiselado = mutableListOf<CostoBiseladoRemoto>()
    private lateinit var upload: UploadSyncCoordinator
    private lateinit var download: DownloadSyncCoordinator

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tracker = SyncStateTracker(db.syncEntityStateDao(), db)
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } coAnswers { secondArg<suspend () -> Unit>().invoke() }
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        upload = object : UploadSyncCoordinator(
            repository = repository,
            supabase = mockk<SupabaseClient>(relaxed = true),
            database = db,
            syncStateTracker = tracker,
            mergeHandler = mockk(relaxed = true),
            networkRetryHelper = networkRetryHelper,
            costoProductoDao = db.costoProductoDao(),
            costoBiseladoDao = db.costoBiseladoDao(),
            costoLcDao = db.costoLcDao(),
        ) {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()

            override suspend fun selectRemoteUpdatedAtChunk(tableName: String, opticaId: String, ids: List<String>): List<RemoteTimestamp> {
                remoteFetchCalls++
                remoteFetchError?.let { throw it }
                return ids.filter { it in remoteTimestamps }.map { RemoteTimestamp(it, remoteTimestamps[it]) }
            }

            override suspend fun upsertCostosProductosChunk(chunk: List<CostoProductoRemoto>) {
                uploadedProductos += chunk
            }

            override suspend fun upsertCostosBiseladoChunk(chunk: List<CostoBiseladoRemoto>) {
                uploadedBiselado += chunk
            }
        }
        download = DownloadSyncCoordinator(
            repository = repository,
            supabase = mockk<SupabaseClient>(relaxed = true),
            syncStateTracker = tracker,
            deletionSyncHelper = mockk<DeletionSyncHelper>(relaxed = true) {
                coEvery { deletedIds(any()) } returns emptySet()
            },
            networkRetryHelper = networkRetryHelper,
            resumenDiarioDao = mockk<ResumenDiarioDao>(relaxed = true),
            configuracionFinancieraDao = mockk<ConfiguracionFinancieraDao>(relaxed = true),
            costoProductoDao = db.costoProductoDao(),
            costoBiseladoDao = db.costoBiseladoDao(),
            costoLcDao = db.costoLcDao(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun producto(id: String, costo: Double, hasta: String? = null, updatedAt: String? = t1) = CostoProductoEntity(
        id = id,
        opticaId = opticaId,
        material = "Resina",
        tipoLente = "Monofocal",
        stockOFabricacion = "stock",
        costoUnitario = costo,
        vigenteDesde = "2026-01-01",
        vigenteHasta = hasta,
        updatedAt = updatedAt,
    )

    private fun biselado(id: String, costo: Double, hasta: String? = null, updatedAt: String? = t1) = CostoBiseladoEntity(
        id = id,
        opticaId = opticaId,
        material = "Resina",
        tipoAro = "completo",
        stockOFabricacion = "stock",
        costoPorPar = costo,
        vigenteDesde = "2026-01-01",
        vigenteHasta = hasta,
        updatedAt = updatedAt,
    )

    private suspend fun seedSyncedProducto(entity: CostoProductoEntity) {
        db.costoProductoDao().upsertAll(listOf(entity))
        tracker.markSynced(opticaId, "costo_producto", entity.id)
    }

    private suspend fun seedSyncedBiselado(entity: CostoBiseladoEntity) {
        db.costoBiseladoDao().upsertAll(listOf(entity))
        tracker.markSynced(opticaId, "costo_biselado", entity.id)
    }

    private suspend fun seedModifiedProducto(entity: CostoProductoEntity) {
        db.costoProductoDao().upsertAll(listOf(entity))
        tracker.markLocallyModified(opticaId, "costo_producto", entity.id)
    }

    @Test
    fun `a stale device does not reopen a closed version or overwrite a newer remote price`() = runTest {
        seedSyncedProducto(producto("P", 18.0))
        seedSyncedBiselado(biselado("B", 10.0))

        assertEquals(0, upload.uploadCostosProductos(opticaId))
        assertEquals(0, upload.uploadCostosBiselado(opticaId))
        assertTrue(uploadedProductos.isEmpty())
        assertTrue(uploadedBiselado.isEmpty())

        download.persistCostosProductos(
            opticaId,
            listOf(
                producto("P", 18.0, hasta = "2026-07-09", updatedAt = t2).toRemoto(),
                producto("Q", 20.0, updatedAt = t2).toRemoto(),
            ),
        )
        assertEquals("2026-07-09", db.costoProductoDao().getById("P")?.vigenteHasta)
        assertEquals(20.0, db.costoProductoDao().getById("Q")!!.costoUnitario, 0.0)
    }

    @Test
    fun `unmodified downloaded rows send no request`() = runTest {
        seedSyncedProducto(producto("P", 18.0))

        upload.uploadCostosProductos(opticaId)

        assertEquals(0, remoteFetchCalls)
        assertTrue(uploadedProductos.isEmpty())
        assertTrue(tracker.isSynced(opticaId, "upload_costos_productos", "batch"))
    }

    @Test
    fun `closing P and inserting Q uploads both rows and marks them synced`() = runTest {
        seedSyncedProducto(producto("P", 18.0))
        seedModifiedProducto(producto("P", 18.0, hasta = "2026-07-19", updatedAt = t3))
        seedModifiedProducto(producto("Q", 20.0, updatedAt = t3))
        remoteTimestamps["P"] = t1

        assertEquals(2, upload.uploadCostosProductos(opticaId))

        assertEquals(setOf("P", "Q"), uploadedProductos.map { it.id }.toSet())
        assertEquals("2026-07-19", uploadedProductos.single { it.id == "P" }.vigenteHasta)
        assertEquals(setOf("P", "Q"), tracker.syncedIds(opticaId, "costo_producto"))
    }

    @Test
    fun `a modified biselado row is uploaded and marked synced`() = runTest {
        db.costoBiseladoDao().upsertAll(listOf(biselado("B", 12.0, updatedAt = t3)))
        tracker.markLocallyModified(opticaId, "costo_biselado", "B")
        seedSyncedBiselado(biselado("C", 9.0))

        assertEquals(1, upload.uploadCostosBiselado(opticaId))

        assertEquals(listOf("B"), uploadedBiselado.map { it.id })
        assertEquals(setOf("B", "C"), tracker.syncedIds(opticaId, "costo_biselado"))
    }

    @Test
    fun `a modified row older than the remote copy is not uploaded and the download replaces it`() = runTest {
        seedModifiedProducto(producto("P", 19.0, updatedAt = t2))
        remoteTimestamps["P"] = t3

        assertEquals(0, upload.uploadCostosProductos(opticaId))
        assertTrue(uploadedProductos.isEmpty())
        assertFalse(tracker.isSynced(opticaId, "costo_producto", "P"))

        download.persistCostosProductos(opticaId, listOf(producto("P", 21.0, updatedAt = t3).toRemoto()))

        assertEquals(21.0, db.costoProductoDao().getById("P")!!.costoUnitario, 0.0)
        assertTrue(tracker.isSynced(opticaId, "costo_producto", "P"))
    }

    @Test
    fun `a modified row newer than the remote copy is uploaded`() = runTest {
        seedModifiedProducto(producto("P", 19.0, updatedAt = t3))
        remoteTimestamps["P"] = t2

        assertEquals(1, upload.uploadCostosProductos(opticaId))

        assertEquals(listOf("P"), uploadedProductos.map { it.id })
    }

    @Test
    fun `a remote timestamp fetch failure fails the table and keeps rows modified`() = runTest {
        seedModifiedProducto(producto("P", 19.0, updatedAt = t3))
        remoteFetchError = IOException("timeout")

        val error = runCatching { upload.uploadCostosProductos(opticaId) }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(uploadedProductos.isEmpty())
        assertFalse(tracker.isSynced(opticaId, "costo_producto", "P"))
        assertFalse(tracker.isSynced(opticaId, "upload_costos_productos", "batch"))
    }

    @Test
    fun `a legacy row without sync state is uploaded with a non-null updated_at`() = runTest {
        db.costoProductoDao().upsertAll(listOf(producto("L", 15.0, updatedAt = null)))

        assertEquals(1, upload.uploadCostosProductos(opticaId))

        val sent = uploadedProductos.single()
        assertEquals("L", sent.id)
        assertNotNull(sent.updatedAt)
        assertTrue(sent.updatedAt!!.isNotBlank())
    }

    @Test
    fun `remote timestamps are fetched in chunks of 80 ids`() = runTest {
        repeat(81) { seedModifiedProducto(producto("N$it", 10.0, updatedAt = t3)) }

        assertEquals(81, upload.uploadCostosProductos(opticaId))

        assertEquals(2, remoteFetchCalls)
    }
}
