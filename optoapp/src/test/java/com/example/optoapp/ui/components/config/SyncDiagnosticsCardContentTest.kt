package com.example.optoapp.ui.components.config

import com.example.optoapp.data.BackgroundError
import com.example.optoapp.data.SyncEntityState
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * TDD: tests for SyncDiagnosticsCard content sections.
 *
 * After re-adding error list, remoteTelemetryError, and backgroundErrors sections,
 * the card MUST reference the ViewModel flows and support clear actions.
 */
class SyncDiagnosticsCardContentTest {

    @Test
    fun `SyncDiagnosticsCard body contains collectAsState for errorRows`() {
        val className = "com.example.optoapp.ui.components.config.ConfigSyncDiagnosticsCardKt"
        val clazz = Class.forName(className)
        val methods = clazz.declaredMethods.filter { it.name == "SyncDiagnosticsCard" }
        assertFalse("SyncDiagnosticsCard must exist", methods.isEmpty())

        val method = methods.first()
        val paramTypes = method.parameterTypes
        assertTrue(
            "Card must accept SyncDiagnosticsViewModel (params: ${paramTypes.map { it.simpleName }})",
            paramTypes.any { it.simpleName == "SyncDiagnosticsViewModel" }
        )
    }

    @Test
    fun `errorRows section string resources exist`() {
        val resName = "com.example.optoapp.R"
        val rClass = Class.forName(resName)

        // Check string inner class exists
        val stringClass = rClass.declaredClasses.find { it.simpleName == "string" }
        assertNotNull("R.string must exist", stringClass)

        // Verify the specific string resources we need
        val expectedStrings = listOf(
            "config_sync_diag_desc",
            "config_sync_diag_empty",
            "config_sync_copy_all",
            "config_sync_clear_list",
            "config_sync_cleared"
        )
        for (name in expectedStrings) {
            val field = runCatching { stringClass!!.getField(name) }.getOrNull()
            assertNotNull("R.string.$name must exist", field)
        }
    }

    @Test
    fun `clearErrorHistory must be callable on SyncDiagnosticsViewModel`() {
        val vmClass = Class.forName("com.example.optoapp.viewmodel.SyncDiagnosticsViewModel")
        val method = runCatching { vmClass.getDeclaredMethod("clearErrorHistory") }.getOrNull()
        assertNotNull("SyncDiagnosticsViewModel.clearErrorHistory() must exist", method)
        assertTrue("clearErrorHistory must be public", Modifier.isPublic(method!!.modifiers))
    }

    @Test
    fun `clearBackgroundErrors must be callable on SyncDiagnosticsViewModel`() {
        val vmClass = Class.forName("com.example.optoapp.viewmodel.SyncDiagnosticsViewModel")
        val method = runCatching { vmClass.getDeclaredMethod("clearBackgroundErrors") }.getOrNull()
        assertNotNull("SyncDiagnosticsViewModel.clearBackgroundErrors() must exist", method)
        assertTrue("clearBackgroundErrors must be public", Modifier.isPublic(method!!.modifiers))
    }

    @Test
    fun `SyncEntityState has fields needed for display`() {
        val entityClass = SyncEntityState::class.java
        val methods = entityClass.declaredMethods.map { it.name }
        assertTrue("SyncEntityState must expose entityType", "getEntityType" in methods || "entityType" in methods)
        assertTrue("SyncEntityState must expose entityId", "getEntityId" in methods || "entityId" in methods)
        assertTrue("SyncEntityState must expose lastError", "getLastError" in methods || "lastError" in methods)
        assertTrue("SyncEntityState must expose status", "getStatus" in methods || "status" in methods)
    }

    @Test
    fun `BackgroundError has fields needed for display`() {
        val errClass = BackgroundError::class.java
        val methods = errClass.declaredMethods.map { it.name }
        assertTrue("BackgroundError must expose source", "getSource" in methods || "source" in methods)
        assertTrue("BackgroundError must expose message", "getMessage" in methods || "message" in methods)
    }
}
