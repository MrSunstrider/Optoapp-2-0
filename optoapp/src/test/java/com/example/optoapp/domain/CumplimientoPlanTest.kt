package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CumplimientoPlanTest {
    @Test
    fun from_countsEstados() {
        val c = CumplimientoPlan.from(
            mapOf(
                "a" to SeguimientoEstado.HECHO,
                "b" to SeguimientoEstado.HECHO,
                "c" to SeguimientoEstado.EN_CURSO,
                "d" to SeguimientoEstado.NO_APLICA,
            ),
        )
        assertEquals(2, c.hechas)
        assertEquals(1, c.enCurso)
        assertEquals(1, c.noAplica)
        assertEquals(1, c.abiertas)
        assertEquals(4, c.total)
    }

    @Test
    fun from_empty_isZero() {
        val c = CumplimientoPlan.from(emptyMap())
        assertEquals(0, c.total)
        assertEquals(0, c.abiertas)
    }
}
