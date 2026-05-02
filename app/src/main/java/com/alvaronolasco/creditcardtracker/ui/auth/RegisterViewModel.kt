package com.alvaronolasco.creditcardtracker.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaronolasco.creditcardtracker.data.repository.AuthError
import com.alvaronolasco.creditcardtracker.data.repository.AuthException
import com.alvaronolasco.creditcardtracker.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isAnonymousSession: Boolean = false,
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val success: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    init {
        _uiState.update { it.copy(isAnonymousSession = authRepository.isAnonymous()) }
    }

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun onConfirmPasswordChange(value: String) = _uiState.update { it.copy(confirmPassword = value, error = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun submit() {
        val state = _uiState.value
        if (!validateLocally(state)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signUpWithEmail(state.email.trim(), state.password)
                .onSuccess { _uiState.update { it.copy(isLoading = false, success = true) } }
                .onFailure { e ->
                    val error = (e as? AuthException)?.error ?: AuthError.Unknown(e.message)
                    _uiState.update { it.copy(isLoading = false, error = error) }
                }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signInWithGoogle(context)
                .onSuccess { _uiState.update { it.copy(isLoading = false, success = true) } }
                .onFailure { e ->
                    val error = (e as? AuthException)?.error ?: AuthError.Unknown(e.message)
                    if (error == AuthError.GoogleSignInCancelled) {
                        _uiState.update { it.copy(isLoading = false) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = error) }
                    }
                }
        }
    }

    private fun validateLocally(state: RegisterUiState): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
        return when {
            !emailRegex.matches(state.email.trim()) -> {
                _uiState.update { it.copy(error = AuthError.InvalidEmail) }
                false
            }
            state.password.length < 8 -> {
                _uiState.update { it.copy(error = AuthError.WeakPassword) }
                false
            }
            state.password != state.confirmPassword -> {
                _uiState.update { it.copy(error = AuthError.Unknown("Las contraseñas no coinciden.")) }
                false
            }
            else -> true
        }
    }
}
