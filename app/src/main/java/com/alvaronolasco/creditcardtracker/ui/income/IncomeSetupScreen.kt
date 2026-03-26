package com.alvaronolasco.creditcardtracker.ui.income

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
import com.alvaronolasco.creditcardtracker.data.entity.IncomeEntry
import com.alvaronolasco.creditcardtracker.ui.components.*
import com.alvaronolasco.creditcardtracker.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeSetupScreen(
    onBack: () -> Unit,
    onAddIncome: () -> Unit,
    onEditIncome: (Int) -> Unit,
    viewModel: IncomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Ajustes de Ingresos",
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
                onClick = onAddIncome,
                containerColor = com.alvaronolasco.creditcardtracker.ui.theme.ForestGreen,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Ingreso")
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
                item {
                    ProfileSummaryCard(
                        employmentType = uiState.profile?.employmentType ?: "EMPLOYED",
                        incomeMode = uiState.profile?.incomeMode ?: "RECURRING",
                        onUpdateProfile = { type, mode -> viewModel.updateProfile(type, mode) }
                    )
                }

                item {
                    IncomeOverviewCard(totalIncome = uiState.totalMonthlyIncome)
                }

                item {
                    Text(
                        text = "Tus ingresos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = Dimensions.SpacingSm)
                    )
                }

                if (uiState.activeEntries.isEmpty()) {
                    item {
                        EmptyStateView(
                            message = "No hay ingresos registrados este mes",
                            modifier = Modifier.height(200.dp)
                        )
                    }
                } else {
                    items(uiState.activeEntries) { entry ->
                        IncomeEntryItem(
                            entry = entry,
                            onEdit = { onEditIncome(entry.id) },
                            onDelete = { viewModel.deleteEntry(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSummaryCard(
    employmentType: String,
    incomeMode: String,
    onUpdateProfile: (String, String) -> Unit
) {
    AppCard {
        Column(
            modifier = Modifier.padding(Dimensions.SpacingMd),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)
        ) {
            Text(
                "Perfil de ingresos", 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)) {
                AppChip(
                    label = "Empleado",
                    selected = employmentType == "EMPLOYED",
                    onClick = { onUpdateProfile("EMPLOYED", "RECURRING") },
                    modifier = Modifier.weight(1f)
                )
                AppChip(
                    label = "Independiente",
                    selected = employmentType == "FREELANCER",
                    onClick = { onUpdateProfile("FREELANCER", incomeMode) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (employmentType == "FREELANCER") {
                Spacer(Modifier.height(Dimensions.SpacingXs))
                Text("Modo de registro", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)) {
                    AppChip(
                        label = "Recurrente",
                        selected = incomeMode == "RECURRING",
                        onClick = { onUpdateProfile(employmentType, "RECURRING") },
                        modifier = Modifier.weight(1f)
                    )
                    AppChip(
                        label = "Manual mensual",
                        selected = incomeMode == "MONTHLY_PROMPT",
                        onClick = { onUpdateProfile(employmentType, "MONTHLY_PROMPT") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun IncomeOverviewCard(totalIncome: Double) {
    AppCard(
        containerColor = com.alvaronolasco.creditcardtracker.ui.theme.MintGreen,
        border = null
    ) {
        Column(modifier = Modifier.padding(Dimensions.SpacingMd)) {
            Text(
                "Total ingresos del mes", 
                style = MaterialTheme.typography.labelMedium,
                color = com.alvaronolasco.creditcardtracker.ui.theme.ForestGreen.copy(alpha = 0.7f)
            )
            Text(
                text = "$${String.format("%.2f", totalIncome)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = com.alvaronolasco.creditcardtracker.ui.theme.ForestGreen
            )
        }
    }
}

@Composable
fun IncomeEntryItem(
    entry: IncomeEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(onClick = onEdit) {
        Row(
            modifier = Modifier
                .padding(Dimensions.SpacingMd)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (entry.isRecurring) "Recurrente (Día ${entry.dayOfMonth})" 
                           else "Único (${entry.monthYear})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = entry.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format("%.2f", entry.amount)}",
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
