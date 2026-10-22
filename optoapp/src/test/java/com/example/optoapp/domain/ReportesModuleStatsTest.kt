package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
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
        assertEquals(1, r.salidasVentaPeriodo) // cantidad units of SALIDA_VENTA in range
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
    fun inventario_enrichesTipoAroMaterialMovementsAndTopSalidas() {
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 31)
        val monturas = listOf(
            Montura(id = "a", marca = "Ray", modelo = "Aviator", stockActual = 4, tipoAro = "Metálico", materialMontura = "Acero"),
            Montura(id = "b", marca = "Oak", modelo = "Sport", stockActual = 2, tipoAro = "Pasta", materialMontura = "Acetato"),
            Montura(id = "c", marca = "Vog", modelo = "Lite", stockActual = 1, tipoAro = "", materialMontura = "Acero"),
        )
        val movimientos = listOf(
            MonturaMovimiento(id = "1", monturaId = "a", tipo = "SALIDA_VENTA", fecha = LocalDate.of(2026, 10, 5), cantidad = 2, stockPrevio = 6, stockNuevo = 4),
            MonturaMovimiento(id = "2", monturaId = "a", tipo = "SALIDA_VENTA", fecha = LocalDate.of(2026, 10, 6), cantidad = 1, stockPrevio = 4, stockNuevo = 3),
            MonturaMovimiento(id = "3", monturaId = "b", tipo = "ENTRADA", fecha = LocalDate.of(2026, 10, 7), cantidad = 5, stockPrevio = 0, stockNuevo = 5),
            MonturaMovimiento(id = "4", monturaId = "b", tipo = "SALIDA_VENTA", fecha = LocalDate.of(2026, 10, 8), cantidad = 1, stockPrevio = 5, stockNuevo = 4),
            MonturaMovimiento(id = "5", monturaId = "a", tipo = "SALIDA_VENTA", fecha = LocalDate.of(2026, 9, 1), cantidad = 9, stockPrevio = 10, stockNuevo = 1),
        )
        val r = ReportesModuleStats.inventario(monturas, movimientos, start, end, sinDatoLabel = "Sin dato")
        assertEquals(4, r.salidasVentaPeriodo)
        assertEquals(listOf("Metálico" to 4, "Pasta" to 2, "Sin dato" to 1), r.stockPorTipoAro.map { it.label to it.unidades })
        assertEquals(listOf("Acero" to 5, "Acetato" to 2), r.stockPorMaterial.map { it.label to it.unidades })
        assertEquals(listOf("ENTRADA" to 5, "SALIDA_VENTA" to 4), r.movimientosPorTipo.map { it.label to it.unidades })
        assertEquals(listOf("Metálico" to 3, "Pasta" to 1), r.salidasPorTipoAro.map { it.label to it.unidades })
        assertEquals(listOf("Ray Aviator" to 3, "Oak Sport" to 1), r.topSalidasModelo.map { it.label to it.unidades })
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
    fun pacientes_bucketsAgeGroupsPediatricAdultElder() {
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 31)
        val pacientes = listOf(
            Paciente(id = "1", nombreCompleto = "P", edad = 10, telefono = "", fechaCreacion = LocalDate.of(2026, 10, 2), opticaId = "o1"),
            Paciente(id = "2", nombreCompleto = "A", edad = 35, telefono = "", fechaCreacion = LocalDate.of(2026, 10, 3), opticaId = "o1"),
            Paciente(id = "3", nombreCompleto = "M", edad = 70, telefono = "", fechaCreacion = LocalDate.of(2026, 10, 4), opticaId = "o1"),
            Paciente(
                id = "4",
                nombreCompleto = "N",
                edad = 99,
                telefono = "",
                fechaCreacion = LocalDate.of(2026, 10, 5),
                fechaNacimiento = LocalDate.of(2010, 10, 5),
                opticaId = "o1",
            ),
            Paciente(id = "5", nombreCompleto = "Out", edad = 8, telefono = "", fechaCreacion = LocalDate.of(2026, 9, 1), opticaId = "o1"),
        )
        val r = ReportesModuleStats.pacientes(pacientes, start, end)
        assertEquals(4, r.altas)
        assertEquals(2, r.pediatricos)
        assertEquals(1, r.adultos)
        assertEquals(1, r.adultosMayores)
    }

    @Test
    fun clinico_filtersBySexoEdadAndEmetropiaAndRanksDiagnoses() {
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 31)
        val pacientes = mapOf(
            "h" to Paciente(id = "h", nombreCompleto = "H", edad = 30, telefono = "", fechaCreacion = start, sexo = "Masculino", opticaId = "o1"),
            "m" to Paciente(id = "m", nombreCompleto = "M", edad = 12, telefono = "", fechaCreacion = start, sexo = "Femenino", opticaId = "o1"),
            "e" to Paciente(id = "e", nombreCompleto = "E", edad = 65, telefono = "", fechaCreacion = start, sexo = "Masculino", opticaId = "o1"),
        )
        val evaluaciones = listOf(
            EvaluacionClinica(
                id = "1",
                pacienteId = "h",
                fecha = LocalDate.of(2026, 10, 5),
                diagnostico = "Miopía",
                recetaOdEsf = "-2.00",
                recetaOdCil = "0",
            ),
            EvaluacionClinica(
                id = "2",
                pacienteId = "m",
                fecha = LocalDate.of(2026, 10, 6),
                diagnostico = "Emetropía",
                recetaOdEsf = "plano",
                recetaOdCil = "0",
            ),
            EvaluacionClinica(
                id = "3",
                pacienteId = "e",
                fecha = LocalDate.of(2026, 10, 7),
                diagnostico = "Presbicia",
                recetaOdEsf = "+1.00",
                recetaOdCil = "0",
            ),
            EvaluacionClinica(
                id = "4",
                pacienteId = "h",
                fecha = LocalDate.of(2026, 9, 1),
                diagnostico = "Fuera",
            ),
        )
        val all = ReportesModuleStats.clinico(evaluaciones, start, end, pacientes)
        assertEquals(3, all.evaluaciones)
        assertEquals(listOf("Miopía" to 1, "Emetropía" to 1, "Presbicia" to 1), all.porDiagnostico)
        assertEquals(2, all.porSexo.first { it.label == "Hombres" }.unidades)
        assertEquals(1, all.porEdad.first { it.label == "Pediátricos" }.unidades)

        val soloMiopiaHombres = ReportesModuleStats.clinico(
            evaluaciones,
            start,
            end,
            pacientes,
            filtros = ClinicoFiltros(sexo = ClinicoSexoFiltro.HOMBRES, emetropia = ClinicoEmetropiaFiltro.MIOPIA),
        )
        assertEquals(1, soloMiopiaHombres.evaluaciones)
        assertEquals(listOf("Miopía" to 1), soloMiopiaHombres.porDiagnostico)

        val pediatricos = ReportesModuleStats.clinico(
            evaluaciones,
            start,
            end,
            pacientes,
            filtros = ClinicoFiltros(edad = ClinicoEdadFiltro.PEDIATRICO),
        )
        assertEquals(1, pediatricos.evaluaciones)
        assertEquals(ClinicoRefraccion.EMETROPIA, pediatricos.porEmetropia.single().label)
    }

    @Test
    fun clinico_rxOnlyCountsAsMiopiaAndCollapsesOdOi() {
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 31)
        val pacientes = mapOf(
            "p" to Paciente(id = "p", nombreCompleto = "P", edad = 30, telefono = "", fechaCreacion = start, opticaId = "o1"),
        )
        val evaluaciones = listOf(
            EvaluacionClinica(
                id = "rx",
                pacienteId = "p",
                fecha = LocalDate.of(2026, 10, 5),
                recetaOdEsf = "-2.00",
                recetaOdCil = "0",
            ),
            EvaluacionClinica(
                id = "eyes",
                pacienteId = "p",
                fecha = LocalDate.of(2026, 10, 6),
                diagnosticoOd = listOf("Miopía (OD)"),
                diagnosticoOi = listOf("Miopia OI"),
            ),
        )
        val r = ReportesModuleStats.clinico(evaluaciones, start, end, pacientes)
        assertEquals(2, r.evaluaciones)
        assertEquals(listOf("Miopía" to 2), r.porDiagnostico)
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
