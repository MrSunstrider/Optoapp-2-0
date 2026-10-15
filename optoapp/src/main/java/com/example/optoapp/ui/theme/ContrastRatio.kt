package com.example.optoapp.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class ContrastRole(val minimum: Double) {
    TEXT(4.5),
    GRAPHIC(3.0),
}

data class ThemedToken(val name: String, val light: Color, val dark: Color, val role: ContrastRole)

object ContrastRatio {

    fun of(foreground: Color, background: Color): Double {
        val a = relativeLuminance(foreground)
        val b = relativeLuminance(background)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    fun violations(
        tokens: List<ThemedToken>,
        lightBackgrounds: List<Color>,
        darkBackgrounds: List<Color>,
    ): List<String> = tokens.flatMap { token ->
        check(token, "light", token.light, lightBackgrounds) + check(token, "dark", token.dark, darkBackgrounds)
    }

    private fun check(token: ThemedToken, theme: String, color: Color, backgrounds: List<Color>): List<String> =
        backgrounds.mapNotNull { background ->
            val ratio = of(color, background)
            if (ratio >= token.role.minimum) {
                null
            } else {
                "${token.name} $theme ${"%.2f".format(ratio)}:1 < ${token.role.minimum}:1 on $background"
            }
        }

    private fun relativeLuminance(color: Color): Double =
        0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)

    private fun linear(channel: Float): Double {
        val c = channel.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
}

object FinanceColorTokens {

    val all: List<ThemedToken> = listOf(
        ThemedToken("positiveGreen", OptoTokens.semantic.positiveGreenLight, OptoTokens.semantic.positiveGreenDark, ContrastRole.TEXT),
        ThemedToken("alertRed", OptoTokens.semantic.alertRedLight, OptoTokens.semantic.alertRedDark, ContrastRole.TEXT),
        ThemedToken("warningGold", OptoTokens.semantic.warningGoldLight, OptoTokens.semantic.warningGoldDark, ContrastRole.TEXT),
        ThemedToken("warningAmber", OptoTokens.semantic.warningAmberLight, OptoTokens.semantic.warningAmberDark, ContrastRole.TEXT),
        ThemedToken("textDark", OptoTokens.semantic.textDarkLight, OptoTokens.semantic.textDarkDark, ContrastRole.TEXT),
        ThemedToken("chartPrimary", OptoTokens.colors.primary, OptoTokens.colorsDark.primary, ContrastRole.GRAPHIC),
        ThemedToken("chartPositive", OptoTokens.semantic.positiveGreenLight, OptoTokens.semantic.positiveGreenDark, ContrastRole.GRAPHIC),
        ThemedToken("chartAlert", OptoTokens.semantic.alertRedLight, OptoTokens.semantic.alertRedDark, ContrastRole.GRAPHIC),
        ThemedToken("chartWarning", OptoTokens.semantic.warningAmberLight, OptoTokens.semantic.warningAmberDark, ContrastRole.GRAPHIC),
    )

    val lightBackgrounds: List<Color> = listOf(OptoTokens.colors.background, OptoTokens.colors.surface).distinct()

    val darkBackgrounds: List<Color> = listOf(OptoTokens.colorsDark.background, OptoTokens.colorsDark.surface).distinct()
}
