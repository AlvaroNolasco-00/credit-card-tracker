package com.alvaronolasco.creditcardtracker.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaronolasco.creditcardtracker.data.entity.ActivityLog
import com.alvaronolasco.creditcardtracker.ui.components.AppChip
import com.alvaronolasco.creditcardtracker.ui.components.AppLoadingIndicator
import com.alvaronolasco.creditcardtracker.ui.components.AppTopBar
import com.alvaronolasco.creditcardtracker.ui.components.EmptyStateView
import com.alvaronolasco.creditcardtracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val categoryFilters = listOf(
    "EXPENSE" to "Gastos",
    "CARD" to "Tarjetas",
    "INCOME" to "Ingresos",
    "BUDGET" to "Presupuesto",
    "NOTIFICATION" to "Notificaciones",
    "CATEGORY" to "Categorías"
)

private fun categoryIcon(category: String): ImageVector = when (category) {
    "EXPENSE" -> Icons.Default.ReceiptLong
    "CARD" -> Icons.Default.CreditCard
    "INCOME" -> Icons.Default.AttachMoney
    "BUDGET" -> Icons.Default.PieChart
    "NOTIFICATION" -> Icons.Default.Notifications
    "CATEGORY" -> Icons.Default.Label
    else -> Icons.Default.Info
}

private fun actionColor(action: String): Color = when (action) {
    "CREATED" -> Color(0xFF4CAF50)
    "UPDATED" -> Color(0xFFFFA726)
    "DELETED" -> Color(0xFFEF5350)
    else -> Color.Gray
}

private fun actionLabel(action: String): String = when (action) {
    "CREATED" -> "Creado"
    "UPDATED" -> "Actualizado"
    "DELETED" -> "Eliminado"
    else -> action
}

private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "MX"))

@Composable
fun ActivityScreen(
    onBack: () -> Unit,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedLog by remember { mutableStateOf<ActivityLog?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Actividad",
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
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimensions.SpacingMd, vertical = Dimensions.SpacingSm),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) },
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
                items(categoryFilters) { (key, label) ->
                    AppChip(
                        selected = uiState.selectedCategory == key,
                        onClick = {
                            viewModel.selectCategory(if (uiState.selectedCategory == key) null else key)
                        },
                        label = label
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            when {
                uiState.isLoading -> AppLoadingIndicator()
                uiState.logs.isEmpty() -> EmptyStateView(
                    message = "No hay actividad registrada",
                    icon = Icons.Default.History
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = Dimensions.SpacingMd,
                        vertical = Dimensions.SpacingSm
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)
                ) {
                    items(uiState.logs) { log ->
                        ActivityLogItem(
                            log = log,
                            onClick = { selectedLog = log }
                        )
                    }
                }
            }
        }
    }

    selectedLog?.let { log ->
        ActivityDetailDialog(
            log = log,
            onDismiss = { selectedLog = null }
        )
    }
}

@Composable
private fun ActivityLogItem(
    log: ActivityLog,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(Dimensions.SpacingMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMd)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(log.category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = CircleShape,
                color = actionColor(log.action).copy(alpha = 0.15f)
            ) {
                Text(
                    text = actionLabel(log.action),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = actionColor(log.action),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ActivityDetailDialog(
    log: ActivityLog,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(Dimensions.SpacingLg),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMd)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon(log.category),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Detalle de actividad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                DetailRow(label = "Descripción", value = log.description)
                DetailRow(label = "Acción", value = actionLabel(log.action), valueColor = actionColor(log.action))
                DetailRow(label = "Categoría", value = categoryFilters.find { it.first == log.category }?.second ?: log.category)
                DetailRow(label = "Fecha", value = dateFormat.format(Date(log.timestamp)))
                if (log.entityId != null) {
                    DetailRow(label = "ID entidad", value = log.entityId.toString())
                }

                Spacer(Modifier.height(Dimensions.SpacingSm))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}
