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
            recetaOdEje = "90"
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
            recetaOdEje = "100"
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
            recetaOdEje = "45"
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
            recetaOdEje = "100"
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
            recetaOdEje = "180"
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
            recetaOiEje = "170"
        )
        val result = normalizeAndTranspose(state, "OI")
        assertEquals("+2.25", result.recetaOiEsf)
        assertEquals("-0.75", result.recetaOiCil)
        assertEquals("80", result.recetaOiEje)
    }
}
