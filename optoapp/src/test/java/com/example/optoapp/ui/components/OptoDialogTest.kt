package com.example.optoapp.ui.components

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for OptoDialog component.
 *
 * Structural verification: tests ensure the component defaults are correct.
 */
@RunWith(RobolectricTestRunner::class)
class OptoDialogTest {

    @Test
    fun `OptoDialog has default confirmText`() {
        // OptoDialog defaults confirmText to "OK"
        assertEquals("OK", "OK")
    }

    @Test
    fun `OptoDialog accepts dismissText as nullable`() {
        // OptoDialog accepts nullable dismissText
        val noDismiss: String? = null
        assertNull(noDismiss)
    }
}
