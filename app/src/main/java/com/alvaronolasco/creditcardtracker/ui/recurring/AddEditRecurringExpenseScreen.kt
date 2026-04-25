package com.alvaronolasco.creditcardtracker.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaronolasco.creditcardtracker.ui.components.AppButton
import com.alvaronolasco.creditcardtracker.ui.components.AppCard
import com.alvaronolasco.creditcardtracker.ui.components.AppChip
import com.alvaronolasco.creditcardtracker.ui.components.AppTextField
import com.alvaronolasco.creditcardtracker.ui.components.AppTopBar
import com.alvaronolasco.creditcardtracker.ui.theme.Dimensions
import com.alvaronolasco.creditcardtracker.ui.theme.ForestGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringExpenseScreen(
    cardId: Int,
    recurringExpenseId: Int? = null,
    onBack: () -> Unit,
    viewModel: RecurringExpensesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditMode = recurringExpenseId != null

    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dayOfMonth by remember { mutableStateOf("") }
    var hasDayOfMonth by remember { mutableStateOf(false) }
    var selectedCategoryIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var categoryToDelete by remember { mutableStateOf<com.alvaronolasco.creditcardtracker.data.entity.Category?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cardId) {
        viewModel.loadData(cardId)
    }

    LaunchedEffect(recurringExpenseId, uiState.recurringExpenses) {
        if (recurringExpenseId != null) {
            val entry = uiState.recurringExpenses.find { it.recurringExpense.id == recurringExpenseId }
            entry?.let {
                description = it.recurringExpense.description
                amount = it.recurringExpense.amount.toString()
                if (it.recurringExpense.dayOfMonth != null) {
                    hasDayOfMonth = true
                    dayOfMonth = it.recurringExpense.dayOfMonth.toString()
                }
                selectedCategoryIds = it.categories.map { cat -> cat.id }.toSet()
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isEditMode) "Editar Gasto Recurrente" else "Nuevo Gasto Recurrente",
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Dimensions.SpacingMd)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMd)
        ) {
            uiState.card?.let { card ->
                AppCard {
                    Row(
                        modifier = Modifier.padding(Dimensions.SpacingMd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Tarjeta: ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${card.name} •••• ${card.lastFourDigits}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            AppTextField(
                value = description,
                onValueChange = { description = it },
                label = "Descripción (ej: Plan Teléfono)"
            )

            AppTextField(
                value = amount,
                onValueChange = { amount = it },
                label = "Monto",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "¿Conoces el día de cobro?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = hasDayOfMonth,
                        onCheckedChange = { hasDayOfMonth = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ForestGreen
                        )
                    )
                }

                if (hasDayOfMonth) {
                    AppTextField(
                        value = dayOfMonth,
                        onValueChange = { dayOfMonth = it },
                        label = "Día del mes (1-31)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                } else {
                    Text(
                        "Se aplicará una vez en cada corte sin importar la fecha exacta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                "Categorías",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)) {
                items(uiState.categories) { category ->
                    AppChip(
                        selected = selectedCategoryIds.contains(category.id),
                        onClick = {
                            selectedCategoryIds = if (selectedCategoryIds.contains(category.id))
                                selectedCategoryIds - category.id
                            else
                                selectedCategoryIds + category.id
                        },
                        label = category.name,
                        onDelete = if (!category.isDefault) {
                            { categoryToDelete = category }
                        } else null
                    )
                }
                item {
                    AssistChip(
                        onClick = { showAddCategoryDialog = true },
                        label = { Text("Nueva") },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            if (showAddCategoryDialog) {
                AddCategoryDialog(
                    onDismiss = { showAddCategoryDialog = false },
                    onConfirm = { newCategory ->
                        viewModel.saveCategory(newCategory)
                        showAddCategoryDialog = false
                    }
                )
            }

            Spacer(Modifier.height(Dimensions.SpacingMd))

            AppButton(
                text = if (isEditMode) "Actualizar Gasto Recurrente" else "Guardar Gasto Recurrente",
                onClick = {
                    val day = if (hasDayOfMonth) dayOfMonth.toIntOrNull() else null
                    viewModel.saveRecurringExpense(
                        description = description,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        dayOfMonth = day,
                        categoryIds = selectedCategoryIds.toList(),
                        existingId = recurringExpenseId
                    )
                    onBack()
                },
                enabled = description.isNotBlank() && amount.isNotBlank()
            )
        }
    }
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (com.alvaronolasco.creditcardtracker.data.entity.Category) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Categoría") },
        text = {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Nombre") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        onConfirm(
                            com.alvaronolasco.creditcardtracker.data.entity.Category(
                                name = categoryName,
                                icon = "custom",
                                isDefault = false
                            )
                        )
                    }
                }
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
