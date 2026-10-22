package com.example.optoapp.ui.components.charts

data class ChartPoint(val label: String, val value: Double)

data class ChartSeries(val name: String, val points: List<ChartPoint>)

data class DonutSlice(val label: String, val amount: Double)

data class ChartValueRange(val min: Double, val max: Double) {
    companion object {
        // Zero stays inside the range so bars grow from a real baseline and negative
        // margins are drawn below the axis instead of being clamped.
        fun of(series: List<ChartSeries>, stacked: Boolean = false, target: Double? = null): ChartValueRange {
            val values = if (stacked) stackedExtremes(series) else series.flatMap { s -> s.points.map { it.value } }
            val candidates = values + listOfNotNull(target) + 0.0
            val min = candidates.min()
            val max = candidates.max()
            return if (max > min) ChartValueRange(min, max) else ChartValueRange(min, min + 1.0)
        }

        private fun stackedExtremes(series: List<ChartSeries>): List<Double> =
            chartXLabels(series).flatMap { label ->
                val values = series.mapNotNull { s -> s.points.firstOrNull { it.label == label }?.value }
                listOf(values.filter { it > 0 }.sum(), values.filter { it < 0 }.sum())
            }
    }
}

fun chartXLabels(series: List<ChartSeries>): List<String> =
    series.flatMap { s -> s.points.map { it.label } }.distinct()

data class DonutPlanSlice(val label: String, val amount: Double, val share: Double)

data class DonutPlan(val slices: List<DonutPlanSlice>, val total: Double) {
    companion object {
        // A donut cannot draw a negative share, so adjustments below zero are left out of
        // both the arcs and the total instead of shrinking the other slices.
        fun from(slices: List<DonutSlice>): DonutPlan {
            val positive = slices.filter { it.amount > 0 }
            val total = positive.sumOf { it.amount }
            if (total <= 0.0) return DonutPlan(emptyList(), 0.0)
            return DonutPlan(positive.map { DonutPlanSlice(it.label, it.amount, it.amount / total) }, total)
        }
    }
}

data class BarChartPlan(
    val series: List<ChartSeries>,
    val labels: List<String>,
    val values: List<List<Double>>,
    val range: ChartValueRange,
    val stacked: Boolean,
) {
    fun contentDescription(title: String): String = ChartSummaryBuilder.cartesian(title, series)

    companion object {
        // Every series must expose one value per label so grouped and stacked columns line up;
        // a period missing from a series is drawn as an empty (zero) bar.
        fun from(series: List<ChartSeries>, stacked: Boolean): BarChartPlan {
            val drawable = series.filter { it.points.isNotEmpty() }
            val labels = chartXLabels(drawable)
            val values = drawable.map { s -> labels.map { label -> s.points.firstOrNull { it.label == label }?.value ?: 0.0 } }
            return BarChartPlan(drawable, labels, values, ChartValueRange.of(drawable, stacked = stacked), stacked)
        }
    }
}

data class LineChartPlan(
    val series: List<ChartSeries>,
    val range: ChartValueRange,
    val targetLine: Double?,
    val showsMarkers: Boolean,
) {
    fun contentDescription(title: String): String = ChartSummaryBuilder.cartesian(title, series, targetLine)

    companion object {
        // A line needs two points; with a single point only a marker makes the value visible.
        fun from(series: List<ChartSeries>, target: Double?): LineChartPlan {
            val drawable = series.filter { it.points.isNotEmpty() }
            return LineChartPlan(
                series = drawable,
                range = ChartValueRange.of(drawable, target = target),
                targetLine = target,
                showsMarkers = drawable.any { it.points.size == 1 },
            )
        }
    }
}
