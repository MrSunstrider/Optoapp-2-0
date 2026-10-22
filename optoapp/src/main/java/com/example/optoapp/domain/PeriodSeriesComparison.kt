package com.example.optoapp.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class PeriodSeriesComparison(
    val granularity: Granularity,
    val labels: List<String>,
    val currentValues: List<Double>,
    val previousValues: List<Double>,
) {
    enum class Granularity { ANUAL_MESES, MENSUAL_SEMANAS, SEMANAL_DIAS }

    init {
        require(labels.size == currentValues.size && labels.size == previousValues.size)
    }

    companion object {
        private val MESES_CORTO = listOf(
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic",
        )
        private val DIAS_CORTO = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

        fun fromMovimientos(
            periodo: String,
            fechaAncla: LocalDate,
            anio: String,
            current: List<MovimientoFinanciero>,
            previous: List<MovimientoFinanciero>,
            semanaLabel: (Int) -> String = { "Sem $it" },
        ): PeriodSeriesComparison? = when (periodo) {
            "Anual" -> anual(current, previous)
            "Mensual" -> mensual(fechaAncla, current, previous, semanaLabel)
            "Semanal" -> semanal(current, previous)
            else -> null
        }

        private fun anual(
            current: List<MovimientoFinanciero>,
            previous: List<MovimientoFinanciero>,
        ): PeriodSeriesComparison {
            val cur = DoubleArray(12)
            val prev = DoubleArray(12)
            current.forEach { cur[it.fecha.monthValue - 1] += it.montoTotal }
            previous.forEach { prev[it.fecha.monthValue - 1] += it.montoTotal }
            return PeriodSeriesComparison(
                granularity = Granularity.ANUAL_MESES,
                labels = MESES_CORTO,
                currentValues = cur.toList(),
                previousValues = prev.toList(),
            )
        }

        private fun mensual(
            fechaAncla: LocalDate,
            current: List<MovimientoFinanciero>,
            previous: List<MovimientoFinanciero>,
            semanaLabel: (Int) -> String,
        ): PeriodSeriesComparison {
            val semanas = maxWeekCount(YearMonth.from(fechaAncla), YearMonth.from(fechaAncla.minusMonths(1)))
            val cur = DoubleArray(semanas)
            val prev = DoubleArray(semanas)
            current.forEach { mov ->
                val w = weekOfMonth(mov.fecha) - 1
                if (w in cur.indices) cur[w] += mov.montoTotal
            }
            previous.forEach { mov ->
                val w = weekOfMonth(mov.fecha) - 1
                if (w in prev.indices) prev[w] += mov.montoTotal
            }
            return PeriodSeriesComparison(
                granularity = Granularity.MENSUAL_SEMANAS,
                labels = (1..semanas).map(semanaLabel),
                currentValues = cur.toList(),
                previousValues = prev.toList(),
            )
        }

        private fun semanal(
            current: List<MovimientoFinanciero>,
            previous: List<MovimientoFinanciero>,
        ): PeriodSeriesComparison {
            val cur = DoubleArray(7)
            val prev = DoubleArray(7)
            current.forEach { cur[dayIndex(it.fecha)] += it.montoTotal }
            previous.forEach { prev[dayIndex(it.fecha)] += it.montoTotal }
            return PeriodSeriesComparison(
                granularity = Granularity.SEMANAL_DIAS,
                labels = DIAS_CORTO,
                currentValues = cur.toList(),
                previousValues = prev.toList(),
            )
        }

        internal fun weekOfMonth(date: LocalDate): Int = ((date.dayOfMonth - 1) / 7) + 1

        private fun maxWeekCount(current: YearMonth, previous: YearMonth): Int =
            maxOf(weekOfMonth(current.atEndOfMonth()), weekOfMonth(previous.atEndOfMonth()))

        private fun dayIndex(date: LocalDate): Int = when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> 0
            DayOfWeek.TUESDAY -> 1
            DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.THURSDAY -> 3
            DayOfWeek.FRIDAY -> 4
            DayOfWeek.SATURDAY -> 5
            DayOfWeek.SUNDAY -> 6
        }
    }
}
