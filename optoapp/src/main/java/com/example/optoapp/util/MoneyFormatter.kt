package com.example.optoapp.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {
    private const val SYMBOL = "S/ "
    const val NOT_AVAILABLE = "\u2014"
    private val THOUSAND = BigDecimal(1_000)
    private val MILLION = BigDecimal(1_000_000)

    fun format(amount: Double): String {
        if (!amount.isFinite()) return NOT_AVAILABLE
        val rounded = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)
        return signOf(rounded) + SYMBOL + twoDecimals().format(rounded.abs())
    }

    fun formatAbbreviated(amount: Double): String {
        if (!amount.isFinite()) return NOT_AVAILABLE
        val value = BigDecimal.valueOf(amount)
        val magnitude = value.abs()
        if (magnitude < THOUSAND) return format(amount)
        val inThousands = scaleTo(magnitude, THOUSAND)
        val text = if (magnitude >= MILLION || inThousands >= THOUSAND) {
            scaleTo(magnitude, MILLION).toPlainString() + "M"
        } else {
            inThousands.toPlainString() + "k"
        }
        return signOf(value) + SYMBOL + text
    }

    private fun scaleTo(magnitude: BigDecimal, unit: BigDecimal): BigDecimal =
        magnitude.divide(unit).setScale(1, RoundingMode.HALF_UP)

    private fun signOf(value: BigDecimal): String = if (value.signum() < 0) "-" else ""

    // DecimalFormat is not thread-safe, and es-PE separators differ across JDK and Android
    // locale data, so the product format pins them explicitly on a fresh instance per call.
    private fun twoDecimals(): DecimalFormat {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("es-PE")).apply {
            decimalSeparator = '.'
            groupingSeparator = ','
        }
        return DecimalFormat("#,##0.00", symbols).apply { roundingMode = RoundingMode.HALF_UP }
    }
}
