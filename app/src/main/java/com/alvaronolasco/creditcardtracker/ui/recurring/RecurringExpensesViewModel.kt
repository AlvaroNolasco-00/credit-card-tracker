package com.alvaronolasco.creditcardtracker.ui.recurring

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaronolasco.creditcardtracker.data.entity.Category
import com.alvaronolasco.creditcardtracker.data.entity.CreditCard
import com.alvaronolasco.creditcardtracker.data.entity.RecurringExpense
import com.alvaronolasco.creditcardtracker.data.entity.RecurringExpenseWithCategories
import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository
import com.alvaronolasco.creditcardtracker.widget.CreditCardWidgetReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringExpensesUiState(
    val cardId: Int = 0,
    val card: CreditCard? = null,
    val recurringExpenses: List<RecurringExpenseWithCategories> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class RecurringExpensesViewModel @Inject constructor(
    private val repository: CreditCardRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringExpensesUiState())
    val uiState: StateFlow<RecurringExpensesUiState> = _uiState.asStateFlow()

    fun loadData(cardId: Int) {
        _uiState.update { it.copy(cardId = cardId, isLoading = true) }
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            _uiState.update { it.copy(card = card) }
        }
        viewModelScope.launch {
            repository.getRecurringExpensesByCard(cardId).collect { expenses ->
                _uiState.update { it.copy(recurringExpenses = expenses, isLoading = false) }
            }
        }
        viewModelScope.launch {
            repository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun saveRecurringExpense(
        description: String,
        amount: Double,
        dayOfMonth: Int?,
        categoryIds: List<Int>,
        existingId: Int? = null
    ) {
        viewModelScope.launch {
            val cardId = _uiState.value.cardId
            val expense = RecurringExpense(
                id = existingId ?: 0,
                cardId = cardId,
                amount = amount,
                description = description,
                dayOfMonth = dayOfMonth
            )
            if (existingId == null) {
                repository.insertRecurringExpense(expense, categoryIds)
            } else {
                repository.updateRecurringExpense(expense, categoryIds)
            }
            CreditCardWidgetReceiver.updateAllWidgets(context)
        }
    }

    fun deleteRecurringExpense(expense: RecurringExpense) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(expense)
            CreditCardWidgetReceiver.updateAllWidgets(context)
        }
    }

    fun saveCategory(category: Category) {
        viewModelScope.launch {
            repository.insertCategory(category)
        }
    }
}
