package com.example.optoapp.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class OptoDarkThemeOverrideTest {

    @Test
    fun app_theme_forced_dark_provides_the_in_app_flag_instead_of_the_system_setting() {
        val provided = optoThemeLocal(darkTheme = true)

        assertSame(LocalOptoDarkTheme, provided.compositionLocal)
        assertEquals(true, provided.value)
    }

    @Test
    fun app_theme_light_provides_false() {
        assertEquals(false, optoThemeLocal(darkTheme = false).value)
    }

    @Test
    fun dark_flag_selects_the_dark_finance_tokens() {
        val palette = SemanticPalette.forDarkTheme(darkTheme = true)

        assertEquals(
            listOf(
                OptoTokens.semantic.positiveGreenDark,
                OptoTokens.semantic.alertRedDark,
                OptoTokens.semantic.warningGoldDark,
                OptoTokens.semantic.warningAmberDark,
                OptoTokens.semantic.textDarkDark,
            ),
            listOf(palette.positiveGreen, palette.alertRed, palette.warningGold, palette.warningAmber, palette.textDark),
        )
    }

    @Test
    fun light_flag_selects_the_light_finance_tokens() {
        val palette = SemanticPalette.forDarkTheme(darkTheme = false)

        assertEquals(
            listOf(
                OptoTokens.semantic.positiveGreenLight,
                OptoTokens.semantic.alertRedLight,
                OptoTokens.semantic.warningGoldLight,
                OptoTokens.semantic.warningAmberLight,
                OptoTokens.semantic.textDarkLight,
            ),
            listOf(palette.positiveGreen, palette.alertRed, palette.warningGold, palette.warningAmber, palette.textDark),
        )
    }
}
