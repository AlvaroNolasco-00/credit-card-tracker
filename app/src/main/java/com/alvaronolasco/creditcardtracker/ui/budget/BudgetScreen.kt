package com.alvaronolasco.creditcardtracker.ui.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaronolasco.creditcardtracker.data.entity.BudgetItem
import com.alvaronolasco.creditcardtracker.data.entity.Category
import com.alvaronolasco.creditcardtracker.ui.components.AppButton
import com.alvaronolasco.creditcardtracker.ui.components.AppOutlinedButton
import com.alvaronolasco.creditcardtracker.ui.components.AppTextField
import com.alvaronolasco.creditcardtracker.ui.components.AppTopBar
import com.alvaronolasco.creditcardtracker.ui.theme.*
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf<BudgetCategoryState?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Presupuesto",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {}
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                MonthSelector(
                    monthYear = uiState.monthYear,
                    onPrevious = { viewModel.navigateMonth(false) },
                    onNext = { viewModel.navigateMonth(true) }
                )
            }

            item {
                BudgetSummaryCard(
                    totalBudgeted = uiState.totalBudgeted,
                    totalSpent = uiState.totalSpent
                )
            }

            if (uiState.categories.none { it.budgetItem != null } && uiState.hasPreviousMonthBudget) {
                item {
                    TextButton(
                        onClick = { viewModel.copyFromPreviousMonth() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Copiar presupuesto del mes anterior",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            val withBudget = uiState.categories.filter { it.budgetItem != null }
            val withoutBudget = uiState.categories.filter { it.budgetItem == null }

            if (withBudget.isNotEmpty()) {
                item {
                    Text(
                        text = "Con presupuesto",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(withBudget, key = { it.category.id }) { state ->
                    BudgetCategoryRow(
                        state = state,
                        onClick = { selectedCategory = state }
                    )
                }
            }

            if (withoutBudget.isNotEmpty()) {
                item {
                    Text(
                        text = "Sin presupuesto",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = if (withBudget.isNotEmpty()) 8.dp else 4.dp)
                    )
                }
                items(withoutBudget, key = { it.category.id }) { state ->
                    BudgetCategoryRow(
                        state = state,
                        onClick = { selectedCategory = state }
                    )
                }
            }
        }
    }

    selectedCategory?.let { state ->
        BudgetEditDialog(
            state = state,
            onDismiss = { selectedCategory = null },
            onSave = { amount ->
                viewModel.saveBudget(state.category, amount)
                selectedCategory = null
            },
            onDelete = { budgetItem ->
                viewModel.deleteBudget(budgetItem)
                selectedCategory = null
            }
        )
    }
}

@Composable
private fun MonthSelector(
    monthYear: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val ym = YearMonth.parse(monthYear)
    val label = ym.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("es", "MX"))
        .replaceFirstChar { it.uppercase() } + " ${ym.year}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Mes anterior",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Mes siguiente",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BudgetSummaryCard(
    totalBudgeted: Double,
    totalSpent: Double
) {
    val fraction = if (totalBudgeted > 0) (totalSpent / totalBudgeted).coerceIn(0.0, 1.0) else 0.0
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.toFloat(),
        animationSpec = tween(600),
        label = "budget_progress"
    )
    val progressColor = when {
        fraction >= 1.0 -> ErrorRed
        fraction >= 0.8 -> Color(0xFFF9A825)
        else -> SuccessGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Gastado",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${"%,.2f".format(totalSpent)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if (totalSpent > totalBudgeted && totalBudgeted > 0) ErrorRed
                               else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Presupuesto",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (totalBudgeted > 0) "$${"%,.2f".format(totalBudgeted)}" else "Sin definir",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (totalBudgeted > 0) {
                LinearProgressIndicator(
                    progress = { animatedFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                val pct = (fraction * 100).toInt()
                Text(
                    text = if (fraction >= 1.0) "Presupuesto superado" else "$pct% utilizado",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (fraction >= 1.0) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BudgetCategoryRow(
    state: BudgetCategoryState,
    onClick: () -> Unit
) {
    val fraction = if ((state.budgetItem?.limitAmount ?: 0.0) > 0)
        (state.actualSpending / state.budgetItem!!.limitAmount).coerceIn(0.0, 1.0)
    else 0.0
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.toFloat(),
        animationSpec = tween(600),
        label = "cat_progress_${state.category.id}"
    )
    val progressColor = when {
        state.budgetItem == null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        fraction >= 1.0 -> ErrorRed
        fraction >= 0.8 -> Color(0xFFF9A825)
        else -> SuccessGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MintGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.category.name.first().uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (state.budgetItem != null)
                            "$${"%,.0f".format(state.actualSpending)} / $${"%,.0f".format(state.budgetItem.limitAmount)}"
                        else
                            "$${"%,.0f".format(state.actualSpending)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.budgetItem != null && state.actualSpending > state.budgetItem.limitAmount)
                            ErrorRed
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.budgetItem != null) {
                    LinearProgressIndicator(
                        progress = { animatedFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetEditDialog(
    state: BudgetCategoryState,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onDelete: (BudgetItem) -> Unit
) {
    var amountText by remember {
        mutableStateOf(state.budgetItem?.limitAmount?.let { "%.2f".format(it) } ?: "")
    }
    val amount = amountText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = state.category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.actualSpending > 0) {
                    Text(
                        text = "Gasto actual: $${"%,.2f".format(state.actualSpending)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AppTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Límite de presupuesto",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            AppButton(
                text = if (state.budgetItem == null) "Guardar" else "Actualizar",
                onClick = { amount?.let { onSave(it) } },
                modifier = Modifier.width(130.dp),
                enabled = amount != null && amount > 0
            )
        },
        dismissButton = {
            if (state.budgetItem != null) {
                AppOutlinedButton(
                    text = "Eliminar",
                    onClick = { onDelete(state.budgetItem) },
                    modifier = Modifier.width(110.dp)
                )
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
