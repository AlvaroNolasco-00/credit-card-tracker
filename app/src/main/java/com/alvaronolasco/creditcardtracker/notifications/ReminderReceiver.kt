package com.alvaronolasco.creditcardtracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_CARD_NAME = "cardName"
        const val EXTRA_BANK = "bank"
        const val EXTRA_LAST_FOUR = "lastFour"
        const val EXTRA_CARD_COLOR = "cardColor"
        const val EXTRA_NOTIFICATION_TYPE = "notificationType"
        const val EXTRA_DAYS_BEFORE = "daysBefore"
        const val EXTRA_EVENT_DATE = "eventDate"
        const val EXTRA_ID = "id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val data = CardNotificationData(
            id = intent.getIntExtra(EXTRA_ID, 0),
            cardName = intent.getStringExtra(EXTRA_CARD_NAME) ?: "Tarjeta",
            bank = intent.getStringExtra(EXTRA_BANK) ?: "",
            lastFour = intent.getStringExtra(EXTRA_LAST_FOUR) ?: "••••",
            cardColor = intent.getIntExtra(EXTRA_CARD_COLOR, Color.parseColor("#6200EE")),
            notificationType = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE) ?: "PAYMENT",
            daysBefore = intent.getIntExtra(EXTRA_DAYS_BEFORE, 0),
            eventDate = intent.getStringExtra(EXTRA_EVENT_DATE) ?: ""
        )

        NotificationHelper(context).showNotification(data)
    }
}
