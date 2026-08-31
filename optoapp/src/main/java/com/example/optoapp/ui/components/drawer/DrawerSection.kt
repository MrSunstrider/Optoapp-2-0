package com.example.optoapp.ui.components.drawer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.optoapp.ui.theme.LocalOptoDensity

@Composable
fun DrawerSection(
    section: DrawerNavSection,
    currentRoute: String?,
    onEntryClick: (DrawerNavEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalOptoDensity.current
    if (section.entries.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(
                start = density.screenPadding,
                top = density.sectionGap,
                bottom = density.blockGap,
            ),
        )
        section.entries.forEach { entry ->
            DrawerNavItem(
                label = entry.label,
                icon = entry.icon,
                contentDescription = entry.contentDescription,
                selected = entry.isSelected(currentRoute),
                onClick = { onEntryClick(entry) },
                badgeCount = entry.badgeCount,
                isDanger = entry.isDanger,
                testTag = entry.testTag,
            )
        }
    }
}
