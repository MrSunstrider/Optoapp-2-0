package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.ServicioExtra
import java.time.LocalDate
import java.time.Period

data class PacientesReporte(
    val altas: Int,
    val hombres: Int,
    val mujeres: Int,
    val sinSexo: Int,
    val pediatricos: Int = 0,
    val adultos: Int = 0,
    val adultosMayores: Int = 0,
)

data class InventarioCritico(
    val label: String,
    val stockActual: Int,
    val stockMinimo: Int,
)

data class InventarioBucket(
    val label: String,
    val unidades: Int,
)

data class InventarioReporte(
    val modelos: Int,
    val stockTotal: Int,
    val lowStock: Int,
    val salidasVentaPeriodo: Int,
    val criticos: List<InventarioCritico> = emptyList(),
    val stockPorTipoAro: List<InventarioBucket> = emptyList(),
    val stockPorMaterial: List<InventarioBucket> = emptyList(),
    val movimientosPorTipo: List<InventarioBucket> = emptyList(),
    val salidasPorTipoAro: List<InventarioBucket> = emptyList(),
    val topSalidasModelo: List<InventarioBucket> = emptyList(),
)

data class ClinicoReporte(
    val evaluaciones: Int,
    val porDiagnostico: List<Pair<String, Int>>,
    val porSexo: List<InventarioBucket> = emptyList(),
    val porEdad: List<InventarioBucket> = emptyList(),
    val porEmetropia: List<InventarioBucket> = emptyList(),
    val filtros: ClinicoFiltros = ClinicoFiltros(),
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
    /** Pediatric 0–17, adult 18–59, older adult 60+. Prefer birth date at alta when present. */
    internal fun edadAlAlta(paciente: Paciente): Int {
        val birth = paciente.fechaNacimiento
        if (birth != null) {
            return Period.between(birth, paciente.fechaCreacion).years.coerceAtLeast(0)
        }
        return paciente.edad.coerceAtLeast(0)
    }

    fun pacientes(pacientes: List<Paciente>, start: LocalDate, end: LocalDate): PacientesReporte {
        val altas = pacientes.filter { it.fechaCreacion in start..end }
        fun isMasculino(p: Paciente) = p.sexo.equals("Masculino", ignoreCase = true)
        fun isFemenino(p: Paciente) = p.sexo.equals("Femenino", ignoreCase = true)
        fun edad(p: Paciente) = edadAlAlta(p)
        return PacientesReporte(
            altas = altas.size,
            hombres = altas.count(::isMasculino),
            mujeres = altas.count(::isFemenino),
            sinSexo = altas.count { !isMasculino(it) && !isFemenino(it) },
            pediatricos = altas.count { edad(it) <= 17 },
            adultos = altas.count { edad(it) in 18..59 },
            adultosMayores = altas.count { edad(it) >= 60 },
        )
    }

    fun inventario(
        monturas: List<Montura>,
        movimientos: List<MonturaMovimiento>,
        start: LocalDate,
        end: LocalDate,
        criticosLimit: Int = 8,
        topLimit: Int = 6,
        sinDatoLabel: String = "Sin dato",
        otrosLabel: String = "Otros",
    ): InventarioReporte {
        val activas = monturas.filter { it.activo }
        val byId = monturas.associateBy { it.id }
        val enPeriodo = movimientos.filter { !it.fecha.isBefore(start) && !it.fecha.isAfter(end) }
        val salidasVenta = enPeriodo.filter { it.tipo == "SALIDA_VENTA" }
        val bajos = activas.filter { m ->
            val threshold = m.stockMinimo.takeIf { it > 0 } ?: 2
            m.stockActual <= threshold
        }
        val criticos = bajos
            .sortedWith(compareBy({ it.stockActual }, { it.marca }, { it.modelo }))
            .take(criticosLimit)
            .map { m ->
                val label = monturaLabel(m)
                val threshold = m.stockMinimo.takeIf { it > 0 } ?: 2
                InventarioCritico(label = label, stockActual = m.stockActual, stockMinimo = threshold)
            }
        return InventarioReporte(
            modelos = activas.size,
            stockTotal = activas.sumOf { it.stockActual },
            lowStock = bajos.size,
            salidasVentaPeriodo = salidasVenta.sumOf { it.cantidad },
            criticos = criticos,
            stockPorTipoAro = bucketStock(activas, { it.tipoAro }, sinDatoLabel, topLimit, otrosLabel),
            stockPorMaterial = bucketStock(activas, { it.materialMontura }, sinDatoLabel, topLimit, otrosLabel),
            movimientosPorTipo = bucketSums(enPeriodo.map { it.tipo to it.cantidad }, topLimit, otrosLabel),
            salidasPorTipoAro = bucketSums(
                salidasVenta.map { mov ->
                    val key = byId[mov.monturaId]?.tipoAro.orEmpty().ifBlank { sinDatoLabel }
                    key to mov.cantidad
                },
                topLimit,
                otrosLabel,
            ),
            topSalidasModelo = bucketSums(
                salidasVenta.map { mov ->
                    val m = byId[mov.monturaId]
                    val label = m?.let(::monturaLabel) ?: mov.monturaId.take(8)
                    label to mov.cantidad
                },
                topLimit,
                otrosLabel,
            ),
        )
    }

    private fun monturaLabel(m: Montura): String =
        listOf(m.marca, m.modelo).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { m.sku.ifBlank { m.id.take(8) } }

    private fun bucketStock(
        monturas: List<Montura>,
        keyOf: (Montura) -> String,
        sinDatoLabel: String,
        topLimit: Int,
        otrosLabel: String,
    ): List<InventarioBucket> = bucketSums(
        monturas.map { m ->
            val key = keyOf(m).trim().ifBlank { sinDatoLabel }
            key to m.stockActual
        },
        topLimit,
        otrosLabel,
    )

    private fun bucketSums(
        pairs: List<Pair<String, Int>>,
        topLimit: Int,
        otrosLabel: String,
    ): List<InventarioBucket> {
        if (pairs.isEmpty()) return emptyList()
        val totals = linkedMapOf<String, Int>()
        pairs.forEach { (key, units) -> totals[key] = (totals[key] ?: 0) + units }
        val ranked = totals.entries.sortedByDescending { it.value }
        if (ranked.size <= topLimit) {
            return ranked.map { InventarioBucket(it.key, it.value) }
        }
        val head = ranked.take(topLimit)
        val rest = ranked.drop(topLimit).sumOf { it.value }
        return head.map { InventarioBucket(it.key, it.value) } + InventarioBucket(otrosLabel, rest)
    }

    fun clinico(
        evaluaciones: List<EvaluacionClinica>,
        start: LocalDate,
        end: LocalDate,
        pacientesById: Map<String, Paciente> = emptyMap(),
        filtros: ClinicoFiltros = ClinicoFiltros(),
        sinDatoLabel: String = "Sin dato",
        sinDiagnosticoLabel: String = "Sin diagnóstico",
        pediatricoLabel: String = "Pediátricos",
        adultoLabel: String = "Adultos",
        adultoMayorLabel: String = "Adulto mayor",
        hombresLabel: String = "Hombres",
        mujeresLabel: String = "Mujeres",
        topLimit: Int = 8,
        otrosLabel: String = "Otros",
    ): ClinicoReporte {
        val inRange = evaluaciones.filter { !it.fecha.isBefore(start) && !it.fecha.isAfter(end) }
        val enriched = inRange.map { ev ->
            val paciente = pacientesById[ev.pacienteId]
            ClinicoEvalFacts(
                evaluacion = ev,
                sexo = sexoBucket(paciente, hombresLabel, mujeresLabel, sinDatoLabel),
                edad = edadBucket(paciente, ev.fecha, pediatricoLabel, adultoLabel, adultoMayorLabel, sinDatoLabel),
                emetropia = ClinicoRefraccion.grupo(ev).let { if (it == ClinicoRefraccion.SIN_DATO) sinDatoLabel else it },
            )
        }
        val filtered = enriched.filter { matchesClinicoFiltros(it, filtros, hombresLabel, mujeresLabel, sinDatoLabel, pediatricoLabel, adultoLabel, adultoMayorLabel) }
        val counts = linkedMapOf<String, Int>()
        filtered.forEach { facts ->
            diagnosticoLabels(facts.evaluacion, sinDiagnosticoLabel).forEach { label ->
                counts[label] = (counts[label] ?: 0) + 1
            }
        }
        return ClinicoReporte(
            evaluaciones = filtered.size,
            porDiagnostico = counts.entries.sortedByDescending { it.value }.map { it.key to it.value },
            porSexo = bucketSums(filtered.map { it.sexo to 1 }, topLimit = 4, otrosLabel = otrosLabel),
            porEdad = bucketSums(filtered.map { it.edad to 1 }, topLimit = 4, otrosLabel = otrosLabel),
            porEmetropia = bucketSums(filtered.map { it.emetropia to 1 }, topLimit = 6, otrosLabel = otrosLabel),
            filtros = filtros,
        )
    }

    private data class ClinicoEvalFacts(
        val evaluacion: EvaluacionClinica,
        val sexo: String,
        val edad: String,
        val emetropia: String,
    )

    private fun diagnosticoLabels(ev: EvaluacionClinica, sinDiagnosticoLabel: String): List<String> {
        val explicit = buildList {
            ev.diagnostico?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
            ev.diagnosticoOd.orEmpty().map { it.trim() }.filter { it.isNotBlank() }.forEach(::add)
            ev.diagnosticoOi.orEmpty().map { it.trim() }.filter { it.isNotBlank() }.forEach(::add)
            ev.diagnosticoOtros.orEmpty().map { it.trim() }.filter { it.isNotBlank() }.forEach(::add)
        }
            .map { ClinicoRefraccion.normalizeDiagnosticoLabel(it) }
            .filter { it.isNotBlank() }
            .distinct()
        if (explicit.isNotEmpty()) return explicit
        // Rx-only evaluations still contribute Miopía / Hipermetropía / etc. to the ranking.
        val fromRx = ClinicoRefraccion.grupo(ev)
            .takeIf { it != ClinicoRefraccion.SIN_DATO }
            ?.let { ClinicoRefraccion.normalizeDiagnosticoLabel(it) }
            ?.takeIf { it.isNotBlank() }
        return if (fromRx != null) listOf(fromRx) else listOf(sinDiagnosticoLabel)
    }

    private fun sexoBucket(
        paciente: Paciente?,
        hombresLabel: String,
        mujeresLabel: String,
        sinDatoLabel: String,
    ): String = when {
        paciente == null -> sinDatoLabel
        paciente.sexo.equals("Masculino", ignoreCase = true) -> hombresLabel
        paciente.sexo.equals("Femenino", ignoreCase = true) -> mujeresLabel
        else -> sinDatoLabel
    }

    private fun edadBucket(
        paciente: Paciente?,
        on: LocalDate,
        pediatricoLabel: String,
        adultoLabel: String,
        adultoMayorLabel: String,
        sinDatoLabel: String,
    ): String {
        if (paciente == null) return sinDatoLabel
        val edad = edadEn(paciente, on)
        return when {
            edad <= 17 -> pediatricoLabel
            edad <= 59 -> adultoLabel
            else -> adultoMayorLabel
        }
    }

    internal fun edadEn(paciente: Paciente, on: LocalDate): Int {
        val birth = paciente.fechaNacimiento
        if (birth != null) return Period.between(birth, on).years.coerceAtLeast(0)
        return paciente.edad.coerceAtLeast(0)
    }

    private fun matchesClinicoFiltros(
        facts: ClinicoEvalFacts,
        filtros: ClinicoFiltros,
        hombresLabel: String,
        mujeresLabel: String,
        sinDatoLabel: String,
        pediatricoLabel: String,
        adultoLabel: String,
        adultoMayorLabel: String,
    ): Boolean {
        val sexoOk = when (filtros.sexo) {
            ClinicoSexoFiltro.TODOS -> true
            ClinicoSexoFiltro.HOMBRES -> facts.sexo == hombresLabel
            ClinicoSexoFiltro.MUJERES -> facts.sexo == mujeresLabel
            ClinicoSexoFiltro.SIN_DATO -> facts.sexo == sinDatoLabel
        }
        val edadOk = when (filtros.edad) {
            ClinicoEdadFiltro.TODOS -> true
            ClinicoEdadFiltro.PEDIATRICO -> facts.edad == pediatricoLabel
            ClinicoEdadFiltro.ADULTO -> facts.edad == adultoLabel
            ClinicoEdadFiltro.ADULTO_MAYOR -> facts.edad == adultoMayorLabel
        }
        val emetropiaOk = when (filtros.emetropia) {
            ClinicoEmetropiaFiltro.TODOS -> true
            ClinicoEmetropiaFiltro.EMETROPIA -> facts.emetropia == ClinicoRefraccion.EMETROPIA
            ClinicoEmetropiaFiltro.MIOPIA -> facts.emetropia == ClinicoRefraccion.MIOPIA
            ClinicoEmetropiaFiltro.HIPERMETROPIA -> facts.emetropia == ClinicoRefraccion.HIPERMETROPIA
            ClinicoEmetropiaFiltro.ASTIGMATISMO -> facts.emetropia == ClinicoRefraccion.ASTIGMATISMO
            ClinicoEmetropiaFiltro.SIN_DATO -> facts.emetropia == sinDatoLabel
        }
        return sexoOk && edadOk && emetropiaOk
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
