package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = MonoDarkElevated,
    onPrimaryContainer = PureWhite,
    secondary = PureWhite,
    onSecondary = PureBlack,
    secondaryContainer = MonoDarkElevated,
    onSecondaryContainer = PureWhite,
    background = MonoDarkBg,
    onBackground = MonoDarkTextPrimary,
    surface = MonoDarkSurface,
    onSurface = MonoDarkTextPrimary,
    surfaceVariant = MonoDarkElevated,
    onSurfaceVariant = MonoDarkTextSecondary,
    outline = MonoDarkBorder,
    outlineVariant = MonoDarkBorderStrong
)

private val LightColorScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    primaryContainer = MonoLightElevated,
    onPrimaryContainer = PureBlack,
    secondary = PureBlack,
    onSecondary = PureWhite,
    secondaryContainer = MonoLightElevated,
    onSecondaryContainer = PureBlack,
    background = MonoLightBg,
    onBackground = MonoLightTextPrimary,
    surface = MonoLightSurface,
    onSurface = MonoLightTextPrimary,
    surfaceVariant = MonoLightElevated,
    onSurfaceVariant = MonoLightTextSecondary,
    outline = MonoLightBorder,
    outlineVariant = MonoLightBorderStrong
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
