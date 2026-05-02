package com.alvaronolasco.creditcardtracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.alvaronolasco.creditcardtracker.data.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InactivityReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val TYPE_3_DAYS = "INACTIVITY_3"
        const val TYPE_7_DAYS = "INACTIVITY_7"
        private const val REQUEST_CODE_3_DAYS = 9001
        private const val REQUEST_CODE_7_DAYS = 9002
        private const val HOUR_OF_DAY = 10
        private const val MINUTE = 0
    }

    fun schedule() {
        if (!userPreferencesRepository.isInactivityNotificationsEnabled()) {
            cancelAll()
            return
        }

        val lastOpen = userPreferencesRepository.getLastAppOpen()
        val now = System.currentTimeMillis()

        cancelAll()

        scheduleAlarm(lastOpen, TimeUnit.DAYS.toMillis(3), REQUEST_CODE_3_DAYS, TYPE_3_DAYS)
        scheduleAlarm(lastOpen, TimeUnit.DAYS.toMillis(7), REQUEST_CODE_7_DAYS, TYPE_7_DAYS)
    }

    fun cancelAll() {
        listOf(REQUEST_CODE_3_DAYS, REQUEST_CODE_7_DAYS).forEach { requestCode ->
            val intent = Intent(context, InactivityReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun scheduleAlarm(
        baseTime: Long,
        offsetMillis: Long,
        requestCode: Int,
        type: String
    ) {
        val triggerAtMillis = baseTime + offsetMillis

        // Si ya pasó el tiempo, no programar (evita notificación instantánea al abrir)
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, InactivityReminderReceiver::class.java).apply {
            putExtra(InactivityReminderReceiver.EXTRA_TYPE, type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }
}
