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
        val requiereAltura =
            ctx.tipoLente == "Bifocal" || ctx.tipoLente == "Progresivo" || ctx.tipoLente == "Ocupacional"
        val tipoLenteLinea = buildString {
            if (ctx.tipoLente.isNotBlank()) {
                append(ctx.tipoLente)
                if (ctx.subTipoBifocal.isNotBlank()) append(" (${ctx.subTipoBifocal})")
                if (ctx.tipoLente == "Monofocal" && ctx.distanciaLente.isNotBlank()) append(" - ${ctx.distanciaLente}")
            }
        }
        val tratamientosStr = tratamientosStr(ctx.tratamientos)
        val alturaParaFormula =
            if (requiereAltura && ctx.altura.isNotBlank()) ctx.altura.trim() else ""
        val bloqueFormula = LaboratorioTicketText.bloqueFormulaLaboratorio(ultimaEval, alturaParaFormula)

        return buildString {
            appendLine("OT: ${ctx.ot.ifBlank { "—" }}")
            appendLine("Fecha: ${DateUtils.formatLocalized(ctx.fecha)}")
            appendLine("Paciente: ${ctx.pacienteNombre.ifBlank { "—" }}")
            appendLine()
            appendLine("— Fórmula —")
            if (bloqueFormula.isNotBlank()) appendLine(bloqueFormula)
            else appendLine("(sin datos en la última evaluación del paciente)")
            appendLine()
            appendLine("— Lente —")
            if (tipoLenteLinea.isNotBlank()) appendLine("Lente: $tipoLenteLinea")
            if (ctx.materialLente.isNotBlank()) appendLine("Material: ${ctx.materialLente}")
            if (ctx.colorLente.isNotBlank()) appendLine("Color: ${ctx.colorLente}")
            if (tratamientosStr.isNotBlank()) appendLine("Trat: $tratamientosStr")
            appendLine()
            appendLine("— Montura —")
            if (ctx.origenMontura.isNotBlank()) appendLine("Origen: ${ctx.origenMontura}")
            if (ctx.tipoAro.isNotBlank()) appendLine("Aro: ${ctx.tipoAro}")
            if (ctx.materialMontura.isNotBlank()) appendLine("Material: ${ctx.materialMontura}")
            if (ctx.descripcionMontura.isNotBlank()) appendLine("Descripción: ${ctx.descripcionMontura}")
            if (ctx.tipoMontura.isNotBlank()) appendLine("Tipo: ${ctx.tipoMontura}")
            if (ctx.notasDiseno.isNotBlank()) {
                appendLine()
                appendLine("Observaciones de diseño / taller:")
                appendLine(ctx.notasDiseno)
            }
        }.trimEnd()
    }

    fun textoCompacto(ctx: LaboratorioTicketContext, ultimaEval: EvaluacionClinica?): String {
        val requiereAltura =
            ctx.tipoLente == "Bifocal" || ctx.tipoLente == "Progresivo" || ctx.tipoLente == "Ocupacional"
        val tratamientosStr = tratamientosStr(ctx.tratamientos).replace(", ", "/")
        val alturaParaFormula =
            if (requiereAltura && ctx.altura.isNotBlank()) ctx.altura.trim() else ""
        val bloqueFormula = LaboratorioTicketText.bloqueFormulaLaboratorio(ultimaEval, alturaParaFormula)
        val lines = mutableListOf<String>()
        lines.add("OT: ${ctx.ot.ifBlank { "—" }}")
        lines.add("Fecha: ${DateUtils.formatLocalized(ctx.fecha)}")
        lines.add("Paciente: ${ctx.pacienteNombre.ifBlank { "—" }}")
        lines.add("Fórmula")
        if (bloqueFormula.isNotBlank()) {
            bloqueFormula.lineSequence().forEach { lines.add(it) }
        } else {
            lines.add("—")
        }
        if (ctx.tipoLente.isNotBlank()) {
            var l = ctx.tipoLente
            if (ctx.subTipoBifocal.isNotBlank()) l += " (${ctx.subTipoBifocal})"
            if (ctx.tipoLente == "Monofocal" && ctx.distanciaLente.isNotBlank()) l += " - ${ctx.distanciaLente}"
            lines.add("Lente: $l")
        }
        if (ctx.materialLente.isNotBlank()) lines.add("Mat ${ctx.materialLente}")
        if (ctx.colorLente.isNotBlank()) lines.add("Color ${ctx.colorLente}")
        if (tratamientosStr.isNotBlank()) lines.add("Trat: $tratamientosStr")
        val monturaBits = listOfNotNull(
            ctx.origenMontura.takeIf { it.isNotBlank() }?.let { "Origen: $it" },
            ctx.tipoAro.takeIf { it.isNotBlank() }?.let { "Aro: $it" },
            ctx.materialMontura.takeIf { it.isNotBlank() }?.let { "Mat: $it" },
            ctx.descripcionMontura.takeIf { it.isNotBlank() },
            ctx.tipoMontura.takeIf { it.isNotBlank() }?.let { "Tipo: $it" }
        )
        if (monturaBits.isNotEmpty()) {
            lines.add("Montura")
            monturaBits.forEach { lines.add(it) }
        }
        if (ctx.notasDiseno.isNotBlank()) lines.add("Obs: ${ctx.notasDiseno}")
        return lines.joinToString("\n")
    }
}
