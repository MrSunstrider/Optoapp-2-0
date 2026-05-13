package com.example.optoapp.ui.components.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.data.SecurityManager

@Composable
fun SecuritySection(
    pinHasBeenSet: Boolean,
    isPinRequired: Boolean,
    pinActual: String,
    nuevoPin: String,
    confirmPin: String,
    onPinActualChange: (String) -> Unit,
    onNuevoPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onPinRequiredChange: (Boolean) -> Unit,
    onUpdatePin: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.config_security_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.config_security_pin_required_title), fontSize = 16.sp)
                    Text(
                        stringResource(R.string.config_security_pin_required_desc),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isPinRequired,
                    onCheckedChange = onPinRequiredChange
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (pinHasBeenSet) {
                OutlinedTextField(
                    value = pinActual,
                    onValueChange = { onPinActualChange(it.filter { c -> c.isDigit() }.take(SecurityManager.PIN_LENGTH)) },
                    label = { Text(stringResource(R.string.config_pin_current_label, SecurityManager.PIN_LENGTH)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            OutlinedTextField(
                value = nuevoPin,
                onValueChange = { onNuevoPinChange(it.filter { c -> c.isDigit() }.take(SecurityManager.PIN_LENGTH)) },
                label = { Text(stringResource(R.string.config_pin_new_label, SecurityManager.PIN_LENGTH)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { onConfirmPinChange(it.filter { c -> c.isDigit() }.take(SecurityManager.PIN_LENGTH)) },
                label = { Text(stringResource(R.string.config_pin_confirm_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(
                onClick = onUpdatePin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (pinHasBeenSet) stringResource(R.string.config_pin_update_action)
                    else "Crear PIN"
                )
            }
        }
    }
}
