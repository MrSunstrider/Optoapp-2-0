package com.example.optoapp.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.optoapp.ui.theme.alertRed
import com.example.optoapp.ui.theme.positiveGreen
import com.example.optoapp.ui.theme.warningAmber

data class ChartLegendEntry(val label: String, val color: Color)

// Only tokens covered by FinanceColorContrastTest (>= 3:1 on light and dark surfaces) are used;
// series beyond four repeat colors and stay distinguishable through the legend labels.
@Composable
fun chartSeriesColors(): List<Color> {
    val scheme = MaterialTheme.colorScheme
    return listOf(scheme.primary, scheme.warningAmber, scheme.positiveGreen, scheme.alertRed)
}

fun <T> List<T>.withChartColors(colors: List<Color>, label: (T) -> String): List<ChartLegendEntry> =
    mapIndexed { index, item -> ChartLegendEntry(label(item), colors[index % colors.size]) }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartLegend(entries: List<ChartLegendEntry>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        entries.forEach { entry ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).background(entry.color, CircleShape))
                Text(entry.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
