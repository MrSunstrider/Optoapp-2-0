package com.example.optoapp.util

object NumberFormatter {
    fun formatCurrency(value: Double): String = MoneyFormatter.format(value)
}
