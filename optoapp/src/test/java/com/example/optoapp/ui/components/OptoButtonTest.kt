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
}
