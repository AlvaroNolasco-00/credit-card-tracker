package com.alvaronolasco.creditcardtracker.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaronolasco.creditcardtracker.data.entity.Category
import com.alvaronolasco.creditcardtracker.data.entity.CreditCard
import com.alvaronolasco.creditcardtracker.data.entity.ExpenseWithCategories
import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ExpenseSearchUiState(
    val filteredExpenses: List<ExpenseWithCategories> = emptyList(),
    val categories: List<Category> = emptyList(),
    val cards: List<CreditCard> = emptyList(),
    val searchText: String = "",
    val selectedCategoryIds: Set<Int> = emptySet(),
    val startDate: Long = System.currentTimeMillis() - 15L * 24 * 60 * 60 * 1000,
    val endDate: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ExpenseSearchViewModel @Inject constructor(
    private val repository: CreditCardRepository
) : ViewModel() {

    private val _searchText = MutableStateFlow("")
    private val _selectedCategoryIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _startDate = MutableStateFlow(System.currentTimeMillis() - 15L * 24 * 60 * 60 * 1000)
    private val _endDate = MutableStateFlow(System.currentTimeMillis())
    private val _allExpenses = MutableStateFlow<List<ExpenseWithCategories>>(emptyList())

    private val _uiState = MutableStateFlow(ExpenseSearchUiState())
    val uiState: StateFlow<ExpenseSearchUiState> = _uiState.asStateFlow()

    init {
        // Reload raw expenses whenever date range changes
        combine(_startDate, _endDate) { start, end -> Pair(start, end) }
            .flatMapLatest { (start, end) ->
                repository.getAllExpensesWithCategoriesInPeriod(start, end)
            }
            .onEach { expenses ->
                _allExpenses.value = expenses
                _uiState.update { it.copy(isLoading = false) }
            }
            .launchIn(viewModelScope)

        // Apply filters reactively
        combine(_allExpenses, _searchText, _selectedCategoryIds) { expenses, text, categoryIds ->
            expenses.filter { ewc ->
                val matchesText = text.isBlank() ||
                    ewc.expense.description.contains(text, ignoreCase = true)
                val matchesCategory = categoryIds.isEmpty() ||
                    ewc.categories.any { it.id in categoryIds }
                matchesText && matchesCategory
            }
        }
            .onEach { filtered ->
                _uiState.update { it.copy(filteredExpenses = filtered) }
            }
            .launchIn(viewModelScope)

        // Sync filter state to uiState for UI to read
        combine(_searchText, _selectedCategoryIds, _startDate, _endDate) { text, cats, start, end ->
            _uiState.update { it.copy(searchText = text, selectedCategoryIds = cats, startDate = start, endDate = end) }
        }.launchIn(viewModelScope)

        repository.getAllCategories()
            .onEach { cats -> _uiState.update { it.copy(categories = cats) } }
            .launchIn(viewModelScope)

        repository.getAllCards()
            .onEach { cards -> _uiState.update { it.copy(cards = cards) } }
            .launchIn(viewModelScope)
    }

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    fun onCategoryToggle(categoryId: Int) {
        _selectedCategoryIds.update { current ->
            if (current.contains(categoryId)) current - categoryId else current + categoryId
        }
    }

    fun onDateRangeChange(start: Long, end: Long) {
        _startDate.value = start
        _endDate.value = end.coerceAtLeast(start)
        _uiState.update { it.copy(isLoading = true) }
    }

    fun cardNameForExpense(cardId: Int?): String =
        if (cardId == null) "Personal" else _uiState.value.cards.find { it.id == cardId }?.name ?: ""
}
