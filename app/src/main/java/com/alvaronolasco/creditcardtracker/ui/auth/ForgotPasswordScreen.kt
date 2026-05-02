package com.alvaronolasco.creditcardtracker.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaronolasco.creditcardtracker.ui.components.AppButton
import com.alvaronolasco.creditcardtracker.ui.components.AppLoadingIndicator
import com.alvaronolasco.creditcardtracker.ui.components.AppTextField
import com.alvaronolasco.creditcardtracker.ui.components.AppTopBar

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.sent) {
        if (uiState.sent) {
            snackbarHostState.showSnackbar("Enlace enviado. Revisa tu correo.")
            onBack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = "Recuperar contraseña",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Ingresa tu correo y te enviaremos un enlace para restablecer tu contraseña.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )

                uiState.error?.let { error ->
                    AuthErrorCard(error = error, onDismiss = viewModel::clearError)
                }

                AppTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Correo electrónico",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !uiState.isLoading
                )

                AppButton(
                    text = "Enviar enlace",
                    onClick = viewModel::submit,
                    enabled = uiState.email.isNotBlank() && !uiState.isLoading
                )
            }

            if (uiState.isLoading) {
                AppLoadingIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
