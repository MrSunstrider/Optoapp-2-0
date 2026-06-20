package com.example.optoapp.ui.components.config

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.ui.theme.OptoTokens
import com.example.optoapp.viewmodel.SyncDiagnosticsViewModel

@Composable
fun SyncDiagnosticsCard(
    syncDiagVm: SyncDiagnosticsViewModel,
    context: Context
) {
    val sessionHealth by syncDiagVm.sessionHealth.collectAsState()
    val remoteTelemetry by syncDiagVm.remoteTelemetry.collectAsState()
    val remoteTelemetryLoading by syncDiagVm.remoteTelemetryLoading.collectAsState()

    Card(
        shape = OptoTokens.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = OptoTokens.elevation.level1)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.config_sync_diag_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            // ── Salud de sesión ────────────────────────────────────────────
            val (sessionIcon, sessionColor, sessionLabel) = when {
                sessionHealth.hasValidSession -> Triple(Icons.Filled.CheckCircle, MaterialTheme.colorScheme.tertiary, stringResource(R.string.config_sync_session_active))
                sessionHealth.consecutiveRefreshFailures > 0 -> Triple(Icons.Filled.SyncProblem, MaterialTheme.colorScheme.error, stringResource(R.string.config_sync_session_token_fail))
                else -> Triple(Icons.Filled.Warning, MaterialTheme.colorScheme.error, stringResource(R.string.config_sync_session_inactive))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(sessionIcon, contentDescription = null, tint = sessionColor, modifier = Modifier.size(20.dp))
                Text(sessionLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = sessionColor)
            }

            HorizontalDivider()

            // ── Última sync ─────────────────────────────────────────────────
            Text(stringResource(R.string.config_sync_last_status), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (remoteTelemetryLoading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.config_sync_checking), fontSize = 12.sp)
                }
            } else {
                val remote = remoteTelemetry
                if (remote == null) {
                    Text(stringResource(R.string.config_sync_no_remote_record), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val statusColor = if (remote.lastStatus == "ok") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    Text(stringResource(R.string.config_sync_status_format, remote.lastStatus.uppercase()), fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.config_sync_last_at_format, remote.lastSyncAt ?: stringResource(R.string.config_sync_unavailable)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            OutlinedButton(onClick = { syncDiagVm.refreshRemoteTelemetry() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.config_sync_verify_now))
            }
        }
    }
}
