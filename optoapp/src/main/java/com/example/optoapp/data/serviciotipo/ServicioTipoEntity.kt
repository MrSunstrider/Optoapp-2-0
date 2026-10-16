package com.example.optoapp.data.serviciotipo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "servicio_tipos",
    indices = [Index(value = ["opticaId"])],
)
data class ServicioTipoEntity(
    @PrimaryKey val id: String,
    val opticaId: String,
    val nombre: String,
    @ColumnInfo(defaultValue = "0") val costoPropio: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val costoTercero: Double = 0.0,
    @ColumnInfo(defaultValue = "1") val activo: Boolean = true,
    val updatedAt: String? = null,
    val updatedBy: String? = null,
)
