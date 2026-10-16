package com.example.optoapp.ui.components.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.optoapp.R
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape

@Composable
fun OptoLineChart(
    title: String,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    target: Double? = null,
) {
    val plan = remember(series, target) { LineChartPlan.from(series, target) }
    val colors = chartSeriesColors()
    val labels = remember(plan) { chartXLabels(plan.series) }
    ChartFrame(
        title = title,
        model = ChartEmptyStateModel.forSeries(title, series).let { if (it.isEmpty) it else it.copy(summary = plan.contentDescription(title)) },
        table = remember(series) { ChartDataTableState.fromSeries(plan.series) },
        labelHeader = stringResource(R.string.chart_columna_periodo),
        legend = plan.series.withChartColors(colors) { it.name },
        modifier = modifier,
    ) {
        val producer = remember { CartesianChartModelProducer() }
        LaunchedEffect(plan) {
            producer.runTransaction {
                lineSeries {
                    plan.series.forEach { s -> series(x = s.points.map { labels.indexOf(it.label) }, y = s.points.map { it.value }) }
                }
            }
        }
        val lines = plan.series.indices.map { index ->
            val color = colors[index % colors.size]
            LineCartesianLayer.rememberLine(
                fill = LineCartesianLayer.LineFill.single(fill(color)),
                pointProvider = if (plan.showsMarkers) {
                    LineCartesianLayer.PointProvider.single(
                        LineCartesianLayer.point(rememberShapeComponent(fill(color), CorneredShape.Pill), size = 8.dp),
                    )
                } else {
                    null
                },
            )
        }
        val targetColor = MaterialTheme.colorScheme.onSurfaceVariant
        val targetStroke = rememberLineComponent(fill(targetColor), thickness = 2.dp)
        val decorations = remember(plan.targetLine, targetStroke) {
            plan.targetLine?.let { y -> listOf(HorizontalLine(y = { y }, line = targetStroke)) } ?: emptyList()
        }
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(lines),
                    rangeProvider = remember(plan) { CartesianLayerRangeProvider.fixed(minY = plan.range.min, maxY = plan.range.max) },
                ),
                startAxis = VerticalAxis.rememberStart(valueFormatter = moneyAxisFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = labelAxisFormatter(labels)),
                decorations = decorations,
            ),
            modelProducer = producer,
            modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT),
        )
    }
}
