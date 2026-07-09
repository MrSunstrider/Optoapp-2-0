package com.example.optoapp.viewmodel

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for [EvaluacionDiagnosticoHelper].
 *
 * Low-level parser/calculator tests (parseRefraction, calcularDiagnostico,
 * parseSnellenToLogMar) have been removed — they are fully covered by
 * [com.example.optoapp.viewmodel.diagnostico.DiagnosticoCalculatorTest].
 * This file tests only the higher-level helper functions, which now
 * delegate to DiagnosticoCalculator as the single source of truth.
 */
class EvaluacionDiagnosticoHelperTest {

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
    fun computeOtrosAuto_isAddAo_syncsCercaAndIntermediaAdd() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            isAddAo = true,
            addCercaOd = "+2.00",
            addCercaOi = "+1.50",
            addIntermediaOd = "+1.75",
            addIntermediaOi = "+1.25"
        )
        val result = computeOtrosAuto(state)
        assertEquals("+2.00", result.addCercaOi)
        assertEquals("+1.75", result.addIntermediaOi)
    }

    @Test
    fun computeOtrosAuto_isAddAoFalse_doesNotSyncAdd() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            isAddAo = false,
            addCercaOd = "+2.00",
            addCercaOi = "+1.50",
            addIntermediaOd = "+1.75",
            addIntermediaOi = "+1.25"
        )
        val result = computeOtrosAuto(state)
        assertEquals("+1.50", result.addCercaOi)
        assertEquals("+1.25", result.addIntermediaOi)
    }

    @Test
    fun computeOtrosAuto_addIntermediaPositive_setsPresbicia() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            addIntermediaOd = "+2.00",
            autoPresbicia = true
        )
        val result = computeOtrosAuto(state)
        assertTrue("Presbicia should be true when addIntermedia > 0", result.otrosPresbicia)
    }

    @Test
    fun computeOtrosAuto_addCercaOiOnlyPositive_setsPresbicia() {
        // addCercaOd vacío, addCercaOi con valor (A/O off)
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            addCercaOi = "+2.00",
            isAddAo = false,
            autoPresbicia = true
        )
        val result = computeOtrosAuto(state)
        assertTrue("Presbicia should be true when addCercaOi > 0", result.otrosPresbicia)
    }

    @Test
    fun computeOtrosAuto_addIntermediaOiOnlyPositive_setsPresbicia() {
        // Solo addIntermediaOi tiene valor
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            addIntermediaOi = "+2.00",
            autoPresbicia = true
        )
        val result = computeOtrosAuto(state)
        assertTrue("Presbicia should be true when addIntermediaOi > 0", result.otrosPresbicia)
    }

    @Test
    fun computeOtrosAuto_allAddEmpty_clearsPresbicia() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            addCercaOd = "", addCercaOi = "",
            addIntermediaOd = "", addIntermediaOi = "",
            autoPresbicia = true
        )
        val result = computeOtrosAuto(state)
        assertFalse("Presbicia should be false when all add fields empty", result.otrosPresbicia)
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

    /**
     * BUG: positive cyl with missing axis must NOT transpose.
     * Axis is mandatory — transposing without it produces a wrong axis (0 + 90 = 90°).
     */
    @Test
    fun normalizeAndTranspose_missingAxis_noTranspose() {
        val state = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            recetaOdEsf = "-1.00",
            recetaOdCil = "+0.50",
            recetaOdEje = ""  // axis not yet entered
        )
        val result = normalizeAndTranspose(state, "OD")
        assertSame("transposition must NOT happen when axis is missing", state, result)
    }

    @Test
    fun anisometropiaUmbral_isTwoDioptrias() {
        assertEquals(2.0, ANISOMETROPIA_UMBRAL_DIOPTRIAS, 0.001)
    }
}
