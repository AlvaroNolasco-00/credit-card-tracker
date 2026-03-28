package com.alvaronolasco.creditcardtracker.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object DateUtils {
    
    fun getDaysUntil(dayOfMonth: Int): Int {
        val today = LocalDate.now()
        var targetDate = today.withDayOfMonth(dayOfMonth.coerceIn(1, today.lengthOfMonth()))
        
        if (targetDate.isBefore(today)) {
            targetDate = targetDate.plusMonths(1)
            // Ensure the next month also has enough days
            val maxDays = targetDate.lengthOfMonth()
            if (dayOfMonth > maxDays) {
                targetDate = targetDate.withDayOfMonth(maxDays)
            }
        }
        
        return ChronoUnit.DAYS.between(today, targetDate).toInt()
    }

    fun getCurrentPeriodRange(cutOffDay: Int): Pair<Long, Long> {
        val today = LocalDate.now()
        var startCutOff = today.withDayOfMonth(cutOffDay.coerceIn(1, today.lengthOfMonth()))
        
        if (startCutOff.isAfter(today)) {
            startCutOff = startCutOff.minusMonths(1)
        }
        
        val endCutOff = startCutOff.plusMonths(1).minusDays(1)
        
        // Return as epoch milliseconds (start of day to end of day)
        return Pair(
            startCutOff.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
            endCutOff.atTime(23, 59, 59).toEpochSecond(java.time.ZoneOffset.UTC) * 1000
        )
    }

    fun getCurrentMonthYear(): String {
        val today = LocalDate.now()
        return String.format("%d-%02d", today.year, today.monthValue)
    }

    fun hasCutOffPassedThisMonth(cutOffDay: Int): Boolean {
        val today = LocalDate.now()
        val cutDate = today.withDayOfMonth(cutOffDay.coerceIn(1, today.lengthOfMonth()))
        return !cutDate.isAfter(today)
    }

    fun getPreviousPeriodRange(cutOffDay: Int): Pair<Long, Long> {
        val today = LocalDate.now()
        val thisCutOff = today.withDayOfMonth(cutOffDay.coerceIn(1, today.lengthOfMonth()))
        val prevMonth = thisCutOff.minusMonths(1)
        val prevCutOff = prevMonth.withDayOfMonth(cutOffDay.coerceIn(1, prevMonth.lengthOfMonth()))
        val endDate = thisCutOff.minusDays(1)
        return Pair(
            prevCutOff.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
            endDate.atTime(23, 59, 59).toEpochSecond(java.time.ZoneOffset.UTC) * 1000
        )
    }
}
