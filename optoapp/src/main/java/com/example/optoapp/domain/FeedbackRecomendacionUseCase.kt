package com.example.optoapp.domain

import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionDao
import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionEntity
import javax.inject.Inject

open class FeedbackRecomendacionUseCase @Inject constructor(
    private val feedbackRecomendacionDao: FeedbackRecomendacionDao,
) {
    suspend fun marcarUtil(recomendacionId: String, opticaId: String) {
        feedbackRecomendacionDao.upsert(
            FeedbackRecomendacionEntity(
                recomendacionId = recomendacionId,
                opticaId = opticaId,
                fueUtil = true,
            ),
        )
    }

    suspend fun marcarNoUtil(recomendacionId: String, opticaId: String) {
        feedbackRecomendacionDao.upsert(
            FeedbackRecomendacionEntity(
                recomendacionId = recomendacionId,
                opticaId = opticaId,
                fueUtil = false,
            ),
        )
    }
}
