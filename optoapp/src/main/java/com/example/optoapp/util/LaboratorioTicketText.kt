package com.example.optoapp.util

import com.example.optoapp.data.EvaluacionClinica
import java.util.Locale

/**
 * Texto para ticket de laboratorio: fórmula (OD/OI), prismas si aplica, ADD, DIP/DNP y altura.
 */
object LaboratorioTicketText {

    private const val COL_W = 42

    /**
     * Bloque multilínea: OD/OI con columna DNP/DIP, prismas si hay datos, luego Add y opcionalmente h: (altura).
     */
    fun bloqueFormulaLaboratorio(ev: EvaluacionClinica?, alturaMontajeMm: String = ""): String {
        if (ev == null) return ""
        val odRx = formatoOjoFormula(ev.recetaOdEsf, ev.recetaOdCil, ev.recetaOdEje, "OD")
        val oiRx = formatoOjoFormula(ev.recetaOiEsf, ev.recetaOiCil, ev.recetaOiEje, "OI")
        val (odRight, oiRight) = columnasDnpDip(ev)

        val lines = mutableListOf<String>()
        lines.add(dosColumnas(odRx, odRight))
        lines.add(dosColumnas(oiRx, oiRight))

        lineaPrismas(ev)?.let { lines.add(it) }

        val addTxt = textoAdd(ev)
        val altura = alturaMontajeMm.trim()
        val addLine = when {
            addTxt.isNotBlank() && altura.isNotBlank() ->
                dosColumnas("Add: $addTxt", "h: $altura mm")
            addTxt.isNotBlank() -> "Add: $addTxt"
            altura.isNotBlank() -> dosColumnas("Add: —", "h: $altura mm")
            else -> ""
        }
        if (addLine.isNotBlank()) lines.add(addLine)

        return lines.filter { it.isNotBlank() }.joinToString("\n")
    }

    /** Una sola línea (p. ej. WhatsApp muy corto); preferir [bloqueFormulaLaboratorio]. */
    fun lineaRefraccionFinalYDipNdP(ev: EvaluacionClinica?): String {
        if (ev == null) return ""
        val od = rxOjoLinePlano(ev.recetaOdEsf, ev.recetaOdCil, ev.recetaOdEje, "OD")
        val oi = rxOjoLinePlano(ev.recetaOiEsf, ev.recetaOiCil, ev.recetaOiEje, "OI")
        val rx = listOf(od, oi).filter { it.isNotBlank() }.joinToString(" | ")
        val addSeg = addicionSegment(ev)
        val dipNdP = dipDnpSegment(ev)
        return listOfNotNull(
            rx.takeIf { it.isNotBlank() },
            addSeg.takeIf { it.isNotBlank() },
            dipNdP.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
    }

    private fun formatoOjoFormula(esf: String, cil: String, eje: String, tag: String): String {
        val e = esf.ifBlank { "—" }
        val c = cil.ifBlank { "—" }
        val ej = eje.ifBlank { "—" }
        return "$tag: $e / $c × $ej°"
    }

    private fun rxOjoLinePlano(esf: String, cil: String, eje: String, tag: String): String {
        if (esf.isBlank() && cil.isBlank() && eje.isBlank()) return ""
        val e = esf.ifBlank { "—" }
        val c = cil.ifBlank { "—" }
        val ej = eje.ifBlank { "—" }
        return "$tag $e / $c x $ej°"
    }

    /**
     * DNP en línea OD si aplica; DIP (y DNP suelto en OI si no salió en OD) en línea OI.
     */
    private fun columnasDnpDip(ev: EvaluacionClinica): Pair<String, String> {
        val dip = dipFormateado(ev)
        when {
            ev.dnpOdMm != null && ev.dnpOiMm != null -> {
                val odR = "DNP ${fmt(ev.dnpOdMm)}/${fmt(ev.dnpOiMm)} mm"
                val oiR = listOfNotNull(dip.takeIf { it.isNotBlank() })
                    .joinToString(" · ")
                return odR to oiR
            }
            ev.dnpOdMm != null -> {
                val odR = "DNP OD ${fmt(ev.dnpOdMm)} mm"
                val oiParts = buildList {
                    ev.dnpOiMm?.let { add("DNP OI ${fmt(it)} mm") }
                    if (dip.isNotBlank()) add(dip)
                }
                return odR to oiParts.joinToString(" · ")
            }
            ev.dnpOiMm != null -> {
                val odR = ""
                val oiParts = buildList {
                    add("DNP OI ${fmt(ev.dnpOiMm)} mm")
                    if (dip.isNotBlank()) add(dip)
                }
                return odR to oiParts.joinToString(" · ")
            }
            else -> return "" to dip
        }
    }

    private fun dipFormateado(ev: EvaluacionClinica): String {
        val parts = mutableListOf<String>()
        if (ev.dipLejos.isNotBlank()) parts.add("${ev.dipLejos.trim()} (lejos)")
        if (ev.dipCerca.isNotBlank()) parts.add("${ev.dipCerca.trim()} (cerca)")
        if (ev.dipIntermedio.isNotBlank()) parts.add("${ev.dipIntermedio.trim()} (interm.)")
        val total = ev.dipTotalMm
        if (parts.isEmpty() && total != null) {
            parts.add("${fmt(total)} mm (DIP total)")
        } else if (parts.isNotEmpty() && total != null) {
            parts.add("${fmt(total)} mm (total)")
        }
        return parts.joinToString(" / ")
    }

    /** Prismas prescritos, si hay. */
    private fun lineaPrismas(ev: EvaluacionClinica): String? {
        fun ojo(valor: String, base: String, tag: String): String? {
            if (valor.isBlank()) return null
            val b = base.trim().takeIf { it.isNotBlank() }
            return if (b != null) "$tag $valor (base $b)" else "$tag $valor"
        }
        val od = ojo(ev.prismaOdValor, ev.prismaOdBase, "OD")
        val oi = ojo(ev.prismaOiValor, ev.prismaOiBase, "OI")
        if (od == null && oi == null) return null
        return "Prisma: ${listOfNotNull(od, oi).joinToString(" · ")}"
    }

    private fun textoAdd(ev: EvaluacionClinica): String {
        val parts = mutableListOf<String>()
        val cerca = listOfNotNull(
            ev.addCercaOd.takeIf { it.isNotBlank() }?.let { "cerca OD $it" },
            ev.addCercaOi.takeIf { it.isNotBlank() }?.let { "cerca OI $it" }
        )
        if (cerca.isNotEmpty()) parts.add(cerca.joinToString(" · "))
        val inter = listOfNotNull(
            ev.addIntermediaOd.takeIf { it.isNotBlank() }?.let { "int. OD $it" },
            ev.addIntermediaOi.takeIf { it.isNotBlank() }?.let { "int. OI $it" }
        )
        if (inter.isNotEmpty()) parts.add(inter.joinToString(" · "))
        return parts.joinToString(" · ")
    }

    private fun addicionSegment(ev: EvaluacionClinica): String {
        val parts = mutableListOf<String>()
        val cerca = listOfNotNull(
            ev.addCercaOd.takeIf { it.isNotBlank() }?.let { "cerca OD $it" },
            ev.addCercaOi.takeIf { it.isNotBlank() }?.let { "cerca OI $it" }
        )
        if (cerca.isNotEmpty()) parts.add("ADD ${cerca.joinToString(" | ")}")
        val inter = listOfNotNull(
            ev.addIntermediaOd.takeIf { it.isNotBlank() }?.let { "int. OD $it" },
            ev.addIntermediaOi.takeIf { it.isNotBlank() }?.let { "int. OI $it" }
        )
        if (inter.isNotEmpty()) parts.add("ADD ${inter.joinToString(" | ")}")
        return parts.joinToString(" · ")
    }

    private fun dipDnpSegment(ev: EvaluacionClinica): String {
        val parts = mutableListOf<String>()
        when {
            ev.dnpOdMm != null && ev.dnpOiMm != null ->
                parts.add("DNP ${fmt(ev.dnpOdMm)}/${fmt(ev.dnpOiMm)} mm")
            else -> {
                if (ev.dnpOdMm != null) parts.add("DNP OD ${fmt(ev.dnpOdMm)} mm")
                if (ev.dnpOiMm != null) parts.add("DNP OI ${fmt(ev.dnpOiMm)} mm")
            }
        }
        if (ev.dipLejos.isNotBlank()) parts.add("DIP lejos ${ev.dipLejos}")
        if (ev.dipCerca.isNotBlank()) parts.add("DIP cerca ${ev.dipCerca}")
        if (ev.dipIntermedio.isNotBlank()) parts.add("DIP interm. ${ev.dipIntermedio}")
        val total = ev.dipTotalMm
        if (total != null) {
            if (parts.isEmpty()) parts.add("DIP total ${fmt(total)} mm")
            else parts.add("DIP total ${fmt(total)} mm")
        }
        return parts.joinToString(" · ")
    }

    private fun dosColumnas(izq: String, der: String): String {
        val left = izq.trimEnd()
        val right = der.trim()
        if (right.isEmpty()) return left
        if (left.length >= COL_W) return "$left\n    $right"
        return left + " ".repeat(COL_W - left.length) + right
    }

    private fun fmt(d: Double) = String.format(Locale.US, "%.1f", d)
}
