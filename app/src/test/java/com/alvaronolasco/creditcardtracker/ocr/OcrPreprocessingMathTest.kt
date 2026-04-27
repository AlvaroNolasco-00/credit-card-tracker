package com.alvaronolasco.creditcardtracker.ocr

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the dark-mode preprocessing color-matrix math.
 *
 * These tests reproduce the formula in OcrProcessor.preprocessBitmapForOcr()
 * without requiring the Android SDK (no Bitmap, Canvas, or Context).
 *
 * Bug history: brightness=80 in dark mode collapsed every pixel to black
 * (out = -1.8 * 200 + 80 = -280 → 0). Fix: brightness = 255 * contrast + lightBrightness.
 */
class OcrPreprocessingMathTest {

    /**
     * Mirrors the formula in preprocessBitmapForOcr() for a single grayscale pixel.
     * R=G=B=v, luminance weights sum to 1.
     */
    private fun applyMatrix(v: Float, sign: Float, contrast: Float, brightness: Float): Float =
        (sign * v * contrast + brightness).coerceIn(0f, 255f)

    // ── Light mode ────────────────────────────────────────────────────────────

    @Test
    fun `light mode large image - white background stays bright`() {
        val contrast = 1.8f
        val lightBrightness = -60f
        val out = applyMatrix(255f, sign = 1f, contrast, lightBrightness)
        assertTrue("White bg should stay near-white, got $out", out >= 200f)
    }

    @Test
    fun `light mode large image - dark text stays dark`() {
        val contrast = 1.8f
        val lightBrightness = -60f
        val out = applyMatrix(0f, sign = 1f, contrast, lightBrightness)
        assertTrue("Black text should stay black, got $out", out == 0f)
    }

    // ── Dark mode (broken before fix) ────────────────────────────────────────

    @Test
    fun `dark mode large image - dark background inverts to bright`() {
        val contrast = 1.8f
        val lightBrightness = -60f
        val brightness = 255f * contrast + lightBrightness  // = 399 (fix)
        val out = applyMatrix(40f, sign = -1f, contrast, brightness)
        assertTrue("Dark bg should invert to near-white after fix, got $out", out >= 200f)
    }

    @Test
    fun `dark mode large image - light text inverts to dark`() {
        val contrast = 1.8f
        val lightBrightness = -60f
        val brightness = 255f * contrast + lightBrightness  // = 399
        val out = applyMatrix(200f, sign = -1f, contrast, brightness)
        assertTrue("Light text should invert to dark after fix, got $out", out <= 60f)
    }

    @Test
    fun `dark mode large image - old broken brightness collapses to black`() {
        // Reproduces pre-fix behavior: brightness=80 → everything black.
        // Kept as documentation; values confirm the old bug was real.
        val contrast = 1.8f
        val brokenBrightness = 80f
        val bgOut   = applyMatrix(40f,  sign = -1f, contrast, brokenBrightness)
        val textOut = applyMatrix(200f, sign = -1f, contrast, brokenBrightness)
        assertTrue("OLD brightness=80: dark bg should collapse near-black, got $bgOut",   bgOut   <= 20f)
        assertTrue("OLD brightness=80: light text should collapse to 0, got $textOut",    textOut == 0f)
    }

    @Test
    fun `dark mode small image - dark background inverts to bright`() {
        val contrast = 1.4f
        val lightBrightness = -40f
        val brightness = 255f * contrast + lightBrightness  // = 317
        val out = applyMatrix(40f, sign = -1f, contrast, brightness)
        assertTrue("Dark bg (small) should invert to near-white, got $out", out >= 200f)
    }

    @Test
    fun `dark mode small image - light text inverts to dark`() {
        val contrast = 1.4f
        val lightBrightness = -40f
        val brightness = 255f * contrast + lightBrightness  // = 317
        val out = applyMatrix(200f, sign = -1f, contrast, brightness)
        assertTrue("Light text (small) should invert to dark, got $out", out <= 60f)
    }

    // ── Symmetry property ────────────────────────────────────────────────────

    @Test
    fun `dark mode output range is symmetric to light mode for complementary inputs`() {
        // light mode: v=255 → bright, v=0 → 0
        // dark mode:  v=0   → bright, v=255 → 0 (inverted)
        // Both extremes should reach the same clamped values
        val contrast = 1.8f; val lightBrightness = -60f
        val darkBrightness = 255f * contrast + lightBrightness

        val lightWhite = applyMatrix(255f, 1f,  contrast, lightBrightness)
        val darkBlack  = applyMatrix(0f,   -1f, contrast, darkBrightness)

        assertTrue("Light white ($lightWhite) and dark black-inverted ($darkBlack) should both be bright",
            lightWhite >= 200f && darkBlack >= 200f)
    }
}
