package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AniyomiPrimary,
    secondary = AniyomiAccent,
    tertiary = AniyomiPrimaryDark,
    background = AmoledBackground,
    surface = AmoledSurface,
    onPrimary = Color.White,
    onBackground = Color(0xFFF0F0F0),
    onSurface = Color(0xFFE0E0E0)
)

private val LightColorScheme = lightColorScheme(
    primary = AniyomiPrimary,
    secondary = AniyomiAccent,
    tertiary = AniyomiPrimaryDark,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onBackground = Color(0xFF1E1E1E),
    onSurface = Color(0xFF2E2E2E)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
