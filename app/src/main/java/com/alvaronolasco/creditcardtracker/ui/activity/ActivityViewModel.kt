package com.alvaronolasco.creditcardtracker.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaronolasco.creditcardtracker.data.entity.ActivityLog
import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ActivityUiState(
    val logs: List<ActivityLog> = emptyList(),
    val selectedCategory: String? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val repository: CreditCardRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ActivityUiState> = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) {
                repository.getAllActivityLogs()
            } else {
                repository.getActivityLogsByCategory(category)
            }
        }
        .map { logs ->
            ActivityUiState(logs = logs, selectedCategory = _selectedCategory.value, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActivityUiState()
        )

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }
}
