package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CloudTermColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005144),
    onPrimaryContainer = Color(0xFF70FCE0),
    secondary = CyanGlow,
    onSecondary = Color(0xFF003544),
    secondaryContainer = Color(0xFF004D63),
    onSecondaryContainer = Color(0xFFBCE9FF),
    tertiary = GreenTerminal,
    onTertiary = Color(0xFF003914),
    tertiaryContainer = Color(0xFF005320),
    onTertiaryContainer = Color(0xFF86F99B),
    background = TerminalBg,
    onBackground = TextPrimary,
    surface = TerminalSurface,
    onSurface = TextPrimary,
    surfaceVariant = TerminalSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = TerminalBorder,
    outlineVariant = Color(0xFF1E293B),
    error = RedError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek cloud terminal dark theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CloudTermColorScheme,
        typography = Typography,
        content = content
    )
}
