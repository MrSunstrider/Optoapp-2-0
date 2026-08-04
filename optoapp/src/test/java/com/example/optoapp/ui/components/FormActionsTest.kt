package com.example.optoapp.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FormActionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersSaveAndCancelByDefault() {
        composeTestRule.setContent {
            FormActions(onSave = {}, onCancel = {})
        }
        composeTestRule.onNodeWithText("Guardar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelar").assertIsDisplayed()
    }

    @Test
    fun hidesCancelWhenNull() {
        composeTestRule.setContent {
            FormActions(onSave = {}, onCancel = null)
        }
        composeTestRule.onNodeWithText("Guardar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelar").assertDoesNotExist()
    }

    @Test
    fun saveButtonIsDisabledWhenSaveEnabledFalse() {
        composeTestRule.setContent {
            FormActions(onSave = {}, onCancel = {}, saveEnabled = false)
        }
        composeTestRule.onNodeWithText("Guardar").assertIsNotEnabled()
    }

    @Test
    fun saveButtonIsEnabledByDefault() {
        composeTestRule.setContent {
            FormActions(onSave = {}, onCancel = {})
        }
        composeTestRule.onNodeWithText("Guardar").assertIsEnabled()
    }

    @Test
    fun customTextsAreRendered() {
        composeTestRule.setContent {
            FormActions(
                onSave = {},
                onCancel = {},
                saveText = "Submit",
                cancelText = "Abort",
            )
        }
        composeTestRule.onNodeWithText("Submit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Abort").assertIsDisplayed()
    }
}
