package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.Paciente
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReportesModuleStatsTest {

    @Test
    fun pacientes_countsAltasInRangeBySexo() {
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 31)
        val pacientes = listOf(
            Paciente(id = "1", nombreCompleto = "A", edad = 20, telefono = "", fechaCreacion = LocalDate.of(2026, 10, 5), sexo = "Masculino", opticaId = "o1"),
            Paciente(id = "2", nombreCompleto = "B", edad = 20, telefono = "", fechaCreacion = LocalDate.of(2026, 10, 8), sexo = "Femenino", opticaId = "o1"),
            Paciente(id = "3", nombreCompleto = "C", edad = 20, telefono = "", fechaCreacion = LocalDate.of(2026, 9, 1), sexo = "Masculino", opticaId = "o1"),
        )
        val r = ReportesModuleStats.pacientes(pacientes, start, end)
        assertEquals(2, r.altas)
        assertEquals(1, r.hombres)
        assertEquals(1, r.mujeres)
    }

    @Test
    fun equipo_groupsByVendedorAndAppliesCommission() {
        val disps = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p", fecha = LocalDate.of(2026, 10, 1), montoTotal = 100.0, vendedorUserId = "u1"),
            DispensacionOptica(id = "d2", pacienteId = "p", fecha = LocalDate.of(2026, 10, 2), montoTotal = 50.0, vendedorUserId = null),
        )
        val r = ReportesModuleStats.equipo(disps, emptyList(), mapOf("u1" to "ana@x.com"), comisionPct = 10.0)
        assertEquals(1, r.filas.size)
        assertEquals(100.0, r.filas[0].vendido, 0.0)
        assertEquals(10.0, r.filas[0].comision, 0.0)
        assertEquals(50.0, r.sinAtribuirVendido, 0.0)
        assertEquals(1, r.sinAtribuirCount)
    }
}
