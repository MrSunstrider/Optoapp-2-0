package com.example.optoapp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.optoapp.MainActivity
import com.example.optoapp.testing.TestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the login + PIN verification flow.
 *
 * These tests validate the login screen rendering and basic interactions
 * **without** requiring network calls or real authentication.
 * Full login→PIN→Main navigation requires configured test credentials.
 *
 * @see com.example.optoapp.ui.screens.LoginScreen
 * @see com.example.optoapp.ui.screens.PinScreen
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LoginFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ── Screen Rendering ──────────────────────────────────────────────────────

    @Test
    fun loginScreen_root_isDisplayed() {
        composeTestRule.onNodeWithTag(TestTags.LOGIN_SCREEN_ROOT)
            .assertIsDisplayed()
    }

    @Test
    fun emailField_isDisplayed() {
        composeTestRule.onNodeWithTag(TestTags.LOGIN_EMAIL_FIELD)
            .assertIsDisplayed()
    }

    @Test
    fun passwordField_isDisplayed() {
        composeTestRule.onNodeWithTag(TestTags.LOGIN_PASSWORD_FIELD)
            .assertIsDisplayed()
    }

    @Test
    fun loginButton_isDisplayed() {
        composeTestRule.onNodeWithTag(TestTags.LOGIN_INGRESAR_BTN)
            .assertIsDisplayed()
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    fun loginButton_isDisabled_whenFieldsEmpty() {
        composeTestRule.onNodeWithTag(TestTags.LOGIN_INGRESAR_BTN)
            .assertIsNotEnabled()
    }

    @Test
    fun loginButton_isEnabled_whenFieldsAreFilled() {
        composeTestRule.onNodeWithTag(TestTags.LOGIN_EMAIL_FIELD)
            .performTextInput("test@example.com")
        composeTestRule.onNodeWithTag(TestTags.LOGIN_PASSWORD_FIELD)
            .performTextInput("password123")

        composeTestRule.onNodeWithTag(TestTags.LOGIN_INGRESAR_BTN)
            .assertIsEnabled()
    }

    @Test
    fun loginButton_showsEntrarAlSistemaText() {
        composeTestRule.onNodeWithText("ENTRAR AL SISTEMA")
            .assertIsDisplayed()
    }

    // ── PIN Screen ────────────────────────────────────────────────────────────

    @Test
    fun pinScreen_notVisible_byDefault() {
        // PIN screen should NOT be visible when no login attempt was made
        composeTestRule.onNodeWithTag(TestTags.PIN_SCREEN_ROOT)
            .assertDoesNotExist()
    }
}
