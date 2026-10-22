package com.example.optoapp.ui.components.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ChartEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}

data class ChartEmptyStateModel(val isEmpty: Boolean, val summary: String, val showDataToggle: Boolean) {
    companion object {
        fun forSeries(title: String, series: List<ChartSeries>): ChartEmptyStateModel {
            val hasData = series.any { chartSeries -> chartSeries.points.any { it.value != 0.0 } }
            return of(hasData, if (hasData) ChartSummaryBuilder.cartesian(title, series) else ChartSummaryBuilder.empty(title))
        }

        fun forSlices(title: String, slices: List<DonutSlice>): ChartEmptyStateModel {
            val hasData = DonutPlan.from(slices).slices.isNotEmpty()
            return of(hasData, if (hasData) ChartSummaryBuilder.donut(title, slices) else ChartSummaryBuilder.empty(title))
        }

        private fun of(hasData: Boolean, summary: String) =
            ChartEmptyStateModel(isEmpty = !hasData, summary = summary, showDataToggle = hasData)
    }
}
