package com.alvaronolasco.creditcardtracker.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

enum class Confidence {
    HIGH, MEDIUM, LOW, NONE
}

class OcrProcessor(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    data class OcrResult(
        val fullText: String,
        val detectedAmount: Double?,
        val confidence: Confidence = Confidence.NONE
    )

    suspend fun processImage(uri: Uri): OcrResult {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val visionText = recognizer.process(image).await()
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
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(image).await()
            val fullText = visionText.text
            val detector = AmountDetector()
            val detection = detector.detect(visionText)
            OcrResult(fullText, detection.amount, detection.confidence)
        } catch (e: Exception) {
            OcrResult("", null, Confidence.NONE)
        }
    }
}

class AmountDetector {

    data class DetectionResult(
        val amount: Double?,
        val confidence: Confidence
    )

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

    // Fix #1: unified pattern — \d+ (no 3-digit cap) handles amounts like 12500.00 correctly.
    // The old two-alternative approach caused \d{1,3} to match "125" from "12500.00",
    // making the engine never try the \d+ alternative.
    // \d+(?:[.,\s]+\d{3})* handles thousands separators including spaces; (?:[.,]\d{1,2})? handles 1-2 decimal places.
    private val amountRegex = Regex(
        """([$€£¥₣₹]|Q|L|HNL|GTQ|USD|MXN|EUR)?\s*(\d+(?:[.,\s]+\d{3})*(?:[.,]\d{1,2})?)(?:\s*(?:USD|MXN|EUR|GTQ|HNL)\b)?""",
        RegexOption.IGNORE_CASE
    )

    // Fix #2: removed overly aggressive \d{7,} pattern that was filtering large valid amounts.
    // A proper phone number requires structural separators between digit groups.
    private val phonePatterns = listOf(
        Regex("""(?:\+?\d{1,3}[-.\s])?\(?\d{2,4}\)?[-.\s]\d{3,4}[-.\s]\d{4}"""),
    )

    private val datePatterns = listOf(
        Regex("""\d{4}[-/]\d{1,2}[-/]\d{1,2}"""),   // YYYY-MM-DD
        Regex("""\d{1,2}[-/]\d{1,2}[-/]\d{2,4}"""), // DD/MM/YYYY
    )

    // Lines containing these words near a keyword indicate it is NOT the grand total
    private val ignoreWords = listOf("precio", "sub", "ahorro", "descuento", "cambio", "su cambio", "vuelto")

    fun detect(visionText: Text): DetectionResult {
        // Layer 1: Keyword Match (Highest confidence)
        val keywordResult = findByKeywords(visionText.text)
        if (keywordResult != null) return DetectionResult(keywordResult, Confidence.HIGH)

        // Layer 2: Positional Analysis — bottom 40% of image with keyword preference
        val positionalResult = findByPosition(visionText)
        if (positionalResult != null) return DetectionResult(positionalResult, Confidence.MEDIUM)

        // Layer 3: Last half of text
        val lastSectionResult = findAmountInLastSection(visionText.text)
        if (lastSectionResult != null) return DetectionResult(lastSectionResult, Confidence.MEDIUM)

        // Layer 4: Last Amount Heuristic (Low confidence)
        val fallbackAmount = findLastAmount(visionText.text)
        if (fallbackAmount != null) return DetectionResult(fallbackAmount, Confidence.LOW)

        return DetectionResult(null, Confidence.NONE)
    }

    // Helper for testing without ML Kit vision objects
    fun detectFromText(text: String): DetectionResult {
        val keywordResult = findByKeywords(text)
        if (keywordResult != null) return DetectionResult(keywordResult, Confidence.HIGH)

        val lastSectionResult = findAmountInLastSection(text)
        if (lastSectionResult != null) return DetectionResult(lastSectionResult, Confidence.MEDIUM)

        val fallbackAmount = findLastAmount(text)
        if (fallbackAmount != null) return DetectionResult(fallbackAmount, Confidence.LOW)

        return DetectionResult(null, Confidence.NONE)
    }

    private fun findByKeywords(text: String): Double? {
        val lines = text.split("\n")
        val reversedLines = lines.asReversed()

        totalKeywords.forEach { keyword ->
            reversedLines.forEachIndexed { i, line ->
                if (!line.contains(keyword, ignoreCase = true)) return@forEachIndexed

                // Skip lines that suggest this is a sub-total or price line, not the grand total
                if (ignoreWords.any { line.contains(it, ignoreCase = true) }) {
                    return@forEachIndexed
                }

                // Check same line: try from keyword position first, then full line as fallback
                // Fix #6: also search before the keyword on the same line
                val amountOnLine = findAmountInLine(line, keyword)
                if (amountOnLine != null) return amountOnLine

                // Fix #3: look below the keyword (lines after it in original = lower index in reversed)
                val startBelow = maxOf(0, i - 7)
                for (j in (i - 1 downTo startBelow)) {
                    val belowLine = reversedLines[j]
                    val found = lastValidAmountOnLine(belowLine)
                    if (found != null) return found
                }

                // Fix #3: look above the keyword as fallback (lines before it in original = higher index in reversed)
                val endAbove = minOf(reversedLines.size - 1, i + 4)
                for (j in (i + 1..endAbove)) {
                    val aboveLine = reversedLines[j]
                    val found = lastValidAmountOnLine(aboveLine)
                    if (found != null) return found
                }
            }
        }
        return null
    }

    // Fix #6: search for amount starting from after the keyword, then fall back to the full line
    private fun findAmountInLine(line: String, keyword: String): Double? {
        val keywordIndex = line.indexOf(keyword, ignoreCase = true)

        // Prefer amount to the right of the keyword
        if (keywordIndex >= 0) {
            val afterKeyword = line.substring(keywordIndex)
            val rightAmount = lastValidAmountOnLine(afterKeyword)
            if (rightAmount != null) return rightAmount
        }

        // Fallback: amount anywhere on the same line (e.g., "$50.00  TOTAL")
        return lastValidAmountOnLine(line)
    }

    private fun lastValidAmountOnLine(line: String): Double? {
        val matches = amountRegex.findAll(line).toList()
        for (match in matches.asReversed()) {
            if (!looksLikeNonMonetary(match.value, line)) {
                val parsed = parseAmount(match.groupValues[2])
                if (parsed != null) return parsed
            }
        }
        return null
    }

    private fun findByPosition(visionText: Text): Double? {
        val blocks = visionText.textBlocks
        if (blocks.isEmpty()) return null

        val maxHeight = blocks.mapNotNull { it.boundingBox?.bottom }.maxOrNull() ?: 1000
        // Fix #4: expanded from bottom 25% to bottom 40% of the image
        val bottomSectionThreshold = (maxHeight * 0.60).toInt()

        data class Candidate(val amount: Double, val hasKeyword: Boolean, val yPos: Int)

        val candidates = mutableListOf<Candidate>()
        blocks.filter { (it.boundingBox?.top ?: 0) > bottomSectionThreshold }.forEach { block ->
            // Fix #5: track whether this block contains a keyword to prefer it over generic max
            val blockLower = block.text.lowercase()
            val hasKeyword = totalKeywords.any { blockLower.contains(it) }
            val yPos = block.boundingBox?.top ?: 0
            amountRegex.findAll(block.text).forEach { match ->
                if (!looksLikeNonMonetary(match.value, block.text)) {
                    parseAmount(match.groupValues[2])?.let { amount ->
                        candidates.add(Candidate(amount, hasKeyword, yPos))
                    }
                }
            }
        }

        if (candidates.isEmpty()) return null

        // Fix #5: prefer amounts in keyword-bearing blocks; among equal-priority pick the bottom-most
        val keywordCandidates = candidates.filter { it.hasKeyword }
        if (keywordCandidates.isNotEmpty()) return keywordCandidates.maxByOrNull { it.yPos }?.amount
        return candidates.maxByOrNull { it.yPos }?.amount
    }

    private fun findLastAmount(text: String): Double? {
        val lines = text.split("\n")
        val halfPoint = lines.size / 2

        val bottomHalfText = lines.drop(halfPoint).joinToString("\n")
        val bottomResult = bottomHalfText.split("\n")
            .flatMap { line -> amountRegex.findAll(line).map { it to line } }
            .filter { !looksLikeNonMonetary(it.first.value, it.second) }
            .mapNotNull { parseAmount(it.first.groupValues[2]) }
            .lastOrNull()

        if (bottomResult != null) return bottomResult

        return lines
            .flatMap { line -> amountRegex.findAll(line).map { it to line } }
            .filter { !looksLikeNonMonetary(it.first.value, it.second) }
            .mapNotNull { parseAmount(it.first.groupValues[2]) }
            .lastOrNull()
    }

    private fun findAmountInLastSection(text: String): Double? {
        val lines = text.split("\n")
        // Fix #8: extended from last 33% to last 50% for better coverage on long receipts
        val lastHalf = lines.takeLast(maxOf(lines.size / 2, 3))
        val candidates = mutableListOf<Double>()
        lastHalf.forEach { line ->
            amountRegex.findAll(line).forEach { match ->
                if (!looksLikeNonMonetary(match.value, line)) {
                    parseAmount(match.groupValues[2])?.let { candidates.add(it) }
                }
            }
        }
        return candidates.lastOrNull()
    }

    private val idKeywords = listOf(
        "nit", "rfc", "ruc", "factura", "orden", "ticket", "folio",
        "autorizacion", "ref", "tarjeta", "terminal", "cajero", "aprobacion", "cuenta"
    )

    private fun looksLikeNonMonetary(matchStr: String, contextStr: String = ""): Boolean {
        if (phonePatterns.any { it.containsMatchIn(matchStr) }) return true
        if (datePatterns.any { it.containsMatchIn(matchStr) }) return true

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

    private fun parseAmount(amountStr: String): Double? {
        var clean = amountStr.replace("[^0-9,.]".toRegex(), "")
        if (clean.isEmpty()) return null

        if (clean.contains(".") && clean.contains(",")) {
            val lastDot = clean.lastIndexOf(".")
            val lastComma = clean.lastIndexOf(",")
            if (lastDot > lastComma) {
                // Comma is thousands separator, dot is decimal (e.g., 1,250.50)
                clean = clean.replace(",", "")
            } else {
                // Dot is thousands separator, comma is decimal (e.g., 1.250,50)
                clean = clean.replace(".", "").replace(",", ".")
            }
        } else {
            val lastSeparatorIndex = if (clean.contains(",")) clean.lastIndexOf(",") else clean.lastIndexOf(".")
            if (lastSeparatorIndex != -1) {
                val charsAfter = clean.length - lastSeparatorIndex - 1
                if (charsAfter == 3 && clean.count { it == ',' || it == '.' } > 1) {
                    // Multiple separators followed by 3 digits → thousands only (e.g., 1,234,567)
                    clean = clean.replace(",", "").replace(".", "")
                } else if (charsAfter <= 2) {
                    // Fix #1: 1 or 2 digits after separator → decimal (was only accepting exactly 2)
                    clean = clean.replace(",", ".")
                } else {
                    // 3+ digits after single separator → treat as thousands
                    clean = clean.replace(",", "").replace(".", "")
                }
            }
        }

        val parsed = clean.toDoubleOrNull()
        return if (parsed != null && isValidAmount(parsed)) parsed else null
    }

    private fun isValidAmount(amount: Double): Boolean {
        return amount in 0.01..999999.99
    }
}
