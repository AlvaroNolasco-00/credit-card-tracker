package com.alvaronolasco.creditcardtracker.ui.auth

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

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val sent: Boolean = false
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun submit() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.sendPasswordResetEmail(email)
                .onSuccess { _uiState.update { it.copy(isLoading = false, sent = true) } }
                .onFailure { e ->
                    val error = (e as? AuthException)?.error ?: AuthError.Unknown(e.message)
                    _uiState.update { it.copy(isLoading = false, error = error) }
                }
        }
    }
}
