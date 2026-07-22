package com.example.optoapp.ui.components.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EmptyStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersIcon_withDefaultInbox() {
        composeTestRule.setContent {
            EmptyState(title = "Test Title")
        }
        composeTestRule.onNodeWithTag("empty_state_icon").assertIsDisplayed()
    }

    @Test
    fun rendersTitle() {
        composeTestRule.setContent {
            EmptyState(title = "No data available")
        }
        composeTestRule.onNodeWithText("No data available").assertIsDisplayed()
    }

    @Test
    fun rendersCustomIcon() {
        composeTestRule.setContent {
            EmptyState(icon = Icons.Default.Warning, title = "Warning")
        }
        composeTestRule.onNodeWithTag("empty_state_icon").assertIsDisplayed()
    }

    @Test
    fun rendersSubtitle_whenProvided() {
        composeTestRule.setContent {
            EmptyState(
                title = "Empty",
                subtitle = "There is nothing here yet",
            )
        }
        composeTestRule.onNodeWithText("There is nothing here yet").assertIsDisplayed()
    }

    @Test
    fun noSubtitle_whenNull() {
        composeTestRule.setContent {
            EmptyState(title = "Empty")
        }
        composeTestRule.onNodeWithText("Empty").assertIsDisplayed()
    }

    @Test
    fun rendersActionButton_whenProvided() {
        composeTestRule.setContent {
            EmptyState(
                title = "Empty",
                action = {
                    TextButton(onClick = { }) {
                        Text("Add Item")
                    }
                },
            )
        }
        composeTestRule.onNodeWithText("Add Item").assertIsDisplayed()
    }
}
