package com.alvaronolasco.creditcardtracker.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaronolasco.creditcardtracker.data.entity.ExpenseWithCategories
import com.alvaronolasco.creditcardtracker.ui.components.AppChip
import com.alvaronolasco.creditcardtracker.ui.components.AppTopBar
import com.alvaronolasco.creditcardtracker.ui.components.EmptyStateView
import com.alvaronolasco.creditcardtracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val DAY_MS = 24L * 60 * 60 * 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseSearchScreen(
    onBack: () -> Unit,
    onExpenseClick: (Int) -> Unit,
    viewModel: ExpenseSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val startPickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.startDate)
    val endPickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.endDate)

    // Preset ranges
    val presets = listOf(
        "15d" to 15L,
        "30d" to 30L,
        "3m" to 90L,
        "6m" to 180L
    )
    var selectedPreset by remember { mutableStateOf<String?>("15d") }

    fun applyPreset(days: Long, label: String) {
        selectedPreset = label
        val end = System.currentTimeMillis()
        val start = end - days * DAY_MS
        viewModel.onDateRangeChange(start, end)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Buscar Gastos",
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
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
            // Search field
            OutlinedTextField(
                value = uiState.searchText,
                onValueChange = { viewModel.onSearchTextChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.SpacingMd, vertical = Dimensions.SpacingSm),
                placeholder = { Text("Buscar por descripción...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextGray)
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // Date range presets
            Text(
                "Período",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextGray,
                modifier = Modifier.padding(horizontal = Dimensions.SpacingMd, vertical = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimensions.SpacingMd),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)
            ) {
                items(presets) { (label, days) ->
                    FilterChip(
                        selected = selectedPreset == label,
                        onClick = { applyPreset(days, label) },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForestGreen,
                            selectedLabelColor = Color.White,
                            labelColor = TextDark,
                            containerColor = SoftGray
                        ),
                        border = null
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPreset == "custom",
                        onClick = {
                            selectedPreset = "custom"
                            showStartDatePicker = true
                        },
                        label = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Personalizado", fontWeight = FontWeight.Bold)
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForestGreen,
                            selectedLabelColor = Color.White,
                            labelColor = TextDark,
                            containerColor = SoftGray
                        ),
                        border = null
                    )
                }
            }

            // Show custom date range when selected
            if (selectedPreset == "custom") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.SpacingMd, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Desde: ${dateFormatter.format(Date(uiState.startDate))}",
                            fontSize = 12.sp
                        )
                    }
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Hasta: ${dateFormatter.format(Date(uiState.endDate))}",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Category filter
            if (uiState.categories.isNotEmpty()) {
                Text(
                    "Categorías",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextGray,
                    modifier = Modifier.padding(horizontal = Dimensions.SpacingMd, vertical = 4.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Dimensions.SpacingMd),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)
                ) {
                    items(uiState.categories) { category ->
                        AppChip(
                            selected = uiState.selectedCategoryIds.contains(category.id),
                            onClick = { viewModel.onCategoryToggle(category.id) },
                            label = category.name
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimensions.SpacingSm),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )

            // Results count
            Text(
                "${uiState.filteredExpenses.size} gasto${if (uiState.filteredExpenses.size != 1) "s" else ""} encontrado${if (uiState.filteredExpenses.size != 1) "s" else ""}",
                fontSize = 13.sp,
                color = TextGray,
                modifier = Modifier.padding(horizontal = Dimensions.SpacingMd).padding(bottom = 4.dp)
            )

            // Results
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ForestGreen)
                }
            } else if (uiState.filteredExpenses.isEmpty()) {
                EmptyStateView(message = "No se encontraron gastos")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Dimensions.SpacingMd, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)
                ) {
                    items(uiState.filteredExpenses, key = { it.expense.id }) { ewc ->
                        ExpenseSearchItem(
                            expenseWithCategories = ewc,
                            cardName = viewModel.cardNameForExpense(ewc.expense.cardId),
                            dateFormatter = dateFormatter,
                            onClick = { onExpenseClick(ewc.expense.id) }
                        )
                    }
                }
            }
        }
    }

    // Date pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startPickerState.selectedDateMillis?.let { millis ->
                        viewModel.onDateRangeChange(millis, uiState.endDate)
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = startPickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endPickerState.selectedDateMillis?.let { millis ->
                        // Set end to end of that day
                        viewModel.onDateRangeChange(uiState.startDate, millis + DAY_MS - 1)
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = endPickerState)
        }
    }
}

@Composable
private fun ExpenseSearchItem(
    expenseWithCategories: ExpenseWithCategories,
    cardName: String,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit
) {
    val expense = expenseWithCategories.expense
    val categoriesText = expenseWithCategories.categories
        .joinToString(", ") { it.name }
        .ifEmpty { "Sin categoría" }

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    expense.description,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextDark,
                    maxLines = 1
                )
                Text(
                    categoriesText,
                    fontSize = 12.sp,
                    color = ForestGreen,
                    maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cardName.isNotEmpty()) {
                        Text(cardName, fontSize = 11.sp, color = TextGray)
                        Text("•", fontSize = 11.sp, color = TextGray)
                    }
                    Text(dateFormatter.format(Date(expense.date)), fontSize = 11.sp, color = TextGray)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "$${String.format("%,.2f", expense.amount)}",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = TextDark
            )
        }
    }
}
