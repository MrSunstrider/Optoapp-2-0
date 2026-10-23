package com.example.optoapp.domain

import com.example.optoapp.util.AnalisisPercentFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

enum class ConfianzaInterpretacion {
    ALTA,
    MEDIA,
    BAJA,
}

data class InterpretacionMes(
    val titular: String,
    val puntos: List<String>,
    val confianza: ConfianzaInterpretacion,
    val proximaAccion: String?,
    /** Explicit fiscal assumption: no IGV breakdown in registered sales/costs. */
    val disclaimerIgv: String = DISCLAIMER_IGV,
) {
    companion object {
        const val DISCLAIMER_IGV =
            "Sin desglose de IGV; cifras según ventas y costos registrados."
    }
}

/**
 * Builds a short coach-style reading of [MarginFacts] for the Análisis home screen.
 * Copy stays concise; Reportes keeps the full evidence tables.
 */
object InterpretacionAnalista {

    private const val COBERTURA_ALTA = 80.0
    private const val COBERTURA_MEDIA = 50.0

    fun from(facts: MarginFacts, topRecTitulo: String?): InterpretacionMes {
        val confianza = confianzaDe(facts.cobertura)
        val titular = titularDe(facts)
        val puntos = buildList {
            addBrecha(facts)
            addCobertura(facts.cobertura)
            addPuntoEquilibrio(facts)
            addTendencia(facts.serie6m)
        }.take(4)

        return InterpretacionMes(
            titular = titular,
            puntos = puntos.ifEmpty { listOf("Revisa el detalle del mes para ver ventas, costos y gastos.") },
            confianza = confianza,
            proximaAccion = topRecTitulo?.takeIf { it.isNotBlank() },
            disclaimerIgv = InterpretacionMes.DISCLAIMER_IGV,
        )
    }

    /** Prefers MarginFacts; falls back to AnalisisMensual headline fields when facts are empty defaults. */
    fun fromAnalisis(analisis: AnalisisMensual, topRecTitulo: String?): InterpretacionMes {
        val base = analisis.marginFacts
        val useHeadline = base.margenNetoPct == 0.0 && !base.detalleServidorDisponible && analisis.margenNetoPct != 0.0
        val facts = if (useHeadline) {
            base.copy(
                margenNetoPct = analisis.margenNetoPct,
                ventasMes = if (base.ventasMes != 0.0) base.ventasMes else analisis.ventasMes,
                brechaMargenNetoPp = analisis.margenNetoPct - base.margenNetoObjetivo,
            )
        } else {
            base
        }
        return from(facts, topRecTitulo)
    }

    private fun confianzaDe(cobertura: CoberturaCosto): ConfianzaInterpretacion = when {
        cobertura.sinDatos -> ConfianzaInterpretacion.BAJA
        cobertura.pctReal >= COBERTURA_ALTA -> ConfianzaInterpretacion.ALTA
        cobertura.pctReal >= COBERTURA_MEDIA -> ConfianzaInterpretacion.MEDIA
        else -> ConfianzaInterpretacion.BAJA
    }

    private fun titularDe(facts: MarginFacts): String {
        val pct = AnalisisPercentFormatter.formatWithSymbol(facts.margenNetoPct)
        val objetivo = AnalisisPercentFormatter.formatWithSymbol(facts.margenNetoObjetivo)
        val incompleto = ResultadoOperativoConfiabilidad.esIncompleto(facts.cobertura)
        val base = when {
            facts.brechaMargenNetoPp < -0.5 ->
                "El resultado operativo del mes ($pct) está debajo del objetivo ($objetivo)."
            facts.brechaMargenNetoPp > 0.5 ->
                "El resultado operativo del mes ($pct) está por encima del objetivo ($objetivo). Buen mes."
            else ->
                "El resultado operativo del mes ($pct) está cerca del objetivo ($objetivo)."
        }
        return if (incompleto) "$base (incompleto)" else base
    }

    private fun MutableList<String>.addBrecha(facts: MarginFacts) {
        val brecha = facts.brechaMargenNetoPp
        if (abs(brecha) < 0.5) return
        val pp = abs(brecha).roundToInt()
        if (brecha < 0) {
            add("Te faltan alrededor de $pp puntos porcentuales para llegar al objetivo de resultado operativo.")
        } else {
            add("Hay $pp puntos porcentuales por encima del objetivo de resultado operativo.")
        }
    }

    private fun MutableList<String>.addCobertura(cobertura: CoberturaCosto) {
        if (cobertura.sinDatos) {
            add("Aún no hay líneas de costo para medir cobertura; los márgenes pueden ser poco confiables.")
            return
        }
        if (cobertura.pctReal < COBERTURA_ALTA) {
            val pct = cobertura.pctReal.roundToInt()
            add("La cobertura de costos reales es del $pct%; completa costos faltantes para confiar más en el análisis.")
        }
    }

    private fun MutableList<String>.addPuntoEquilibrio(facts: MarginFacts) {
        val pe = facts.puntoEquilibrio ?: return
        if (pe <= 0.0 || facts.ventasMes <= 0.0) return
        if (facts.ventasMes < pe) {
            val falta = (pe - facts.ventasMes).roundToInt()
            add("Aún no se alcanza el punto de equilibrio; faltan cerca de S/ $falta en ventas.")
        }
    }

    private fun MutableList<String>.addTendencia(serie: List<PuntoSerieMensual>) {
        if (serie.size < 2) return
        val prev = serie[serie.lastIndex - 1].margenNetoPct
        val curr = serie.last().margenNetoPct
        val delta = curr - prev
        if (abs(delta) < 0.5) return
        val pp = AnalisisPercentFormatter.format(abs(delta).coerceAtLeast(0.1))
        if (delta < 0) {
            add("La tendencia del resultado operativo bajó unos $pp puntos vs el mes anterior.")
        } else {
            add("La tendencia del resultado operativo subió unos $pp puntos vs el mes anterior.")
        }
    }
}
