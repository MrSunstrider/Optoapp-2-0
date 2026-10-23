package com.example.optoapp.domain

import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionDao
import com.example.optoapp.domain.sync.ConflictHelper
import com.example.optoapp.domain.sync.LEGACY_NULL_UPDATED_AT
import com.example.optoapp.domain.sync.RemoteTimestamp
import com.example.optoapp.util.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.inject.Inject

/** Plan-de-acción sync: stale devices must not clobber newer remote or wipe unsynced local edits. */
open class SyncSeguimientoUseCase @Inject constructor(
    private val feedbackRecomendacionDao: FeedbackRecomendacionDao,
    private val supabase: SupabaseClient,
    private val syncStateTracker: SyncStateTracker,
    private val networkRetryHelper: NetworkRetryHelper,
) {
    companion object {
        private const val TAG = "SyncSeguimiento"
        private const val TABLE = "feedback_recomendaciones"
        private const val ENTITY = "feedback_recomendacion"
        private const val UPSERT_BATCH = 80
    }

    suspend fun upload(opticaId: String): Int {
        require(opticaId.isNotBlank()) { "opticaId must not be blank for upload" }
        val locals = feedbackRecomendacionDao.getByOpticaId(opticaId)
            .filter { it.periodo.isNotBlank() && it.estado.isNotBlank() }
        val synced = syncStateTracker.syncedIds(opticaId, ENTITY)
        val pending = locals.filter { it.id !in synced }
        if (pending.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_$ENTITY", "batch")
            return 0
        }
        val opticaRemota = opticaId.trim()
        val remoteUpdatedAt = try {
            fetchRemoteUpdatedAt(opticaRemota, pending.map { it.id })
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AppLogger.e(TAG, "Error leyendo updated_at de $ENTITY: ${e.message}", e)
            throw UploadPartialException(0, e)
        }
        val rows = pending
            .filter { row ->
                val remote = remoteUpdatedAt[row.id]
                remote == null ||
                    ConflictHelper.isLocalNewerOrEqual(
                        row.updatedAt.ifBlank { LEGACY_NULL_UPDATED_AT },
                        remote,
                    )
            }
            .map { it.toRemoto().copy(opticaId = opticaRemota) }
            .distinctBy { it.id }
        if (rows.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_$ENTITY", "batch")
            return 0
        }
        var uploaded = 0
        try {
            rows.chunked(UPSERT_BATCH).forEach { chunk ->
                networkRetryHelper.retryNetwork("upload:$TABLE") {
                    supabase.postgrest[TABLE].upsert(chunk)
                }
                chunk.forEach { syncStateTracker.markSynced(opticaId, ENTITY, it.id) }
                uploaded += chunk.size
            }
            syncStateTracker.markSynced(opticaId, "upload_$ENTITY", "batch")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw UploadPartialException(uploaded, e)
        }
        return uploaded
    }

    suspend fun download(opticaId: String): Int {
        require(opticaId.isNotBlank()) { "opticaId must not be blank for download" }
        val remotos = try {
            var result: List<FeedbackRecomendacionRemoto> = emptyList()
            networkRetryHelper.retryNetwork("download:$TABLE") {
                result = fetchRemoteRows(opticaId)
            }
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.w(TAG, "download failed: ${e.message}", e)
            return 0
        }
        var applied = 0
        for (remote in remotos) {
            if (remote.periodo.isBlank()) continue
            val local = feedbackRecomendacionDao.getById(remote.id)
            // Equal timestamps converge on remote so devices do not oscillate.
            val keepLocal = local != null &&
                local.updatedAt.isNotBlank() &&
                !syncStateTracker.isSynced(opticaId, ENTITY, remote.id) &&
                (remote.updatedAt == null ||
                    !ConflictHelper.isLocalNewerOrEqual(remote.updatedAt, local.updatedAt))
            if (keepLocal) continue
            feedbackRecomendacionDao.upsert(remote.toEntity())
            syncStateTracker.markSynced(opticaId, ENTITY, remote.id)
            applied++
        }
        return applied
    }

    internal open suspend fun fetchRemoteRows(opticaId: String): List<FeedbackRecomendacionRemoto> =
        supabase.postgrest[TABLE]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList()

    internal open suspend fun selectRemoteUpdatedAtChunk(
        opticaId: String,
        ids: List<String>,
    ): List<RemoteTimestamp> = supabase.postgrest[TABLE]
        .select(Columns.list("id", "updated_at")) {
            filter {
                eq("optica_id", opticaId)
                isIn("id", ids)
            }
        }
        .decodeList()

    private suspend fun fetchRemoteUpdatedAt(opticaId: String, ids: List<String>): Map<String, String?> {
        val remote = mutableMapOf<String, String?>()
        ids.chunked(UPSERT_BATCH).forEachIndexed { index, chunk ->
            networkRetryHelper.retryNetwork("updated_at:$TABLE:chunk${index + 1}") {
                selectRemoteUpdatedAtChunk(opticaId, chunk).forEach { remote[it.id] = it.updatedAt }
            }
        }
        return remote
    }
}
