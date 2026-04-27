package com.alvaronolasco.creditcardtracker.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OcrAmountDetectorTest {

    // ──────────────────────────────────────────────
    // Existing tests (must keep passing)
    // ──────────────────────────────────────────────

    @Test
    fun `test detect total with dot decimal`() {
        val text = "Subtotal: 100.00\nTax: 15.00\nTOTAL: 115.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(115.00, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun `test detect total with comma decimal and dot thousands`() {
        val text = "Importe Total: 1.250,50"
        val result = AmountDetector().detectFromText(text)
        assertEquals(1250.50, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun `test detect total with comma thousands and dot decimal`() {
        val text = "Total Amount: 1,500.75"
        val result = AmountDetector().detectFromText(text)
        assertEquals(1500.75, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun `test detect total without decimals`() {
        val text = "GRAN TOTAL $450"
        val result = AmountDetector().detectFromText(text)
        assertEquals(450.0, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun `test detect total with currency symbols`() {
        val text = "Price: Q150.00\nTotal: L300.50"
        val result = AmountDetector().detectFromText(text)
        assertEquals(300.50, result.amount!!, 0.001)
    }

    @Test
    fun `test take maximum amount when no keyword found`() {
        val text = "Item 1: 5.00\nItem 2: 10.00\n15.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(15.00, result.amount!!, 0.001)
        // 15.00 is in the last section of the text → MEDIUM (last-section analysis, Layer 3)
        assertEquals(Confidence.MEDIUM, result.confidence)
    }

    @Test
    fun `test ignore dates and phone numbers`() {
        val text = "Date: 2024-03-16\nTel: 555-0199\nTotal 75.20"
        val result = AmountDetector().detectFromText(text)
        assertEquals(75.20, result.amount!!, 0.001)
    }

    @Test
    fun `test phone number above total is ignored`() {
        val text = "Restaurante XYZ\nTel: 2334-5678\nItem 1: 50.00\nItem 2: 30.00\nTotal: 80.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(80.00, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun `test long phone number not mistaken for amount`() {
        val text = "Tienda ABC\n+502 5555 1234\nMonto Total: 125.50"
        val result = AmountDetector().detectFromText(text)
        assertEquals(125.50, result.amount!!, 0.001)
    }

    @Test
    fun `test multiple totals picks last one`() {
        val text = "Subtotal: 100.00\nImpuesto: 12.00\nTotal: 100.00\nDescuento: 5.00\nTOTAL A PAGAR: 107.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(107.00, result.amount!!, 0.001)
    }

    @Test
    fun `test receipt with header phone and footer total`() {
        val text = "COMERCIAL SAN JOSE\nNIT: 12345678\nTel: 7890-1234\n---\nProducto A  25.00\nProducto B  15.00\n---\nSubtotal  40.00\nIVA  4.80\nTotal  44.80"
        val result = AmountDetector().detectFromText(text)
        assertEquals(44.80, result.amount!!, 0.001)
    }

    @Test
    fun `test no keywords with phone number uses last section amount`() {
        val text = "TIENDA XYZ\nTel: 5555-0199\nPan  3.50\nLeche  12.00\n15.50"
        val result = AmountDetector().detectFromText(text)
        assertEquals(15.50, result.amount!!, 0.001)
    }

    @Test
    fun `test date pattern not confused with amount`() {
        val text = "Fecha: 16/03/2024\nHora: 14:30\nTotal: 250.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(250.00, result.amount!!, 0.001)
    }

    @Test
    fun `test empty text returns null`() {
        val result = AmountDetector().detectFromText("")
        assertEquals(null, result.amount)
        assertEquals(Confidence.NONE, result.confidence)
    }

    // ──────────────────────────────────────────────
    // New tests covering the improvements
    // ──────────────────────────────────────────────

    // Fix #1 — 1 decimal place
    @Test
    fun `test amount with single decimal place`() {
        val text = "Total: 99.5"
        val result = AmountDetector().detectFromText(text)
        assertEquals(99.5, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    // Fix #3 — amount ABOVE the keyword
    @Test
    fun `test amount printed above the keyword`() {
        val text = "Producto A  10.00\nProducto B  20.00\n45.50\nTOTAL"
        val result = AmountDetector().detectFromText(text)
        assertEquals(45.50, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    // Fix #6 — amount before keyword on the same line
    @Test
    fun `test amount before keyword on same line`() {
        val text = "Descripcion  Monto\n55.00  TOTAL"
        val result = AmountDetector().detectFromText(text)
        assertEquals(55.00, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    // Fix #7 — new regional keywords
    @Test
    fun `test neto a pagar keyword`() {
        val text = "Subtotal: 200.00\nNeto a pagar: 218.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(218.00, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun `test total ventas keyword`() {
        val text = "Ventas  350.00\nIVA  42.00\nTotal Ventas: 392.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(392.00, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun `test total general keyword`() {
        val text = "Suma: 500.00\nTotal General  575.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(575.00, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun `test cobrado keyword`() {
        val text = "Items: 3\nCobrado: 88.50"
        val result = AmountDetector().detectFromText(text)
        assertEquals(88.50, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    // Fix #8 — last section now covers last 50% of text
    @Test
    fun `test long receipt amount detected in middle section`() {
        // 20 item lines + total at line 15 (middle-ish) — old 33% wouldn't reach it
        val items = (1..12).map { "Item $it: ${it * 5}.00" }.joinToString("\n")
        val text = "$items\nTotal: 390.00\nGracias por su compra\nTel: 2222-3333"
        val result = AmountDetector().detectFromText(text)
        assertEquals(390.00, result.amount!!, 0.001)
    }

    // Fix #2 — structured phone with dashes is filtered but raw large number is not
    @Test
    fun `test large valid amount not filtered as phone`() {
        val text = "Compra especial\nTotal: 12500.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(12500.00, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    // Regression — subtotal lines with keyword should still be ignored
    @Test
    fun `test subtotal line is skipped in favor of real total`() {
        val text = "Subtotal: 300.00\nIVA 5%%: 15.00\nTotal: 315.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(315.00, result.amount!!, 0.001)
    }

    // ──────────────────────────────────────────────
    // ADR-055 — Receipt edge-case fixes
    // ──────────────────────────────────────────────

    // Fix 1: date filtering on contextStr
    @Test
    fun `test date component numbers are filtered when in date context`() {
        val text = "Fecha: 25/04/2026\nMonto\n\$ 25.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(25.00, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun `test year in date line is not a valid amount`() {
        val text = "Date: 2024-03-16\nTotal: 75.20"
        val result = AmountDetector().detectFromText(text)
        assertEquals(75.20, result.amount!!, 0.001)
    }

    // Fix 2: card number pattern detection
    @Test
    fun `test card number is not detected as amount`() {
        val text = "Número de tarjeta\n**** **** **** 4399\nMonto\n\$ 25.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(25.00, result.amount!!, 0.001)
    }

    @Test
    fun `test masked card number on same line is filtered`() {
        val text = "Tarjeta **** 4399\nTotal: 100.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(100.00, result.amount!!, 0.001)
    }

    // Fix 3: OCR normalization for keywords
    @Test
    fun `test keyword with OCR error O-zero is still detected`() {
        val text = "M0nto\n\$ 25.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(25.00, result.amount!!, 0.001)
    }

    // Fix 4: currency proximity boost
    @Test
    fun `test amount with currency near keyword gets high score`() {
        val text = "Monto\n\$ 25.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(25.00, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun `test currency amount near keyword beats plain amount`() {
        val text = "Item 1: 4399\nMonto\n\$ 25.00"
        val result = AmountDetector().detectFromText(text)
        assertEquals(25.00, result.amount!!, 0.001)
    }
}
