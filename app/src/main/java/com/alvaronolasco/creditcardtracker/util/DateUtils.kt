package com.alvaronolasco.creditcardtracker.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

object DateUtils {

    fun getDaysUntil(dayOfMonth: Int): Int = getDaysUntil(dayOfMonth, LocalDate.now())

    fun getDaysUntil(dayOfMonth: Int, today: LocalDate): Int {
        var targetDate = today.withDayOfMonth(dayOfMonth.coerceIn(1, today.lengthOfMonth()))

        if (targetDate.isBefore(today)) {
            targetDate = targetDate.plusMonths(1)
            val maxDays = targetDate.lengthOfMonth()
            if (dayOfMonth > maxDays) {
                targetDate = targetDate.withDayOfMonth(maxDays)
            }
        }

        return ChronoUnit.DAYS.between(today, targetDate).toInt()
    }

    fun getCurrentPeriodRange(cutOffDay: Int): Pair<Long, Long> =
        getCurrentPeriodRange(cutOffDay, LocalDate.now())

    fun getCurrentPeriodRange(cutOffDay: Int, today: LocalDate): Pair<Long, Long> {
        var startCutOff = today.withDayOfMonth(cutOffDay.coerceIn(1, today.lengthOfMonth()))

        if (startCutOff.isAfter(today)) {
            startCutOff = startCutOff.minusMonths(1)
        }

        val endCutOff = startCutOff.plusMonths(1).minusDays(1)

        return Pair(
            startCutOff.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000,
            endCutOff.atTime(23, 59, 59).toEpochSecond(ZoneOffset.UTC) * 1000
        )
    }

    fun getCurrentMonthYear(): String {
        val today = LocalDate.now()
        return String.format("%d-%02d", today.year, today.monthValue)
    }

    fun hasCutOffPassedThisMonth(cutOffDay: Int): Boolean =
        hasCutOffPassedThisMonth(cutOffDay, LocalDate.now())

    fun hasCutOffPassedThisMonth(cutOffDay: Int, today: LocalDate): Boolean {
        val cutDate = today.withDayOfMonth(cutOffDay.coerceIn(1, today.lengthOfMonth()))
        return !cutDate.isAfter(today)
    }

    fun getPaymentDueDateForCurrentCycle(cutOffDay: Int, paymentDueDay: Int): LocalDate =
        getPaymentDueDateForCurrentCycle(cutOffDay, paymentDueDay, LocalDate.now())

    fun getPaymentDueDateForCurrentCycle(cutOffDay: Int, paymentDueDay: Int, today: LocalDate): LocalDate {
        var cutOffDate = today.withDayOfMonth(cutOffDay.coerceIn(1, today.lengthOfMonth()))
        if (cutOffDate.isAfter(today)) {
            cutOffDate = cutOffDate.minusMonths(1)
            cutOffDate = cutOffDate.withDayOfMonth(cutOffDay.coerceIn(1, cutOffDate.lengthOfMonth()))
        }
        var dueDate = cutOffDate.withDayOfMonth(paymentDueDay.coerceIn(1, cutOffDate.lengthOfMonth()))
        if (!dueDate.isAfter(cutOffDate)) {
            val next = cutOffDate.plusMonths(1)
            dueDate = next.withDayOfMonth(paymentDueDay.coerceIn(1, next.lengthOfMonth()))
        }
        return dueDate
    }

    fun getDaysOverduePayment(cutOffDay: Int, paymentDueDay: Int): Int =
        getDaysOverduePayment(cutOffDay, paymentDueDay, LocalDate.now())

    fun getDaysOverduePayment(cutOffDay: Int, paymentDueDay: Int, today: LocalDate): Int {
        val dueDate = getPaymentDueDateForCurrentCycle(cutOffDay, paymentDueDay, today)
        return if (today.isAfter(dueDate)) ChronoUnit.DAYS.between(dueDate, today).toInt() else 0
    }

    fun getPreviousPeriodRange(cutOffDay: Int): Pair<Long, Long> =
        getPreviousPeriodRange(cutOffDay, LocalDate.now())

    fun getPreviousPeriodRange(cutOffDay: Int, today: LocalDate): Pair<Long, Long> {
        val (currentStart, _) = getCurrentPeriodRange(cutOffDay, today)
        val currentStartDate = Instant.ofEpochMilli(currentStart).atZone(ZoneOffset.UTC).toLocalDate()
        val endDate = currentStartDate.minusDays(1)
        val startDate = endDate.minusMonths(1).plusDays(1)
        return Pair(
            startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000,
            endDate.atTime(23, 59, 59).toEpochSecond(ZoneOffset.UTC) * 1000
        )
    }

    fun getPeriodsRange(cutOffDay: Int, count: Int): List<Pair<Long, Long>> {
        val periods = mutableListOf<Pair<Long, Long>>()
        val today = LocalDate.now()

        var currentStartBasis = today.withDayOfMonth(cutOffDay.coerceIn(1, today.lengthOfMonth()))
        if (currentStartBasis.isAfter(today)) {
            currentStartBasis = currentStartBasis.minusMonths(1)
        }

        for (i in 0 until count) {
            val start = currentStartBasis.minusMonths(i.toLong())
            val end = start.plusMonths(1).minusDays(1)

            periods.add(
                Pair(
                    start.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000,
                    end.atTime(23, 59, 59).toEpochSecond(ZoneOffset.UTC) * 1000
                )
            )
        }

        return periods.reversed()
    }

    fun isRecurringExpenseApplicable(dayOfMonth: Int?, periodStart: Long, periodEnd: Long): Boolean {
        if (dayOfMonth == null) return true
        val startDate = Instant.ofEpochMilli(periodStart).atZone(ZoneId.systemDefault()).toLocalDate()
        val endDate = Instant.ofEpochMilli(periodEnd).atZone(ZoneId.systemDefault()).toLocalDate()
        val monthsToCheck = listOf(
            YearMonth.from(startDate),
            YearMonth.from(endDate)
        ).distinct()
        return monthsToCheck.any { yearMonth ->
            val safeDay = dayOfMonth.coerceAtMost(yearMonth.lengthOfMonth())
            val candidateDate = yearMonth.atDay(safeDay)
            !candidateDate.isBefore(startDate) && !candidateDate.isAfter(endDate)
        }
    }
}
