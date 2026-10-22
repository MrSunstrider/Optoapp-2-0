package com.example.optoapp.ui.components.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartModelsTest {

    @Test
    fun negativeValue_isPreservedAndExtendsTheRangeBelowZero() {
        val margen = ChartSeries("Margen", listOf(ChartPoint("Ene", -150.0), ChartPoint("Feb", 200.0)))

        val range = ChartValueRange.of(listOf(margen))

        assertEquals(-150.0, margen.points.first().value, 0.0)
        assertEquals(-150.0, range.min, 0.0)
        assertEquals(200.0, range.max, 0.0)
    }

    @Test
    fun positiveValues_keepZeroAsBaseline() {
        val range = ChartValueRange.of(listOf(ChartSeries("Ventas", listOf(ChartPoint("Ene", 40.0)))))

        assertEquals(0.0, range.min, 0.0)
        assertEquals(40.0, range.max, 0.0)
    }

    @Test
    fun stacked_sumsPositivesUpAndNegativesDownPerPeriod() {
        val a = ChartSeries("A", listOf(ChartPoint("Ene", 100.0), ChartPoint("Feb", -50.0)))
        val b = ChartSeries("B", listOf(ChartPoint("Ene", 200.0), ChartPoint("Feb", -30.0)))

        val range = ChartValueRange.of(listOf(a, b), stacked = true)

        assertEquals(-80.0, range.min, 0.0)
        assertEquals(300.0, range.max, 0.0)
    }

    @Test
    fun target_isIncludedInTheRange() {
        val range = ChartValueRange.of(listOf(ChartSeries("Ventas", listOf(ChartPoint("Ene", 40.0)))), target = -500.0)

        assertEquals(-500.0, range.min, 0.0)
    }

    @Test
    fun allZeroValues_stillProduceANonEmptyRange() {
        val range = ChartValueRange.of(listOf(ChartSeries("Ventas", listOf(ChartPoint("Ene", 0.0)))))

        assertTrue(range.max > range.min)
    }

    @Test
    fun xLabels_unionLabelsInFirstSeenOrder() {
        val a = ChartSeries("A", listOf(ChartPoint("Ene", 1.0), ChartPoint("Feb", 2.0)))
        val b = ChartSeries("B", listOf(ChartPoint("Feb", 3.0), ChartPoint("Mar", 4.0)))

        assertEquals(listOf("Ene", "Feb", "Mar"), chartXLabels(listOf(a, b)))
    }

    @Test
    fun barPlan_keepsNegativeBarsAndAlignsEverySeriesToTheSameLabels() {
        val margen = ChartSeries("Margen", listOf(ChartPoint("Ene", -300.0), ChartPoint("Feb", 200.0)))
        val costo = ChartSeries("Costo", listOf(ChartPoint("Feb", 50.0)))

        val plan = BarChartPlan.from(listOf(margen, costo), stacked = false)

        assertEquals(listOf("Ene", "Feb"), plan.labels)
        assertEquals(listOf(listOf(-300.0, 200.0), listOf(0.0, 50.0)), plan.values)
        assertEquals(-300.0, plan.range.min, 0.0)
        assertEquals(200.0, plan.range.max, 0.0)
    }

    @Test
    fun barPlan_stackedRangeGrowsWithTheSumOfEachPeriod() {
        val lentes = ChartSeries("Lentes", listOf(ChartPoint("Ene", 120.0)))
        val monturas = ChartSeries("Monturas", listOf(ChartPoint("Ene", 80.0)))

        val plan = BarChartPlan.from(listOf(lentes, monturas), stacked = true)

        assertTrue(plan.stacked)
        assertEquals(200.0, plan.range.max, 0.0)
        assertEquals(
            ChartSummaryBuilder.cartesian("Costo por componente", listOf(lentes, monturas)),
            plan.contentDescription("Costo por componente"),
        )
    }

    @Test
    fun donutPlan_keepsPositiveSlicesWithTheirShareOfTheTotal() {
        val plan = DonutPlan.from(
            listOf(DonutSlice("Lentes", 30.0), DonutSlice("Ajuste", -10.0), DonutSlice("Monturas", 90.0)),
        )

        assertEquals(listOf("Lentes", "Monturas"), plan.slices.map { it.label })
        assertEquals(120.0, plan.total, 0.0)
        assertEquals(listOf(0.25, 0.75), plan.slices.map { it.share })
    }

    @Test
    fun donutPlan_allNonPositive_returnsEmptyWithoutDividingByZero() {
        val plan = DonutPlan.from(listOf(DonutSlice("A", 0.0), DonutSlice("B", -5.0)))

        assertTrue(plan.slices.isEmpty())
        assertEquals(0.0, plan.total, 0.0)
    }

    @Test
    fun barChartPlan_fromSemanalThenMensual_labelCountsChangeWithoutThrowing() {
        val semanal = BarChartPlan.from(
            listOf(
                ChartSeries("Actual", listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom").map { ChartPoint(it, 1.0) }),
                ChartSeries("Anterior", listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom").map { ChartPoint(it, 2.0) }),
            ),
            stacked = false,
        )
        val mensual = BarChartPlan.from(
            listOf(
                ChartSeries("Actual", listOf("Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5").map { ChartPoint(it, 3.0) }),
                ChartSeries("Anterior", listOf("Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5").map { ChartPoint(it, 4.0) }),
            ),
            stacked = false,
        )

        assertEquals(7, semanal.labels.size)
        assertEquals(5, mensual.labels.size)
        assertEquals(2, semanal.values.size)
        assertEquals(2, mensual.values.size)
    }
}
