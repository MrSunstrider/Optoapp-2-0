package com.example.optoapp.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for normalizeAndTranspose — verifies the positive cylinder to
 * negative cylinder transposition formula and the preservation of the '+'
 * sign on positive sphere values.
 *
 * Bug discovered 2026-07-14: "%.2f".format() drops the '+' prefix on
 * positive values. Optometric notation requires explicit signs.
 */
class EvaluacionDiagnosticoHelperTest {

    private val baseState = EvaluacionUiState(fecha = LocalDate.now())

    @Test
    fun `transpose positive cylinder to negative — preserves plus sign on sphere`() {
        // Input: +2.00 +1.00 x 90°
        // Expected output: +3.00 -1.00 x 180°
        val state = baseState.copy(
            recetaOdEsf = "+2.00",
            recetaOdCil = "+1.00",
            recetaOdEje = "90",
        )
        val result = normalizeAndTranspose(state, "OD")
        assertEquals("+3.00", result.recetaOdEsf)
        assertEquals("-1.00", result.recetaOdCil)
        assertEquals("180", result.recetaOdEje)
    }

    @Test
    fun `transpose with negative sphere and positive cylinder — plus sign on result`() {
        // Input: -1.00 +2.00 x 100°
        // newEsf = -1.00 + 2.00 = +1.00 → debe mostrar "+1.00"
        val state = baseState.copy(
            recetaOdEsf = "-1.00",
            recetaOdCil = "+2.00",
            recetaOdEje = "100",
        )
        val result = normalizeAndTranspose(state, "OD")
        assertEquals("+1.00", result.recetaOdEsf)
        assertEquals("-2.00", result.recetaOdCil)
        assertEquals("10", result.recetaOdEje)
    }

    @Test
    fun `transpose result plano when esf plus cil equals zero`() {
        // Input: -1.00 +1.00 x 45°
        // newEsf = -1.00 + 1.00 = 0.00 → "plano"
        val state = baseState.copy(
            recetaOdEsf = "-1.00",
            recetaOdCil = "+1.00",
            recetaOdEje = "45",
        )
        val result = normalizeAndTranspose(state, "OD")
        assertEquals("plano", result.recetaOdEsf)
        assertEquals("-1.00", result.recetaOdCil)
        assertEquals("135", result.recetaOdEje)
    }

    @Test
    fun `transpose with axis overflow wraps correctly`() {
        // Input: +0.50 +0.75 x 100°
        // newEje = 100 + 90 = 190 → 190 - 180 = 10°
        val state = baseState.copy(
            recetaOdEsf = "+0.50",
            recetaOdCil = "+0.75",
            recetaOdEje = "100",
        )
        val result = normalizeAndTranspose(state, "OD")
        assertEquals("+1.25", result.recetaOdEsf)
        assertEquals("-0.75", result.recetaOdCil)
        assertEquals("10", result.recetaOdEje)
    }

    @Test
    fun `negative cylinder does not trigger transposition`() {
        // Input: -2.00 -0.50 x 180°
        // cVal is negative, so no transposition. State unchanged.
        val state = baseState.copy(
            recetaOdEsf = "-2.00",
            recetaOdCil = "-0.50",
            recetaOdEje = "180",
        )
        val result = normalizeAndTranspose(state, "OD")
        assertEquals("-2.00", result.recetaOdEsf)
        assertEquals("-0.50", result.recetaOdCil)
        assertEquals("180", result.recetaOdEje)
    }

    @Test
    fun `transpose handles OI eye`() {
        val state = baseState.copy(
            recetaOiEsf = "+1.50",
            recetaOiCil = "+0.75",
            recetaOiEje = "170",
        )
        val result = normalizeAndTranspose(state, "OI")
        assertEquals("+2.25", result.recetaOiEsf)
        assertEquals("-0.75", result.recetaOiCil)
        assertEquals("80", result.recetaOiEje)
    }

    // --- computeOtrosAuto amblyopia (bidirectional AV → logMAR) ---

    @Test
    fun `computeOtrosAuto mixed Snellen and decimal below threshold clears amblyopia`() {
        val state = baseState.copy(
            avCcOdLejos = "20/20",
            avCcOiLejos = "1.0",
            autoAmbliopia = true,
            otrosAmbliopia = true,
        )
        val result = computeOtrosAuto(state)
        assertEquals(false, result.otrosAmbliopia)
    }

    @Test
    fun `computeOtrosAuto mixed 20 over 20 and decimal 0_5 triggers amblyopia`() {
        val state = baseState.copy(
            avCcOdLejos = "20/20",
            avCcOiLejos = "0.5",
            autoAmbliopia = true,
            otrosAmbliopia = false,
        )
        val result = computeOtrosAuto(state)
        assertEquals(true, result.otrosAmbliopia)
    }

    @Test
    fun `computeOtrosAuto mixed 20 over 40 and decimal 1_0 triggers amblyopia`() {
        val state = baseState.copy(
            avCcOdLejos = "20/40",
            avCcOiLejos = "1.0",
            autoAmbliopia = true,
            otrosAmbliopia = false,
        )
        val result = computeOtrosAuto(state)
        assertEquals(true, result.otrosAmbliopia)
    }

    @Test
    fun `computeOtrosAuto decimal-only with comma triggers amblyopia`() {
        val state = baseState.copy(
            avCcOdLejos = "1.0",
            avCcOiLejos = "0,5",
            autoAmbliopia = true,
            otrosAmbliopia = false,
        )
        val result = computeOtrosAuto(state)
        assertEquals(true, result.otrosAmbliopia)
    }

    @Test
    fun `computeOtrosAuto below threshold pair clears amblyopia`() {
        val state = baseState.copy(
            avCcOdLejos = "20/20",
            avCcOiLejos = "0.8",
            autoAmbliopia = true,
            otrosAmbliopia = true,
        )
        val result = computeOtrosAuto(state)
        assertEquals(false, result.otrosAmbliopia)
    }

    @Test
    fun `computeOtrosAuto inclusive threshold 0_19 triggers amblyopia`() {
        val oiDecimal = Math.pow(10.0, -0.19)
        val oiStr = String.format(java.util.Locale.US, "%.6f", oiDecimal)
        val state = baseState.copy(
            avCcOdLejos = "1.0",
            avCcOiLejos = oiStr,
            autoAmbliopia = true,
            otrosAmbliopia = false,
        )
        val result = computeOtrosAuto(state)
        assertEquals(true, result.otrosAmbliopia)
    }

    @Test
    fun `computeOtrosAuto unparseable OD preserves prior amblyopia true`() {
        val state = baseState.copy(
            avCcOdLejos = "20",
            avCcOiLejos = "20/20",
            autoAmbliopia = true,
            otrosAmbliopia = true,
        )
        val result = computeOtrosAuto(state)
        assertEquals(true, result.otrosAmbliopia)
    }

    @Test
    fun `computeOtrosAuto blank OI preserves prior amblyopia false`() {
        val state = baseState.copy(
            avCcOdLejos = "20/20",
            avCcOiLejos = "",
            autoAmbliopia = true,
            otrosAmbliopia = false,
        )
        val result = computeOtrosAuto(state)
        assertEquals(false, result.otrosAmbliopia)
    }

    @Test
    fun `computeOtrosAuto autoAmbliopia false preserves manual even when delta would fire`() {
        val state = baseState.copy(
            avCcOdLejos = "20/20",
            avCcOiLejos = "0.5",
            autoAmbliopia = false,
            otrosAmbliopia = false,
        )
        val result = computeOtrosAuto(state)
        assertEquals(false, result.otrosAmbliopia)
    }
}
