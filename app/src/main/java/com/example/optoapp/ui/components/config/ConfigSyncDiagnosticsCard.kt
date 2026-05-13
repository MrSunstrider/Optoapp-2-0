package com.example.optoapp.ui.components.config

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.data.SyncEntityState
import com.example.optoapp.data.SyncTelemetryRemoteRow
import com.example.optoapp.viewmodel.SyncDiagnosticsViewModel
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SyncDiagnosticsCard(
    syncErrors: List<SyncEntityState>,
    remoteSyncTelemetry: SyncTelemetryRemoteRow?,
    remoteSyncTelemetryLoading: Boolean,
    remoteSyncTelemetryError: String?,
    userTimeZone: String?,
    syncDiagVm: SyncDiagnosticsViewModel,
    context: Context
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.config_sync_diag_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.config_sync_diag_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    val systemZone = java.time.ZoneId.systemDefault().id
                    val effectiveZone = userTimeZone ?: systemZone
                    val localNow = java.time.ZonedDateTime.now(java.time.ZoneId.of(effectiveZone))

                    Text("Reloj Efectivo: ${localNow.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Sistema: $systemZone | Manual: ${userTimeZone ?: "Ninguna"}", fontSize = 10.sp)
                    Text("Fecha Operativa: ${localNow.toLocalDate()}", fontSize = 10.sp)
                }
            }

            HorizontalDivider()
            Text("Estado remoto (servidor)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (remoteSyncTelemetryLoading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text("Consultando última sincronización remota…", fontSize = 12.sp)
                }
            } else {
                val remote = remoteSyncTelemetry
                if (remote == null) {
                    Text("Sin registro remoto aún para esta óptica.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val statusColor = if (remote.lastStatus == "ok") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    Text("Estado: ${remote.lastStatus.uppercase()}", fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                    Text("Etapa: ${remote.lastStage.ifBlank { "n/a" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Última sync: ${formatRemoteSyncDateTime(remote.lastSyncAt, userTimeZone)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Actualizado ${formatRelativeSyncAge(remote.lastSyncAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (remote.lastError.isNotBlank()) {
                        Text("Error: ${remote.lastError}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            remoteSyncTelemetryError?.let { err ->
                Text("No se pudo leer telemetría remota: $err", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }
            OutlinedButton(onClick = { syncDiagVm.refreshRemoteTelemetry() }, modifier = Modifier.fillMaxWidth()) {
                Text("Actualizar estado remoto")
            }
            HorizontalDivider()
            if (syncErrors.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Text(stringResource(R.string.config_sync_diag_empty), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            } else {
                OutlinedButton(
                    onClick = {
                        val body = syncErrors.joinToString("\n\n") { row ->
                            buildString {
                                appendLine("${row.entityType} · ${row.entityId}")
                                appendLine("Estado: ${row.status}")
                                appendLine("Error: ${row.lastError}")
                                append("Actualizado (ms): ${row.updatedAt}")
                            }
                        }
                        val clip = ClipData.newPlainText("Errores sincronización OptoApp", body)
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(clip)
                        Toast.makeText(context, context.getString(R.string.config_sync_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.config_sync_copy_all, syncErrors.size))
                }
                TextButton(onClick = {
                    syncDiagVm.clearErrorHistory()
                    Toast.makeText(context, context.getString(R.string.config_sync_cleared), Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.config_sync_clear_list))
                }
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(syncErrors, key = { "${it.entityType}-${it.entityId}-${it.updatedAt}" }) { row ->
                        Text("${row.entityType} · ${row.entityId.take(12)}… → ${row.lastError}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

private fun formatRemoteSyncDateTime(raw: String?, overrideZoneId: String?): String {
    if (raw.isNullOrBlank()) return "No disponible"
    return runCatching {
        val utcDate = OffsetDateTime.parse(raw)
        val zoneId = if (!overrideZoneId.isNullOrBlank()) ZoneId.of(overrideZoneId) else java.util.TimeZone.getDefault().toZoneId()
        val localDate = utcDate.atZoneSameInstant(zoneId)
        localDate.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
    }.getOrDefault(raw ?: "")
}

private fun formatRelativeSyncAge(raw: String?): String {
    if (raw.isNullOrBlank()) return "hace un momento"
    return runCatching {
        val then = OffsetDateTime.parse(raw).toInstant()
        val now = java.time.Instant.now()
        val seconds = java.time.Duration.between(then, now).seconds.coerceAtLeast(0)
        when {
            seconds < 60 -> "hace menos de 1 min"
            seconds < 3600 -> "hace ${seconds / 60} min"
            seconds < 86400 -> "hace ${seconds / 3600} h"
            else -> "hace ${seconds / 86400} d"
        }
    }.getOrDefault("recientemente")
}
