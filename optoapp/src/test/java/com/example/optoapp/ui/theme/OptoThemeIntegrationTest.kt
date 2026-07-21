package com.example.optoapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for OptoAppTheme.
 *
 * Verifies that OptoTokens values are consistent and that the Color.kt
 * palette matches the token definitions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OptoThemeIntegrationTest {

    @Test
    fun color_primary_matchesOptoTokens() {
        assertEquals(OptoTokens.colors.primary, Primary)
        assertEquals(OptoTokens.colorsDark.primary, PrimaryDark)
    }

    @Test
    fun color_secondary_matchesOptoTokens() {
        assertEquals(OptoTokens.colors.secondary, Secondary)
        assertEquals(OptoTokens.colorsDark.secondary, SecondaryDark)
    }

    @Test
    fun color_background_matchesOptoTokens() {
        assertEquals(OptoTokens.colors.background, Background)
        assertEquals(OptoTokens.colorsDark.background, BackgroundDark)
    }

    @Test
    fun color_surface_matchesOptoTokens() {
        assertEquals(OptoTokens.colors.surface, Surface)
        assertEquals(OptoTokens.colorsDark.surface, SurfaceDark)
    }

    @Test
    fun color_surfaceVariant_matchesOptoTokens() {
        assertEquals(OptoTokens.colors.surfaceVariant, SurfaceVariant)
        assertEquals(OptoTokens.colorsDark.surfaceVariant, SurfaceVariantDark)
    }

    @Test
    fun color_outline_matchesOptoTokens() {
        assertEquals(OptoTokens.colors.outline, Outline)
        assertEquals(OptoTokens.colorsDark.outline, OutlineDark)
    }

    @Test
    fun color_error_matchesOptoTokens() {
        assertEquals(OptoTokens.colors.error, Error)
        assertEquals(OptoTokens.colorsDark.error, ErrorDark)
    }

    @Test
    fun shapes_small_is12dp() {
        assertEquals(RoundedCornerShape(12.dp), OptoTokens.shapes.small)
    }

    @Test
    fun shapes_medium_is16dp() {
        assertEquals(RoundedCornerShape(16.dp), OptoTokens.shapes.medium)
    }

    @Test
    fun shapes_large_is24dp() {
        assertEquals(RoundedCornerShape(24.dp), OptoTokens.shapes.large)
    }

    @Test
    fun getShapes_returnsCorrectValues() {
        val shapes = OptoTokens.getShapes()
        assertEquals(OptoTokens.shapes.small, shapes.small)
        assertEquals(OptoTokens.shapes.medium, shapes.medium)
        assertEquals(OptoTokens.shapes.large, shapes.large)
    }

    @Test
    fun theme_m3Slots_defined() {
        // Verify all required M3 color scheme slots have non-null values
        // This tests the Color.kt constants directly without compose
        assertNotNull(Primary)
        assertNotNull(OnPrimary)
        assertNotNull(Secondary)
        assertNotNull(Background)
        assertNotNull(Surface)
        assertNotNull(SurfaceVariant)
        assertNotNull(OnSurfaceVariant)
        assertNotNull(Outline)
        assertNotNull(OutlineVariant)
        assertNotNull(Error)
        assertNotNull(InverseSurface)
        assertNotNull(Scrim)
        assertNotNull(SurfaceTint)
    }
}
