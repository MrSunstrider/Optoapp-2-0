package com.example.optoapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_telemetry_log")
data class SyncTelemetryLogEntity(
    @PrimaryKey val id: String,
    val opticaId: String,
    val status: String,
    val stage: String,
    val errorMessage: String,
    val createdAt: Long,
)
