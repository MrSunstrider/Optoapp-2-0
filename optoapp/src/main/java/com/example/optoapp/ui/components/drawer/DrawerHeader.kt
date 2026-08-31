package com.example.optoapp.ui.components.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.optoapp.BuildConfig
import com.example.optoapp.ui.theme.LocalOptoDensity
import com.example.optoapp.viewmodel.OpticaHeaderUi

@Composable
fun DrawerHeader(
    opticaHeader: OpticaHeaderUi,
    isOnline: Boolean,
    onChangeOptica: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalOptoDensity.current
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = density.sectionGap, horizontal = density.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val initial = opticaHeader.nombreOptica.firstOrNull()?.uppercase() ?: "O"
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(density.blockGap))
            Text(
                text = opticaHeader.nombreOptica.ifBlank { "Sin óptica" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = opticaHeader.fiscalEtiqueta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isOnline) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            shape = CircleShape,
                        ),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isOnline) "En línea" else "Sin conexión",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOnline) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = " · v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            Spacer(modifier = Modifier.height(density.blockGap))
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onChangeOptica)
                    .padding(horizontal = density.blockGap),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Cambiar óptica",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Cambiar óptica",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = density.screenPadding))
    }
}
