package com.alvaronolasco.creditcardtracker.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaronolasco.creditcardtracker.data.entity.RecurringExpense
import com.alvaronolasco.creditcardtracker.data.entity.RecurringExpenseWithCategories
import com.alvaronolasco.creditcardtracker.ui.components.*
import com.alvaronolasco.creditcardtracker.ui.theme.Dimensions
import com.alvaronolasco.creditcardtracker.ui.theme.ForestGreen
import com.alvaronolasco.creditcardtracker.ui.theme.MintGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesScreen(
    cardId: Int,
    onBack: () -> Unit,
    onAddRecurring: () -> Unit,
    onEditRecurring: (Int) -> Unit,
    viewModel: RecurringExpensesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(cardId) {
        viewModel.loadData(cardId)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Gastos Recurrentes",
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRecurring,
                containerColor = ForestGreen,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Gasto Recurrente")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            AppLoadingIndicator(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(Dimensions.SpacingMd),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMd)
            ) {
                uiState.card?.let { card ->
                    item {
                        RecurringOverviewCard(
                            cardName = card.name,
                            totalRecurring = uiState.recurringExpenses.sumOf { it.recurringExpense.amount }
                        )
                    }
                }

                item {
                    Text(
                        text = "Gastos recurrentes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = Dimensions.SpacingSm)
                    )
                }

                if (uiState.recurringExpenses.isEmpty()) {
                    item {
                        EmptyStateView(
                            message = "No hay gastos recurrentes para esta tarjeta",
                            modifier = Modifier.height(200.dp)
                        )
                    }
                } else {
                    items(uiState.recurringExpenses) { expenseWithCategories ->
                        RecurringExpenseItem(
                            expenseWithCategories = expenseWithCategories,
                            onEdit = { onEditRecurring(expenseWithCategories.recurringExpense.id) },
                            onDelete = { viewModel.deleteRecurringExpense(expenseWithCategories.recurringExpense) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecurringOverviewCard(
    cardName: String,
    totalRecurring: Double
) {
    AppCard(
        containerColor = MintGreen,
        border = null
    ) {
        Column(modifier = Modifier.padding(Dimensions.SpacingMd)) {
            Text(
                cardName,
                style = MaterialTheme.typography.labelMedium,
                color = ForestGreen.copy(alpha = 0.7f)
            )
            Text(
                "Total gastos recurrentes",
                style = MaterialTheme.typography.labelSmall,
                color = ForestGreen.copy(alpha = 0.7f)
            )
            Text(
                text = "$${String.format("%.2f", totalRecurring)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = ForestGreen
            )
        }
    }
}

@Composable
fun RecurringExpenseItem(
    expenseWithCategories: RecurringExpenseWithCategories,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val expense = expenseWithCategories.recurringExpense
    val categoriesText = expenseWithCategories.categories
        .joinToString(", ") { it.name }
        .ifEmpty { "Sin categoría" }

    AppCard(onClick = onEdit) {
        Row(
            modifier = Modifier
                .padding(Dimensions.SpacingMd)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (expense.dayOfMonth != null) "Día ${expense.dayOfMonth}" else "Sin fecha fija",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = categoriesText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format("%.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
