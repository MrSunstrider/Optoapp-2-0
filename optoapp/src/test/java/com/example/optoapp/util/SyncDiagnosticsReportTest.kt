package com.example.optoapp.util

import com.example.optoapp.data.BackgroundError
import com.example.optoapp.data.SyncEntityState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncDiagnosticsReportTest {

    private val quarantined = SyncEntityState(
        opticaId = "0f0e0d0c-0000-4000-8000-00000000000a",
        entityType = "pago",
        entityId = "11111111-0000-4000-8000-000000000001",
        status = "error",
        lastError = "quarantine:check_violation:pagos_monto_chk HTTP 400 code=23514",
    )

    private val bgError = BackgroundError(
        source = "sync:finanzas",
        message = "HTTP 400 Authorization: Bearer eyJleak.sig — 23514 servicios_extra_estado_domain_chk",
        timestampMs = 1_760_000_000_000L,
    )

    @Test
    fun `report includes counts for both sections`() {
        val text = SyncDiagnosticsReport.build(listOf(quarantined), listOf(bgError))
        assertTrue("local error count expected", text.contains("errores locales: 1", ignoreCase = true))
        assertTrue("background count expected", text.contains("segundo plano: 1", ignoreCase = true))
    }

    @Test
    fun `report includes entity identifiers and failure detail`() {
        val text = SyncDiagnosticsReport.build(listOf(quarantined), emptyList())
        assertTrue(text.contains("pago"))
        assertTrue(text.contains("11111111-0000-4000-8000-000000000001"))
        assertTrue(text.contains("pagos_monto_chk"))
        assertTrue(text.contains("23514"))
        assertTrue("optica id gives support the tenant", text.contains("0f0e0d0c-0000-4000-8000-00000000000a"))
    }

    @Test
    fun `report includes background source and sanitized message`() {
        val text = SyncDiagnosticsReport.build(emptyList(), listOf(bgError))
        assertTrue(text.contains("sync:finanzas"))
        assertTrue(text.contains("servicios_extra_estado_domain_chk"))
        assertFalse("bearer token must be redacted", text.contains("eyJleak.sig"))
    }

    @Test
    fun `report is never blank when there is nothing to report`() {
        val text = SyncDiagnosticsReport.build(emptyList(), emptyList())
        assertTrue(text.isNotBlank())
        assertTrue(text.contains("errores locales: 0", ignoreCase = true))
        assertTrue(text.contains("segundo plano: 0", ignoreCase = true))
    }

    @Test
    fun `background section alone is copyable`() {
        val text = SyncDiagnosticsReport.backgroundSection(listOf(bgError))
        assertTrue(text.contains("sync:finanzas"))
        assertFalse(text.contains("eyJleak.sig"))
        assertTrue(text.contains("23514"))
    }
}
