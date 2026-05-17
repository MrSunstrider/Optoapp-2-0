package com.example.optoapp.ui.theme

import androidx.compose.ui.graphics.Color

// --- Modo Claro (mayor contraste, esmeralda como web) ---
val Primary = Color(0xFF059669)           // Verde Esmeralda
val PrimaryDark_s = Color(0xFF047857)     // Verde más oscuro (secondary)
val AccentGreen = Color(0xFF10B981)       // Verde brillante
val ErrorRed = Color(0xFFDC2626)          // Rojo
val WarningGold = Color(0xFFD97706)       // Ámbar

// Fondos claro
val BackgroundLight = Color(0xFFF5F7FA)   // Fondo suave
val SurfaceLight = Color(0xFFFFFFFF)      // Blanco
val SurfaceLightMuted = Color(0xFFF0F2F5) // Card bg
val BorderLight = Color(0xFFD0D5DD)       // Bordes visibles

// Textos claro
val TextPrimary = Color(0xFF0F172A)       // Casi negro
val TextSecondary = Color(0xFF475569)      // Gris oscuro

// --- Modo Oscuro (Deep Navy como web) ---
val PrimaryDark = Color(0xFF34D399)        // Verde esmeralda brillante
val PrimaryDarkVariant = Color(0xFF6EE7B7)
val AccentGreenDark = Color(0xFF6EE7B7)
val ErrorRedDark = Color(0xFFF87171)
val WarningGoldDark = Color(0xFFFBBF24)

// Fondos oscuros
val BackgroundDark = Color(0xFF080C14)     // Deep Navy
val SurfaceDark = Color(0xFF111827)        // Card
val SurfaceDarkMuted = Color(0xFF1E293B)   // Gris azulado

// Textos oscuros
val TextPrimaryDark = Color(0xFFF1F5F9)
val TextSecondaryDark = Color(0xFF94A3B8)

// --- Alias de compatibilidad ---
val PrimaryBlue = Primary
val PrimaryBlueDark = PrimaryDark
val BackgroundWhite = BackgroundLight
val TextDark = TextPrimary
val TextLight = TextPrimaryDark
val TextSecondaryLight = TextSecondaryDark
val SurfaceGray = SurfaceLightMuted
val SurfaceGrayDark = SurfaceDarkMuted
