package com.example.optoapp.ui.screens.costos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.optoapp.R
import com.example.optoapp.domain.MarginFacts
import com.example.optoapp.ui.components.charts.ChartEmptyState
import com.example.optoapp.ui.components.charts.ChartPoint
import com.example.optoapp.ui.components.charts.ChartSeries
import com.example.optoapp.ui.components.charts.DonutSlice
import com.example.optoapp.ui.components.charts.OptoBarChart
import com.example.optoapp.ui.components.charts.OptoDonutChart
import com.example.optoapp.ui.components.common.CostCoverageBadge
import com.example.optoapp.ui.components.common.CostCoverageBadgeModel
import java.util.Locale

data class ComponentesCostoCopy(
    val serie: String,
    val lentes: String,
    val montura: String,
    val lc: String,
    val biselado: String,
    val servicios: String,
    val regalos: String,
)

sealed interface ComponentesCostoChart {
    data object SinConexion : ComponentesCostoChart
    data class Serie(val series: List<ChartSeries>) : ComponentesCostoChart
}

object CostosChartsModel {
    fun gastosPorCategoria(facts: MarginFacts): List<DonutSlice> =
        facts.gastosPorCategoria.map { gasto ->
            DonutSlice(gasto.categoria.replaceFirstChar { it.titlecase(Locale.ROOT) }, gasto.monto)
        }

    // Offline facts carry no per-component detail; drawing their zero defaults would read as "no cost".
    fun componentes(facts: MarginFacts, copy: ComponentesCostoCopy): ComponentesCostoChart {
        if (!facts.detalleServidorDisponible) return ComponentesCostoChart.SinConexion
        val linea = facts.costosPorLinea
        val servicios = linea.serviciosPropios + linea.serviciosTercerizados + linea.serviciosSinClasificar
        // WHY: omit zero bars so short X labels (LC, etc.) stay aligned under visible columns
        val puntos = listOf(
            ChartPoint(copy.lentes, linea.lentes),
            ChartPoint(copy.montura, linea.montura),
            ChartPoint(copy.lc, linea.lc),
            ChartPoint(copy.biselado, linea.biselado),
            ChartPoint(copy.servicios, servicios),
            ChartPoint(copy.regalos, linea.regalos),
        ).filter { it.value > 0.0 }
        return ComponentesCostoChart.Serie(listOf(ChartSeries(copy.serie, puntos)))
    }

    fun cobertura(facts: MarginFacts): CostCoverageBadgeModel =
        CostCoverageBadgeModel.from(facts.cobertura, facts.fuente)
}

@Composable
fun CostosCharts(facts: MarginFacts, modifier: Modifier = Modifier) {
    val copy = ComponentesCostoCopy(
        serie = stringResource(R.string.costos_componente_serie),
        lentes = stringResource(R.string.costos_componente_lentes),
        montura = stringResource(R.string.costos_componente_montura),
        lc = stringResource(R.string.costos_componente_lc),
        biselado = stringResource(R.string.costos_componente_biselado),
        servicios = stringResource(R.string.costos_componente_servicios),
        regalos = stringResource(R.string.costos_componente_regalos),
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CostCoverageBadge(cobertura = facts.cobertura, fuente = facts.fuente)
        OptoDonutChart(
            title = stringResource(R.string.costos_chart_gastos_titulo),
            slices = CostosChartsModel.gastosPorCategoria(facts),
        )
        when (val componentes = CostosChartsModel.componentes(facts, copy)) {
            ComponentesCostoChart.SinConexion -> ChartEmptyState(
                message = stringResource(R.string.costos_chart_componentes_offline),
            )
            is ComponentesCostoChart.Serie -> OptoBarChart(
                title = stringResource(R.string.costos_chart_componentes_titulo),
                series = componentes.series,
            )
        }
    }
}
