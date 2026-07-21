package com.example.optoapp.data

import androidx.room.Entity

/**
 * P0-T5: estado de sincronización por registro (pending / synced / error) por óptica.
 */
@Entity(
    tableName = "sync_entity_state",
    primaryKeys = ["opticaId", "entityType", "entityId"],
)
data class SyncEntityState(
    val opticaId: String,
    /** p. ej. paciente, evaluacion, dispensacion, servicio_extra, pago, upload_pacientes */
    val entityType: String,
    val entityId: String,
    /** pending | synced | error */
    val status: String,
    val lastError: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)
