package com.example.optoapp.ui.components.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartDataTableTest {

    private val months = listOf("May", "Jun", "Jul", "Ago", "Set", "Oct")

    private val ingresos = ChartSeries(
        name = "Ingresos",
        points = months.mapIndexed { index, month -> ChartPoint(month, 100.0 * (index + 1)) },
    )
    private val costos = ChartSeries(
        name = "Costos",
        points = months.drop(1).map { ChartPoint(it, -25.0) },
    )

    @Test
    fun fromSeries_buildsOneRowPerPeriodWithFormattedValues() {
        val state = ChartDataTableState.fromSeries(listOf(ingresos, costos))

        assertEquals(6, state.rows.size)
        assertEquals(months, state.rows.map { it.label })
        assertEquals(listOf("Ingresos", "Costos"), state.seriesNames)
        assertEquals(listOf("S/ 100.00", "\u2014"), state.rows.first().values)
        assertEquals(listOf("S/ 600.00", "-S/ 25.00"), state.rows.last().values)
    }

    @Test
    fun fromSlices_listsPositiveSlicesWithAmountAndShare() {
        val state = ChartDataTableState.fromSlices(
            listOf(DonutSlice("Lentes", 255.0), DonutSlice("Ajuste", -5.0), DonutSlice("Monturas", 345.0)),
            amountHeader = "Monto",
            shareHeader = "Porcentaje",
        )

        assertEquals(listOf("Monto", "Porcentaje"), state.seriesNames)
        assertEquals(listOf("Lentes", "Monturas"), state.rows.map { it.label })
        assertEquals(listOf("S/ 255.00", "42.5 %"), state.rows.first().values)
        assertEquals(listOf("S/ 345.00", "57.5 %"), state.rows.last().values)
    }

    @Test
    fun startsCollapsedWithNoVisibleRows() {
        val state = ChartDataTableState.fromSeries(listOf(ingresos))

        assertFalse(state.expanded)
        assertTrue(state.visibleRows.isEmpty())
    }

    @Test
    fun toggle_expandsAndCollapsesTheRows() {
        val collapsed = ChartDataTableState.fromSeries(listOf(ingresos))

        val expanded = collapsed.toggle()
        assertTrue(expanded.expanded)
        assertEquals(6, expanded.visibleRows.size)

        val collapsedAgain = expanded.toggle()
        assertFalse(collapsedAgain.expanded)
        assertTrue(collapsedAgain.visibleRows.isEmpty())
    }
}
