package com.alvaronolasco.creditcardtracker.ui.income

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaronolasco.creditcardtracker.ui.components.*
import com.alvaronolasco.creditcardtracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditIncomeScreen(
    incomeId: Int? = null,
    onBack: () -> Unit,
    viewModel: IncomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dayOfMonth by remember { mutableStateOf("1") }
    var isRecurring by remember { mutableStateOf(true) }
    var selectedType by remember { mutableStateOf("BASE") }

    val incomeTypes = listOf("BASE", "COMMISSION", "BONUS", "OTHER")

    LaunchedEffect(incomeId, uiState.activeEntries) {
        if (incomeId != null) {
            val entry = uiState.activeEntries.find { it.id == incomeId }
            entry?.let {
                label = it.label
                amount = it.amount.toString()
                dayOfMonth = it.dayOfMonth.toString()
                isRecurring = it.isRecurring
                selectedType = it.type
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (incomeId == null) "Nuevo Ingreso" else "Editar Ingreso",
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
            AppTextField(
                value = label,
                onValueChange = { label = it },
                label = "Descripción (ej: Salario)"
            )

            AppTextField(
                value = amount,
                onValueChange = { amount = it },
                label = "Monto",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            AppTextField(
                value = dayOfMonth,
                onValueChange = { dayOfMonth = it },
                label = "Día del mes (1-31)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Text(
                "Tipo de ingreso", 
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)
            ) {
                incomeTypes.forEach { type ->
                    AppChip(
                        label = when(type) {
                            "BASE" -> "Base"
                            "COMMISSION" -> "Comisión"
                            "BONUS" -> "Bono"
                            else -> "Otro"
                        },
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿Es ingreso recurrente?", modifier = Modifier.weight(1f))
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ForestGreen
                    )
                )
            }

            if (!isRecurring) {
                Text(
                    "Este ingreso solo se aplicará al mes actual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(Dimensions.SpacingMd))

            AppButton(
                text = if (incomeId == null) "Guardar Ingreso" else "Actualizar Ingreso",
                onClick = {
                    viewModel.saveEntry(
                        label = label,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        dayOfMonth = dayOfMonth.toIntOrNull() ?: 1,
                        isRecurring = isRecurring,
                        type = selectedType,
                        existingId = incomeId
                    )
                    onBack()
                },
                enabled = label.isNotBlank() && amount.isNotBlank()
            )
        }
    }
}

