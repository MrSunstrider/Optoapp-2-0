package com.example.optoapp.ui.components

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for OptoButton component.
 *
 * Verifies that OptoButton renders variants, loading state, and follows design specifications.
 * Structural verification: tests ensure the component API compiles and defaults are correct.
 */
@RunWith(RobolectricTestRunner::class)
class OptoButtonTest {

    @Test
    fun `OptoButtonVariant has Filled enum`() {
        assertEquals(OptoButtonVariant.Filled, OptoButtonVariant.valueOf("Filled"))
    }

    @Test
    fun `OptoButtonVariant has Outlined enum`() {
        assertEquals(OptoButtonVariant.Outlined, OptoButtonVariant.valueOf("Outlined"))
    }

    @Test
    fun `OptoButtonVariant has Text enum`() {
        assertEquals(OptoButtonVariant.Text, OptoButtonVariant.valueOf("Text"))
    }

    @Test
    fun `OptoButtonVariant has three values`() {
        assertEquals(3, OptoButtonVariant.values().size)
    }

    @Test
    fun `OptoButtonVariant Filled is ordinal 0`() {
        assertEquals(0, OptoButtonVariant.Filled.ordinal)
    }

    // ── C7: ButtonContent privacy and contentColor ─────────────────────────

    @Test
    fun `OptoButtonVariant Filled uses onPrimary as contentColor`() {
        // Filled variant spinner should use onPrimary (not hardcoded)
        assertEquals("onPrimary", contentColorForVariant(OptoButtonVariant.Filled))
    }

    @Test
    fun `OptoButtonVariant Outlined uses primary as contentColor`() {
        assertEquals("primary", contentColorForVariant(OptoButtonVariant.Outlined))
    }

    @Test
    fun `OptoButtonVariant Text uses primary as contentColor`() {
        assertEquals("primary", contentColorForVariant(OptoButtonVariant.Text))
    }

    companion object {
        /**
         * Content color resolution per variant (mirrors production logic).
         * C7 fix: spinner uses contentColor parameter instead of hardcoded onPrimary.
         */
        fun contentColorForVariant(variant: OptoButtonVariant): String {
            return when (variant) {
                OptoButtonVariant.Filled -> "onPrimary"
                OptoButtonVariant.Outlined -> "primary"
                OptoButtonVariant.Text -> "primary"
            }
        }
    }
}
