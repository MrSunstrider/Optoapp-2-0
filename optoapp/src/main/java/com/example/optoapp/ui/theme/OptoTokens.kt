package com.example.optoapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens for OptoApp's Material 3 theme.
 * 
 * This file contains the single source of truth for:
 * - Color constants (light/dark variants)
 * - Shape definitions (small, medium, large)
 * - Spacing scale (xs, sm, md, lg, xl, xxl)
 * - Elevation levels (level0, level1, level2, level3)
 */

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

    // --- Elevation tokens ---
    object elevation {
        val level0: Dp = 0.dp
        val level1: Dp = 2.dp
        val level2: Dp = 4.dp
        val level3: Dp = 8.dp
    }

    // --- Color tokens (light theme) ---
    object colors {
        // Primary palette
        val primary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF6D4AFF)  // #6D4AFF
        val onPrimary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFFFFFFF)  // #FFFFFF
        val primaryContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFEDE8FF)  // #EDE8FF
        
        // Secondary palette
        val secondary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF3DD9A5)  // #3DD9A5
        
        // Background and surface
        val background: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFF5F7FA)  // #F5F7FA
        val surface: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFFFFFFF)  // #FFFFFF
        
        // Additional M3 slots
        val surfaceVariant: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFE8EAF0)  // #E8EAF0
        val onSurfaceVariant: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF475569)  // #475569
        val outline: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFCBD5E1)  // #CBD5E1
        val outlineVariant: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFE2E8F0)  // #E2E8F0
        val error: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFDC2626)  // #DC2626
        val inverseSurface: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF1A0F3D)  // #1A0F3D
        val scrim: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF000000)  // #000000
        val surfaceTint: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF6D4AFF)  // #6D4AFF
    }

    // --- Color tokens (dark theme) ---
    object colorsDark {
        // Primary palette (dark)
        val primary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF9B8AFF)  // #9B8AFF
        val onPrimary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF1A0F3D)  // #1A0F3D
        val primaryContainer: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF2D1F6E)  // #2D1F6E
        
        // Secondary palette (dark)
        val secondary: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF6EE7B7)  // #6EE7B7
        
        // Background and surface (dark)
        val background: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF0B1220)  // #0B1220
        val surface: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF172033)  // #172033
        
        // Additional M3 slots (dark)
        val surfaceVariant: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF1E293B)  // #1E293B
        val onSurfaceVariant: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF94A3B8)  // #94A3B8
        val outline: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF334155)  // #334155
        val outlineVariant: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF1E293B)  // #1E293B
        val error: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFF87171)  // #F87171
        val inverseSurface: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFFFFFFF)  // #FFFFFF
        val scrim: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF000000)  // #000000
        val surfaceTint: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF9B8AFF)  // #9B8AFF
    }

    // --- Helper function to get shapes for MaterialTheme ---
    fun getShapes(): Shapes = Shapes(
        small = shapes.small,
        medium = shapes.medium,
        large = shapes.large
    )
}