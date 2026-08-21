package com.galvyx.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GalvyxVioletBright,
    onPrimary = GalvyxSpaceBlack,
    primaryContainer = GalvyxViolet,
    onPrimaryContainer = GalvyxTextPrimary,
    secondary = GalvyxCyan,
    onSecondary = GalvyxSpaceBlack,
    tertiary = GalvyxAlienGreen,
    onTertiary = GalvyxSpaceBlack,
    background = GalvyxSpaceBlack,
    onBackground = GalvyxTextPrimary,
    surface = GalvyxDeepNavy,
    onSurface = GalvyxTextPrimary,
    surfaceVariant = GalvyxCard,
    onSurfaceVariant = GalvyxTextSecondary,
    outline = GalvyxOutline
)

private val LightColorScheme = lightColorScheme(
    primary = GalvyxViolet,
    onPrimary = Color.White,
    primaryContainer = GalvyxVioletBright,
    onPrimaryContainer = GalvyxLightText,
    secondary = GalvyxCyan,
    onSecondary = GalvyxLightText,
    tertiary = GalvyxAlienGreen,
    onTertiary = GalvyxLightText,
    background = GalvyxLightBackground,
    onBackground = GalvyxLightText,
    surface = GalvyxLightSurface,
    onSurface = GalvyxLightText,
    surfaceVariant = Color(0xFFE9ECF7),
    onSurfaceVariant = Color(0xFF344054),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun GalvyxTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
