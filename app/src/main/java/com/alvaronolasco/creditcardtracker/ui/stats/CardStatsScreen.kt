package com.alvaronolasco.creditcardtracker.ui.stats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                // Interactive Chart Section
                StatsChartSection(
                    periods = uiState.periods,
                    selectedIndex = uiState.selectedPeriodIndex,
                    onPeriodSelected = { viewModel.selectPeriod(it) }
                )

                Spacer(Modifier.height(24.dp))

                // Period Summary Details
                val selectedPeriod = uiState.periods.getOrNull(uiState.selectedPeriodIndex)
                if (selectedPeriod != null) {
                    PeriodDetailSection(selectedPeriod)
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Text(
                        "Calendario del Periodo",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Días con movimientos registrados",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    UsageCalendar(selectedPeriod)
                }
                
                Spacer(Modifier.height(32.dp))
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(MintGreen.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
        ) {
            LineChart(
                periods = periods,
                selectedIndex = selectedIndex,
                onIndexSelected = onPeriodSelected,
                modifier = Modifier.fillMaxSize().padding(16.dp)
            )
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
    val selectionAlpha by animateFloatAsState(targetValue = 1f, label = "selection")

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
            val x = index * spacing
            val y = height - (period.totalExpenses.toFloat() / maxVal * height)
            Offset(x, y)
        }

        // Draw path
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

        // Draw area under path
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(ForestGreen.copy(alpha = 0.3f), Color.Transparent)
            )
        )

        // Draw points and selection
        points.forEachIndexed { index, offset ->
            val isSelected = index == selectedIndex
            val color = if (isSelected) ForestGreen else ForestGreen.copy(alpha = 0.5f)
            val radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx()

            if (isSelected) {
                // Draw selection vertical line
                drawLine(
                    color = ForestGreen.copy(alpha = 0.2f),
                    start = Offset(offset.x, 0f),
                    end = Offset(offset.x, height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
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
                Column {
                    Text("Total Gastado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${String.format("%,.2f", period.totalExpenses)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
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
fun UsageCalendar(period: PeriodStats) {
    val startInstant = Instant.ofEpochMilli(period.startDate)
    val endInstant = Instant.ofEpochMilli(period.endDate)
    val startDate = startInstant.atZone(ZoneId.systemDefault()).toLocalDate()
    val endDate = endInstant.atZone(ZoneId.systemDefault()).toLocalDate()

    // We'll show a "month view" based on the primary month of the period
    // Since periods can span two months, we'll show all days from start to end in a custom grid
    val days = mutableListOf<LocalDate>()
    var curr = startDate
    while (!curr.isAfter(endDate)) {
        days.add(curr)
        curr = curr.plusDays(1)
    }

    // Header for days of week
    val dayLabels = listOf("L", "M", "M", "J", "V", "S", "D")
    
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

        // Grid of days
        // We find the day of week for the first day to add padding
        val firstDayOfWeek = startDate.dayOfWeek.value // 1 (Mon) to 7 (Sun)
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
                                (amount / (period.totalExpenses / period.expensesCount).coerceAtLeast(1.0)).toFloat().coerceIn(0.2f, 1f)
                            } else 0f

                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = if (hasExpense) MintGreen.copy(alpha = intensity) else Color.Transparent,
                                contentColor = if (hasExpense) ForestGreen else MaterialTheme.colorScheme.onSurface
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "${day.dayOfMonth}",
                                        fontSize = 12.sp,
                                        fontWeight = if (hasExpense) FontWeight.Bold else FontWeight.Normal
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
