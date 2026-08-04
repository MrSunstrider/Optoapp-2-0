package com.example.optoapp.ui.components.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LoadingStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersCircularIndicator() {
        composeTestRule.setContent { LoadingState() }
        // No text, just the indicator — assert layout is composed without crash
        composeTestRule.waitForIdle()
    }

    @Test
    fun rendersMessageWhenProvided() {
        composeTestRule.setContent { LoadingState(message = "Cargando datos...") }
        composeTestRule.onNodeWithText("Cargando datos...").assertIsDisplayed()
    }

    @Test
    fun noMessageWhenNull() {
        composeTestRule.setContent { LoadingState(message = null) }
        composeTestRule.onNodeWithText("Cargando datos...").assertDoesNotExist()
    }
}
