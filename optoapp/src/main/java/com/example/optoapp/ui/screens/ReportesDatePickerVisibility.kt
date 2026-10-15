package com.example.optoapp.ui.screens

import androidx.annotation.StringRes
import com.example.optoapp.R

// Kept untranslated because ReportesViewModel and the PDF generator match on these exact keys.
val REPORTES_PERIODOS = listOf("Diario", "Semanal", "Mensual", "Anual", "Total")

@StringRes
fun reportesPeriodoLabelRes(periodo: String): Int = when (periodo) {
    "Diario" -> R.string.reportes_periodo_diario
    "Semanal" -> R.string.reportes_periodo_semanal
    "Mensual" -> R.string.reportes_periodo_mensual
    "Anual" -> R.string.reportes_periodo_anual
    else -> R.string.reportes_periodo_total
}

// Only Diario and Semanal are anchored to a chosen date; the other periods ignore it.
fun reportesShowsDatePicker(periodo: String): Boolean =
    periodo == "Diario" || periodo == "Semanal"
