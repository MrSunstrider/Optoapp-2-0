package com.example.optoapp.ui.screens.reportes

import com.example.optoapp.domain.ClinicoReporte
import com.example.optoapp.domain.EquipoReporte
import com.example.optoapp.domain.InventarioReporte
import com.example.optoapp.domain.PacientesReporte
import com.example.optoapp.domain.PeriodComparison
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

    fun clinicoDiagnosticos(reporte: ClinicoReporte, top: Int = 6, otrosLabel: String): List<DonutSlice> {
        if (reporte.porDiagnostico.isEmpty()) return emptyList()
        val head = reporte.porDiagnostico.take(top)
        val rest = reporte.porDiagnostico.drop(top).sumOf { it.second }
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
