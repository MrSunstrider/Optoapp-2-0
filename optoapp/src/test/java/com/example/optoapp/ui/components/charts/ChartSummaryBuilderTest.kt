package com.example.optoapp.ui.components.charts

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartSummaryBuilderTest {

    private val ingresos = ChartSeries(
        name = "Ingresos",
        points = listOf(ChartPoint("Ene", 1200.0), ChartPoint("Feb", 850.5), ChartPoint("Mar", 2300.0)),
    )
    private val costos = ChartSeries(
        name = "Costos",
        points = listOf(ChartPoint("Ene", -40.0), ChartPoint("Feb", 300.0)),
    )

    @Test
    fun cartesian_describesTitleSeriesPointCountAndMoneyRange() {
        val summary = ChartSummaryBuilder.cartesian("Ingresos mensuales", listOf(ingresos, costos))

        assertEquals(
            "Ingresos mensuales. " +
                "Ingresos: 3 puntos, mínimo S/ 850.50, máximo S/ 2,300.00. " +
                "Costos: 2 puntos, mínimo -S/ 40.00, máximo S/ 300.00.",
            summary,
        )
    }

    @Test
    fun cartesian_usesSingularForOnePoint() {
        val summary = ChartSummaryBuilder.cartesian(
            "Margen",
            listOf(ChartSeries("Margen", listOf(ChartPoint("Ene", 75.0)))),
        )

        assertEquals("Margen. Margen: 1 punto, mínimo S/ 75.00, máximo S/ 75.00.", summary)
    }

    @Test
    fun cartesian_appendsTargetWhenPresent() {
        val summary = ChartSummaryBuilder.cartesian("Ventas", listOf(costos), target = 5000.0)

        assertEquals(
            "Ventas. Costos: 2 puntos, mínimo -S/ 40.00, máximo S/ 300.00. Meta: S/ 5,000.00.",
            summary,
        )
    }

    @Test
    fun cartesian_skipsSeriesWithoutPoints() {
        val summary = ChartSummaryBuilder.cartesian(
            "Ventas",
            listOf(ChartSeries("Vacía", emptyList()), costos),
        )

        assertEquals("Ventas. Costos: 2 puntos, mínimo -S/ 40.00, máximo S/ 300.00.", summary)
    }

    @Test
    fun donut_listsCategoryAmountAndPercentage() {
        val summary = ChartSummaryBuilder.donut(
            "Costos por categoría",
            listOf(DonutSlice("Lentes", 255.0), DonutSlice("Monturas", 345.0)),
        )

        assertEquals(
            "Costos por categoría. Lentes: S/ 255.00, 42.5 %. Monturas: S/ 345.00, 57.5 %.",
            summary,
        )
    }

    @Test
    fun percent_roundsHalfUpToOneDecimal() {
        assertEquals("33.3 %", ChartSummaryBuilder.percent(1.0, 3.0))
        assertEquals("66.7 %", ChartSummaryBuilder.percent(2.0, 3.0))
        assertEquals("42.5 %", ChartSummaryBuilder.percent(255.0, 600.0))
        assertEquals("100.0 %", ChartSummaryBuilder.percent(80.0, 80.0))
    }

    @Test
    fun percent_isZeroWhenTotalIsNotPositive() {
        assertEquals("0.0 %", ChartSummaryBuilder.percent(10.0, 0.0))
    }

    @Test
    fun empty_saysThereIsNoData() {
        assertEquals("Ventas. Sin datos para este período.", ChartSummaryBuilder.empty("Ventas"))
    }
}
