package com.example.optoapp.data.opticasettings

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "optica_settings")
data class OpticaSettingsEntity(
    @PrimaryKey val opticaId: String,
    val configJson: String = "{}",
)
