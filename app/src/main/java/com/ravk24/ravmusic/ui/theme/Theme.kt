package com.ravk24.ravmusic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Blurple,
    onPrimary = White,
    primaryContainer = Lavender,
    onPrimaryContainer = Blurple,
    secondary = Navy,
    onSecondary = White,
    secondaryContainer = Mist,
    onSecondaryContainer = Navy,
    tertiary = Cyan,
    onTertiary = Navy,
    background = White,
    onBackground = Navy,
    surface = White,
    onSurface = Navy,
    surfaceVariant = Mist,
    onSurfaceVariant = Slate,
    surfaceContainer = Mist,
    surfaceContainerLow = White,
    surfaceContainerHigh = Mist,
    outline = Border,
    outlineVariant = BorderSoft,
)

private val DarkColors = darkColorScheme(
    primary = Blurple,
    onPrimary = White,
    primaryContainer = NavySurface,
    onPrimaryContainer = Cyan,
    secondary = Cyan,
    onSecondary = Navy,
    secondaryContainer = NavySurface,
    onSecondaryContainer = White,
    tertiary = Cyan,
    onTertiary = Navy,
    background = Navy,
    onBackground = White,
    surface = NavySurface,
    onSurface = White,
    surfaceVariant = NavySurface,
    onSurfaceVariant = SlateDark,
    surfaceContainer = NavySurface,
    surfaceContainerLow = Navy,
    surfaceContainerHigh = NavySurface,
    outline = NavyBorder,
    outlineVariant = NavyBorder,
)

/**
 * App theme. [darkTheme] defaults to the system setting; it is a parameter so a later
 * Settings change can inject a user override without touching any screen.
 * Dynamic colour is intentionally never applied.
 */
@Composable
fun RavMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = RavMusicTypography,
        content = content,
    )
}
