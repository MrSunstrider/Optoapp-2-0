package com.example.optoapp.util

import com.example.optoapp.domain.EquipoReporte
import com.example.optoapp.domain.PacientesReporte
import com.example.optoapp.domain.PeriodComparison
import com.example.optoapp.domain.VendedorVenta
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportePdfSectionLinesTest {

    @Test
    fun build_includesComparisonPacientesAndEquipo() {
        val lines = ReportePdfSectionLines.build(
            ReportePdfExtras(
                comparison = PeriodComparison(
                    currentVendido = 150.0,
                    previousVendido = 100.0,
                    currentMargen = 60.0,
                    previousMargen = 40.0,
                    currentTransacciones = 3,
                    previousTransacciones = 2,
                ),
                pacientes = PacientesReporte(altas = 2, hombres = 1, mujeres = 1, sinSexo = 0),
                equipo = EquipoReporte(
                    filas = listOf(
                        VendedorVenta("u1", "ana@x.com", 100.0, 1, 10.0),
                    ),
                    sinAtribuirVendido = 50.0,
                    sinAtribuirCount = 1,
                    comisionPct = 10.0,
                ),
            ),
        )
        assertTrue(lines.any { it.startsWith("COMPARATIVO") })
        assertTrue(lines.any { it.startsWith("PACIENTES") })
        assertTrue(lines.any { it.contains("Altas: 2") })
        assertTrue(lines.any { it.contains("Pediatricos") })
        assertTrue(lines.any { it.startsWith("EQUIPO") })
        assertTrue(lines.any { it.contains("ana@x.com") })
        assertTrue(lines.any { it.startsWith("Sin atribuir") })
    }
}
