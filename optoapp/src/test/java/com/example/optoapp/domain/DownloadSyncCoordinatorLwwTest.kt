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
class DownloadSyncCoordinatorLwwTest {

    private val opticaId = "o1"
    private val older = "2026-07-01T10:00:00Z"
    private val newer = "2026-07-20T10:00:00Z"
    private lateinit var db: OptoDatabase
    private lateinit var tracker: SyncStateTracker
    private val repository = mockk<OptoRepository>(relaxed = true)
    private lateinit var coordinator: DownloadSyncCoordinator

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tracker = SyncStateTracker(db.syncEntityStateDao(), db)
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        coordinator = DownloadSyncCoordinator(
            repository = repository,
            supabase = mockk<SupabaseClient>(relaxed = true),
            syncStateTracker = tracker,
            deletionSyncHelper = mockk<DeletionSyncHelper>(relaxed = true) {
                coEvery { deletedIds(any()) } returns emptySet()
            },
            networkRetryHelper = mockk<NetworkRetryHelper>(relaxed = true),
            resumenDiarioDao = mockk<ResumenDiarioDao>(relaxed = true),
            configuracionFinancieraDao = mockk<ConfiguracionFinancieraDao>(relaxed = true),
            costoProductoDao = db.costoProductoDao(),
            costoBiseladoDao = db.costoBiseladoDao(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun producto(costo: Double, updatedAt: String?) = CostoProductoEntity(
        id = "P",
        opticaId = opticaId,
        material = "Resina",
        tipoLente = "Monofocal",
        stockOFabricacion = "stock",
        costoUnitario = costo,
        vigenteDesde = "2026-01-01",
        updatedAt = updatedAt,
    )

    private fun biselado(costo: Double, updatedAt: String?) = CostoBiseladoEntity(
        id = "B",
        opticaId = opticaId,
        material = "Resina",
        tipoAro = "completo",
        stockOFabricacion = "stock",
        costoPorPar = costo,
        vigenteDesde = "2026-01-01",
        updatedAt = updatedAt,
    )

    @Test
    fun `a pending local edit newer than the remote copy is kept`() = runTest {
        db.costoProductoDao().upsertAll(listOf(producto(20.0, newer)))
        tracker.markLocallyModified(opticaId, "costo_producto", "P")

        coordinator.persistCostosProductos(opticaId, listOf(producto(18.0, older).toRemoto()))

        assertEquals(20.0, db.costoProductoDao().getById("P")!!.costoUnitario, 0.0)
        assertFalse(tracker.isSynced(opticaId, "costo_producto", "P"))
    }

    @Test
    fun `a newer remote copy wins over a pending older local edit and marks it synced`() = runTest {
        db.costoProductoDao().upsertAll(listOf(producto(20.0, older)))
        tracker.markLocallyModified(opticaId, "costo_producto", "P")

        coordinator.persistCostosProductos(opticaId, listOf(producto(22.0, newer).toRemoto()))

        assertEquals(22.0, db.costoProductoDao().getById("P")!!.costoUnitario, 0.0)
        assertEquals(newer, db.costoProductoDao().getById("P")!!.updatedAt)
        assertTrue(tracker.isSynced(opticaId, "costo_producto", "P"))
    }

    @Test
    fun `a pending biselado edit newer than the remote copy is kept`() = runTest {
        db.costoBiseladoDao().upsertAll(listOf(biselado(12.0, newer)))
        tracker.markLocallyModified(opticaId, "costo_biselado", "B")

        coordinator.persistCostosBiselado(opticaId, listOf(biselado(9.0, older).toRemoto()))

        assertEquals(12.0, db.costoBiseladoDao().getById("B")!!.costoPorPar, 0.0)
        assertFalse(tracker.isSynced(opticaId, "costo_biselado", "B"))
    }

    @Test
    fun `a synced local row is always replaced by the remote copy`() = runTest {
        db.costoBiseladoDao().upsertAll(listOf(biselado(12.0, newer)))
        tracker.markSynced(opticaId, "costo_biselado", "B")

        coordinator.persistCostosBiselado(opticaId, listOf(biselado(9.0, older).toRemoto()))

        assertEquals(9.0, db.costoBiseladoDao().getById("B")!!.costoPorPar, 0.0)
    }

    @Test
    fun `remote updated_at is parsed and an absent key maps to null`() {
        val json = Json { ignoreUnknownKeys = true }
        val base = """"id":"P","optica_id":"o1","material":"Resina","tipo_lente":"Monofocal","stock_o_fabricacion":"stock","costo_unitario":18.0,"vigente_desde":"2026-01-01""""

        val withTs = json.decodeFromString<CostoProductoRemoto>("{$base,\"updated_at\":\"$newer\"}")
        val withoutTs = json.decodeFromString<CostoProductoRemoto>("{$base}")
        val biseladoTs = json.decodeFromString<CostoBiseladoRemoto>(
            """{"id":"B","optica_id":"o1","material":"Resina","tipo_aro":"completo","stock_o_fabricacion":"stock","costo_por_par":9.0,"vigente_desde":"2026-01-01","updated_at":"$newer"}""",
        )

        assertEquals(newer, withTs.toEntity().updatedAt)
        assertNull(withoutTs.toEntity().updatedAt)
        assertEquals(newer, biseladoTs.toEntity().updatedAt)
    }
}
