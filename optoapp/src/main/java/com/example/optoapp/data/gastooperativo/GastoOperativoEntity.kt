package com.example.optoapp.data.gastooperativo

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate

@Entity(
    tableName = "gastos_operativos",
    indices = [Index(value = ["opticaId"])],
)
data class GastoOperativoEntity(
    @PrimaryKey val id: String,
    val opticaId: String,
    val categoria: String,
    val descripcion: String? = null,
    val monto: BigDecimal,
    val fecha: LocalDate,
    val fechaProgramada: LocalDate? = null,
    val nota: String? = null,
    val isRecurring: Boolean = false,
    val frecuencia: String = "mensual",
    val createdAt: String? = null,
)
