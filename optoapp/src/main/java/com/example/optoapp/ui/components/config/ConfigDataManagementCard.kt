package com.example.optoapp.ui.components.config

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.ui.theme.OptoTokens

@Composable
fun DataManagementCard(
    canManageBackups: Boolean,
    createBackupLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    restoreBackupLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    Card(
        shape = OptoTokens.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = OptoTokens.elevation.level1)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.config_data_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.config_data_section_desc), fontSize = 14.sp)
            if (!canManageBackups) {
                Text(stringResource(R.string.config_data_admin_only_backup), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { createBackupLauncher.launch("OptoApp_Backup_${System.currentTimeMillis()}.json") },
                modifier = Modifier.fillMaxWidth(),
                enabled = canManageBackups
            ) {
                Text(stringResource(R.string.config_backup_download_action))
            }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = "Advertencia", tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.config_data_restore_warning), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
            OutlinedButton(
                onClick = { restoreBackupLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth(),
                enabled = canManageBackups,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.config_backup_restore_action))
            }
        }
    }
}
