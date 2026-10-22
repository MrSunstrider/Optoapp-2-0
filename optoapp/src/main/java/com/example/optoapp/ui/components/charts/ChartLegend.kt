package com.example.optoapp.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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

// WHY: only finance tokens with proven >= 3:1 contrast; extra series reuse colors and rely on labels.
@Composable
fun chartSeriesColors(): List<Color> {
    val scheme = MaterialTheme.colorScheme
    return listOf(scheme.primary, scheme.warningAmber, scheme.positiveGreen, scheme.alertRed)
}

fun <T> List<T>.withChartColors(colors: List<Color>, label: (T) -> String): List<ChartLegendEntry> =
    mapIndexed { index, item -> ChartLegendEntry(label(item), colors[index % colors.size]) }

@Composable
fun ChartLegend(entries: List<ChartLegendEntry>, modifier: Modifier = Modifier) {
    // WHY: FlowRow glued "Otros" onto the previous diagnosis on narrow widths.
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        entries.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(10.dp).background(entry.color, CircleShape))
                Text(entry.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
