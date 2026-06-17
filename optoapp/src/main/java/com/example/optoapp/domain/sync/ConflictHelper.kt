package com.example.optoapp.domain.sync

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.SyncStateTracker
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper para detectar conflictos entre Room local y Supabase remoto
 * antes de hacer upsert, usando el campo updated_at como referencia.
 *
 * Cada UseCase de sync lo invoca antes del upload masivo.
 *
 * Compara timestamps como [Instant] (no como strings) para evitar
 * falsos conflictos por diferencias de formato (milisegundos, zona horaria).
 */
@Singleton
open class ConflictHelper @Inject constructor(
    private val supabase: SupabaseClient,
    private val syncStateTracker: SyncStateTracker,
    private val conflictDao: ConflictDao
) {
    companion object {
        private const val TAG = "ConflictHelper"

        /**
         * Compara dos timestamps como [Instant].
         * Retorna true si local es más nuevo o igual que remoto.
         *
         * Tolerancia: si ambos se parsean al mismo Instant, se consideran iguales
         * aunque el string tenga formatos diferentes (con/sin ms, +00:00 vs Z).
         */
        fun isLocalNewerOrEqual(localTs: String, remoteTs: String): Boolean {
            val local = parseInstant(localTs)
            val remote = parseInstant(remoteTs)
            if (local == null || remote == null) {
                Log.w(TAG, "No se pudieron parsear timestamps, fallback a string comparison: local=$localTs, remote=$remoteTs")
                return localTs >= remoteTs
            }
            return local >= remote
        }

        private fun parseInstant(ts: String): Instant? {
            return try {
                Instant.parse(ts)
            } catch (_: Exception) {
                try {
                    java.time.LocalDateTime.parse(ts).toInstant(java.time.ZoneOffset.UTC)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Filtra una lista de entidades locales, separando las seguras para upsert
     * de las que están en conflicto. Las entidades en conflicto se registran
     * en [ConflictRecord] y en [SyncStateTracker].
     *
     * Entities routed to the safe list also have their stale conflict record
     * cleared via [ConflictDao.resolveConflict] (idempotent auto-heal).
     *
     * @param tableName nombre de la tabla en Supabase (snake_case)
     * @param opticaId óptica activa
     * @param entityType tipo de entidad para SyncStateTracker
     * @param localEntities lista de entidades locales con id y updatedAt
     * @return lista de entidades SEGURAS para upsert (sin conflicto)
     */
    suspend fun filterConflicts(
        tableName: String,
        opticaId: String,
        entityType: String,
        localEntities: List<LocalEntity>
    ): List<LocalEntity> {
        if (localEntities.isEmpty()) return localEntities

        val checkable = localEntities.filter { it.updatedAt != null }
        if (checkable.isEmpty()) return localEntities

        val checkableIds = checkable.map { it.id }
        val remoteTimestamps = fetchRemoteUpdatedAt(tableName, opticaId, checkableIds)

        val safe = mutableListOf<LocalEntity>()
        for (entity in localEntities) {
            val remoteUpdatedAt = remoteTimestamps[entity.id]
            if (entity.updatedAt == null || remoteUpdatedAt == null) {
                safe.add(entity)
                conflictDao.resolveConflict(entity.id, opticaId)
                continue
            }

            if (isLocalNewerOrEqual(entity.updatedAt, remoteUpdatedAt)) {
                safe.add(entity)
                conflictDao.resolveConflict(entity.id, opticaId)
            } else {
                Log.w(TAG, "Conflicto en $entityType/${entity.id}: local=${entity.updatedAt} < remoto=$remoteUpdatedAt")
                conflictDao.upsertConflict(
                    entityId = entity.id,
                    opticaId = opticaId,
                    entityType = entityType,
                    localSnapshot = entity.updatedAt,
                    remoteSnapshot = remoteUpdatedAt
                )
                syncStateTracker.markConflicted(opticaId, entityType, entity.id)
            }
        }

        val conflictedCount = localEntities.size - safe.size
        if (conflictedCount > 0) {
            Log.w(TAG, "$conflictedCount entidades $entityType en conflicto, se omiten del upload")
        }
        return safe
    }

    /**
     * Obtiene el mapa id → updated_at desde Supabase para una lista de IDs.
     *
     * Returns an empty map immediately when [ids] is empty, without any network call.
     * Delegates the actual remote query to [selectRemoteRows], which is overridable
     * for testing purposes.
     */
    internal suspend fun fetchRemoteUpdatedAt(
        tableName: String,
        opticaId: String,
        ids: List<String>
    ): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        return try {
            val rows = selectRemoteRows(tableName, opticaId, ids)
            rows.mapNotNull { row -> row.updatedAt?.let { ts -> row.id to ts } }.toMap()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote timestamps from $tableName: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Executes the actual Supabase query for remote timestamps, filtered to exactly
     * the given [ids]. Extracted as a seam to allow test subclasses in the same module
     * to override without touching the network.
     */
    @VisibleForTesting
    internal open suspend fun selectRemoteRows(
        tableName: String,
        opticaId: String,
        ids: List<String>
    ): List<RemoteTimestamp> {
        return supabase.postgrest[tableName]
            .select {
                filter {
                    eq("optica_id", opticaId)
                    isIn("id", ids)
                }
            }
            .decodeList()
    }
}

/** Internal so test subclasses in the same Gradle module can reference the type without exposing it to consumers. */
@Serializable
internal data class RemoteTimestamp(
    val id: String,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

data class LocalEntity(
    val id: String,
    val updatedAt: String? = null
)
