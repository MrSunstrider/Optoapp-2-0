package com.example.optoapp.data

import androidx.room.withTransaction
import java.time.LocalDate
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
                status = STATUS_SYNCED,
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
                status = STATUS_ERROR,
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
                status = STATUS_DELETED,
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
                status = STATUS_CONFLICTED,
                lastError = "",
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    // The remote copy is authoritative and the local one is known stale, so it must not be uploaded
    // until a download overwrites it.
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

    // Downloaded rows without this mark are never uploaded: a stale device must not resend old versions.
    suspend fun markLocallyModified(opticaId: String, entityType: String, entityId: String) {
        dao.upsert(
            SyncEntityState(
                opticaId = opticaId,
                entityType = entityType,
                entityId = entityId,
                status = STATUS_PENDING_UPLOAD,
                lastError = "",
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun syncedIds(opticaId: String, entityType: String): Set<String> =
        dao.getByStatus(opticaId, STATUS_SYNCED)
            .asSequence()
            .filter { it.entityType == entityType }
            .map { it.entityId }
            .toSet()

    suspend fun isSynced(opticaId: String, entityType: String, entityId: String): Boolean =
        dao.getState(opticaId, entityType, entityId)?.status == STATUS_SYNCED

    suspend fun clear(opticaId: String, entityType: String, entityId: String) =
        dao.clearEntityState(opticaId, entityType, entityId)

    suspend fun getConflictedCount(opticaId: String): Int = dao.countByStatus(opticaId, STATUS_CONFLICTED)

    suspend fun getErrorsCount(opticaId: String): Int = dao.countByStatus(opticaId, STATUS_ERROR)

    suspend fun quarantinedEntityIds(opticaId: String, entityType: String): Set<String> =
        dao.getByStatus(opticaId, STATUS_ERROR)
            .asSequence()
            .filter { it.entityType == entityType && it.lastError.startsWith("quarantine:") }
            .map { it.entityId }
            .toSet()

    suspend fun quarantineReasons(opticaId: String, entityType: String): Map<String, String> =
        dao.getByStatus(opticaId, STATUS_ERROR)
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

    // Re-marking refreshes markedAt so an edit made after an upload waits for the next upload.
    suspend fun markPendingRecalc(opticaId: String, fecha: LocalDate, causeType: String, causeId: String) {
        dao.upsert(
            SyncEntityState(
                opticaId = opticaId,
                entityType = ENTITY_RESUMEN_RECALC,
                entityId = recalcEntityId(fecha, causeType, causeId),
                status = STATUS_PENDING_RECALC,
                lastError = "",
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun pendingRecalcMarkers(opticaId: String): List<PendingRecalcMarker> =
        dao.getByStatus(opticaId, STATUS_PENDING_RECALC)
            .filter { it.entityType == ENTITY_RESUMEN_RECALC }
            .mapNotNull { parseRecalcMarker(it) }

    // A marker re-marked after it was read survives: its newer edit still has to be uploaded first.
    suspend fun clearPendingRecalc(opticaId: String, marker: PendingRecalcMarker) {
        val entityId = recalcEntityId(marker.fecha, marker.causeType, marker.causeId)
        database.withTransaction {
            val current = dao.getState(opticaId, ENTITY_RESUMEN_RECALC, entityId)
            if (current?.status == STATUS_PENDING_RECALC && current.updatedAt == marker.markedAt) {
                dao.clearEntityState(opticaId, ENTITY_RESUMEN_RECALC, entityId)
            }
        }
    }

    suspend fun stateOf(opticaId: String, entityType: String, entityId: String): SyncEntityState? =
        dao.getState(opticaId, entityType, entityId)

    private fun recalcEntityId(fecha: LocalDate, causeType: String, causeId: String) = "$fecha|$causeType|$causeId"

    private fun parseRecalcMarker(state: SyncEntityState): PendingRecalcMarker? {
        val parts = state.entityId.split("|", limit = 3)
        if (parts.size != 3 || parts[1].isBlank() || parts[2].isBlank()) return null
        val fecha = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return null
        return PendingRecalcMarker(fecha, parts[1], parts[2], state.updatedAt)
    }

    companion object {
        const val STATUS_PENDING_RECALC = "pending_recalc"
        const val ENTITY_RESUMEN_RECALC = "resumen_recalc"
        private const val STATUS_SYNCED = "synced"
        private const val STATUS_ERROR = "error"
        private const val STATUS_DELETED = "deleted"
        private const val STATUS_CONFLICTED = "conflicted"
        private const val STATUS_AWAITING_REMOTE = "awaiting_remote"
        private const val STATUS_PENDING_UPLOAD = "pending_upload"
    }
}

data class PendingRecalcMarker(val fecha: LocalDate, val causeType: String, val causeId: String, val markedAt: Long)
