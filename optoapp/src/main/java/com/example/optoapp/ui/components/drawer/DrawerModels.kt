package com.example.optoapp.ui.components.drawer

import androidx.compose.ui.graphics.vector.ImageVector

data class DrawerNavEntry(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
    val isSelected: (String?) -> Boolean = { current -> current == route },
    val badgeCount: Int? = null,
    val isDanger: Boolean = false,
    val testTag: String? = null,
)

data class DrawerNavSection(
    val title: String,
    val entries: List<DrawerNavEntry>,
)

data class DrawerQuickAccessEntry(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
    val isSelected: (String?) -> Boolean = { current -> current == route },
    val testTag: String? = null,
)
