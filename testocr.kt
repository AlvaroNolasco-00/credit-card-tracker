import java.io.File
import kotlin.math.max

// Paste AmountDetector here to test
class AmountDetector {
    
    data class DetectionResult(
        val amount: Double?,
        val confidence: String
    )

    private val totalKeywords = listOf(
        "total", "total a pagar", "monto total", "importe total",
        "gran total", "neto", "amount due", "balance due",
        "monto", "importe", "a pagar", "cobro", "cargo total",
        "amount", "net total", "grand total", "sum", "due", "pay",
        "compra por", "consumo", "pagado", "pago", "por usd", "por mxn", "usd", "mxn", "eur"
    )

    private val amountRegex = Regex(
        """([$€£¥₣₹]|Q|L|HNL|GTQ|USD|MXN|EUR)?\s*(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2})?|\d+(?:[.,]\d{2})?)\b""",
        RegexOption.IGNORE_CASE
    )

    private val phonePatterns = listOf(
        Regex("""(?:\+?\d{1,3}[-.\s]?)?\(?\d{2,4}\)?[-.\s]?\d{3,4}[-.\s]?\d{4}"""), 
        Regex("""\d{7,}"""),  
    )

    private val datePatterns = listOf(
        Regex("""\d{4}[-/]\d{1,2}[-/]\d{1,2}"""),   
        Regex("""\d{1,2}[-/]\d{1,2}[-/]\d{2,4}"""), 
    )

    fun detectFromText(text: String): DetectionResult {
        val keywordResult = findByKeywords(text)
        if (keywordResult != null) return DetectionResult(keywordResult, "HIGH")

        val lastSectionResult = findAmountInLastSection(text)
        if (lastSectionResult != null) return DetectionResult(lastSectionResult, "MEDIUM")

        val maxAmount = findMaxAmount(text)
        if (maxAmount != null) return DetectionResult(maxAmount, "LOW")

        return DetectionResult(null, "NONE")
    }

    private fun findByKeywords(text: String): Double? {
        val lines = text.split("\n")
        val reversedLines = lines.asReversed()

        totalKeywords.forEach { keyword ->
            reversedLines.forEachIndexed { i, line ->
                if (line.contains(keyword, ignoreCase = true)) {
                    val substring = line.substring(line.indexOf(keyword, ignoreCase = true))
                    val match = amountRegex.find(substring)
                    
                    if (match != null && !looksLikeNonMonetary(match.value)) {
                        parseAmount(match.groupValues[2])?.let { 
                            if (it > 0.0) return it 
                        }
                    }

                    val startSearch = max(0, i - 5)
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

    private fun findMaxAmount(text: String): Double? {
        val lines = text.split("\n")
        val halfPoint = lines.size / 2
        val bottomHalfText = lines.drop(halfPoint).joinToString("\n")
        val bottomResult = amountRegex.findAll(bottomHalfText)
            .filter { !looksLikeNonMonetary(it.value) }
            .mapNotNull { parseAmount(it.groupValues[2]) }
            .filter { it in 0.01..999999.99 }
            .maxOrNull()
        
        if (bottomResult != null) return bottomResult
        
        return amountRegex.findAll(text)
            .filter { !looksLikeNonMonetary(it.value) }
            .mapNotNull { parseAmount(it.groupValues[2]) }
            .filter { it in 0.01..999999.99 }
            .maxOrNull()
    }

    private fun findAmountInLastSection(text: String): Double? {
        val lines = text.split("\n")
        val lastThird = lines.takeLast(max(lines.size / 3, 1))
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
        if (phonePatterns.any { it.containsMatchIn(text) }) return true
        if (datePatterns.any { it.containsMatchIn(text) }) return true
        return false
    }

    private fun parseAmount(amountStr: String): Double? {
        var clean = amountStr.replace("[^0-9,.]".toRegex(), "")
        if (clean.isEmpty()) return null

        if (clean.contains(".") && clean.contains(",")) {
            val lastDot = clean.lastIndexOf(".")
            val lastComma = clean.lastIndexOf(",")
            if (lastDot > lastComma) {
                clean = clean.replace(",", "")
            } else {
                clean = clean.replace(".", "").replace(",", ".")
            }
        } else {
            val lastSeparatorIndex = if (clean.contains(",")) clean.lastIndexOf(",") else clean.lastIndexOf(".")
            if (lastSeparatorIndex != -1) {
                val charsAfter = clean.length - lastSeparatorIndex - 1
                if (charsAfter == 3 && clean.count { it == ',' || it == '.' } > 1) {
                    clean = clean.replace(",", "").replace(".", "")
                } else if (charsAfter <= 2) {
                    clean = clean.replace(",", ".")
                } else {
                    clean = clean.replace(",", "").replace(".", "")
                }
            }
        }
        return clean.toDoubleOrNull()
    }
}

fun main() {
    val text = """Suc.: S0241
Ca.Ja:
2753
Fecha: 29/03/26 01:09:24 p.m.
NO ES UN DOCUMENTO FISCAL
TIPO DOC: FACTURA CONSUMIDOR FINAL
Fecha y hora de generacion:
29/03/26 07:10 p.m.
Codigo de generacion:
1EBA281E-3383-42F9-B67E-D72FEDDC11EB
Numero de control:
DTE-01-S219P006-000000000144159
Sello de autorizacion:
202684F5B18D3A884C94B76ED7E82A295045XAOP
Cantid Descripción
Precio Total $:
1
1
1
1
HIELO SELECTOS 2
GASEOSA TROPICAL
VASO
BIODEGRADABL
OFERTA
SAL MOLIDA DOÑA B
AGUA PURIFICADA S
BOQUITAS FRIJOLI
PIGUIS S/TOCINO/L
CHICLE TRIDENT FR
SERVILLETA CUADRA
OFERTA
DORITO SALSA VERD
LOCION REPELENT N
GASEOSA COCA COLA
1.15
1.30
0.99
0.18
0.85
1.21
1.19
2.84
0.65
1.56
3.32
1.30
1.15G
1.30G
0.99G
-0.10
0. 18G
0.85G
1.21 G
1.19G
2.84G
0. 65G
-0.10
1.56G
3.32G
1.30G
G-GRAVADO
E-EXENTO
SUB TOTAL $:
EXENTO $:
GRAVADO $:
VENTAS NO SUJETAS $:
CESC $:
TOTAL A PAGAR $:
NICO
NICO
407319******7543
Terminal ID
16.34
0.00
16.34
0.00
0.00
16.34
16.34
00286047
Numero de productos
12
Ahorros totales:
0.20
Terminal ID: 30878"""

    val result = AmountDetector().detectFromText(text)
    println("Detected amount: ${result.amount}")
}
