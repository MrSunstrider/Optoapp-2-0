package com.example.optoapp.ui.components.charts

import com.example.optoapp.util.MoneyFormatter
import java.math.BigDecimal
import java.math.RoundingMode

// Summaries feed TalkBack through semantics contentDescription, which is built outside
// composition (pure and unit-tested), so the Spanish wording lives here instead of strings.xml.
object ChartSummaryBuilder {
    private const val NO_DATA = "Sin datos para este período."
    private val HUNDRED = BigDecimal(100)

    fun cartesian(title: String, series: List<ChartSeries>, target: Double? = null): String {
        val described = series.filter { it.points.isNotEmpty() }
        if (described.isEmpty()) return empty(title)
        val parts = described.map { describeSeries(it) } + listOfNotNull(target?.let { "Meta: ${MoneyFormatter.format(it)}." })
        return "$title. " + parts.joinToString(" ")
    }

    fun donut(title: String, slices: List<DonutSlice>): String {
        val plan = DonutPlan.from(slices)
        if (plan.slices.isEmpty()) return empty(title)
        val parts = plan.slices.map { "${it.label}: ${MoneyFormatter.format(it.amount)}, ${percent(it.amount, plan.total)}." }
        return "$title. " + parts.joinToString(" ")
    }

    fun percent(part: Double, total: Double): String {
        if (total <= 0.0) return "0.0 %"
        val value = BigDecimal.valueOf(part).multiply(HUNDRED).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
        return "${value.toPlainString()} %"
    }

    fun empty(title: String): String = "$title. $NO_DATA"

    private fun describeSeries(series: ChartSeries): String {
        val values = series.points.map { it.value }
        val count = if (values.size == 1) "1 punto" else "${values.size} puntos"
        return "${series.name}: $count, mínimo ${MoneyFormatter.format(values.min())}, máximo ${MoneyFormatter.format(values.max())}."
    }
}
