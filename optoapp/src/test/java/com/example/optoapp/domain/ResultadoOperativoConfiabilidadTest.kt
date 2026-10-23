package com.example.optoapp.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultadoOperativoConfiabilidadTest {

    @Test
    fun incompleto_whenSinDatos() {
        assertTrue(ResultadoOperativoConfiabilidad.esIncompleto(CoberturaCosto()))
    }

    @Test
    fun incompleto_whenPctRealBelowThreshold() {
        assertTrue(
            ResultadoOperativoConfiabilidad.esIncompleto(
                CoberturaCosto(lineasTotal = 10, lineasReales = 7, pctReal = 70.0, pctFaltante = 30.0),
            ),
        )
    }

    @Test
    fun completo_whenPctRealAtOrAboveThreshold() {
        assertFalse(
            ResultadoOperativoConfiabilidad.esIncompleto(
                CoberturaCosto(lineasTotal = 10, lineasReales = 8, pctReal = 80.0, pctFaltante = 20.0),
            ),
        )
    }
}
