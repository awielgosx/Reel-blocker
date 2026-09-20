package com.reelblocker.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** The "stage" behind every screen: the cosmic nebula in dark mode, the silver sweep in light mode. */
@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize()) {
        if (isSystemInDarkTheme()) CosmicStage() else SilverStage()
        content()
    }
}

@Composable
private fun CosmicStage() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = CosmicGlass.stageBase)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(CosmicGlass.stageDeep, CosmicGlass.stageBase),
                center = Offset(size.width * 0.5f, size.height * 1.3f),
                radius = size.height * 1.1f,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(CosmicGlass.stagePurple, Color.Transparent),
                center = Offset(size.width * 0.88f, size.height * 0.12f),
                radius = size.width * 0.75f,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(CosmicGlass.stageBlue, Color.Transparent),
                center = Offset(size.width * 0.18f, size.height * 0.08f),
                radius = size.width * 0.85f,
            ),
        )
    }
}

@Composable
private fun SilverStage() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(LiquidSilver.stageWhite, LiquidSilver.stageMid, LiquidSilver.stageEdge),
                center = Offset(size.width * 0.3f, 0f),
                radius = size.width * 1.1f,
            ),
        )
    }
}
