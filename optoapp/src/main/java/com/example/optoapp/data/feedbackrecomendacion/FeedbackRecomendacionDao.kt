package com.example.optoapp.data.feedbackrecomendacion

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface FeedbackRecomendacionDao {
    @Upsert
    suspend fun upsert(feedback: FeedbackRecomendacionEntity)

    @Query("SELECT * FROM feedback_recomendaciones WHERE opticaId = :opticaId ORDER BY fecha DESC")
    suspend fun getByOpticaId(opticaId: String): List<FeedbackRecomendacionEntity>
}
