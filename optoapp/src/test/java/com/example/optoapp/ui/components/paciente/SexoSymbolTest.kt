package com.example.optoapp.ui.components.paciente

import com.example.optoapp.ui.theme.OptoTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `mars uses blue and venus uses rose in light and dark`() {
        val maleLight = sexoAvatarColor(SexoSymbol.MARTE, darkTheme = false)
        val maleDark = sexoAvatarColor(SexoSymbol.MARTE, darkTheme = true)
        val femaleLight = sexoAvatarColor(SexoSymbol.VENUS, darkTheme = false)
        val femaleDark = sexoAvatarColor(SexoSymbol.VENUS, darkTheme = true)

        assertEquals(OptoTokens.semantic.maleBlueLight, maleLight)
        assertEquals(OptoTokens.semantic.maleBlueDark, maleDark)
        assertEquals(OptoTokens.semantic.femaleRoseLight, femaleLight)
        assertEquals(OptoTokens.semantic.femaleRoseDark, femaleDark)
        assertTrue(maleLight != maleDark)
        assertTrue(femaleLight != femaleDark)
        assertTrue(maleLight != femaleLight)
    }

    @Test
    fun `unknown sex has no dedicated avatar color`() {
        assertEquals(null, sexoAvatarColor(SexoSymbol.DESCONOCIDO, darkTheme = false))
        assertEquals(null, sexoAvatarColor(SexoSymbol.DESCONOCIDO, darkTheme = true))
    }
}
