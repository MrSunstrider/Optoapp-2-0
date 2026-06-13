package com.example.optoapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for OptoTokens.kt design tokens.
 * 
 * Verifies that all design tokens match the specification values
 * and that WCAG AA contrast ratios are met.
 */
class OptoTokensTest {

    // --- Spacing token tests ---
    
    @Test
    fun spacing_xs_is4dp() {
        assertEquals(4.dp, OptoTokens.spacing.xs)
    }
    
    @Test
    fun spacing_sm_is8dp() {
        assertEquals(8.dp, OptoTokens.spacing.sm)
    }
    
    @Test
    fun spacing_md_is12dp() {
        assertEquals(12.dp, OptoTokens.spacing.md)
    }
    
    @Test
    fun spacing_lg_is16dp() {
        assertEquals(16.dp, OptoTokens.spacing.lg)
    }
    
    @Test
    fun spacing_xl_is24dp() {
        assertEquals(24.dp, OptoTokens.spacing.xl)
    }
    
    @Test
    fun spacing_xxl_is32dp() {
        assertEquals(32.dp, OptoTokens.spacing.xxl)
    }

    // --- Shape token tests ---
    
    @Test
    fun shape_small_is12dp() {
        assertEquals(RoundedCornerShape(12.dp), OptoTokens.shapes.small)
    }
    
    @Test
    fun shape_medium_is16dp() {
        assertEquals(RoundedCornerShape(16.dp), OptoTokens.shapes.medium)
    }
    
    @Test
    fun shape_large_is24dp() {
        assertEquals(RoundedCornerShape(24.dp), OptoTokens.shapes.large)
    }

    // --- Elevation token tests ---
    
    @Test
    fun elevation_level0_is0dp() {
        assertEquals(0.dp, OptoTokens.elevation.level0)
    }
    
    @Test
    fun elevation_level1_is2dp() {
        assertEquals(2.dp, OptoTokens.elevation.level1)
    }
    
    @Test
    fun elevation_level2_is4dp() {
        assertEquals(4.dp, OptoTokens.elevation.level2)
    }
    
    @Test
    fun elevation_level3_is8dp() {
        assertEquals(8.dp, OptoTokens.elevation.level3)
    }

    // --- Light theme color token tests ---
    
    @Test
    fun primary_light_is6D4AFF() {
        assertEquals(Color(0xFF6D4AFF), OptoTokens.colors.primary)
    }
    
    @Test
    fun onPrimary_light_isFFFFFF() {
        assertEquals(Color(0xFFFFFFFF), OptoTokens.colors.onPrimary)
    }
    
    @Test
    fun primaryContainer_light_isEDE8FF() {
        assertEquals(Color(0xFFEDE8FF), OptoTokens.colors.primaryContainer)
    }
    
    @Test
    fun secondary_light_is3DD9A5() {
        assertEquals(Color(0xFF3DD9A5), OptoTokens.colors.secondary)
    }
    
    @Test
    fun background_light_isF5F7FA() {
        assertEquals(Color(0xFFF5F7FA), OptoTokens.colors.background)
    }
    
    @Test
    fun surface_light_isFFFFFF() {
        assertEquals(Color(0xFFFFFFFF), OptoTokens.colors.surface)
    }
    
    @Test
    fun surfaceVariant_light_isE8EAF0() {
        assertEquals(Color(0xFFE8EAF0), OptoTokens.colors.surfaceVariant)
    }
    
    @Test
    fun onSurfaceVariant_light_is475569() {
        assertEquals(Color(0xFF475569), OptoTokens.colors.onSurfaceVariant)
    }
    
    @Test
    fun outline_light_isCBD5E1() {
        assertEquals(Color(0xFFCBD5E1), OptoTokens.colors.outline)
    }
    
    @Test
    fun outlineVariant_light_isE2E8F0() {
        assertEquals(Color(0xFFE2E8F0), OptoTokens.colors.outlineVariant)
    }
    
    @Test
    fun error_light_isDC2626() {
        assertEquals(Color(0xFFDC2626), OptoTokens.colors.error)
    }
    
    @Test
    fun inverseSurface_light_is1A0F3D() {
        assertEquals(Color(0xFF1A0F3D), OptoTokens.colors.inverseSurface)
    }
    
    @Test
    fun scrim_light_is000000() {
        assertEquals(Color(0xFF000000), OptoTokens.colors.scrim)
    }
    
    @Test
    fun surfaceTint_light_is6D4AFF() {
        assertEquals(Color(0xFF6D4AFF), OptoTokens.colors.surfaceTint)
    }

    // --- Dark theme color token tests ---
    
    @Test
    fun primary_dark_is9B8AFF() {
        assertEquals(Color(0xFF9B8AFF), OptoTokens.colorsDark.primary)
    }
    
    @Test
    fun onPrimary_dark_is1A0F3D() {
        assertEquals(Color(0xFF1A0F3D), OptoTokens.colorsDark.onPrimary)
    }
    
    @Test
    fun primaryContainer_dark_is2D1F6E() {
        assertEquals(Color(0xFF2D1F6E), OptoTokens.colorsDark.primaryContainer)
    }
    
    @Test
    fun secondary_dark_is6EE7B7() {
        assertEquals(Color(0xFF6EE7B7), OptoTokens.colorsDark.secondary)
    }
    
    @Test
    fun background_dark_is0B1220() {
        assertEquals(Color(0xFF0B1220), OptoTokens.colorsDark.background)
    }
    
    @Test
    fun surface_dark_is172033() {
        assertEquals(Color(0xFF172033), OptoTokens.colorsDark.surface)
    }
    
    @Test
    fun surfaceVariant_dark_is1E293B() {
        assertEquals(Color(0xFF1E293B), OptoTokens.colorsDark.surfaceVariant)
    }
    
    @Test
    fun onSurfaceVariant_dark_is94A3B8() {
        assertEquals(Color(0xFF94A3B8), OptoTokens.colorsDark.onSurfaceVariant)
    }
    
    @Test
    fun outline_dark_is334155() {
        assertEquals(Color(0xFF334155), OptoTokens.colorsDark.outline)
    }
    
    @Test
    fun outlineVariant_dark_is1E293B() {
        assertEquals(Color(0xFF1E293B), OptoTokens.colorsDark.outlineVariant)
    }
    
    @Test
    fun error_dark_isF87171() {
        assertEquals(Color(0xFFF87171), OptoTokens.colorsDark.error)
    }
    
    @Test
    fun inverseSurface_dark_isFFFFFF() {
        assertEquals(Color(0xFFFFFFFF), OptoTokens.colorsDark.inverseSurface)
    }
    
    @Test
    fun scrim_dark_is000000() {
        assertEquals(Color(0xFF000000), OptoTokens.colorsDark.scrim)
    }
    
    @Test
    fun surfaceTint_dark_is9B8AFF() {
        assertEquals(Color(0xFF9B8AFF), OptoTokens.colorsDark.surfaceTint)
    }

    // --- WCAG AA Contrast Ratio Tests ---
    
    private fun relativeLuminance(c: Color): Double {
        fun linearize(channel: Float): Double {
            val s = channel.toDouble()
            return if (s <= 0.04045) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * linearize(c.red) + 0.7152 * linearize(c.green) + 0.0722 * linearize(c.blue)
    }
    
    private fun contrastRatio(c1: Color, c2: Color): Double {
        val l1 = relativeLuminance(c1)
        val l2 = relativeLuminance(c2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }
    
    @Test
    fun primary_onPrimary_contrast_meetsAA() {
        val ratio = contrastRatio(OptoTokens.colors.primary, OptoTokens.colors.onPrimary)
        assertTrue("Primary/onPrimary contrast $ratio is below 4.5:1 (AA)", ratio >= 4.5)
    }
    
    @Test
    fun primary_onPrimary_dark_contrast_meetsAA() {
        val ratio = contrastRatio(OptoTokens.colorsDark.primary, OptoTokens.colorsDark.onPrimary)
        assertTrue("Dark primary/onPrimary contrast $ratio is below 4.5:1 (AA)", ratio >= 4.5)
    }
    
    @Test
    fun background_onBackground_contrast_meetsAA() {
        val ratio = contrastRatio(OptoTokens.colors.background, Color(0xFF0F172A)) // OnBackground light
        assertTrue("Background/onBackground contrast $ratio is below 4.5:1 (AA)", ratio >= 4.5)
    }
    
    @Test
    fun background_onBackground_dark_contrast_meetsAA() {
        val ratio = contrastRatio(OptoTokens.colorsDark.background, Color(0xFFF1F5F9)) // OnBackground dark
        assertTrue("Dark background/onBackground contrast $ratio is below 4.5:1 (AA)", ratio >= 4.5)
    }
    
    @Test
    fun surface_onSurface_dark_contrast_meetsAALarge() {
        val ratio = contrastRatio(OptoTokens.colorsDark.surface, OptoTokens.colorsDark.onSurfaceVariant)
        assertTrue("Dark surface/onSurfaceVariant contrast $ratio is below 3:1 (AA large)", ratio >= 3.0)
    }
}