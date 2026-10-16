package com.example.optoapp.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Weighted average frame cost after a purchase-order receipt.
 *
 * A previous cost of zero means "never costed" rather than "free", so it is replaced instead of
 * averaged; otherwise one uncosted legacy frame would halve the cost of every later receipt.
 */
object CostoPromedioPonderado {
    fun calcular(stockPrevio: Int, costoPrevio: Double?, cantidad: Int, costoPo: Double): Double {
        if (cantidad <= 0 || costoPo <= 0.0) return costoPrevio ?: 0.0
        if (stockPrevio <= 0 || costoPrevio == null || costoPrevio <= 0.0) return costoPo
        val valorPrevio = BigDecimal.valueOf(stockPrevio.toLong()).multiply(BigDecimal.valueOf(costoPrevio))
        val valorEntrada = BigDecimal.valueOf(cantidad.toLong()).multiply(BigDecimal.valueOf(costoPo))
        return valorPrevio.add(valorEntrada)
            .divide(BigDecimal.valueOf(stockPrevio.toLong() + cantidad), 2, RoundingMode.HALF_UP)
            .toDouble()
    }
}
