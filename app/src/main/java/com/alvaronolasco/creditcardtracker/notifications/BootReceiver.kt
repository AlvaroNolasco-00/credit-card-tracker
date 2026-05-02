package com.alvaronolasco.creditcardtracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: CreditCardRepository
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var inactivityScheduler: InactivityReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cards = repository.getAllCards().first()
                cards.forEach { card ->
                    val configs = repository.getConfigsByCard(card.id).first()
                    scheduler.scheduleReminders(card, configs)
                }
                inactivityScheduler.schedule()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
