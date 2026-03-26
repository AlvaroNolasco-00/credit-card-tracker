package com.alvaronolasco.creditcardtracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.alvaronolasco.creditcardtracker.data.entity.CreditCard
import com.alvaronolasco.creditcardtracker.data.entity.NotificationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Deterministic ID from card + type + days — allows cancellation without config entity
    private fun alarmId(cardId: Int, type: String, daysBefore: Int) =
        "${cardId}_${type}_${daysBefore}".hashCode()

    fun scheduleReminders(card: CreditCard, configs: List<NotificationConfig>) {
        cancelReminders(card)
        configs.filter { it.enabled }.forEach { config ->
            val targetDay = if (config.type == "CUT_OFF") card.cutOffDay else card.paymentDueDay
            val triggerTime = calculateTriggerTime(targetDay, config.daysBefore)

            // Skip if trigger time is in the past
            if (triggerTime <= System.currentTimeMillis()) return@forEach

            val id = alarmId(card.id, config.type, config.daysBefore)
            val eventName = if (config.type == "CUT_OFF") "corte" else "pago"
            val daysText = if (config.daysBefore == 0) "es hoy" else "es en ${config.daysBefore} día(s)"

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("title", "Tarjeta ${card.name} — ${card.bank}")
                putExtra("message", "Tu fecha de $eventName $daysText")
                putExtra("id", id)
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
    }

    private fun calculateTriggerTime(dayOfMonth: Int, daysBefore: Int): Long {
        val today = LocalDate.now()
        var targetDate = today.withDayOfMonth(dayOfMonth.coerceIn(1, today.lengthOfMonth()))
        if (targetDate.isBefore(today) || targetDate == today) {
            val next = targetDate.plusMonths(1)
            targetDate = next.withDayOfMonth(dayOfMonth.coerceIn(1, next.lengthOfMonth()))
        }
        val reminderDate = targetDate.minusDays(daysBefore.toLong())
        val triggerDateTime = LocalDateTime.of(reminderDate, LocalTime.of(9, 0))
        return triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
