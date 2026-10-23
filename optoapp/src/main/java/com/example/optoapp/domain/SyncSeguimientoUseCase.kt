package com.example.optoapp.domain

import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionDao
import com.example.optoapp.domain.sync.ConflictHelper
import com.example.optoapp.util.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.inject.Inject

/**
 * Uploads/downloads plan-de-acción seguimiento with updatedAt LWW against feedback_recomendaciones.
 */
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
        if (locals.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_$ENTITY", "batch")
            return 0
        }
        val rows = locals.map { it.toRemoto().copy(opticaId = opticaId.trim()) }.distinctBy { it.id }
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
                result = supabase.postgrest[TABLE]
                    .select { filter { eq("optica_id", opticaId) } }
                    .decodeList()
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
            val keepLocal = local != null &&
                local.updatedAt.isNotBlank() &&
                !syncStateTracker.isSynced(opticaId, ENTITY, remote.id) &&
                (remote.updatedAt == null ||
                    ConflictHelper.isLocalNewerOrEqual(local.updatedAt, remote.updatedAt))
            if (keepLocal) continue
            feedbackRecomendacionDao.upsert(remote.toEntity())
            syncStateTracker.markSynced(opticaId, ENTITY, remote.id)
            applied++
        }
        return applied
    }
}
