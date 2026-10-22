package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PeriodSeriesComparisonTest {

    private fun mov(id: String, fecha: LocalDate, monto: Double) = MovimientoFinanciero(
        id = id,
        fecha = fecha,
        tipo = TipoMovimiento.VENTA,
        origen = Origen.DISPENSACION,
        origenId = id,
        montoTotal = monto,
        montoPagado = monto,
        costo = 0.0,
        pacienteId = "p",
        opticaId = "o",
        descripcion = id,
    )

    @Test
    fun switchingPeriodo_changesBucketCountSemanalMensualAnual() {
        val ancla = LocalDate.of(2026, 10, 21)
        val empty = emptyList<MovimientoFinanciero>()
        val semanal = PeriodSeriesComparison.fromMovimientos("Semanal", ancla, "2026", empty, empty)!!
        val mensual = PeriodSeriesComparison.fromMovimientos("Mensual", ancla, "2026", empty, empty)!!
        val anual = PeriodSeriesComparison.fromMovimientos("Anual", ancla, "2026", empty, empty)!!

        assertEquals(7, semanal.labels.size)
        assertEquals(PeriodSeriesComparison.Granularity.SEMANAL_DIAS, semanal.granularity)
        assertTrue(mensual.labels.size in 4..6)
        assertEquals(PeriodSeriesComparison.Granularity.MENSUAL_SEMANAS, mensual.granularity)
        assertEquals(12, anual.labels.size)
        assertEquals(PeriodSeriesComparison.Granularity.ANUAL_MESES, anual.granularity)
    }

    @Test
    fun diarioAndTotalReturnNull() {
        assertNull(
            PeriodSeriesComparison.fromMovimientos(
                "Diario", LocalDate.of(2026, 10, 21), "2026", emptyList(), emptyList(),
            ),
        )
        assertNull(
            PeriodSeriesComparison.fromMovimientos(
                "Total", LocalDate.of(2026, 10, 21), "2026", emptyList(), emptyList(),
            ),
        )
    }

    @Test
    fun anual_bucketsTwelveMonthsVsPreviousYear() {
        val current = listOf(
            mov("a", LocalDate.of(2026, 1, 5), 100.0),
            mov("b", LocalDate.of(2026, 1, 20), 50.0),
            mov("c", LocalDate.of(2026, 3, 1), 200.0),
        )
        val previous = listOf(
            mov("d", LocalDate.of(2025, 1, 10), 80.0),
            mov("e", LocalDate.of(2025, 12, 1), 40.0),
        )
        val series = PeriodSeriesComparison.fromMovimientos(
            "Anual", LocalDate.of(2026, 6, 1), "2026", current, previous,
        )!!
        assertEquals(PeriodSeriesComparison.Granularity.ANUAL_MESES, series.granularity)
        assertEquals(12, series.labels.size)
        assertEquals(150.0, series.currentValues[0], 0.0)
        assertEquals(200.0, series.currentValues[2], 0.0)
        assertEquals(80.0, series.previousValues[0], 0.0)
        assertEquals(40.0, series.previousValues[11], 0.0)
        assertEquals(0.0, series.currentValues[11], 0.0)
    }

    @Test
    fun mensual_bucketsWeeksOfMonth() {
        val current = listOf(
            mov("a", LocalDate.of(2026, 10, 3), 10.0),
            mov("b", LocalDate.of(2026, 10, 10), 20.0),
            mov("c", LocalDate.of(2026, 10, 25), 30.0),
        )
        val previous = listOf(
            mov("d", LocalDate.of(2026, 9, 2), 5.0),
            mov("e", LocalDate.of(2026, 9, 28), 7.0),
        )
        val series = PeriodSeriesComparison.fromMovimientos(
            "Mensual", LocalDate.of(2026, 10, 15), "2026", current, previous,
        )!!
        assertEquals(PeriodSeriesComparison.Granularity.MENSUAL_SEMANAS, series.granularity)
        assertEquals(5, series.labels.size)
        assertEquals(10.0, series.currentValues[0], 0.0)
        assertEquals(20.0, series.currentValues[1], 0.0)
        assertEquals(30.0, series.currentValues[3], 0.0)
        assertEquals(5.0, series.previousValues[0], 0.0)
        assertEquals(7.0, series.previousValues[3], 0.0)
    }

    @Test
    fun semanal_bucketsWeekdays() {
        // Mon 2026-10-19 .. Sun 2026-10-25
        val current = listOf(
            mov("a", LocalDate.of(2026, 10, 20), 15.0), // Tue
            mov("b", LocalDate.of(2026, 10, 24), 25.0), // Sat
        )
        val previous = listOf(
            mov("c", LocalDate.of(2026, 10, 12), 8.0), // Mon previous week
        )
        val series = PeriodSeriesComparison.fromMovimientos(
            "Semanal", LocalDate.of(2026, 10, 21), "2026", current, previous,
        )!!
        assertEquals(PeriodSeriesComparison.Granularity.SEMANAL_DIAS, series.granularity)
        assertEquals(listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"), series.labels)
        assertEquals(15.0, series.currentValues[1], 0.0)
        assertEquals(25.0, series.currentValues[5], 0.0)
        assertEquals(8.0, series.previousValues[0], 0.0)
    }

    @Test
    fun weekOfMonth_dayBoundaries() {
        assertEquals(1, PeriodSeriesComparison.weekOfMonth(LocalDate.of(2026, 10, 1)))
        assertEquals(1, PeriodSeriesComparison.weekOfMonth(LocalDate.of(2026, 10, 7)))
        assertEquals(2, PeriodSeriesComparison.weekOfMonth(LocalDate.of(2026, 10, 8)))
        assertEquals(5, PeriodSeriesComparison.weekOfMonth(LocalDate.of(2026, 10, 31)))
    }
}
