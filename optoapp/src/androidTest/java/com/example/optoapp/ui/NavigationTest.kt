package com.example.optoapp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.optoapp.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for navigation elements.
 *
 * At the login-screen level, tests verify that navigation affordances
 * (Google sign-in, create account, app title) are rendered.
 *
 * The real Supabase client is used (production modules), but in CI the
 * credentials point to the test project (see android-ci.yml local.properties).
 *
 * @see com.example.optoapp.ui.screens.LoginScreen
 * @see com.example.optoapp.ui.components.MainDrawerContent
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ── Login-Screen Navigation ──────────────────────────────────────────────

    @Test
    fun loginScreen_showsOptoAppTitle() {
        composeTestRule.onNodeWithText("OptoApp").assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsClinicalSoftwareSubtitle() {
        composeTestRule.onNodeWithText("Clinical Software 2026").assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsEntrarButton() {
        composeTestRule.onNodeWithText("ENTRAR AL SISTEMA").assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsGoogleSignInButton() {
        composeTestRule.onNodeWithText("Continuar con Google").assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsCrearCuentaButton() {
        composeTestRule.onNodeWithText("Crear cuenta con correo electrónico").assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsVersionLabel() {
        composeTestRule.onNodeWithText(
            "Si ya tienes cuenta, contacta al administrador de tu óptica."
        ).assertIsDisplayed()
    }
}
