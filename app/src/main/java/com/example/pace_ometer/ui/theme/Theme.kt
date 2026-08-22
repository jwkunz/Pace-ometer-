package com.example.pace_ometer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PaceometerDarkColorScheme = darkColorScheme(
    primary = Crimson,
    onPrimary = OffWhite,
    primaryContainer = CrimsonDark,
    onPrimaryContainer = OffWhite,
    secondary = Gold,
    onSecondary = Black,
    secondaryContainer = GoldDark,
    onSecondaryContainer = Black,
    tertiary = GoldLight,
    onTertiary = Black,
    background = Black,
    onBackground = OffWhite,
    surface = Charcoal,
    onSurface = OffWhite,
    surfaceVariant = CharcoalLight,
    onSurfaceVariant = OffWhite,
    error = CrimsonLight,
    onError = Black
)

private val PaceometerLightColorScheme = lightColorScheme(
    primary = CrimsonDark,
    onPrimary = OffWhite,
    primaryContainer = CrimsonLight,
    onPrimaryContainer = Black,
    secondary = GoldDark,
    onSecondary = OffWhite,
    secondaryContainer = GoldLight,
    onSecondaryContainer = Black,
    tertiary = GoldDark,
    onTertiary = OffWhite,
    background = OffWhite,
    onBackground = Black,
    surface = Color.White,
    onSurface = Black,
    error = CrimsonDark,
    onError = OffWhite
)

/**
 * Pace-ometer's fixed crimson/gold/black brand palette.
 * Dynamic (wallpaper-derived) color is intentionally never used here.
 */
@Composable
fun PaceometerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PaceometerDarkColorScheme else PaceometerLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
