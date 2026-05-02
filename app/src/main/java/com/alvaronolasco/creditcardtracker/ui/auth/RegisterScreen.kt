package com.alvaronolasco.creditcardtracker.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaronolasco.creditcardtracker.data.repository.AuthError
import com.alvaronolasco.creditcardtracker.ui.components.*
import com.alvaronolasco.creditcardtracker.ui.theme.MintGreen
import com.alvaronolasco.creditcardtracker.ui.theme.ForestGreen

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.success) {
        if (uiState.success) onSuccess()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Crear cuenta",
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                if (uiState.isAnonymousSession) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MintGreen.copy(alpha = 0.3f)),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = ForestGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Tus datos actuales se vincularán a tu nueva cuenta automáticamente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ForestGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                uiState.error?.let { error ->
                    AuthErrorCard(error = error, onDismiss = viewModel::clearError)
                }

                AppTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Correo electrónico",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = uiState.error is AuthError.InvalidEmail || uiState.error is AuthError.EmailInUse,
                    enabled = !uiState.isLoading
                )

                PasswordTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Contraseña (mín. 8 caracteres)",
                    isError = uiState.error is AuthError.WeakPassword,
                    enabled = !uiState.isLoading
                )

                val confirmError = uiState.error
                PasswordTextField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = "Confirmar contraseña",
                    isError = confirmError is AuthError.Unknown && confirmError.message?.contains("coinciden") == true,
                    enabled = !uiState.isLoading
                )

                AppButton(
                    text = "Crear cuenta",
                    onClick = viewModel::submit,
                    enabled = uiState.email.isNotBlank() && uiState.password.isNotBlank()
                        && uiState.confirmPassword.isNotBlank() && !uiState.isLoading
                )

                RegisterDivider()

                AppOutlinedButton(
                    text = "Continuar con Google",
                    onClick = { viewModel.signInWithGoogle(context) },
                    enabled = !uiState.isLoading
                )

                Spacer(Modifier.height(24.dp))
            }

            if (uiState.isLoading) {
                AppLoadingIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun RegisterDivider() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Text(
            text = "  o  ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }
}
