package com.example.optoapp.ui.theme

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OptoDensityTest {

    @Test
    fun comfortable_screenPadding_is16dp() {
        assertEquals(16.dp, OptoDensity.Comfortable.screenPadding)
    }

    @Test
    fun compact_screenPadding_is12dp() {
        assertEquals(12.dp, OptoDensity.Compact.screenPadding)
    }

    @Test
    fun compact_isSmallerThanComfortable_forAllTokens() {
        val comfortable = OptoDensity.Comfortable
        val compact = OptoDensity.Compact

        assertTrue(compact.screenPadding < comfortable.screenPadding)
        assertTrue(compact.cardPadding < comfortable.cardPadding)
        assertTrue(compact.listItemPadding < comfortable.listItemPadding)
        assertTrue(compact.sectionGap < comfortable.sectionGap)
        assertTrue(compact.blockGap < comfortable.blockGap)
        assertTrue(compact.tightGap < comfortable.tightGap)
        assertTrue(compact.wizardHeaderPadding < comfortable.wizardHeaderPadding)
        assertTrue(compact.emptyStatePadding < comfortable.emptyStatePadding)
    }

    @Test
    fun compact_preset_values_matchSpecification() {
        val compact = OptoDensity.Compact
        assertEquals(12.dp, compact.screenPadding)
        assertEquals(12.dp, compact.cardPadding)
        assertEquals(10.dp, compact.listItemPadding)
        assertEquals(8.dp, compact.sectionGap)
        assertEquals(6.dp, compact.blockGap)
        assertEquals(2.dp, compact.tightGap)
        assertEquals(8.dp, compact.wizardHeaderPadding)
        assertEquals(20.dp, compact.emptyStatePadding)
        assertTrue(compact.isDense)
        assertFalse(OptoDensity.Comfortable.isDense)
    }

    @Test
    fun comfortable_preset_values_matchSpecification() {
        val comfortable = OptoDensity.Comfortable
        assertEquals(16.dp, comfortable.screenPadding)
        assertEquals(16.dp, comfortable.cardPadding)
        assertEquals(14.dp, comfortable.listItemPadding)
        assertEquals(12.dp, comfortable.sectionGap)
        assertEquals(8.dp, comfortable.blockGap)
        assertEquals(4.dp, comfortable.tightGap)
        assertEquals(12.dp, comfortable.wizardHeaderPadding)
        assertEquals(32.dp, comfortable.emptyStatePadding)
        assertFalse(comfortable.isDense)
    }

    @Test
    fun resolveOptoDensity_nullWindowSizeClass_returnsComfortable() {
        assertEquals(OptoDensity.Comfortable, resolveOptoDensity(null))
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun resolveOptoDensity_compactWidth_returnsCompact() {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(360.dp, 640.dp))
        assertEquals(OptoDensity.Compact, resolveOptoDensity(windowSizeClass))
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun resolveOptoDensity_mediumWidth_returnsCompact() {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(700.dp, 360.dp))
        assertEquals(OptoDensity.Compact, resolveOptoDensity(windowSizeClass))
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun resolveOptoDensity_expandedWidth_returnsComfortable() {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(1200.dp, 800.dp))
        assertEquals(OptoDensity.Comfortable, resolveOptoDensity(windowSizeClass))
    }
}
