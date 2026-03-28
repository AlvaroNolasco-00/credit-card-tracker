package com.alvaronolasco.creditcardtracker.ui.income

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaronolasco.creditcardtracker.data.entity.IncomeEntry
import com.alvaronolasco.creditcardtracker.data.entity.IncomeProfile
import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository
import com.alvaronolasco.creditcardtracker.util.DateUtils
import com.alvaronolasco.creditcardtracker.widget.CreditCardWidgetReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IncomeUiState(
    val profile: IncomeProfile? = null,
    val activeEntries: List<IncomeEntry> = emptyList(),
    val totalMonthlyIncome: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val repository: CreditCardRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncomeUiState())
    val uiState: StateFlow<IncomeUiState> = _uiState.asStateFlow()

    init {
        loadIncomeData()
    }

    private fun loadIncomeData() {
        val currentMonthYear = DateUtils.getCurrentMonthYear()
        
        combine(
            repository.getIncomeProfile(),
            repository.getIncomeEntriesForMonth(currentMonthYear),
            repository.getTotalIncomeForMonth(currentMonthYear)
        ) { profile, entries, total ->
            IncomeUiState(
                profile = profile,
                activeEntries = entries,
                totalMonthlyIncome = total ?: 0.0,
                isLoading = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun updateProfile(employmentType: String, incomeMode: String) {
        viewModelScope.launch {
            repository.saveIncomeProfile(
                IncomeProfile(
                    employmentType = employmentType,
                    incomeMode = incomeMode
                )
            )
        }
    }

    fun saveEntry(
        label: String,
        amount: Double,
        dayOfMonth: Int,
        isRecurring: Boolean,
        type: String,
        existingId: Int? = null
    ) {
        viewModelScope.launch {
            val currentMonthYear = if (isRecurring) null else DateUtils.getCurrentMonthYear()
            val entry = IncomeEntry(
                id = existingId ?: 0,
                label = label,
                amount = amount,
                dayOfMonth = dayOfMonth,
                isRecurring = isRecurring,
                type = type,
                monthYear = currentMonthYear
            )
            if (existingId == null) {
                repository.insertIncomeEntry(entry)
            } else {
                repository.updateIncomeEntry(entry)
            }
            CreditCardWidgetReceiver.updateAllWidgets(context)
        }
    }

    fun deleteEntry(entry: IncomeEntry) {
        viewModelScope.launch {
            repository.deleteIncomeEntry(entry)
            CreditCardWidgetReceiver.updateAllWidgets(context)
        }
    }
}
