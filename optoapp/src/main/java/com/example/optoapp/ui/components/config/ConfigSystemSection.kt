package com.example.optoapp.ui.components.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.ui.components.OptoDropdownMenuField
import com.example.optoapp.ui.theme.OptoTokens

@Composable
fun SystemSection(
    userTimeZone: String?,
    availableTimeZones: List<String>,
    remindersEnabled: Boolean,
    notificationPermissionGranted: Boolean,
    systemNotificationsEnabled: Boolean,
    onUserTimeZoneSelected: (String) -> Unit,
    onRemindersEnabledChanged: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit,
) {
    Card(
        shape = OptoTokens.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = OptoTokens.elevation.level1),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.config_general_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Zona Horaria (SaaS)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Ajusta esto si la hora del app no coincide con tu reloj local.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OptoDropdownMenuField(
                    label = "Seleccionar Ciudad/Zona",
                    selected = userTimeZone ?: "Detectar automáticamente",
                    options = availableTimeZones,
                    onSelected = onUserTimeZoneSelected,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.config_general_reminders_title), fontSize = 16.sp)
                    Text(
                        stringResource(R.string.config_general_reminders_desc),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = onRemindersEnabledChanged,
                )
            }
            val remindersStatusText = when {
                !notificationPermissionGranted -> stringResource(R.string.config_general_reminders_state_no_permission)
                !systemNotificationsEnabled -> stringResource(R.string.config_general_reminders_state_system_disabled)
                !remindersEnabled -> stringResource(R.string.config_general_reminders_state_app_disabled)
                else -> stringResource(R.string.config_general_reminders_state_active)
            }
            val remindersStatusColor = when {
                !notificationPermissionGranted || !systemNotificationsEnabled -> MaterialTheme.colorScheme.errorContainer
                !remindersEnabled -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.primaryContainer
            }
            val remindersStatusTextColor = when {
                !notificationPermissionGranted || !systemNotificationsEnabled -> MaterialTheme.colorScheme.onErrorContainer
                !remindersEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.config_general_reminders_state_prefix),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = remindersStatusText,
                    fontSize = 10.sp,
                    color = remindersStatusTextColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(remindersStatusColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            OutlinedButton(
                onClick = onSendTestNotification,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.config_test_notification_action))
            }
        }
    }
}
