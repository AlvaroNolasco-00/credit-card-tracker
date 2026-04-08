package com.alvaronolasco.creditcardtracker.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object CardGradients {
    fun getBrushForColor(colorInt: Int): Brush {
        val color = Color(colorInt)
        // Return solid color as brush (no gradients)
        return Brush.verticalGradient(listOf(color, color))
    }

    fun getTextColorForCard(colorInt: Int): Color {
        val color = Color(colorInt)
        // Use WCAG algorithm to calculate optimal text color
        return WCAGContrastUtil.getOptimalTextColor(color)
    }
}
