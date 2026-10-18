package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.ServicioExtra
import java.time.LocalDate

data class PacientesReporte(
    val altas: Int,
    val hombres: Int,
    val mujeres: Int,
    val sinSexo: Int,
)

data class InventarioCritico(
    val label: String,
    val stockActual: Int,
    val stockMinimo: Int,
)

data class InventarioReporte(
    val modelos: Int,
    val stockTotal: Int,
    val lowStock: Int,
    val salidasVentaPeriodo: Int,
    val criticos: List<InventarioCritico> = emptyList(),
)

data class ClinicoReporte(
    val evaluaciones: Int,
    val porDiagnostico: List<Pair<String, Int>>,
)

data class VendedorVenta(
    val userId: String,
    val displayName: String,
    val vendido: Double,
    val transacciones: Int,
    val comision: Double,
)

data class EquipoReporte(
    val filas: List<VendedorVenta>,
    val sinAtribuirVendido: Double,
    val sinAtribuirCount: Int,
    val comisionPct: Double,
)

object ReportesModuleStats {
    fun pacientes(pacientes: List<Paciente>, start: LocalDate, end: LocalDate): PacientesReporte {
        val altas = pacientes.filter { it.fechaCreacion in start..end }
        fun isMasculino(p: Paciente) = p.sexo.equals("Masculino", ignoreCase = true)
        fun isFemenino(p: Paciente) = p.sexo.equals("Femenino", ignoreCase = true)
        return PacientesReporte(
            altas = altas.size,
            hombres = altas.count(::isMasculino),
            mujeres = altas.count(::isFemenino),
            sinSexo = altas.count { !isMasculino(it) && !isFemenino(it) },
        )
    }

    fun inventario(
        monturas: List<Montura>,
        movimientos: List<MonturaMovimiento>,
        start: LocalDate,
        end: LocalDate,
        criticosLimit: Int = 8,
    ): InventarioReporte {
        val activas = monturas.filter { it.activo }
        val salidas = movimientos.count {
            it.tipo == "SALIDA_VENTA" && !it.fecha.isBefore(start) && !it.fecha.isAfter(end)
        }
        val bajos = activas.filter { m ->
            val threshold = m.stockMinimo.takeIf { it > 0 } ?: 2
            m.stockActual <= threshold
        }
        val criticos = bajos
            .sortedWith(compareBy({ it.stockActual }, { it.marca }, { it.modelo }))
            .take(criticosLimit)
            .map { m ->
                val label = listOf(m.marca, m.modelo).filter { it.isNotBlank() }.joinToString(" ")
                    .ifBlank { m.sku.ifBlank { m.id.take(8) } }
                val threshold = m.stockMinimo.takeIf { it > 0 } ?: 2
                InventarioCritico(label = label, stockActual = m.stockActual, stockMinimo = threshold)
            }
        return InventarioReporte(
            modelos = activas.size,
            stockTotal = activas.sumOf { it.stockActual },
            lowStock = bajos.size,
            salidasVentaPeriodo = salidas,
            criticos = criticos,
        )
    }

    fun clinico(evaluaciones: List<EvaluacionClinica>, start: LocalDate, end: LocalDate): ClinicoReporte {
        val inRange = evaluaciones.filter { !it.fecha.isBefore(start) && !it.fecha.isAfter(end) }
        val counts = linkedMapOf<String, Int>()
        inRange.forEach { ev ->
            val labels = buildList {
                ev.diagnostico?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
                ev.diagnosticoOd.orEmpty().map { it.trim() }.filter { it.isNotBlank() }.forEach(::add)
                ev.diagnosticoOi.orEmpty().map { it.trim() }.filter { it.isNotBlank() }.forEach(::add)
                ev.diagnosticoOtros.orEmpty().map { it.trim() }.filter { it.isNotBlank() }.forEach(::add)
            }.ifEmpty { listOf("Sin diagnóstico") }.distinct()
            labels.forEach { label -> counts[label] = (counts[label] ?: 0) + 1 }
        }
        return ClinicoReporte(
            evaluaciones = inRange.size,
            porDiagnostico = counts.entries.sortedByDescending { it.value }.map { it.key to it.value },
        )
    }

    fun equipo(
        dispensaciones: List<DispensacionOptica>,
        servicios: List<ServicioExtra>,
        namesByUserId: Map<String, String>,
        comisionPct: Double,
    ): EquipoReporte {
        data class Acc(var vendido: Double = 0.0, var count: Int = 0)
        val bySeller = linkedMapOf<String?, Acc>()
        fun add(uid: String?, monto: Double) {
            val acc = bySeller.getOrPut(uid) { Acc() }
            acc.vendido += monto
            acc.count += 1
        }
        dispensaciones.forEach { add(it.vendedorUserId, it.montoTotal) }
        servicios.forEach { add(it.vendedorUserId, it.montoTotal) }
        val sin = bySeller[null] ?: Acc()
        val filas = bySeller.filterKeys { it != null }.map { (uid, acc) ->
            val id = uid!!
            VendedorVenta(
                userId = id,
                displayName = namesByUserId[id] ?: id.take(8),
                vendido = acc.vendido,
                transacciones = acc.count,
                comision = acc.vendido * (comisionPct / 100.0),
            )
        }.sortedByDescending { it.vendido }
        return EquipoReporte(
            filas = filas,
            sinAtribuirVendido = sin.vendido,
            sinAtribuirCount = sin.count,
            comisionPct = comisionPct,
        )
    }
}
