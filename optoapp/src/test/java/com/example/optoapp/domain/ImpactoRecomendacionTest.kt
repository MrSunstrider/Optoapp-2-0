package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImpactoRecomendacionTest {

    @Test
    fun cobrar_usesFullDebtAsImpact() {
        val impacto = ImpactoRecomendacion.cobrar(1_500.0)
        assertEquals(1_500.0, impacto.monto!!, 0.001)
        assertTrue(impacto.etiqueta.contains("1") || impacto.etiqueta.contains("500"))
    }

    @Test
    fun mejorarPrecio_estimatesUpliftToTargetMargin() {
        // 10% margin on 1000 ventas → gap to 25% = 150
        val impacto = ImpactoRecomendacion.mejorarPrecio(ventas = 1_000.0, margenPct = 10.0)
        assertEquals(150.0, impacto!!.monto!!, 0.001)
        assertTrue(impacto.etiqueta.isNotBlank())
    }

    @Test
    fun mejorarPrecio_nullWhenAlreadyAtOrAboveTarget() {
        assertNull(ImpactoRecomendacion.mejorarPrecio(ventas = 1_000.0, margenPct = 30.0))
        assertNull(ImpactoRecomendacion.mejorarPrecio(ventas = 1_000.0, margenPct = null))
    }

    @Test
    fun liquidarStock_estimatesCapitalFreedAtSeventyPercentOfCost() {
        val impacto = ImpactoRecomendacion.liquidarStock(costoTotal = 1_000.0)
        assertEquals(700.0, impacto.monto!!, 0.001)
    }

    @Test
    fun venderMasDe_estimatesTenPercentMoreAtCurrentMargin() {
        // 10% more of 2000 at 40% margin = 80
        val impacto = ImpactoRecomendacion.venderMasDe(ventas = 2_000.0, margenPct = 40.0)
        assertEquals(80.0, impacto!!.monto!!, 0.001)
    }

    @Test
    fun alertaCaida_usesGapToExpectedSales() {
        val impacto = ImpactoRecomendacion.alertaCaida(ventasActual = 700.0, ventasEsperado = 1_000.0)
        assertEquals(300.0, impacto!!.monto!!, 0.001)
    }

    @Test
    fun alertaCaida_nullWhenNotBelowExpected() {
        assertNull(ImpactoRecomendacion.alertaCaida(ventasActual = 1_100.0, ventasEsperado = 1_000.0))
    }

    @Test
    fun reducirGasto_estimatesTenPercentCut() {
        val impacto = ImpactoRecomendacion.reducirGasto(gastosMes = 2_000.0)
        assertEquals(200.0, impacto.monto!!, 0.001)
    }

    @Test
    fun etiquetaSinImpacto_isStable() {
        assertEquals("Sin impacto estimable", ImpactoRecomendacion.SIN_IMPACTO)
    }
}
