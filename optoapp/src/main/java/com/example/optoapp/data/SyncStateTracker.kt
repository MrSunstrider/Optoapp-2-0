package com.example.optoapp.data

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncStateTracker @Inject constructor(
    internal val dao: SyncEntityStateDao,
    private val database: OptoDatabase,
) {
    suspend fun markSynced(opticaId: String, entityType: String, entityId: String) {
        dao.upsert(
            SyncEntityState(
                opticaId = opticaId,
                entityType = entityType,
                entityId = entityId,
                status = "synced",
                lastError = "",
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun markError(opticaId: String, entityType: String, entityId: String, err: String?) {
        dao.upsert(
            SyncEntityState(
                opticaId = opticaId,
                entityType = entityType,
                entityId = entityId,
                status = "error",
                lastError = (err ?: "error").take(500),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun markDeleted(opticaId: String, entityType: String, entityId: String) {
        dao.upsert(
            SyncEntityState(
                opticaId = opticaId,
                entityType = entityType,
                entityId = entityId,
                status = "deleted",
                lastError = "",
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun markConflicted(opticaId: String, entityType: String, entityId: String) {
        dao.upsert(
            SyncEntityState(
                opticaId = opticaId,
                entityType = entityType,
                entityId = entityId,
                status = "conflicted",
                lastError = "",
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * Holds a local row back from upload until the next download overwrites it: the remote copy is
     * authoritative and the local one is known stale. A downloaded row's markSynced releases it.
     */
    suspend fun markAwaitingRemote(opticaId: String, entityType: String, entityId: String) {
        dao.upsert(
            SyncEntityState(
                opticaId = opticaId,
                entityType = entityType,
                entityId = entityId,
                status = STATUS_AWAITING_REMOTE,
                lastError = "",
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun awaitingRemoteIds(opticaId: String, entityType: String): Set<String> =
        dao.getByStatus(opticaId, STATUS_AWAITING_REMOTE)
            .asSequence()
            .filter { it.entityType == entityType }
            .map { it.entityId }
            .toSet()

    suspend fun isSynced(opticaId: String, entityType: String, entityId: String): Boolean =
        dao.getState(opticaId, entityType, entityId)?.status == "synced"

    suspend fun clear(opticaId: String, entityType: String, entityId: String) =
        dao.clearEntityState(opticaId, entityType, entityId)

    suspend fun getConflictedCount(opticaId: String): Int = dao.countByStatus(opticaId, "conflicted")

    suspend fun getErrorsCount(opticaId: String): Int = dao.countByStatus(opticaId, "error")

    suspend fun quarantinedEntityIds(opticaId: String, entityType: String): Set<String> =
        dao.getByStatus(opticaId, "error")
            .asSequence()
            .filter { it.entityType == entityType && it.lastError.startsWith("quarantine:") }
            .map { it.entityId }
            .toSet()

    suspend fun quarantineReasons(opticaId: String, entityType: String): Map<String, String> =
        dao.getByStatus(opticaId, "error")
            .asSequence()
            .filter { it.entityType == entityType && it.lastError.startsWith("quarantine:") }
            .associate { it.entityId to it.lastError }

    // WHY: atomic sync state + operation prevents partial updates on failure
    suspend fun markSyncedAtomic(opticaId: String, entityType: String, entityId: String, block: suspend () -> Unit) {
        database.withTransaction {
            block()
            markSynced(opticaId, entityType, entityId)
        }
    }

    private companion object {
        const val STATUS_AWAITING_REMOTE = "awaiting_remote"
    }
}
