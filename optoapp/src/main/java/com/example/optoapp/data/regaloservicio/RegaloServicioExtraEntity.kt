package com.example.optoapp.data.regaloservicio

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.optoapp.data.ServicioExtra

@Entity(
    tableName = "regalos_servicio_extra",
    foreignKeys = [
        ForeignKey(
            entity = ServicioExtra::class,
            parentColumns = ["id"],
            childColumns = ["servicio_extra_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["servicio_extra_id"]),
        Index(value = ["optica_id"]),
    ],
)
data class RegaloServicioExtraEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "servicio_extra_id") val servicioExtraId: String,
    @ColumnInfo(name = "producto_id") val productoId: String,
    val cantidad: Int,
    @ColumnInfo(name = "costo_unitario") val costoUnitario: Double,
    val descripcion: String,
    val motivo: String = "",
    @ColumnInfo(name = "optica_id") val opticaId: String,
)
