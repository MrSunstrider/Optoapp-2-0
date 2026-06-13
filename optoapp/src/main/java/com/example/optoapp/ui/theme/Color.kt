package com.example.optoapp.ui.theme

import androidx.compose.ui.graphics.Color

// --- Paleta Principal (Purple) ---
// Primary: #6D4AFF (Purple)
val Primary = Color(0xFF6D4AFF)
val PrimaryDark = Color(0xFF9B8AFF)  // Dark variant for dark theme

// Secondary: #3DD9A5 (Teal accent)
val Secondary = Color(0xFF3DD9A5)
val SecondaryDark = Color(0xFF6EE7B7)  // Dark variant for dark theme

// --- Colores de Error ---
val Error = Color(0xFFDC2626)
val ErrorDark = Color(0xFFF87171)  // Dark variant for dark theme

// --- Fondos ---
val Background = Color(0xFFF5F7FA)   // Fondo suave (light)
val BackgroundDark = Color(0xFF0B1220)  // Fondo oscuro (dark)

val Surface = Color(0xFFFFFFFF)      // Fondo de superficie (light)
val SurfaceDark = Color(0xFF172033)   // Fondo de superficie (dark)

// --- Colores de texto ---
val OnPrimary = Color(0xFFFFFFFF)    // Texto sobre Primary (light)
val OnPrimaryDark = Color(0xFF1A0F3D)  // Texto sobre Primary (dark)

val OnBackground = Color(0xFF0F172A)  // Texto sobre Background (light)
val OnBackgroundDark = Color(0xFFF1F5F9)  // Texto sobre Background (dark)

val OnSurface = Color(0xFF0F172A)     // Texto sobre Surface (light)
val OnSurfaceDark = Color(0xFFF1F5F9)  // Texto sobre Surface (dark)

// --- Colores de soporte M3 ---
val SurfaceVariant = Color(0xFFE8EAF0)   // Fondo de superficie variante (light)
val SurfaceVariantDark = Color(0xFF1E293B)  // Fondo de superficie variante (dark)

val OnSurfaceVariant = Color(0xFF475569)  // Texto sobre SurfaceVariant (light)
val OnSurfaceVariantDark = Color(0xFF94A3B8)  // Texto sobre SurfaceVariant (dark)

val Outline = Color(0xFFCBD5E1)        // Bordes (light)
val OutlineDark = Color(0xFF334155)     // Bordes (dark)

val OutlineVariant = Color(0xFFE2E8F0)  // Bordes variantes (light)
val OutlineVariantDark = Color(0xFF1E293B)  // Bordes variantes (dark)

val InverseSurface = Color(0xFFFFFFFF)   // Superficie inversa (light)
val InverseSurfaceDark = Color(0xFF1A0F3D)  // Superficie inversa (dark)

val Scrim = Color(0xFF000000)          // Scrim (overlay)
val SurfaceTint = Color(0xFF6D4AFF)    // Tinte de superficie (light)
val SurfaceTintDark = Color(0xFF9B8AFF)  // Tinte de superficie (dark)

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
