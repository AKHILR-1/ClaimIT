package com.example.claimit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkEditorialColorScheme = darkColorScheme(
    primary = AccentCyanPrimary,
    onPrimary = DarkSlateBackground,
    primaryContainer = DarkSlateSurfaceElevated,
    onPrimaryContainer = TextPrimaryDark,
    secondary = TagAiIndigo,
    onSecondary = TextPrimaryDark,
    background = DarkSlateBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSlateSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSlateSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorderOutline,
    outlineVariant = DarkBorderHighlight,
    error = StatusLostCrimson,
    onError = TextPrimaryDark
)

private val LightEditorialColorScheme = lightColorScheme(
    primary = AccentCyanDark,
    onPrimary = LightSlateSurface,
    primaryContainer = LightSlateSurfaceElevated,
    onPrimaryContainer = TextPrimaryLight,
    secondary = TagAiIndigo,
    onSecondary = TextPrimaryDark,
    background = LightSlateBackground,
    onBackground = TextPrimaryLight,
    surface = LightSlateSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSlateSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorderOutline,
    outlineVariant = LightBorderHighlight,
    error = StatusLostCrimson,
    onError = LightSlateSurface
)

@Composable
fun ClaimITTheme(
    darkTheme: Boolean = true, // Default to sleek dark editorial mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkEditorialColorScheme else LightEditorialColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ClaimItTypography,
        content = content
    )
}
