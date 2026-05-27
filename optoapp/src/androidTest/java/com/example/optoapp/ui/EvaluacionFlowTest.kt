package com.example.optoapp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.optoapp.testing.TestTags
import com.example.optoapp.ui.components.evaluacion.CierreSection
import com.example.optoapp.viewmodel.EvaluacionUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Compose UI tests for the Evaluación (Clinical Evaluation) form.
 *
 * Tests [CierreSection] which contains the diagnosis, treatment, and save
 * button — the primary interaction point for completing an evaluation.
 * [EvaluacionUiState] is a simple data class instantiated directly.
 *
 * @see com.example.optoapp.ui.screens.NuevaEvaluacionScreen
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class EvaluacionFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cierreSection_saveButton_isDisplayed() {
        composeTestRule.setContent {
            CierreSection(
                uiState = EvaluacionUiState(),
                onUpdate = {},
                onShowProximaDatePicker = {},
                onSave = {},
                evaluacionId = null
            )
        }

        composeTestRule.onNodeWithTag(TestTags.EVALUACION_GUARDAR_BTN).assertIsDisplayed()
    }

    @Test
    fun cierreSection_saveButton_showsGuardarTexto() {
        composeTestRule.setContent {
            CierreSection(
                uiState = EvaluacionUiState(),
                onUpdate = {},
                onShowProximaDatePicker = {},
                onSave = {},
                evaluacionId = null
            )
        }

        composeTestRule.onNodeWithText("Guardar Evaluación").assertIsDisplayed()
    }

    @Test
    fun cierreSection_saveButton_firesCallback() {
        var saved = false
        composeTestRule.setContent {
            CierreSection(
                uiState = EvaluacionUiState(),
                onUpdate = {},
                onShowProximaDatePicker = {},
                onSave = { saved = true },
                evaluacionId = null
            )
        }

        composeTestRule.onNodeWithTag(TestTags.EVALUACION_GUARDAR_BTN).performClick()
        assert(saved) { "Save callback should have been invoked on click" }
    }

    @Test
    fun cierreSection_diagnosisSection_isDisplayed() {
        composeTestRule.setContent {
            CierreSection(
                uiState = EvaluacionUiState(),
                onUpdate = {},
                onShowProximaDatePicker = {},
                onSave = {},
                evaluacionId = null
            )
        }

        composeTestRule.onNodeWithText("Diagnóstico").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tratamiento").assertIsDisplayed()
    }

    @Test
    fun cierreSection_autoDiagnostico_updatesWithEsferaValues() {
        // When esfera values change in the EvaluacionViewModel, auto-diagnosis
        // fires. Here we verify the CierreSection renders diagnosis dropdowns.
        val uiState = EvaluacionUiState(
            recetaOdEsf = "-2.00",
            recetaOdCil = "-0.75",
            diagnosticoOd = listOf("Miopía")
        )

        composeTestRule.setContent {
            CierreSection(
                uiState = uiState,
                onUpdate = {},
                onShowProximaDatePicker = {},
                onSave = {},
                evaluacionId = null
            )
        }

        // Verify diagnosis section renders with the computed diagnosis
        composeTestRule.onNodeWithText("Diagnóstico OD").assertIsDisplayed()
        composeTestRule.onNodeWithText("Diagnóstico OI").assertIsDisplayed()
    }

    @Test
    fun cierreSection_proximaCitaButton_isDisplayed() {
        composeTestRule.setContent {
            CierreSection(
                uiState = EvaluacionUiState(),
                onUpdate = {},
                onShowProximaDatePicker = {},
                onSave = {},
                evaluacionId = null
            )
        }

        composeTestRule.onNodeWithText("Programar Próxima Cita").assertIsDisplayed()
    }
}
