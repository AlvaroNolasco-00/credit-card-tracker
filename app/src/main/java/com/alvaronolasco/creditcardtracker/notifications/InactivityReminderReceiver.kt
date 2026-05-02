package com.alvaronolasco.creditcardtracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alvaronolasco.creditcardtracker.data.repository.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InactivityReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    companion object {
        const val EXTRA_TYPE = "inactivity_type"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!userPreferencesRepository.isInactivityNotificationsEnabled()) return

        val type = intent.getStringExtra(EXTRA_TYPE) ?: return
        val days = when (type) {
            InactivityReminderScheduler.TYPE_3_DAYS -> 3
            InactivityReminderScheduler.TYPE_7_DAYS -> 7
            else -> return
        }

        NotificationHelper(context).showInactivityNotification(days)
    }
}
