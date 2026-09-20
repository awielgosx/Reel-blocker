package com.reelblocker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicGlassScheme = darkColorScheme(
    primary = CosmicGlass.accent,
    onPrimary = CosmicGlass.stageBase,
    secondary = CosmicGlass.mutedInk,
    background = Color.Transparent,
    surface = Color.Transparent,
    onBackground = CosmicGlass.ink,
    onSurface = CosmicGlass.ink,
    error = Color(0xFFFF6B6B),
)

private val LiquidSilverScheme = lightColorScheme(
    primary = LiquidSilver.accent,
    onPrimary = Color.White,
    secondary = LiquidSilver.mutedInk,
    background = Color.Transparent,
    surface = Color.Transparent,
    onBackground = LiquidSilver.ink,
    onSurface = LiquidSilver.ink,
    error = Color(0xFFB3261E),
)

/**
 * "Cosmic Glass" (dark) / "Liquid Silver" (light), per the design reference. Screens should
 * wrap their content in [com.reelblocker.ui.theme.AppBackground] and use [ReelBlockerPanel]
 * instead of a plain Card to pick up the right material.
 */
@Composable
fun ReelBlockerTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) CosmicGlassScheme else LiquidSilverScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
