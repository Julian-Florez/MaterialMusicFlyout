package com.myg.materialmusicflyout.shared.logic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.max

data class WidgetPalette(
    val dominant: Color,
    val container: Color,
    val accent: Color,
    val onAccent: Color
)

fun paletteFromCoverSeed(seed: List<Color>): WidgetPalette {
    val safeSeed = if (seed.isEmpty()) listOf(Color(0xFF546E7A)) else seed
    val dominant = safeSeed.reduce { acc, color -> lerp(acc, color, 0.35f) }
    // Accent follows the average/dominant cover color directly.
    val accent = dominant
    val container = lerp(dominant, Color.Black, 0.55f)
    val luminance = accent.red * 0.2126f + accent.green * 0.7152f + accent.blue * 0.0722f
    val onAccent = if (luminance > 0.6f) Color(0xFF111111) else Color.White

    return WidgetPalette(
        dominant = dominant,
        container = container,
        accent = accent,
        onAccent = onAccent
    )
}

fun blendOverlay(color: Color, alpha: Float): Color {
    val clamped = max(0f, alpha.coerceAtMost(1f))
    return color.copy(alpha = clamped)
}

