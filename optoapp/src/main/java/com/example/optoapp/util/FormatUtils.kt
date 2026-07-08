package com.example.optoapp.util

import java.util.Locale

/**
 * Formats a [Double] value with locale-aware thousand separators.
 *
 * - Whole numbers → no decimal places (e.g. "1,234")
 * - Decimals → two decimal places (e.g. "1,234.56")
 */
fun Double.fmt(): String {
    return if (this == this.toLong().toDouble()) {
        String.format(Locale.getDefault(), "%,.0f", this)
    } else {
        String.format(Locale.getDefault(), "%,.2f", this)
    }
}
