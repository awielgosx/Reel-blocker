package com.reelblocker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Cosmic Glass & Liquid Silver" design tokens, as specified in the design reference.
 * Cosmic Glass = dark theme, Liquid Silver = light theme.
 */
object CosmicGlass {
    // Stage (background) gradient blobs
    val stageBlue = Color(0xFF20305C)
    val stagePurple = Color(0xFF402058)
    val stageDeep = Color(0xFF0A1226)
    val stageBase = Color(0xFF05060E)

    // Panel
    val panelFill = Color.White.copy(alpha = 0.07f)
    val panelBorder = Color.White.copy(alpha = 0.2f)
    val panelTopHighlight = Color.White.copy(alpha = 0.5f)
    val panelUnderGlow = Color(0xFF7896FF).copy(alpha = 0.25f)
    val panelSpecular = Color.White.copy(alpha = 0.35f)
    val panelShadow = Color.Black.copy(alpha = 0.7f)

    // Text / ink
    val ink = Color(0xFFEEF3FB)
    val mutedInk = Color(0xFFB0B4D8)
    val accent = Color(0xFF9195FF)
}

object LiquidSilver {
    // Stage (background) gradient
    val stageWhite = Color(0xFFFFFFFF)
    val stageMid = Color(0xFFEEF1F5)
    val stageEdge = Color(0xFFD3DAE2)

    // Panel gradient stops (160deg)
    val panelStop0 = Color(0xFFFCFDFF)
    val panelStop30 = Color(0xFFECEFF4)
    val panelStop55 = Color(0xFFDBE1E9)
    val panelStop76 = Color(0xFFF4F6F9)
    val panelStop100 = Color(0xFFD3DAE2)

    val panelBorder = Color.White.copy(alpha = 0.9f)
    val panelUnderside = Color(0xFF78A0A5).copy(alpha = 0f) // unused placeholder
    val panelShadow = Color(0xFF283C5A).copy(alpha = 0.45f)
    val panelGloss = Color.White.copy(alpha = 0.7f)

    // Chip / control
    val chipTop = Color(0xFFF7F9FB)
    val chipBottom = Color(0xFFE7ECF1)

    // Text / ink
    val ink = Color(0xFF1B2534)
    val mutedInk = Color(0xFF41506A)
    val accent = Color(0xFF5B5FE0)
}
