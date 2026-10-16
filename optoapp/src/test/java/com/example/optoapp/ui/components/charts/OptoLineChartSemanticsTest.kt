package com.example.optoapp.ui.components.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OptoLineChartSemanticsTest {

    private val trend = ChartSeries(
        name = "Ventas",
        points = listOf(ChartPoint("Ene", 1000.0), ChartPoint("Feb", 1500.0), ChartPoint("Mar", 1200.0)),
    )

    @Test
    fun singlePoint_stillRendersAMarker() {
        val plan = LineChartPlan.from(listOf(ChartSeries("Ventas", listOf(ChartPoint("Ene", 1000.0)))), target = null)

        assertTrue(plan.showsMarkers)
        assertEquals(1, plan.series.single().points.size)
    }

    @Test
    fun severalPoints_drawOnlyTheLine() {
        val plan = LineChartPlan.from(listOf(trend), target = null)

        assertFalse(plan.showsMarkers)
    }

    @Test
    fun noTarget_hasNoTargetLineAndSummaryOmitsIt() {
        val plan = LineChartPlan.from(listOf(trend), target = null)

        assertNull(plan.targetLine)
        assertFalse(plan.contentDescription("Ventas mensuales").contains("Meta"))
    }

    @Test
    fun target_isDrawnInsideTheValueRangeAndDescribed() {
        val plan = LineChartPlan.from(listOf(trend), target = 5000.0)

        assertEquals(5000.0, plan.targetLine!!, 0.0)
        assertTrue(plan.range.max >= 5000.0)
        assertEquals(
            ChartSummaryBuilder.cartesian("Ventas mensuales", listOf(trend), target = 5000.0),
            plan.contentDescription("Ventas mensuales"),
        )
    }

    @Test
    fun seriesWithoutPoints_areDroppedFromThePlan() {
        val plan = LineChartPlan.from(listOf(ChartSeries("Vacía", emptyList()), trend), target = null)

        assertEquals(listOf("Ventas"), plan.series.map { it.name })
    }
}
