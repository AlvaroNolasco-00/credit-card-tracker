package com.alvaronolasco.creditcardtracker.ocr

import android.content.Context
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
}

class AmountDetector {
    
    data class DetectionResult(
        val amount: Double?,
        val confidence: Confidence
    )

    private val totalKeywords = listOf(
        "total", "total a pagar", "monto total", "importe total",
        "gran total", "neto", "amount due", "balance due",
        "monto", "importe", "a pagar", "cobro", "cargo total",
        "amount", "net total", "grand total", "sum", "due", "pay",
        "compra por", "consumo", "pagado", "pago", "por usd", "por mxn", "usd", "mxn", "eur"
    )

    // Regex to match currency amounts:
    // Support symbols: $, Q, L, €, etc., and codes like USD, MXN
    // Support thousands separators: 1,234.56 or 1.234,56
    // Support no decimals: $100
    private val amountRegex = Regex(
        """([$€£¥₣₹]|Q|L|HNL|GTQ|USD|MXN|EUR)?\s*(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2})?|\d+(?:[.,]\d{2})?)\b""",
        RegexOption.IGNORE_CASE
    )

    private val phonePatterns = listOf(
        Regex("""(?:\+?\d{1,3}[-.\s]?)?\(?\d{2,4}\)?[-.\s]?\d{3,4}[-.\s]?\d{4}"""),  // Phone formats
        Regex("""\d{7,}"""),  // 7+ consecutive digits (phone-like)
    )

    private val datePatterns = listOf(
        Regex("""\d{4}[-/]\d{1,2}[-/]\d{1,2}"""),   // YYYY-MM-DD
        Regex("""\d{1,2}[-/]\d{1,2}[-/]\d{2,4}"""), // DD/MM/YYYY
    )

    fun detect(visionText: Text): DetectionResult {
        // Layer 1: Keyword Match (Highest confidence)
        val keywordResult = findByKeywords(visionText.text)
        if (keywordResult != null) return DetectionResult(keywordResult, Confidence.HIGH)

        // Layer 2: Positional Analysis (Medium confidence)
        // We look for amounts in the bottom quarter of the image
        val positionalResult = findByPosition(visionText)
        if (positionalResult != null) return DetectionResult(positionalResult, Confidence.MEDIUM)

        // Layer 3: Last Section Analysis (Medium confidence)
        // If positional blocks fail, try searching in the last lines of the text
        val lastSectionResult = findAmountInLastSection(visionText.text)
        if (lastSectionResult != null) return DetectionResult(lastSectionResult, Confidence.MEDIUM)

        // Layer 4: Max Amount Heuristic (Low confidence)
        val maxAmount = findMaxAmount(visionText.text)
        if (maxAmount != null) return DetectionResult(maxAmount, Confidence.LOW)

        return DetectionResult(null, Confidence.NONE)
    }

    // Helper for testing without ML Kit vision objects
    fun detectFromText(text: String): DetectionResult {
        val keywordResult = findByKeywords(text)
        if (keywordResult != null) return DetectionResult(keywordResult, Confidence.HIGH)

        // Layer 2 (text-only): Look for amounts in the last section of the text
        val lastSectionResult = findAmountInLastSection(text)
        if (lastSectionResult != null) return DetectionResult(lastSectionResult, Confidence.MEDIUM)

        val maxAmount = findMaxAmount(text)
        if (maxAmount != null) return DetectionResult(maxAmount, Confidence.LOW)

        return DetectionResult(null, Confidence.NONE)
    }

    private fun findByKeywords(text: String): Double? {
        val lines = text.split("\n")
        val reversedLines = lines.asReversed()
        val ignoreWords = listOf("precio", "sub", "ahorro", "descuento", "cambio", "su cambio", "vuelto")

        // Search bottom-up: last occurrence of a keyword is more likely the final total
        totalKeywords.forEach { keyword ->
            reversedLines.forEachIndexed { i, line ->
                if (line.contains(keyword, ignoreCase = true)) {
                    // Skip if the line has words indicating it's not the final total (like 'precio total' or 'sub total')
                    if (ignoreWords.any { line.contains(it, ignoreCase = true) } && !keyword.contains("sub", ignoreCase = true)) {
                        return@forEachIndexed // Continue to the next line in reversedLines
                    }

                    // Try to find amount on the same line
                    val substring = line.substring(line.indexOf(keyword, ignoreCase = true))
                    val match = amountRegex.find(substring)
                    
                    if (match != null && !looksLikeNonMonetary(match.value)) {
                        parseAmount(match.groupValues[2])?.let { 
                            if (it > 0.0) return it 
                        }
                    }

                    // If not found on the same line, check the next few lines (which are visually "below" on the receipt)
                    // The "below" lines are the previous indices in the reversed list.
                    // Loop from i - 1 down to i - 7 to catch amounts pushed further down by MLKit grouping
                    val startSearch = maxOf(0, i - 7)
                    for (j in (i - 1 downTo startSearch)) {
                        val nextLine = reversedLines[j]
                        val nextMatches = amountRegex.findAll(nextLine)
                        for (nextMatch in nextMatches) {
                            if (!looksLikeNonMonetary(nextMatch.value)) {
                                parseAmount(nextMatch.groupValues[2])?.let {
                                    if (it > 0.0) return it
                                }
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun findByPosition(visionText: Text): Double? {
        val blocks = visionText.textBlocks
        if (blocks.isEmpty()) return null

        // Find image height estimate from blocks
        val maxHeight = blocks.mapNotNull { it.boundingBox?.bottom }.maxOrNull() ?: 1000
        // Total amount is usually in the bottom quarter of the receipt
        val bottomQuarterThreshold = (maxHeight * 0.75).toInt()

        val candidates = mutableListOf<Double>()
        blocks.filter { (it.boundingBox?.top ?: 0) > bottomQuarterThreshold }.forEach { block ->
            amountRegex.findAll(block.text).forEach { match ->
                if (!looksLikeNonMonetary(match.value)) {
                    parseAmount(match.groupValues[2])?.let { candidates.add(it) }
                }
            }
        }
        
        return candidates.maxOrNull()
    }

    private fun findMaxAmount(text: String): Double? {
        val lines = text.split("\n")
        val halfPoint = lines.size / 2
        
        // Prefer amounts from the bottom half of the text
        val bottomHalfText = lines.drop(halfPoint).joinToString("\n")
        val bottomResult = amountRegex.findAll(bottomHalfText)
            .filter { !looksLikeNonMonetary(it.value) }
            .mapNotNull { parseAmount(it.groupValues[2]) }
            .filter { it in 0.01..999999.99 } // Sanity check
            .maxOrNull()
        
        if (bottomResult != null) return bottomResult
        
        // Fallback to full text
        return amountRegex.findAll(text)
            .filter { !looksLikeNonMonetary(it.value) }
            .mapNotNull { parseAmount(it.groupValues[2]) }
            .filter { it in 0.01..999999.99 }
            .maxOrNull()
    }

    private fun findAmountInLastSection(text: String): Double? {
        val lines = text.split("\n")
        val lastThird = lines.takeLast(maxOf(lines.size / 3, 1))
        val candidates = mutableListOf<Double>()
        lastThird.forEach { line ->
            amountRegex.findAll(line).forEach { match ->
                if (!looksLikeNonMonetary(match.value)) {
                    parseAmount(match.groupValues[2])?.let { candidates.add(it) }
                }
            }
        }
        return candidates.maxOrNull()
    }

    private fun looksLikeNonMonetary(text: String): Boolean {
        // Check if the match is part of a phone number pattern
        if (phonePatterns.any { it.containsMatchIn(text) }) return true
        // Check if it's a date
        if (datePatterns.any { it.containsMatchIn(text) }) return true
        
        return false
    }

    private fun parseAmount(amountStr: String): Double? {
        // Remove everything except numbers, dots and commas
        var clean = amountStr.replace("[^0-9,.]".toRegex(), "")
        
        if (clean.isEmpty()) return null

        // If it has both dots and commas
        if (clean.contains(".") && clean.contains(",")) {
            val lastDot = clean.lastIndexOf(".")
            val lastComma = clean.lastIndexOf(",")
            if (lastDot > lastComma) {
                // Comma is thousands, dot is decimal (e.g., 1,250.50)
                clean = clean.replace(",", "")
            } else {
                // Dot is thousands, comma is decimal (e.g., 1.250,50)
                clean = clean.replace(".", "").replace(",", ".")
            }
        } else {
            // Only one type of separator (or none)
            val lastSeparatorIndex = if (clean.contains(",")) clean.lastIndexOf(",") else clean.lastIndexOf(".")
            
            if (lastSeparatorIndex != -1) {
                val charsAfter = clean.length - lastSeparatorIndex - 1
                // Most currency formats have 2 decimal places. 
                // If there are exactly 3 digits after a single separator, it's ambiguous.
                // In context of OCR of tickets, if it's a dot and 2 digits, it's decimal.
                // If it's a dot and 3 digits (e.g., 1.500), it's likely thousands in some locales, 
                // but without context we'll assume decimal unless there are more thousands separators.
                
                if (charsAfter == 3 && clean.count { it == ',' || it == '.' } > 1) {
                    // Multiple separators of same type followed by 3 digits -> thousands
                    clean = clean.replace(",", "").replace(".", "")
                } else if (charsAfter <= 2) {
                    // 1 or 2 digits after -> decimal
                    clean = clean.replace(",", ".")
                } else {
                    // More than 3 or specifically 3 but no other separators -> thousands (remove it)
                    clean = clean.replace(",", "").replace(".", "")
                }
            }
        }
        
        return clean.toDoubleOrNull()
    }
}

