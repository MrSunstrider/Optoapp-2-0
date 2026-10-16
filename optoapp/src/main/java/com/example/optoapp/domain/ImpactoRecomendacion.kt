package com.example.optoapp.domain

import com.example.optoapp.util.MoneyFormatter

data class ImpactoEstimado(
    val etiqueta: String,
    val monto: Double?,
)

/**
 * Estimates actionable money impact for each recommendation family.
 * Labels are coach-facing; [monto] drives ranking when present.
 */
object ImpactoRecomendacion {

    const val SIN_IMPACTO = "Sin impacto estimable"

    private const val MARGEN_OBJETIVO_PRECIO = 25.0
    private const val RECUPERO_STOCK_PCT = 0.70
    private const val UPSIDE_VENTAS_PCT = 0.10
    private const val CORTE_GASTO_PCT = 0.10

    fun cobrar(deudaTotal: Double): ImpactoEstimado {
        val monto = deudaTotal.coerceAtLeast(0.0)
        return ImpactoEstimado(
            etiqueta = "${MoneyFormatter.format(monto)} si se cobra todo",
            monto = monto,
        )
    }

    fun mejorarPrecio(ventas: Double, margenPct: Double?, targetPct: Double = MARGEN_OBJETIVO_PRECIO): ImpactoEstimado? {
        if (ventas <= 0.0 || margenPct == null) return null
        if (margenPct >= targetPct) return null
        val monto = ventas * ((targetPct - margenPct) / 100.0)
        if (monto <= 0.0) return null
        return ImpactoEstimado(
            etiqueta = "Hasta ${MoneyFormatter.format(monto)} más de margen si subes al ${targetPct.toInt()}%",
            monto = monto,
        )
    }

    fun liquidarStock(costoTotal: Double): ImpactoEstimado {
        val monto = (costoTotal.coerceAtLeast(0.0) * RECUPERO_STOCK_PCT)
        return ImpactoEstimado(
            etiqueta = "Hasta ${MoneyFormatter.format(monto)} de capital liberado (estimado)",
            monto = monto,
        )
    }

    fun venderMasDe(ventas: Double, margenPct: Double?): ImpactoEstimado? {
        if (ventas <= 0.0 || margenPct == null || margenPct <= 0.0) return null
        val monto = ventas * UPSIDE_VENTAS_PCT * (margenPct / 100.0)
        if (monto <= 0.0) return null
        return ImpactoEstimado(
            etiqueta = "Hasta ${MoneyFormatter.format(monto)} más de margen con +10% de ventas",
            monto = monto,
        )
    }

    fun alertaCaida(ventasActual: Double, ventasEsperado: Double): ImpactoEstimado? {
        val gap = ventasEsperado - ventasActual
        if (gap <= 0.0) return null
        return ImpactoEstimado(
            etiqueta = "${MoneyFormatter.format(gap)} por debajo de lo esperado",
            monto = gap,
        )
    }

    fun reducirGasto(gastosMes: Double): ImpactoEstimado {
        val monto = gastosMes.coerceAtLeast(0.0) * CORTE_GASTO_PCT
        return ImpactoEstimado(
            etiqueta = "${MoneyFormatter.format(monto)} si reduces un 10% los gastos",
            monto = monto,
        )
    }
}
