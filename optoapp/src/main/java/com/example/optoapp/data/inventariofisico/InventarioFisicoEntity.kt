package com.example.optoapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "inventario_fisico",
    indices = [
        Index(value = ["opticaId"]),
        Index(value = ["estado"])
    ]
)
data class InventarioFisico(
    @PrimaryKey val id: String,
    val fecha: LocalDate,
    val estado: String = "EN_PROGRESO",
    val opticaId: String,
    val userId: String,
    val notas: String = "",
    val updatedAt: String? = null
)

@Entity(
    tableName = "inventario_fisico_detalle",
    indices = [
        Index(value = ["inventarioId"]),
        Index(value = ["monturaId"]),
        Index(value = ["inventarioId", "monturaId"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = InventarioFisico::class,
            parentColumns = ["id"],
            childColumns = ["inventarioId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Montura::class,
            parentColumns = ["id"],
            childColumns = ["monturaId"]
        )
    ]
)
data class InventarioFisicoDetalle(
    @PrimaryKey val id: String,
    val inventarioId: String,
    val monturaId: String,
    val stockSistema: Int,
    val stockContado: Int? = null,
    val diferencia: Int? = null
)
