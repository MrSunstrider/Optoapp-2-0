package com.example.optoapp.domain

import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * TDD tests for NEW download conflict guards in [DownloadSyncCoordinator].
 *
 * conflictDao is ALREADY injected (existing constructor). These tests verify
 * the two remaining unguarded download methods:
 *  - downloadDispensacionItems → guards with "dispensacion_item"
 *  - downloadArqueos → guards with "arqueo_caja"
 */
class DownloadSyncCoordinatorNewGuardsTest {

    private val opticaId = "optica-new-guards"

    private val repository = mockk<OptoRepository>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val deletionSyncHelper = mockk<DeletionSyncHelper>(relaxed = true)
    private val conflictDao = mockk<ConflictDao>(relaxed = true)

    private val fakeSupabase = createSupabaseClient(
        supabaseUrl = "https://placeholder.supabase.co",
        supabaseKey = "placeholder-key"
    ) {}

    private lateinit var coordinator: DownloadSyncCoordinator

    @Before
    fun setUp() {
        coEvery { deletionSyncHelper.deletedIds(any()) } returns emptySet()
        coEvery { conflictDao.getConflictEntityIds(any(), any()) } returns emptyList()
        coEvery { syncStateTracker.markSynced(any(), any(), any()) } just Runs
        coEvery { syncStateTracker.markError(any(), any(), any(), any()) } just Runs

        coordinator = DownloadSyncCoordinator(
            repository = repository,
            supabase = fakeSupabase,
            syncStateTracker = syncStateTracker,
            deletionSyncHelper = deletionSyncHelper,
            conflictDao = conflictDao
        )
    }

    // ─── Guard: downloadDispensacionItems — "dispensacion_item" ───────────

    @Test
    fun downloadDispensacionItems_queriesConflictEntityIds() = runBlocking {
        runCatching { coordinator.downloadDispensacionItems(opticaId) }

        coVerify {
            conflictDao.getConflictEntityIds(opticaId, "dispensacion_item")
        }
    }

    @Test
    fun downloadDispensacionItems_usesCorrectEntityType() = runBlocking {
        runCatching { coordinator.downloadDispensacionItems(opticaId) }

        coVerify {
            conflictDao.getConflictEntityIds(opticaId, "dispensacion_item")
        }
        coVerify(exactly = 0) {
            conflictDao.getConflictEntityIds(opticaId, "dispensacion")
        }
    }

    // ─── Guard: downloadArqueos — "arqueo_caja" ──────────────────────────

    @Test
    fun downloadArqueos_queriesConflictEntityIds() = runBlocking {
        runCatching { coordinator.downloadArqueos(opticaId) }

        coVerify {
            conflictDao.getConflictEntityIds(opticaId, "arqueo_caja")
        }
    }

    @Test
    fun downloadArqueos_usesCorrectEntityType() = runBlocking {
        runCatching { coordinator.downloadArqueos(opticaId) }

        coVerify {
            conflictDao.getConflictEntityIds(opticaId, "arqueo_caja")
        }
        coVerify(exactly = 0) {
            conflictDao.getConflictEntityIds(opticaId, "dispensacion")
        }
    }
}
