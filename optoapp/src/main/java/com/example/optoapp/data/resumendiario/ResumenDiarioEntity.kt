package com.example.optoapp.data.resumendiario

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "resumen_diario",
    indices = [Index(value = ["opticaId", "fecha"], unique = true)],
)
data class ResumenDiarioEntity(
    @PrimaryKey val id: String,
    val opticaId: String,
    val fecha: String,
    val ventasCantidad: Int = 0,
    val ventasMontoTotal: Double = 0.0,
    val ventasCostoTotal: Double = 0.0,
    val cobrosCantidad: Int = 0,
    val cobrosMontoTotal: Double = 0.0,
    val saldoPendienteTotal: Double = 0.0,
    val saldoPendienteCantidad: Int = 0,
    val inventarioValor: Double? = null,
    val inventarioUnidades: Int? = null,
    val calculadoEn: String? = null,
)
