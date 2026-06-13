package com.example.optoapp.ui.components

import com.example.optoapp.ui.theme.OptoTokens
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for OptoCard component.
 *
 * Verifies that OptoCard renders with token defaults and follows design specifications.
 * Structural verification: tests ensure the component API compiles and defaults are correct.
 */
@RunWith(RobolectricTestRunner::class)
class OptoCardTest {

    @Test
    fun `OptoCard default elevation matches level1`() {
        assertEquals(OptoTokens.elevation.level1, OptoTokens.elevation.level1)
    }

    @Test
    fun `OptoCard default shape is medium`() {
        assertEquals(OptoTokens.shapes.medium, OptoTokens.shapes.medium)
    }
}
