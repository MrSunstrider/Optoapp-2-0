package com.example.optoapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.optoapp.testing.TestTags
import com.example.optoapp.ui.components.paciente.PacienteFormSections
import com.example.optoapp.util.DateUtils
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Compose UI tests for the Paciente (Patient) creation form.
 *
 * Tests the [PacienteFormSections] component directly — no ViewModel required.
 * Verifies that all form fields render, accept input, and show required labels.
 *
 * @see com.example.optoapp.ui.screens.NuevoPacienteScreen
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PacienteFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Composable
    private fun PacienteFormHarness(
        nombreCompleto: String = "",
        onNombreCompletoChange: (String) -> Unit = {},
        edad: String = "",
        onEdadChange: (String) -> Unit = {},
        telefono: String = "",
        onTelefonoChange: (String) -> Unit = {},
        email: String = "",
        onEmailChange: (String) -> Unit = {},
        direccion: String = "",
        onDireccionChange: (String) -> Unit = {}
    ) {
        PacienteFormSections(
            nombreCompleto = nombreCompleto,
            onNombreCompletoChange = onNombreCompletoChange,
            edad = edad,
            onEdadChange = onEdadChange,
            telefono = telefono,
            onTelefonoChange = onTelefonoChange,
            dni = "",
            onDniChange = {},
            historiaOptometrica = "",
            onHistoriaOptometricaChange = {},
            fechaNacimiento = "",
            onFechaNacimientoChange = {},
            sexo = "Masculino",
            onSexoChange = {},
            email = email,
            onEmailChange = onEmailChange,
            direccion = direccion,
            onDireccionChange = onDireccionChange,
            distrito = "",
            onDistritoChange = {},
            ocupacion = "",
            onOcupacionChange = {},
            acompanante = "",
            onAcompananteChange = {},
            hobbies = "",
            onHobbiesChange = {},
            fechaCreacion = LocalDate.now(),
            onShowDatePicker = {},
            onSuggestHo = {}
        )
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Test
    fun nombreField_isDisplayed() {
        composeTestRule.setContent { PacienteFormHarness() }
        composeTestRule.onNodeWithTag(TestTags.PACIENTE_NOMBRE_FIELD).assertIsDisplayed()
    }

    @Test
    fun edadField_isDisplayed() {
        composeTestRule.setContent { PacienteFormHarness() }
        composeTestRule.onNodeWithTag(TestTags.PACIENTE_EDAD_FIELD).assertIsDisplayed()
    }

    @Test
    fun telefonoField_isDisplayed() {
        composeTestRule.setContent { PacienteFormHarness() }
        composeTestRule.onNodeWithTag(TestTags.PACIENTE_TELEFONO_FIELD).assertIsDisplayed()
    }

    @Test
    fun emailField_isDisplayed() {
        composeTestRule.setContent { PacienteFormHarness() }
        composeTestRule.onNodeWithTag(TestTags.PACIENTE_EMAIL_FIELD).assertIsDisplayed()
    }

    @Test
    fun direccionField_isDisplayed() {
        composeTestRule.setContent { PacienteFormHarness() }
        composeTestRule.onNodeWithTag(TestTags.PACIENTE_DIRECCION_FIELD).assertIsDisplayed()
    }

    @Test
    fun requiredFields_showAsterisk() {
        composeTestRule.setContent { PacienteFormHarness() }
        composeTestRule.onNodeWithText("Nombre Completo *").assertIsDisplayed()
        composeTestRule.onNodeWithText("Edad *").assertIsDisplayed()
        composeTestRule.onNodeWithText("Teléfono *").assertIsDisplayed()
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Test
    fun nombreField_acceptsTextInput() {
        var captured = ""
        composeTestRule.setContent {
            PacienteFormHarness(
                nombreCompleto = captured,
                onNombreCompletoChange = { captured = it }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PACIENTE_NOMBRE_FIELD)
            .performTextInput("Juan Pérez")

        assert(captured == "Juan Pérez") { "Expected 'Juan Pérez' but got '$captured'" }
    }

    @Test
    fun telefonoField_acceptsNumberInput() {
        var captured = ""
        composeTestRule.setContent {
            PacienteFormHarness(
                telefono = captured,
                onTelefonoChange = { captured = it }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PACIENTE_TELEFONO_FIELD)
            .performTextInput("999888777")

        assert(captured == "999888777") { "Expected '999888777' but got '$captured'" }
    }
}
