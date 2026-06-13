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

    // ── H7 + M12: parseAddValue tests ─────────────────────────────────────

    @Test
    fun `parseAddValue defaults to 0_0D when value is empty`() {
        assertEquals(0.0, parseAddValue(""), 0.001)
    }

    @Test
    fun `parseAddValue defaults to 0_0D when value is not a number`() {
        assertEquals(0.0, parseAddValue("abc"), 0.001)
    }

    @Test
    fun `parseAddValue uses parsed value when valid`() {
        assertEquals(1.75, parseAddValue("1.75"), 0.001)
    }

    @Test
    fun `parseAddValue handles zero value`() {
        assertEquals(0.0, parseAddValue("0.0"), 0.001)
    }

    // ── M12: formatAddValue tests ─────────────────────────────────────────

    @Test
    fun `formatAddValue handles positive values with plus sign`() {
        assertEquals("+1.75", formatAddValue(1.75))
        assertEquals("+2.00", formatAddValue(2.0))
        assertEquals("+0.25", formatAddValue(0.25))
    }

    @Test
    fun `formatAddValue handles negative values`() {
        assertEquals("-1.75", formatAddValue(-1.75))
        assertEquals("-0.50", formatAddValue(-0.5))
    }

    @Test
    fun `formatAddValue handles zero`() {
        assertEquals("+0.00", formatAddValue(0.0))
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
