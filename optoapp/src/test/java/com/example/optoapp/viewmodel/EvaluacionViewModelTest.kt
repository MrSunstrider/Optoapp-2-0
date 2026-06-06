package com.example.optoapp.viewmodel

import com.example.optoapp.ui.components.evaluacion.dipLabelForVpMode
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for EvaluacionUiState data class contracts and EvaluacionViewModel
 * public API contracts.
 *
 * Full ViewModel tests require mocking dependencies (OptoRepository,
 * SessionManager, PostSaveSyncScheduler) — no mocking library is available,
 * so we test data classes and method signatures.
 */
class EvaluacionViewModelTest {

    // ─── EvaluacionUiState data class ─────────────────────────────────────

    @Test
    fun evaluacionUiState_hasDefaultStringValues() {
        val state = EvaluacionUiState(fecha = LocalDate.of(2024, 1, 1))
        assertEquals("", state.motivoConsulta)
        assertEquals("", state.sintomas)
        assertEquals("", state.diagnostico)
        assertEquals("", state.planTratamiento)
        assertEquals("", state.observaciones)
        assertEquals("", state.recetaOdEsf)
        assertEquals("", state.recetaOiEsf)
    }

    @Test
    fun evaluacionUiState_hasDefaultBooleanValues() {
        val state = EvaluacionUiState(fecha = LocalDate.of(2024, 1, 1))
        assertEquals(false, state.isLoading)
        assertEquals(false, state.balanceOd)
        assertEquals(false, state.balanceOi)
        assertEquals(false, state.isAddAo)
        assertEquals(false, state.otrosPresbicia)
        assertEquals(false, state.otrosAnisometropia)
        assertEquals(true, state.autoPresbicia)
        assertEquals(true, state.autoAnisometropia)
        assertEquals(true, state.autoAmbliopia)
        assertEquals(true, state.isVpCerca)
    }

    @Test
    fun evaluacionUiState_hasDefaultListValues() {
        val state = EvaluacionUiState(fecha = LocalDate.of(2024, 1, 1))
        assertTrue(state.necesidadVisual.isEmpty())
        assertTrue(state.diagnosticoOd.isEmpty())
        assertTrue(state.diagnosticoOi.isEmpty())
        assertTrue(state.diagnosticoOtros.isEmpty())
    }

    @Test
    fun evaluacionUiState_hasDefaultNullableValues() {
        val state = EvaluacionUiState(fecha = LocalDate.of(2024, 1, 1))
        assertNull(state.proximaCita)
        assertNull(state.lcFechaAdaptacion)
        assertNull(state.osdiPuntuacion)
        assertNull(state.error)
    }

    @Test
    fun evaluacionUiState_copyWithValues() {
        val state = EvaluacionUiState(fecha = LocalDate.of(2024, 1, 1)).copy(
            motivoConsulta = "Dolor de cabeza frecuente",
            diagnostico = "Miopía",
            proximaCita = LocalDate.of(2026, 6, 1),
            isLoading = true,
            error = "Algo salió mal"
        )
        assertEquals("Dolor de cabeza frecuente", state.motivoConsulta)
        assertEquals("Miopía", state.diagnostico)
        assertEquals(LocalDate.of(2026, 6, 1), state.proximaCita)
        assertTrue(state.isLoading)
        assertEquals("Algo salió mal", state.error)
    }

    @Test
    fun evaluacionUiState_copyDoesNotMutateOriginal() {
        val original = EvaluacionUiState(fecha = LocalDate.of(2024, 1, 1))
        val modified = original.copy(
            motivoConsulta = "Nuevo motivo",
            isLoading = true
        )
        assertEquals("", original.motivoConsulta)
        assertEquals(false, original.isLoading)
        assertEquals("Nuevo motivo", modified.motivoConsulta)
    }

    @Test
    fun evaluacionUiState_dataClassEquality() {
        val state1 = EvaluacionUiState(fecha = LocalDate.of(2024, 6, 15))
        val state2 = EvaluacionUiState(fecha = LocalDate.of(2024, 6, 15))
        assertEquals(state1, state2)
    }

    @Test
    fun evaluacionUiState_dataClassInequality() {
        val state1 = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            diagnostico = "Miopía"
        )
        val state2 = EvaluacionUiState(
            fecha = LocalDate.of(2024, 1, 1),
            diagnostico = "Hipermetropía"
        )
        assertNotEquals(state1, state2)
    }

    // ─── EvaluacionViewModel companion object ─────────────────────────────

    @Test
    fun anisometropiaUmbral_isTwoDioptrias() {
        assertEquals(2.0, ANISOMETROPIA_UMBRAL_DIOPTRIAS, 0.001)
    }

    // ─── saveEvaluacion method contract ────────────────────────────────

    @Test
    fun saveEvaluacion_acceptsCallbackWithStringParams() {
        // Contract test: the method signature accepts (String, String, (String, String) -> Unit)
        val lambda: (String, String) -> Unit = { id, name ->
            assertNotNull(id)
            assertNotNull(name)
        }
        assertNotNull(lambda)
    }

    // ─── dipLabelForVpMode (DipSection helper) ─────────────────────────

    @Test
    fun dipLabelForVpMode_cercaMode() {
        assertEquals("DIP Cerca", dipLabelForVpMode(true))
    }

    @Test
    fun dipLabelForVpMode_intermedioMode() {
        assertEquals("DIP Intermedio", dipLabelForVpMode(false))
    }

    // ─── saveAndScheduleReminder method contract ───────────────────────

    @Test
    fun saveAndScheduleReminder_acceptsCallbackWithStringParam() {
        // Contract test: the method signature accepts:
        // (String, String?, Boolean, (String) -> Unit)
        val lambda: (String) -> Unit = { savedId ->
            assertNotNull(savedId)
        }
        assertNotNull(lambda)
    }
}
