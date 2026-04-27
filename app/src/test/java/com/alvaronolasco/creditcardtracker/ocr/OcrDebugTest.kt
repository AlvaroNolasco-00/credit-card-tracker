package com.alvaronolasco.creditcardtracker.ocr

import org.junit.Test

class OcrDebugTest {

    private val fullReceiptText = """
Transacción Exitosa
Te presentamos el comprobante de la Transacción realizada
Cliente
ALVARO GUANDIQUE NOLASCO
Número de tarjeta
**** **** **** 4399
Monto
$ 25.00
Comercio
WOMPI*PUMA EL TRINGULO SAN MIGUEL SV
Fecha
25/04/2026
""".trimIndent()

    private val croppedText = "Monto\n$ 25.00"

    @Test
    fun `debug full receipt detection`() {
        println("=== FULL RECEIPT TEXT ===")
        println(fullReceiptText)
        println()

        val result = AmountDetector().detectFromText(fullReceiptText)
        println("=== DETECTION RESULT ===")
        println("Amount: ${result.amount}")
        println("Confidence: ${result.confidence}")
        println()

        // Trace each layer
        println("=== LAYER-BY-LAYER ANALYSIS ===")

        // We need to understand which layer produces what
        // Let's print all amounts found in each line
        val lines = fullReceiptText.split("\n")
        println("Lines (${lines.size} total):")
        lines.forEachIndexed { idx, line ->
            println("  [$idx] '$line'")
        }
        println()

        // Check what amounts are found per line
        println("Amounts found per line (raw regex matches):")
        lines.forEachIndexed { idx, line ->
            val matches = Regex(
                """([$€£¥₣₹]|Q|L|HNL|GTQ|USD|MXN|EUR)?\s*(\d{4,}(?:[.,]\d{1,2})?|\d{1,3}(?:[.,\s]?\d{3})*(?:[.,]\d{1,2})?)(?:\s*(?:USD|MXN|EUR|GTQ|HNL)\b)?""",
                RegexOption.IGNORE_CASE
            ).findAll(line).map { it.value }.toList()
            if (matches.isNotEmpty()) {
                println("  [$idx] '$line' → matches: $matches")
            }
        }
        println()

        // Check bottom 30% lines
        val bottom30Threshold = (lines.size * 0.70).toInt()
        println("Bottom 30% starts at line index: $bottom30Threshold")
        println("Bottom 30% lines: ${lines.subList(bottom30Threshold, lines.size)}")
        println()

        // Check keyword matches
        val keywords = listOf("monto", "total", "importe", "a pagar", "amount")
        println("Keyword matches:")
        lines.forEachIndexed { idx, line ->
            keywords.forEach { kw ->
                if (line.contains(kw, ignoreCase = true)) {
                    println("  [$idx] '$line' contains keyword '$kw'")
                }
            }
        }
        println()

        // Check card number filtering
        println("Card number pattern check on '**** **** **** 4399':")
        val cardPattern = Regex("""\*{4}[\s*]*\d{4}""")
        println("  Matches card pattern: ${cardPattern.containsMatchIn("**** **** **** 4399")}")
        println()

        // Check date filtering
        println("Date pattern check on '25/04/2026':")
        val datePatterns = listOf(
            Regex("""\d{4}[-/]\d{1,2}[-/]\d{1,2}"""),
            Regex("""\d{1,2}[-/]\d{1,2}[-/]\d{2,4}"""),
        )
        datePatterns.forEachIndexed { idx, pattern ->
            println("  Pattern $idx matches: ${pattern.containsMatchIn("25/04/2026")}")
        }
        println()

        // Check what "2026" looks like as an amount
        println("Does '2026' match amount regex?")
        val amountRegex = Regex(
            """([$€£¥₣₹]|Q|L|HNL|GTQ|USD|MXN|EUR)?\s*(\d{4,}(?:[.,]\d{1,2})?|\d{1,3}(?:[.,\s]?\d{3})*(?:[.,]\d{1,2})?)(?:\s*(?:USD|MXN|EUR|GTQ|HNL)\b)?""",
            RegexOption.IGNORE_CASE
        )
        val matches2026 = amountRegex.findAll("25/04/2026").toList()
        matches2026.forEach { match ->
            println("  Match: '${match.value}', groups: ${match.groupValues}")
        }
        println()

        // Check OCR error correction on date line
        println("OCR error correction on '25/04/2026':")
        val corrected = Regex("""[0-9OoIlSBZ]+(?:[.,][0-9OoIlSBZ]+)*""").replace("25/04/2026") { match ->
            if (match.value.any { it.isDigit() }) {
                match.value
                    .replace(Regex("""[Oo]"""), "0")
                    .replace(Regex("""[lI]"""), "1")
                    .replace(Regex("""[S]"""), "5")
                    .replace(Regex("""[B]"""), "8")
                    .replace(Regex("""[Z]"""), "2")
            } else {
                match.value
            }
        }
        println("  Corrected: '$corrected'")
        println()
    }

    @Test
    fun `debug cropped text detection`() {
        println("=== CROPPED TEXT ===")
        println("'$croppedText'")
        println()

        val result = AmountDetector().detectFromText(croppedText)
        println("=== DETECTION RESULT ===")
        println("Amount: ${result.amount}")
        println("Confidence: ${result.confidence}")
        println()

        // Analyze why
        val lines = croppedText.split("\n")
        println("Lines: $lines")
        println("Last section (last 50%): ${lines.takeLast(maxOf(lines.size / 2, 3))}")
        println()

        // Check keyword matching
        println("Keyword 'monto' in lines:")
        lines.forEachIndexed { idx, line ->
            val normalized = line
                .replace("0", "O", ignoreCase = true)
                .replace("1", "l", ignoreCase = true)
                .replace("5", "S", ignoreCase = true)
            println("  [$idx] '$line' → normalized: '$normalized'")
            println("      contains 'monto': ${normalized.contains("monto", ignoreCase = true)}")
        }
        println()

        // Check amount regex on "$ 25.00"
        val amountRegex = Regex(
            """([$€£¥₣₹]|Q|L|HNL|GTQ|USD|MXN|EUR)?\s*(\d{4,}(?:[.,]\d{1,2})?|\d{1,3}(?:[.,\s]?\d{3})*(?:[.,]\d{1,2})?)(?:\s*(?:USD|MXN|EUR|GTQ|HNL)\b)?""",
            RegexOption.IGNORE_CASE
        )
        println("Amount regex matches on '\$ 25.00':")
        val matches = amountRegex.findAll("$ 25.00").toList()
        if (matches.isEmpty()) {
            println("  NO MATCHES!")
        } else {
            matches.forEach { match ->
                println("  Match: '${match.value}', groups: ${match.groupValues}")
            }
        }
        println()

        // Check if "25" alone matches (without currency)
        println("Amount regex matches on '25.00':")
        val matches2 = amountRegex.findAll("25.00").toList()
        matches2.forEach { match ->
            println("  Match: '${match.value}', groups: ${match.groupValues}")
        }
        println()

        // Check last section scoring
        println("Last section analysis:")
        val lastHalf = lines.takeLast(maxOf(lines.size / 2, 3))
        lastHalf.forEach { line ->
            val lineMatches = amountRegex.findAll(line).toList()
            println("  Line '$line' → ${lineMatches.size} matches")
            lineMatches.forEach { match ->
                println("    '${match.value}' → groups: ${match.groupValues}")
            }
        }
    }

    @Test
    fun `debug where does 6 come from`() {
        println("=== INVESTIGATING '6' DETECTION ===")
        println()

        // Check if "6" appears anywhere that could be matched
        val text = fullReceiptText

        // Search for standalone "6" or numbers containing 6
        val numberRegex = Regex("""\d+""")
        println("All numbers in text:")
        numberRegex.findAll(text).forEach { match ->
            println("  '${match.value}' at position ${match.range}")
        }
        println()

        // Check amount regex on each line for single-digit matches
        val amountRegex = Regex(
            """([$€£¥₣₹]|Q|L|HNL|GTQ|USD|MXN|EUR)?\s*(\d{4,}(?:[.,]\d{1,2})?|\d{1,3}(?:[.,\s]?\d{3})*(?:[.,]\d{1,2})?)(?:\s*(?:USD|MXN|EUR|GTQ|HNL)\b)?""",
            RegexOption.IGNORE_CASE
        )

        println("Amount regex matches per line:")
        text.split("\n").forEachIndexed { idx, line ->
            val matches = amountRegex.findAll(line).toList()
            if (matches.isNotEmpty()) {
                println("  [$idx] '$line':")
                matches.forEach { match ->
                    println("    Match: '${match.value}' → groups: ${match.groupValues}")
                }
            }
        }
        println()

        // Check if "6" could come from OCR error correction
        // Look for characters that could become "6" after correction
        println("Characters that could produce '6' after OCR correction:")
        println("  (None - the correction map is: O→0, l/I→1, S→5, B→8, Z→2)")
        println("  '6' must come from an actual '6' in the text")
        println()

        // Check "2026" specifically
        println("Checking '2026' parsing:")
        val parseResult = try {
            java.text.NumberFormat.getNumberInstance(java.util.Locale.US).parse("2026")?.toDouble()
        } catch (e: Exception) {
            null
        }
        println("  Parsed as: $parseResult")
        println("  Valid amount (0.01-999999.99): ${parseResult != null && parseResult in 0.01..999999.99}")
        println()

        // Check if date context filtering applies to "2026" when on its own line
        println("Date context filtering for '25/04/2026' line:")
        val datePatterns = listOf(
            Regex("""\d{4}[-/]\d{1,2}[-/]\d{1,2}"""),
            Regex("""\d{1,2}[-/]\d{1,2}[-/]\d{2,4}"""),
        )
        val hasDateContext = datePatterns.any { it.containsMatchIn("25/04/2026") }
        println("  Has date context: $hasDateContext")
        println("  This means '2026' on this line would be filtered as non-monetary")
        println()

        // But what if ML Kit splits the date into separate lines?
        println("HYPOTHESIS: ML Kit might split '25/04/2026' into separate text blocks")
        println("  If '2026' appears on its OWN line (without '/' characters),")
        println("  then datePatterns won't match and it won't be filtered!")
        println()

        // Test with "2026" alone
        println("Testing '2026' alone (no date context):")
        val testResult = AmountDetector().detectFromText("Fecha\n2026")
        println("  Result: amount=${testResult.amount}, confidence=${testResult.confidence}")
        println()

        // Test with full date on same line
        println("Testing '25/04/2026' on same line:")
        val testResult2 = AmountDetector().detectFromText("Fecha\n25/04/2026")
        println("  Result: amount=${testResult2.amount}, confidence=${testResult2.confidence}")
    }

    @Test
    fun `debug card number 4399 detection`() {
        println("=== INVESTIGATING '4399' DETECTION ===")
        println()

        val text = "**** **** **** 4399"

        // Check card pattern
        val cardPatterns = listOf(
            Regex("""\d{4}[\s*-]*\d{4}[\s*-]*\d{4}[\s*-]*\d{4}"""),
            Regex("""\*{4}[\s*]*\d{4}"""),
        )

        println("Card pattern matches on '$text':")
        cardPatterns.forEachIndexed { idx, pattern ->
            println("  Pattern $idx: ${pattern.containsMatchIn(text)}")
        }
        println()

        // Check amount regex
        val amountRegex = Regex(
            """([$€£¥₣₹]|Q|L|HNL|GTQ|USD|MXN|EUR)?\s*(\d{4,}(?:[.,]\d{1,2})?|\d{1,3}(?:[.,\s]?\d{3})*(?:[.,]\d{1,2})?)(?:\s*(?:USD|MXN|EUR|GTQ|HNL)\b)?""",
            RegexOption.IGNORE_CASE
        )

        println("Amount regex matches on '$text':")
        val matches = amountRegex.findAll(text).toList()
        matches.forEach { match ->
            println("  Match: '${match.value}' → groups: ${match.groupValues}")
        }
        println()

        // Check looksLikeNonMonetary logic
        println("Non-monetary filter for '4399' in context '$text':")
        val hasCardContext = cardPatterns.any { it.containsMatchIn(text) }
        println("  Has card number context: $hasCardContext")
        println("  Would be filtered: $hasCardContext")
    }

    @Test
    fun `debug ML Kit split date scenario`() {
        println("=== HYPOTHESIS: ML Kit splits '25/04/2026' into separate lines ===")
        println()

        // Scenario: ML Kit reads date as three separate lines
        val splitDateText = """
Transacción Exitosa
Cliente
ALVARO GUANDIQUE NOLASCO
Número de tarjeta
**** **** **** 4399
Monto
$ 25.00
Comercio
WOMPI*PUMA
Fecha
25
04
2026
""".trimIndent()

        println("Text with split date:")
        println(splitDateText)
        println()

        val result = AmountDetector().detectFromText(splitDateText)
        println("=== DETECTION RESULT ===")
        println("Amount: ${result.amount}")
        println("Confidence: ${result.confidence}")
        println()

        // Check if "2026" is now detected (no date context on its own line)
        println("Line-by-line analysis:")
        splitDateText.split("\n").forEachIndexed { idx, line ->
            val amountRegex = Regex(
                """([$€£¥₣₹]|Q|L|HNL|GTQ|USD|MXN|EUR)?\s*(\d{4,}(?:[.,]\d{1,2})?|\d{1,3}(?:[.,\s]?\d{3})*(?:[.,]\d{1,2})?)(?:\s*(?:USD|MXN|EUR|GTQ|HNL)\b)?""",
                RegexOption.IGNORE_CASE
            )
            val datePatterns = listOf(
                Regex("""\d{4}[-/]\d{1,2}[-/]\d{1,2}"""),
                Regex("""\d{1,2}[-/]\d{1,2}[-/]\d{2,4}"""),
            )
            val cardPatterns = listOf(
                Regex("""\d{4}[\s*-]*\d{4}[\s*-]*\d{4}[\s*-]*\d{4}"""),
                Regex("""\*{4}[\s*]*\d{4}"""),
            )

            val amountMatches = amountRegex.findAll(line).toList()
            val hasDateContext = datePatterns.any { it.containsMatchIn(line) }
            val hasCardContext = cardPatterns.any { it.containsMatchIn(line) }

            if (amountMatches.isNotEmpty()) {
                println("  [$idx] '$line':")
                amountMatches.forEach { match ->
                    val wouldBeFiltered = hasDateContext || hasCardContext
                    println("    Match: '${match.value}' → dateCtx=$hasDateContext, cardCtx=$hasCardContext, filtered=$wouldBeFiltered")
                }
            }
        }
        println()

        // Check bottom 30% - where does 2026 fall?
        val lines = splitDateText.split("\n")
        val bottom30Start = (lines.size * 0.70).toInt()
        println("Bottom 30% starts at index: $bottom30Start")
        println("Lines in bottom 30%: ${lines.subList(bottom30Start, lines.size)}")
        println()

        // Check keyword proximity to 2026
        println("Keyword 'monto' position vs '2026' position:")
        val montoIdx = lines.indexOfFirst { it.contains("Monto", ignoreCase = true) }
        val yearIdx = lines.indexOf("2026")
        println("  'Monto' at index: $montoIdx")
        println("  '2026' at index: $yearIdx")
        println("  Distance: ${yearIdx - montoIdx} lines")
        println("  Within 7 lines below keyword: ${yearIdx > montoIdx && yearIdx - montoIdx <= 7}")
        println()

        // Check what score 2026 would get
        println("Scoring analysis for '2026':")
        println("  Layer 1 (findByKeywordsScored):")
        println("    - 'Monto' keyword found, looks 7 lines below")
        println("    - '2026' is ${yearIdx - montoIdx} lines below 'Monto'")
        println("    - Would be added with score: 50 (KEYWORD_MATCH)")
        println("  Layer 3 (findAmountInLastSectionScored):")
        val lastHalfStart = maxOf(lines.size / 2, 3)
        val inLastSection = yearIdx >= lines.size - lastHalfStart
        println("    - In last 50% of lines: $inLastSection")
        println("    - Would be added with score: 15 (LAST_SECTION)")
        println("  Layer 4 (findLastAmountScored):")
        println("    - Would be added with score: 20 (LAST_AMOUNT)")
        println()

        // Compare with 25.00 score
        val amountIdx = lines.indexOfFirst { it.contains("$ 25.00") }
        println("Scoring analysis for '\$ 25.00':")
        println("  Layer 1 (findByKeywordsScored):")
        println("    - 'Monto' keyword found, '\$ 25.00' is ${amountIdx - montoIdx} lines below")
        println("    - Same line search: no (different line)")
        println("    - Below keyword search: yes, within 7 lines")
        println("    - Score: 50 (KEYWORD_MATCH) + 30 (CURRENCY_BONUS) = 80")
        println("  Layer 2.8 (findAmountsWithCurrencyNearKeywordsScored):")
        val currencyNearKeyword = amountIdx >= 0 && montoIdx >= 0 && kotlin.math.abs(amountIdx - montoIdx) <= 2
        println("    - Has currency symbol: true")
        println("    - Within 2 lines of keyword: $currencyNearKeyword")
        println("    - Score: 60 (CURRENCY_NEAR_KEYWORD)")
    }

    @Test
    fun `debug OCR misread scenarios for dollar sign`() {
        println("=== HYPOTHESIS: ML Kit misreads '$ 25.00' ===")
        println()

        // Common OCR misreads of "$ 25.00":
        val misreadScenarios = listOf(
            "6 25.00" to "Dollar sign misread as '6'",
            "S 25.00" to "Dollar sign misread as 'S'",
            "25.00" to "Dollar sign not detected",
            "$ 6.00" to "Amount misread as 6",
            "$2S.OO" to "OCR errors on amount (S→5, O→0)",
            "$ 2S.OO" to "OCR errors with space",
            "Monto\n6" to "Only '6' detected after Monto",
            "Monto\n$ 6" to "Dollar + 6 detected",
        )

        misreadScenarios.forEach { (text, description) ->
            println("Scenario: $description")
            println("  Text: '$text'")
            val result = AmountDetector().detectFromText(text)
            println("  Result: amount=${result.amount}, confidence=${result.confidence}")
            println()
        }
    }

    @Test
    fun `debug crop scenario with minimal text`() {
        println("=== CROP SCENARIOS ===")
        println()

        val cropScenarios = listOf(
            "Monto\n$ 25.00" to "Full crop with keyword and amount",
            "$ 25.00" to "Amount only (no keyword)",
            "Monto" to "Keyword only (no amount)",
            "Monto\n\$ 25" to "Amount without cents",
            "Monto\n25.00" to "Amount without currency symbol",
            "Total\n$ 25.00" to "Different keyword",
            "Monto\n$25.00" to "No space between $ and amount",
            "Monto\n$  25.00" to "Extra space",
            "Total: $ 25.00" to "Keyword and amount on same line",
        )

        cropScenarios.forEach { (text, description) ->
            println("Scenario: $description")
            println("  Text: '${text.replace("\n", "\\n")}'")
            val result = AmountDetector().detectFromText(text)
            println("  Result: amount=${result.amount}, confidence=${result.confidence}")
            println()
        }
    }
}
