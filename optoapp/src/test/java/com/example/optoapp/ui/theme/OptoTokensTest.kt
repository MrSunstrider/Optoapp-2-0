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
    fun primary_light_is006D6F() {
        assertEquals(Color(0xFF006D6F), OptoTokens.colors.primary)
    }
    
    @Test
    fun onPrimary_light_isFFFFFF() {
        assertEquals(Color(0xFFFFFFFF), OptoTokens.colors.onPrimary)
    }
    
    @Test
    fun primaryContainer_light_isA0F0F0() {
        assertEquals(Color(0xFFA0F0F0), OptoTokens.colors.primaryContainer)
    }
    
    @Test
    fun secondary_light_is4A6A70() {
        assertEquals(Color(0xFF4A6A70), OptoTokens.colors.secondary)
    }
    
    @Test
    fun background_light_isF8FCFB() {
        assertEquals(Color(0xFFF8FCFB), OptoTokens.colors.background)
    }
    
    @Test
    fun surface_light_isF8FCFB() {
        assertEquals(Color(0xFFF8FCFB), OptoTokens.colors.surface)
    }
    
    @Test
    fun surfaceVariant_light_isDAE5E4() {
        assertEquals(Color(0xFFDAE5E4), OptoTokens.colors.surfaceVariant)
    }
    
    @Test
    fun onSurfaceVariant_light_is3F4948() {
        assertEquals(Color(0xFF3F4948), OptoTokens.colors.onSurfaceVariant)
    }
    
    @Test
    fun outline_light_is6F7978() {
        assertEquals(Color(0xFF6F7978), OptoTokens.colors.outline)
    }
    
    @Test
    fun outlineVariant_light_isBEC9C8() {
        assertEquals(Color(0xFFBEC9C8), OptoTokens.colors.outlineVariant)
    }
    
    @Test
    fun error_light_isBA1A1A() {
        assertEquals(Color(0xFFBA1A1A), OptoTokens.colors.error)
    }
    
    @Test
    fun inverseSurface_light_is2D3131() {
        assertEquals(Color(0xFF2D3131), OptoTokens.colors.inverseSurface)
    }
    
    @Test
    fun scrim_light_is000000() {
        assertEquals(Color(0xFF000000), OptoTokens.colors.scrim)
    }
    
    @Test
    fun surfaceTint_light_is006D6F() {
        assertEquals(Color(0xFF006D6F), OptoTokens.colors.surfaceTint)
    }

    // --- Dark theme color token tests ---
    
    @Test
    fun primary_dark_is4EDBDB() {
        assertEquals(Color(0xFF4EDBDB), OptoTokens.colorsDark.primary)
    }
    
    @Test
    fun onPrimary_dark_is003738() {
        assertEquals(Color(0xFF003738), OptoTokens.colorsDark.onPrimary)
    }
    
    @Test
    fun primaryContainer_dark_is005354() {
        assertEquals(Color(0xFF005354), OptoTokens.colorsDark.primaryContainer)
    }
    
    @Test
    fun secondary_dark_isB5CBD1() {
        assertEquals(Color(0xFFB5CBD1), OptoTokens.colorsDark.secondary)
    }
    
    @Test
    fun background_dark_is0E1515() {
        assertEquals(Color(0xFF0E1515), OptoTokens.colorsDark.background)
    }
    
    @Test
    fun surface_dark_is161D1D() {
        assertEquals(Color(0xFF161D1D), OptoTokens.colorsDark.surface)
    }
    
    @Test
    fun surfaceVariant_dark_is3F4948() {
        assertEquals(Color(0xFF3F4948), OptoTokens.colorsDark.surfaceVariant)
    }
    
    @Test
    fun onSurfaceVariant_dark_isBEC9C8() {
        assertEquals(Color(0xFFBEC9C8), OptoTokens.colorsDark.onSurfaceVariant)
    }
    
    @Test
    fun outline_dark_is889392() {
        assertEquals(Color(0xFF889392), OptoTokens.colorsDark.outline)
    }
    
    @Test
    fun outlineVariant_dark_is3F4948() {
        assertEquals(Color(0xFF3F4948), OptoTokens.colorsDark.outlineVariant)
    }
    
    @Test
    fun error_dark_isF2B8B5() {
        assertEquals(Color(0xFFF2B8B5), OptoTokens.colorsDark.error)
    }
    
    @Test
    fun inverseSurface_dark_isE0E3E3() {
        assertEquals(Color(0xFFE0E3E3), OptoTokens.colorsDark.inverseSurface)
    }
    
    @Test
    fun scrim_dark_is000000() {
        assertEquals(Color(0xFF000000), OptoTokens.colorsDark.scrim)
    }
    
    @Test
    fun surfaceTint_dark_is4EDBDB() {
        assertEquals(Color(0xFF4EDBDB), OptoTokens.colorsDark.surfaceTint)
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
        val ratio = contrastRatio(OptoTokens.colors.background, OptoTokens.colors.onBackground)
        assertTrue("Background/onBackground contrast $ratio is below 4.5:1 (AA)", ratio >= 4.5)
    }
    
    @Test
    fun background_onBackground_dark_contrast_meetsAA() {
        val ratio = contrastRatio(OptoTokens.colorsDark.background, OptoTokens.colorsDark.onBackground)
        assertTrue("Dark background/onBackground contrast $ratio is below 4.5:1 (AA)", ratio >= 4.5)
    }
    
    @Test
    fun surface_onSurface_dark_contrast_meetsAALarge() {
        val ratio = contrastRatio(OptoTokens.colorsDark.surface, OptoTokens.colorsDark.onSurfaceVariant)
        assertTrue("Dark surface/onSurfaceVariant contrast $ratio is below 3:1 (AA large)", ratio >= 3.0)
    }
}