package com.alvaronolasco.creditcardtracker.widget

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.alvaronolasco.creditcardtracker.R

@Suppress("RestrictedApi")
object WidgetColors {
    // Colores que cambian con dark mode - usan color resources (values / values-night)
    val widgetBackground = ColorProvider(R.color.widget_background)
    val textPrimary = ColorProvider(R.color.widget_text_primary)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val incomeBrand = ColorProvider(R.color.widget_income_brand)
    val incomeOverlay = ColorProvider(R.color.widget_income_overlay)

    // Textos SOBRE la tarjeta: siempre blancos
    val textOnCard = ColorProvider(Color(0xFFFFFFFF))
    val textOnCardSecondary = ColorProvider(Color(0xB3FFFFFF))  // White 70%

    // Overlays semi-transparentes sobre el color de la tarjeta
    val chipOverlay = Color(0x26FFFFFF)   // White 15%
    val badgeOverlay = Color(0x33FFFFFF)  // White 20%

    // Urgencia: rojo suave visible sobre colores oscuros
    val urgentOnCard = Color(0xFFFFD0D0)  // Rojo claro sobre tarjeta
    val urgentColor = ColorProvider(Color(0xFFE57373))  // Para texto fuera de tarjeta

    // Solid color for widgets
    fun cardColor(colorInt: Int) = ColorProvider(Color(colorInt))

    // Return solid color as drawable - widgets now use solid colors
    @DrawableRes
    fun cardGradientDrawable(colorInt: Int): Int {
        // For now, return a generic solid drawable
        // The widget should use cardColor() for the actual color
        return when {
            isCloseToColor(colorInt, 0xFFFF4242.toInt()) -> R.drawable.widget_card_red
            isCloseToColor(colorInt, 0xFFFFFF42.toInt()) -> R.drawable.widget_card_yellow
            isCloseToColor(colorInt, 0xFF4265FF.toInt()) -> R.drawable.widget_card_blue
            isCloseToColor(colorInt, 0xFF42FF45.toInt()) -> R.drawable.widget_card_green
            isCloseToColor(colorInt, 0xFFD342FF.toInt()) -> R.drawable.widget_card_purple
            isCloseToColor(colorInt, 0xFFFF8C42.toInt()) -> R.drawable.widget_card_orange
            else -> R.drawable.widget_card_dark
        }
    }

    private fun isCloseToColor(colorInt: Int, targetColor: Int, tolerance: Int = 40): Boolean {
        val r = (colorInt shr 16) and 0xFF
        val g = (colorInt shr 8) and 0xFF
        val b = colorInt and 0xFF
        val tr = (targetColor shr 16) and 0xFF
        val tg = (targetColor shr 8) and 0xFF
        val tb = targetColor and 0xFF
        return isClose(r, g, b, tr, tg, tb, tolerance)
    }

    private fun isClose(r: Int, g: Int, b: Int, tr: Int, tg: Int, tb: Int, tol: Int = 40): Boolean =
        kotlin.math.abs(r - tr) <= tol &&
        kotlin.math.abs(g - tg) <= tol &&
        kotlin.math.abs(b - tb) <= tol
}
