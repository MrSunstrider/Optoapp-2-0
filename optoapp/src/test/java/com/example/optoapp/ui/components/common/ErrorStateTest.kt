package com.example.optoapp.ui.components.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ErrorStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersMessage() {
        composeTestRule.setContent { ErrorState(message = "Algo salió mal") }
        composeTestRule.onNodeWithText("Algo salió mal").assertIsDisplayed()
    }

    @Test
    fun rendersRetryButtonWhenProvided() {
        composeTestRule.setContent {
            ErrorState(message = "Error de red", onRetry = {})
        }
        composeTestRule.onNodeWithText("Reintentar").assertIsDisplayed()
    }

    @Test
    fun noRetryButtonWhenNull() {
        composeTestRule.setContent { ErrorState(message = "Error") }
        composeTestRule.onNodeWithText("Reintentar").assertDoesNotExist()
    }

    @Test
    fun retryButtonCallsCallback() {
        var called = false
        composeTestRule.setContent {
            ErrorState(message = "Error", onRetry = { called = true })
        }
        composeTestRule.onNodeWithText("Reintentar").performClick()
        assertTrue("onRetry should be called", called)
    }

    @Test
    fun rendersErrorIcon() {
        composeTestRule.setContent { ErrorState(message = "Error") }
        composeTestRule.onNodeWithTag("error_state_icon").assertIsDisplayed()
    }
}
