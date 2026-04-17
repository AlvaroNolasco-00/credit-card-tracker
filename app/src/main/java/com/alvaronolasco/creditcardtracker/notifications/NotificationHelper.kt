package com.alvaronolasco.creditcardtracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.alvaronolasco.creditcardtracker.MainActivity
import com.alvaronolasco.creditcardtracker.R

/**
 * Datos necesarios para construir una notificación personalizada con la tarjeta.
 *
 * @param id              ID único de la notificación (hash de cardId+type+daysBefore)
 * @param cardName        Nombre de la tarjeta (e.g. "Visa Signature")
 * @param bank            Banco emisor (e.g. "BBVA")
 * @param lastFour        Últimos 4 dígitos
 * @param cardColor       Color ARGB de la tarjeta (CreditCard.color)
 * @param notificationType "CUT_OFF" o "PAYMENT"
 * @param daysBefore      Días de anticipación del recordatorio
 * @param eventDate       Fecha formateada del evento (e.g. "viernes, 18 de abril")
 */
data class CardNotificationData(
    val id: Int,
    val cardName: String,
    val bank: String,
    val lastFour: String,
    val cardColor: Int,
    val notificationType: String,
    val daysBefore: Int,
    val eventDate: String
)

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "reminder_channel"
        const val CHANNEL_NAME = "Recordatorios de Tarjeta"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para cortes y pagos de tarjeta"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(data: CardNotificationData) {
        val pendingIntent = buildPendingIntent()

        val isOverdue = data.notificationType == ReminderScheduler.TYPE_OVERDUE
        val isCutOff = data.notificationType == "CUT_OFF"

        val eventLabel = when {
            isOverdue -> "Pago vencido"
            isCutOff -> "Corte"
            else -> "Pago"
        }
        val badgeLabel = when {
            isOverdue -> "VENCIDO"
            isCutOff -> "CORTE"
            else -> "PAGO"
        }

        val daysText = when {
            isOverdue -> "Llevas ${data.daysBefore} día(s) sin registrar tu pago"
            data.daysBefore == 0 -> "¡Hoy!"
            data.daysBefore == 1 -> "Mañana"
            else -> "En ${data.daysBefore} días"
        }

        val contentTitle = "$eventLabel · ${data.cardName}"
        val contentText = if (isOverdue) "Si ya pagaste, ábrelo en la app y regístralo" else "$daysText — ${data.eventDate}"

        // --- Large icon: card-shaped bitmap (visible in collapsed notification) ---
        val largeIconBitmap = CardBitmapHelper.generateLargeIcon(
            cardName = data.cardName,
            bank = data.bank,
            lastFour = data.lastFour,
            cardColor = data.cardColor
        )

        // --- Expanded custom view (RemoteViews) ---
        val expandedView = buildExpandedView(data, badgeLabel, daysText)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_card)
            .setLargeIcon(largeIconBitmap)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setColor(data.cardColor)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomBigContentView(expandedView)
            .build()

        notificationManager.notify(data.id, notification)
    }

    private fun buildExpandedView(
        data: CardNotificationData,
        badgeLabel: String,
        daysText: String
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.notification_expanded)

        // Card thumbnail bitmap
        val cardBitmap = CardBitmapHelper.generate(
            cardName = data.cardName,
            bank = data.bank,
            lastFour = data.lastFour,
            cardColor = data.cardColor,
            widthPx = 240,
            heightPx = 150
        )
        views.setImageViewBitmap(R.id.notif_card_image, cardBitmap)

        // Badge type text + color accent (red for overdue)
        views.setTextViewText(R.id.notif_type_badge, badgeLabel)
        val badgeColor = if (data.notificationType == ReminderScheduler.TYPE_OVERDUE)
            android.graphics.Color.parseColor("#E53935") else data.cardColor
        views.setTextColor(R.id.notif_type_badge, badgeColor)

        // Card name
        views.setTextViewText(R.id.notif_card_name, "${data.cardName} · ${data.bank}")

        // Message: "En 3 días" / "Mañana" / "¡Hoy!" / "Llevas N días sin registrar..."
        views.setTextViewText(R.id.notif_message, daysText)

        // Event date (hidden for overdue — no specific event date)
        val dateText = if (data.notificationType == ReminderScheduler.TYPE_OVERDUE)
            "Registra tu pago en la app"
        else data.eventDate.replaceFirstChar { it.uppercase() }
        views.setTextViewText(R.id.notif_date, dateText)

        return views
    }

    private fun buildPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }
}
