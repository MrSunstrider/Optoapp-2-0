package com.example.optoapp.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.costolc.CostoLcEntity
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Real-Room DAO test: in-memory Room needs Robolectric for its Android context, the AGENTS.md exception.
@RunWith(RobolectricTestRunner::class)
class DownloadSyncCoordinatorCostosLcTest {

    private val opticaId = "o1"
    private val older = "2026-07-01T10:00:00Z"
    private val newer = "2026-07-20T10:00:00Z"
    private val deletedLocally = mutableSetOf<String>()
    private lateinit var db: OptoDatabase
    private lateinit var tracker: SyncStateTracker
    private lateinit var coordinator: DownloadSyncCoordinator

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tracker = SyncStateTracker(db.syncEntityStateDao(), db)
        val repository = mockk<OptoRepository>(relaxed = true)
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        coordinator = DownloadSyncCoordinator(
            repository = repository,
            supabase = mockk<SupabaseClient>(relaxed = true),
            syncStateTracker = tracker,
            deletionSyncHelper = mockk<DeletionSyncHelper>(relaxed = true) {
                coEvery { deletedIds(any()) } answers { deletedLocally }
            },
            networkRetryHelper = mockk<NetworkRetryHelper>(relaxed = true),
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

    private fun lc(id: String, costo: Double, updatedAt: String?) = CostoLcEntity(
        id = id,
        opticaId = opticaId,
        tipoLc = "graduado",
        materialLc = "Hydrogel",
        modalidad = "mensual",
        costoUnitario = costo,
        vigenteDesde = "2026-01-01",
        updatedAt = updatedAt,
    )

    @Test
    fun `a new remote LC row is stored and marked synced`() = runTest {
        assertEquals(1, coordinator.persistCostosLc(opticaId, listOf(lc("P", 45.0, newer).toRemoto())))

        assertEquals(45.0, db.costoLcDao().getById("P")!!.costoUnitario, 0.0)
        assertTrue(tracker.isSynced(opticaId, "costo_lc", "P"))
    }

    @Test
    fun `a newer remote LC row wins over a pending older local edit`() = runTest {
        db.costoLcDao().upsertAll(listOf(lc("P", 47.0, older)))
        tracker.markLocallyModified(opticaId, "costo_lc", "P")

        coordinator.persistCostosLc(opticaId, listOf(lc("P", 50.0, newer).toRemoto()))

        assertEquals(50.0, db.costoLcDao().getById("P")!!.costoUnitario, 0.0)
        assertTrue(tracker.isSynced(opticaId, "costo_lc", "P"))
    }

    @Test
    fun `a pending local LC edit newer than the remote copy is kept for upload`() = runTest {
        db.costoLcDao().upsertAll(listOf(lc("P", 47.0, newer)))
        tracker.markLocallyModified(opticaId, "costo_lc", "P")

        coordinator.persistCostosLc(opticaId, listOf(lc("P", 45.0, older).toRemoto()))

        assertEquals(47.0, db.costoLcDao().getById("P")!!.costoUnitario, 0.0)
        assertFalse(tracker.isSynced(opticaId, "costo_lc", "P"))
    }

    @Test
    fun `a locally deleted LC row is not downloaded again`() = runTest {
        deletedLocally += "D"

        coordinator.persistCostosLc(opticaId, listOf(lc("D", 45.0, newer).toRemoto()))

        assertNull(db.costoLcDao().getById("D"))
    }

    @Test
    fun `remote LC payload maps every column and an absent updated_at to null`() {
        val json = Json { ignoreUnknownKeys = true }
        val base = """"id":"P","optica_id":"o1","tipo_lc":"graduado","material_lc":"Hydrogel","modalidad":"mensual",""" +
            """"radio_base":"8.6","diametro":"14.2","laboratorio_id":"lab1","costo_unitario":45.0,""" +
            """"vigente_desde":"2026-01-01","vigente_hasta":"2026-07-31""""

        val withTs = json.decodeFromString<CostoLcRemoto>("{$base,\"updated_at\":\"$newer\",\"updated_by\":\"u1\"}").toEntity()
        val withoutTs = json.decodeFromString<CostoLcRemoto>("{$base}").toEntity()

        assertEquals(
            CostoLcEntity(
                id = "P",
                opticaId = "o1",
                tipoLc = "graduado",
                materialLc = "Hydrogel",
                modalidad = "mensual",
                radioBase = "8.6",
                diametro = "14.2",
                laboratorioId = "lab1",
                costoUnitario = 45.0,
                vigenteDesde = "2026-01-01",
                vigenteHasta = "2026-07-31",
                updatedAt = newer,
            ),
            withTs,
        )
        assertEquals(withTs, withTs.toRemoto().toEntity())
        assertNull(withoutTs.updatedAt)
    }
}
