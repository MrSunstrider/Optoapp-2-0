package com.example.optoapp.ui.theme

import androidx.compose.ui.graphics.Color

// --- Paleta Principal (Purple) ---
// Primary: #6D4AFF (Purple)
val Primary get() = OptoTokens.colors.primary
val PrimaryDark get() = OptoTokens.colorsDark.primary

// Secondary: #3DD9A5 (Teal accent)
val Secondary get() = OptoTokens.colors.secondary
val SecondaryDark get() = OptoTokens.colorsDark.secondary

// --- Colores de Error ---
val Error get() = OptoTokens.colors.error
val ErrorDark get() = OptoTokens.colorsDark.error

// --- Fondos ---
val Background get() = OptoTokens.colors.background
val BackgroundDark get() = OptoTokens.colorsDark.background

val Surface get() = OptoTokens.colors.surface
val SurfaceDark get() = OptoTokens.colorsDark.surface

// --- Colores de texto ---
val OnPrimary get() = OptoTokens.colors.onPrimary
val OnPrimaryDark get() = OptoTokens.colorsDark.onPrimary

val OnBackground get() = OptoTokens.colors.onBackground
val OnBackgroundDark get() = OptoTokens.colorsDark.onBackground

val OnSurface get() = OptoTokens.colors.onSurface
val OnSurfaceDark get() = OptoTokens.colorsDark.onSurface

// --- Colores de soporte M3 ---
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

// --- Colores de advertencia (mantenidos para compatibilidad) ---
val WarningGold = Color(0xFFD97706)       // Ámbar
val WarningGoldDark = Color(0xFFFBBF24)    // Ámbar (dark)

// --- Alias de compatibilidad para código existente ---
// Estos alias mantienen compatibilidad con código que aún referencia
// los nombres anteriores. Se reemplazarán completamente en fases posteriores.
val TextPrimaryDark = OnBackgroundDark
val TextSecondaryDark = OnSurfaceVariantDark
val SurfaceDarkMuted = SurfaceVariantDark

// --- DEPRECADO: Colores antiguos ---
// Estos valores están obsoletos y se reemplazan por la nueva paleta de colores.
// Se mantienen aquí solo para compatibilidad con código existente.
// Utilice los nuevos tokens de OptoTokens en su lugar.

// Paleta antigua (verde esmeralda) - DEPRECADA
@Deprecated("Utilice OptoTokens.colors.primary en su lugar")
val OldPrimary = Color(0xFF059669)

@Deprecated("Utilice OptoTokens.colors.secondary en su lugar")
val OldSecondary = Color(0xFF10B981)

@Deprecated("Utilice OptoTokens.colors.error en su lugar")
val OldError = Color(0xFFDC2626)

// Fondos antiguos - DEPRECADOS
@Deprecated("Utilice OptoTokens.colors.background en su lugar")
val OldBackgroundLight = Color(0xFFF5F7FA)

@Deprecated("Utilice OptoTokens.colors.backgroundDark en su lugar")
val OldBackgroundDark = Color(0xFF080C14)

// Superficies antiguas - DEPRECADAS
@Deprecated("Utilice OptoTokens.colors.surface en su lugar")
val OldSurfaceLight = Color(0xFFFFFFFF)

@Deprecated("Utilice OptoTokens.colors.surfaceDark en su lugar")
val OldSurfaceDark = Color(0xFF111827)

// Textos antiguos - DEPRECADOS
@Deprecated("Utilice OptoTokens.colors.onBackground en su lugar")
val OldTextPrimary = Color(0xFF0F172A)

@Deprecated("Utilice OptoTokens.colors.onBackgroundDark en su lugar")
val OldTextPrimaryDark = Color(0xFFF1F5F9)
