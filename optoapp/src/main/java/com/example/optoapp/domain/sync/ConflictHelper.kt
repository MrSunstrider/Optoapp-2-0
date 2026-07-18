package com.example.optoapp.domain.sync

import com.example.optoapp.util.AppLogger
import androidx.annotation.VisibleForTesting
import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.SyncStateTracker
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
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
                AppLogger.w(TAG, "No se pudieron parsear timestamps, fallback a string comparison: local=$localTs, remote=$remoteTs")
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

        /**
         * Pure comparison logic: detects conflicts between local and remote
         * MonturaMovimiento lists using composite key (referenciaId, tipo, monturaId)
         * and stockNuevo comparison.
         *
         * @param local local movimientos to check
         * @param remote movimientos already in Supabase (same opticaId)
         * @return Pair of (safe IDs, conflicted IDs)
         */
        fun detectConflictMovimientos(
            local: List<MonturaMovimiento>,
            remote: List<MonturaMovimiento>
        ): Pair<List<String>, List<String>> {
            val remoteByKey = remote.associateBy {
                Triple(it.referenciaId, it.tipo, it.monturaId)
            }

            val safe = mutableListOf<String>()
            val conflicted = mutableListOf<String>()

            for (mov in local) {
                val key = Triple(mov.referenciaId, mov.tipo, mov.monturaId)
                val remoteMov = remoteByKey[key]
                when {
                    remoteMov == null -> safe.add(mov.id)
                    remoteMov.stockNuevo == mov.stockNuevo -> safe.add(mov.id)
                    else -> conflicted.add(mov.id)
                }
            }

            return Pair(safe, conflicted)
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
        if (checkable.isEmpty()) {
            // All entities lack updatedAt (pre-migration data). They are safe to upload,
            // but we must still clear any stale conflict_records — otherwise the download
            // guard blocks their download indefinitely and Room never receives server timestamps.
            localEntities.forEach { entity -> conflictDao.resolveConflict(entity.id, opticaId) }
            return localEntities
        }

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
                AppLogger.w(TAG, "Conflicto en $entityType/${entity.id}: local=${entity.updatedAt} < remoto=$remoteUpdatedAt")
                // FR-08: Capture full-entity snapshots at conflict detection time
                val localDataJson = entity.localData.ifBlank { entity.updatedAt ?: "" }
                val remoteDataJson = try {
                    fetchRemoteRowJson(tableName, opticaId, entity.id)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error capturing remote snapshot for ${entity.id}: ${e.message}")
                    "{}"
                }
                conflictDao.upsertConflict(
                    entityId = entity.id,
                    opticaId = opticaId,
                    entityType = entityType,
                    localSnapshot = entity.updatedAt,
                    remoteSnapshot = remoteUpdatedAt,
                    baseSnapshot = "{}",
                    localData = localDataJson,
                    remoteData = remoteDataJson
                )
                syncStateTracker.markConflicted(opticaId, entityType, entity.id)
            }
        }

        val conflictedCount = localEntities.size - safe.size
        if (conflictedCount > 0) {
            AppLogger.w(TAG, "$conflictedCount entidades $entityType en conflicto, se omiten del upload")
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
    internal open suspend fun fetchRemoteUpdatedAt(
        tableName: String,
        opticaId: String,
        ids: List<String>
    ): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        return try {
            val rows = selectRemoteRows(tableName, opticaId, ids)
            rows.mapNotNull { row -> row.updatedAt?.let { ts -> row.id to ts } }.toMap()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error fetching remote timestamps from $tableName: ${e.message}")
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

    /**
     * Fetches the full remote row as a JSON string for snapshot capture.
     *
     * Used by [filterConflicts] to serialize `remoteData` on conflict detection.
     * Overridable in tests via a test subclass.
     *
     * @return the full Supabase row as JSON, or `"{}"` on any error
     */
    @VisibleForTesting
    internal open suspend fun fetchRemoteRowJson(
        tableName: String,
        opticaId: String,
        entityId: String
    ): String {
        return try {
            val result = supabase.postgrest[tableName]
                .select {
                    filter {
                        eq("optica_id", opticaId)
                        eq("id", entityId)
                    }
                }
            val rawData = result.data
            if (rawData.isBlank() || rawData == "[]") "{}" else rawData
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error fetching remote row from $tableName/$entityId: ${e.message}")
            "{}"
        }
    }

    /**
     * Fetches remote movimientos matching composite keys, detects conflicts,
     * flags conflicted movements in SyncStateTracker AND persists conflict_records.
     *
     * @return IDs of local movimientos safe to upload
     */
    suspend fun filterConflictMovimientos(
        opticaId: String,
        localMovimientos: List<MonturaMovimiento>
    ): List<String> {
        if (localMovimientos.isEmpty()) return emptyList()

        val remoteMovimientos = try {
            fetchRemoteMovimientos(opticaId)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error fetching remote movimientos for conflict detection: ${e.message}")
            emptyList()
        }

        val (safeIds, conflictedIds) = detectConflictMovimientos(localMovimientos, remoteMovimientos)

        for (id in conflictedIds) {
            AppLogger.w(TAG, "Conflicto en movimiento $id: stockNuevo difiere del remoto")
            conflictDao.upsertConflict(
                entityId = id,
                opticaId = opticaId,
                entityType = "montura_movimiento",
                localSnapshot = "",
                remoteSnapshot = ""
            )
            syncStateTracker.markConflicted(opticaId, "montura_movimiento", id)
        }

        val conflictedCount = conflictedIds.size
        if (conflictedCount > 0) {
            AppLogger.w(TAG, "$conflictedCount movimientos en conflicto, se omiten del upload")
        }

        return safeIds
    }

    /**
     * Fetches remote movimientos from Supabase for conflict detection.
     * Extracted as a testability seam — test subclasses override to inject canned data.
     */
    @VisibleForTesting
    internal open suspend fun fetchRemoteMovimientos(opticaId: String): List<MonturaMovimiento> {
        return supabase.postgrest["montura_movimientos"]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<MovimientoRemotoRow>()
            .mapNotNull { it.toEntityOrNull() }
    }
}

/** Internal so test subclasses in the same Gradle module can reference the type without exposing it to consumers. */
@Serializable
internal data class RemoteTimestamp(
    val id: String,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Represents a local entity with its id, updatedAt, and optionally serialized full entity data
 * for snapshot-based three-way merge (Phase B).
 *
 * @param localData serialized full entity JSON (kotlinx.serialization format),
 *                  used for snapshot capture on conflict detection. Defaults to `""`.
 */
data class LocalEntity(
    val id: String,
    val updatedAt: String? = null,
    val localData: String = ""
)

/**
 * Lightweight remote movimiento DTO for conflict detection queries.
 * Only fetches the fields needed for composite-key + stockNuevo comparison.
 */
@Serializable
private data class MovimientoRemotoRow(
    val id: String,
    @SerialName("montura_id") val monturaId: String,
    val tipo: String,
    @SerialName("stock_nuevo") val stockNuevo: Int,
    @SerialName("referencia_id") val referenciaId: String
) {
    fun toEntityOrNull(): MonturaMovimiento? {
        return try {
            MonturaMovimiento(
                id = id,
                monturaId = monturaId,
                tipo = tipo,
                cantidad = 0,
                stockPrevio = 0,
                stockNuevo = stockNuevo,
                referenciaId = referenciaId,
                nota = "",
                opticaId = ""
            )
        } catch (_: Exception) {
            null
        }
    }
}
