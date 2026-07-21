package com.example.optoapp.domain.sync

import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.SyncStateTracker
import io.github.jan.supabase.SupabaseClient
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Phase 11 — RED tests for snapshot capture in [ConflictHelper.filterConflicts].
 *
 * Verifies that when a conflict is detected, the full entity JSON is captured
 * in `localData` and `remoteData` fields of the `conflict_records` row.
 */
@RunWith(RobolectricTestRunner::class)
class ConflictHelperSnapshotTest {

    private val opticaId = "optica-snapshot-test"
    private val tableName = "pacientes"
    private val entityType = "paciente"

    private val conflictDao = mockk<ConflictDao>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val supabase = mockk<SupabaseClient>(relaxed = true)

    private lateinit var conflictHelper: TestConflictHelper

    @Before
    fun setUp() {
        coEvery { conflictDao.resolveConflict(any(), any()) } just Runs
        coEvery { syncStateTracker.markConflicted(any(), any(), any()) } just Runs

        conflictHelper = TestConflictHelper(
            supabase = supabase,
            syncStateTracker = syncStateTracker,
            conflictDao = conflictDao,
        )
    }

    // ─── Test 1: conflict detection captures localData + remoteData ────────

    @Test
    fun filterConflicts_capturesLocalDataAndRemoteDataOnConflict() = runBlocking {
        val entityId = "conflict-paciente-1"
        val localDataJson = """{"id":"$entityId","nombre":"Juan","telefono":"555-0100"}"""
        val remoteDataJson = """{"id":"$entityId","nombre":"Juan","telefono":"555-0999"}"""

        // Remote has a newer timestamp → conflict
        conflictHelper.remoteTimestamps = mapOf(entityId to "2026-06-22T10:00:00Z")
        conflictHelper.remoteRowJsons = mapOf(entityId to remoteDataJson)

        val localEntities = listOf(
            LocalEntity(id = entityId, updatedAt = "2026-06-22T09:00:00Z", localData = localDataJson),
        )

        conflictHelper.filterConflicts(tableName, opticaId, entityType, localEntities)

        coVerify {
            conflictDao.upsertConflict(
                entityId = entityId,
                opticaId = opticaId,
                entityType = entityType,
                localSnapshot = any(),
                remoteSnapshot = any(),
                baseSnapshot = "{}",
                localData = localDataJson,
                remoteData = remoteDataJson,
                detectedAt = any(),
            )
        }
    }

    // ─── Test 2: no snapshots when no conflict ───────────────────────────

    @Test
    fun filterConflicts_doesNotCaptureSnapshotsForNonConflictedEntities() = runBlocking {
        val entityId = "safe-paciente-1"

        // Local is newer → no conflict
        conflictHelper.remoteTimestamps = mapOf(entityId to "2026-06-22T09:00:00Z")

        val localEntities = listOf(
            LocalEntity(id = entityId, updatedAt = "2026-06-22T10:00:00Z", localData = """{"id":"$entityId"}"""),
        )

        conflictHelper.filterConflicts(tableName, opticaId, entityType, localEntities)

        // upsertConflict should NOT be called for non-conflicted entities
        coVerify(inverse = true) {
            conflictDao.upsertConflict(
                entityId = entityId,
                opticaId = opticaId,
                entityType = entityType,
                localSnapshot = any(),
                remoteSnapshot = any(),
                baseSnapshot = any(),
                localData = any(),
                remoteData = any(),
                detectedAt = any(),
            )
        }
    }

    // ─── Test 3: remote fetch failure → remoteData = "{}" ───────────────

    @Test
    fun filterConflicts_whenRemoteFetchFails_setsEmptyRemoteData() = runBlocking {
        val entityId = "conflict-fetch-fail"
        val localDataJson = """{"id":"$entityId","nombre":"Maria"}"""

        // Remote timestamp is newer → conflict, but remote row fetch throws
        conflictHelper.remoteTimestamps = mapOf(entityId to "2026-06-22T10:00:00Z")
        conflictHelper.remoteRowJsons = mapOf() // empty = simulate fetch failure
        conflictHelper.throwOnRemoteRowFetch = true

        val localEntities = listOf(
            LocalEntity(id = entityId, updatedAt = "2026-06-22T09:00:00Z", localData = localDataJson),
        )

        conflictHelper.filterConflicts(tableName, opticaId, entityType, localEntities)

        coVerify {
            conflictDao.upsertConflict(
                entityId = entityId,
                opticaId = opticaId,
                entityType = entityType,
                localSnapshot = any(),
                remoteSnapshot = any(),
                baseSnapshot = "{}",
                localData = localDataJson,
                remoteData = "{}",
                detectedAt = any(),
            )
        }
    }
}

/**
 * Test subclass of [ConflictHelper] that overrides network-dependent methods
 * to inject canned timestamps and remote row data without hitting Supabase.
 */
private class TestConflictHelper(
    supabase: SupabaseClient,
    syncStateTracker: SyncStateTracker,
    conflictDao: ConflictDao,
) : ConflictHelper(supabase, syncStateTracker, conflictDao) {

    /** Canned remote timestamps: entityId → updatedAt string */
    var remoteTimestamps: Map<String, String> = emptyMap()

    /** Canned remote row full JSON: entityId → JSON string */
    var remoteRowJsons: Map<String, String> = emptyMap()

    /** If true, [fetchRemoteRowJson] throws to simulate network error */
    var throwOnRemoteRowFetch = false

    override suspend fun fetchRemoteUpdatedAt(
        tableName: String,
        opticaId: String,
        ids: List<String>,
    ): Map<String, String> = remoteTimestamps

    override suspend fun fetchRemoteRowJson(
        tableName: String,
        opticaId: String,
        entityId: String,
    ): String {
        if (throwOnRemoteRowFetch) throw RuntimeException("Simulated network error")
        return remoteRowJsons[entityId] ?: "{}"
    }
}
