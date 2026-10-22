package com.example.optoapp.ui.components.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.optoapp.R
import com.example.optoapp.util.MoneyFormatter
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer

@Composable
fun OptoBarChart(
    title: String,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
) {
    val plan = remember(series, stacked) { BarChartPlan.from(series, stacked) }
    val colors = chartSeriesColors()
    val emptyState = ChartEmptyStateModel.forSeries(title, series)
    val canRenderChart = !emptyState.isEmpty && plan.labels.isNotEmpty() && plan.series.isNotEmpty() && plan.values.isNotEmpty()
    ChartFrame(
        title = title,
        model = emptyState.let { if (it.isEmpty) it else it.copy(summary = plan.contentDescription(title)) },
        table = remember(series) { ChartDataTableState.fromSeries(plan.series) },
        labelHeader = stringResource(R.string.chart_columna_periodo),
        legend = plan.series.withChartColors(colors) { it.name },
        modifier = modifier,
    ) {
        if (!canRenderChart) {
            ChartEmptyState(message = emptyState.summary)
            return@ChartFrame
        }
        // WHY: Reusing one Vico ModelProducer across Semanal(7)/Mensual(~5)/Anual(12) x-sizes
        // native-crashes the process when the user switches periodo in Reportes → Ventas.
        key(plan.labels.size, plan.series.size, plan.stacked, title) {
            val producer = remember { CartesianChartModelProducer() }
            LaunchedEffect(plan) {
                producer.runTransaction { columnSeries { plan.values.forEach { series(it) } } }
            }
            val columns = List(plan.series.size) { index ->
                rememberLineComponent(fill(colors[index % colors.size]), thickness = 16.dp)
            }
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProvider = ColumnCartesianLayer.ColumnProvider.series(columns),
                        mergeMode = { if (plan.stacked) ColumnCartesianLayer.MergeMode.Stacked else ColumnCartesianLayer.MergeMode.Grouped() },
                        rangeProvider = remember(plan) { CartesianLayerRangeProvider.fixed(minY = plan.range.min, maxY = plan.range.max) },
                    ),
                    startAxis = VerticalAxis.rememberStart(valueFormatter = moneyAxisFormatter),
                    bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = labelAxisFormatter(plan.labels)),
                ),
                modelProducer = producer,
                modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT),
            )
        }
    }
}

internal val CHART_HEIGHT = 220.dp

internal val moneyAxisFormatter = CartesianValueFormatter { _, value, _ -> MoneyFormatter.formatAbbreviated(value) }

internal fun labelAxisFormatter(labels: List<String>) =
    CartesianValueFormatter { _, value, _ -> labels.getOrElse(value.toInt()) { "" } }
