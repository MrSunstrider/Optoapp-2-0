package com.example.optoapp.ui.components.config

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.BuildConfig
import com.example.optoapp.R

@Composable
fun ConfigProfileSection(
    email: String,
    rol: String,
    opticaName: String
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.config_section_profile), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.config_profile_email_label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(email.ifBlank { "—" }, fontSize = 12.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.config_profile_rol_label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(rol.ifBlank { "—" }, fontSize = 12.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.config_profile_version_label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(BuildConfig.VERSION_NAME, fontSize = 12.sp)
            }

            if (opticaName.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.config_fiscal_nombre_comercial_label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(opticaName, fontSize = 12.sp)
                }
            }
        }
    }
}
