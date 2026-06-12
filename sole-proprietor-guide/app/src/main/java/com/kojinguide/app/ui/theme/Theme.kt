package com.kojinguide.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF0B3D10),
    secondary = Color(0xFF33691E),
    secondaryContainer = Color(0xFFDCEDC8),
    onSecondaryContainer = Color(0xFF1A3409),
    background = Color(0xFFF8FAF5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEAF2E4)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF0B3D10),
    primaryContainer = Color(0xFF2E5C33),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFAED581),
    secondaryContainer = Color(0xFF36511F),
    onSecondaryContainer = Color(0xFFDCEDC8)
)

@Composable
fun KojinGuideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
