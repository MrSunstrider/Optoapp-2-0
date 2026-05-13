package com.example.optoapp.viewmodel

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class EvaluacionDiagnosticoHelperTest {

    // ─── parseRefraction ─────────────────────────────────────────────

    @Test
    fun parseRefraction_plano_returnsZero() {
        val result = parseRefraction("plano")
        assertNotNull(result)
        assertEquals(0.0, result!!, 0.001)
    }

    @Test
    fun parseRefraction_PL_returnsZero() {
        val result = parseRefraction("PL")
        assertNotNull(result)
        assertEquals(0.0, result!!, 0.001)
    }

    @Test
    fun parseRefraction_neutro_returnsZero() {
        val result = parseRefraction("neutro")
        assertNotNull(result)
        assertEquals(0.0, result!!, 0.001)
    }

    @Test
    fun parseRefraction_negativeValue_returnsCorrect() {
        val result = parseRefraction("-1.50")
        assertNotNull(result)
        assertEquals(-1.5, result!!, 0.001)
    }

    @Test
    fun parseRefraction_positiveValue_returnsCorrect() {
        val result = parseRefraction("+2.00")
        assertNotNull(result)
        assertEquals(2.0, result!!, 0.001)
    }

    @Test
    fun parseRefraction_invalidString_returnsNull() {
        assertNull(parseRefraction("abc"))
    }

    @Test
    fun parseRefraction_commaDecimal_returnsCorrect() {
        val result = parseRefraction("-0,75")
        assertNotNull(result)
        assertEquals(-0.75, result!!, 0.001)
    }

    @Test
    fun parseRefraction_emptyString_returnsNull() {
        assertNull(parseRefraction(""))
    }

    @Test
    fun parseRefraction_ntAbbreviation_returnsZero() {
        val result = parseRefraction("nt")
        assertNotNull(result)
        assertEquals(0.0, result!!, 0.001)
    }

    // ─── calcularDiagnostico ─────────────────────────────────────────

    @Test
    fun calcularDiagnostico_bothNull_returnsEmpty() {
        assertEquals("", calcularDiagnostico("", ""))
    }

    @Test
    fun calcularDiagnostico_miopiaPura_returnsMiopia() {
        assertEquals("Miopía", calcularDiagnostico("-1.00", ""))
    }

    @Test
    fun calcularDiagnostico_hipermetropiaPura_returnsHipermetropia() {
        assertEquals("Hipermetropía", calcularDiagnostico("+1.00", ""))
    }

    @Test
    fun calcularDiagnostico_emetropia_returnsEmetropia() {
        assertEquals("Emetropía", calcularDiagnostico("0.00", ""))
    }

    @Test
    fun calcularDiagnostico_astigmatismoMiopicoCompuesto_returnsCorrect() {
        assertEquals("Astigmatismo miópico compuesto", calcularDiagnostico("-1.00", "-0.50"))
    }

    @Test
    fun calcularDiagnostico_astigmatismoMixto_returnsCorrect() {
        // +1.00 / -2.50 → meridian1 = +1.0, meridian2 = +1.0 + (-2.5) = -1.5 → one positive, one negative
        assertEquals("Astigmatismo mixto", calcularDiagnostico("+1.00", "-2.50"))
    }

    @Test
    fun calcularDiagnostico_planoCil_returnsMiopia() {
        assertEquals("Miopía", calcularDiagnostico("-2.00", "0.00"))
    }

    // ─── parseSnellenToLogMar ───────────────────────────────────────

    @Test
    fun parseSnellenToLogMar_2020_returnsZero() {
        val result = parseSnellenToLogMar("20/20")
        assertNotNull(result)
        assertEquals(0.0, result!!, 0.001)
    }

    @Test
    fun parseSnellenToLogMar_2040_returnsCorrect() {
        val result = parseSnellenToLogMar("20/40")
        assertNotNull(result)
        val expected = -Math.log10(20.0 / 40.0)
        assertEquals(expected, result!!, 0.001)
    }

    @Test
    fun parseSnellenToLogMar_20200_returnsOne() {
        val result = parseSnellenToLogMar("20/200")
        assertNotNull(result)
        val expected = -Math.log10(20.0 / 200.0)
        assertEquals(expected, result!!, 0.001)
    }

    @Test
    fun parseSnellenToLogMar_invalid_returnsNull() {
        assertNull(parseSnellenToLogMar("abc"))
    }

    @Test
    fun parseSnellenToLogMar_noSlash_returnsNull() {
        assertNull(parseSnellenToLogMar("2020"))
    }

    @Test
    fun parseSnellenToLogMar_zeroDenominator_returnsNull() {
        assertNull(parseSnellenToLogMar("20/0"))
    }

    @Test
    fun parseSnellenToLogMar_trimmed_works() {
        val result = parseSnellenToLogMar("  20/20  ")
        assertNotNull(result)
        assertEquals(0.0, result!!, 0.001)
    }

    // ─── computeDiagnosticoAuto ──────────────────────────────────────

    @Test
    fun computeDiagnosticoAuto_balanceOd_setsBalance() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            balanceOd = true,
            recetaOdEsf = "-1.00",
            recetaOdCil = "-0.50"
        )
        val result = computeDiagnosticoAuto(state)
        assertEquals(listOf("Balance"), result.diagnosticoOd)
        assertEquals(emptyList<String>(), result.diagnosticoOi)
    }

    @Test
    fun computeDiagnosticoAuto_hasDataOd_computesDiagnosis() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            recetaOdEsf = "-1.00",
            recetaOdCil = "-0.50"
        )
        val result = computeDiagnosticoAuto(state)
        assertEquals(listOf("Astigmatismo miópico compuesto"), result.diagnosticoOd)
        assertEquals(emptyList<String>(), result.diagnosticoOi)
    }

    @Test
    fun computeDiagnosticoAuto_noData_returnsEmptyLists() {
        val state = EvaluacionUiState(fecha = LocalDate.of(2024, 1, 1))
        val result = computeDiagnosticoAuto(state)
        assertEquals(emptyList<String>(), result.diagnosticoOd)
        assertEquals(emptyList<String>(), result.diagnosticoOi)
    }

    @Test
    fun computeDiagnosticoAuto_textBal_recognizesBalanceKeyword() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            recetaOdEsf = "bal",
            recetaOdCil = "-0.50"
        )
        val result = computeDiagnosticoAuto(state)
        assertEquals(listOf("Balance"), result.diagnosticoOd)
    }

    @Test
    fun computeDiagnosticoAuto_bothEyesWithData_computesBoth() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            recetaOdEsf = "-1.00",
            recetaOdCil = "",
            recetaOiEsf = "+1.00",
            recetaOiCil = ""
        )
        val result = computeDiagnosticoAuto(state)
        assertEquals(listOf("Miopía"), result.diagnosticoOd)
        assertEquals(listOf("Hipermetropía"), result.diagnosticoOi)
    }

    // ─── computeOtrosAuto ────────────────────────────────────────────

    @Test
    fun computeOtrosAuto_addCercaPositive_setsPresbicia() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            addCercaOd = "+2.00",
            autoPresbicia = true
        )
        val result = computeOtrosAuto(state)
        assertTrue("Presbicia should be true when add > 0", result.otrosPresbicia)
    }

    @Test
    fun computeOtrosAuto_addCercaZero_clearsPresbicia() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            addCercaOd = "plano",
            autoPresbicia = true
        )
        val result = computeOtrosAuto(state)
        assertFalse("Presbicia should be false when add is plano", result.otrosPresbicia)
    }

    @Test
    fun computeOtrosAuto_anisometropiaAboveThreshold_detected() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            recetaOdEsf = "-5.00",
            recetaOdCil = "",
            recetaOiEsf = "-1.00",
            recetaOiCil = "",
            autoAnisometropia = true
        )
        val result = computeOtrosAuto(state)
        assertTrue("Anisometropía should be true when |EE| >= 2", result.otrosAnisometropia)
    }

    @Test
    fun computeOtrosAuto_anisometropiaBelowThreshold_notDetected() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            recetaOdEsf = "-1.50",
            recetaOdCil = "",
            recetaOiEsf = "-1.00",
            recetaOiCil = "",
            autoAnisometropia = true
        )
        val result = computeOtrosAuto(state)
        assertFalse("Anisometropía should be false when |EE| < 2", result.otrosAnisometropia)
    }

    @Test
    fun computeOtrosAuto_ambliopiaDetected_fromLogMar() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            avCcOdLejos = "20/20",
            avCcOiLejos = "20/40",
            autoAmbliopia = true
        )
        val result = computeOtrosAuto(state)
        assertTrue("Ambliopía should be true when logMar diff >= 0.19", result.otrosAmbliopia)
    }

    @Test
    fun computeOtrosAuto_ambliopiaNotDetected_fromLogMar() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            avCcOdLejos = "20/20",
            avCcOiLejos = "20/20",
            autoAmbliopia = true
        )
        val result = computeOtrosAuto(state)
        assertFalse("Ambliopía should be false when logMar diff < 0.19", result.otrosAmbliopia)
    }

    @Test
    fun computeOtrosAuto_isAddAo_syncsAdd() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            isAddAo = true,
            addCercaOd = "+2.00",
            addCercaOi = "+1.50"
        )
        val result = computeOtrosAuto(state)
        assertEquals("+2.00", result.addCercaOi)
    }

    @Test
    fun computeOtrosAuto_balanceOd_skipsAnisometropia() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            recetaOdEsf = "-5.00",
            recetaOiEsf = "-1.00",
            balanceOd = true,
            autoAnisometropia = true
        )
        val result = computeOtrosAuto(state)
        assertFalse("Anisometropía should be false when balance", result.otrosAnisometropia)
    }

    // ─── normalizeAndTranspose ───────────────────────────────────────

    @Test
    fun normalizeAndTranspose_positiveCil_OD_transposes() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            recetaOdEsf = "-1.00",
            recetaOdCil = "+0.50",
            recetaOdEje = "90"
        )
        val result = normalizeAndTranspose(state, "OD")
        assertEquals("-0.50", result.recetaOdEsf)
        assertEquals("-0.50", result.recetaOdCil)
        assertEquals("180", result.recetaOdEje)
    }

    @Test
    fun normalizeAndTranspose_positiveCil_OI_transposes() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            recetaOiEsf = "-1.00",
            recetaOiCil = "+0.50",
            recetaOiEje = "90"
        )
        val result = normalizeAndTranspose(state, "OI")
        assertEquals("-0.50", result.recetaOiEsf)
        assertEquals("-0.50", result.recetaOiCil)
        assertEquals("180", result.recetaOiEje)
    }

    @Test
    fun normalizeAndTranspose_negativeCil_unchanged() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            recetaOdEsf = "-1.00",
            recetaOdCil = "-0.50",
            recetaOdEje = "90"
        )
        val result = normalizeAndTranspose(state, "OD")
        assertEquals("-1.00", result.recetaOdEsf)
        assertEquals("-0.50", result.recetaOdCil)
        assertEquals("90", result.recetaOdEje)
    }

    @Test
    fun normalizeAndTranspose_noData_returnsSameState() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            recetaOdEsf = "",
            recetaOdCil = ""
        )
        val result = normalizeAndTranspose(state, "OD")
        assertSame(state, result)
    }

    @Test
    fun normalizeAndTranspose_positiveCil_axisExceeds180_adjusts() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            recetaOdEsf = "-1.00",
            recetaOdCil = "+0.50",
            recetaOdEje = "100"
        )
        val result = normalizeAndTranspose(state, "OD")
        assertEquals("-0.50", result.recetaOdEsf)
        assertEquals("-0.50", result.recetaOdCil)
        assertEquals("10", result.recetaOdEje)  // 100 + 90 = 190, - 180 = 10
    }

    @Test
    fun normalizeAndTranspose_esfBecomesPlano_afterTransposition() {
        // -0.50 / +0.50 → transposes to 0.0 sphere → should display as "plano"
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            recetaOdEsf = "-0.50",
            recetaOdCil = "+0.50",
            recetaOdEje = "90"
        )
        val result = normalizeAndTranspose(state, "OD")
        assertEquals("plano", result.recetaOdEsf)
        assertEquals("-0.50", result.recetaOdCil)
        assertEquals("180", result.recetaOdEje)
    }

    // ─── ANISOMETROPIA_UMBRAL_DIOPTRIAS ─────────────────────────────

    @Test
    fun anisometropiaUmbral_isTwoDioptrias() {
        assertEquals(2.0, ANISOMETROPIA_UMBRAL_DIOPTRIAS, 0.001)
    }
}
