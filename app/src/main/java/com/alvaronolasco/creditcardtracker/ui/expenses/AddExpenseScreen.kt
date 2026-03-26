package com.alvaronolasco.creditcardtracker.ui.expenses

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.navigation.NavController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.alvaronolasco.creditcardtracker.data.entity.Category
import com.alvaronolasco.creditcardtracker.data.entity.CreditCard
import com.alvaronolasco.creditcardtracker.ui.components.*
import com.alvaronolasco.creditcardtracker.ui.theme.Dimensions
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    cardId: Int,
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

    val isEditMode = expenseId != null

    LaunchedEffect(cardId) {
        if (!isEditMode && cardId > 0) viewModel.loadCard(cardId)
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
            viewModel.loadCard(ewc.expense.cardId)
        }
    }

    val context = LocalContext.current

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
            amount = it.toString()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isEditMode) "Editar Gasto" else "Agregar Gasto",
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
            uiState.currentCard?.let { card ->
                CardTargetBanner(card)
            }

            AppTextField(
                value = amount,
                onValueChange = { amount = it },
                label = "Monto ($)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
            )

            AppTextField(
                value = description,
                onValueChange = { description = it },
                label = "Descripción"
            )

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
                            enabled = newCategoryName.isNotBlank()
                        ) { Text("Crear") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showAddCategoryDialog = false
                            newCategoryName = ""
                        }) { Text("Cancelar") }
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
                        TextButton(onClick = { categoryToDelete = null }) { Text("Cancelar") }
                    }
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
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary
                            )
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
                    modifier = Modifier.weight(1f)
                )
                AppOutlinedButton(
                    text = "Galería",
                    onClick = { galleryLauncher.launch("image/*") },
                    icon = Icons.Default.PhotoLibrary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(Dimensions.SpacingSm))

            AppButton(
                text = if (isEditMode) "Actualizar Gasto" else "Guardar Gasto",
                onClick = {
                    viewModel.saveExpense(
                        cardId = cardId,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        description = description,
                        categoryIds = selectedCategoryIds.toList(),
                        imagePath = capturedImageUri?.toString(),
                        date = uiState.currentExpense?.expense?.date ?: System.currentTimeMillis(),
                        expenseId = expenseId,
                        onSuccess = onBack
                    )
                },
                enabled = amount.isNotBlank() && description.isNotBlank()
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
private fun CardTargetBanner(card: CreditCard) {
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
