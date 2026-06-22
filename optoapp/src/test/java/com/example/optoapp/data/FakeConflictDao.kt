package com.example.optoapp.data

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Fake [ConflictDao] that records invocations of [getConflictEntityIds] and [upsertConflict].
 *
 * Uses [AtomicBoolean]/[AtomicInteger] for flags to ensure correct visibility
 * across coroutine boundaries when exceptions occur during suspend function
 * dispatch (e.g. `Log.e()` throwing `RuntimeException` inside catch blocks).
 */
class FakeConflictDao : ConflictDao {

    /** Whether [getConflictEntityIds] was called at least once */
    val getConflictEntityIdsCalled = AtomicBoolean(false)

    /** Number of times [getConflictEntityIds] was called */
    val getConflictEntityIdsCallCount = AtomicInteger(0)

    /** Last [opticaId] passed to [getConflictEntityIds] */
    @Volatile
    var lastOpticaId: String? = null

    /** Last [entityType] passed to [getConflictEntityIds] */
    @Volatile
    var lastEntityType: String? = null

    /** Return value for [getConflictEntityIds] */
    var returnEntityIds: List<String> = emptyList()

    /** Captured snapshot params from the last [upsertConflict] call */
    @Volatile
    var lastUpsertBaseSnapshot: String = "{}"

    @Volatile
    var lastUpsertLocalData: String = "{}"

    @Volatile
    var lastUpsertRemoteData: String = "{}"

    /** All [upsertConflict] calls recorded in order */
    val upsertCalls = mutableListOf<UpsertConflictCall>()

    override suspend fun getConflictEntityIds(opticaId: String, entityType: String): List<String> {
        lastOpticaId = opticaId
        lastEntityType = entityType
        getConflictEntityIdsCalled.set(true)
        getConflictEntityIdsCallCount.incrementAndGet()
        return returnEntityIds
    }

    override suspend fun getConflicts(opticaId: String): List<ConflictRecord> = emptyList()
    override suspend fun getConflictsByType(opticaId: String, entityType: String): List<ConflictRecord> = emptyList()
    override suspend fun countConflicts(opticaId: String): Int = 0
    override suspend fun upsertConflict(
        entityId: String,
        opticaId: String,
        entityType: String,
        localSnapshot: String,
        remoteSnapshot: String,
        detectedAt: Long,
        baseSnapshot: String,
        localData: String,
        remoteData: String
    ) {
        lastUpsertBaseSnapshot = baseSnapshot
        lastUpsertLocalData = localData
        lastUpsertRemoteData = remoteData
        upsertCalls.add(
            UpsertConflictCall(
                entityId = entityId,
                opticaId = opticaId,
                entityType = entityType,
                localSnapshot = localSnapshot,
                remoteSnapshot = remoteSnapshot,
                detectedAt = detectedAt,
                baseSnapshot = baseSnapshot,
                localData = localData,
                remoteData = remoteData
            )
        )
    }
    override suspend fun resolveConflict(entityId: String, opticaId: String) = Unit
    override suspend fun clearConflicts(opticaId: String) = Unit
    override suspend fun getConflictSnapshot(entityId: String, opticaId: String): ConflictSnapshot? = null
}

/** Recorded [ConflictDao.upsertConflict] invocation for test assertions. */
data class UpsertConflictCall(
    val entityId: String,
    val opticaId: String,
    val entityType: String,
    val localSnapshot: String,
    val remoteSnapshot: String,
    val detectedAt: Long,
    val baseSnapshot: String,
    val localData: String,
    val remoteData: String
)
