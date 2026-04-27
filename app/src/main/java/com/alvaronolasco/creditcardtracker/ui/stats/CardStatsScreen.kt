package com.alvaronolasco.creditcardtracker.ui.stats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaronolasco.creditcardtracker.ui.components.AppLoadingIndicator
import com.alvaronolasco.creditcardtracker.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardStatsScreen(
    cardId: Int,
    onBack: () -> Unit,
    viewModel: CardStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Estadísticas de Uso", style = MaterialTheme.typography.titleMedium)
                        uiState.card?.let {
                            Text(it.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            AppLoadingIndicator(Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(8.dp))

                RangeFilterChips(
                    selected = uiState.monthCount,
                    options = listOf(
                        "1M" to 1,
                        "3M" to 3,
                        "6M" to 6,
                        "1A" to 12
                    ),
                    onSelected = { viewModel.selectMonthCount(it) }
                )

                Spacer(Modifier.height(16.dp))

                uiState.summary?.let { summary ->
                    KpiSummaryRow(
                        summary = summary,
                        selectedPeriod = uiState.periods.getOrNull(uiState.selectedPeriodIndex),
                        previousPeriod = uiState.periods.getOrNull(uiState.selectedPeriodIndex - 1),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (uiState.insights.isNotEmpty()) {
                    InsightsCarousel(
                        insights = uiState.insights,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                }

                StatsChartSection(
                    periods = uiState.periods,
                    selectedIndex = uiState.selectedPeriodIndex,
                    onPeriodSelected = { viewModel.selectPeriod(it) }
                )

                Spacer(Modifier.height(24.dp))

                PaymentsVsExpensesSection(
                    periods = uiState.periods,
                    selectedIndex = uiState.selectedPeriodIndex,
                    onPeriodSelected = { viewModel.selectPeriod(it) }
                )

                Spacer(Modifier.height(24.dp))

                val selectedPeriod = uiState.periods.getOrNull(uiState.selectedPeriodIndex)
                var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

                AnimatedContent(
                    targetState = selectedPeriod,
                    label = "period_detail",
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { period ->
                    if (period != null) {
                        Column {
                            PeriodDetailSection(period)

                            Spacer(Modifier.height(24.dp))

                            CategoryBreakdownSection(
                                categories = period.categoryBreakdown,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )

                            Spacer(Modifier.height(24.dp))

                            Text(
                                "Calendario del Periodo",
                                modifier = Modifier.padding(horizontal = 20.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Días con movimientos registrados",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                CalendarLegend()
                            }

                            Spacer(Modifier.height(12.dp))

                            UsageCalendar(
                                period = period,
                                selectedDay = selectedDay,
                                onDaySelected = { selectedDay = it }
                            )

                            Spacer(Modifier.height(24.dp))

                            AnimatedContent(
                                targetState = selectedDay,
                                label = "day_expenses",
                                transitionSpec = {
                                    fadeIn() + slideInVertically { it / 2 } togetherWith
                                    fadeOut() + slideOutVertically { it / 2 }
                                }
                            ) { day ->
                                if (day != null) {
                                    val dayExpenses = period.expenseDetails.filter { expense ->
                                        val expenseDate = Instant.ofEpochMilli(expense.expense.date)
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDate()
                                        expenseDate == day
                                    }

                                    Column {
                                        Text(
                                            "Gastos del ${day.dayOfMonth} de ${day.month.getDisplayName(TextStyle.FULL, Locale("es"))}",
                                            modifier = Modifier.padding(horizontal = 20.dp),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(Modifier.height(12.dp))

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                if (dayExpenses.isEmpty()) {
                                                    EmptyDayState(day = day)
                                                } else {
                                                    dayExpenses.forEach { expense ->
                                                        val expenseTime = Instant.ofEpochMilli(expense.expense.date)
                                                            .atZone(ZoneId.systemDefault())
                                                            .toLocalTime()

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                val category = expense.categories.firstOrNull()
                                                                val catColor = if (category != null) {
                                                                    val hash = category.name.hashCode()
                                                                    Color(
                                                                        (hash and 0xFF0000 shr 16).coerceIn(80, 200),
                                                                        (hash and 0x00FF00 shr 8).coerceIn(80, 200),
                                                                        (hash and 0x0000FF).coerceIn(80, 200)
                                                                    )
                                                                } else MaterialTheme.colorScheme.outline

                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(36.dp)
                                                                        .clip(CircleShape)
                                                                        .background(catColor.copy(alpha = 0.15f)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        category?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                                                        fontSize = 14.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = catColor
                                                                    )
                                                                }
                                                                Spacer(Modifier.width(12.dp))
                                                                Column {
                                                                    Text(
                                                                        expense.expense.description,
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        fontWeight = FontWeight.Medium
                                                                    )
                                                                    Text(
                                                                        String.format("%02d:%02d", expenseTime.hour, expenseTime.minute),
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            }
                                                            Text(
                                                                "$${String.format("%,.2f", expense.expense.amount)}",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = ForestGreen
                                                            )
                                                        }

                                                        if (expense != dayExpenses.last()) {
                                                            HorizontalDivider(
                                                                modifier = Modifier.padding(vertical = 8.dp),
                                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                                            )
            }
        }
    }
}

                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun EmptyDayState(day: LocalDate, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "No hay gastos registrados este día",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun RangeFilterChips(
    selected: Int,
    options: List<Pair<String, Int>>,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, count) ->
            val isSelected = selected == count
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(count) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ForestGreen.copy(alpha = 0.15f),
                    selectedLabelColor = ForestGreen,
                    selectedLeadingIconColor = ForestGreen
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = ForestGreen.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun KpiSummaryRow(
    summary: PeriodsSummary,
    selectedPeriod: PeriodStats?,
    previousPeriod: PeriodStats?,
    modifier: Modifier = Modifier
) {
    val trendPercent = remember(selectedPeriod, previousPeriod) {
        if (selectedPeriod != null && previousPeriod != null && previousPeriod.totalExpenses > 0) {
            ((selectedPeriod.totalExpenses - previousPeriod.totalExpenses) / previousPeriod.totalExpenses * 100).toFloat()
        } else null
    }

    Column(modifier = modifier) {
        Text(
            "Resumen",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard(
                    label = "Promedio mensual",
                    value = "$${String.format("%,.0f", summary.avgMonthlyExpense)}",
                    icon = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    label = "Mes con más gasto",
                    value = summary.maxExpensePeriodLabel,
                    subtitle = "$${String.format("%,.0f", summary.maxMonthlyExpense)}",
                    icon = Icons.Default.EmojiEvents,
                    modifier = Modifier.weight(1f)
                )
            }

            if (trendPercent != null || (selectedPeriod != null && selectedPeriod.creditUtilizationPercent > 0)) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (trendPercent != null) {
                        val isPositiveTrend = trendPercent < 0
                        KpiCard(
                            label = "vs mes anterior",
                            value = "${if (trendPercent > 0) "+" else ""}${String.format("%.1f", trendPercent)}%",
                            icon = if (isPositiveTrend) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                            valueColor = if (isPositiveTrend) ForestGreen else MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (selectedPeriod != null && selectedPeriod.creditUtilizationPercent > 0) {
                        KpiCard(
                            label = "Utilización",
                            value = "${String.format("%.0f", selectedPeriod.creditUtilizationPercent * 100)}%",
                            icon = Icons.Default.CreditCard,
                            valueColor = when {
                                selectedPeriod.creditUtilizationPercent > 0.8f -> MaterialTheme.colorScheme.error
                                selectedPeriod.creditUtilizationPercent > 0.5f -> Color(0xFFFFA000)
                                else -> ForestGreen
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                fontSize = 22.sp
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatsChartSection(
    periods: List<PeriodStats>,
    selectedIndex: Int,
    onPeriodSelected: (Int) -> Unit
) {
    if (periods.isEmpty()) return

    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            "Gastos por Periodo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        var canvasSize by remember { mutableStateOf(IntSize.Zero) }
        val density = LocalDensity.current

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(MintGreen.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .onSizeChanged { canvasSize = it }
        ) {
            LineChart(
                periods = periods,
                selectedIndex = selectedIndex,
                onIndexSelected = onPeriodSelected,
                modifier = Modifier.fillMaxSize().padding(16.dp)
            )

            if (selectedIndex in periods.indices && canvasSize != IntSize.Zero) {
                val period = periods[selectedIndex]
                val maxVal = (periods.maxOfOrNull { it.totalExpenses } ?: 0.0).coerceAtLeast(1.0).toFloat() * 1.2f
                val width = canvasSize.width
                val height = canvasSize.height
                val spacing = width / (periods.size - 1).coerceAtLeast(1)
                val x = selectedIndex * spacing
                val y = height - (period.totalExpenses.toFloat() / maxVal * height)

                val tooltipX = with(density) { x.toDp() - 40.dp }
                val tooltipY = with(density) { y.toDp() - 48.dp }

                Box(
                    modifier = Modifier
                        .offset(
                            x = tooltipX.coerceIn(0.dp, with(density) { (width - 80).toDp() }),
                            y = tooltipY.coerceIn(0.dp, 160.dp)
                        )
                        .width(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$${String.format("%,.0f", period.totalExpenses)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            periods.forEachIndexed { index, period ->
                Text(
                    period.periodLabel,
                    fontSize = 11.sp,
                    color = if (index == selectedIndex) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LineChart(
    periods: List<PeriodStats>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxVal = (periods.maxOfOrNull { it.totalExpenses } ?: 0.0).coerceAtLeast(1.0).toFloat() * 1.2f
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo"
    )
    val guideColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)

    Canvas(modifier = modifier.pointerInput(periods) {
        detectTapGestures { offset ->
            val sectionWidth = size.width / periods.size
            val index = (offset.x / sectionWidth).toInt().coerceIn(0, periods.lastIndex)
            onIndexSelected(index)
        }
    }) {
        val width = size.width
        val height = size.height
        val spacing = width / (periods.size - 1).coerceAtLeast(1)

        val points = periods.mapIndexed { index, period ->
            val x = index * spacing.toFloat()
            val y = height - (period.totalExpenses.toFloat() / maxVal * height)
            Offset(x, y)
        }

        val guideCount = 3
        for (i in 0..guideCount) {
            val y = height * i / guideCount
            drawLine(
                color = guideColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val p1 = points[i-1]
                    val p2 = points[i]
                    val con1 = Offset(p1.x + (p2.x - p1.x) / 2, p1.y)
                    val con2 = Offset(p1.x + (p2.x - p1.x) / 2, p2.y)
                    cubicTo(con1.x, con1.y, con2.x, con2.y, p2.x, p2.y)
                }
            }
        }

        drawPath(
            path = path,
            color = ForestGreen,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(ForestGreen.copy(alpha = 0.25f), Color.Transparent)
            )
        )

        points.forEachIndexed { index, offset ->
            val isSelected = index == selectedIndex
            val color = if (isSelected) ForestGreen else ForestGreen.copy(alpha = 0.5f)
            val radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx()

            if (isSelected) {
                drawLine(
                    color = ForestGreen.copy(alpha = 0.15f),
                    start = Offset(offset.x, 0f),
                    end = Offset(offset.x, height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                drawCircle(
                    color = ForestGreen.copy(alpha = haloAlpha),
                    radius = radius + 8.dp.toPx(),
                    center = offset
                )
            }

            drawCircle(
                color = Color.White,
                radius = radius + 2.dp.toPx(),
                center = offset
            )
            drawCircle(
                color = color,
                radius = radius,
                center = offset
            )
        }
    }
}

@Composable
fun PeriodDetailSection(period: PeriodStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MintGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Gastado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${String.format("%,.2f", period.totalExpenses)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                PaymentHealthBadge(period = period)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                InfoItem(
                    icon = Icons.Default.ShoppingBag,
                    label = "Transacciones",
                    value = "${period.expensesCount}",
                    modifier = Modifier.weight(1f)
                )
                InfoItem(
                    icon = Icons.Default.Payments,
                    label = "Pagos realizados",
                    value = "${period.paymentsCount}",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                InfoItem(
                    icon = Icons.Default.ReceiptLong,
                    label = "Promedio por tx",
                    value = "$${String.format("%,.0f", period.avgTransactionAmount)}",
                    modifier = Modifier.weight(1f)
                )
                InfoItem(
                    icon = Icons.Default.Savings,
                    label = "Total pagado",
                    value = "$${String.format("%,.0f", period.totalPaymentsAmount)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PaymentHealthBadge(period: PeriodStats, modifier: Modifier = Modifier) {
    val isFullyPaid = period.totalPaymentsAmount >= period.totalExpenses
    val color = when {
        isFullyPaid -> ForestGreen
        period.totalPaymentsAmount > 0 -> Color(0xFFFFA000)
        else -> MaterialTheme.colorScheme.error
    }
    val bgColor = color.copy(alpha = 0.12f)
    val label = when {
        isFullyPaid -> "Pagado"
        period.totalPaymentsAmount > 0 -> "Parcial"
        else -> "Pendiente"
    }
    val icon = when {
        isFullyPaid -> Icons.Default.CheckCircle
        period.totalPaymentsAmount > 0 -> Icons.Default.Warning
        else -> Icons.Default.Error
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CategoryBreakdownSection(
    categories: List<CategorySpend>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "Gastos por Categoría",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        if (categories.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sin categorías asignadas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    categories.forEachIndexed { index, cat ->
                        val catColor = remember(cat.categoryName) {
                            val hash = cat.categoryName.hashCode()
                            Color(
                                (hash and 0xFF0000 shr 16).coerceIn(80, 200),
                                (hash and 0x00FF00 shr 8).coerceIn(80, 200),
                                (hash and 0x0000FF).coerceIn(80, 200)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(catColor)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                cat.categoryName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${String.format("%.0f", cat.percentage * 100)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "$${String.format("%,.0f", cat.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { cat.percentage.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = catColor,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                        if (index < categories.lastIndex) {
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CalendarLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Sin gasto", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)))
        Text("→", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MintGreen.copy(alpha = 0.5f)))
        Text("→", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ForestGreen))
        Text("Alto", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
    }
}

@Composable
fun UsageCalendar(
    period: PeriodStats,
    selectedDay: LocalDate?,
    onDaySelected: (LocalDate) -> Unit
) {
    val startInstant = Instant.ofEpochMilli(period.startDate)
    val endInstant = Instant.ofEpochMilli(period.endDate)
    val startDate = startInstant.atZone(ZoneId.systemDefault()).toLocalDate()
    val endDate = endInstant.atZone(ZoneId.systemDefault()).toLocalDate()

    val days = mutableListOf<LocalDate>()
    var curr = startDate
    while (!curr.isAfter(endDate)) {
        days.add(curr)
        curr = curr.plusDays(1)
    }

    val dayLabels = listOf("L", "M", "M", "J", "V", "S", "D")
    val maxDayAmount = remember(period) { period.expensesByDay.values.maxOrNull() ?: 1.0 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        val firstDayOfWeek = startDate.dayOfWeek.value
        val padding = firstDayOfWeek - 1
        val gridContent = (0 until padding).map { null } + days

        for (i in gridContent.indices step 7) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                for (j in 0 until 7) {
                    val day = if (i + j < gridContent.size) gridContent[i + j] else null
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val hasExpense = period.expensesByDay.containsKey(day.dayOfMonth) &&
                                             (day.month == startDate.month || day.month == endDate.month)

                            val intensity = if (hasExpense) {
                                val amount = period.expensesByDay[day.dayOfMonth] ?: 0.0
                                (amount / maxDayAmount).toFloat().coerceIn(0.2f, 1f)
                            } else 0f

                            val isSelected = selectedDay == day

                            Surface(
                                modifier = Modifier
                                    .size(32.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures {
                                            onDaySelected(day)
                                        }
                                    },
                                shape = CircleShape,
                                color = when {
                                    isSelected -> ForestGreen
                                    hasExpense -> MintGreen.copy(alpha = intensity)
                                    else -> Color.Transparent
                                },
                                border = if (isSelected && !hasExpense) {
                                    BorderStroke(2.dp, ForestGreen)
                                } else null,
                                contentColor = if (isSelected) Color.White else if (hasExpense) ForestGreen else MaterialTheme.colorScheme.onSurface
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "${day.dayOfMonth}",
                                        fontSize = 12.sp,
                                        fontWeight = if (hasExpense || isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentsVsExpensesSection(
    periods: List<PeriodStats>,
    selectedIndex: Int,
    onPeriodSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (periods.isEmpty()) return

    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        Text(
            "Pagos vs Gastos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Comparación por período",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ForestGreen))
                        Spacer(Modifier.width(4.dp))
                        Text("Gastos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2196F3)))
                        Spacer(Modifier.width(4.dp))
                        Text("Pagos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(12.dp))

                PaymentsVsExpensesChart(
                    periods = periods,
                    selectedIndex = selectedIndex,
                    onIndexSelected = onPeriodSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    periods.forEachIndexed { index, period ->
                        Text(
                            period.periodLabel,
                            fontSize = 10.sp,
                            color = if (index == selectedIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentsVsExpensesChart(
    periods: List<PeriodStats>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val expenseColor = ForestGreen
    val paymentColor = Color(0xFF2196F3)
    val selectionLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val maxVal = periods.maxOf { maxOf(it.totalExpenses, it.totalPaymentsAmount) }
        .coerceAtLeast(1.0)
        .toFloat() * 1.1f

    Canvas(modifier = modifier.pointerInput(periods) {
        detectTapGestures { offset ->
            val sectionWidth = size.width / periods.size
            val index = (offset.x / sectionWidth).toInt().coerceIn(0, periods.lastIndex)
            onIndexSelected(index)
        }
    }) {
        val width = size.width
        val height = size.height
        val sectionWidth = width / periods.size
        val barWidth = sectionWidth * 0.25f
        val spacing = sectionWidth * 0.15f

        periods.forEachIndexed { index, period ->
            val centerX = index * sectionWidth + sectionWidth / 2
            val isSelected = index == selectedIndex

            val expenseHeight = (period.totalExpenses.toFloat() / maxVal) * height
            val expenseLeft = centerX - barWidth - spacing / 2
            drawRoundRect(
                color = if (isSelected) expenseColor else expenseColor.copy(alpha = 0.6f),
                topLeft = Offset(expenseLeft, height - expenseHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, expenseHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            val paymentHeight = (period.totalPaymentsAmount.toFloat() / maxVal) * height
            val paymentLeft = centerX + spacing / 2
            drawRoundRect(
                color = if (isSelected) paymentColor else paymentColor.copy(alpha = 0.6f),
                topLeft = Offset(paymentLeft, height - paymentHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, paymentHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            if (isSelected) {
                drawLine(
                    color = selectionLineColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }
        }
    }
}


@Composable
fun InsightsCarousel(
    insights: List<Insight>,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    
    // Auto-scroll every 5 seconds
    LaunchedEffect(insights) {
        while (insights.size > 1) {
            delay(5000)
            currentIndex = (currentIndex + 1) % insights.size
        }
    }

    Column(modifier = modifier) {
        Text(
            "Insights",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        AnimatedContent(
            targetState = currentIndex,
            label = "insight",
            transitionSpec = { fadeIn() + slideInHorizontally { it / 2 } togetherWith fadeOut() + slideOutHorizontally { -it / 2 } }
        ) { index ->
            val insight = insights[index]
            val color = CardStatsInsights.getInsightColor(insight.type)
            val bgColor = color.copy(alpha = 0.08f)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = insight.icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        insight.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (insights.size > 1) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                insights.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentIndex) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                    if (index < insights.lastIndex) {
                        Spacer(Modifier.width(6.dp))
                    }
                }
            }
        }
    }
}