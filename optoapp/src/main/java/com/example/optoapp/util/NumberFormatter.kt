package com.example.optoapp.util

import java.util.Locale

object NumberFormatter {
    fun formatCurrency(value: Double): String = if (value == value.toLong().toDouble()) {
        String.format(Locale.getDefault(), "%,.0f", value)
    } else {
        String.format(Locale.getDefault(), "%,.1f", value)
    }
}
