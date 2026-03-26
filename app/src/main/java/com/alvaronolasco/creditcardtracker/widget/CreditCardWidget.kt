package com.alvaronolasco.creditcardtracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.ImageProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.alvaronolasco.creditcardtracker.MainActivity
import com.alvaronolasco.creditcardtracker.data.AppDatabase
import com.alvaronolasco.creditcardtracker.data.entity.CreditCard
import com.alvaronolasco.creditcardtracker.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WidgetCardData(
    val card: CreditCard,
    val totalSpent: Double,
    val dateInfo: WidgetDateInfo
) {
    val totalDue: Double = totalSpent + card.extraFinancingPayment
    val progress: Float = if (card.creditLimit > 0)
        (totalDue / card.creditLimit).toFloat().coerceIn(0f, 1f)
    else 0f
}

class CreditCardWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 120.dp),  // SMALL: compacto ~2x2
            DpSize(250.dp, 120.dp),  // MEDIUM: ~4x2
            DpSize(250.dp, 240.dp)   // LARGE: ~4x4
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
        val cards = db.creditCardDao().getAllCards().first()

        val cardDataList = cards.map { card ->
            val (start, end) = DateUtils.getCurrentPeriodRange(card.cutOffDay)
            val spent = db.expenseDao().getTotalSpentInPeriod(card.id, start, end).first() ?: 0.0
            val dateInfo = WidgetDateHelper.getWidgetDateInfo(card)
            WidgetCardData(card, spent, dateInfo)
        }

        provideContent {
            GlanceTheme {
                WidgetContent(cardDataList, LocalSize.current, context)
            }
        }
    }

    @Composable
    private fun WidgetContent(cards: List<WidgetCardData>, size: DpSize, context: Context) {
        if (cards.isEmpty()) {
            EmptyWidgetContent(context)
            return
        }

        val isSmall = size.width < 250.dp
        val isLarge = size.height >= 240.dp
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(WidgetColors.widgetBackground)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity(launchIntent))
        ) {
            when {
                isSmall -> SmallLayout(cards, context)
                isLarge -> LargeLayout(cards, context)
                else -> MediumLayout(cards, context)
            }
        }
    }

    // ─── SMALL LAYOUT ───────────────────────────────────────────────────────

    @Composable
    private fun SmallLayout(cards: List<WidgetCardData>, context: Context) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Tarjetas",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = WidgetColors.textPrimary
                )
            )
            Spacer(GlanceModifier.height(8.dp))
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(cards, itemId = { it.card.id.toLong() }) { data ->
                    Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 5.dp, horizontal = 8.dp)) {
                        SmallCardRow(data)
                    }
                }
            }
        }
    }

    @Composable
    private fun SmallCardRow(data: WidgetCardData) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .width(4.dp)
                    .height(28.dp)
                    .cornerRadius(2.dp)
                    .background(ImageProvider(WidgetColors.cardGradientDrawable(data.card.color)))
            ) {}
            Spacer(GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = data.card.name,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = WidgetColors.textPrimary
                    )
                )
            }
            Text(
                text = "${data.dateInfo.label}: ${data.dateInfo.dateText}",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = if (data.dateInfo.isUrgent) WidgetColors.urgentColor else WidgetColors.textSecondary
                )
            )
        }
    }

    // ─── MEDIUM LAYOUT ──────────────────────────────────────────────────────

    @Composable
    private fun MediumLayout(cards: List<WidgetCardData>, context: Context) {
        LazyColumn(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
            items(cards, itemId = { it.card.id.toLong() }) { data ->
                Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 6.dp)) {
                    MiniCardRow(data, context)
                }
            }
        }
    }

    @Composable
    private fun MiniCardRow(data: WidgetCardData, context: Context) {
        val addExpenseIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("card_id", data.card.id)
        }
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(56.dp)
                .cornerRadius(14.dp)
                .background(ImageProvider(WidgetColors.cardGradientDrawable(data.card.color)))
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Contenido principal
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lado izquierdo: banco + nombre
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = data.card.bank,
                                style = TextStyle(
                                    fontSize = 9.sp,
                                    color = WidgetColors.textOnCardSecondary
                                )
                            )
                            Spacer(GlanceModifier.defaultWeight())
                        }
                        Text(
                            text = data.card.name,
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = WidgetColors.textOnCard
                            )
                        )
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    // Lado derecho: digitos + fecha + boton +
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "**** ${data.card.lastFourDigits}",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    color = WidgetColors.textOnCardSecondary
                                )
                            )
                            Spacer(GlanceModifier.width(6.dp))
                            Box(
                                modifier = GlanceModifier
                                    .width(20.dp)
                                    .height(14.dp)
                                    .cornerRadius(6.dp)
                                    .background(ColorProvider(WidgetColors.chipOverlay))
                                    .clickable(actionStartActivity(addExpenseIntent)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = WidgetColors.textOnCard
                                    )
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${data.dateInfo.label} ${data.dateInfo.dayNumber}",
                                style = TextStyle(
                                    fontSize = 9.sp,
                                    color = WidgetColors.textOnCardSecondary
                                )
                            )
                            Text(
                                text = " · ${data.dateInfo.dateText}",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = if (data.dateInfo.isUrgent)
                                        ColorProvider(WidgetColors.urgentOnCard)
                                    else
                                        WidgetColors.textOnCard
                                )
                            )
                        }
                    }
                }
                // Barra de progreso de saldo al fondo de la tarjeta
                CreditUsageProgressBar(
                    progress = data.progress,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .padding(horizontal = 10.dp)
                )
                Spacer(GlanceModifier.height(4.dp))
            }
        }
    }

    // ─── LARGE LAYOUT ───────────────────────────────────────────────────────

    @Composable
    private fun LargeLayout(cards: List<WidgetCardData>, context: Context) {
        LazyColumn(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
            items(cards, itemId = { it.card.id.toLong() }) { data ->
                Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 6.dp)) {
                    FullCardItem(data, context)
                }
            }
        }
    }

    @Composable
    private fun FullCardItem(data: WidgetCardData, context: Context) {
        val daysShort = when (data.dateInfo.daysRemaining) {
            0 -> "HOY"
            1 -> "1d"
            else -> "${data.dateInfo.daysRemaining}d"
        }
        val badgeText = "${data.dateInfo.label} ${data.dateInfo.dayNumber} · $daysShort"
        val addExpenseIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("card_id", data.card.id)
        }
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(84.dp)
                .cornerRadius(16.dp)
                .background(ImageProvider(WidgetColors.cardGradientDrawable(data.card.color)))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Fila 1: banco + boton +
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.card.bank,
                        style = TextStyle(
                            fontSize = 9.sp,
                            color = WidgetColors.textOnCardSecondary
                        )
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Box(
                        modifier = GlanceModifier
                            .width(24.dp)
                            .height(16.dp)
                            .cornerRadius(8.dp)
                            .background(ColorProvider(WidgetColors.chipOverlay))
                            .clickable(actionStartActivity(addExpenseIntent)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = WidgetColors.textOnCard
                            )
                        )
                    }
                }
                Spacer(GlanceModifier.height(2.dp))
                // Fila 2: gasto + ultimos 4 digitos
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${formatCurrency(data.totalSpent)}",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = WidgetColors.textOnCard
                        )
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        text = "**** ${data.card.lastFourDigits}",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = WidgetColors.textOnCardSecondary
                        )
                    )
                }
                Spacer(GlanceModifier.height(5.dp))
                // Barra de progreso de saldo
                CreditUsageProgressBar(
                    progress = data.progress,
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp)
                )
                Spacer(GlanceModifier.defaultWeight())
                // Fila 3: nombre + badge fecha
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.card.name,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = WidgetColors.textOnCard
                        )
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Box(
                        modifier = GlanceModifier
                            .cornerRadius(8.dp)
                            .background(ColorProvider(WidgetColors.badgeOverlay))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = if (data.dateInfo.isUrgent)
                                    ColorProvider(WidgetColors.urgentOnCard)
                                else
                                    WidgetColors.textOnCard
                            )
                        )
                    }
                }
            }
        }
    }

    // ─── EMPTY STATE ────────────────────────────────────────────────────────

    @Composable
    private fun EmptyWidgetContent(context: Context) {
        val launchIntent = Intent(context, MainActivity::class.java)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(WidgetColors.widgetBackground)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity(launchIntent)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Agrega una tarjeta",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = WidgetColors.textSecondary
                )
            )
        }
    }

    // ─── PROGRESS BAR ───────────────────────────────────────────────────────

    @Composable
    private fun CreditUsageProgressBar(progress: Float, modifier: GlanceModifier = GlanceModifier) {
        val fillColor = when {
            progress >= 0.95f -> Color(0xFFFF6B6B)
            progress >= 0.80f -> Color(0xFFFFCC80)
            else              -> Color.White
        }

        // Usamos una fila completa para la barra lo que permite que sea una sola entidad visual continua
        Row(
            modifier = modifier
                .background(ColorProvider(Color.White.copy(alpha = 0.25f)))
                .cornerRadius(3.dp)
        ) {
            if (progress > 0) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight(progress)
                        .fillMaxHeight()
                        .background(ColorProvider(fillColor))
                        .cornerRadius(3.dp)
                ) {}
            }
            if (progress < 1f) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight(1f - progress)
                        .fillMaxHeight()
                ) {}
            }
        }
    }

    // ─── HELPERS ────────────────────────────────────────────────────────────

    private fun formatCurrency(amount: Double): String = String.format("%,.2f", amount)
}

class CreditCardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CreditCardWidget()

    companion object {
        suspend fun updateAllWidgets(context: Context) {
            try {
                val manager = GlanceAppWidgetManager(context)
                manager.getGlanceIds(CreditCardWidget::class.java).forEach { id ->
                    CreditCardWidget().update(context, id)
                }
            } catch (e: Exception) {
                // Silently catch widget update errors to avoid disrupting the main flow
            }
        }
    }
}
