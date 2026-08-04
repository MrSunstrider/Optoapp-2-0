package com.example.optoapp.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable

fun WindowSizeClass.isExpanded(): Boolean =
    widthSizeClass == WindowWidthSizeClass.Expanded

fun WindowSizeClass.isCompact(): Boolean =
    widthSizeClass == WindowWidthSizeClass.Compact

@Composable
fun isTabletLayout(windowSizeClass: WindowSizeClass): Boolean =
    windowSizeClass.isExpanded()
