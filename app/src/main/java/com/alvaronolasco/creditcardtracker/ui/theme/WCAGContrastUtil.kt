package com.alvaronolasco.creditcardtracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * WCAG 2.1 Contrast Ratio Utilities
 * 
 * Provides functions to calculate luminance, contrast ratios, and determine
 * optimal text colors for accessibility compliance.
 */
object WCAGContrastUtil {

    /**
     * Calculates the relative luminance of a color according to WCAG 2.1 formula.
     * L = 0.2126 * R + 0.7152 * G + 0.0722 * B
     * where R, G, B are the linearized sRGB values.
     */
    fun calculateLuminance(color: Color): Double {
        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)
        
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /**
     * Linearizes an sRGB component value.
     * If value <= 0.04045: value / 12.92
     * Otherwise: ((value + 0.055) / 1.055) ^ 2.4
     */
    private fun linearize(component: Float): Double {
        val c = component.toDouble()
        return if (c <= 0.04045) {
            c / 12.92
        } else {
            Math.pow((c + 0.055) / 1.055, 2.4)
        }
    }

    /**
     * Calculates the contrast ratio between two colors.
     * Ratio = (L1 + 0.05) / (L2 + 0.05)
     * where L1 is the lighter luminance and L2 is the darker.
     */
    fun calculateContrastRatio(foreground: Color, background: Color): Double {
        val l1 = calculateLuminance(foreground)
        val l2 = calculateLuminance(background)
        
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        
        return (lighter + 0.05) / (darker + 0.05)
    }

    /**
     * Determines the optimal text color for a given background color.
     * Returns a color that meets WCAG AA (4.5:1) contrast ratio if possible.
     * 
     * Strategy:
     * 1. First try to generate a "familiar" color (similar hue to background)
     * 2. If that doesn't meet AA, fall back to black or white
     */
    fun getOptimalTextColor(background: Color): Color {
        // Try to generate a familiar color first
        val familiarColor = generateFamiliarColor(background)
        val contrastWithFamiliar = calculateContrastRatio(familiarColor, background)
        
        // WCAG AA requires 4.5:1 for normal text
        if (contrastWithFamiliar >= 4.5) {
            return familiarColor
        }
        
        // Fall back to black or white
        return if (calculateLuminance(background) > 0.5) {
            Color.Black
        } else {
            Color.White
        }
    }

    /**
     * Generates a "familiar" color that's related to the background color
     * but adjusted to potentially meet contrast requirements.
     * 
     * Strategy:
     * - For dark backgrounds: lighten significantly
     * - For light backgrounds: darken significantly
     * - Maintain the original hue
     */
    private fun generateFamiliarColor(background: Color): Color {
        val luminance = calculateLuminance(background)
        
        val hsl = rgbToHSL(background.red, background.green, background.blue)
        
        // Adjust lightness to create contrast
        val newLightness = if (luminance < 0.5) {
            // Dark background - make text significantly lighter
            minOf(hsl.lightness + 0.7f, 1.0f)
        } else {
            // Light background - make text significantly darker
            maxOf(hsl.lightness - 0.7f, 0.0f)
        }
        
        return hslToRGB(hsl.hue, hsl.saturation, newLightness)
    }

    /**
     * Converts RGB to HSL color space.
     * Returns HSL values where:
     * - hue: 0-360 degrees
     * - saturation: 0-1
     * - lightness: 0-1
     */
    private fun rgbToHSL(r: Float, g: Float, b: Float): HSL {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        
        var hue = 0f
        val saturation: Float
        val lightness = (max + min) / 2f
        
        if (delta == 0f) {
            hue = 0f
            saturation = 0f
        } else {
            saturation = if (lightness > 0.5f) {
                delta / (2f - max - min)
            } else {
                delta / (max + min)
            }
            
            hue = when {
                max == r -> ((g - b) / delta + if (g < b) 6f else 0f) / 6f
                max == g -> ((b - r) / delta + 2f) / 6f
                else -> ((r - g) / delta + 4f) / 6f
            }
        }
        
        return HSL(hue * 360f, saturation, lightness)
    }

    /**
     * Converts HSL to RGB color space.
     */
    private fun hslToRGB(hue: Float, saturation: Float, lightness: Float): Color {
        val h = hue / 360f
        
        val r: Float
        val g: Float
        val b: Float
        
        if (saturation == 0f) {
            r = lightness
            g = lightness
            b = lightness
        } else {
            val q = if (lightness < 0.5f) {
                lightness * (1f + saturation)
            } else {
                lightness + saturation - lightness * saturation
            }
            val p = 2f * lightness - q
            
            r = hueToRGB(p, q, h + 1f/3f)
            g = hueToRGB(p, q, h)
            b = hueToRGB(p, q, h - 1f/3f)
        }
        
        return Color(r, g, b)
    }

    private fun hueToRGB(p: Float, q: Float, t: Float): Float {
        val tNorm = when {
            t < 0f -> t + 1f
            t > 1f -> t - 1f
            else -> t
        }
        
        return when {
            tNorm < 1f/6f -> p + (q - p) * 6f * tNorm
            tNorm < 1f/2f -> q
            tNorm < 2f/3f -> p + (q - p) * (2f/3f - tNorm) * 6f
            else -> p
        }
    }

    /**
     * Checks if the contrast ratio meets WCAG AA (4.5:1) for normal text.
     */
    fun meetsAA(foreground: Color, background: Color): Boolean {
        return calculateContrastRatio(foreground, background) >= 4.5
    }

    /**
     * Checks if the contrast ratio meets WCAG AAA (7:1) for normal text.
     */
    fun meetsAAA(foreground: Color, background: Color): Boolean {
        return calculateContrastRatio(foreground, background) >= 7.0
    }

    private data class HSL(
        val hue: Float,
        val saturation: Float,
        val lightness: Float
    )
}
