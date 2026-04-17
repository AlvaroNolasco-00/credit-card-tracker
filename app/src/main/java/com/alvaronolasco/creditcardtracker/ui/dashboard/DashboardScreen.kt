package com.alvaronolasco.creditcardtracker.ui.dashboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import com.alvaronolasco.creditcardtracker.data.entity.ExpenseWithCategories
import com.alvaronolasco.creditcardtracker.ui.components.AppLoadingIndicator
import com.alvaronolasco.creditcardtracker.ui.components.EmptyStateView
import com.alvaronolasco.creditcardtracker.ui.theme.*
import com.alvaronolasco.creditcardtracker.ui.theme.CardGradients

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddCard: () -> Unit,
    onCardClick: (Int) -> Unit,
    onAddExpense: (Int) -> Unit,
    onAddPersonalExpense: () -> Unit,
    onIncomeClick: () -> Unit,
    onBudgetClick: () -> Unit,
    onCameraOpen: () -> Unit,
    onSearchClick: () -> Unit,
    onExpenseClick: (Int) -> Unit,
    onSupportClick: () -> Unit,
    onActivityClick: () -> Unit,
    onStatsClick: (Int) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // +1 for the "Add card" tile at the end
    val pagerState = rememberPagerState { uiState.cards.size + 1 }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectCard(pagerState.currentPage)
    }

    val selectedCard = uiState.cards.getOrNull(pagerState.currentPage)

    if (uiState.showNamePrompt) {
        NameSetupBottomSheet(
            onConfirm = { name -> viewModel.saveName(name) },
            onDismiss = { viewModel.dismissNamePrompt() }
        )
    }

    if (uiState.showBudgetPrompt && !uiState.showNamePrompt) {
        BudgetReminderDialog(
            onGoToBudget = {
                viewModel.dismissBudgetPrompt()
                onBudgetClick()
            },
            onDismiss = { viewModel.dismissBudgetPrompt() }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!uiState.isLoading && uiState.cards.isNotEmpty()) {
                BottomActionBar(
                    onAddExpense = { selectedCard?.let { onAddExpense(it.card.id) } },
                    onAddPersonalExpense = onAddPersonalExpense,
                    onCameraOpen = onCameraOpen
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                AppLoadingIndicator(Modifier.padding(padding))
            }
            uiState.cards.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No tienes tarjetas agregadas",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onAddCard,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Agregar tarjeta")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD600),
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (uiState.userName.isNullOrBlank()) "¡Hola!" else "Hola, ${uiState.userName}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onActivityClick,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SoftLime.copy(alpha = 0.85f))
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = "Actividad",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            IconButton(
                                onClick = onSupportClick,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SoftLime.copy(alpha = 0.85f))
                            ) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = "Apoya al desarrollador",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Card pager inside mint background container
                    item {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MintGreen)
                                .padding(vertical = 20.dp)
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                contentPadding = PaddingValues(horizontal = 32.dp),
                                pageSpacing = 12.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->
                                if (page < uiState.cards.size) {
                                    CreditCardPagerItem(
                                        state = uiState.cards[page],
                                        onClick = { onCardClick(uiState.cards[page].card.id) },
                                        onStatsClick = { onStatsClick(uiState.cards[page].card.id) }
                                    )
                                } else {
                                    AddCardTile(onClick = onAddCard)
                                }
                            }
                        }
                    }

                    // Page indicators
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dots only for real cards (not the add-card tile)
                            repeat(uiState.cards.size) { index ->
                                val isSelected = pagerState.currentPage == index
                                val width by animateDpAsState(
                                    targetValue = if (isSelected) 22.dp else 8.dp,
                                    label = "dot_width"
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .height(8.dp)
                                        .width(width)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) ForestGreen
                                            else Color.Gray.copy(alpha = 0.3f)
                                        )
                                )
                            }
                        }
                    }

                    // Overdue payment alert banner
                    if (selectedCard != null && selectedCard.isPaymentOverdue) {
                        item {
                            OverduePaymentBanner(
                                cardName = selectedCard.card.name,
                                daysOverdue = selectedCard.daysOverdue,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Cut-off + Payment info row
                    if (selectedCard != null) {
                        item {
                            CardInfoRow(
                                state = selectedCard,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Pay balance button (only when cut period has unpaid balance)
                    if (selectedCard != null &&
                        selectedCard.cutOffHappenedThisMonth &&
                        !selectedCard.isPaidThisCycle &&
                        (selectedCard.cutPeriodTotal + selectedCard.extraFinancingPayment - selectedCard.partiallyPaidAmount) > 0.0
                    ) {
                        item {
                            PayBalanceCard(
                                state = selectedCard,
                                onConfirmPay = { amount -> viewModel.payPartial(selectedCard, amount) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Income setup banner (when no income configured) or salary usage card
                    item {
                        if (uiState.totalMonthlyIncome == 0.0) {
                            IncomeSetupBanner(
                                onSetupClick = onIncomeClick,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        } else {
                            SalaryUsageCard(
                                totalSpent = uiState.totalAllCardsSpent,
                                totalNonCardSpent = uiState.totalNonCardSpent,
                                totalIncome = uiState.totalMonthlyIncome,
                                onTap = onIncomeClick,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Budget section
                    item {
                        BudgetSectionCard(
                            hasBudget = uiState.hasBudgetThisMonth,
                            onTap = onBudgetClick,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Support developer card (frequency-gated)
                    if (uiState.showSupportCard) {
                        item {
                            SupportDeveloperCard(
                                onClick = onSupportClick,
                                onDismiss = { viewModel.dismissSupportCard() },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Transactions header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Transacciones",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            IconButton(
                                onClick = onSearchClick,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Buscar gastos",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (uiState.recentExpenses.isEmpty()) {
                        item {
                            Text(
                                "Sin transacciones recientes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    } else {
                        items(uiState.recentExpenses) { ewc ->
                            TransactionItem(
                                expenseWithCategories = ewc,
                                onClick = { onExpenseClick(ewc.expense.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreditCardPagerItem(
    state: CardDashboardState,
    onClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    val totalDue = if (state.cutOffHappenedThisMonth)
        state.cutPeriodTotal + state.totalSpent + state.extraFinancingPayment
    else
        state.totalSpent + state.extraFinancingPayment
    val progress = if (state.card.creditLimit > 0) {
        (totalDue / state.card.creditLimit).toFloat().coerceIn(0f, 1f)
    } else 0f

    val gradient = CardGradients.getBrushForColor(state.card.color)
    val cardTextColor = CardGradients.getTextColorForCard(state.card.color)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = Color.Transparent,
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Progress bar at top
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )

                Spacer(Modifier.height(14.dp))

                // Saldo and Límite
                if (state.cutOffHappenedThisMonth) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Saldo corte",
                                color = cardTextColor.copy(alpha = 0.65f),
                                fontSize = 10.sp
                            )
                            Text(
                                "$${String.format("%,.2f", state.cutPeriodTotal + state.extraFinancingPayment)}",
                                color = cardTextColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Período actual",
                                color = cardTextColor.copy(alpha = 0.65f),
                                fontSize = 10.sp
                            )
                            Text(
                                "$${String.format("%,.2f", state.totalSpent)}",
                                color = cardTextColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "Límite",
                                color = cardTextColor.copy(alpha = 0.65f),
                                fontSize = 10.sp
                            )
                            Text(
                                "$${String.format("%,.2f", state.card.creditLimit)}",
                                color = cardTextColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Saldo",
                                color = cardTextColor.copy(alpha = 0.65f),
                                fontSize = 12.sp
                            )
                            Text(
                                "$${String.format("%,.2f", totalDue)}",
                                color = cardTextColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (state.extraFinancingPayment > 0.0) {
                                Text(
                                    "Extra: $${String.format("%,.2f", state.extraFinancingPayment)}",
                                    color = cardTextColor.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Límite",
                                color = cardTextColor.copy(alpha = 0.65f),
                                fontSize = 12.sp
                            )
                            Text(
                                "$${String.format("%,.2f", state.card.creditLimit)}",
                                color = cardTextColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Card name + digits | info button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            state.card.name,
                            color = cardTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "**** ${state.card.lastFourDigits}",
                            color = cardTextColor.copy(alpha = 0.75f),
                            fontSize = 14.sp
                        )
                    }

                    Row {
                    if (state.hasStatsAvailable) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { onStatsClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Insights,
                                contentDescription = "Ver estadísticas",
                                tint = Color(state.card.color),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Información de tarjeta",
                            tint = Color(state.card.color),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun AddCardTile(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            brush = Brush.linearGradient(listOf(ForestGreen.copy(alpha = 0.5f), MintGreen)),
        ),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MintGreen.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Agregar otra tarjeta",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = ForestGreen
            )
        }
    }
}

@Composable
fun CardInfoRow(
    state: CardDashboardState,
    modifier: Modifier = Modifier
) {
    val isCutOffSoon = state.daysUntilCutOff <= 9
    val isPaymentOk = state.daysUntilPayment > 3

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoChip(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.ContentCut,
            title = "Corte",
            dateLabel = state.cutOffDateLabel,
            statusText = "Faltan ${state.daysUntilCutOff} días",
            trailingIcon = Icons.Default.Warning,
            trailingTint = Color(0xFFFFA000),
            showTrailing = isCutOffSoon
        )

        if (state.isPaymentOverdue) {
            InfoChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AttachMoney,
                title = "Pago",
                dateLabel = null,
                statusText = "Vencido hace ${state.daysOverdue} día(s)",
                trailingIcon = Icons.Default.Error,
                trailingTint = Color(0xFFE53935),
                showTrailing = true,
                containerColor = Color(0xFFE53935).copy(alpha = 0.08f)
            )
        } else if (state.isPaidThisCycle) {
            InfoChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AttachMoney,
                title = "Pago",
                dateLabel = null,
                statusText = "Saldo pagado",
                trailingIcon = Icons.Default.CheckCircle,
                trailingTint = Color(0xFF4CAF50),
                showTrailing = true
            )
        } else {
            InfoChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AttachMoney,
                title = "Pago",
                dateLabel = null,
                statusText = if (isPaymentOk) "Estás al día" else "Vence en ${state.daysUntilPayment} días",
                trailingIcon = if (isPaymentOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                trailingTint = if (isPaymentOk) Color(0xFF4CAF50) else Color(0xFFFFA000),
                showTrailing = true
            )
        }
    }
}

@Composable
fun PayBalanceCard(
    state: CardDashboardState,
    onConfirmPay: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val totalDue = state.cutPeriodTotal + state.extraFinancingPayment
    val remaining = (totalDue - state.partiallyPaidAmount).coerceAtLeast(0.0)

    if (showDialog) {
        var amountText by remember(remaining) { mutableStateOf(String.format("%.2f", remaining)) }
        val amountValue = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
        val isValid = amountValue > 0.0 && amountValue <= remaining

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Registrar pago — ${state.card.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state.partiallyPaidAmount > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Ya abonado",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$${String.format("%,.2f", state.partiallyPaidAmount)}",
                                fontSize = 13.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Pendiente",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$${String.format("%,.2f", remaining)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Monto a pagar") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = amountText.isNotEmpty() && !isValid,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onConfirmPay(amountValue.coerceAtMost(remaining))
                    },
                    enabled = isValid
                ) {
                    Text("Confirmar", color = if (isValid) Color(0xFF2E7D32) else Color.Gray)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    val accentColor = if (state.isPaymentOverdue) Color(0xFFE53935) else Color(0xFF4CAF50)
    val iconTint = if (state.isPaymentOverdue) Color(0xFFB71C1C) else Color(0xFF2E7D32)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (state.isPaymentOverdue) Icons.Default.Error else Icons.Default.AttachMoney,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when {
                        state.isPaymentOverdue -> "¡Pago vencido! Registra ahora"
                        state.partiallyPaidAmount > 0.0 -> "Saldo pendiente"
                        else -> "Saldo a pagar"
                    },
                    fontSize = 12.sp,
                    color = if (state.isPaymentOverdue) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$${String.format("%,.2f", remaining)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (state.partiallyPaidAmount > 0.0) {
                    Text(
                        "Abonado: $${String.format("%,.2f", state.partiallyPaidAmount)}",
                        fontSize = 11.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            Button(
                onClick = { showDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                )
            ) {
                Text("Pagar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InfoChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    dateLabel: String?,
    statusText: String,
    trailingIcon: ImageVector,
    trailingTint: Color,
    showTrailing: Boolean,
    containerColor: Color? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor ?: MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (dateLabel != null) {
                    Text(
                        dateLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    statusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (showTrailing) {
                Icon(
                    trailingIcon,
                    contentDescription = null,
                    tint = trailingTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun TransactionItem(
    expenseWithCategories: ExpenseWithCategories,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expense = expenseWithCategories.expense
    val categoriesText = expenseWithCategories.categories
        .joinToString(", ") { it.name }
        .ifEmpty { null }

    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF5BAD6F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AttachMoney,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    expense.description,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "$${String.format("%,.2f", expense.amount)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (categoriesText != null) {
                    Text(
                        categoriesText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun IncomeSetupBanner(
    onSetupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSetupClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Configura tus ingresos",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Así podrás ver qué porcentaje de tu salario va en tarjetas",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SalaryUsageCard(
    totalSpent: Double,
    totalNonCardSpent: Double,
    totalIncome: Double,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalUsed = totalSpent + totalNonCardSpent
    val ratio = if (totalIncome > 0) (totalUsed / totalIncome).coerceIn(0.0, 1.0) else 0.0
    val percent = (ratio * 100).toInt()
    val remaining = (totalIncome - totalUsed).coerceAtLeast(0.0)

    val barColor: Color
    val alertText: String
    when {
        ratio < 0.30 -> { barColor = Color(0xFF4CAF50); alertText = "" }
        ratio < 0.50 -> { barColor = Color(0xFFFFA000); alertText = "Estás usando bastante de tu ingreso" }
        else         -> { barColor = Color(0xFFE53935); alertText = "¡Tus gastos superan el 50% de tu ingreso!" }
    }
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurface

    val animatedProgress by animateFloatAsState(
        targetValue = ratio.toFloat(),
        animationSpec = tween(durationMillis = 800),
        label = "salary_bar"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, barColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = barColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Uso de ingreso mensual",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = labelColor
                    )
                }
                Text(
                    "$percent%",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = barColor
                )
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = barColor,
                trackColor = barColor.copy(alpha = 0.15f)
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Tarjetas: $${String.format("%,.0f", totalSpent)}",
                        fontSize = 12.sp,
                        color = labelColor
                    )
                    if (totalNonCardSpent > 0.0) {
                        Text(
                            "Personal: $${String.format("%,.0f", totalNonCardSpent)}",
                            fontSize = 12.sp,
                            color = labelColor.copy(alpha = 0.75f)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Ingreso: $${String.format("%,.0f", totalIncome)}",
                        fontSize = 12.sp,
                        color = labelColor
                    )
                    Text(
                        "Disponible: $${String.format("%,.0f", remaining)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (remaining > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                    )
                }
            }

            if (alertText.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = barColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        alertText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = barColor
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetSectionCard(
    hasBudget: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MintGreen.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PieChart,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (hasBudget) "Presupuesto mensual" else "Crea tu presupuesto",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (hasBudget) "Ve tus límites y gastos por categoría"
                    else "Define límites de gasto por categoría",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ForestGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameSetupBottomSheet(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val trimmed = nameText.trim()
    val isValid = trimmed.isNotEmpty()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        LaunchedEffect(Unit) {
            delay(200) // Give it a bit more time to settle
            focusRequester.requestFocus()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ForestGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "¿Cómo te llamas?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Queremos darte una atención personalizada. Tu nombre solo se guarda en este dispositivo.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                placeholder = { Text("Tu nombre") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (isValid) onConfirm(trimmed) }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForestGreen,
                    focusedLabelColor = ForestGreen,
                    cursorColor = ForestGreen
                )
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onConfirm(trimmed) },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestGreen,
                    contentColor = Color.White,
                    disabledContainerColor = ForestGreen.copy(alpha = 0.35f),
                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                )
            ) {
                Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))

            TextButton(onClick = onDismiss) {
                Text(
                    "Ahora no",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BottomActionBar(
    onAddExpense: () -> Unit,
    onAddPersonalExpense: () -> Unit,
    onCameraOpen: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onAddExpense,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftLime,
                    contentColor = ForestGreen
                )
            ) {
                Text(
                    "Tarjeta",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onAddPersonalExpense,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(
                    "Personal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onCameraOpen,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF8EA88E))
            ) {
                Icon(
                    Icons.Default.AddAPhoto,
                    contentDescription = "Escanear recibo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun BudgetReminderDialog(
    onGoToBudget: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Savings,
                contentDescription = null,
                tint = ForestGreen,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "¿Ya tienes tu presupuesto?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "El mes ya comenzó y aún no has definido un presupuesto. Establecer uno te ayuda a controlar mejor tus finanzas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onGoToBudget,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear presupuesto", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Más tarde",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun OverduePaymentBanner(
    cardName: String,
    daysOverdue: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE53935).copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = Color(0xFFE53935),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Pago de $cardName vencido hace $daysOverdue día(s) — registra tu pago",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE53935)
            )
        }
    }
}

@Composable
fun SupportDeveloperCard(
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, SoftLime.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SoftLime.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Apoya al desarrollador",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Si la app te es útil, invítame un café",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = SoftLime,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        // Dismiss button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .offset(x = 6.dp, y = (-6).dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
