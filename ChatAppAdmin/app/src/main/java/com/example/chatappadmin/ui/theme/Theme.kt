package com.example.chatappadmin.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    primaryContainer = LightPrimaryVariant,
    secondary = LightSecondary,
    background = LightBackground,
    surface = LightBackground,
    surfaceVariant = LightReceivedBubble,
    onPrimary = Color.White,
    onSecondary = LightTextPrimary,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    error = LightError,
    onError = LightBackground,
    outline = LightDivider,
    outlineVariant = LightDivider.copy(alpha = 0.5f),
    scrim = LightTextPrimary.copy(alpha = 0.3f)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    primaryContainer = DarkPrimaryVariant,
    secondary = DarkSecondary,
    background = DarkBackground,
    surface = DarkBackground,
    surfaceVariant = DarkReceivedBubble,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    error = DarkError,
    onError = DarkTextPrimary,
    outline = DarkDivider,
    outlineVariant = DarkDivider.copy(alpha = 0.5f),
    scrim = DarkTextPrimary.copy(alpha = 0.3f)
)

@Composable
fun ChatAppAdminTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
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
        content = content
    )
}