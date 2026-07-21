package com.example.optoapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncTelemetryRemoteRow(
    @SerialName("optica_id") val opticaId: String,
    @SerialName("last_sync_at") val lastSyncAt: String? = null,
    @SerialName("last_status") val lastStatus: String,
    @SerialName("last_stage") val lastStage: String,
    @SerialName("last_error") val lastError: String = "",
)
