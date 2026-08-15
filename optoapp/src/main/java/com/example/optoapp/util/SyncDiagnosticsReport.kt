package com.example.optoapp.util

import com.example.optoapp.data.BackgroundError
import com.example.optoapp.data.SyncEntityState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Texto de copy-all para soporte. Conserva clase de falla, SQLSTATE, constraint e IDs;
 * los secretos se eliminan vía [SyncErrorSanitizer.forDiagnostics].
 */
object SyncDiagnosticsReport {

    fun build(
        errorRows: List<SyncEntityState>,
        backgroundErrors: List<BackgroundError>,
    ): String = buildString {
        appendLine("OptoApp — diagnóstico de sincronización")
        appendLine("Generado: ${formatTimestamp(System.currentTimeMillis())}")
        appendLine(
            "Errores locales: ${errorRows.size} | Errores en segundo plano: ${backgroundErrors.size}",
        )
        errorRows.firstOrNull()?.opticaId?.takeIf { it.isNotBlank() }?.let { appendLine("Óptica: $it") }
        appendLine()
        appendLine("[ERRORES LOCALES] (${errorRows.size})")
        if (errorRows.isEmpty()) {
            appendLine("(ninguno)")
        } else {
            errorRows.forEach { row ->
                appendLine("[${row.entityType}] ${row.entityId} status=${row.status}")
                appendLine("  ${SyncErrorSanitizer.forDiagnostics(row.lastError)}")
            }
        }
        appendLine()
        append(backgroundSection(backgroundErrors))
    }

    fun backgroundSection(backgroundErrors: List<BackgroundError>): String = buildString {
        appendLine("[SEGUNDO PLANO] (${backgroundErrors.size})")
        if (backgroundErrors.isEmpty()) {
            appendLine("(ninguno)")
        } else {
            backgroundErrors.forEach { err ->
                appendLine("${formatTimestamp(err.timestampMs)} [${err.source}]")
                appendLine("  ${SyncErrorSanitizer.forDiagnostics(err.message)}")
            }
        }
    }

    private fun formatTimestamp(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ms))
}
