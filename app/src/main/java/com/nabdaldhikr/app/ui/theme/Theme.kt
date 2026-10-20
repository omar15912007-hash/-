package com.nabdaldhikr.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    background = ScreenBg,
    surface = CardBg,
    primary = PrimaryGold,
    onPrimary = ScreenBg,
    onSurface = TextPrimary,
    onBackground = TextPrimary,
    secondary = PrimaryGold,
    onSecondary = ScreenBg,
    surfaceVariant = HeaderBg,
    onSurfaceVariant = TextSecondary,
    outline = ProgressTrack,
    tertiary = CompletedGreen
)

private val LightColorScheme = lightColorScheme(
    background = Color(0xFFF5F5F0),
    surface = Color(0xFFFFFFFF),
    primary = Color(0xFF2A7A5E),
    onPrimary = TextPrimary,
    onSurface = Color(0xFF0D3B2E),
    onBackground = Color(0xFF0D3B2E),
    secondary = PrimaryGold,
    surfaceVariant = Color(0xFFE8E8E0)
)

@Composable
fun NabdAlDhikrTheme(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
