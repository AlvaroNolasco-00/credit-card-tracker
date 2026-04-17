package com.alvaronolasco.creditcardtracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository
import com.alvaronolasco.creditcardtracker.util.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: CreditCardRepository

    companion object {
        const val EXTRA_CARD_NAME = "cardName"
        const val EXTRA_BANK = "bank"
        const val EXTRA_LAST_FOUR = "lastFour"
        const val EXTRA_CARD_COLOR = "cardColor"
        const val EXTRA_NOTIFICATION_TYPE = "notificationType"
        const val EXTRA_DAYS_BEFORE = "daysBefore"
        const val EXTRA_EVENT_DATE = "eventDate"
        const val EXTRA_ID = "id"
        const val EXTRA_CARD_ID = "cardId"
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

        if (data.notificationType == ReminderScheduler.TYPE_OVERDUE) {
            val cardId = intent.getIntExtra(EXTRA_CARD_ID, -1)
            if (cardId == -1) return
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val card = repository.getCardById(cardId) ?: return@launch
                    val (_, prevEnd) = DateUtils.getPreviousPeriodRange(card.cutOffDay)
                    val isPaid = card.lastPaymentDate > prevEnd
                    if (!isPaid && DateUtils.getDaysOverduePayment(card.cutOffDay, card.paymentDueDay) > 0) {
                        NotificationHelper(context).showNotification(data)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            NotificationHelper(context).showNotification(data)
        }
    }
}
