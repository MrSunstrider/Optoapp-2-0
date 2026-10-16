package com.example.optoapp.util

import com.example.optoapp.domain.ClinicoReporte
import com.example.optoapp.domain.EquipoReporte
import com.example.optoapp.domain.InventarioReporte
import com.example.optoapp.domain.PacientesReporte
import com.example.optoapp.domain.PeriodComparison
import java.util.Locale

/**
 * Optional rich-report sections for PDF export (RF-REP-03..06 + Equipo).
 * Pure lines — no Android/PdfDocument — so unit tests stay Robolectric-free.
 */
data class ReportePdfExtras(
    val comparison: PeriodComparison? = null,
    val pacientes: PacientesReporte? = null,
    val inventario: InventarioReporte? = null,
    val clinico: ClinicoReporte? = null,
    val equipo: EquipoReporte? = null,
)

object ReportePdfSectionLines {
    fun build(extras: ReportePdfExtras): List<String> {
        val lines = mutableListOf<String>()
        extras.comparison?.let { cmp ->
            lines += "COMPARATIVO VS PERIODO ANTERIOR"
            lines += "Vendido delta: S/ ${cmp.vendidoDelta.toMoney()} (${pctLabel(cmp.vendidoDeltaPct())})"
            lines += "Margen delta: S/ ${cmp.margenDelta.toMoney()} (${pctLabel(cmp.margenDeltaPct())})"
            lines += "Transacciones delta: ${cmp.transaccionesDelta}"
            lines += ""
        }
        extras.pacientes?.let { p ->
            lines += "PACIENTES"
            lines += "Altas: ${p.altas}"
            lines += "Hombres ${p.hombres} · Mujeres ${p.mujeres} · Sin dato ${p.sinSexo}"
            lines += ""
        }
        extras.inventario?.let { inv ->
            lines += "INVENTARIO"
            lines += "Modelos: ${inv.modelos}"
            lines += "Stock total: ${inv.stockTotal}"
            lines += "Stock bajo: ${inv.lowStock}"
            lines += "Salidas por venta: ${inv.salidasVentaPeriodo}"
            lines += ""
        }
        extras.clinico?.let { c ->
            lines += "CLINICO"
            lines += "Evaluaciones: ${c.evaluaciones}"
            c.porDiagnostico.take(12).forEach { (label, count) ->
                lines += "$label: $count"
            }
            lines += ""
        }
        extras.equipo?.let { e ->
            lines += "EQUIPO"
            lines += "Comision %: ${String.format(Locale.US, "%.1f", e.comisionPct)}"
            e.filas.forEach { row ->
                lines += "${row.displayName}: S/ ${row.vendido.toMoney()} (comision S/ ${row.comision.toMoney()})"
            }
            if (e.sinAtribuirCount > 0) {
                lines += "Sin atribuir: S/ ${e.sinAtribuirVendido.toMoney()} (${e.sinAtribuirCount})"
            }
            lines += ""
        }
        return lines.dropLastWhile { it.isEmpty() }
    }

    private fun pctLabel(pct: Double?): String =
        pct?.let { String.format(Locale.US, "%.1f%%", it) } ?: "—"

    private fun Double.toMoney(): String =
        String.format(Locale.forLanguageTag("es-PE"), "%.2f", this)
}
