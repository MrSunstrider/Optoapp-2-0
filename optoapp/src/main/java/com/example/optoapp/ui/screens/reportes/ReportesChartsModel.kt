package com.example.optoapp.ui.screens.reportes

import com.example.optoapp.domain.ClinicoRefraccion
import com.example.optoapp.domain.ClinicoReporte
import com.example.optoapp.domain.EquipoReporte
import com.example.optoapp.domain.InventarioBucket
import com.example.optoapp.domain.InventarioReporte
import com.example.optoapp.domain.PacientesReporte
import com.example.optoapp.domain.PeriodComparison
import com.example.optoapp.domain.PeriodSeriesComparison
import com.example.optoapp.ui.components.charts.ChartPoint
import com.example.optoapp.ui.components.charts.ChartSeries
import com.example.optoapp.ui.components.charts.DonutSlice

object ReportesChartsModel {
    fun sexoSlices(
        reporte: PacientesReporte,
        hombresLabel: String,
        mujeresLabel: String,
        sinDatoLabel: String,
    ): List<DonutSlice> = listOf(
        DonutSlice(hombresLabel, reporte.hombres.toDouble()),
        DonutSlice(mujeresLabel, reporte.mujeres.toDouble()),
        DonutSlice(sinDatoLabel, reporte.sinSexo.toDouble()),
    )

    fun edadSlices(
        reporte: PacientesReporte,
        pediatricosLabel: String,
        adultosLabel: String,
        adultosMayoresLabel: String,
    ): List<DonutSlice> = listOf(
        DonutSlice(pediatricosLabel, reporte.pediatricos.toDouble()),
        DonutSlice(adultosLabel, reporte.adultos.toDouble()),
        DonutSlice(adultosMayoresLabel, reporte.adultosMayores.toDouble()),
    )

    fun inventarioEstado(
        reporte: InventarioReporte,
        okLabel: String,
        bajoLabel: String,
    ): List<DonutSlice> {
        val ok = (reporte.modelos - reporte.lowStock).coerceAtLeast(0)
        return listOf(
            DonutSlice(okLabel, ok.toDouble()),
            DonutSlice(bajoLabel, reporte.lowStock.toDouble()),
        )
    }

    fun inventarioBuckets(buckets: List<InventarioBucket>): List<DonutSlice> =
        buckets.filter { it.unidades > 0 }.map { DonutSlice(it.label, it.unidades.toDouble()) }

    fun inventarioMovimientos(
        reporte: InventarioReporte,
        labelOf: (String) -> String,
    ): List<DonutSlice> = inventarioBuckets(
        reporte.movimientosPorTipo.map { it.copy(label = labelOf(it.label)) },
    )

    fun clinicoDiagnosticos(reporte: ClinicoReporte, top: Int = 6, otrosLabel: String): List<DonutSlice> {
        if (reporte.porDiagnostico.isEmpty()) return emptyList()
        // Keep core refraction groups visible even when a long tail would push them into Otros.
        val pinnedNames = setOf(
            ClinicoRefraccion.MIOPIA,
            ClinicoRefraccion.HIPERMETROPIA,
            ClinicoRefraccion.EMETROPIA,
            ClinicoRefraccion.ASTIGMATISMO,
        )
        val all = reporte.porDiagnostico.filter { it.second > 0 }
        val pinned = all.filter { it.first in pinnedNames }
        val remaining = all.filter { it.first !in pinnedNames }
        val slots = (top - pinned.size).coerceAtLeast(0)
        val head = (pinned + remaining.take(slots)).sortedByDescending { it.second }
        val rest = remaining.drop(slots).sumOf { it.second }
        val slices = head.map { DonutSlice(it.first, it.second.toDouble()) }.toMutableList()
        if (rest > 0) slices += DonutSlice(otrosLabel, rest.toDouble())
        return slices
    }

    fun equipoVendido(reporte: EquipoReporte, serieName: String, sinAtribuirLabel: String): List<ChartSeries> {
        val points = buildList {
            reporte.filas.forEach { add(ChartPoint(it.displayName.take(12), it.vendido)) }
            if (reporte.sinAtribuirVendido > 0) {
                add(ChartPoint(sinAtribuirLabel, reporte.sinAtribuirVendido))
            }
        }
        if (points.isEmpty()) return emptyList()
        return listOf(ChartSeries(serieName, points))
    }

    fun ventasComparativo(
        cmp: PeriodComparison,
        actualLabel: String,
        anteriorLabel: String,
        vendidoLabel: String,
        margenLabel: String,
    ): List<ChartSeries> = listOf(
        ChartSeries(
            actualLabel,
            listOf(
                ChartPoint(vendidoLabel, cmp.currentVendido),
                ChartPoint(margenLabel, cmp.currentMargen),
            ),
        ),
        ChartSeries(
            anteriorLabel,
            listOf(
                ChartPoint(vendidoLabel, cmp.previousVendido),
                ChartPoint(margenLabel, cmp.previousMargen),
            ),
        ),
    )

    fun ventasSerieComparativo(
        series: PeriodSeriesComparison,
        actualLabel: String,
        anteriorLabel: String,
    ): List<ChartSeries> = listOf(
        ChartSeries(actualLabel, series.labels.mapIndexed { i, label -> ChartPoint(label, series.currentValues[i]) }),
        ChartSeries(anteriorLabel, series.labels.mapIndexed { i, label -> ChartPoint(label, series.previousValues[i]) }),
    )

    fun ventasMix(
        dispensacionesMonto: Double,
        serviciosMonto: Double,
        otLabel: String,
        serviciosLabel: String,
    ): List<DonutSlice> = listOf(
        DonutSlice(otLabel, dispensacionesMonto),
        DonutSlice(serviciosLabel, serviciosMonto),
    )
}
