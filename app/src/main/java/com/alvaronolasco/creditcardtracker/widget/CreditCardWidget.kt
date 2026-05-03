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
    val availableCredit: Double = (card.creditLimit - totalDue).coerceAtLeast(0.0)
}

data class WidgetIncomeData(
    val totalIncome: Double,
    val totalSpent: Double
) {
    val progress: Float = if (totalIncome > 0)
        (totalSpent / totalIncome).toFloat().coerceIn(0f, 1f)
    else 0f
    val remaining: Double = (totalIncome - totalSpent).coerceAtLeast(0.0)
}

data class WidgetUiState(
    val cards: List<WidgetCardData>,
    val income: WidgetIncomeData
)

class CreditCardWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 120.dp),
            DpSize(210.dp, 120.dp),
            DpSize(210.dp, 210.dp)
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
                .padding(bottom = 6.dp)
        ) {
            IncomeSummaryMiniCard(state.income)
            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                items(state.cards, itemId = { it.card.id.toLong() }) { data ->
                    SmallCardRow(data)
                }
            }
        }
    }

    @Composable
    private fun SmallCardRow(data: WidgetCardData) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = GlanceModifier
                        .width(4.dp)
                        .height(24.dp)
                        .cornerRadius(2.dp)
                        .background(WidgetColors.cardColor(data.card.color))
                ) {}
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = data.card.name,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = WidgetColors.textPrimary
                    )
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = "$${formatCurrency(data.totalDue)}",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = WidgetColors.textPrimary
                    )
                )
            }
            Text(
                text = "${data.dateInfo.label} ${data.dateInfo.dayNumber} · ${data.dateInfo.dateText}",
                modifier = GlanceModifier.padding(start = 12.dp),
                style = TextStyle(
                    fontSize = 9.sp,
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
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            item {
                IncomeSummaryCard(state.income, isLarge = false)
            }
            items(state.cards, itemId = { it.card.id.toLong() }) { data ->
                Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 5.dp, horizontal = 6.dp)) {
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
                .height(80.dp)
                .cornerRadius(14.dp)
                .background(WidgetColors.cardColor(data.card.color))
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Fila 1: banco + boton +
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.card.bank,
                        style = TextStyle(fontSize = 9.sp, color = WidgetColors.textOnCardSecondary)
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Box(
                        modifier = GlanceModifier
                            .width(28.dp)
                            .height(18.dp)
                            .cornerRadius(6.dp)
                            .background(ColorProvider(WidgetColors.chipOverlay))
                            .clickable(actionStartActivity(addExpenseIntent)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = WidgetColors.textOnCard)
                        )
                    }
                }
                // Fila 2: nombre tarjeta + ultimos 4 digitos
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp),
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
                    Text(
                        text = "**** ${data.card.lastFourDigits}",
                        style = TextStyle(fontSize = 9.sp, color = WidgetColors.textOnCardSecondary)
                    )
                }
                Spacer(GlanceModifier.height(4.dp))
                // Fila 3: consumo + badge fecha
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${formatCurrency(data.totalDue)} / $${formatCurrency(data.card.creditLimit)}",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = WidgetColors.textOnCard
                        )
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    DateBadge(data.dateInfo)
                }
                Spacer(GlanceModifier.height(6.dp))
                // Barra de progreso
                CreditUsageProgressBar(
                    progress = data.progress,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .padding(horizontal = 12.dp)
                )
                Spacer(GlanceModifier.height(8.dp))
            }
        }
    }

    // ─── LARGE LAYOUT ───────────────────────────────────────────────────────

    @Composable
    private fun LargeLayout(state: WidgetUiState, context: Context) {
        LazyColumn(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            item {
                IncomeSummaryCard(state.income, isLarge = true)
            }
            items(state.cards, itemId = { it.card.id.toLong() }) { data ->
                Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 5.dp, horizontal = 6.dp)) {
                    FullCardItem(data, context)
                }
            }
        }
    }

    @Composable
    private fun FullCardItem(data: WidgetCardData, context: Context) {
        val addExpenseIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("card_id", data.card.id)
        }
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(120.dp)
                .cornerRadius(16.dp)
                .background(WidgetColors.cardColor(data.card.color))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Fila 1: banco + boton +
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.card.bank,
                        style = TextStyle(fontSize = 9.sp, color = WidgetColors.textOnCardSecondary)
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Box(
                        modifier = GlanceModifier
                            .width(28.dp)
                            .height(18.dp)
                            .cornerRadius(8.dp)
                            .background(ColorProvider(WidgetColors.chipOverlay))
                            .clickable(actionStartActivity(addExpenseIntent)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = WidgetColors.textOnCard)
                        )
                    }
                }
                Spacer(GlanceModifier.height(6.dp))
                // Fila 2: monto hero
                Text(
                    text = "$${formatCurrency(data.totalDue)}",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = WidgetColors.textOnCard
                    )
                )
                // Fila 3: contexto de limite + disponible
                Text(
                    text = "de $${formatCurrency(data.card.creditLimit)}  ·  Disp: $${formatCurrency(data.availableCredit)}",
                    style = TextStyle(fontSize = 9.sp, color = WidgetColors.textOnCardSecondary)
                )
                Spacer(GlanceModifier.height(8.dp))
                // Barra de progreso
                CreditUsageProgressBar(
                    progress = data.progress,
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp)
                )
                Spacer(GlanceModifier.defaultWeight())
                // Fila 4: nombre tarjeta + badge fecha
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
                    DateBadge(data.dateInfo)
                }
            }
        }
    }

    // ─── DATE BADGE ─────────────────────────────────────────────────────────

    @Composable
    private fun DateBadge(dateInfo: WidgetDateInfo) {
        val daysShort = when (dateInfo.daysRemaining) {
            0 -> "HOY"
            1 -> "1d"
            else -> "${dateInfo.daysRemaining}d"
        }
        val badgeText = "${dateInfo.label} ${dateInfo.dayNumber} · $daysShort"
        val bgColor = when {
            dateInfo.isUrgent -> ColorProvider(WidgetColors.urgentBadgeBg)
            dateInfo.daysRemaining > 7 -> ColorProvider(WidgetColors.safeBadgeBg)
            else -> ColorProvider(WidgetColors.badgeOverlay)
        }
        Box(
            modifier = GlanceModifier
                .cornerRadius(6.dp)
                .background(bgColor)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = badgeText,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    color = if (dateInfo.isUrgent) ColorProvider(WidgetColors.urgentOnCard) else WidgetColors.textOnCard
                )
            )
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = GlanceModifier.padding(16.dp)
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(40.dp)
                        .cornerRadius(12.dp)
                        .background(WidgetColors.incomeBrand),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = WidgetColors.textOnCard
                        )
                    )
                }
                Spacer(GlanceModifier.height(10.dp))
                Text(
                    text = "Sin tarjetas aún",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = WidgetColors.textPrimary
                    )
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = "Toca para agregar",
                    style = TextStyle(fontSize = 11.sp, color = WidgetColors.textSecondary)
                )
            }
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
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Presupuesto Mensual",
                        style = TextStyle(fontSize = 10.sp, color = WidgetColors.textOnCardSecondary)
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    if (isLarge) {
                        val remainingColor = ColorProvider(
                            when {
                                data.remaining <= 0 -> WidgetColors.progressDanger
                                data.progress >= 0.8f -> WidgetColors.progressWarning
                                else -> WidgetColors.progressSafe
                            }
                        )
                        Text(
                            text = "Restan: $${formatCurrency(data.remaining)}",
                            style = TextStyle(fontSize = 9.sp, color = remainingColor)
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
                        style = TextStyle(fontSize = 11.sp, color = WidgetColors.textOnCardSecondary)
                    )
                }
                Spacer(GlanceModifier.height(8.dp))
                LinearProgressIndicator(
                    progress = data.progress,
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(3.dp),
                    color = ColorProvider(semanticProgressColor(data.progress)),
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
                .padding(bottom = 10.dp)
                .cornerRadius(24.dp)
                .background(WidgetColors.incomeBrand)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Gasto Mensual",
                        style = TextStyle(fontSize = 9.sp, color = WidgetColors.textOnCardSecondary)
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    val pctColor = ColorProvider(semanticProgressColor(data.progress))
                    Text(
                        text = "${(data.progress * 100).toInt()}%",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = pctColor
                        )
                    )
                }
                Spacer(GlanceModifier.height(4.dp))
                LinearProgressIndicator(
                    progress = data.progress,
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp),
                    color = ColorProvider(semanticProgressColor(data.progress)),
                    backgroundColor = ColorProvider(Color.White.copy(alpha = 0.25f))
                )
            }
        }
    }

    // ─── PROGRESS BAR ───────────────────────────────────────────────────────

    @Composable
    private fun CreditUsageProgressBar(progress: Float, modifier: GlanceModifier = GlanceModifier) {
        LinearProgressIndicator(
            progress = progress,
            modifier = modifier.cornerRadius(3.dp),
            color = ColorProvider(semanticProgressColor(progress)),
            backgroundColor = ColorProvider(Color.White.copy(alpha = 0.25f))
        )
    }

    private fun semanticProgressColor(progress: Float): Color = when {
        progress >= 0.85f -> WidgetColors.progressDanger
        progress >= 0.70f -> WidgetColors.progressWarning
        progress >= 0.50f -> WidgetColors.progressAttention
        else              -> WidgetColors.progressSafe
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
