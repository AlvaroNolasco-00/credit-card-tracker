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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val success: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signInWithEmail(state.email.trim(), state.password)
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
}
