package com.example.optoapp.domain

/**
 * Period-over-period deltas for Reportes Ventas (RF-REP-04).
 * Positive [deltaPct] means the current period is higher than the previous window.
 */
data class PeriodComparison(
    val currentVendido: Double,
    val previousVendido: Double,
    val currentMargen: Double,
    val previousMargen: Double,
    val currentTransacciones: Int,
    val previousTransacciones: Int,
) {
    val vendidoDelta: Double get() = currentVendido - previousVendido
    val margenDelta: Double get() = currentMargen - previousMargen
    val transaccionesDelta: Int get() = currentTransacciones - previousTransacciones

    fun vendidoDeltaPct(): Double? = pct(currentVendido, previousVendido)
    fun margenDeltaPct(): Double? = pct(currentMargen, previousMargen)

    companion object {
        fun pct(current: Double, previous: Double): Double? {
            if (previous == 0.0) return if (current == 0.0) 0.0 else null
            return ((current - previous) / previous) * 100.0
        }

        fun fromMovimientos(
            current: List<MovimientoFinanciero>,
            previous: List<MovimientoFinanciero>,
        ): PeriodComparison {
            fun vendido(rows: List<MovimientoFinanciero>) = rows.sumOf { it.montoTotal }
            fun margen(rows: List<MovimientoFinanciero>) =
                rows.filterNot { it.costoIncompleto }.sumOf { it.montoTotal - it.costo }
            return PeriodComparison(
                currentVendido = vendido(current),
                previousVendido = vendido(previous),
                currentMargen = margen(current),
                previousMargen = margen(previous),
                currentTransacciones = current.size,
                previousTransacciones = previous.size,
            )
        }
    }
}
