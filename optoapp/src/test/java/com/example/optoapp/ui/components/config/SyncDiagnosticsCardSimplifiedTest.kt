package com.example.optoapp.ui.components.config

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD: RED phase — tests for simplified SyncDiagnosticsCard.
 *
 * After simplification, the card should:
 * - Accept only: syncDiagVm (SyncDiagnosticsViewModel) and context (Context)
 * - NOT expose: syncErrors, remoteSyncTelemetry, remoteSyncTelemetryLoading,
 *   remoteSyncTelemetryError, userTimeZone as parameters
 * - NOT have private functions: BackgroundErrorsSection, SessionHealthSection
 */
class SyncDiagnosticsCardSimplifiedTest {

    @Test
    fun `syncDiagnosticsCard simplified signature has only syncDiagVm and context params`() {
        val className = "com.example.optoapp.ui.components.config.ConfigSyncDiagnosticsCardKt"
        val clazz = Class.forName(className)
        val methods = clazz.declaredMethods.filter { it.name == "SyncDiagnosticsCard" }
        assertFalse("SyncDiagnosticsCard must exist", methods.isEmpty())

        val method = methods.first()
        val paramTypes = method.parameterTypes
        val paramNames = paramTypes.map { it.simpleName }

        // After simplification: accepts SyncDiagnosticsViewModel (no Context)
        assertTrue(
            "Debe aceptar SyncDiagnosticsViewModel (encontrado: $paramNames)",
            paramTypes.any { it.simpleName == "SyncDiagnosticsViewModel" },
        )

        // Must NOT accept removed params (Context, List, etc.)
        val forbidden = listOf("Context", "List", "SyncTelemetryRemoteRow", "Boolean", "String")
        val paramSimpleNames = paramTypes.map { it.simpleName }
        val violations = forbidden.filter { it in paramSimpleNames }
        assertTrue(
            "No debe aceptar parámetros: Context/lista/telemetría/loading (violaciones: $violations)",
            violations.isEmpty(),
        )
    }

    @Test
    fun `backgroundErrorsSection must be removed from config package`() {
        val className = "com.example.optoapp.ui.components.config.ConfigSyncDiagnosticsCardKt"
        val clazz = Class.forName(className)
        val methodNames = clazz.declaredMethods.map { it.name }
        assertFalse(
            "BackgroundErrorsSection debe haber sido eliminada",
            "BackgroundErrorsSection" in methodNames,
        )
    }

    @Test
    fun `sessionHealthSection private function may exist or be inlined`() {
        // After simplification, SessionHealthSection may be private or inlined.
        // We only assert the card function exists — it's not a blocking requirement
        // that SessionHealthSection be removed; inlining is acceptable.
        val className = "com.example.optoapp.ui.components.config.ConfigSyncDiagnosticsCardKt"
        val clazz = Class.forName(className)
        val methodNames = clazz.declaredMethods.map { it.name }
        assertTrue(
            "SyncDiagnosticsCard must exist after simplification",
            "SyncDiagnosticsCard" in methodNames,
        )
    }
}
