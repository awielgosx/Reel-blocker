package com.reelblocker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Placeholder palette - swap once the "cosmic glass" design reference is provided.
private val ReelBlockerColorScheme = darkColorScheme(
    primary = Color(0xFF8B9CFF),
    onPrimary = Color(0xFF10123B),
    secondary = Color(0xFFB0B4D8),
    background = Color(0xFF0E1026),
    surface = Color(0xFF1B1F3B),
    onBackground = Color(0xFFF2F3FA),
    onSurface = Color(0xFFF2F3FA),
    error = Color(0xFFFF6B6B),
)

@Composable
fun ReelBlockerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ReelBlockerColorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
