package com.example.optoapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object OptoTokens {
    // --- Spacing tokens ---
    object spacing {
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 24.dp
        val xxl: Dp = 32.dp
    }

    // --- Shape tokens ---
    object shapes {
        val small: RoundedCornerShape = RoundedCornerShape(12.dp)
        val medium: RoundedCornerShape = RoundedCornerShape(16.dp)
        val large: RoundedCornerShape = RoundedCornerShape(24.dp)
    }

    // --- Semantic colors for Analisis de Negocio ---
    object analisis {
        val positiveGreen: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF27AE60)
        val alertRed: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFE74C3C)
        val textDark: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF2C3E50)
        val warningAmber: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFF39C12)
    }

    // --- Elevation tokens ---
    object elevation {
        val level0: Dp = 0.dp
        val level1: Dp = 2.dp
        val level2: Dp = 4.dp
        val level3: Dp = 8.dp
    }

    // --- Color tokens (light theme) ---
    object colors {
        // Primary — deep teal (premium medical)
        val primary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF006D6F)
        val onPrimary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        val primaryContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFA0F0F0)
        val onPrimaryContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF002020)
        val secondary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF4A6A70)
        val onSecondary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        val secondaryContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFCDE8E8)
        val onSecondaryContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF042024)
        val tertiary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFB35B5B)
        val onTertiary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        val tertiaryContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFFFD9DA)
        val onTertiaryContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF401114)
        val error: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFBA1A1A)
        val onError: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        val errorContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFFFDAD6)
        val onErrorContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF410002)
        val background: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFF8FCFB)
        val onBackground: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF191C1C)
        val surface: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFF8FCFB)
        val onSurface: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF191C1C)
        val surfaceVariant: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFDAE5E4)
        val onSurfaceVariant: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF3F4948)
        val outline: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF6F7978)
        val outlineVariant: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFBEC9C8)
        val scrim: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF000000)
        val surfaceTint: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF006D6F)
        val inverseSurface: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF2D3131)
        val inverseOnSurface: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFEFF1F1)
        val inversePrimary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF4EDBDB)
    }

    // --- Color tokens (dark theme) ---
    object colorsDark {
        val primary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF4EDBDB)
        val onPrimary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF003738)
        val primaryContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF005354)
        val onPrimaryContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFA0F0F0)
        val secondary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFB5CBD1)
        val onSecondary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF1F3538)
        val secondaryContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF364B4F)
        val onSecondaryContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFCDE8E8)
        val tertiary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFFFB3B0)
        val onTertiary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF5E1418)
        val tertiaryContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF7F2C2E)
        val onTertiaryContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFFFD9DA)
        val error: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFF2B8B5)
        val onError: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF601410)
        val errorContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF8C1D18)
        val onErrorContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFFFDAD6)
        val background: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF0E1515)
        val onBackground: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFE0E3E3)
        val surface: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF161D1D)
        val onSurface: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFE0E3E3)
        val surfaceVariant: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF3F4948)
        val onSurfaceVariant: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFBEC9C8)
        val outline: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF889392)
        val outlineVariant: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF3F4948)
        val scrim: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF000000)
        val surfaceTint: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF4EDBDB)
        val inverseSurface: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFE0E3E3)
        val inverseOnSurface: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF2D3131)
        val inversePrimary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF006D6F)
    }

    fun getShapes(): Shapes = Shapes(
        small = shapes.small,
        medium = shapes.medium,
        large = shapes.large
    )
}
