package com.example.optoapp.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.optoapp.R

private const val FULL_CIRCLE = 360f
private const val START_ANGLE = -90f

// Vico 2.x has no pie layer, so the donut is drawn directly on a Canvas from the same
// DonutPlan that feeds the summary and the "Ver datos" table.
@Composable
fun OptoDonutChart(
    title: String,
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
) {
    val plan = remember(slices) { DonutPlan.from(slices) }
    val colors = chartSeriesColors()
    val amountHeader = stringResource(R.string.chart_columna_monto)
    val shareHeader = stringResource(R.string.chart_columna_porcentaje)
    ChartFrame(
        title = title,
        model = ChartEmptyStateModel.forSlices(title, slices),
        table = remember(slices, amountHeader, shareHeader) { ChartDataTableState.fromSlices(slices, amountHeader, shareHeader) },
        labelHeader = stringResource(R.string.chart_columna_categoria),
        legend = plan.slices.withChartColors(colors) { "${it.label} ${ChartSummaryBuilder.percent(it.amount, plan.total)}" },
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(160.dp)) {
                val stroke = 28.dp.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                var start = START_ANGLE
                plan.slices.forEachIndexed { index, slice ->
                    val sweep = (slice.share * FULL_CIRCLE).toFloat()
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke),
                    )
                    start += sweep
                }
            }
        }
    }
}
