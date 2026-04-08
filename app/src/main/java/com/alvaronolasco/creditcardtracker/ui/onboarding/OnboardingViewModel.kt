package com.alvaronolasco.creditcardtracker.ui.onboarding

import androidx.lifecycle.ViewModel
import com.alvaronolasco.creditcardtracker.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class OnboardingUiState(
    val currentPage: Int = 0,
    val userName: String = ""
) {
    val totalPages: Int = onboardingPages.size
    val isLastPage: Boolean = currentPage == totalPages - 1
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(userName = name) }
    }

    fun nextPage() {
        _uiState.update { state ->
            if (state.currentPage < state.totalPages - 1)
                state.copy(currentPage = state.currentPage + 1)
            else state
        }
    }

    fun goToPage(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun completeOnboarding() {
        val name = _uiState.value.userName.trim()
        if (name.isNotEmpty()) {
            userPreferencesRepository.saveName(name)
        }
        userPreferencesRepository.setOnboardingCompleted()
    }
}
