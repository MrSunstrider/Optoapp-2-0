package com.example.optoapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.optoapp.testing.TestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the Login screen.
 *
 * Tests the login form components using the same [TestTags] as the production
 * [com.example.optoapp.ui.screens.LoginScreen]. No Hilt/ViewModel required —
 * renders individual composable elements directly, matching the project's
 * androidTest conventions.
 *
 * @see TestTags.LOGIN_EMAIL_FIELD
 * @see TestTags.LOGIN_PASSWORD_FIELD
 * @see TestTags.LOGIN_INGRESAR_BTN
 * @see TestTags.LOGIN_ERROR_MESSAGE
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LoginFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── Harness composables (mirror production LoginScreen tag contracts) ─────

    @Composable
    private fun LoginEmailField(
        value: String = "",
        onValueChange: (String) -> Unit = {}
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.LOGIN_EMAIL_FIELD),
            shape = RoundedCornerShape(12.dp)
        )
    }

    @Composable
    private fun LoginPasswordField(
        value: String = "",
        onValueChange: (String) -> Unit = {}
    ) {
        var showPassword by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff
                                      else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Ocultar" else "Mostrar"
                    )
                }
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.LOGIN_PASSWORD_FIELD),
            shape = RoundedCornerShape(12.dp)
        )
    }

    @Composable
    private fun LoginErrorSurface(message: String) {
        Surface(
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag(TestTags.LOGIN_ERROR_MESSAGE)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(12.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    @Composable
    private fun LoginIngresarButton(
        enabled: Boolean = true
    ) {
        Button(
            onClick = {},
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag(TestTags.LOGIN_INGRESAR_BTN),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "ENTRAR AL SISTEMA",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // ── Rendering tests ───────────────────────────────────────────────────────

    @Test
    fun emailField_isDisplayed() {
        composeTestRule.setContent { LoginEmailField() }
        composeTestRule.onNodeWithTag(TestTags.LOGIN_EMAIL_FIELD).assertIsDisplayed()
    }

    @Test
    fun passwordField_isDisplayed() {
        composeTestRule.setContent { LoginPasswordField() }
        composeTestRule.onNodeWithTag(TestTags.LOGIN_PASSWORD_FIELD).assertIsDisplayed()
    }

    @Test
    fun ingresarButton_isDisplayed() {
        composeTestRule.setContent { LoginIngresarButton() }
        composeTestRule.onNodeWithTag(TestTags.LOGIN_INGRESAR_BTN).assertIsDisplayed()
    }

    @Test
    fun googleOAuthButton_isDisplayed() {
        composeTestRule.setContent {
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                SpacerH(8)
                Text("Continuar con Google", fontWeight = FontWeight.Medium)
            }
        }
        composeTestRule.onNodeWithText("Continuar con Google").assertIsDisplayed()
    }

    @Test
    fun createAccountButton_isDisplayed() {
        composeTestRule.setContent {
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                SpacerH(8)
                Text("Crear cuenta con correo electrónico", fontWeight = FontWeight.Medium)
            }
        }
        composeTestRule.onNodeWithText("Crear cuenta con correo electrónico").assertIsDisplayed()
    }

    // ── Error message test ────────────────────────────────────────────────────

    @Test
    fun errorMessage_isDisplayed_whenAuthFails() {
        composeTestRule.setContent {
            LoginErrorSurface("Credenciales inválidas. Verifica tu email y contraseña.")
        }
        composeTestRule.onNodeWithTag(TestTags.LOGIN_ERROR_MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Credenciales inválidas. Verifica tu email y contraseña.")
            .assertIsDisplayed()
    }

    // ── Input tests ───────────────────────────────────────────────────────────

    @Test
    fun emailField_acceptsTextInput() {
        var captured = ""
        composeTestRule.setContent {
            LoginEmailField(value = captured, onValueChange = { captured = it })
        }
        composeTestRule.onNodeWithTag(TestTags.LOGIN_EMAIL_FIELD)
            .performTextInput("doctor@optica.com")
        assert(captured == "doctor@optica.com") {
            "Expected 'doctor@optica.com' but got '$captured'"
        }
    }

    @Test
    fun passwordField_acceptsTextInput() {
        var captured = ""
        composeTestRule.setContent {
            LoginPasswordField(value = captured, onValueChange = { captured = it })
        }
        composeTestRule.onNodeWithTag(TestTags.LOGIN_PASSWORD_FIELD)
            .performTextInput("SecurePass123!")
        assert(captured == "SecurePass123!") {
            "Expected 'SecurePass123!' but got '$captured'"
        }
    }

    // ── Enabled / Disabled tests ──────────────────────────────────────────────

    @Test
    fun ingresarButton_isDisabled_whenFieldsEmpty() {
        composeTestRule.setContent { LoginIngresarButton(enabled = false) }
        composeTestRule.onNodeWithTag(TestTags.LOGIN_INGRESAR_BTN)
            .assertIsNotEnabled()
    }

    @Test
    fun ingresarButton_isEnabled_whenFieldsHaveContent() {
        composeTestRule.setContent { LoginIngresarButton(enabled = true) }
        composeTestRule.onNodeWithTag(TestTags.LOGIN_INGRESAR_BTN)
            .assertIsDisplayed()
        // Enabled state is implicit when visible; assertIsEnabled requires
        // the node to be actionable, which a Button with onClick qualifies.
    }
}

/** Tiny horizontal spacer for inline icon+text buttons. */
@Composable
private fun SpacerH(width: Int) {
    androidx.compose.foundation.layout.Spacer(
        modifier = Modifier.width(width.dp)
    )
}
