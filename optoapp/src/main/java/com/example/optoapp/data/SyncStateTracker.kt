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

    suspend fun getConflictedCount(opticaId: String): Int = dao.countByStatus(opticaId, "conflicted")

    suspend fun getErrorsCount(opticaId: String): Int = dao.countByStatus(opticaId, "error")

    // WHY: atomic sync state + operation prevents partial updates on failure
    suspend fun markSyncedAtomic(opticaId: String, entityType: String, entityId: String, block: suspend () -> Unit) {
        database.withTransaction {
            block()
            markSynced(opticaId, entityType, entityId)
        }
    }
}
