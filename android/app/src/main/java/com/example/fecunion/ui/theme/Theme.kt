package com.example.fecunion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF00897B),
    secondaryContainer = Color(0xFFB2DFDB),
    surface = Color(0xFFFAFAFA),
    background = Color(0xFFF5F5F5),
)

@Composable
fun FECunionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
