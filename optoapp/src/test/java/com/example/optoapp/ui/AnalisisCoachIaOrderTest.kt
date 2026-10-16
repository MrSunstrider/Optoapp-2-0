package com.example.optoapp.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural guard: Análisis home stays coach-first (S7/S11), not a KPI-first mini-Reportes.
 */
class AnalisisCoachIaOrderTest {

    private val source get() = MainSources.read(MainSources.ANALISIS_NEGOCIO_SCREEN)

    @Test
    fun interpretacionCoach_appearsBeforePlanDeAccion() {
        val coach = source.indexOf("InterpretacionCoachCard(")
        val plan = source.indexOf("R.string.analisis_recomendaciones")
        assertTrue("coach=$coach plan=$plan", coach in 0 until plan)
    }

    @Test
    fun planDeAccion_appearsBeforeContextoBreve() {
        val plan = source.indexOf("R.string.analisis_recomendaciones")
        val contexto = source.indexOf("ContextoBreveCard(")
        assertTrue("plan=$plan contexto=$contexto", plan in 0 until contexto)
    }

    @Test
    fun contextoBreve_appearsBeforeDemotedResumen() {
        val contexto = source.indexOf("ContextoBreveCard(")
        val resumen = source.indexOf("ResumenCard(")
        assertTrue("contexto=$contexto resumen=$resumen", contexto in 0 until resumen)
    }

    @Test
    fun resumenCard_isBehindExpandedFlag() {
        val source = this.source
        val resumenIdx = source.indexOf("ResumenCard(")
        assertTrue(resumenIdx > 0)
        val before = source.substring(0, resumenIdx)
        assertTrue(
            "ResumenCard must be demoted behind an expanded metrics flag",
            before.contains("mostrarMetricasDetalle") || before.contains("metricasDetalleExpanded"),
        )
    }
}
