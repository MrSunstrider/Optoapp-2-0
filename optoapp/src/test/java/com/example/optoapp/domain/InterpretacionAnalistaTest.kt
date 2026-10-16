package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class InterpretacionAnalistaTest {

    @Test
    fun from_belowTarget_titularWarnsAndListsBrecha() {
        val facts = MarginFacts(
            ventasMes = 10_000.0,
            margenNetoPct = 8.0,
            margenNetoObjetivo = 15.0,
            brechaMargenNetoPp = -7.0,
            cobertura = CoberturaCosto(lineasTotal = 10, lineasReales = 9, pctReal = 90.0),
        )

        val out = InterpretacionAnalista.from(facts, topRecTitulo = "Cobra deudas vencidas")

        assertTrue(out.titular.contains("debajo", ignoreCase = true) || out.titular.contains("bajo", ignoreCase = true))
        assertTrue(out.puntos.any { it.contains("7") || it.contains("objetivo") })
        assertEquals(ConfianzaInterpretacion.ALTA, out.confianza)
        assertEquals("Cobra deudas vencidas", out.proximaAccion)
    }

    @Test
    fun from_atOrAboveTarget_titularPositive() {
        val facts = MarginFacts(
            margenNetoPct = 18.0,
            margenNetoObjetivo = 15.0,
            brechaMargenNetoPp = 3.0,
            cobertura = CoberturaCosto(lineasTotal = 5, lineasReales = 5, pctReal = 100.0),
        )

        val out = InterpretacionAnalista.from(facts, topRecTitulo = null)

        assertTrue(out.titular.contains("objetivo", ignoreCase = true) || out.titular.contains("bien", ignoreCase = true))
        assertEquals(null, out.proximaAccion)
    }

    @Test
    fun from_lowCoverage_confianzaBaja() {
        val facts = MarginFacts(
            margenNetoPct = 20.0,
            cobertura = CoberturaCosto(lineasTotal = 20, lineasReales = 5, pctReal = 25.0, pctFaltante = 75.0),
        )

        val out = InterpretacionAnalista.from(facts, topRecTitulo = null)

        assertEquals(ConfianzaInterpretacion.BAJA, out.confianza)
        assertTrue(out.puntos.any { it.contains("cobertura", ignoreCase = true) || it.contains("costo", ignoreCase = true) })
    }

    @Test
    fun from_serie6m_addsTendenciaWhenEnoughPoints() {
        val mes = YearMonth.of(2026, 10)
        val facts = MarginFacts(
            margenNetoPct = 12.0,
            margenNetoObjetivo = 15.0,
            cobertura = CoberturaCosto(lineasTotal = 4, lineasReales = 4, pctReal = 100.0),
            serie6m = listOf(
                PuntoSerieMensual(mes.minusMonths(1), 1000.0, 400.0, 100.0, 500.0, 10.0),
                PuntoSerieMensual(mes, 900.0, 400.0, 100.0, 400.0, 8.0),
            ),
        )

        val out = InterpretacionAnalista.from(facts, topRecTitulo = null)

        assertTrue(out.puntos.any { it.contains("tendencia", ignoreCase = true) || it.contains("cayó", ignoreCase = true) || it.contains("bajó", ignoreCase = true) })
    }

    @Test
    fun fromAnalisis_fallsBackToHeadlineMargenWhenFactsEmpty() {
        val analisis = AnalisisMensual(
            ventasMes = 10_000.0,
            cobrosMes = 8_000.0,
            margenNetoPct = 35.0,
            margenPorCategoria = emptyList(),
            deudores = DeudoresResumen(0, 0.0),
            proyeccionCaja = null,
            stockEstancado = emptyList(),
            valorInventario = 0.0,
            ventasMesAnterior = 0.0,
            variacionVentasPct = null,
            marginFacts = MarginFacts(),
        )

        val out = InterpretacionAnalista.fromAnalisis(analisis, topRecTitulo = "Cobranza")

        assertTrue(out.titular.contains("35") || out.titular.contains("objetivo"))
        assertEquals("Cobranza", out.proximaAccion)
    }

    @Test
    fun from_limitsPuntosToFour() {
        val facts = MarginFacts(
            margenNetoPct = 5.0,
            margenNetoObjetivo = 15.0,
            brechaMargenNetoPp = -10.0,
            margenContribucionPct = 30.0,
            puntoEquilibrio = 8000.0,
            ventasMes = 5000.0,
            cobertura = CoberturaCosto(lineasTotal = 10, lineasReales = 4, pctReal = 40.0, pctFaltante = 60.0),
            serie6m = listOf(
                PuntoSerieMensual(YearMonth.of(2026, 9), 6000.0, 2000.0, 500.0, 3500.0, 12.0),
                PuntoSerieMensual(YearMonth.of(2026, 10), 5000.0, 2000.0, 500.0, 2500.0, 5.0),
            ),
        )

        val out = InterpretacionAnalista.from(facts, topRecTitulo = "Acción")

        assertTrue(out.puntos.size in 1..4)
        assertFalse(out.puntos.isEmpty())
    }
}
