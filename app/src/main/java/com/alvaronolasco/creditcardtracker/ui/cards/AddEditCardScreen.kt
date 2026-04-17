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
import com.alvaronolasco.creditcardtracker.data.entity.SupportedBank
import com.alvaronolasco.creditcardtracker.ui.theme.*
import com.alvaronolasco.creditcardtracker.ui.components.*
import com.alvaronolasco.creditcardtracker.ui.theme.Dimensions
import com.alvaronolasco.creditcardtracker.ui.components.BankPicker
import com.alvaronolasco.creditcardtracker.ui.components.CardColorPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardScreen(
    cardId: Int? = null,
    onBack: () -> Unit,
    viewModel: CardsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var selectedBankId by remember { mutableStateOf<String?>(null) }
    var customBankName by remember { mutableStateOf("") }
    var lastFour by remember { mutableStateOf("") }
    var creditLimit by remember { mutableStateOf("") }
    var cutOffDay by remember { mutableStateOf("1") }
    var paymentDay by remember { mutableStateOf("1") }
    var extraFinancingPayment by remember { mutableStateOf("") }
    var selectedCardColor by remember { mutableStateOf(CardBlue) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cardId) {
        if (cardId != null) {
            viewModel.loadCard(cardId)
        }
    }

    LaunchedEffect(uiState.editingCard) {
        uiState.editingCard?.let { card ->
            name = card.name
            val resolvedBank = card.bankId?.let { SupportedBank.fromId(it) }
                ?: SupportedBank.fromDisplayName(card.bank)
            selectedBankId = resolvedBank?.id
            customBankName = if (resolvedBank == null) card.bank else ""
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
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
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

                BankPicker(
                    selectedBankId = selectedBankId,
                    customBankName = customBankName,
                    onBankSelected = { id, name -> selectedBankId = id; customBankName = if (id == null) name else "" }
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
                CardColorPicker(
                    selectedColor = selectedCardColor,
                    onColorSelected = { selectedCardColor = it }
                )

                // Notification settings — only visible when editing an existing card
                if (cardId != null && uiState.notificationConfigs.isNotEmpty()) {
                    NotificationSettingsSection(
                        configs = uiState.notificationConfigs,
                        onToggle = { viewModel.toggleNotificationConfig(it) }
                    )
                }

                Spacer(Modifier.height(Dimensions.SpacingMd))

                val effectiveBankName = SupportedBank.fromId(selectedBankId)?.displayName ?: customBankName

                AppButton(
                    text = if (cardId == null) "Guardar Tarjeta" else "Actualizar Tarjeta",
                    onClick = {
                        viewModel.saveCard(
                            name = name,
                            bank = effectiveBankName,
                            bankId = selectedBankId,
                            lastFour = lastFour,
                            color = selectedCardColor.toArgb(),
                            cutOff = cutOffDay.toIntOrNull() ?: 1,
                            payment = paymentDay.toIntOrNull() ?: 1,
                            limit = creditLimit.toDoubleOrNull() ?: 0.0,
                            extraFinancingPayment = extraFinancingPayment.toDoubleOrNull() ?: 0.0,
                            existingCardId = cardId,
                            onSuccess = onBack
                        )
                    },
                    enabled = name.isNotBlank() && effectiveBankName.isNotBlank()
                )
            }
        }
    }
}

@Composable
fun NotificationSettingsSection(
    configs: List<NotificationConfig>,
    onToggle: (NotificationConfig) -> Unit
) {
    val dayLabels = mapOf(0 to "El mismo día", 1 to "1 día antes", 3 to "3 días antes", 5 to "5 días antes")
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

    sections.forEach { (type, label) ->
        val typeConfigs = configs.filter { it.type == type }.sortedBy { it.daysBefore }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Recordatorios de $label",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                typeConfigs.forEachIndexed { index, config ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dayLabels[config.daysBefore] ?: "${config.daysBefore} días antes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = config.enabled,
                            onCheckedChange = { onToggle(config) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}

