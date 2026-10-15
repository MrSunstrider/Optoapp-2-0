package com.example.optoapp.ui.components.charts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.optoapp.R
import com.example.optoapp.util.MoneyFormatter

@Composable
fun ChartDataTable(
    state: ChartDataTableState,
    labelHeader: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TextButton(onClick = onToggle, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(if (state.expanded) R.string.chart_ocultar_datos else R.string.chart_ver_datos))
        }
        if (state.expanded) {
            ChartDataTableRow(labelHeader, state.seriesNames, header = true)
            state.visibleRows.forEach { ChartDataTableRow(it.label, it.values, header = false) }
        }
    }
}

@Composable
private fun ChartDataTableRow(label: String, values: List<String>, header: Boolean) {
    val weight = if (header) FontWeight.SemiBold else FontWeight.Normal
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).semantics(mergeDescendants = true) {}) {
        (listOf(label) + values).forEach { cell ->
            Text(cell, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = weight)
        }
    }
}

data class ChartDataRow(val label: String, val values: List<String>)

data class ChartDataTableState(
    val seriesNames: List<String>,
    val rows: List<ChartDataRow>,
    val expanded: Boolean = false,
) {
    val visibleRows: List<ChartDataRow> get() = if (expanded) rows else emptyList()

    fun toggle(): ChartDataTableState = copy(expanded = !expanded)

    companion object {
        private const val MISSING = "\u2014"

        fun fromSeries(series: List<ChartSeries>): ChartDataTableState {
            val rows = chartXLabels(series).map { label ->
                ChartDataRow(
                    label = label,
                    values = series.map { s -> s.points.firstOrNull { it.label == label }?.let { MoneyFormatter.format(it.value) } ?: MISSING },
                )
            }
            return ChartDataTableState(seriesNames = series.map { it.name }, rows = rows)
        }

        fun fromSlices(slices: List<DonutSlice>, amountHeader: String, shareHeader: String): ChartDataTableState {
            val plan = DonutPlan.from(slices)
            val rows = plan.slices.map {
                ChartDataRow(it.label, listOf(MoneyFormatter.format(it.amount), ChartSummaryBuilder.percent(it.amount, plan.total)))
            }
            return ChartDataTableState(seriesNames = listOf(amountHeader, shareHeader), rows = rows)
        }
    }
}
