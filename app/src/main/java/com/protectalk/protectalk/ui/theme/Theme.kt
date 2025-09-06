package com.protectalk.protectalk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Green Color Palette
private val LightGreen200 = Color(0xFFA5D6A7)
private val LightGreen500 = Color(0xFF4CAF50)
private val LightGreen700 = Color(0xFF388E3C)
private val LightGreen900 = Color(0xFF1B5E20)
private val GreenAccent200 = Color(0xFF81C784)
private val GreenAccent700 = Color(0xFF2E7D32)
private val LightGreen50 = Color(0xFFF1F8E9)
private val LightGreen100 = Color(0xFFDCEDC8)

private val LightGreenColorScheme = lightColorScheme(
    primary = LightGreen500,
    onPrimary = Color.White,
    primaryContainer = LightGreen100,
    onPrimaryContainer = LightGreen900,
    secondary = GreenAccent200,
    onSecondary = Color.White,
    secondaryContainer = LightGreen50,
    onSecondaryContainer = LightGreen700,
    tertiary = GreenAccent700,
    onTertiary = Color.White,
    background = LightGreen50,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = LightGreen700,
    error = Color(0xFFB00020),
    onError = Color.White
)

@Composable
fun ProtectTalkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightGreenColorScheme,
        content = content
    )
}
