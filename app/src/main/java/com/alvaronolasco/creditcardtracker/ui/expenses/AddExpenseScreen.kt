package com.alvaronolasco.creditcardtracker.ui.expenses

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.navigation.NavController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.alvaronolasco.creditcardtracker.data.entity.Category
import com.alvaronolasco.creditcardtracker.data.entity.CreditCard
import com.alvaronolasco.creditcardtracker.data.entity.PaymentMethod
import com.alvaronolasco.creditcardtracker.ui.components.*
import com.alvaronolasco.creditcardtracker.ui.theme.Dimensions
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    cardId: Int? = null,
    expenseId: Int? = null,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    navController: NavController,
    viewModel: ExpensesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var msiEnabled by remember { mutableStateOf(false) }
    var msiMonths by remember { mutableStateOf(3) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedCardId by remember { mutableStateOf(cardId) }
    var showCardPicker by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember {
        mutableStateOf(if (cardId != null) PaymentMethod.CREDIT_CARD else PaymentMethod.DEBIT_CARD)
    }

    val isEditMode = expenseId != null

    LaunchedEffect(cardId) {
        if (!isEditMode && cardId != null && cardId > 0) {
            selectedCardId = cardId
        }
    }

    LaunchedEffect(expenseId) {
        if (expenseId != null) {
            viewModel.loadExpense(expenseId)
        }
    }

    LaunchedEffect(uiState.currentExpense) {
        uiState.currentExpense?.let { ewc ->
            amount = ewc.expense.amount.toString()
            description = ewc.expense.description
            selectedCategoryIds = ewc.categories.map { it.id }.toSet()
            capturedImageUri = ewc.expense.receiptImagePath?.let { Uri.parse(it) }
            selectedDateMillis = ewc.expense.date
            selectedCardId = ewc.expense.cardId
            selectedPaymentMethod = runCatching {
                PaymentMethod.valueOf(ewc.expense.paymentMethod)
            }.getOrElse { PaymentMethod.CREDIT_CARD }
            ewc.expense.cardId?.let { viewModel.loadCard(it) }
            if (ewc.expense.msiMonths > 1) {
                msiEnabled = true
                msiMonths = ewc.expense.msiMonths
            }
        }
    }

    LaunchedEffect(selectedCardId) {
        val cid = selectedCardId
        if (cid != null && cid > 0) viewModel.loadCard(cid)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            capturedImageUri = it
            viewModel.processOcr(it)
        }
    }

    LaunchedEffect(navController.currentBackStackEntry) {
        navController.currentBackStackEntry?.savedStateHandle?.get<String>("captured_image_uri")?.let { uriString ->
            val uri = Uri.parse(uriString)
            capturedImageUri = uri
            viewModel.processOcr(uri)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("captured_image_uri")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onOpenCamera()
        }
    }

    LaunchedEffect(uiState.ocrResultAmount) {
        uiState.ocrResultAmount?.let {
            amount = it.toBigDecimal().stripTrailingZeros().toPlainString()
                .replace("[^0-9.]".toRegex(), "")
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isEditMode) "Editar Gasto" else if (selectedPaymentMethod == PaymentMethod.CREDIT_CARD) "Gasto de Tarjeta" else "Gasto Personal",
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
                        @Suppress("DEPRECATION")
                        Icon(
                            Icons.Filled.ArrowBack,
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
                .padding(Dimensions.SpacingMd)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMd)
        ) {
            PaymentMethodSelector(
                selected = selectedPaymentMethod,
                onSelect = { method ->
                    selectedPaymentMethod = method
                    if (method != PaymentMethod.CREDIT_CARD) {
                        selectedCardId = null
                        viewModel.clearCurrentCard()
                        msiEnabled = false
                    } else if (cardId != null && cardId > 0) {
                        selectedCardId = cardId
                    }
                }
            )

            if (selectedPaymentMethod == PaymentMethod.CREDIT_CARD) {
                uiState.currentCard?.let { card ->
                    CardTargetBanner(card, onClick = { showCardPicker = true })
                }
            }

            if (showCardPicker && selectedPaymentMethod == PaymentMethod.CREDIT_CARD) {
                AlertDialog(
                    onDismissRequest = { showCardPicker = false },
                    title = { Text("Cambiar tarjeta") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            uiState.allCards.forEach { card ->
                                val isSelected = card.id == selectedCardId
                                TextButton(
                                    onClick = {
                                        selectedCardId = card.id
                                        showCardPicker = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (isSelected)
                                            MaterialTheme.colorScheme.secondary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = card.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                text = "${card.bank} · **** ${card.lastFourDigits}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(
                                                        color = Color(card.color),
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(
                            onClick = { showCardPicker = false },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) { Text("Cancelar") }
                    }
                )
            }

            AppTextField(
                value = amount,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        amount = newValue
                    }
                },
                label = "Monto ($)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
            )

            AppTextField(
                value = description,
                onValueChange = { description = it },
                label = "Descripción"
            )

            DatePickerSection(
                selectedDateMillis = selectedDateMillis,
                onClick = { showDatePicker = true }
            )

            if (selectedPaymentMethod == PaymentMethod.CREDIT_CARD) {
                MsiSection(
                    enabled = msiEnabled,
                    onEnabledChange = { msiEnabled = it },
                    selectedMonths = msiMonths,
                    onMonthsChange = { msiMonths = it },
                    totalAmount = amount.toDoubleOrNull() ?: 0.0
                )
            }

            Text(
                "Categorías",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
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

            if (showDatePicker) {
                val zoneId = ZoneId.systemDefault()
                val today = LocalDate.now(zoneId)
                val todayStartMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

                // If an existing expense has a future date, clamp it so we never keep/submit a future selection.
                val initialSelectedDateMillis = run {
                    val selectedLocalDate = Instant.ofEpochMilli(selectedDateMillis)
                        .atZone(zoneId)
                        .toLocalDate()
                    if (selectedLocalDate.isAfter(today)) todayStartMillis else selectedDateMillis
                }

                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = initialSelectedDateMillis,
                    selectableDates = object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            val localDate = Instant.ofEpochMilli(utcTimeMillis)
                                .atZone(zoneId)
                                .toLocalDate()
                            return !localDate.isAfter(today) // Allow today and past only.
                        }

                        override fun isSelectableYear(year: Int): Boolean {
                            // If the year is after the current year, all its dates are non-selectable.
                            return year <= today.year
                        }
                    }
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                                showDatePicker = false
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.secondary
                            )
                        ) { Text("Aceptar") }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDatePicker = false },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) { Text("Cancelar") }
                    }
                ) {
                    DatePicker(
                        state = datePickerState,
                        colors = DatePickerDefaults.colors(
                            selectedDayContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedDayContentColor = MaterialTheme.colorScheme.onSecondary,
                            todayDateBorderColor = MaterialTheme.colorScheme.secondary,
                            todayContentColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }

            if (showAddCategoryDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showAddCategoryDialog = false
                        newCategoryName = ""
                    },
                    title = { Text("Nueva categoría") },
                    text = {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("Nombre") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val name = newCategoryName.trim()
                                if (name.isNotEmpty()) {
                                    viewModel.createCategory(name) { id ->
                                        selectedCategoryIds = selectedCategoryIds + id
                                    }
                                }
                                showAddCategoryDialog = false
                                newCategoryName = ""
                            },
                            enabled = newCategoryName.isNotBlank(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.secondary
                            )
                        ) { Text("Crear") }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showAddCategoryDialog = false
                                newCategoryName = ""
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) { Text("Cancelar") }
                    }
                )
            }

            categoryToDelete?.let { cat ->
                AlertDialog(
                    onDismissRequest = { categoryToDelete = null },
                    title = { Text("Eliminar categoría") },
                    text = { Text("¿Eliminar \"${cat.name}\"? Los gastos con esta categoría quedarán sin ella.") },
                    confirmButton = {
                        TextButton(onClick = {
                            selectedCategoryIds = selectedCategoryIds - cat.id
                            viewModel.deleteCategory(cat)
                            categoryToDelete = null
                        }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { categoryToDelete = null },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) { Text("Cancelar") }
                    }
                )
            }

            if (uiState.ocrDialogState != OcrDialogState.NONE) {
                OcrLowConfidenceDialog(
                    dialogState = uiState.ocrDialogState,
                    pendingAmount = uiState.ocrPendingAmount,
                    imageUri = capturedImageUri,
                    isProcessing = uiState.ocrProcessing,
                    onConfirm = { viewModel.confirmOcrAmount() },
                    onShowCrop = { viewModel.showOcrCropMode() },
                    onCropConfirm = { bitmap -> viewModel.processOcrFromCrop(bitmap) },
                    onDismiss = { viewModel.dismissOcrDialog() }
                )
            }

            Text(
                "Recibo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (capturedImageUri != null) {
                AppCard(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    elevation = Dimensions.ElevationLow
                ) {
                    Box {
                        AsyncImage(
                            model = capturedImageUri,
                            contentDescription = "Ticket",
                            modifier = Modifier.fillMaxSize()
                        )
                        if (uiState.ocrProcessing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Analizando recibo...",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMd)
            ) {
                AppOutlinedButton(
                    text = "Tomar Foto",
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    icon = Icons.Default.CameraAlt,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.ocrProcessing
                )
                AppOutlinedButton(
                    text = "Galería",
                    onClick = { galleryLauncher.launch("image/*") },
                    icon = Icons.Default.PhotoLibrary,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.ocrProcessing
                )
            }

            if (capturedImageUri != null) {
                TextButton(
                    onClick = { viewModel.showOcrCropMode() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.ocrProcessing,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = "Seleccionar área del monto manualmente",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(Dimensions.SpacingSm))

            AppButton(
                text = if (isEditMode) "Actualizar Gasto" else "Guardar Gasto",
                onClick = {
                    viewModel.saveExpense(
                        cardId = selectedCardId,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        description = description,
                        categoryIds = selectedCategoryIds.toList(),
                        imagePath = capturedImageUri?.toString(),
                        date = selectedDateMillis,
                        expenseId = expenseId,
                        msiMonths = if (msiEnabled) msiMonths else 1,
                        paymentMethod = selectedPaymentMethod,
                        onSuccess = onBack
                    )
                },
                enabled = amount.isNotBlank() && description.isNotBlank() && !uiState.ocrProcessing
                    && (selectedPaymentMethod != PaymentMethod.CREDIT_CARD || selectedCardId != null)
            )

            if (isEditMode) {
                TextButton(
                    onClick = {
                        uiState.currentExpense?.let {
                            viewModel.deleteExpense(it.expense, onBack)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar Gasto")
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodSelector(
    selected: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit
) {
    val methods = listOf(
        PaymentMethod.DEBIT_CARD,
        PaymentMethod.TRANSFER,
        PaymentMethod.CASH,
        PaymentMethod.CREDIT_CARD
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Método de pago",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(methods) { method ->
                FilterChip(
                    selected = selected == method,
                    onClick = { onSelect(method) },
                    label = { Text(method.label, style = MaterialTheme.typography.bodySmall) },
                    shape = RoundedCornerShape(50)
                )
            }
        }
    }
}

@Composable
private fun MsiSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    selectedMonths: Int,
    onMonthsChange: (Int) -> Unit,
    totalAmount: Double
) {
    val msiOptions = listOf(3, 6, 9, 12, 18, 24)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Meses sin intereses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!enabled) {
                    Text(
                        text = "Pago de contado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }

        if (enabled) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(msiOptions) { months ->
                    FilterChip(
                        selected = selectedMonths == months,
                        onClick = { onMonthsChange(months) },
                        label = { Text("${months}M") },
                        shape = RoundedCornerShape(50)
                    )
                }
            }

            if (totalAmount > 0) {
                val monthly = totalAmount / selectedMonths
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$selectedMonths pagos de",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$${String.format("%.2f", monthly)}/mes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun CardTargetBanner(card: CreditCard, onClick: () -> Unit) {
    val cardColor = Color(card.color)
    val darkColor = Color(
        red = (cardColor.red * 0.65f).coerceIn(0f, 1f),
        green = (cardColor.green * 0.65f).coerceIn(0f, 1f),
        blue = (cardColor.blue * 0.65f).coerceIn(0f, 1f)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(
                brush = Brush.horizontalGradient(listOf(cardColor, darkColor)),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.bank,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "**** ${card.lastFourDigits}",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Cambiar",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = card.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun DatePickerSection(
    selectedDateMillis: Long,
    onClick: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es"))
    val dateLabel = Instant.ofEpochMilli(selectedDateMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Fecha de transacción",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onClick) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = "Seleccionar fecha",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun OcrLowConfidenceDialog(
    dialogState: OcrDialogState,
    pendingAmount: Double?,
    imageUri: Uri?,
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onShowCrop: () -> Unit,
    onCropConfirm: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    when (dialogState) {
        OcrDialogState.CONFIRM -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Valor detectado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pendingAmount != null) {
                        Text(
                            text = "$${String.format("%.2f", pendingAmount)}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "El valor detectado tiene baja confianza. ¿Es el total correcto?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "No se pudo detectar el total automáticamente.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                if (pendingAmount != null) {
                    TextButton(
                        onClick = onConfirm,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) { Text("Sí, está bien") }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onShowCrop,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) { Text("Seleccionar área") }
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) { Text("Cancelar") }
                }
            }
        )

        OcrDialogState.CROP -> if (imageUri != null) {
            Dialog(onDismissRequest = onDismiss) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    ImageCropCanvas(
                        imageUri = imageUri,
                        isProcessing = isProcessing,
                        onCropConfirm = onCropConfirm,
                        onCancel = onDismiss
                    )
                }
            }
        }

        OcrDialogState.NONE -> Unit
    }
}

@Composable
private fun ImageCropCanvas(
    imageUri: Uri,
    isProcessing: Boolean,
    onCropConfirm: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var composableSize by remember { mutableStateOf(IntSize.Zero) }
    var selStart by remember { mutableStateOf<Offset?>(null) }
    var selEnd by remember { mutableStateOf<Offset?>(null) }
    var isDrawMode by remember { mutableStateOf(false) }
    var scale by remember { mutableStateOf(1f) }
    // Absolute canvas position of the image center.
    // Storing center (not a canvas-relative offset) means a layout resize caused by
    // the instruction text changing height won't visually shift the image.
    var imageCenter by remember { mutableStateOf(Offset.Zero) }

    // Track previous composable size so we can adjust imageCenter proportionally.
    var prevComposableSize by remember { mutableStateOf(IntSize.Zero) }

    // Center image once the canvas size is first known, and adjust
    // imageCenter proportionally if the canvas resizes later.
    LaunchedEffect(composableSize) {
        if (composableSize.width > 0 && composableSize.height > 0) {
            if (imageCenter == Offset.Zero) {
                // First time: center the image.
                imageCenter = Offset(composableSize.width / 2f, composableSize.height / 2f)
            } else if (prevComposableSize.width > 0 && prevComposableSize.height > 0) {
                // Canvas resized (e.g. layout shift): keep the image at the same
                // relative position so it doesn't appear to jump.
                val dx = (composableSize.width - prevComposableSize.width) / 2f
                val dy = (composableSize.height - prevComposableSize.height) / 2f
                imageCenter = Offset(imageCenter.x + dx, imageCenter.y + dy)
            }
            prevComposableSize = composableSize
        }
    }

    LaunchedEffect(imageUri) {
        val loaded = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }?.let { bmp -> rotateBitmapByExif(context, imageUri, bmp) }
            } catch (e: Exception) {
                null
            }
        }
        bitmap = loaded
        selStart = null
        selEnd = null
        scale = 1f
        imageCenter = if (composableSize.width > 0)
            Offset(composableSize.width / 2f, composableSize.height / 2f)
        else
            Offset.Zero
    }

    // Fitted size at scale=1 (letterbox/pillarbox).
    val baseFitSize: Size? = remember(bitmap, composableSize) {
        val bmp = bitmap ?: return@remember null
        if (composableSize.width == 0 || composableSize.height == 0) return@remember null
        val bmpRatio = bmp.width.toFloat() / bmp.height
        val canvasRatio = composableSize.width.toFloat() / composableSize.height
        if (bmpRatio > canvasRatio)
            Size(composableSize.width.toFloat(), composableSize.width / bmpRatio)
        else
            Size(composableSize.height * bmpRatio, composableSize.height.toFloat())
    }

    // Effective rect: expands from imageCenter as scale grows.
    val imageRect: Rect? = remember(baseFitSize, scale, imageCenter) {
        val sz = baseFitSize ?: return@remember null
        val halfW = sz.width * scale / 2f
        val halfH = sz.height * scale / 2f
        Rect(imageCenter.x - halfW, imageCenter.y - halfH,
             imageCenter.x + halfW, imageCenter.y + halfH)
    }

    val hasSelection = remember(selStart, selEnd) {
        val s = selStart; val e = selEnd
        s != null && e != null &&
            maxOf(s.x, e.x) - minOf(s.x, e.x) > 10f &&
            maxOf(s.y, e.y) - minOf(s.y, e.y) > 10f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Fixed height prevents text-length differences from resizing the canvas
        // (which would shift the zoom anchor and make the image appear to jump).
        Box(modifier = Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = if (isDrawMode)
                    "Dibuja un rectángulo alrededor del total"
                else
                    "Pellizca para zoom · Arrastra para mover",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }

        // Fixed height prevents the "Limpiar" button from resizing this row,
        // which would cascade into a canvas resize and shift the image.
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !isDrawMode,
                onClick = { isDrawMode = false },
                label = { Text("Zoom / Mover") },
                leadingIcon = {
                    Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )
            FilterChip(
                selected = isDrawMode,
                onClick = { isDrawMode = true },
                label = { Text("Seleccionar") },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )
            // Always present so its appearance doesn't change row height / canvas size.
            TextButton(
                onClick = { selStart = null; selEnd = null },
                enabled = hasSelection,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.alpha(if (hasSelection) 1f else 0f)
            ) {
                Text("Limpiar", style = MaterialTheme.typography.bodySmall)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
                .onSizeChanged { composableSize = it }
        ) {
            // Single canvas: draws image + selection overlay.
            // Single pointerInput keyed on isDrawMode: when the mode changes the coroutine
            // is cancelled and restarted from scratch, so neither detector ever sees
            // stale gesture state from the previous mode.
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isDrawMode) {
                        if (isDrawMode) {
                            // Draw mode: track one-finger drag to define a selection rect.
                            // awaitEachGesture loops automatically for subsequent draws.
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                selStart = down.position
                                selEnd = down.position
                                down.consume()
                                drag(down.id) { change ->
                                    selEnd = change.position
                                    change.consume()
                                }
                            }
                        } else {
                            // Zoom/pan mode: standard multi-touch transform.
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val prevScale = scale
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                val ratio = scale / prevScale
                                imageCenter = centroid + (imageCenter - centroid) * ratio + pan
                            }
                        }
                    }
            ) {
                val bmp = bitmap ?: return@Canvas
                val rect = imageRect ?: return@Canvas

                // Layer 1: image
                drawImage(
                    image = bmp.asImageBitmap(),
                    dstOffset = IntOffset(rect.left.toInt(), rect.top.toInt()),
                    dstSize = IntSize(rect.width.toInt(), rect.height.toInt())
                )

                // Layer 2: selection rectangle (drawn on top of the image, same coord space)
                val s = selStart; val e = selEnd
                if (s != null && e != null) {
                    val left = minOf(s.x, e.x)
                    val top = minOf(s.y, e.y)
                    val width = maxOf(s.x, e.x) - left
                    val height = maxOf(s.y, e.y) - top
                    if (width > 4f && height > 4f) {
                        drawRect(
                            color = Color(0x330000FF),
                            topLeft = Offset(left, top),
                            size = Size(width, height)
                        )
                        drawRect(
                            color = Color.Blue,
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }

            if (bitmap == null || isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMd)
        ) {
            AppButton(
                text = "Validar área",
                onClick = {
                    val bmp = bitmap ?: return@AppButton
                    val rect = imageRect ?: return@AppButton
                    val s = selStart ?: return@AppButton
                    val e = selEnd ?: return@AppButton

                    val scaleX = bmp.width / rect.width
                    val scaleY = bmp.height / rect.height

                    val selLeft = minOf(s.x, e.x)
                    val selTop = minOf(s.y, e.y)
                    val selRight = maxOf(s.x, e.x)
                    val selBottom = maxOf(s.y, e.y)

                    val cropLeft = ((selLeft - rect.left) * scaleX).toInt().coerceIn(0, bmp.width - 1)
                    val cropTop = ((selTop - rect.top) * scaleY).toInt().coerceIn(0, bmp.height - 1)
                    val cropRight = ((selRight - rect.left) * scaleX).toInt().coerceIn(cropLeft + 1, bmp.width)
                    val cropBottom = ((selBottom - rect.top) * scaleY).toInt().coerceIn(cropTop + 1, bmp.height)

                    val cropped = Bitmap.createBitmap(
                        bmp,
                        cropLeft,
                        cropTop,
                        cropRight - cropLeft,
                        cropBottom - cropTop
                    )
                    onCropConfirm(cropped)
                },
                enabled = hasSelection && !isProcessing,
                modifier = Modifier.weight(1f)
            )
            AppOutlinedButton(
                text = "Cancelar",
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun rotateBitmapByExif(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    val orientation = try {
        context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
            ?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ?: ExifInterface.ORIENTATION_NORMAL
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun createTempImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply {
        if (!exists()) mkdirs()
    }
    val tempFile = File.createTempFile("temp_image", ".jpg", imagesDir).apply {
        deleteOnExit()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}
