package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CostosPorLineaTest {

    @Test
    fun total_onConvergenceFixture_equalsCostoMes() {
        val facts = MarginFactsParser.parse(MarginFactsFixtures.fullResponse())

        assertEquals(facts.costoMes, facts.costosPorLinea.total(), 0.01)
    }

    @Test
    fun total_ofDefaultLines_isZero() {
        assertEquals(0.0, CostosPorLinea().total(), 0.0)
    }

    @Test
    fun total_addsEveryLineIncludingServiceBucketsAndGifts() {
        val lines = CostosPorLinea(
            lentes = 1.0,
            montura = 2.0,
            lc = 4.0,
            biselado = 8.0,
            serviciosPropios = 16.0,
            serviciosTercerizados = 32.0,
            serviciosSinClasificar = 64.0,
            regalos = 128.0,
        )

        assertEquals(255.0, lines.total(), 0.0)
    }
}
