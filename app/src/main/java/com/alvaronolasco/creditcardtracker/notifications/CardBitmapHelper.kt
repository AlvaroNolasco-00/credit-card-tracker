package com.alvaronolasco.creditcardtracker.notifications

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface

/**
 * Generates a credit-card-shaped Bitmap for use as the large icon and
 * the card thumbnail in the expanded notification RemoteViews.
 *
 * The bitmap is drawn entirely with Android Canvas — no Compose, no RemoteViews
 * limitations. The result can be set via:
 *   - NotificationCompat.Builder.setLargeIcon(bitmap)     ← collapsed view
 *   - RemoteViews.setImageViewBitmap(R.id.notif_card_image, bitmap) ← expanded view
 */
object CardBitmapHelper {

    /**
     * @param cardName   Display name of the card (e.g. "Visa Signature")
     * @param bank       Issuer / bank name (e.g. "BBVA")
     * @param lastFour   Last four digits of the card number
     * @param cardColor  ARGB Int color stored in CreditCard.color
     * @param widthPx    Output width in pixels (default: 240 — ~96dp at xxhdpi)
     * @param heightPx   Output height in pixels (default: 150 — ~60dp at xxhdpi)
     */
    fun generate(
        cardName: String,
        bank: String,
        lastFour: String,
        cardColor: Int,
        widthPx: Int = 240,
        heightPx: Int = 150
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // --- Rounded-rect clipping path ---
        val cornerRadius = heightPx * 0.1f
        val clipPath = Path().apply {
            addRoundRect(
                RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()),
                cornerRadius, cornerRadius,
                Path.Direction.CW
            )
        }
        canvas.clipPath(clipPath)

        // --- Background gradient: card color → darkened variant ---
        val darkColor = darken(cardColor, 0.50f)
        val gradient = LinearGradient(
            0f, 0f,
            widthPx.toFloat(), heightPx.toFloat(),
            intArrayOf(lighten(cardColor, 1.15f), cardColor, darkColor),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = gradient }
        canvas.drawRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), bgPaint)

        // --- Subtle translucent circle shimmer (top-right) ---
        val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 25
        }
        canvas.drawCircle(widthPx * 0.88f, heightPx * -0.25f, heightPx * 0.85f, shimmerPaint)
        canvas.drawCircle(widthPx * 0.72f, heightPx * -0.10f, heightPx * 0.55f, shimmerPaint)

        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val padding = w * 0.07f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            isAntiAlias = true
        }

        // --- Bank name (top-left, small, translucent) ---
        textPaint.apply {
            alpha = 190
            textSize = h * 0.135f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText(bank.take(20).uppercase(), padding, h * 0.26f, textPaint)

        // --- Card name (below bank, bold) ---
        textPaint.apply {
            alpha = 255
            textSize = h * 0.165f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(cardName.take(18), padding, h * 0.46f, textPaint)

        // --- EMV Chip (golden rounded rectangle) ---
        val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")  // gold
        }
        val chipLeft = padding
        val chipTop = h * 0.56f
        val chipRight = chipLeft + w * 0.13f
        val chipBottom = chipTop + h * 0.16f
        canvas.drawRoundRect(
            RectF(chipLeft, chipTop, chipRight, chipBottom),
            3f, 3f,
            chipPaint
        )
        // Chip horizontal line
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B8860B")
            strokeWidth = 1.5f
        }
        val chipMidY = (chipTop + chipBottom) / 2f
        canvas.drawLine(chipLeft, chipMidY, chipRight, chipMidY, linePaint)

        // --- Last four digits (bottom-left, monospace) ---
        textPaint.apply {
            alpha = 210
            textSize = h * 0.145f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        canvas.drawText("\u2022\u2022\u2022\u2022 $lastFour", padding, h * 0.88f, textPaint)

        return bitmap
    }

    /**
     * Generates a square version for the notification large icon (collapsed view).
     * Same card design, slightly smaller text proportions for square ratio.
     */
    fun generateLargeIcon(
        cardName: String,
        bank: String,
        lastFour: String,
        cardColor: Int,
        sizePx: Int = 192
    ): Bitmap = generate(
        cardName = cardName,
        bank = bank,
        lastFour = lastFour,
        cardColor = cardColor,
        widthPx = (sizePx * 1.6f).toInt(),
        heightPx = sizePx
    )

    // Multiplies each RGB channel by [factor], clamped to 0-255. Alpha unchanged.
    private fun darken(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }

    // Lightens each RGB channel by [factor] (>1.0 = brighter), clamped to 255.
    private fun lighten(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }
}
