package com.example.optoapp.ui.components.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.theme.OptoTokens

@Composable
fun LaboratorySection(
    labNombre: String,
    labContacto: String,
    onLabNombreChange: (String) -> Unit,
    onLabContactoChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        shape = OptoTokens.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = OptoTokens.elevation.level1)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.config_laboratory_section_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.config_laboratory_section_description),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OptoTextField(
                value = labNombre,
                onValueChange = onLabNombreChange,
                label = stringResource(R.string.config_laboratory_name_label)
            )
            OptoTextField(
                value = labContacto,
                onValueChange = onLabContactoChange,
                label = stringResource(R.string.config_laboratory_contact_label),
                keyboardType = KeyboardType.Phone
            )
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.config_laboratory_save_action))
            }
        }
    }
}
