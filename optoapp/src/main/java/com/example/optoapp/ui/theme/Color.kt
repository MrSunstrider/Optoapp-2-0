package com.example.optoapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- Paleta desde OptoTokens (fuente única de verdad) ---
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

// --- Advertencia (independiente de M3) ---
@Deprecated("Use MaterialTheme.colorScheme.warningGold instead", ReplaceWith("warningGold"))
val WarningGold = Color(0xFFD97706)
@Deprecated("Use OptoTokens.semantic.warningGoldDark instead", ReplaceWith("OptoTokens.semantic.warningGoldDark"))
val WarningGoldDark = Color(0xFFFBBF24)

// --- Colores semánticos para Análisis de Negocio ---
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

// --- Semantic ColorScheme extensions (dark-mode-aware) ---
val ColorScheme.positiveGreen: Color
    @Composable get() = if (isSystemInDarkTheme()) OptoTokens.semantic.positiveGreenDark else OptoTokens.semantic.positiveGreenLight

val ColorScheme.alertRed: Color
    @Composable get() = if (isSystemInDarkTheme()) OptoTokens.semantic.alertRedDark else OptoTokens.semantic.alertRedLight

val ColorScheme.warningGold: Color
    @Composable get() = if (isSystemInDarkTheme()) OptoTokens.semantic.warningGoldDark else OptoTokens.semantic.warningGoldLight

val ColorScheme.warningAmber: Color
    @Composable get() = if (isSystemInDarkTheme()) OptoTokens.semantic.warningAmberDark else OptoTokens.semantic.warningAmberLight

val ColorScheme.textDark: Color
    @Composable get() = if (isSystemInDarkTheme()) OptoTokens.semantic.textDarkDark else OptoTokens.semantic.textDarkLight
