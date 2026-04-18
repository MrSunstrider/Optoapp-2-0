package com.example.optoapp.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * P0-T5: persiste estado por fila tras bajar/subir desde Supabase.
 */
@Singleton
class SyncStateTracker @Inject constructor(
    private val dao: SyncEntityStateDao
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
}
