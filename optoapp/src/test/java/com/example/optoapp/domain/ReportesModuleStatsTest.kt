package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.Paciente
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReportesModuleStatsTest {

    @Test
    fun inventario_listsCriticosSortedByLowestStock() {
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 31)
        val monturas = listOf(
            Montura(id = "a", marca = "Ray", modelo = "Ok", stockActual = 5, stockMinimo = 2),
            Montura(id = "b", marca = "Oak", modelo = "Low", stockActual = 1, stockMinimo = 3),
            Montura(id = "c", marca = "Vog", modelo = "Zero", stockActual = 0, stockMinimo = 1),
        )
        val movimientos = listOf(
            MonturaMovimiento(
                id = "m1",
                monturaId = "a",
                tipo = "SALIDA_VENTA",
                fecha = LocalDate.of(2026, 10, 5),
                cantidad = 1,
                stockPrevio = 6,
                stockNuevo = 5,
            ),
        )
        val r = ReportesModuleStats.inventario(monturas, movimientos, start, end)
        assertEquals(3, r.modelos)
        assertEquals(6, r.stockTotal)
        assertEquals(2, r.lowStock)
        assertEquals(1, r.salidasVentaPeriodo)
        assertEquals(listOf("Vog Zero", "Oak Low"), r.criticos.map { it.label })
        assertEquals(0, r.criticos[0].stockActual)
        assertEquals(1, r.criticos[1].stockActual)
    }

    @Test
    fun inventario_ignoresInactiveMonturasForCatalogAndLowStock() {
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 31)
        val monturas = listOf(
            Montura(id = "a", marca = "Ray", modelo = "Ok", stockActual = 5, stockMinimo = 2, activo = true),
            Montura(id = "b", marca = "Oak", modelo = "Gone", stockActual = 0, stockMinimo = 3, activo = false),
        )
        val r = ReportesModuleStats.inventario(monturas, emptyList(), start, end)
        assertEquals(1, r.modelos)
        assertEquals(5, r.stockTotal)
        assertEquals(0, r.lowStock)
        assertEquals(emptyList<String>(), r.criticos.map { it.label })
    }

    @Test
    fun inventario_stockMinimoZeroUsesDefaultThresholdTwo() {
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 31)
        val monturas = listOf(
            Montura(id = "a", marca = "Ray", modelo = "Edge", stockActual = 2, stockMinimo = 0, activo = true),
            Montura(id = "b", marca = "Oak", modelo = "Ok", stockActual = 3, stockMinimo = 0, activo = true),
        )
        val r = ReportesModuleStats.inventario(monturas, emptyList(), start, end)
        assertEquals(2, r.modelos)
        assertEquals(1, r.lowStock)
        assertEquals(listOf("Ray Edge"), r.criticos.map { it.label })
        assertEquals(2, r.criticos[0].stockMinimo)
    }

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
