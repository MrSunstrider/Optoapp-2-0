package com.example.optoapp.ui.components.paciente

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [validateFechaNacimiento] extracted from [PacienteFormSections].
 *
 * Covers C1: intermediate-length input validation.
 */
class PacienteFormSectionsTest {

    @Test
    fun `empty input returns null`() {
        assertNull(validateFechaNacimiento(""))
    }

    @Test
    fun `short input 2 chars returns incomplete error`() {
        assertEquals("Fecha completa requerida (8 dígitos)", validateFechaNacimiento("12"))
    }

    @Test
    fun `short input 7 chars returns incomplete error`() {
        assertEquals("Fecha completa requerida (8 dígitos)", validateFechaNacimiento("3102202"))
    }

    @Test
    fun `valid 8 digit date returns null`() {
        assertNull(validateFechaNacimiento("15061990"))
    }

    @Test
    fun `valid 8 digit date 29022020 returns null`() {
        assertNull(validateFechaNacimiento("29022020"))
    }

    @Test
    fun `invalid month returns error`() {
        assertEquals("Mes debe ser 1-12", validateFechaNacimiento("15302020"))
    }

    @Test
    fun `invalid day returns error`() {
        assertEquals("Día debe ser 1-31", validateFechaNacimiento("32122020"))
    }

    @Test
    fun `invalid date 31022020 returns error`() {
        assertEquals("Fecha inválida", validateFechaNacimiento("31022020"))
    }
}
