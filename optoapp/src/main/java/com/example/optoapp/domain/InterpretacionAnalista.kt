package com.example.optoapp.domain

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
)

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
            puntos = puntos.ifEmpty { listOf("Revisá el detalle del mes para ver ventas, costos y gastos.") },
            confianza = confianza,
            proximaAccion = topRecTitulo?.takeIf { it.isNotBlank() },
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
        val pct = facts.margenNetoPct.roundToInt()
        val objetivo = facts.margenNetoObjetivo.roundToInt()
        return when {
            facts.brechaMargenNetoPp < -0.5 ->
                "El margen neto del mes ($pct%) está debajo del objetivo ($objetivo%)."
            facts.brechaMargenNetoPp > 0.5 ->
                "El margen neto del mes ($pct%) está por encima del objetivo ($objetivo%). Vas bien."
            else ->
                "El margen neto del mes ($pct%) está cerca del objetivo ($objetivo%)."
        }
    }

    private fun MutableList<String>.addBrecha(facts: MarginFacts) {
        val brecha = facts.brechaMargenNetoPp
        if (abs(brecha) < 0.5) return
        val pp = abs(brecha).roundToInt()
        if (brecha < 0) {
            add("Te faltan alrededor de $pp puntos porcentuales para llegar al objetivo de margen.")
        } else {
            add("Estás $pp puntos porcentuales por encima del objetivo de margen.")
        }
    }

    private fun MutableList<String>.addCobertura(cobertura: CoberturaCosto) {
        if (cobertura.sinDatos) {
            add("Aún no hay líneas de costo para medir cobertura; los márgenes pueden ser poco confiables.")
            return
        }
        if (cobertura.pctReal < COBERTURA_ALTA) {
            val pct = cobertura.pctReal.roundToInt()
            add("La cobertura de costos reales es del $pct%; completá costos faltantes para confiar más en el análisis.")
        }
    }

    private fun MutableList<String>.addPuntoEquilibrio(facts: MarginFacts) {
        val pe = facts.puntoEquilibrio ?: return
        if (pe <= 0.0 || facts.ventasMes <= 0.0) return
        if (facts.ventasMes < pe) {
            val falta = (pe - facts.ventasMes).roundToInt()
            add("Aún no llegás al punto de equilibrio; faltan cerca de S/ $falta en ventas.")
        }
    }

    private fun MutableList<String>.addTendencia(serie: List<PuntoSerieMensual>) {
        if (serie.size < 2) return
        val prev = serie[serie.lastIndex - 1].margenNetoPct
        val curr = serie.last().margenNetoPct
        val delta = curr - prev
        if (abs(delta) < 0.5) return
        val pp = abs(delta).roundToInt().coerceAtLeast(1)
        if (delta < 0) {
            add("La tendencia del margen neto bajó unos $pp puntos vs el mes anterior.")
        } else {
            add("La tendencia del margen neto subió unos $pp puntos vs el mes anterior.")
        }
    }
}
