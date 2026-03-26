package com.alvaronolasco.creditcardtracker.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaronolasco.creditcardtracker.data.entity.NotificationConfig
import com.alvaronolasco.creditcardtracker.ui.theme.*
import com.alvaronolasco.creditcardtracker.ui.components.*
import com.alvaronolasco.creditcardtracker.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardScreen(
    cardId: Int? = null,
    onBack: () -> Unit,
    viewModel: CardsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var lastFour by remember { mutableStateOf("") }
    var creditLimit by remember { mutableStateOf("") }
    var cutOffDay by remember { mutableStateOf("1") }
    var paymentDay by remember { mutableStateOf("1") }
    var extraFinancingPayment by remember { mutableStateOf("") }
    var selectedCardColor by remember { mutableStateOf(CardBlue) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }

    val colors = listOf(CardBlue, CardGreen, CardRed, CardYellow, CardPurple, CardOrange, CardDark)

    LaunchedEffect(cardId) {
        if (cardId != null) {
            viewModel.loadCard(cardId)
        }
    }

    LaunchedEffect(uiState.editingCard) {
        uiState.editingCard?.let { card ->
            name = card.name
            bank = card.bank
            lastFour = card.lastFourDigits
            creditLimit = card.creditLimit.toString()
            cutOffDay = card.cutOffDay.toString()
            paymentDay = card.paymentDueDay.toString()
            extraFinancingPayment = if (card.extraFinancingPayment > 0.0) card.extraFinancingPayment.toString() else ""
            selectedCardColor = Color(card.color)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Tarjeta") },
            text = { Text("¿Estás seguro de que deseas eliminar esta tarjeta? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        cardId?.let { viewModel.deleteCard(it, onBack) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (cardId == null) "New Card" else "Edit Card",
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
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    if (cardId != null) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = CircleShape
                                )
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
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
                    .padding(Dimensions.SpacingMd)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMd)
            ) {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre de la Tarjeta (Ej: Oro)"
                )

                AppTextField(
                    value = bank,
                    onValueChange = { bank = it },
                    label = "Banco"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMd)) {
                    AppTextField(
                        value = lastFour,
                        onValueChange = { if (it.length <= 4) lastFour = it },
                        label = "Últimos 4 dígitos",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = creditLimit,
                        onValueChange = { creditLimit = it },
                        label = "Límite de Crédito",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    "Date configuration", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMd)) {
                    AppTextField(
                        value = cutOffDay,
                        onValueChange = { cutOffDay = it },
                        label = "Cut-off Day",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = paymentDay,
                        onValueChange = { paymentDay = it },
                        label = "Payment Limit",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    "Extrafinanciamiento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )

                AppTextField(
                    value = extraFinancingPayment,
                    onValueChange = { extraFinancingPayment = it },
                    label = "Cuota mensual (dejar vacío si no aplica)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Text(
                    "Card color",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSm)
                ) {
                    colors.forEach { color: Color ->
                        val isSelected = selectedCardColor == color
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color = color)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, com.alvaronolasco.creditcardtracker.ui.theme.ForestGreen, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedCardColor = color }
                        )
                    }
                }

                // Notification settings — only visible when editing an existing card
                if (cardId != null && uiState.notificationConfigs.isNotEmpty()) {
                    NotificationSettingsSection(
                        configs = uiState.notificationConfigs,
                        onToggle = { viewModel.toggleNotificationConfig(it) }
                    )
                }

                Spacer(Modifier.height(Dimensions.SpacingMd))

                AppButton(
                    text = if (cardId == null) "Guardar Tarjeta" else "Actualizar Tarjeta",
                    onClick = {
                        viewModel.saveCard(
                            name, bank, lastFour, selectedCardColor.toArgb(),
                            cutOffDay.toIntOrNull() ?: 1,
                            paymentDay.toIntOrNull() ?: 1,
                            creditLimit.toDoubleOrNull() ?: 0.0,
                            extraFinancingPayment = extraFinancingPayment.toDoubleOrNull() ?: 0.0,
                            existingCardId = cardId,
                            onSuccess = onBack
                        )
                    },
                    enabled = name.isNotBlank() && bank.isNotBlank()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsSection(
    configs: List<NotificationConfig>,
    onToggle: (NotificationConfig) -> Unit
) {
    val dayLabels = mapOf(0 to "El día", 1 to "1 día antes", 3 to "3 días antes", 5 to "5 días antes")
    val sections = listOf(
        "CUT_OFF" to "Corte",
        "PAYMENT" to "Pago"
    )

    Text(
        "Notificaciones",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onBackground
    )

    sections.forEach { (type: String, label: String) ->
        val typeConfigs = configs.filter { it.type == type }.sortedBy { it.daysBefore }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    typeConfigs.forEach { config ->
                        FilterChip(
                            selected = config.enabled,
                            onClick = { onToggle(config) },
                            label = {
                                Text(
                                    dayLabels[config.daysBefore] ?: "${config.daysBefore}d",
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForestGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

