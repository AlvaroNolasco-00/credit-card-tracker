package com.alvaronolasco.creditcardtracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Recordatorio de Tarjeta"
        val message = intent.getStringExtra("message") ?: "Tienes un evento próximo"
        val id = intent.getIntExtra("id", 0)

        val notificationHelper = NotificationHelper(context)
        notificationHelper.showNotification(title, message, id)
    }
}
