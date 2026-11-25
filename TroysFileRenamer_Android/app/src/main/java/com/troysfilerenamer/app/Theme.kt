package com.troysfilerenamer.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GreenNew = Color(0xFF00FFAA)
val PurpleAccent = Color(0xFFCC66FF)
val DimText = Color(0xFF9090C8)
val WarnText = Color(0xFFFFB066)

@Composable
fun TroyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7B6FFF),
            onPrimary = Color.White,
            secondary = Color(0xFF00FFAA),
            onSecondary = Color(0xFF0A0A14),
            background = Color(0xFF12121F),
            onBackground = Color(0xFFF0F0FF),
            surface = Color(0xFF181830),
            onSurface = Color(0xFFE8E8FF),
            surfaceVariant = Color(0xFF1E1E38),
            onSurfaceVariant = Color(0xFFB0B0D8),
            outline = Color(0xFF3A3A60),
            error = Color(0xFFFF6B6B),
        ),
        content = content
    )
}
