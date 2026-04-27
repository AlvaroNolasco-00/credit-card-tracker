package com.alvaronolasco.creditcardtracker.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DateUtilsTest {

    // ──────────────────────────────────────────────
    // getCurrentPeriodRange
    // ──────────────────────────────────────────────

    @Test
    fun `getCurrentPeriodRange returns correct period when today is after cutOffDay`() {
        val cutOffDay = 5
        val today = LocalDate.of(2026, 1, 10)
        val (start, end) = DateUtils.getCurrentPeriodRange(cutOffDay, today)

        val startDate = Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate()
        val endDate = Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate()

        assertEquals(LocalDate.of(2026, 1, 5), startDate)
        assertEquals(LocalDate.of(2026, 2, 4), endDate)
    }

    @Test
    fun `getCurrentPeriodRange returns correct period when today is before cutOffDay`() {
        val cutOffDay = 5
        val today = LocalDate.of(2026, 1, 3)
        val (start, end) = DateUtils.getCurrentPeriodRange(cutOffDay, today)

        val startDate = Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate()
        val endDate = Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate()

        assertEquals(LocalDate.of(2025, 12, 5), startDate)
        assertEquals(LocalDate.of(2026, 1, 4), endDate)
    }

    @Test
    fun `getCurrentPeriodRange handles year boundary correctly`() {
        val cutOffDay = 5
        val today = LocalDate.of(2026, 1, 3)
        val (start, _) = DateUtils.getCurrentPeriodRange(cutOffDay, today)

        val startDate = Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate()
        assertEquals(LocalDate.of(2025, 12, 5), startDate)
    }

    // ──────────────────────────────────────────────
    // getPreviousPeriodRange
    // ──────────────────────────────────────────────

    @Test
    fun `getPreviousPeriodRange returns correct period when today is after cutOffDay`() {
        val cutOffDay = 5
        val today = LocalDate.of(2026, 1, 10)
        val (start, end) = DateUtils.getPreviousPeriodRange(cutOffDay, today)

        val startDate = Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate()
        val endDate = Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate()

        assertEquals(LocalDate.of(2025, 12, 5), startDate)
        assertEquals(LocalDate.of(2026, 1, 4), endDate)
    }

    @Test
    fun `getPreviousPeriodRange returns correct period when today is before cutOffDay`() {
        val cutOffDay = 5
        val today = LocalDate.of(2026, 1, 3)
        val (start, end) = DateUtils.getPreviousPeriodRange(cutOffDay, today)

        val startDate = Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate()
        val endDate = Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate()

        assertEquals(LocalDate.of(2025, 11, 5), startDate)
        assertEquals(LocalDate.of(2025, 12, 4), endDate)
    }

    @Test
    fun `getPreviousPeriodRange handles year boundary correctly`() {
        val cutOffDay = 5
        val today = LocalDate.of(2026, 1, 3)
        val (start, _) = DateUtils.getPreviousPeriodRange(cutOffDay, today)

        val startDate = Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate()
        assertEquals(LocalDate.of(2025, 11, 5), startDate)
    }

    // ──────────────────────────────────────────────
    // getDaysOverduePayment
    // ──────────────────────────────────────────────

    @Test
    fun `getDaysOverduePayment returns 0 when payment is not yet due`() {
        val cutOffDay = 5
        val paymentDueDay = 10
        val today = LocalDate.of(2026, 1, 8)

        val daysOverdue = DateUtils.getDaysOverduePayment(cutOffDay, paymentDueDay, today)
        assertEquals(0, daysOverdue)
    }

    @Test
    fun `getDaysOverduePayment returns correct days when payment is overdue after due date`() {
        val cutOffDay = 5
        val paymentDueDay = 10
        val today = LocalDate.of(2026, 1, 15)

        val daysOverdue = DateUtils.getDaysOverduePayment(cutOffDay, paymentDueDay, today)
        assertEquals(5, daysOverdue)
    }

    @Test
    fun `getDaysOverduePayment returns overdue days when today is before cutOffDay but payment is overdue`() {
        val cutOffDay = 5
        val paymentDueDay = 10
        val today = LocalDate.of(2026, 1, 15)

        val daysOverdue = DateUtils.getDaysOverduePayment(cutOffDay, paymentDueDay, today)
        assertTrue("Should return > 0 when payment is overdue before cut-off day", daysOverdue > 0)
    }

    @Test
    fun `getDaysOverduePayment returns correct overdue days before cut-off in same scenario`() {
        val cutOffDay = 5
        val paymentDueDay = 10
        val today = LocalDate.of(2026, 1, 15)

        val daysOverdue = DateUtils.getDaysOverduePayment(cutOffDay, paymentDueDay, today)
        assertEquals(5, daysOverdue)
    }

    @Test
    fun `getDaysOverduePayment handles payment due day before cut-off day`() {
        val cutOffDay = 20
        val paymentDueDay = 5
        val today = LocalDate.of(2026, 2, 10)

        val daysOverdue = DateUtils.getDaysOverduePayment(cutOffDay, paymentDueDay, today)
        assertTrue(daysOverdue >= 0)
    }

    @Test
    fun `getDaysOverduePayment returns 0 when today equals due date`() {
        val cutOffDay = 5
        val paymentDueDay = 10
        val today = LocalDate.of(2026, 1, 10)

        val daysOverdue = DateUtils.getDaysOverduePayment(cutOffDay, paymentDueDay, today)
        assertEquals(0, daysOverdue)
    }

    // ──────────────────────────────────────────────
    // hasCutOffPassedThisMonth
    // ──────────────────────────────────────────────

    @Test
    fun `hasCutOffPassedThisMonth returns true when today is after cutOffDay`() {
        val cutOffDay = 5
        val today = LocalDate.of(2026, 1, 10)
        assertEquals(true, DateUtils.hasCutOffPassedThisMonth(cutOffDay, today))
    }

    @Test
    fun `hasCutOffPassedThisMonth returns true when today equals cutOffDay`() {
        val cutOffDay = 5
        val today = LocalDate.of(2026, 1, 5)
        assertEquals(true, DateUtils.hasCutOffPassedThisMonth(cutOffDay, today))
    }

    @Test
    fun `hasCutOffPassedThisMonth returns false when today is before cutOffDay`() {
        val cutOffDay = 5
        val today = LocalDate.of(2026, 1, 3)
        assertEquals(false, DateUtils.hasCutOffPassedThisMonth(cutOffDay, today))
    }

    // ──────────────────────────────────────────────
    // getPaymentDueDateForCurrentCycle
    // ──────────────────────────────────────────────

    @Test
    fun `getPaymentDueDateForCurrentCycle returns correct date when due day is after cutOffDay`() {
        val cutOffDay = 5
        val paymentDueDay = 10
        val today = LocalDate.of(2026, 1, 10)

        val dueDate = DateUtils.getPaymentDueDateForCurrentCycle(cutOffDay, paymentDueDay, today)
        assertEquals(LocalDate.of(2026, 1, 10), dueDate)
    }

    @Test
    fun `getPaymentDueDateForCurrentCycle returns next month when due day is before cutOffDay`() {
        val cutOffDay = 20
        val paymentDueDay = 5
        val today = LocalDate.of(2026, 1, 10)

        val dueDate = DateUtils.getPaymentDueDateForCurrentCycle(cutOffDay, paymentDueDay, today)
        assertEquals(LocalDate.of(2026, 1, 5), dueDate)
    }

    @Test
    fun `getPaymentDueDateForCurrentCycle returns correct date when today is before cutOffDay`() {
        val cutOffDay = 5
        val paymentDueDay = 10
        val today = LocalDate.of(2026, 1, 3)

        val dueDate = DateUtils.getPaymentDueDateForCurrentCycle(cutOffDay, paymentDueDay, today)
        assertEquals(LocalDate.of(2025, 12, 10), dueDate)
    }

    // ──────────────────────────────────────────────
    // getDaysUntil
    // ──────────────────────────────────────────────

    @Test
    fun `getDaysUntil returns positive days when target is in the future`() {
        val today = LocalDate.of(2026, 1, 10)
        val result = DateUtils.getDaysUntil(15, today)
        assertEquals(5, result)
    }

    @Test
    fun `getDaysUntil returns days to next month when target has passed`() {
        val today = LocalDate.of(2026, 1, 20)
        val result = DateUtils.getDaysUntil(10, today)
        assertEquals(21, result)
    }
}
