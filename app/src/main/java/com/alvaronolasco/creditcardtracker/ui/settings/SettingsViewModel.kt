package com.alvaronolasco.creditcardtracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaronolasco.creditcardtracker.data.repository.AuthRepository
import com.alvaronolasco.creditcardtracker.data.repository.AuthState
import com.alvaronolasco.creditcardtracker.data.repository.UserPreferencesRepository
import com.alvaronolasco.creditcardtracker.notifications.InactivityReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val inactivityScheduler: InactivityReminderScheduler,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _inactivityEnabled = MutableStateFlow(userPreferencesRepository.isInactivityNotificationsEnabled())
    val inactivityEnabled: StateFlow<Boolean> = _inactivityEnabled

    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    fun setInactivityEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setInactivityNotificationsEnabled(enabled)
            _inactivityEnabled.value = enabled
            inactivityScheduler.schedule()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
