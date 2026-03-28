package com.alvaronolasco.creditcardtracker.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object CardGradients {
    fun getBrushForColor(colorInt: Int): Brush {
        val color = Color(colorInt)

        return when {
            // Red gradient
            isCloseToColor(color, CardRedLight) -> createRedGradient()
            isCloseToColor(color, CardRedMid) -> createRedGradient()
            isCloseToColor(color, CardRedDark) -> createRedGradient()

            // Yellow gradient
            isCloseToColor(color, CardYellowLight) -> createYellowGradient()
            isCloseToColor(color, CardYellowMid) -> createYellowGradient()
            isCloseToColor(color, CardYellowDark) -> createYellowGradient()

            // Blue gradient
            isCloseToColor(color, CardBlueLight) -> createBlueGradient()
            isCloseToColor(color, CardBlueMid) -> createBlueGradient()
            isCloseToColor(color, CardBlueDark) -> createBlueGradient()

            // Green gradient
            isCloseToColor(color, CardGreenLight) -> createGreenGradient()
            isCloseToColor(color, CardGreenMid) -> createGreenGradient()
            isCloseToColor(color, CardGreenDark) -> createGreenGradient()

            // Purple gradient
            isCloseToColor(color, CardPurpleLight) -> createPurpleGradient()
            isCloseToColor(color, CardPurpleMid) -> createPurpleGradient()
            isCloseToColor(color, CardPurpleDark) -> createPurpleGradient()

            // Default: return solid color as gradient
            else -> Brush.verticalGradient(listOf(color, color))
        }
    }

    fun createRedGradient(): Brush = Brush.verticalGradient(
        colors = listOf(CardRedLight, CardRedMid, CardRedDark)
    )

    fun createYellowGradient(): Brush = Brush.verticalGradient(
        colors = listOf(CardYellowLight, CardYellowMid, CardYellowDark)
    )

    fun createBlueGradient(): Brush = Brush.verticalGradient(
        colors = listOf(CardBlueLight, CardBlueMid, CardBlueDark)
    )

    fun createGreenGradient(): Brush = Brush.verticalGradient(
        colors = listOf(CardGreenLight, CardGreenMid, CardGreenDark)
    )

    fun createPurpleGradient(): Brush = Brush.verticalGradient(
        colors = listOf(CardPurpleLight, CardPurpleMid, CardPurpleDark)
    )

    fun getTextColorForCard(colorInt: Int): Color {
        val color = Color(colorInt)

        return when {
            // Yellow and Green cards use dark text for contrast
            isCloseToColor(color, CardYellowLight) || isCloseToColor(color, CardYellowMid) || isCloseToColor(color, CardYellowDark) -> TextDark
            isCloseToColor(color, CardGreenLight) || isCloseToColor(color, CardGreenMid) || isCloseToColor(color, CardGreenDark) -> TextDark
            // All other cards use white text
            else -> Color.White
        }
    }

    // Helper function to check if a color is close to the target (within tolerance)
    private fun isCloseToColor(color: Color, target: Color, tolerance: Int = 10): Boolean {
        val c = color.toArgb()
        val t = target.toArgb()

        val diffR = kotlin.math.abs(((c shr 16) and 0xFF) - ((t shr 16) and 0xFF))
        val diffG = kotlin.math.abs(((c shr 8) and 0xFF) - ((t shr 8) and 0xFF))
        val diffB = kotlin.math.abs((c and 0xFF) - (t and 0xFF))

        return diffR <= tolerance && diffG <= tolerance && diffB <= tolerance
    }
}
