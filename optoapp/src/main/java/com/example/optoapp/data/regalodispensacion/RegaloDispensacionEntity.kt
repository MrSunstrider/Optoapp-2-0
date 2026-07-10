package com.example.optoapp.data.regalodispensacion

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.optoapp.data.DispensacionOptica

@Entity(
    tableName = "regalos_dispensacion",
    foreignKeys = [
        ForeignKey(
            entity = DispensacionOptica::class,
            parentColumns = ["id"],
            childColumns = ["dispensacion_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["dispensacion_id"])
    ]
)
data class RegaloDispensacionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "dispensacion_id") val dispensacionId: String,
    @ColumnInfo(name = "producto_id") val productoId: String,
    val cantidad: Int,
    @ColumnInfo(name = "costo_unitario") val costoUnitario: Double,
    val descripcion: String,
    val motivo: String = "",
    @ColumnInfo(name = "optica_id") val opticaId: String
)
