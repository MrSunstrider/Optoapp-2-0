package com.example.optoapp.ui.screens

/** Calendar button for Reportes: only Diario/Semanal are date-anchored. */
fun reportesShowsDatePicker(periodo: String): Boolean =
    periodo == "Diario" || periodo == "Semanal"
