package com.example.optoapp.ui.components.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.theme.OptoTokens
import com.example.optoapp.viewmodel.BusinessHoursConfigUi

@Composable
fun BusinessHoursSection(
    ui: BusinessHoursConfigUi,
    onHorarioChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(
        shape = OptoTokens.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = OptoTokens.elevation.level1),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.config_business_hours_section_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.config_business_hours_section_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OptoTextField(
                value = ui.horario,
                onValueChange = onHorarioChange,
                label = stringResource(R.string.config_business_hours_label),
            )
            Button(
                onClick = onSave,
                enabled = !ui.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (ui.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.config_business_hours_save_action))
                }
            }
            ui.message?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp) }
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        }
    }
}
