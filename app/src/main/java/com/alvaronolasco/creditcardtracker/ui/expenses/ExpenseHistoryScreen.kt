package com.alvaronolasco.creditcardtracker.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaronolasco.creditcardtracker.data.entity.ExpenseWithCategories
import com.alvaronolasco.creditcardtracker.ui.components.AppChip
import com.alvaronolasco.creditcardtracker.ui.components.AppTopBar
import com.alvaronolasco.creditcardtracker.ui.components.EmptyStateView
import com.alvaronolasco.creditcardtracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseHistoryScreen(
    cardId: Int,
    onBack: () -> Unit,
    onExpenseClick: (Int) -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilterCategoryId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(cardId) {
        viewModel.loadExpenses(cardId)
    }

    val filteredExpenses = remember(uiState.expenses, selectedFilterCategoryId) {
        if (selectedFilterCategoryId == null) uiState.expenses
        else uiState.expenses.filter { ewc ->
            ewc.categories.any { it.id == selectedFilterCategoryId }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Historial de Gastos",
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
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Dimensions.SpacingMd, vertical = Dimensions.SpacingSm),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilterCategoryId == null,
                            onClick = { selectedFilterCategoryId = null },
                            label = {
                                Text(
                                    "Todos",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = null
                        )
                    }
                    items(uiState.categories) { category ->
                        AppChip(
                            selected = selectedFilterCategoryId == category.id,
                            onClick = {
                                selectedFilterCategoryId = if (selectedFilterCategoryId == category.id) null
                                else category.id
                            },
                            label = category.name
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            }

            if (filteredExpenses.isEmpty()) {
                EmptyStateView(
                    message = if (selectedFilterCategoryId != null)
                        "No hay gastos con esta categoría"
                    else
                        "No hay gastos registrados para esta tarjeta"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimensions.SpacingMd),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)
                ) {
                    items(filteredExpenses) { ewc ->
                        ExpenseHistoryItem(
                            expenseWithCategories = ewc,
                            onClick = { onExpenseClick(ewc.expense.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseHistoryItem(
    expenseWithCategories: ExpenseWithCategories,
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val expense = expenseWithCategories.expense
    val categoriesText = expenseWithCategories.categories
        .joinToString(", ") { it.name }
        .ifEmpty { "Sin categoría" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
            ) {
                Icon(
                    Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$categoriesText • ${dateFormatter.format(Date(expense.date))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (expense.msiMonths > 1) {
                    Text(
                        text = "${expense.msiMonths} MSI • $${String.format("%.2f", expense.msiMonthlyAmount)}/mes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Text(
            text = "$${String.format("%.2f", expense.amount)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black
        )
    }
}
