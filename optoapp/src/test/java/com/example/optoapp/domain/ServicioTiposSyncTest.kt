package com.example.optoapp.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import com.example.optoapp.data.servicio.ServicioExtraItem
import com.example.optoapp.data.serviciotipo.ServicioTipoEntity
import com.example.optoapp.domain.sync.RemoteTimestamp
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.LocalDate

// Robolectric because Room.inMemoryDatabaseBuilder needs an Android Context (AGENTS.md real-Room exception).
@RunWith(RobolectricTestRunner::class)
class ServicioTiposSyncTest {

    private val opticaId = "o1"
    private val older = "2026-07-01T10:00:00Z"
    private val newer = "2026-07-20T10:00:00Z"
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var db: OptoDatabase
    private lateinit var tracker: SyncStateTracker
    private val networkRetryHelper = mockk<NetworkRetryHelper>()
    private val remoteTimestamps = mutableMapOf<String, String?>()
    private var remoteFetchError: Exception? = null
    private var remoteFetchCalls = 0
    private val uploaded = mutableListOf<ServicioTipoRemota>()
    private lateinit var upload: UploadSyncCoordinator
    private lateinit var download: DownloadSyncCoordinator

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
                assertEquals("servicio_tipos", tableName)
                remoteFetchCalls++
                remoteFetchError?.let { throw it }
                return ids.filter { it in remoteTimestamps }.map { RemoteTimestamp(it, remoteTimestamps[it]) }
            }

            override suspend fun upsertServicioTiposChunk(chunk: List<ServicioTipoRemota>) {
                uploaded += chunk
            }
        }
        val repository = mockk<OptoRepository>(relaxed = true)
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        download = DownloadSyncCoordinator(
            repository = repository,
            supabase = mockk<SupabaseClient>(relaxed = true),
            syncStateTracker = tracker,
            deletionSyncHelper = mockk<DeletionSyncHelper>(relaxed = true),
            networkRetryHelper = mockk<NetworkRetryHelper>(relaxed = true),
            resumenDiarioDao = mockk<ResumenDiarioDao>(relaxed = true),
            configuracionFinancieraDao = db.configuracionFinancieraDao(),
            costoProductoDao = db.costoProductoDao(),
            costoBiseladoDao = db.costoBiseladoDao(),
            costoLcDao = db.costoLcDao(),
            servicioTipoDao = db.servicioTipoDao(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun tipo(id: String, costoTercero: Double = 12.0, activo: Boolean = true, updatedAt: String? = older) =
        ServicioTipoEntity(
            id = id,
            opticaId = opticaId,
            nombre = "Grabado",
            costoPropio = 5.0,
            costoTercero = costoTercero,
            activo = activo,
            updatedAt = updatedAt,
            updatedBy = "u1",
        )

    private suspend fun seed(entity: ServicioTipoEntity, synced: Boolean) {
        db.servicioTipoDao().upsert(entity)
        if (synced) {
            tracker.markSynced(opticaId, "servicio_tipo", entity.id)
        } else {
            tracker.markLocallyModified(opticaId, "servicio_tipo", entity.id)
        }
    }

    @Test
    fun `upload sends servicio_tipos before servicios_extra and download stores them before servicios`() = runTest {
        val uploadCoordinator = mockk<UploadSyncCoordinator>(relaxed = true)
        val downloadCoordinator = mockk<DownloadSyncCoordinator>(relaxed = true)
        coEvery { uploadCoordinator.uploadServicioTipos(any()) } returns 2
        coEvery { downloadCoordinator.downloadServicioTipos(any()) } returns 3
        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = mockk(relaxed = true),
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            discardLosingClaim = mockk(relaxed = true),
            resumenRecalcCoordinator = mockk(relaxed = true),
            syncSeguimientoUseCase = mockk(relaxed = true),
        )

        val result = useCase(opticaId) as com.example.optoapp.data.Resource.Success

        coVerifyOrder {
            uploadCoordinator.uploadDispensacionItems(opticaId)
            uploadCoordinator.uploadServicioTipos(opticaId)
            uploadCoordinator.uploadServicios(opticaId)
            downloadCoordinator.downloadDispensacionItems(opticaId)
            downloadCoordinator.downloadServicioTipos(opticaId)
            downloadCoordinator.downloadServicios(opticaId)
        }
        assertEquals(2, result.data!!.uploadedServicioTipos)
        assertEquals(3, result.data!!.downloadedServicioTipos)
    }

    @Test
    fun `a stale device whose catalog was only downloaded sends no servicio_tipos request`() = runTest {
        seed(tipo("T", costoTercero = 12.0), synced = true)

        assertEquals(0, upload.uploadServicioTipos(opticaId))

        assertEquals(0, remoteFetchCalls)
        assertTrue(uploaded.isEmpty())
        assertTrue(tracker.isSynced(opticaId, "upload_servicio_tipos", "batch"))
    }

    @Test
    fun `a locally deactivated type uploads activo false with every column and is marked synced`() = runTest {
        seed(tipo("T", activo = false, updatedAt = newer), synced = false)
        remoteTimestamps["T"] = older

        assertEquals(1, upload.uploadServicioTipos(opticaId))

        assertEquals(
            ServicioTipoRemota(
                id = "T",
                opticaId = opticaId,
                nombre = "Grabado",
                costoPropio = 5.0,
                costoTercero = 12.0,
                activo = false,
                updatedAt = newer,
                updatedBy = "u1",
            ),
            uploaded.single(),
        )
        assertEquals(setOf("T"), tracker.syncedIds(opticaId, "servicio_tipo"))
    }

    @Test
    fun `a pending type edit older than the remote copy is not uploaded`() = runTest {
        seed(tipo("T", costoTercero = 14.0, updatedAt = older), synced = false)
        remoteTimestamps["T"] = newer

        assertEquals(0, upload.uploadServicioTipos(opticaId))

        assertTrue(uploaded.isEmpty())
        assertFalse(tracker.isSynced(opticaId, "servicio_tipo", "T"))
    }

    @Test
    fun `a remote timestamp failure fails the servicio_tipos upload and keeps the row pending`() = runTest {
        seed(tipo("T", updatedAt = newer), synced = false)
        remoteFetchError = IOException("timeout")

        val error = runCatching { upload.uploadServicioTipos(opticaId) }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(uploaded.isEmpty())
        assertFalse(tracker.isSynced(opticaId, "servicio_tipo", "T"))
    }

    @Test
    fun `a type deactivated on another device is downloaded as inactive and marked synced`() = runTest {
        seed(tipo("T", activo = true, updatedAt = older), synced = true)

        assertEquals(1, download.persistServicioTipos(opticaId, listOf(tipo("T", activo = false, updatedAt = newer).toRemoto())))

        assertFalse(db.servicioTipoDao().getById("T", opticaId)!!.activo)
        assertTrue(tracker.isSynced(opticaId, "servicio_tipo", "T"))
    }

    @Test
    fun `a pending local type edit newer than the remote copy survives the download`() = runTest {
        seed(tipo("T", costoTercero = 15.0, updatedAt = newer), synced = false)

        download.persistServicioTipos(opticaId, listOf(tipo("T", costoTercero = 12.0, updatedAt = older).toRemoto()))

        assertEquals(15.0, db.servicioTipoDao().getById("T", opticaId)!!.costoTercero, 0.0)
        assertFalse(tracker.isSynced(opticaId, "servicio_tipo", "T"))
    }

    @Test
    fun `servicio executor and cost fields survive an upload and download round trip`() {
        val local = ServicioExtra(
            id = "s1",
            descripcion = "Grabado",
            montoTotal = 40.0,
            estado = "Pendiente",
            fecha = LocalDate.of(2026, 7, 10),
            opticaId = opticaId,
            servicioTipoId = "T",
            realizadoPor = "tercero",
            realizadoPorNombre = "Optica Lima",
            costoReal = 12.0,
        )

        val payload = json.encodeToString(ServicioRemoto.serializer(), local.toRemoto())
        val roundTrip = json.decodeFromString(ServicioRemoto.serializer(), payload).toEntity()

        assertTrue(payload.contains("\"servicio_tipo_id\":\"T\""))
        assertTrue(payload.contains("\"realizado_por\":\"tercero\""))
        assertTrue(payload.contains("\"realizado_por_nombre\":\"Optica Lima\""))
        assertTrue(payload.contains("\"costo_real\":12.0"))
        assertEquals("T", roundTrip.servicioTipoId)
        assertEquals("tercero", roundTrip.realizadoPor)
        assertEquals("Optica Lima", roundTrip.realizadoPorNombre)
        assertEquals(12.0, roundTrip.costoReal, 0.0)
    }

    @Test
    fun `servicio item frame cost survives an upload and download round trip`() {
        val item = ServicioExtraItem(
            id = "i1",
            servicioExtraId = "s1",
            monturaId = "m1",
            descripcion = "Cofre",
            monto = 20.0,
            opticaId = opticaId,
            costoReal = 30.0,
        )

        val payload = json.encodeToString(ServicioExtraItemRemota.serializer(), item.toRemoto())

        assertTrue(payload.contains("\"costo_real\":30.0"))
        assertEquals(item, json.decodeFromString(ServicioExtraItemRemota.serializer(), payload).toEntity())
    }

    @Test
    fun `a remote servicio and item from an older client map with defaults`() {
        val servicio = json.decodeFromString(
            ServicioRemoto.serializer(),
            """{"id":"s1","descripcion":"Ajuste","monto_total":20.0,"estado":"Pendiente","fecha":"2026-07-10","optica_id":"o1"}""",
        ).toEntity()
        val item = json.decodeFromString(
            ServicioExtraItemRemota.serializer(),
            """{"id":"i1","servicio_extra_id":"s1","descripcion":"Ajuste","monto":20.0,"optica_id":"o1"}""",
        ).toEntity()

        assertNull(servicio.servicioTipoId)
        assertNull(servicio.realizadoPor)
        assertNull(servicio.realizadoPorNombre)
        assertEquals(0.0, servicio.costoReal, 0.0)
        assertEquals(0.0, item.costoReal, 0.0)
    }

    @Test
    fun `remote servicio_tipos payload maps every column`() {
        val remote = json.decodeFromString(
            ServicioTipoRemota.serializer(),
            """{"id":"T","optica_id":"o1","nombre":"Grabado","costo_propio":5.0,"costo_tercero":12.0,""" +
                """"activo":false,"updated_at":"$newer","updated_by":"u1","created_at":"$older"}""",
        )

        assertEquals(tipo("T", activo = false, updatedAt = newer), remote.toEntity())
        assertEquals(remote, remote.toEntity().toRemoto())
    }
}
