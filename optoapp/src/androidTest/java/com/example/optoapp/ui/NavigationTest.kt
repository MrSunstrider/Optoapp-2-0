package com.example.optoapp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.optoapp.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for navigation elements.
 *
 * At the login-screen level, tests verify that navigation affordances
 * (Google sign-in, create account, app title) are rendered.
 *
 * [TestSupabaseModule] replaces the real Supabase client so tests
 * run against the test project without affecting production.
 *
 * @see com.example.optoapp.ui.screens.LoginScreen
 * @see com.example.optoapp.ui.components.MainDrawerContent
 * @see com.example.optoapp.di.TestSupabaseModule
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@LargeTest
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

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
