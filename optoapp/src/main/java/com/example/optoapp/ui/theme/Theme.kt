package com.example.optoapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = OptoTokens.colorsDark.primary,
    onPrimary = OptoTokens.colorsDark.onPrimary,
    primaryContainer = OptoTokens.colorsDark.primaryContainer,
    onPrimaryContainer = OptoTokens.colorsDark.onPrimaryContainer,
    secondary = OptoTokens.colorsDark.secondary,
    onSecondary = OptoTokens.colorsDark.onSecondary,
    secondaryContainer = OptoTokens.colorsDark.secondaryContainer,
    onSecondaryContainer = OptoTokens.colorsDark.onSecondaryContainer,
    tertiary = OptoTokens.colorsDark.tertiary,
    onTertiary = OptoTokens.colorsDark.onTertiary,
    tertiaryContainer = OptoTokens.colorsDark.tertiaryContainer,
    onTertiaryContainer = OptoTokens.colorsDark.onTertiaryContainer,
    error = OptoTokens.colorsDark.error,
    onError = OptoTokens.colorsDark.onError,
    errorContainer = OptoTokens.colorsDark.errorContainer,
    onErrorContainer = OptoTokens.colorsDark.onErrorContainer,
    background = OptoTokens.colorsDark.background,
    onBackground = OptoTokens.colorsDark.onBackground,
    surface = OptoTokens.colorsDark.surface,
    onSurface = OptoTokens.colorsDark.onSurface,
    surfaceVariant = OptoTokens.colorsDark.surfaceVariant,
    onSurfaceVariant = OptoTokens.colorsDark.onSurfaceVariant,
    outline = OptoTokens.colorsDark.outline,
    outlineVariant = OptoTokens.colorsDark.outlineVariant,
    inverseSurface = OptoTokens.colorsDark.inverseSurface,
    inverseOnSurface = OptoTokens.colorsDark.inverseOnSurface,
    inversePrimary = OptoTokens.colorsDark.inversePrimary,
    scrim = OptoTokens.colorsDark.scrim,
    surfaceTint = OptoTokens.colorsDark.surfaceTint
)

private val LightColorScheme = lightColorScheme(
    primary = OptoTokens.colors.primary,
    onPrimary = OptoTokens.colors.onPrimary,
    primaryContainer = OptoTokens.colors.primaryContainer,
    onPrimaryContainer = OptoTokens.colors.onPrimaryContainer,
    secondary = OptoTokens.colors.secondary,
    onSecondary = OptoTokens.colors.onSecondary,
    secondaryContainer = OptoTokens.colors.secondaryContainer,
    onSecondaryContainer = OptoTokens.colors.onSecondaryContainer,
    tertiary = OptoTokens.colors.tertiary,
    onTertiary = OptoTokens.colors.onTertiary,
    tertiaryContainer = OptoTokens.colors.tertiaryContainer,
    onTertiaryContainer = OptoTokens.colors.onTertiaryContainer,
    error = OptoTokens.colors.error,
    onError = OptoTokens.colors.onError,
    errorContainer = OptoTokens.colors.errorContainer,
    onErrorContainer = OptoTokens.colors.onErrorContainer,
    background = OptoTokens.colors.background,
    onBackground = OptoTokens.colors.onBackground,
    surface = OptoTokens.colors.surface,
    onSurface = OptoTokens.colors.onSurface,
    surfaceVariant = OptoTokens.colors.surfaceVariant,
    onSurfaceVariant = OptoTokens.colors.onSurfaceVariant,
    outline = OptoTokens.colors.outline,
    outlineVariant = OptoTokens.colors.outlineVariant,
    inverseSurface = OptoTokens.colors.inverseSurface,
    inverseOnSurface = OptoTokens.colors.inverseOnSurface,
    inversePrimary = OptoTokens.colors.inversePrimary,
    scrim = OptoTokens.colors.scrim,
    surfaceTint = OptoTokens.colors.surfaceTint
)

@Composable
fun OptoAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = OptoTokens.getShapes(),
        content = content
    )
}
