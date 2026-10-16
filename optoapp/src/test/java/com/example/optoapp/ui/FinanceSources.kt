package com.example.optoapp.ui

import java.io.File

object FinanceSources {
    private const val UI = "java/com/example/optoapp/ui"

    private val screens = listOf(
        "AnalisisDetalleScreen.kt",
        "AnalisisNegocioScreen.kt",
        "CostosYGastosScreen.kt",
        "NuevaDispensacionScreen.kt",
        "NuevoServicioScreen.kt",
        "ReportesScreen.kt",
        "ServiciosExtraScreen.kt",
        "dispensacion/CostoMargenOtSection.kt",
    )

    private val componentFiles = listOf(
        "common/CostCoverageBadge.kt",
        "common/GastoFormDialog.kt",
        "common/MetricTile.kt",
        "common/MonthSelector.kt",
    )

    // The composables this change created or redesigned; untouched legacy screens migrate separately.
    fun files(): List<File> =
        screens.map { MainSources.file("$UI/screens/$it") } +
            MainSources.kotlinFilesUnder("$UI/screens/costos") +
            MainSources.kotlinFilesUnder("$UI/components/charts") +
            MainSources.kotlinFilesUnder("$UI/components/servicio") +
            componentFiles.map { MainSources.file("$UI/components/$it") }
}
