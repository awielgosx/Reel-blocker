package com.reelblocker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The concentric-radii rule from the design spec: stage 28 / panel 26 / controls 14. */
object PanelRadii {
    val stage: Dp = 28.dp
    val panel: Dp = 26.dp
    val control: Dp = 14.dp
}

/** Picks Cosmic Glass or Liquid Silver to match the active theme - use this for every card/panel. */
@Composable
fun ReelBlockerPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (isSystemInDarkTheme()) {
        CosmicGlassPanel(modifier, content)
    } else {
        LiquidSilverPanel(modifier, content)
    }
}

@Composable
fun CosmicGlassPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(PanelRadii.panel)
    Box(
        modifier = modifier
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = CosmicGlass.panelShadow,
                spotColor = CosmicGlass.panelShadow,
            )
            .clip(shape)
            .background(CosmicGlass.panelFill)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(CosmicGlass.panelTopHighlight, CosmicGlass.panelBorder)),
                shape = shape,
            ),
    ) {
        // Under-glow: faint blue bloom pooling at the bottom.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(CosmicGlass.panelUnderGlow, Color.Transparent),
                        center = Offset(0.5f, 1.1f),
                    ),
                ),
        )
        // Specular streak: diagonal catch-light across the top-left.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(CosmicGlass.panelSpecular, Color.Transparent),
                    ),
                ),
        )
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun LiquidSilverPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(PanelRadii.panel)
    Box(
        modifier = modifier
            .shadow(elevation = 18.dp, shape = shape, ambientColor = LiquidSilver.panelShadow, spotColor = LiquidSilver.panelShadow)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to LiquidSilver.panelStop0,
                        0.30f to LiquidSilver.panelStop30,
                        0.55f to LiquidSilver.panelStop55,
                        0.76f to LiquidSilver.panelStop76,
                        1.00f to LiquidSilver.panelStop100,
                    ),
                ),
            )
            .border(width = 1.dp, color = LiquidSilver.panelBorder, shape = shape),
    ) {
        // Catch-light: soft gloss over the top portion of the panel.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(0f to LiquidSilver.panelGloss, 0.55f to Color.Transparent),
                    ),
                ),
        )
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            content()
        }
    }
}
