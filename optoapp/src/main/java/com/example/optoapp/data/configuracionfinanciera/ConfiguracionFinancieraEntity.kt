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
    val frecuenciaRecalculoDias: Int = 1,
    /** Default Propio | Tercero for new OT product lines. */
    val origenBiseladoDefault: String = "Tercero",
    /** Internal pair rate when origen biselado is Propio; 0 means no COGS. */
    val costoBiseladoPropio: Double = 0.0,
    /** Plan-de-acción impact assumptions (local; preserved across config download merge). */
    val impactoMargenObjetivoPct: Double = 25.0,
    val impactoRecuperoStockPct: Double = 0.70,
    val impactoUpsideVentasPct: Double = 0.10,
    val impactoCorteGastoPct: Double = 0.10,
    val impactoRecuperoCobranzaPct: Double = 1.0,
    /** Local alert gates (preserved across config download merge). */
    val margenBajoAlertaPct: Double = 10.0,
    val margenAltoAlertaPct: Double = 35.0,
    val contribucionAltaRatio: Double = 0.25,
    val gastoFueraRangoRatio: Double = 0.4,
)
