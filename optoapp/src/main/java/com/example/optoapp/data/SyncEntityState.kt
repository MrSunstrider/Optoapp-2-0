package com.example.optoapp.data

import androidx.room.Entity

@Entity(
    tableName = "sync_entity_state",
    primaryKeys = ["opticaId", "entityType", "entityId"],
)
data class SyncEntityState(
    val opticaId: String,
    val entityType: String,
    val entityId: String,
    val status: String,
    val lastError: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)
