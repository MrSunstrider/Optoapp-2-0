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
import org.junit.Assert.*

/**
 * TDD tests for the download conflict guard in [DownloadSyncCoordinator].
 *
 * The guard ensures that entities with active conflict records are NOT
 * overwritten by a download cycle. These tests verify:
 *  - [ConflictDao.getConflictEntityIds] is called during each download phase.
 *  - The constructor accepts [ConflictDao] as a 5th parameter.
 *
 * Note: The Supabase calls inside download methods will throw a network
 * error in unit tests (no real server); the guard is invoked BEFORE the
 * remote decode loop, so [coVerify] for the DAO call still fires unless
 * the guard is absent, which is what RED phase catches.
 */
class DownloadSyncCoordinatorConflictGuardTest {

    private val opticaId = "optica-guard-test"

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

    // ─── Constructor contract ────────────────────────────────────────────

    @Test
    fun constructor_takesFiveDependencies() {
        val constructors = DownloadSyncCoordinator::class.java.declaredConstructors
        assertEquals(1, constructors.size)
        val params = constructors[0].parameterTypes
        assertEquals(
            "DownloadSyncCoordinator should accept exactly 5 constructor params after ConflictDao injection",
            5,
            params.size
        )
    }

    @Test
    fun conflictDao_isAcceptedAsConstructorParam() {
        val constructors = DownloadSyncCoordinator::class.java.declaredConstructors
        val params = constructors[0].parameterTypes
        val hasConflictDao = params.any { it.simpleName == "ConflictDao" }
        assertTrue(
            "ConflictDao must be a constructor parameter of DownloadSyncCoordinator",
            hasConflictDao
        )
    }

    // ─── Guard: getConflictEntityIds called per download ─────────────────

    @Test
    fun downloadServicios_queriesConflictEntityIds() = runBlocking {
        runCatching { coordinator.downloadServicios(opticaId) }

        coVerify {
            conflictDao.getConflictEntityIds(opticaId, "servicio_extra")
        }
    }

    @Test
    fun downloadDispensaciones_queriesConflictEntityIds() = runBlocking {
        runCatching { coordinator.downloadDispensaciones(opticaId) }

        coVerify {
            conflictDao.getConflictEntityIds(opticaId, "dispensacion")
        }
    }

    @Test
    fun downloadPagos_queriesConflictEntityIds() = runBlocking {
        runCatching { coordinator.downloadPagos(opticaId) }

        coVerify {
            conflictDao.getConflictEntityIds(opticaId, "pago")
        }
    }

    // ─── Guard: no active conflicts — all entities should be processed ────

    @Test
    fun downloadServicios_withNoConflicts_doesNotCallGetConflictEntityIdsMoreThanOnce() = runBlocking {
        coEvery { conflictDao.getConflictEntityIds(opticaId, "servicio_extra") } returns emptyList()

        runCatching { coordinator.downloadServicios(opticaId) }

        coVerify(exactly = 1) {
            conflictDao.getConflictEntityIds(opticaId, "servicio_extra")
        }
    }

    // ─── Guard: DAO query is entity-type-specific ─────────────────────────

    @Test
    fun downloadServicios_usesEntityType_servicio_extra() = runBlocking {
        runCatching { coordinator.downloadServicios(opticaId) }

        coVerify {
            conflictDao.getConflictEntityIds(opticaId, "servicio_extra")
        }
        // Must NOT query for the wrong type
        coVerify(exactly = 0) {
            conflictDao.getConflictEntityIds(opticaId, "dispensacion")
        }
    }

    @Test
    fun downloadDispensaciones_usesEntityType_dispensacion() = runBlocking {
        runCatching { coordinator.downloadDispensaciones(opticaId) }

        coVerify {
            conflictDao.getConflictEntityIds(opticaId, "dispensacion")
        }
        coVerify(exactly = 0) {
            conflictDao.getConflictEntityIds(opticaId, "servicio_extra")
        }
    }

    @Test
    fun downloadPagos_usesEntityType_pago() = runBlocking {
        runCatching { coordinator.downloadPagos(opticaId) }

        coVerify {
            conflictDao.getConflictEntityIds(opticaId, "pago")
        }
        coVerify(exactly = 0) {
            conflictDao.getConflictEntityIds(opticaId, "servicio_extra")
        }
    }
}
