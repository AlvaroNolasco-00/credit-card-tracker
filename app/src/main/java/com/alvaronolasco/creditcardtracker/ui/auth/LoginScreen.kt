package com.alvaronolasco.creditcardtracker.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.alvaronolasco.creditcardtracker.ui.theme.ForestGreen

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onForgotPassword: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
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
                title = "Iniciar sesión",
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

                uiState.error?.let { error ->
                    AuthErrorCard(error = error, onDismiss = viewModel::clearError)
                }

                AppTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Correo electrónico",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = uiState.error is AuthError.InvalidEmail || uiState.error is AuthError.UserNotFound,
                    enabled = !uiState.isLoading
                )

                PasswordTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Contraseña",
                    isError = uiState.error is AuthError.WrongPassword,
                    enabled = !uiState.isLoading
                )

                TextButton(
                    onClick = onForgotPassword,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        color = ForestGreen,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                AppButton(
                    text = "Iniciar sesión",
                    onClick = viewModel::submit,
                    enabled = uiState.email.isNotBlank() && uiState.password.isNotBlank() && !uiState.isLoading
                )

                AuthDivider()

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
private fun AuthDivider() {
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

@Composable
fun AuthErrorCard(error: AuthError, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = error.toSpanish(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("✕", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

fun AuthError.toSpanish(): String = when (this) {
    is AuthError.InvalidEmail -> "Correo electrónico inválido."
    is AuthError.WeakPassword -> "La contraseña debe tener al menos 8 caracteres."
    is AuthError.EmailInUse -> "Este correo ya está registrado. Intenta iniciar sesión."
    is AuthError.UserNotFound -> "No existe una cuenta con este correo."
    is AuthError.WrongPassword -> "Contraseña incorrecta."
    is AuthError.NetworkError -> "Sin conexión. Verifica tu internet e intenta de nuevo."
    is AuthError.TooManyRequests -> "Demasiados intentos. Espera un momento antes de intentar de nuevo."
    is AuthError.UserDisabled -> "Esta cuenta ha sido deshabilitada."
    is AuthError.EmailAlreadyLinked -> "Este correo ya está vinculado a otra cuenta."
    is AuthError.GoogleSignInCancelled -> "Inicio de sesión con Google cancelado."
    is AuthError.Unknown -> "Error inesperado: ${this.message ?: "desconocido"}"
}
