package com.example.optoapp.util

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Single display rounding for Análisis Financiero percentages (1 decimal).
 * Stored RPC values may still use mixed precision; UI must go through here.
 */
object AnalisisPercentFormatter {

    fun format(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return String.format(Locale.US, "%.1f", rounded)
    }

    fun formatWithSymbol(value: Double): String = "${format(value)}%"
}
