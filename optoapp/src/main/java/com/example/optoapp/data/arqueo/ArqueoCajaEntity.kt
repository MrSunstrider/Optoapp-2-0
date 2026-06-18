package com.example.optoapp.data.arqueo

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "arqueo_caja",
    indices = [Index(value = ["fecha", "opticaId"], unique = true)]
)
data class ArqueoCaja(
    @PrimaryKey val id: String,
    val fecha: LocalDate,
    val opticaId: String,
    val fondoCaja: Double,
    val efectivoContado: Double,
    val tarjetaContado: Double,
    val transferenciaContado: Double,
    val movilContado: Double,
    val efectivoCobrado: Double,
    val tarjetaCobrado: Double,
    val transferenciaCobrado: Double,
    val movilCobrado: Double,
    val diferenciaEfectivo: Double,
    val diferenciaTarjeta: Double,
    val diferenciaTransferencia: Double,
    val diferenciaMovil: Double,
    val diferenciaTotal: Double,
    val cerradoPor: String,
    val sellado: Boolean = false,
    val createdAt: String,   // ISO-8601 Instant as String
    val updatedAt: String,   // ISO-8601 Instant as String
    val updatedBy: String = ""
)
