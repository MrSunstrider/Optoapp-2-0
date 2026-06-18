package com.example.optoapp.domain.sync

import android.util.Log
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
class ConflictHelper @Inject constructor(
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

        /** Parsea un string ISO 8601 a [Instant], tolerando formatos comunes. */
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
                continue
            }

            if (isLocalNewerOrEqual(entity.updatedAt, remoteUpdatedAt)) {
                safe.add(entity)
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
     */
    private suspend fun fetchRemoteUpdatedAt(
        tableName: String,
        opticaId: String,
        ids: List<String>
    ): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        return try {
            val allRows = supabase.postgrest[tableName]
                .select {
                    filter { eq("optica_id", opticaId) }
                }
                .decodeList<RemoteTimestamp>()
            val idSet = ids.toSet()
            allRows
                .mapNotNull { row ->
                    if (row.id !in idSet) return@mapNotNull null
                    row.updatedAt?.let { ts -> row.id to ts }
                }
                .toMap()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote timestamps from $tableName: ${e.message}")
            emptyMap()
        }
    }

    @Serializable
    private data class RemoteTimestamp(
        val id: String,
        @kotlinx.serialization.SerialName("updated_at")
        val updatedAt: String? = null
    )

    // ─── Movimiento-specific conflict detection ─────────────────────

    /**
     * Instance method: fetches remote movimientos matching composite keys,
     * detects conflicts, flags conflicted movements in SyncStateTracker.
     *
     * @return IDs of local movimientos safe to upload
     */
    suspend fun filterConflictMovimientos(
        opticaId: String,
        localMovimientos: List<MonturaMovimiento>
    ): List<String> {
        if (localMovimientos.isEmpty()) return emptyList()

        // Fetch remote movimientos matching any of our keys
        val remoteMovimientos = try {
            supabase.postgrest["montura_movimientos"]
                .select { filter { eq("optica_id", opticaId) } }
                .decodeList<MovimientoRemotoRow>()
                .mapNotNull { it.toEntityOrNull() }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote movimientos for conflict detection: ${e.message}")
            emptyList()
        }

        val (safeIds, conflictedIds) = detectConflictMovimientos(localMovimientos, remoteMovimientos)

        // Flag conflicted movements
        for (id in conflictedIds) {
            Log.w(TAG, "Conflicto en movimiento $id: stockNuevo difiere del remoto")
            syncStateTracker.markConflicted(opticaId, "montura_movimiento", id)
        }

        val conflictedCount = conflictedIds.size
        if (conflictedCount > 0) {
            Log.w(TAG, "$conflictedCount movimientos en conflicto, se omiten del upload")
        }

        return safeIds
    }
}

/**
 * Represents a local entity with its id and updatedAt for comparison with remote.
 */
data class LocalEntity(
    val id: String,
    val updatedAt: String? = null
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
