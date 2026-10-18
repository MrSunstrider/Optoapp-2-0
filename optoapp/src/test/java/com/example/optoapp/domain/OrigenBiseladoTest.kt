package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrigenBiseladoTest {

    @Test
    fun normalize_mapsUnknownAndBlankToTercero() {
        assertEquals(OrigenBiselado.TERCERO, OrigenBiselado.normalize(null))
        assertEquals(OrigenBiselado.TERCERO, OrigenBiselado.normalize(""))
        assertEquals(OrigenBiselado.TERCERO, OrigenBiselado.normalize("  "))
        assertEquals(OrigenBiselado.TERCERO, OrigenBiselado.normalize("externo"))
    }

    @Test
    fun normalize_keepsPropioAndTercero() {
        assertEquals(OrigenBiselado.PROPIO, OrigenBiselado.normalize("Propio"))
        assertEquals(OrigenBiselado.PROPIO, OrigenBiselado.normalize(" Propio "))
        assertEquals(OrigenBiselado.TERCERO, OrigenBiselado.normalize("Tercero"))
    }

    @Test
    fun isPropio_and_isTercero_areExclusive() {
        assertTrue(OrigenBiselado.isPropio("Propio"))
        assertFalse(OrigenBiselado.isTercero("Propio"))
        assertTrue(OrigenBiselado.isTercero(null))
        assertFalse(OrigenBiselado.isPropio(null))
    }
}
