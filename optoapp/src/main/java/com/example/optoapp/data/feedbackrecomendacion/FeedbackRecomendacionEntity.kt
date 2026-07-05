package com.example.optoapp.data.feedbackrecomendacion

import androidx.room.Entity

@Entity(
    tableName = "feedback_recomendaciones",
    primaryKeys = ["recomendacionId", "opticaId"]
)
data class FeedbackRecomendacionEntity(
    val recomendacionId: String,
    val opticaId: String,
    val fueUtil: Boolean,
    val fecha: Long = System.currentTimeMillis()
)
