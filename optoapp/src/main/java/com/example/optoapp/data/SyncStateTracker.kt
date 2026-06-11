package com.example.optoapp.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * P0-T5: persiste estado por fila tras bajar/subir desde Supabase.
 *
 * Soportados: synced | error | deleted | conflicted
 */
@Singleton
class SyncStateTracker @Inject constructor(
    internal val dao: SyncEntityStateDao
) {
    suspend fun markSynced(opticaId: String, entityType: String, entityId: String) {
        dao.upsert(
            SyncEntityState(
                opticaId = opticaId,
                entityType = entityType,
                entityId = entityId,
                status = "synced",
                lastError = "",
                updatedAt = System.currentTimeMillis()
            )
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
                updatedAt = System.currentTimeMillis()
            )
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
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** Marca una entidad como en conflicto — requiere resolución manual. */
    suspend fun markConflicted(opticaId: String, entityType: String, entityId: String) {
        dao.upsert(
            SyncEntityState(
                opticaId = opticaId,
                entityType = entityType,
                entityId = entityId,
                status = "conflicted",
                lastError = "",
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** Query estados por óptica */
    suspend fun getConflictedCount(opticaId: String): Int =
        dao.countByStatus(opticaId, "conflicted")

    suspend fun getErrorsCount(opticaId: String): Int =
        dao.countByStatus(opticaId, "error")
}
