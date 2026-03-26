package com.alvaronolasco.creditcardtracker.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrAmountDetectorTest {

    @Test
    fun `test detect total with dot decimal`() {
        val text = "Subtotal: 100.00\nTax: 15.00\nTOTAL: 115.00"
        val detector = AmountDetector()
        val result = detector.detectFromText(text)
        assertEquals(115.00, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun `test detect total with comma decimal and dot thousands`() {
        val text = "Importe Total: 1.250,50"
        val detector = AmountDetector()
        val result = detector.detectFromText(text)
        assertEquals(1250.50, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun `test detect total with comma thousands and dot decimal`() {
        val text = "Total Amount: 1,500.75"
        val detector = AmountDetector()
        val result = detector.detectFromText(text)
        assertEquals(1500.75, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun `test detect total without decimals`() {
        val text = "GRAN TOTAL $450"
        val detector = AmountDetector()
        val result = detector.detectFromText(text)
        assertEquals(450.0, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun `test detect total with currency symbols`() {
        val text = "Price: Q150.00\nTotal: L300.50"
        val detector = AmountDetector()
        val result = detector.detectFromText(text)
        assertEquals(300.50, result.amount!!, 0.001)
    }

    @Test
    fun `test take maximum amount when no keyword found`() {
        val text = "Item 1: 5.00\nItem 2: 10.00\n15.00"
        val detector = AmountDetector()
        val result = detector.detectFromText(text)
        assertEquals(15.00, result.amount!!, 0.001)
        assertEquals(Confidence.LOW, result.confidence)
    }

    @Test
    fun `test ignore dates and phone numbers`() {
        val text = "Date: 2024-03-16\nTel: 555-0199\nTotal 75.20"
        val detector = AmountDetector()
        val result = detector.detectFromText(text)
        assertEquals(75.20, result.amount!!, 0.001)
    }

    @Test
    fun `test phone number above total is ignored`() {
        val text = "Restaurante XYZ\nTel: 2334-5678\nItem 1: 50.00\nItem 2: 30.00\nTotal: 80.00"
        val detector = AmountDetector()
        val result = detector.detectFromText(text)
        assertEquals(80.00, result.amount!!, 0.001)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun `test long phone number not mistaken for amount`() {
        val text = "Tienda ABC\n+502 5555 1234\nMonto Total: 125.50"
        val detector = AmountDetector()
        val result = detector.detectFromText(text)
        assertEquals(125.50, result.amount!!, 0.001)
    }

    @Test
    fun `test multiple totals picks last one`() {
        val text = "Subtotal: 100.00\nImpuesto: 12.00\nTotal: 100.00\nDescuento: 5.00\nTOTAL A PAGAR: 107.00"
        val detector = AmountDetector()
        val result = detector.detectFromText(text)
        assertEquals(107.00, result.amount!!, 0.001)
    }

    @Test
    fun `test receipt with header phone and footer total`() {
        val text = "COMERCIAL SAN JOSE\nNIT: 12345678\nTel: 7890-1234\n---\nProducto A  25.00\nProducto B  15.00\n---\nSubtotal  40.00\nIVA  4.80\nTotal  44.80"
        val detector = AmountDetector()
        val result = detector.detectFromText(text)
        assertEquals(44.80, result.amount!!, 0.001)
    }

    @Test
    fun `test no keywords with phone number uses last section amount`() {
        val text = "TIENDA XYZ\nTel: 5555-0199\nPan  3.50\nLeche  12.00\n15.50"
        val detector = AmountDetector()
        val result = detector.detectFromText(text)
        assertEquals(15.50, result.amount!!, 0.001)
    }

    @Test
    fun `test date pattern not confused with amount`() {
        val text = "Fecha: 16/03/2024\nHora: 14:30\nTotal: 250.00"
        val detector = AmountDetector()
        val result = detector.detectFromText(text)
        assertEquals(250.00, result.amount!!, 0.001)
    }

    @Test
    fun `test empty text returns null`() {
        val text = ""
        val detector = AmountDetector()
        val result = detector.detectFromText(text)
        assertEquals(null, result.amount)
        assertEquals(Confidence.NONE, result.confidence)
    }
}
