package com.example.optoapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Primary get() = OptoTokens.colors.primary
val PrimaryDark get() = OptoTokens.colorsDark.primary

val Secondary get() = OptoTokens.colors.secondary
val SecondaryDark get() = OptoTokens.colorsDark.secondary

val Error get() = OptoTokens.colors.error
val ErrorDark get() = OptoTokens.colorsDark.error

val Background get() = OptoTokens.colors.background
val BackgroundDark get() = OptoTokens.colorsDark.background

val Surface get() = OptoTokens.colors.surface
val SurfaceDark get() = OptoTokens.colorsDark.surface

val OnPrimary get() = OptoTokens.colors.onPrimary
val OnPrimaryDark get() = OptoTokens.colorsDark.onPrimary

val OnBackground get() = OptoTokens.colors.onBackground
val OnBackgroundDark get() = OptoTokens.colorsDark.onBackground

val OnSurface get() = OptoTokens.colors.onSurface
val OnSurfaceDark get() = OptoTokens.colorsDark.onSurface

val SurfaceVariant get() = OptoTokens.colors.surfaceVariant
val SurfaceVariantDark get() = OptoTokens.colorsDark.surfaceVariant

val OnSurfaceVariant get() = OptoTokens.colors.onSurfaceVariant
val OnSurfaceVariantDark get() = OptoTokens.colorsDark.onSurfaceVariant

val Outline get() = OptoTokens.colors.outline
val OutlineDark get() = OptoTokens.colorsDark.outline

val OutlineVariant get() = OptoTokens.colors.outlineVariant
val OutlineVariantDark get() = OptoTokens.colorsDark.outlineVariant

val InverseSurface get() = OptoTokens.colors.inverseSurface
val InverseSurfaceDark get() = OptoTokens.colorsDark.inverseSurface

val Scrim get() = OptoTokens.colors.scrim
val SurfaceTint get() = OptoTokens.colors.surfaceTint
val SurfaceTintDark get() = OptoTokens.colorsDark.surfaceTint

@Deprecated("Use MaterialTheme.colorScheme.warningGold instead", ReplaceWith("warningGold"))
val WarningGold = Color(0xFFD97706)
@Deprecated("Use OptoTokens.semantic.warningGoldDark instead", ReplaceWith("OptoTokens.semantic.warningGoldDark"))
val WarningGoldDark = Color(0xFFFBBF24)

@Deprecated("Use MaterialTheme.colorScheme.positiveGreen instead", ReplaceWith("positiveGreen"))
val PositiveGreen = Color(0xFF27AE60)
@Deprecated("Use MaterialTheme.colorScheme.alertRed instead", ReplaceWith("alertRed"))
val AlertRed = Color(0xFFE74C3C)
@Deprecated("Use MaterialTheme.colorScheme.textDark instead", ReplaceWith("textDark"))
val TextDark = Color(0xFF2C3E50)
@Deprecated("Use MaterialTheme.colorScheme.warningAmber instead", ReplaceWith("warningAmber"))
val WarningAmber = Color(0xFFF39C12)
val TextPrimaryDark = OnBackgroundDark
val TextSecondaryDark = OnSurfaceVariantDark
val SurfaceDarkMuted = SurfaceVariantDark

// Provided by OptoAppTheme so semantic colors follow the in-app theme, not the system setting.
val LocalOptoDarkTheme = staticCompositionLocalOf { false }

data class SemanticPalette(
    val positiveGreen: Color,
    val alertRed: Color,
    val warningGold: Color,
    val warningAmber: Color,
    val textDark: Color,
    val maleBlue: Color,
    val femaleRose: Color,
) {
    companion object {
        private val light = SemanticPalette(
            positiveGreen = OptoTokens.semantic.positiveGreenLight,
            alertRed = OptoTokens.semantic.alertRedLight,
            warningGold = OptoTokens.semantic.warningGoldLight,
            warningAmber = OptoTokens.semantic.warningAmberLight,
            textDark = OptoTokens.semantic.textDarkLight,
            maleBlue = OptoTokens.semantic.maleBlueLight,
            femaleRose = OptoTokens.semantic.femaleRoseLight,
        )
        private val dark = SemanticPalette(
            positiveGreen = OptoTokens.semantic.positiveGreenDark,
            alertRed = OptoTokens.semantic.alertRedDark,
            warningGold = OptoTokens.semantic.warningGoldDark,
            warningAmber = OptoTokens.semantic.warningAmberDark,
            textDark = OptoTokens.semantic.textDarkDark,
            maleBlue = OptoTokens.semantic.maleBlueDark,
            femaleRose = OptoTokens.semantic.femaleRoseDark,
        )

        fun forDarkTheme(darkTheme: Boolean): SemanticPalette = if (darkTheme) dark else light
    }
}

private val inAppThemePalette: SemanticPalette
    @Composable get() = SemanticPalette.forDarkTheme(LocalOptoDarkTheme.current)

val ColorScheme.positiveGreen: Color
    @Composable get() = inAppThemePalette.positiveGreen

val ColorScheme.alertRed: Color
    @Composable get() = inAppThemePalette.alertRed

val ColorScheme.warningGold: Color
    @Composable get() = inAppThemePalette.warningGold

val ColorScheme.warningAmber: Color
    @Composable get() = inAppThemePalette.warningAmber

val ColorScheme.textDark: Color
    @Composable get() = inAppThemePalette.textDark

val ColorScheme.maleBlue: Color
    @Composable get() = inAppThemePalette.maleBlue

val ColorScheme.femaleRose: Color
    @Composable get() = inAppThemePalette.femaleRose