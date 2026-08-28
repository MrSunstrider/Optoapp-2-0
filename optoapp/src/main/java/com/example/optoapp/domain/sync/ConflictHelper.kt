package com.example.optoapp.domain.sync

import androidx.annotation.VisibleForTesting
import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.util.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private val conflictDao: ConflictDao,
) {
    companion object {
        private const val TAG = "ConflictHelper"
        private const val REMOTE_MOVIMIENTO_PAGE_SIZE = 500L

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
                return normalizeTimestamp(localTs) >= normalizeTimestamp(remoteTs)
            }
            return local >= remote
        }

        /**
         * Normalizes a timestamp string for [Instant.parse] compatibility.
         * Truncates fractional seconds to at most 3 digits and replaces
         * `+00:00` / `+0000` UTC offset suffixes with `Z`.
         *
         * Idempotent — already-normalized timestamps pass through unchanged.
         */
        internal fun normalizeTimestamp(ts: String): String {
            val dotIndex = ts.indexOf('.')
            val result = if (dotIndex >= 0) {
                val afterDot = ts.substring(dotIndex + 1)
                val endIdx = afterDot.indexOfFirst { it == 'Z' || it == '+' || it == '-' }
                val fraction = if (endIdx >= 0) afterDot.substring(0, endIdx) else afterDot
                val truncated = if (fraction.length > 3) fraction.substring(0, 3) else fraction
                ts.substring(0, dotIndex + 1) + truncated +
                    if (endIdx >= 0) afterDot.substring(endIdx) else ""
            } else {
                ts
            }
            return result.replace("+00:00", "Z").replace("+0000", "Z")
        }

        private fun parseInstant(ts: String): Instant? = try {
            Instant.parse(normalizeTimestamp(ts))
        } catch (_: Exception) {
            try {
                java.time.LocalDateTime.parse(ts).toInstant(java.time.ZoneOffset.UTC)
            } catch (_: Exception) {
                null
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
            remote: List<MonturaMovimiento>,
        ): Pair<List<String>, List<String>> = detectConflictMovimientos(
            local,
            indexRemoteByCompositeKey(remote),
        )

        fun detectConflictMovimientos(
            local: List<MonturaMovimiento>,
            remoteByKey: Map<MovimientoCompositeKey, MonturaMovimiento>,
        ): Pair<List<String>, List<String>> {
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

        /**
         * Splits safe movimientos into rows that need a remote upsert vs rows already
         * present remotely under a different PK but the same composite key.
         */
        fun partitionMovimientosForUpload(
            safeMovimientos: List<MonturaMovimiento>,
            remoteByKey: Map<MovimientoCompositeKey, MonturaMovimiento>,
        ): MovimientoUploadPartition {
            val toUpload = mutableListOf<MonturaMovimiento>()
            val toReconcileLocally = mutableListOf<Pair<MonturaMovimiento, String>>()

            for (local in safeMovimientos) {
                val key = MovimientoCompositeKey(local.referenciaId, local.tipo, local.monturaId)
                val remote = remoteByKey[key]
                when {
                    remote == null -> toUpload.add(local)
                    remote.id != local.id -> toReconcileLocally.add(local to remote.id)
                    else -> toUpload.add(local)
                }
            }

            return MovimientoUploadPartition(
                toUpload = toUpload,
                toReconcileLocally = toReconcileLocally,
            )
        }

        /**
         * Collapses remote rows that share a composite key deterministically (smallest id wins).
         */
        fun indexRemoteByCompositeKey(remote: List<MonturaMovimiento>): Map<MovimientoCompositeKey, MonturaMovimiento> =
            remote.groupBy { MovimientoCompositeKey(it.referenciaId, it.tipo, it.monturaId) }
                .mapValues { (_, rows) -> rows.minBy { it.id } }
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
        localEntities: List<LocalEntity>,
        remoteUpdatedAtMap: Map<String, String>? = null,
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
        val remoteTimestamps = remoteUpdatedAtMap
            ?: fetchRemoteUpdatedAt(tableName, opticaId, checkableIds)

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
                val localDataJson = entity.localData.ifBlank { "{}" }
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
                    localSnapshot = if (entity.localData.isNotBlank()) entity.localData else localDataJson,
                    remoteSnapshot = remoteUpdatedAt,
                    baseSnapshot = "{}",
                    localData = localDataJson,
                    remoteData = remoteDataJson,
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
        ids: List<String>,
    ): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        val rows = selectRemoteRows(tableName, opticaId, ids)
        if (ids.isNotEmpty() && rows.isEmpty()) {
            AppLogger.w(TAG, "No remote rows found for $tableName (IDs may not exist remotely or all chunks failed)")
        }
        return rows.mapNotNull { row -> row.updatedAt?.let { ts -> row.id to ts } }.toMap()
    }

    /**
     * Executes a single-chunk Supabase query for remote timestamps, filtered to
     * exactly the given [ids]. Extracted as a seam to allow test subclasses to
     * override per-chunk behavior without bypassing the chunking logic.
     */
    @VisibleForTesting
    internal open suspend fun selectRemoteRowsChunk(
        tableName: String,
        opticaId: String,
        ids: List<String>,
    ): List<RemoteTimestamp> = supabase.postgrest[tableName]
        .select {
            filter {
                eq("optica_id", opticaId)
                isIn("id", ids)
            }
        }
        .decodeList()

    /**
     * Executes remote timestamp queries in batches of at most 80 IDs per chunk,
     * running chunks in parallel via structured concurrency. Merges results.
     *
     * Per-chunk failures are caught and logged; surviving chunks still contribute
     * results. Returns empty list only when ALL chunks fail or [ids] is empty.
     */
    @VisibleForTesting
    internal open suspend fun selectRemoteRows(
        tableName: String,
        opticaId: String,
        ids: List<String>,
    ): List<RemoteTimestamp> {
        if (ids.isEmpty()) return emptyList()
        return coroutineScope {
            ids.chunked(80).map { chunk ->
                async {
                    try {
                        selectRemoteRowsChunk(tableName, opticaId, chunk)
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Chunk query failed for ${chunk.size} IDs: ${e.message}")
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
        }
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
        entityId: String,
    ): String = try {
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

    /**
     * Fetches remote movimientos matching composite keys, detects conflicts,
     * flags conflicted movements in SyncStateTracker AND persists conflict_records.
     *
     * @return upload plan with safe IDs, remote lookup by composite key, and conflicted IDs
     */
    suspend fun filterConflictMovimientos(
        opticaId: String,
        localMovimientos: List<MonturaMovimiento>,
    ): MovimientoUploadPlan {
        if (localMovimientos.isEmpty()) {
            return MovimientoUploadPlan(emptyList(), emptyMap(), emptyList())
        }

        val remoteMovimientos = try {
            fetchRemoteMovimientos(opticaId)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error fetching remote movimientos for conflict detection: ${e.message}")
            return MovimientoUploadPlan(
                safeIds = emptyList(),
                remoteByKey = emptyMap(),
                conflictedIds = emptyList(),
                remoteFetchSucceeded = false,
            )
        }

        val remoteByKey = buildRemoteByKey(remoteMovimientos)
        val (safeIds, conflictedIds) = detectConflictMovimientos(localMovimientos, remoteByKey)

        for (id in conflictedIds) {
            AppLogger.w(TAG, "Conflicto en movimiento $id: stockNuevo difiere del remoto")
            val localMov = localMovimientos.find { it.id == id }
            val remoteMov = localMov?.let {
                remoteByKey[MovimientoCompositeKey(it.referenciaId, it.tipo, it.monturaId)]
            }
            val localJson = localMov?.let { EntitySnapshotSerializer.serialize(it) } ?: "{}"
            val remoteJson = remoteMov?.let { EntitySnapshotSerializer.serialize(it) } ?: "{}"
            conflictDao.upsertConflict(
                entityId = id,
                opticaId = opticaId,
                entityType = "montura_movimiento",
                localSnapshot = localJson,
                remoteSnapshot = remoteJson,
                localData = localJson,
                remoteData = remoteJson,
            )
            syncStateTracker.markConflicted(opticaId, "montura_movimiento", id)
        }

        val conflictedCount = conflictedIds.size
        if (conflictedCount > 0) {
            AppLogger.w(TAG, "$conflictedCount movimientos en conflicto, se omiten del upload")
        }

        return MovimientoUploadPlan(
            safeIds = safeIds,
            remoteByKey = remoteByKey,
            conflictedIds = conflictedIds,
        )
    }

    private fun buildRemoteByKey(remoteMovimientos: List<MonturaMovimiento>): Map<MovimientoCompositeKey, MonturaMovimiento> {
        val grouped = remoteMovimientos.groupBy {
            MovimientoCompositeKey(it.referenciaId, it.tipo, it.monturaId)
        }
        grouped.forEach { (key, rows) ->
            if (rows.size > 1) {
                AppLogger.w(TAG, "Duplicate remote composite key $key (${rows.size} rows); keeping smallest id")
            }
        }
        return ConflictHelper.indexRemoteByCompositeKey(remoteMovimientos)
    }

    /**
     * Fetches remote movimientos from Supabase for conflict detection.
     * Extracted as a testability seam — test subclasses override to inject canned data.
     */
    @VisibleForTesting
    internal open suspend fun fetchRemoteMovimientos(opticaId: String): List<MonturaMovimiento> {
        val pageSize = REMOTE_MOVIMIENTO_PAGE_SIZE
        val all = mutableListOf<MonturaMovimiento>()
        var offset = 0L
        while (true) {
            val page = fetchRemoteMovimientosPage(opticaId, offset, offset + pageSize - 1)
            all.addAll(page)
            if (page.size < pageSize) break
            offset += pageSize
        }
        return all
    }

    @VisibleForTesting
    internal open suspend fun fetchRemoteMovimientosPage(
        opticaId: String,
        from: Long,
        to: Long,
    ): List<MonturaMovimiento> = supabase.postgrest["montura_movimientos"]
        .select {
            filter { eq("optica_id", opticaId) }
            order("id", Order.ASCENDING)
            range(from..to)
        }
        .decodeList<MovimientoRemotoRow>()
        .mapNotNull { it.toEntityOrNull(opticaId) }
}

typealias MovimientoCompositeKey = Triple<String, String, String>

data class MovimientoUploadPlan(
    val safeIds: List<String>,
    val remoteByKey: Map<MovimientoCompositeKey, MonturaMovimiento>,
    val conflictedIds: List<String>,
    val remoteFetchSucceeded: Boolean = true,
)

data class MovimientoUploadPartition(
    val toUpload: List<MonturaMovimiento>,
    val toReconcileLocally: List<Pair<MonturaMovimiento, String>>,
)

/** Internal so test subclasses in the same Gradle module can reference the type without exposing it to consumers. */
@Serializable
internal data class RemoteTimestamp(
    val id: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
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
    val localData: String = "",
)

/**
 * Lightweight remote movimiento DTO for conflict detection queries.
 * Only fetches the fields needed for composite-key + stockNuevo comparison.
 */
@Serializable
private data class MovimientoRemotoRow(
    val id: String,
    @SerialName("montura_id") val monturaId: String,
    val fecha: String? = null,
    val tipo: String,
    val cantidad: Int = 0,
    @SerialName("stock_previo") val stockPrevio: Int = 0,
    @SerialName("stock_nuevo") val stockNuevo: Int,
    @SerialName("referencia_id") val referenciaId: String,
    val nota: String = "",
    @SerialName("optica_id") val opticaId: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun toEntityOrNull(fallbackOpticaId: String): MonturaMovimiento? = try {
        MonturaMovimiento(
            id = id,
            monturaId = monturaId,
            fecha = fecha?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now(),
            tipo = tipo,
            cantidad = cantidad,
            stockPrevio = stockPrevio,
            stockNuevo = stockNuevo,
            referenciaId = referenciaId,
            nota = nota,
            opticaId = opticaId.ifBlank { fallbackOpticaId },
            updatedAt = updatedAt,
        )
    } catch (_: Exception) {
        null
    }
}
