package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GastoFueraDeRangoTest {

    @Test
    fun alerta_whenGastosOver40PercentOfVentas() {
        val alerta = GastoFueraDeRango.evaluar(ventasMes = 10_000.0, gastosMes = 4_500.0)
        assertTrue(alerta!!.activa)
        assertEquals(45.0, alerta.ratioPct, 0.001)
    }

    @Test
    fun noAlerta_whenAtOrBelow40Percent() {
        assertNull(GastoFueraDeRango.evaluar(ventasMes = 10_000.0, gastosMes = 4_000.0))
        assertNull(GastoFueraDeRango.evaluar(ventasMes = 10_000.0, gastosMes = 3_000.0))
    }

    @Test
    fun noAlerta_whenVentasZero() {
        assertNull(GastoFueraDeRango.evaluar(ventasMes = 0.0, gastosMes = 100.0))
    }

    @Test
    fun threshold_matchesRecommendationEngine() {
        assertEquals(0.4, GastoFueraDeRango.RATIO_ALERTA, 0.0)
        assertFalse(GastoFueraDeRango.estaFueraDeRango(ratio = 0.4))
        assertTrue(GastoFueraDeRango.estaFueraDeRango(ratio = 0.4001))
    }
}
