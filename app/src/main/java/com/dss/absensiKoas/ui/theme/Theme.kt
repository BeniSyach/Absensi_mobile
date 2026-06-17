package com.dss.absensiKoas.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = PrimaryLight,
    onPrimary        = Color.White,
    primaryContainer = PrimaryMid,
    onPrimaryContainer = TextPrimary,
    secondary        = AccentCyan,
    onSecondary      = PrimaryDark,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = AccentCyan,
    tertiary         = AccentGreen,
    onTertiary       = PrimaryDark,
    background       = PrimaryDark,
    onBackground     = TextPrimary,
    surface          = CardDark,
    onSurface        = TextPrimary,
    surfaceVariant   = Color(0xFF1E293B),
    onSurfaceVariant = TextSecondary,
    error            = ErrorRed,
    onError          = Color.White,
    outline          = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
    primary          = LightPrimary,
    onPrimary        = Color.White,
    primaryContainer = LightBackground,
    onPrimaryContainer = TextDark,
    secondary        = AccentCyan,
    onSecondary      = Color.White,
    tertiary         = AccentGreen,
    onTertiary       = Color.White,
    background       = LightSurface,
    onBackground     = TextDark,
    surface          = Color.White,
    onSurface        = TextDark,
    surfaceVariant   = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    error            = ErrorRed,
    onError          = Color.White
)

@Composable
fun AbsensiKoasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}