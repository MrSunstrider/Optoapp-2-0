package com.example.optoapp.data.feedbackrecomendacion

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "feedback_recomendaciones"
)
data class FeedbackRecomendacionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val recomendacionId: String,
    val opticaId: String,
    val fueUtil: Boolean,
    val fecha: Long = System.currentTimeMillis()
)
