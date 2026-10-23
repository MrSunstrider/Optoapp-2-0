package com.example.optoapp.domain

import com.example.optoapp.util.AnalisisPercentFormatter
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

    fun cobrar(deudaTotal: Double, supuestos: SupuestosImpacto = SupuestosImpacto()): ImpactoEstimado {
        val monto = deudaTotal.coerceAtLeast(0.0) * supuestos.recuperoCobranzaPct.coerceIn(0.0, 1.0)
        return ImpactoEstimado(
            etiqueta = "${MoneyFormatter.format(monto)} si se cobra lo estimado",
            monto = monto,
        )
    }

    fun mejorarPrecio(
        ventas: Double,
        margenPct: Double?,
        supuestos: SupuestosImpacto = SupuestosImpacto(),
    ): ImpactoEstimado? {
        val targetPct = supuestos.margenObjetivoPct
        if (ventas <= 0.0 || margenPct == null) return null
        if (margenPct >= targetPct) return null
        val monto = ventas * ((targetPct - margenPct) / 100.0)
        if (monto <= 0.0) return null
        return ImpactoEstimado(
            etiqueta = "Hasta ${MoneyFormatter.format(monto)} más de margen si subes al ${AnalisisPercentFormatter.format(targetPct)}%",
            monto = monto,
        )
    }

    fun liquidarStock(
        costoTotal: Double,
        supuestos: SupuestosImpacto = SupuestosImpacto(),
    ): ImpactoEstimado {
        val monto = costoTotal.coerceAtLeast(0.0) * supuestos.recuperoStockPct.coerceIn(0.0, 1.0)
        return ImpactoEstimado(
            etiqueta = "Hasta ${MoneyFormatter.format(monto)} de capital liberado (estimado)",
            monto = monto,
        )
    }

    fun venderMasDe(
        ventas: Double,
        margenPct: Double?,
        supuestos: SupuestosImpacto = SupuestosImpacto(),
    ): ImpactoEstimado? {
        if (ventas <= 0.0 || margenPct == null || margenPct <= 0.0) return null
        val monto = ventas * supuestos.upsideVentasPct.coerceAtLeast(0.0) * (margenPct / 100.0)
        if (monto <= 0.0) return null
        val upsideLabel = AnalisisPercentFormatter.format(supuestos.upsideVentasPct * 100)
        return ImpactoEstimado(
            etiqueta = "Hasta ${MoneyFormatter.format(monto)} más de margen con +$upsideLabel% de ventas",
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

    fun reducirGasto(
        gastosMes: Double,
        supuestos: SupuestosImpacto = SupuestosImpacto(),
    ): ImpactoEstimado {
        val monto = gastosMes.coerceAtLeast(0.0) * supuestos.corteGastoPct.coerceIn(0.0, 1.0)
        val corteLabel = AnalisisPercentFormatter.format(supuestos.corteGastoPct * 100)
        return ImpactoEstimado(
            etiqueta = "${MoneyFormatter.format(monto)} si reduces un $corteLabel% los gastos",
            monto = monto,
        )
    }
}
