package com.example.optoapp.ui.components.paciente

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Maps the free-text `sexo` field to the Mars/Venus glyph shown in the patient list avatar.
 * The column is a nullable String shared with the web companion, so tolerate variants.
 */
class SexoSymbolTest {

    @Test
    fun `masculino maps to mars`() {
        assertEquals(SexoSymbol.MARTE, sexoSymbolOf("Masculino"))
        assertEquals(SexoSymbol.MARTE, sexoSymbolOf("masculino"))
        assertEquals(SexoSymbol.MARTE, sexoSymbolOf("  MASCULINO  "))
    }

    @Test
    fun `femenino maps to venus`() {
        assertEquals(SexoSymbol.VENUS, sexoSymbolOf("Femenino"))
        assertEquals(SexoSymbol.VENUS, sexoSymbolOf("femenino"))
        assertEquals(SexoSymbol.VENUS, sexoSymbolOf("  FEMENINO  "))
    }

    @Test
    fun `single letter shorthand is supported`() {
        assertEquals(SexoSymbol.MARTE, sexoSymbolOf("M"))
        assertEquals(SexoSymbol.VENUS, sexoSymbolOf("F"))
    }

    @Test
    fun `common synonyms are supported`() {
        assertEquals(SexoSymbol.MARTE, sexoSymbolOf("Varón"))
        assertEquals(SexoSymbol.MARTE, sexoSymbolOf("varon"))
        assertEquals(SexoSymbol.MARTE, sexoSymbolOf("Hombre"))
        assertEquals(SexoSymbol.VENUS, sexoSymbolOf("Mujer"))
    }

    @Test
    fun `mujer wins over the masculine m prefix`() {
        assertEquals(SexoSymbol.VENUS, sexoSymbolOf("mujer"))
    }

    @Test
    fun `null blank or unknown maps to desconocido`() {
        assertEquals(SexoSymbol.DESCONOCIDO, sexoSymbolOf(null))
        assertEquals(SexoSymbol.DESCONOCIDO, sexoSymbolOf(""))
        assertEquals(SexoSymbol.DESCONOCIDO, sexoSymbolOf("   "))
        assertEquals(SexoSymbol.DESCONOCIDO, sexoSymbolOf("otro"))
        assertEquals(SexoSymbol.DESCONOCIDO, sexoSymbolOf("x"))
    }
}
