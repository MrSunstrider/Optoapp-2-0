package com.example.optoapp.ui.theme

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.optoapp.ui.navigation.isExpanded

data class OptoDensity(
    val screenPadding: Dp,
    val cardPadding: Dp,
    val listItemPadding: Dp,
    val sectionGap: Dp,
    val blockGap: Dp,
    val wizardHeaderPadding: Dp,
    val emptyStatePadding: Dp,
    val isCompact: Boolean,
) {
    companion object {
        val Comfortable = OptoDensity(
            screenPadding = 16.dp,
            cardPadding = 16.dp,
            listItemPadding = 14.dp,
            sectionGap = 12.dp,
            blockGap = 8.dp,
            wizardHeaderPadding = 12.dp,
            emptyStatePadding = 32.dp,
            isCompact = false,
        )

        val Compact = OptoDensity(
            screenPadding = 12.dp,
            cardPadding = 12.dp,
            listItemPadding = 10.dp,
            sectionGap = 8.dp,
            blockGap = 6.dp,
            wizardHeaderPadding = 8.dp,
            emptyStatePadding = 20.dp,
            isCompact = true,
        )
    }
}

fun resolveOptoDensity(windowSizeClass: WindowSizeClass?): OptoDensity =
    if (windowSizeClass != null && !windowSizeClass.isExpanded()) {
        OptoDensity.Compact
    } else {
        OptoDensity.Comfortable
    }

val LocalOptoDensity = staticCompositionLocalOf { OptoDensity.Comfortable }
