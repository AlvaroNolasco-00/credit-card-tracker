package com.alvaronolasco.creditcardtracker.widget

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.alvaronolasco.creditcardtracker.R

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

    // Solid fallback (still used for small widget accent bar)
    fun cardColor(colorInt: Int) = ColorProvider(Color(colorInt))

    // Gradient drawable selection — one per color family
    @DrawableRes
    fun cardGradientDrawable(colorInt: Int): Int {
        val r = (colorInt shr 16) and 0xFF
        val g = (colorInt shr 8) and 0xFF
        val b = colorInt and 0xFF
        return when {
            isClose(r, g, b, 0xFF, 0x42, 0x42) -> R.drawable.widget_card_red      // CardRed
            isClose(r, g, b, 0xFF, 0xFF, 0x42) -> R.drawable.widget_card_yellow   // CardYellow
            isClose(r, g, b, 0x42, 0x65, 0xFF) -> R.drawable.widget_card_blue     // CardBlue
            isClose(r, g, b, 0x42, 0xFF, 0x45) -> R.drawable.widget_card_green    // CardGreen
            isClose(r, g, b, 0xD3, 0x42, 0xFF) -> R.drawable.widget_card_purple   // CardPurple
            isClose(r, g, b, 0xFF, 0x8C, 0x42) -> R.drawable.widget_card_orange   // CardOrange
            else -> R.drawable.widget_card_dark
        }
    }

    private fun isClose(r: Int, g: Int, b: Int, tr: Int, tg: Int, tb: Int, tol: Int = 40): Boolean =
        kotlin.math.abs(r - tr) <= tol &&
        kotlin.math.abs(g - tg) <= tol &&
        kotlin.math.abs(b - tb) <= tol
}
