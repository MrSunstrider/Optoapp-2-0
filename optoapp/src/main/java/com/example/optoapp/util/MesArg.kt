package com.example.optoapp.util

import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object MesArg {
    const val KEY = "mes"
    private val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM")

    fun format(mes: YearMonth): String = mes.format(FORMAT)

    fun parseOrCurrent(raw: String?, currentMonth: YearMonth): YearMonth {
        val parsed = raw?.let {
            try {
                YearMonth.parse(it, FORMAT)
            } catch (e: DateTimeParseException) {
                null
            }
        } ?: return currentMonth
        return minOf(parsed, currentMonth)
    }
}
