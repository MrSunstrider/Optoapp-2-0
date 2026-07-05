package com.example.optoapp.data.configuracionfinanciera

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configuracion_financiera")
data class ConfiguracionFinancieraEntity(
    @PrimaryKey val opticaId: String,
    val margenNetoObjetivo: Double = 15.0,
    val ticketPromedioObjetivo: Double? = null,
    val caidaVentasAlertaPct: Double = 10.0,
    val deudaViejaAlertaDias: Int = 30,
    val deudaTotalAlertaMonto: Double = 3000.0,
    val stockEstancadoAlertaDias: Int = 180,
    val stockBajoAlertaUnidades: Int = 2,
    val minVentasParaRecomendar: Int = 5,
    val frecuenciaRecalculoDias: Int = 1
)
