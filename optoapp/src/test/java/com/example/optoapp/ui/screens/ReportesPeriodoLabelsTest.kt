package com.example.optoapp.ui.screens

import com.example.optoapp.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportesPeriodoLabelsTest {

    @Test
    fun `period keys keep the order and values ReportesViewModel switches on`() {
        assertEquals(listOf("Diario", "Semanal", "Mensual", "Anual", "Total"), REPORTES_PERIODOS)
    }

    @Test
    fun `every period key has its own label resource`() {
        val labels = REPORTES_PERIODOS.map(::reportesPeriodoLabelRes)
        assertEquals(
            listOf(
                R.string.reportes_periodo_diario,
                R.string.reportes_periodo_semanal,
                R.string.reportes_periodo_mensual,
                R.string.reportes_periodo_anual,
                R.string.reportes_periodo_total,
            ),
            labels,
        )
    }

    @Test
    fun `unknown period key falls back to the total label`() {
        assertEquals(R.string.reportes_periodo_total, reportesPeriodoLabelRes("Todo"))
    }
}
