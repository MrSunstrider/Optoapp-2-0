package com.example.optoapp.util

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
import java.time.LocalDate

/**
 * Datos de dispensación necesarios para armar el ticket de laboratorio (misma forma que en pantalla de edición).
 */
data class LaboratorioTicketContext(
    val ot: String,
    val fecha: LocalDate,
    val pacienteNombre: String,
    val tipoLente: String,
    val subTipoBifocal: String,
    val distanciaLente: String,
    val altura: String,
    val materialLente: String,
    val tratamientos: List<String>,
    val colorLente: String,
    val notasDiseno: String,
    val origenMontura: String,
    val tipoAro: String,
    val materialMontura: String,
    val descripcionMontura: String,
    val tipoMontura: String,
) {
    companion object {
        fun fromDispensacion(d: DispensacionOptica, pacienteNombre: String) = LaboratorioTicketContext(
            ot = d.ot,
            fecha = d.fecha,
            pacienteNombre = pacienteNombre,
            tipoLente = d.tipoLente,
            subTipoBifocal = d.subTipoBifocal,
            distanciaLente = d.distanciaLente,
            altura = d.altura,
            materialLente = d.materialLente,
            tratamientos = d.tratamientos,
            colorLente = d.colorLente,
            notasDiseno = d.notasDiseno,
            origenMontura = d.origenMontura,
            tipoAro = d.tipoAro,
            materialMontura = d.materialMontura,
            descripcionMontura = d.descripcionMontura,
            tipoMontura = d.tipoMontura,
        )
    }
}

object DispensacionLaboratorioTicket {

    private fun tratamientosStr(tratamientos: List<String>) =
        tratamientos.filter { it.isNotBlank() && it != "Ninguno" }.joinToString(", ")

    fun textoCompleto(ctx: LaboratorioTicketContext, ultimaEval: EvaluacionClinica?): String {
        return buildTicketFormatoLaboratorio(ctx, ultimaEval)
    }

    fun textoCompacto(ctx: LaboratorioTicketContext, ultimaEval: EvaluacionClinica?): String {
        return buildTicketFormatoLaboratorio(ctx, ultimaEval)
    }

    private fun buildTicketFormatoLaboratorio(ctx: LaboratorioTicketContext, ev: EvaluacionClinica?): String {
        val dipLinea = dipLejosCerca(ev)
        val addLinea = addOdOi(ev)
        val requiereAltura =
            ctx.tipoLente == "Bifocal" || ctx.tipoLente == "Progresivo" || ctx.tipoLente == "Ocupacional"
        val alturaLinea = if (requiereAltura && ctx.altura.isNotBlank()) "h: ${ctx.altura.trim()} mm" else ""

        val od = formatOjo(ev?.recetaOdEsf, ev?.recetaOdCil, ev?.recetaOdEje, "OD")
        val oi = formatOjo(ev?.recetaOiEsf, ev?.recetaOiCil, ev?.recetaOiEje, "OI")
        val lente = buildString {
            append(ctx.tipoLente.ifBlank { "—" })
            if (ctx.subTipoBifocal.isNotBlank()) append(" (${ctx.subTipoBifocal})")
            if (ctx.tipoLente == "Monofocal" && ctx.distanciaLente.isNotBlank()) append(" - ${ctx.distanciaLente}")
        }

        return buildString {
            appendLine("OT: ${ctx.ot.ifBlank { "—" }}")
            appendLine("Fecha: ${DateUtils.formatLocalized(ctx.fecha)}")
            appendLine("Paciente: ${ctx.pacienteNombre.ifBlank { "—" }}")
            appendLine("Fórmula")
            appendLine(od)
            appendLine(oi)
            if (dipLinea.isNotBlank()) appendLine(dipLinea)
            if (addLinea.isNotBlank()) appendLine("Add: $addLinea")
            if (alturaLinea.isNotBlank()) appendLine(alturaLinea)
            appendLine("Lente: $lente")
            if (ctx.materialLente.isNotBlank()) appendLine("Mat ${ctx.materialLente}")
            appendLine("Montura")
            if (ctx.origenMontura.isNotBlank()) appendLine("Origen: ${ctx.origenMontura}")
            if (ctx.tipoAro.isNotBlank()) appendLine("Aro: ${ctx.tipoAro}")
            if (ctx.materialMontura.isNotBlank()) appendLine("Mat: ${ctx.materialMontura}")
        }.trimEnd()
    }

    private fun formatOjo(esf: String?, cil: String?, eje: String?, label: String): String {
        val e = esf?.takeIf { it.isNotBlank() } ?: "—"
        val c = cil?.takeIf { it.isNotBlank() } ?: "—"
        val x = eje?.takeIf { it.isNotBlank() } ?: "—"
        return "$label: $e / $c  × $x°"
    }

    private fun dipLejosCerca(ev: EvaluacionClinica?): String {
        if (ev == null) return ""
        val lejos = ev.dipLejos.trim()
        val cerca = ev.dipCerca.trim()
        return when {
            lejos.isNotBlank() && cerca.isNotBlank() -> "$lejos (lejos) / $cerca (cerca)"
            lejos.isNotBlank() -> "$lejos (lejos)"
            cerca.isNotBlank() -> "$cerca (cerca)"
            else -> ""
        }
    }

    private fun addOdOi(ev: EvaluacionClinica?): String {
        if (ev == null) return ""
        val od = ev.addCercaOd.trim()
        val oi = ev.addCercaOi.trim()
        return when {
            od.isNotBlank() && oi.isNotBlank() -> "OD $od · OI $oi"
            od.isNotBlank() -> "OD $od"
            oi.isNotBlank() -> "OI $oi"
            ev.addAv.isNotBlank() -> ev.addAv.trim()
            else -> ""
        }
    }
}
