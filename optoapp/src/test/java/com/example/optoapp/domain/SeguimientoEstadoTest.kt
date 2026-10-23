package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeguimientoEstadoTest {

    @Test
    fun hecho_mapsToFueUtilTrue() {
        assertTrue(SeguimientoEstado.HECHO.fueUtil)
    }

    @Test
    fun enCursoAndNoAplica_mapToFueUtilFalse() {
        assertFalse(SeguimientoEstado.EN_CURSO.fueUtil)
        assertFalse(SeguimientoEstado.NO_APLICA.fueUtil)
    }

    @Test
    fun fromStorage_legacyMasTarde_mapsToEnCurso() {
        assertEquals(SeguimientoEstado.EN_CURSO, SeguimientoEstado.fromStorage("MAS_TARDE"))
    }

    @Test
    fun fromStorage_roundTripsKnownValues() {
        SeguimientoEstado.entries.forEach { estado ->
            assertEquals(estado, SeguimientoEstado.fromStorage(estado.name))
        }
    }

    @Test
    fun fromStorage_nullOrBlank_returnsNull() {
        assertNull(SeguimientoEstado.fromStorage(null))
        assertNull(SeguimientoEstado.fromStorage(""))
        assertNull(SeguimientoEstado.fromStorage("  "))
    }

    @Test
    fun fromStorage_unknown_returnsNull() {
        assertNull(SeguimientoEstado.fromStorage("IGNORED"))
    }
}
