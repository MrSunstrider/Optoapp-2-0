package com.example.optoapp.domain

/**
 * Pure month P&L lines: Ventas − COGS − Gastos = Utilidad.
 */
data class AnalisisPnL(
    val ventas: Double,
    val cogs: Double,
    val gastos: Double,
    val utilidad: Double,
)

object AnalisisPnLCalculator {
    fun fromAnalisis(analisis: AnalisisMensual): AnalisisPnL {
        val cogs = analisis.costoDeVentas()
        val utilidad = analisis.ventasMes - cogs - analisis.gastosMes
        return AnalisisPnL(
            ventas = analisis.ventasMes,
            cogs = cogs,
            gastos = analisis.gastosMes,
            utilidad = utilidad,
        )
    }
}
