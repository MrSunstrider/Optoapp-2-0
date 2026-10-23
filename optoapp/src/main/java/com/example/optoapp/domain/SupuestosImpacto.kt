package com.example.optoapp.domain

import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import com.example.optoapp.util.AnalisisPercentFormatter

data class SupuestosImpacto(
    val margenObjetivoPct: Double = DEFAULT_MARGEN_OBJETIVO_PCT,
    val recuperoStockPct: Double = DEFAULT_RECUPERO_STOCK_PCT,
    val upsideVentasPct: Double = DEFAULT_UPSIDE_VENTAS_PCT,
    val corteGastoPct: Double = DEFAULT_CORTE_GASTO_PCT,
    val recuperoCobranzaPct: Double = DEFAULT_RECUPERO_COBRANZA_PCT,
) {
    fun labelMejorarPrecio(): String =
        "Supuesto: subir margen a ${AnalisisPercentFormatter.format(margenObjetivoPct)}% con el mismo volumen"

    fun labelLiquidarStock(): String =
        "Supuesto: recupero ${AnalisisPercentFormatter.format(recuperoStockPct * 100)}% del costo en stock"

    fun labelVenderMasDe(): String =
        "Supuesto: +${AnalisisPercentFormatter.format(upsideVentasPct * 100)}% ventas al margen actual"

    fun labelReducirGasto(): String =
        "Supuesto: recortar ${AnalisisPercentFormatter.format(corteGastoPct * 100)}% de opex"

    fun labelCobrar(): String =
        "Supuesto: recupero ${AnalisisPercentFormatter.format(recuperoCobranzaPct * 100)}% de la deuda"

    companion object {
        const val DEFAULT_MARGEN_OBJETIVO_PCT = 25.0
        const val DEFAULT_RECUPERO_STOCK_PCT = 0.70
        const val DEFAULT_UPSIDE_VENTAS_PCT = 0.10
        const val DEFAULT_CORTE_GASTO_PCT = 0.10
        const val DEFAULT_RECUPERO_COBRANZA_PCT = 1.0

        fun from(config: ConfiguracionFinancieraEntity): SupuestosImpacto = SupuestosImpacto(
            margenObjetivoPct = config.impactoMargenObjetivoPct,
            recuperoStockPct = config.impactoRecuperoStockPct,
            upsideVentasPct = config.impactoUpsideVentasPct,
            corteGastoPct = config.impactoCorteGastoPct,
            recuperoCobranzaPct = config.impactoRecuperoCobranzaPct,
        )
    }
}

enum class EsfuerzoRecomendacion { BAJO, MEDIO, ALTO }

enum class CtaRecomendacion {
    DEUDORES,
    COSTOS,
    DETALLE,
    NINGUNA,
}
