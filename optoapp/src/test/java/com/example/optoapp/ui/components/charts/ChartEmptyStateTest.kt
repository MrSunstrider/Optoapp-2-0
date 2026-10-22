package com.example.optoapp.ui.components.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartEmptyStateTest {

    @Test
    fun forSeries_withoutPoints_showsEmptyStateWithNoDataSummaryAndNoDataToggle() {
        val model = ChartEmptyStateModel.forSeries("Ingresos", listOf(ChartSeries("Ingresos", emptyList())))

        assertTrue(model.isEmpty)
        assertEquals("Ingresos. Sin datos para este período.", model.summary)
        assertFalse(model.showDataToggle)
    }

    @Test
    fun forSeries_withNoSeries_isEmpty() {
        val model = ChartEmptyStateModel.forSeries("Ingresos", emptyList())

        assertTrue(model.isEmpty)
        assertFalse(model.showDataToggle)
    }

    @Test
    fun forSeries_withPoints_showsChartSummaryAndDataToggle() {
        val series = listOf(ChartSeries("Ingresos", listOf(ChartPoint("Ene", 100.0))))

        val model = ChartEmptyStateModel.forSeries("Ingresos", series)

        assertFalse(model.isEmpty)
        assertEquals(ChartSummaryBuilder.cartesian("Ingresos", series), model.summary)
        assertTrue(model.showDataToggle)
    }

    @Test
    fun forSeries_withOnlyZeroValues_isEmpty() {
        val series = listOf(ChartSeries("Ingresos", listOf(ChartPoint("Ene", 0.0), ChartPoint("Feb", 0.0))))

        val model = ChartEmptyStateModel.forSeries("Ingresos", series)

        assertTrue(model.isEmpty)
        assertEquals("Ingresos. Sin datos para este período.", model.summary)
        assertFalse(model.showDataToggle)
    }

    @Test
    fun forSlices_withOnlyZeroAmounts_isEmpty() {
        val model = ChartEmptyStateModel.forSlices("Costos", listOf(DonutSlice("Lentes", 0.0)))

        assertTrue(model.isEmpty)
        assertEquals("Costos. Sin datos para este período.", model.summary)
        assertFalse(model.showDataToggle)
    }

    @Test
    fun forSlices_withAmounts_showsDonutSummaryAndDataToggle() {
        val slices = listOf(DonutSlice("Lentes", 30.0), DonutSlice("Monturas", 70.0))

        val model = ChartEmptyStateModel.forSlices("Costos", slices)

        assertFalse(model.isEmpty)
        assertEquals(ChartSummaryBuilder.donut("Costos", slices), model.summary)
        assertTrue(model.showDataToggle)
    }
}
