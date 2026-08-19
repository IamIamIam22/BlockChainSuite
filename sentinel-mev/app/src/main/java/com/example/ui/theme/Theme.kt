package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CosmicColorScheme = darkColorScheme(
    primary = SentinelGold,
    secondary = SentinelBlue,
    tertiary = SentinelEmerald,
    background = CosmicBackground,
    surface = CosmicSurface,
    onPrimary = CosmicBackground,
    onSecondary = CosmicBackground,
    onTertiary = CosmicBackground,
    onBackground = CosmicTextPrimary,
    onSurface = CosmicTextPrimary,
    surfaceVariant = CosmicCardInner,
    onSurfaceVariant = CosmicTextSecondary,
    outline = CosmicDivider
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CosmicColorScheme,
        typography = Typography,
        content = content
    )
}
