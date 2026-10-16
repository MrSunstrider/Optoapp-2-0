package com.example.optoapp.ui.components.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// The plot is hidden from TalkBack and replaced by the summary; the "Ver datos" table
// carries the per-point values, so screen-reader users never depend on the drawing.
@Composable
internal fun ChartFrame(
    title: String,
    model: ChartEmptyStateModel,
    table: ChartDataTableState,
    labelHeader: String,
    legend: List<ChartLegendEntry>,
    modifier: Modifier = Modifier,
    chart: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (model.isEmpty) {
            ChartEmptyState(message = model.summary)
        } else {
            Box(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = model.summary }) { chart() }
            if (legend.size > 1) ChartLegend(legend)
            var tableState by remember(table) { mutableStateOf(table) }
            ChartDataTable(tableState, labelHeader, onToggle = { tableState = tableState.toggle() })
        }
    }
}
