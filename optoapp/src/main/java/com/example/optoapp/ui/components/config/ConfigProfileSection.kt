package com.example.optoapp.ui.components.config

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.BuildConfig
import com.example.optoapp.R
import com.example.optoapp.ui.theme.OptoTokens

@Composable
fun ConfigProfileSection(
    email: String,
    rol: String,
    opticaName: String,
) {
    Card(
        shape = OptoTokens.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = OptoTokens.elevation.level1),
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(email.ifBlank { "\u2014" }, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                text = stringResource(R.string.config_profile_rol_label),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(rol.ifBlank { "\u2014" }, fontSize = 13.sp)
            if (opticaName.isNotBlank()) {
                Text(opticaName, fontSize = 13.sp)
            }
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
