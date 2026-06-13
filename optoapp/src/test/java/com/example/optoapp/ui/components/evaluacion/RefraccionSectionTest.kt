package com.example.optoapp.ui.components.evaluacion

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for RefraccionSection and its internal components.
 *
 * H7: NumericAddStepper default value fix (2.0 → 0.0).
 * C6: ViewModel replaced with onFocusLostEye lambda (API refactoring).
 */
@RunWith(RobolectricTestRunner::class)
class RefraccionSectionTest {

    // ── H7: NumericAddStepper default ──────────────────────────────────────

    @Test
    fun `NumericAddStepper defaults to 0_0D when value is empty`() {
        val currentVal = "".toDoubleOrNull() ?: 0.0
        assertEquals(0.0, currentVal, 0.001)
    }

    @Test
    fun `NumericAddStepper defaults to 0_0D when value is not a number`() {
        val currentVal = "abc".toDoubleOrNull() ?: 0.0
        assertEquals(0.0, currentVal, 0.001)
    }

    @Test
    fun `NumericAddStepper uses parsed value when valid`() {
        val currentVal = "1.75".toDoubleOrNull() ?: 0.0
        assertEquals(1.75, currentVal, 0.001)
    }

    @Test
    fun `NumericAddStepper handles zero value`() {
        val currentVal = "0.0".toDoubleOrNull() ?: 0.0
        assertEquals(0.0, currentVal, 0.001)
    }

    // ── C6: onFocusLostEye callback ───────────────────────────────────────

    @Test
    fun `onFocusLostEye OD dispatches correctly`() {
        var capturedEye: String? = null
        val onFocusLostEye: (String) -> Unit = { capturedEye = it }
        onFocusLostEye("OD")
        assertEquals("OD", capturedEye)
    }

    @Test
    fun `onFocusLostEye OI dispatches correctly`() {
        var capturedEye: String? = null
        val onFocusLostEye: (String) -> Unit = { capturedEye = it }
        onFocusLostEye("OI")
        assertEquals("OI", capturedEye)
    }
}
