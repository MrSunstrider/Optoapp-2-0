package com.example.optoapp.data.venta

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "ventas",
    indices = [
        Index(value = ["opticaId"]),
        Index(value = ["origen", "origenId"])
    ]
)
data class Venta(
    @PrimaryKey val id: String,
    val opticaId: String,
    val origen: String,
    val origenId: String,
    val pacienteId: String = "",
    val ot: String = "",
    val fecha: LocalDate,
    val fechaEntrega: LocalDate? = null,
    val montoTotal: Double,
    val costoUnitarioSnapshot: Double? = null,
    val estado: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val updatedBy: String? = null,
    val categoriaProductoId: String? = null
)
