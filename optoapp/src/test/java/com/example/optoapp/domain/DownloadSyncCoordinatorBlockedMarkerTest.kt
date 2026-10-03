package com.example.optoapp.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class DownloadSyncCoordinatorBlockedMarkerTest {

    private val opticaId = "o1"
    private lateinit var db: OptoDatabase
    private lateinit var tracker: SyncStateTracker
    private val repository = mockk<OptoRepository>(relaxed = true)
    private val deletionSyncHelper = mockk<DeletionSyncHelper>(relaxed = true)
    private val networkRetryHelper = mockk<NetworkRetryHelper>(relaxed = true)
    private lateinit var coordinator: DownloadSyncCoordinator

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tracker = SyncStateTracker(db.syncEntityStateDao(), db)
        coEvery { deletionSyncHelper.deletedIds(opticaId) } returns emptySet()
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        coordinator = DownloadSyncCoordinator(
            repository = repository,
            supabase = mockk<SupabaseClient>(relaxed = true),
            syncStateTracker = tracker,
            deletionSyncHelper = deletionSyncHelper,
            networkRetryHelper = networkRetryHelper,
            resumenDiarioDao = mockk<ResumenDiarioDao>(relaxed = true),
            configuracionFinancieraDao = mockk<ConfiguracionFinancieraDao>(relaxed = true),
            costoProductoDao = mockk<CostoProductoDao>(relaxed = true),
            costoBiseladoDao = mockk<CostoBiseladoDao>(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun remotePago(id: String) = PagoRemoto(id = id, dispensacionId = "d1", fecha = "2026-09-01", tipo = "Reverso", monto = 10.0, opticaId = opticaId)

    private fun remoteDispensacion(id: String) =
        DispensacionRemota(id = id, ot = "2026-0042", pacienteId = "p1", fecha = "2026-09-01", opticaId = opticaId)

    private suspend fun markedBlocked(entityType: String) = tracker.isSynced(opticaId, "download_${entityType}_blocked", "batch")

    @Test
    fun `a quarantined pago blocks the pagos batch locally`() = runTest {
        tracker.markError(opticaId, "pago", "p1", "quarantine:constraint:23514")

        coordinator.persistPagos(opticaId, listOf(remotePago("p1")))

        assertTrue(markedBlocked("pago"))
        assertFalse(tracker.isSynced(opticaId, "download_pago", "batch"))
    }

    @Test
    fun `a later network failure leaves no stale pagos block behind`() = runTest {
        tracker.markError(opticaId, "pago", "p1", "quarantine:constraint:23514")
        coordinator.persistPagos(opticaId, listOf(remotePago("p1")))
        assertTrue(markedBlocked("pago"))
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } throws IOException("timeout")

        coordinator.downloadPagos(opticaId)

        assertFalse(markedBlocked("pago"))
        assertEquals(
            listOf("timeout"),
            db.syncEntityStateDao().getByStatus(opticaId, "error")
                .filter { it.entityType == "download_pago" }.map { it.lastError },
        )
    }

    @Test
    fun `entities without a consumer never write a blocked marker`() = runTest {
        tracker.markError(opticaId, "dispensacion", "d1", "quarantine:constraint:23514")

        coordinator.persistDispensaciones(opticaId, listOf(remoteDispensacion("d1")))

        assertFalse(markedBlocked("dispensacion"))
        assertTrue(db.syncEntityStateDao().getByStatus(opticaId, "synced").none { it.entityType.endsWith("_blocked") })
    }

    @Test
    fun `a blocked marker left by the previous version is removed on the next fetch`() = runTest {
        tracker.markSynced(opticaId, "download_dispensacion_blocked", "batch")
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } throws IOException("timeout")

        coordinator.downloadDispensaciones(opticaId)

        assertFalse(markedBlocked("dispensacion"))
    }
}
