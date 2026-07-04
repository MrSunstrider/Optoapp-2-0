package com.example.optoapp.ui.components.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.data.BackgroundError
import com.example.optoapp.data.SyncEntityState
import com.example.optoapp.ui.theme.OptoTokens
import com.example.optoapp.viewmodel.SessionRepairState
import com.example.optoapp.viewmodel.SyncDiagnosticsViewModel

@Composable
fun SyncDiagnosticsCard(
    syncDiagVm: SyncDiagnosticsViewModel
) {
    val sessionHealth by syncDiagVm.sessionHealth.collectAsState()
    val remoteTelemetry by syncDiagVm.remoteTelemetry.collectAsState()
    val remoteTelemetryLoading by syncDiagVm.remoteTelemetryLoading.collectAsState()
    val remoteTelemetryError by syncDiagVm.remoteTelemetryError.collectAsState()
    val errorRows by syncDiagVm.errorRows.collectAsState()
    val backgroundErrors by syncDiagVm.backgroundErrors.collectAsState()
    val ctx = LocalContext.current

    Card(
        shape = OptoTokens.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = OptoTokens.elevation.level1)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            Text(
                stringResource(R.string.config_sync_diag_section_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )


            val (sessionIcon, sessionColor, sessionLabel) = when {
                sessionHealth.hasValidSession -> Triple(
                    Icons.Filled.CheckCircle,
                    MaterialTheme.colorScheme.tertiary,
                    stringResource(R.string.config_sync_session_active)
                )
                sessionHealth.consecutiveRefreshFailures > 0 -> Triple(
                    Icons.Filled.SyncProblem,
                    MaterialTheme.colorScheme.error,
                    stringResource(R.string.config_sync_session_token_fail)
                )
                else -> Triple(
                    Icons.Filled.Warning,
                    MaterialTheme.colorScheme.error,
                    stringResource(R.string.config_sync_session_inactive)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(sessionIcon, contentDescription = null, tint = sessionColor, modifier = Modifier.size(20.dp))
                Text(
                    sessionLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = sessionColor
                )
            }

            HorizontalDivider()


            Text(
                stringResource(R.string.config_sync_last_status),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (remoteTelemetryLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.config_sync_checking), fontSize = 12.sp)
                }
            } else {
                val errorMsg = remoteTelemetryError
                if (errorMsg != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            stringResource(R.string.config_sync_remote_error, errorMsg),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                val remote = remoteTelemetry
                if (remote == null && errorMsg == null) {
                    Text(
                        stringResource(R.string.config_sync_no_remote_record),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (remote != null) {
                    val statusColor = if (remote.lastStatus == "ok") {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    Text(
                        stringResource(R.string.config_sync_status_format, remote.lastStatus.uppercase()),
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(
                            R.string.config_sync_last_at_format,
                            remote.lastSyncAt ?: stringResource(R.string.config_sync_unavailable)
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            OutlinedButton(
                onClick = { syncDiagVm.refreshRemoteTelemetry() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.config_sync_verify_now))
            }


            val sessionRepairState by syncDiagVm.sessionRepairState.collectAsState()
            when (val s = sessionRepairState) {
                is SessionRepairState.Idle -> {
                    OutlinedButton(
                        onClick = { syncDiagVm.repairSessionOpticaId() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.SyncProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.config_sync_repair_session), fontSize = 12.sp)
                    }
                }
                is SessionRepairState.Working -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.config_sync_repair_working), fontSize = 12.sp)
                    }
                }
                is SessionRepairState.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                        Text(
                            s.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                is SessionRepairState.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Text(
                            s.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider()


            Text(
                stringResource(R.string.config_sync_local_errors_title),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                stringResource(R.string.config_sync_diag_desc),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))

            if (errorRows.isEmpty()) {
                Text(
                    stringResource(R.string.config_sync_diag_empty),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Card(
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(errorRows, key = { "${it.entityType}:${it.entityId}" }) { row ->
                            SyncErrorRow(row)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            syncDiagVm.clearErrorHistory()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.config_sync_clear_list), fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val text = errorRows.joinToString("\n---\n") { row ->
                                "[${row.entityType}] ${row.entityId}\n${row.lastError}"
                            }
                            val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("sync_errors", text))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.config_sync_copy_all, errorRows.size),
                            fontSize = 12.sp
                        )
                    }
                }
            }


            if (backgroundErrors.isNotEmpty()) {
                HorizontalDivider()

                Text(
                    stringResource(R.string.config_sync_bg_errors_title),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Card(
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        backgroundErrors.take(10).forEach { bgErr ->
                            BackgroundErrorRow(bgErr)
                        }
                        if (backgroundErrors.size > 10) {
                            Text(
                                "... y ${backgroundErrors.size - 10} más",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { syncDiagVm.clearBackgroundErrors() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.config_sync_bg_errors_clear), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SyncErrorRow(row: SyncEntityState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(14.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "[${row.entityType}] ${row.entityId}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                row.lastError,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BackgroundErrorRow(err: BackgroundError) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(14.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "[${err.source}]",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                err.message,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
