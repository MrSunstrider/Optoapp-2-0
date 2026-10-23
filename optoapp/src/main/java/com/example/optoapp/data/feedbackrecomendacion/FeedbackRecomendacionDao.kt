package com.example.optoapp.data.feedbackrecomendacion

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface FeedbackRecomendacionDao {
    @Upsert
    suspend fun upsert(feedback: FeedbackRecomendacionEntity)

    @Upsert
    suspend fun upsertAll(rows: List<FeedbackRecomendacionEntity>)

    @Query("SELECT * FROM feedback_recomendaciones WHERE opticaId = :opticaId ORDER BY fecha DESC")
    suspend fun getByOpticaId(opticaId: String): List<FeedbackRecomendacionEntity>

    @Query("SELECT * FROM feedback_recomendaciones WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FeedbackRecomendacionEntity?

    @Query(
        "SELECT * FROM feedback_recomendaciones WHERE opticaId = :opticaId AND periodo = :periodo ORDER BY fecha DESC",
    )
    suspend fun getByOpticaIdAndPeriodo(
        opticaId: String,
        periodo: String,
    ): List<FeedbackRecomendacionEntity>
}
