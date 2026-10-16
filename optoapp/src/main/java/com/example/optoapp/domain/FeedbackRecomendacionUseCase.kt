package com.example.optoapp.domain

import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionDao
import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionEntity
import javax.inject.Inject

open class FeedbackRecomendacionUseCase @Inject constructor(
    private val feedbackRecomendacionDao: FeedbackRecomendacionDao,
) {
    companion object {
        fun stableId(opticaId: String, periodo: String, recomendacionId: String): String =
            "$opticaId|$periodo|$recomendacionId"
    }

    suspend fun marcarSeguimiento(
        recomendacionId: String,
        opticaId: String,
        periodo: String,
        estado: SeguimientoEstado,
    ) {
        feedbackRecomendacionDao.upsert(
            FeedbackRecomendacionEntity(
                id = stableId(opticaId, periodo, recomendacionId),
                recomendacionId = recomendacionId,
                opticaId = opticaId,
                fueUtil = estado.fueUtil,
                estado = estado.name,
                periodo = periodo,
            ),
        )
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
