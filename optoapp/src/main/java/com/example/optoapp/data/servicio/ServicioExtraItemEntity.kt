package com.example.optoapp.data.servicio

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.optoapp.data.ServicioExtra
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(
    tableName = "servicio_extra_items",
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
@Serializable
data class ServicioExtraItem(
    @PrimaryKey val id: String,
    @SerialName("servicio_extra_id")
    @ColumnInfo(name = "servicio_extra_id")
    val servicioExtraId: String,
    @SerialName("montura_id")
    @ColumnInfo(name = "montura_id")
    val monturaId: String? = null,
    val descripcion: String = "",
    val monto: Double = 0.0,
    @SerialName("optica_id")
    @ColumnInfo(name = "optica_id")
    val opticaId: String,
    @SerialName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,
    @SerialName("updated_by")
    @ColumnInfo(name = "updated_by")
    val updatedBy: String? = null,
)
