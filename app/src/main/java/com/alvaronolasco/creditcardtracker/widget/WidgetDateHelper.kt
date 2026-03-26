package com.alvaronolasco.creditcardtracker.widget

import com.alvaronolasco.creditcardtracker.data.entity.CreditCard
import com.alvaronolasco.creditcardtracker.util.DateUtils

enum class DateInfoType { CUTOFF, PAYMENT }

data class WidgetDateInfo(
    val label: String,
    val dateText: String,
    val dayNumber: Int,
    val daysRemaining: Int,
    val isUrgent: Boolean,
    val type: DateInfoType
)

object WidgetDateHelper {

    fun getWidgetDateInfo(card: CreditCard): WidgetDateInfo {
        val daysUntilCutOff = DateUtils.getDaysUntil(card.cutOffDay)
        val daysUntilPayment = DateUtils.getDaysUntil(card.paymentDueDay)

        return if (daysUntilPayment < daysUntilCutOff) {
            // La tarjeta ya corto -> mostrar info de PAGO
            WidgetDateInfo(
                label = "Pago",
                dateText = formatDays(daysUntilPayment),
                dayNumber = card.paymentDueDay,
                daysRemaining = daysUntilPayment,
                isUrgent = daysUntilPayment <= 2,
                type = DateInfoType.PAYMENT
            )
        } else {
            // Acercandose al corte -> mostrar info de CORTE
            WidgetDateInfo(
                label = "Corte",
                dateText = formatDays(daysUntilCutOff),
                dayNumber = card.cutOffDay,
                daysRemaining = daysUntilCutOff,
                isUrgent = daysUntilCutOff <= 2,
                type = DateInfoType.CUTOFF
            )
        }
    }

    private fun formatDays(days: Int): String = when (days) {
        0 -> "HOY"
        1 -> "Manana"
        else -> "En $days dias"
    }
}
