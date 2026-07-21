package com.example.optoapp.util

import java.util.Locale

/**
 * Formats a [Double] value with locale-aware thousand separators.
 *
 * - Whole numbers → no decimal places (e.g. "1,234")
 * - Decimals → two decimal places (e.g. "1,234.56")
 */
fun Double.fmt(): String = if (this == this.toLong().toDouble()) {
    String.format(Locale.US, "%,.0f", this)
} else {
    String.format(Locale.US, "%,.2f", this)
}

/** Formats a [Double] as currency with "S/ " prefix and two decimals. */
fun Double.formatAsCurrency(): String = "S/ %.2f".format(this)

/** Formats an [Int] with locale-aware thousand separators (e.g. "1,234"). */
fun Int.formatAsInteger(): String = "%,d".format(this)
