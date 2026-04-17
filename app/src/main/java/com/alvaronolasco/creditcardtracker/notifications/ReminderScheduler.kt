package com.alvaronolasco.creditcardtracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.alvaronolasco.creditcardtracker.data.entity.CreditCard
import com.alvaronolasco.creditcardtracker.data.entity.NotificationConfig
import com.alvaronolasco.creditcardtracker.util.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val TYPE_OVERDUE = "OVERDUE"
        private val OVERDUE_DAYS_OFFSETS = listOf(1, 4, 7)
    }

    // Deterministic ID from card + type + days — allows cancellation without config entity
    private fun alarmId(cardId: Int, type: String, daysBefore: Int) =
        "${cardId}_${type}_${daysBefore}".hashCode()

    fun scheduleReminders(card: CreditCard, configs: List<NotificationConfig>) {
        cancelReminders(card)
        configs.filter { it.enabled }.forEach { config ->
            val targetDay = if (config.type == "CUT_OFF") card.cutOffDay else card.paymentDueDay
            val (triggerTime, eventDate) = calculateTriggerTimeAndDate(targetDay, config.daysBefore)

            // Skip if trigger time is in the past
            if (triggerTime <= System.currentTimeMillis()) return@forEach

            val id = alarmId(card.id, config.type, config.daysBefore)

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                // Card identity & display data
                putExtra(ReminderReceiver.EXTRA_CARD_NAME, card.name)
                putExtra(ReminderReceiver.EXTRA_BANK, card.bank)
                putExtra(ReminderReceiver.EXTRA_LAST_FOUR, card.lastFourDigits)
                putExtra(ReminderReceiver.EXTRA_CARD_COLOR, card.color)
                // Notification context
                putExtra(ReminderReceiver.EXTRA_NOTIFICATION_TYPE, config.type)
                putExtra(ReminderReceiver.EXTRA_DAYS_BEFORE, config.daysBefore)
                putExtra(ReminderReceiver.EXTRA_EVENT_DATE, eventDate)
                putExtra(ReminderReceiver.EXTRA_ID, id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
        scheduleOverdueAlarm(card)
    }

    fun scheduleOverdueAlarm(card: CreditCard) {
        cancelOverdueAlarm(card)
        val dueDate = DateUtils.getPaymentDueDateForCurrentCycle(card.cutOffDay, card.paymentDueDay)
        OVERDUE_DAYS_OFFSETS.forEach { daysAfter ->
            val triggerDate = dueDate.plusDays(daysAfter.toLong())
            val triggerDateTime = LocalDateTime.of(triggerDate, LocalTime.of(10, 0))
            val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (triggerMillis <= System.currentTimeMillis()) return@forEach

            val id = alarmId(card.id, TYPE_OVERDUE, daysAfter)
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(ReminderReceiver.EXTRA_CARD_ID, card.id)
                putExtra(ReminderReceiver.EXTRA_CARD_NAME, card.name)
                putExtra(ReminderReceiver.EXTRA_BANK, card.bank)
                putExtra(ReminderReceiver.EXTRA_LAST_FOUR, card.lastFourDigits)
                putExtra(ReminderReceiver.EXTRA_CARD_COLOR, card.color)
                putExtra(ReminderReceiver.EXTRA_NOTIFICATION_TYPE, TYPE_OVERDUE)
                putExtra(ReminderReceiver.EXTRA_DAYS_BEFORE, daysAfter)
                putExtra(ReminderReceiver.EXTRA_EVENT_DATE, "")
                putExtra(ReminderReceiver.EXTRA_ID, id)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun cancelOverdueAlarm(card: CreditCard) {
        OVERDUE_DAYS_OFFSETS.forEach { daysAfter ->
            val id = alarmId(card.id, TYPE_OVERDUE, daysAfter)
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    fun cancelReminders(card: CreditCard) {
        listOf("CUT_OFF", "PAYMENT").forEach { type ->
            listOf(0, 1, 3, 5).forEach { daysBefore ->
                val id = alarmId(card.id, type, daysBefore)
                val intent = Intent(context, ReminderReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context, id, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            }
        }
        cancelOverdueAlarm(card)
    }

    /**
     * Returns the trigger epoch-millis (9:00 AM on the reminder day) and a formatted
     * string of the actual event date (e.g. "viernes, 18 de abril").
     */
    private fun calculateTriggerTimeAndDate(dayOfMonth: Int, daysBefore: Int): Pair<Long, String> {
        val today = LocalDate.now()
        var targetDate = today.withDayOfMonth(dayOfMonth.coerceIn(1, today.lengthOfMonth()))
        if (targetDate.isBefore(today) || targetDate == today) {
            val next = targetDate.plusMonths(1)
            targetDate = next.withDayOfMonth(dayOfMonth.coerceIn(1, next.lengthOfMonth()))
        }

        val formattedEventDate = buildFormattedDate(targetDate)

        val reminderDate = targetDate.minusDays(daysBefore.toLong())
        val triggerDateTime = LocalDateTime.of(reminderDate, LocalTime.of(9, 0))
        val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return Pair(triggerMillis, formattedEventDate)
    }

    /** "viernes, 18 de abril" */
    private fun buildFormattedDate(date: LocalDate): String {
        val locale = Locale("es")
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val monthName = date.month.getDisplayName(TextStyle.FULL, locale)
        return "$dayName, ${date.dayOfMonth} de $monthName"
    }
}
