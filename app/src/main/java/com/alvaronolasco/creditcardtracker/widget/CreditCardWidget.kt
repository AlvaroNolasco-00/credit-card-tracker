package com.alvaronolasco.creditcardtracker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
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

data class WidgetIncomeData(
    val totalIncome: Double,
    val totalSpent: Double
) {
    val progress: Float = if (totalIncome > 0)
        (totalSpent / totalIncome).toFloat().coerceIn(0f, 1f)
    else 0f
}

data class WidgetUiState(
    val cards: List<WidgetCardData>,
    val income: WidgetIncomeData
)

class CreditCardWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 120.dp),  // SMALL: compacto ~2x2
            DpSize(210.dp, 120.dp),  // MEDIUM: ~4x2
            DpSize(210.dp, 210.dp)   // LARGE: ~4x4
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

        val currentMonthYear = DateUtils.getCurrentMonthYear()
        val monthlyIncome = db.incomeDao().getTotalIncomeForMonth(currentMonthYear).first() ?: 0.0
        val totalSpentAllCards = cardDataList.sumOf { it.totalDue }
        val incomeData = WidgetIncomeData(monthlyIncome, totalSpentAllCards)

        provideContent {
            GlanceTheme {
                WidgetContent(WidgetUiState(cardDataList, incomeData), LocalSize.current, context)
            }
        }
    }

    @Composable
    private fun WidgetContent(state: WidgetUiState, size: DpSize, context: Context) {
        if (state.cards.isEmpty() && state.income.totalIncome == 0.0) {
            EmptyWidgetContent(context)
            return
        }

        val isSmall = size.width < 210.dp
        val isLarge = size.height >= 210.dp
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
                isSmall -> SmallLayout(state)
                isLarge -> LargeLayout(state, context)
                else -> MediumLayout(state, context)
            }
        }
    }

    // ─── SMALL LAYOUT ───────────────────────────────────────────────────────

    @Composable
    private fun SmallLayout(state: WidgetUiState) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(bottom = 8.dp)
        ) {
            // New Income Card for Small
            IncomeSummaryMiniCard(state.income)

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Tarjetas",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = WidgetColors.textPrimary
                    )
                )
                Spacer(GlanceModifier.height(4.dp))
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(state.cards, itemId = { it.card.id.toLong() }) { data ->
                        Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp)) {
                            SmallCardRow(data)
                        }
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
    private fun MediumLayout(state: WidgetUiState, context: Context) {
        LazyColumn(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            item {
                IncomeSummaryCard(state.income, isLarge = false)
            }
            items(state.cards, itemId = { it.card.id.toLong() }) { data ->
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
                .height(72.dp)
                .cornerRadius(14.dp)
                .background(ImageProvider(WidgetColors.cardGradientDrawable(data.card.color)))
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Contenido principal
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
    private fun LargeLayout(state: WidgetUiState, context: Context) {
        LazyColumn(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
            item {
                IncomeSummaryCard(state.income, isLarge = true)
            }
            items(state.cards, itemId = { it.card.id.toLong() }) { data ->
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
                .height(120.dp)
                .cornerRadius(16.dp)
                .background(ImageProvider(WidgetColors.cardGradientDrawable(data.card.color)))
                .padding(horizontal = 12.dp, vertical = 12.dp)
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
                        text = "$${formatCurrency(data.totalDue)}",
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
                Spacer(GlanceModifier.height(10.dp))
                // Barra de progreso de saldo
                CreditUsageProgressBar(
                    progress = data.progress,
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp)
                )
                Spacer(GlanceModifier.height(10.dp))
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
                text = "Sin datos de ingresos o tarjetas",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = WidgetColors.textSecondary
                )
            )
        }
    }

    // ─── INCOME SUMMARY CARD ────────────────────────────────────────────────

    @Composable
    private fun IncomeSummaryCard(data: WidgetIncomeData, isLarge: Boolean) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(if (isLarge) 110.dp else 90.dp)
                .padding(vertical = 6.dp, horizontal = 6.dp)
                .cornerRadius(16.dp)
                .background(WidgetColors.incomeBrand)
                .padding(14.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Presupuesto Mensual",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = WidgetColors.textOnCardSecondary
                        )
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    if (isLarge) {
                        Text(
                            text = "Presupuesto total: $${formatCurrency(data.totalIncome)}",
                            style = TextStyle(
                                fontSize = 9.sp,
                                color = WidgetColors.textOnCardSecondary
                            )
                        )
                    }
                }
                
                Spacer(GlanceModifier.defaultWeight())
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${formatCurrency(data.totalSpent)}",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = WidgetColors.textOnCard
                        )
                    )
                    Text(
                        text = " de $${formatCurrency(data.totalIncome)}",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = WidgetColors.textOnCardSecondary
                        )
                    )
                }
                
                Spacer(GlanceModifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = data.progress,
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(3.dp),
                    color = ColorProvider(Color.White),
                    backgroundColor = ColorProvider(Color.White.copy(alpha = 0.25f))
                )
            }
        }
    }

    @Composable
    private fun IncomeSummaryMiniCard(data: WidgetIncomeData) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(bottom = 12.dp)
                .cornerRadius(24.dp)
                .background(WidgetColors.incomeBrand)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Gasto Mensual",
                        style = TextStyle(fontSize = 9.sp, color = WidgetColors.textOnCardSecondary)
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        text = "${(data.progress * 100).toInt()}%",
                        style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = WidgetColors.textOnCard)
                    )
                }
                Spacer(GlanceModifier.height(4.dp))
                LinearProgressIndicator(
                    progress = data.progress,
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp),
                    color = ColorProvider(Color.White),
                    backgroundColor = ColorProvider(Color.White.copy(alpha = 0.25f))
                )
            }
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

        LinearProgressIndicator(
            progress = progress,
            modifier = modifier.cornerRadius(3.dp),
            color = ColorProvider(fillColor),
            backgroundColor = ColorProvider(Color.White.copy(alpha = 0.25f))
        )
    }

    // ─── HELPERS ────────────────────────────────────────────────────────────

    private fun formatCurrency(amount: Double): String = String.format("%,.2f", amount)
}

class CreditCardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CreditCardWidget()

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, CreditCardWidgetReceiver::class.java)
            )
            if (ids.isNotEmpty()) {
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                    component = ComponentName(context, CreditCardWidgetReceiver::class.java)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
