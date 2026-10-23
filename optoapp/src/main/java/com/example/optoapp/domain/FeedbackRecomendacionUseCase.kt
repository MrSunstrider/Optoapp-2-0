package com.example.optoapp.domain

import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionDao
import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionEntity
import java.time.Instant
import javax.inject.Inject

open class FeedbackRecomendacionUseCase @Inject constructor(
    private val feedbackRecomendacionDao: FeedbackRecomendacionDao,
    private val syncStateTracker: SyncStateTracker,
) {
    companion object {
        private const val ENTITY = "feedback_recomendacion"

        fun stableId(opticaId: String, periodo: String, recomendacionId: String): String =
            "$opticaId|$periodo|$recomendacionId"
    }

    suspend fun marcarSeguimiento(
        recomendacionId: String,
        opticaId: String,
        periodo: String,
        estado: SeguimientoEstado,
    ) {
        val id = stableId(opticaId, periodo, recomendacionId)
        feedbackRecomendacionDao.upsert(
            FeedbackRecomendacionEntity(
                id = id,
                recomendacionId = recomendacionId,
                opticaId = opticaId,
                fueUtil = estado.fueUtil,
                estado = estado.name,
                periodo = periodo,
                updatedAt = Instant.now().toString(),
            ),
        )
        // Otherwise a later download treats this row as synced and overwrites with older remote.
        syncStateTracker.markLocallyModified(opticaId, ENTITY, id)
    }

    suspend fun listarPorPeriodo(
        opticaId: String,
        periodo: String,
    ): Map<String, SeguimientoEstado> {
        return feedbackRecomendacionDao.getByOpticaIdAndPeriodo(opticaId, periodo)
            .mapNotNull { row ->
                val estado = SeguimientoEstado.fromStorage(row.estado) ?: return@mapNotNull null
                row.recomendacionId to estado
            }
            .toMap()
    }
}
