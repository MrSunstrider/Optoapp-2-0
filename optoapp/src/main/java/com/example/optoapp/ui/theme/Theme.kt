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
    primary = PrimaryDark,
    secondary = PrimaryDarkVariant,
    tertiary = AccentGreenDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color(0xFF080C14),
    onSecondary = Color(0xFF080C14),
    onTertiary = Color(0xFF080C14),
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = ErrorRedDark,
    secondaryContainer = SurfaceDarkMuted,
    onSecondaryContainer = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = PrimaryDark_s,
    tertiary = AccentGreen,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorRed,
    secondaryContainer = SurfaceLightMuted,
    onSecondaryContainer = TextPrimary
)

@Composable
fun OptoAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Desactivamos dynamicColor para forzar nuestra paleta médica profesional.
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
        content = content
    )
}
