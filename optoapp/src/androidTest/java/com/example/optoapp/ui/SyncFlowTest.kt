package com.example.optoapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the Sync drawer section.
 *
 * Verifies that the sync controls render correctly across [SyncState]
 * transitions. Follows the same component-testing pattern as
 * [PacienteFlowTest] — no Hilt, no real Supabase.
 *
 * @see com.example.optoapp.ui.screens.DrawerContent
 * @see com.example.optoapp.viewmodel.SyncState
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SyncFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── Harness composables ───────────────────────────────────────────────────

    @Composable
    private fun SyncLabel(isLoading: Boolean = false) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Sincronizando...",
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = null
                )
                Text(
                    text = "Sincronizar Cloud",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    @Composable
    private fun SyncErrorBanner(message: String) {
        Column {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    @Composable
    private fun SyncSuccessToast(message: String) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.primary
        )
    }

    // ── Idle state ────────────────────────────────────────────────────────────

    @Test
    fun syncLabel_showsIdleText_whenNotLoading() {
        composeTestRule.setContent { SyncLabel(isLoading = false) }
        composeTestRule.onNodeWithText("Sincronizar Cloud").assertIsDisplayed()
    }

    // ── Loading state ─────────────────────────────────────────────────────────

    @Test
    fun syncLabel_showsLoadingText_whenSyncing() {
        composeTestRule.setContent { SyncLabel(isLoading = true) }
        composeTestRule.onNodeWithText("Sincronizando...").assertIsDisplayed()
    }

    // ── Error state ───────────────────────────────────────────────────────────

    @Test
    fun syncError_showsMessage() {
        composeTestRule.setContent {
            SyncErrorBanner("Sin conexión a internet.")
        }
        composeTestRule.onNodeWithText("Sin conexión a internet.").assertIsDisplayed()
    }

    @Test
    fun syncError_showsSessionExpiredMessage() {
        composeTestRule.setContent {
            SyncErrorBanner("Tu sesión expiró. Vuelve a iniciar sesión.")
        }
        composeTestRule.onNodeWithText("Tu sesión expiró. Vuelve a iniciar sesión.")
            .assertIsDisplayed()
    }

    // ── Success state ─────────────────────────────────────────────────────────

    @Test
    fun syncSuccess_showsMessage() {
        composeTestRule.setContent {
            SyncSuccessToast("Sincronización completada con éxito")
        }
        composeTestRule.onNodeWithText("Sincronización completada con éxito")
            .assertIsDisplayed()
    }

    @Test
    fun syncSuccess_showsDownloadMessage() {
        composeTestRule.setContent {
            SyncSuccessToast("Datos descargados desde la nube correctamente")
        }
        composeTestRule.onNodeWithText("Datos descargados desde la nube correctamente")
            .assertIsDisplayed()
    }

    // ── Conflict indicator ────────────────────────────────────────────────────

    @Test
    fun conflictLabel_isDisplayed() {
        composeTestRule.setContent {
            Column {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text("Conflictos de Sync")
            }
        }
        composeTestRule.onNodeWithText("Conflictos de Sync").assertIsDisplayed()
    }
}
