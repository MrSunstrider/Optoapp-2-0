package com.example.optoapp.ui.components.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import com.example.optoapp.ui.theme.LocalOptoDensity

@Composable
fun DrawerQuickAccess(
    entries: List<DrawerQuickAccessEntry>,
    currentRoute: String?,
    onEntryClick: (DrawerQuickAccessEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalOptoDensity.current
    if (entries.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "ACCESOS RÁPIDOS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(
                start = density.screenPadding,
                top = density.sectionGap,
                bottom = density.blockGap,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = density.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(density.blockGap),
        ) {
            entries.forEach { entry ->
                val selected = entry.isSelected(currentRoute)
                Surface(
                    onClick = { onEntryClick(entry) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .then(entry.testTag?.let { Modifier.testTag(it) } ?: Modifier),
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = density.sectionGap, horizontal = density.blockGap),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = entry.icon,
                            contentDescription = entry.contentDescription,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
