package com.alvaronolasco.creditcardtracker.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale
import java.util.regex.Pattern
import java.io.Closeable

enum class Confidence {
    VERIFIED, HIGH, MEDIUM, LOW, NONE
}

class OcrProcessor(private val context: Context) : Closeable {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun close() {
        recognizer.close()
    }

    data class OcrResult(
        val fullText: String,
        val detectedAmount: Double?,
        val confidence: Confidence = Confidence.NONE
    )

    suspend fun processImage(uri: Uri): OcrResult {
        return try {
            val raw = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return OcrResult("", null, Confidence.NONE)
            val processed = preprocessBitmapForOcr(raw)
            if (processed !== raw) raw.recycle()
            val image = InputImage.fromBitmap(processed, 0)
            val visionText = recognizer.process(image).await()
            processed.recycle()
            val fullText = visionText.text
            val detector = AmountDetector()
            val detection = detector.detect(visionText)
            OcrResult(fullText, detection.amount, detection.confidence)
        } catch (e: Exception) {
            OcrResult("", null, Confidence.NONE)
        }
    }

    suspend fun processImageBitmap(bitmap: Bitmap): OcrResult {
        return try {
            val processed = preprocessBitmapForOcr(bitmap)
            val image = InputImage.fromBitmap(processed, 0)
            val visionText = recognizer.process(image).await()
            if (processed !== bitmap) processed.recycle()
            val fullText = visionText.text
            val detector = AmountDetector()
            val detection = detector.detect(visionText)
            OcrResult(fullText, detection.amount, detection.confidence)
        } catch (e: Exception) {
            OcrResult("", null, Confidence.NONE)
        }
    }

    /**
     * Preprocesa el bitmap antes de entregárselo a ML Kit:
     * 1. Escala hacia abajo imágenes muy grandes (ML Kit no necesita más de 2048 px).
     * 2. Convierte a escala de grises usando los pesos de luminancia estándar.
     * 3. Aumenta el contraste para que el texto negro resalte sobre el fondo blanco,
     *    reduciendo la confusión entre caracteres similares (5/S, ,/.).
     *
     * No usa OpenCV; todo es API nativa de Android (ColorMatrix + Canvas).
     */
    private fun preprocessBitmapForOcr(src: Bitmap): Bitmap {
        // 1. Escalar si el lado mayor supera los 2048 px
        val maxDim = 2048
        val scaled = if (src.width > maxDim || src.height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(src.width, src.height)
            Bitmap.createScaledBitmap(
                src,
                (src.width * scale).toInt(),
                (src.height * scale).toInt(),
                true
            )
        } else {
            src
        }

        // 2 + 3. Escala de grises + contraste en un único paso mediante ColorMatrix.
        //
        // La matriz combina los pesos de luminancia (fila RGB idéntica → grises)
        // multiplicados por el factor de contraste, más un offset negativo de brillo
        // que oscurece los tonos medios y separa aún más el texto del fondo.
        //
        //   contrast: amplifica la diferencia tinta/papel.
        //   brightness: desplaza hacia negro para que los grises "sucios" no
        //               confundan al reconocedor de caracteres.
        val contrast = 1.8f
        val brightness = -60f
        val matrix = ColorMatrix(floatArrayOf(
            0.299f * contrast, 0.587f * contrast, 0.114f * contrast, 0f, brightness,
            0.299f * contrast, 0.587f * contrast, 0.114f * contrast, 0f, brightness,
            0.299f * contrast, 0.587f * contrast, 0.114f * contrast, 0f, brightness,
            0f,                0f,                0f,                1f, 0f
        ))

        val result = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(scaled, 0f, 0f, paint)

        if (scaled !== src) scaled.recycle()
        return result
    }
}

class AmountDetector {

    data class DetectionResult(
        val amount: Double?,
        val confidence: Confidence
    )

    /** Internal candidate carrying a numeric score instead of a raw confidence level. */
    private data class ScoredCandidate(val amount: Double, val score: Int)

    // ── Base scores per detection layer ──────────────────────────────────────
    private val SCORE_GEOMETRIC_ALIGN = 50  // same row as keyword (spatial)
    private val SCORE_KEYWORD_MATCH   = 40  // keyword found in plain text
    private val SCORE_POSITION_BASED  = 25  // bottom 40% of image, no keyword
    private val SCORE_LAST_SECTION    = 15  // bottom 50% of text lines
    private val SCORE_LAST_AMOUNT     =  5  // final fallback

    // ── Bonuses ──────────────────────────────────────────────────────────────
    private val BONUS_CURRENCY_SYMBOL    = 30  // regex group 1 has $, Q, USD, MXN…
    private val BONUS_LARGEST_IN_BOTTOM30 = 20  // largest amount in last 30% of lines
    private val BONUS_KEYWORD_IN_BLOCK   = 15  // position block also contains keyword

    // Fix #7: expanded keywords for Central American / Mexican market
    private val totalKeywords = listOf(
        // Most specific first (multi-word) to avoid partial matches on generic terms
        "total a pagar", "neto a pagar", "monto total", "importe total",
        "gran total", "total general", "total factura", "total de compra",
        "total ventas", "venta total", "monto de su compra", "monto de compra",
        "importe neto", "cargo total", "balance due", "amount due",
        "net total", "grand total",
        // Single-word keywords (lower priority)
        "total", "neto", "cobrado", "monto", "importe", "a pagar",
        "amount", "sum", "due", "pay", "cobro",
        "compra por", "consumo", "pagado", "pago"
    )

    // Fix #1: unified pattern — handles large amounts, thousands separators, 1-2 decimal places.
    private val amountRegex = Regex(
        """([$€£¥₣₹]|Q|L|HNL|GTQ|USD|MXN|EUR)?\s*(\d+(?:[.,\s]+\d{3})*(?:[.,]\d{1,2})?)(?:\s*(?:USD|MXN|EUR|GTQ|HNL)\b)?""",
        RegexOption.IGNORE_CASE
    )

    // Fix #2: phone number structural separators only.
    private val phonePatterns = listOf(
        Regex("""(?:\+?\d{1,3}[-.\s])?\(?\d{2,4}\)?[-.\s]\d{3,4}[-.\s]\d{4}"""),
    )

    private val datePatterns = listOf(
        Regex("""\d{4}[-/]\d{1,2}[-/]\d{1,2}"""),
        Regex("""\d{1,2}[-/]\d{1,2}[-/]\d{2,4}"""),
    )

    private val subtotalKeywords = listOf(
        "subtotal", "sub total", "sub-total",
        "antes de impuesto", "antes de iva", "importe bruto",
        "base imponible", "base gravable", "base"
    )

    private val taxKeywords = listOf(
        "i.v.a", "iva", "isv", "igv", "itbis",
        "impuesto", "impuestos", "tax", "gst", "vat", "isr"
    )

    private val ignoreWords = listOf("precio", "sub", "ahorro", "descuento", "cambio", "su cambio", "vuelto")

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    fun detect(visionText: Text): DetectionResult {
        val fullText = visionText.text

        val allCandidates = mutableListOf<ScoredCandidate>()
        allCandidates += findByKeywordsScored(fullText)
        allCandidates += findByGeometricAlignmentScored(visionText)
        allCandidates += findByPositionScored(visionText)
        allCandidates += findAmountInLastSectionScored(fullText)
        allCandidates += findLastAmountScored(fullText)

        if (allCandidates.isEmpty()) return DetectionResult(null, Confidence.NONE)

        // Apply "largest amount in bottom 30%" bonus
        val bottom30Max = largestAmountInBottom30Percent(fullText)
        val scored = allCandidates.map { c ->
            if (bottom30Max != null && c.amount == bottom30Max)
                c.copy(score = c.score + BONUS_LARGEST_IN_BOTTOM30)
            else c
        }

        // Arithmetic cross-check: Subtotal + Tax ≈ Total → VERIFIED
        val confidencePairs = scored.map { it.amount to scoreToConfidence(it.score) }
        val arithmeticResult = verifyArithmetically(fullText, confidencePairs)
        if (arithmeticResult != null) return arithmeticResult

        val best = scored.maxByOrNull { it.score }!!
        return DetectionResult(best.amount, scoreToConfidence(best.score))
    }

    /** Text-only path used in unit tests (no ML Kit vision objects). */
    fun detectFromText(text: String): DetectionResult {
        val allCandidates = mutableListOf<ScoredCandidate>()
        allCandidates += findByKeywordsScored(text)
        allCandidates += findAmountInLastSectionScored(text)
        allCandidates += findLastAmountScored(text)

        if (allCandidates.isEmpty()) return DetectionResult(null, Confidence.NONE)

        val bottom30Max = largestAmountInBottom30Percent(text)
        val scored = allCandidates.map { c ->
            if (bottom30Max != null && c.amount == bottom30Max)
                c.copy(score = c.score + BONUS_LARGEST_IN_BOTTOM30)
            else c
        }

        val confidencePairs = scored.map { it.amount to scoreToConfidence(it.score) }
        val arithmeticResult = verifyArithmetically(text, confidencePairs)
        if (arithmeticResult != null) return arithmeticResult

        val best = scored.maxByOrNull { it.score }!!
        return DetectionResult(best.amount, scoreToConfidence(best.score))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Score → Confidence mapping
    // ─────────────────────────────────────────────────────────────────────────

    private fun scoreToConfidence(score: Int): Confidence = when {
        score >= 70 -> Confidence.HIGH
        score >= 40 -> Confidence.MEDIUM
        score >= 20 -> Confidence.LOW
        else        -> Confidence.NONE
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Arithmetic verification (unchanged logic)
    // ─────────────────────────────────────────────────────────────────────────

    private fun verifyArithmetically(
        text: String,
        candidates: List<Pair<Double, Confidence>>
    ): DetectionResult? {
        val subtotal = extractAmountByKeyword(text, subtotalKeywords) ?: return null
        val tax = extractAmountByKeyword(text, taxKeywords) ?: return null
        val expected = subtotal + tax
        val tolerance = maxOf(0.02, expected * 0.01)
        val match = candidates.firstOrNull { (amount, _) ->
            Math.abs(amount - expected) <= tolerance
        }
        return match?.let { DetectionResult(it.first, Confidence.VERIFIED) }
    }

    private fun extractAmountByKeyword(text: String, keywords: List<String>): Double? {
        val lines = text.split("\n")
        for (keyword in keywords) {
            for (line in lines) {
                if (!line.contains(keyword, ignoreCase = true)) continue
                if (totalKeywords.any { line.contains(it, ignoreCase = true) }) continue
                val amount = lastValidAmountOnLine(line)
                if (amount != null) return amount
            }
        }
        return null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Detection layers — all return List<ScoredCandidate>
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Layer 1: keyword search in plain text.
     * Collects ALL keyword matches (no early return) so the scorer can pick the best.
     * Currency symbol in the match grants +BONUS_CURRENCY_SYMBOL.
     */
    private fun findByKeywordsScored(text: String): List<ScoredCandidate> {
        val reversedLines = text.split("\n").asReversed()
        val results = mutableListOf<ScoredCandidate>()

        for (keyword in totalKeywords) {
            for ((i, line) in reversedLines.withIndex()) {
                if (!line.contains(keyword, ignoreCase = true)) continue
                if (ignoreWords.any { line.contains(it, ignoreCase = true) }) continue

                // Same line (prefer amount after the keyword)
                findScoredAmountInLine(line, keyword, SCORE_KEYWORD_MATCH)
                    ?.let { results.add(it) }

                // Lines below the keyword in the receipt (lower index in reversed list)
                val startBelow = maxOf(0, i - 7)
                for (j in (i - 1 downTo startBelow)) {
                    lastScoredCandidateOnLine(reversedLines[j], SCORE_KEYWORD_MATCH - 5)
                        ?.let { results.add(it) }
                }

                // Lines above the keyword (fallback)
                val endAbove = minOf(reversedLines.size - 1, i + 4)
                for (j in (i + 1..endAbove)) {
                    lastScoredCandidateOnLine(reversedLines[j], SCORE_KEYWORD_MATCH - 10)
                        ?.let { results.add(it) }
                }
            }
        }
        return results
    }

    /**
     * Layer 2: geometric alignment — amounts that share the same horizontal row as a keyword
     * in the ML Kit TextBlock layout. Highest base score because spatial alignment is the
     * strongest signal for a two-column receipt layout (keyword left | amount right).
     */
    private fun findByGeometricAlignmentScored(visionText: Text): List<ScoredCandidate> {
        val allLines = visionText.textBlocks.flatMap { it.lines }
        if (allLines.isEmpty()) return emptyList()
        val results = mutableListOf<ScoredCandidate>()

        for (keyword in totalKeywords) {
            for (keywordLine in allLines) {
                val lineLower = keywordLine.text.lowercase()
                if (!lineLower.contains(keyword)) continue
                if (ignoreWords.any { lineLower.contains(it) }) continue

                val keywordBox = keywordLine.boundingBox ?: continue
                val keywordCenterY = (keywordBox.top + keywordBox.bottom) / 2.0
                val lineHeight = (keywordBox.bottom - keywordBox.top).coerceAtLeast(1)
                val verticalTolerance = lineHeight * 1.2

                // 1. Amount on the same TextLine, after the keyword
                val keywordIdx = keywordLine.text.indexOf(keyword, ignoreCase = true)
                val searchFrom = if (keywordIdx >= 0) keywordLine.text.substring(keywordIdx)
                                 else keywordLine.text
                amountRegex.findAll(searchFrom)
                    .filter { !looksLikeNonMonetary(it.value, keywordLine.text) }
                    .mapNotNull { match ->
                        parseAmount(match.groupValues[2])?.let { amount ->
                            val currencyBonus = if (match.groupValues[1].isNotBlank()) BONUS_CURRENCY_SYMBOL else 0
                            ScoredCandidate(amount, SCORE_GEOMETRIC_ALIGN + currencyBonus)
                        }
                    }
                    .lastOrNull()?.let { results.add(it) }

                // 2. Amounts in different TextLines on the same horizontal row, to the right
                allLines
                    .filter { candidateLine ->
                        if (candidateLine === keywordLine) return@filter false
                        val box = candidateLine.boundingBox ?: return@filter false
                        val centerY = (box.top + box.bottom) / 2.0
                        Math.abs(centerY - keywordCenterY) <= verticalTolerance &&
                            box.left >= keywordBox.left
                    }
                    .sortedByDescending { it.boundingBox?.left ?: 0 }
                    .forEach { line ->
                        lastScoredCandidateOnLine(line.text, SCORE_GEOMETRIC_ALIGN)
                            ?.let { results.add(it) }
                    }
            }
        }
        return results
    }

    /**
     * Layer 3: position-based — amounts in the bottom 40% of the image.
     * Blocks that also contain a total keyword get an extra bonus.
     */
    private fun findByPositionScored(visionText: Text): List<ScoredCandidate> {
        val blocks = visionText.textBlocks
        if (blocks.isEmpty()) return emptyList()

        val maxHeight = blocks.mapNotNull { it.boundingBox?.bottom }.maxOrNull() ?: 1000
        val threshold = (maxHeight * 0.60).toInt()
        val results = mutableListOf<ScoredCandidate>()

        blocks.filter { (it.boundingBox?.top ?: 0) > threshold }.forEach { block ->
            val blockLower = block.text.lowercase()
            val keywordBonus = if (totalKeywords.any { blockLower.contains(it) }) BONUS_KEYWORD_IN_BLOCK else 0
            amountRegex.findAll(block.text).forEach { match ->
                if (!looksLikeNonMonetary(match.value, block.text)) {
                    parseAmount(match.groupValues[2])?.let { amount ->
                        val currencyBonus = if (match.groupValues[1].isNotBlank()) BONUS_CURRENCY_SYMBOL else 0
                        results.add(ScoredCandidate(amount, SCORE_POSITION_BASED + keywordBonus + currencyBonus))
                    }
                }
            }
        }
        return results
    }

    /** Layer 4: amounts in the bottom half of text lines. */
    private fun findAmountInLastSectionScored(text: String): List<ScoredCandidate> {
        val lines = text.split("\n")
        val lastHalf = lines.takeLast(maxOf(lines.size / 2, 3))
        val results = mutableListOf<ScoredCandidate>()
        lastHalf.forEach { line ->
            amountRegex.findAll(line).forEach { match ->
                if (!looksLikeNonMonetary(match.value, line)) {
                    parseAmount(match.groupValues[2])?.let { amount ->
                        val currencyBonus = if (match.groupValues[1].isNotBlank()) BONUS_CURRENCY_SYMBOL else 0
                        results.add(ScoredCandidate(amount, SCORE_LAST_SECTION + currencyBonus))
                    }
                }
            }
        }
        return results
    }

    /** Layer 5: final fallback — every amount found in the full text. */
    private fun findLastAmountScored(text: String): List<ScoredCandidate> {
        val results = mutableListOf<ScoredCandidate>()
        text.split("\n").forEach { line ->
            amountRegex.findAll(line).forEach { match ->
                if (!looksLikeNonMonetary(match.value, line)) {
                    parseAmount(match.groupValues[2])?.let { amount ->
                        val currencyBonus = if (match.groupValues[1].isNotBlank()) BONUS_CURRENCY_SYMBOL else 0
                        results.add(ScoredCandidate(amount, SCORE_LAST_AMOUNT + currencyBonus))
                    }
                }
            }
        }
        return results
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Low-level helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the last valid scored amount on [line], preferring the portion after [keyword].
     * Falls back to the full line if nothing is found after the keyword.
     */
    private fun findScoredAmountInLine(line: String, keyword: String, baseScore: Int): ScoredCandidate? {
        val keywordIndex = line.indexOf(keyword, ignoreCase = true)
        if (keywordIndex >= 0) {
            val afterKeyword = line.substring(keywordIndex)
            val match = amountRegex.findAll(afterKeyword)
                .filter { !looksLikeNonMonetary(it.value, line) }
                .lastOrNull { parseAmount(it.groupValues[2]) != null }
            if (match != null) {
                val amount = parseAmount(match.groupValues[2])!!
                val currencyBonus = if (match.groupValues[1].isNotBlank()) BONUS_CURRENCY_SYMBOL else 0
                return ScoredCandidate(amount, baseScore + currencyBonus)
            }
        }
        // Fallback: search the full line (e.g. "$50.00  TOTAL")
        return lastScoredCandidateOnLine(line, baseScore - 5)
    }

    /**
     * Returns the rightmost valid amount on [line] as a [ScoredCandidate].
     * Currency symbol in the match adds [BONUS_CURRENCY_SYMBOL] to [baseScore].
     */
    private fun lastScoredCandidateOnLine(line: String, baseScore: Int): ScoredCandidate? {
        for (match in amountRegex.findAll(line).toList().asReversed()) {
            if (!looksLikeNonMonetary(match.value, line)) {
                val amount = parseAmount(match.groupValues[2]) ?: continue
                val currencyBonus = if (match.groupValues[1].isNotBlank()) BONUS_CURRENCY_SYMBOL else 0
                return ScoredCandidate(amount, baseScore + currencyBonus)
            }
        }
        return null
    }

    /**
     * Returns the largest valid monetary amount found in the bottom 30% of [text] lines.
     * Used to grant the [BONUS_LARGEST_IN_BOTTOM30] bonus.
     */
    private fun largestAmountInBottom30Percent(text: String): Double? {
        val lines = text.split("\n")
        val startIdx = (lines.size * 0.70).toInt()
        return lines.drop(startIdx).flatMap { line ->
            amountRegex.findAll(line)
                .filter { !looksLikeNonMonetary(it.value, line) }
                .mapNotNull { parseAmount(it.groupValues[2]) }
        }.maxOrNull()
    }

    /** Used only by [extractAmountByKeyword] → [verifyArithmetically]. */
    private fun lastValidAmountOnLine(line: String): Double? {
        for (match in amountRegex.findAll(line).toList().asReversed()) {
            if (!looksLikeNonMonetary(match.value, line)) {
                val parsed = parseAmount(match.groupValues[2])
                if (parsed != null) return parsed
            }
        }
        return null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Non-monetary filters
    // ─────────────────────────────────────────────────────────────────────────

    private val idKeywords = listOf(
        "nit", "rfc", "ruc", "factura", "orden", "ticket", "folio",
        "autorizacion", "ref", "tarjeta", "terminal", "cajero", "aprobacion", "cuenta",
        // Postal codes (C.P., ZIP) — numbers near these labels are addresses, not amounts
        "c.p.", "c.p", "cod. postal", "codigo postal", "zip"
    )

    private fun looksLikeNonMonetary(matchStr: String, contextStr: String = ""): Boolean {
        if (phonePatterns.any { it.containsMatchIn(matchStr) }) return true
        if (datePatterns.any { it.containsMatchIn(matchStr) }) return true

        // Exclude values used as percentage rates (e.g. "16%", "12.5% IVA")
        if (Regex(Pattern.quote(matchStr.trim()) + """\s*%""").containsMatchIn(contextStr)) return true

        val cleanNum = matchStr.replace("[^0-9]".toRegex(), "")
        if (cleanNum.length >= 6 && !matchStr.contains(".") && !matchStr.contains(",")) return true

        if (contextStr.isNotBlank()) {
            val lowerContext = contextStr.lowercase()
            val hasIdKeyword = idKeywords.any { lowerContext.contains(it) }
            val hasTotalKeyword = totalKeywords.any { lowerContext.contains(it) }
            if (hasIdKeyword && !hasTotalKeyword) return true
        }

        return false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Amount parsing
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseAmount(amountStr: String): Double? {
        val clean = amountStr.replace("[^0-9,.]".toRegex(), "")
        if (clean.isEmpty()) return null

        // Try each locale in priority order (dot-decimal markets first, then comma-decimal).
        // ParsePosition ensures the entire string is consumed — no silent partial parses.
        val locales = listOf(
            Locale.US,                  // 1,250.50  — USD receipts
            Locale("es", "MX"),         // same conventions as US for MXN
            Locale("es", "GT"),         // GTQ (Guatemala)
            Locale("es", "HN"),         // HNL (Honduras)
            Locale.GERMANY,             // 1.250,50  — European/some Latin American formats
        )
        for (locale in locales) {
            try {
                val nf = NumberFormat.getNumberInstance(locale)
                val pos = ParsePosition(0)
                val result = nf.parse(clean, pos) ?: continue
                if (pos.index == clean.length) {          // full string consumed
                    val value = result.toDouble()
                    if (isValidAmount(value)) return value
                }
            } catch (_: Exception) { /* try next locale */ }
        }
        return null
    }

    private fun isValidAmount(amount: Double): Boolean {
        return amount in 0.01..999999.99
    }
}
