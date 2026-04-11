package com.alvaronolasco.creditcardtracker.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaronolasco.creditcardtracker.data.entity.CreditCard
import com.alvaronolasco.creditcardtracker.data.entity.ExpenseWithCategories
import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository
import com.alvaronolasco.creditcardtracker.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class PeriodStats(
    val startDate: Long,
    val endDate: Long,
    val periodLabel: String,
    val totalExpenses: Double,
    val expensesCount: Int,
    val paymentsCount: Int,
    val expensesByDay: Map<Int, Double>, // Day of month to amount
    val expenseDetails: List<ExpenseWithCategories>
)

data class CardStatsUiState(
    val card: CreditCard? = null,
    val periods: List<PeriodStats> = emptyList(),
    val selectedPeriodIndex: Int = -1,
    val isLoading: Boolean = true
)

@HiltViewModel
class CardStatsViewModel @Inject constructor(
    private val repository: CreditCardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Int = checkNotNull(savedStateHandle["cardId"])
    private val _uiState = MutableStateFlow(CardStatsUiState())
    val uiState: StateFlow<CardStatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            if (card == null) return@launch
            
            // Get last 6 months of periods
            val ranges = DateUtils.getPeriodsRange(card.cutOffDay, 6)
            
            val periodStatsList = coroutineScope {
                ranges.map { (start, end) ->
                    async {
                        val expenses = repository.getExpensesWithCategoriesInPeriod(cardId, start, end).first()
                        val logs = repository.getLogsByEntityInPeriod(cardId, "CARD", start, end).first()
                        
                        val payments = logs.filter { it.action == "PAYMENT" }
                        val totalPayments = payments.size
                        
                        val total = expenses.sumOf { it.expense.amount }
                        val count = expenses.size
                        
                        val byDay = expenses.groupBy { 
                            Instant.ofEpochMilli(it.expense.date).atZone(ZoneId.systemDefault()).toLocalDate().dayOfMonth
                        }.mapValues { (_, group) -> group.sumOf { it.expense.amount } }

                        val startLocalDate = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate()
                        val monthName = startLocalDate.month.getDisplayName(TextStyle.SHORT, Locale("es"))
                        val label = "${startLocalDate.year % 100} $monthName"

                        PeriodStats(
                            startDate = start,
                            endDate = end,
                            periodLabel = label,
                            totalExpenses = total,
                            expensesCount = count,
                            paymentsCount = totalPayments,
                            expensesByDay = byDay,
                            expenseDetails = expenses
                        )
                    }
                }.awaitAll()
            }

            _uiState.update { 
                it.copy(
                    card = card,
                    periods = periodStatsList,
                    selectedPeriodIndex = if (periodStatsList.isNotEmpty()) periodStatsList.size - 1 else -1,
                    isLoading = false
                )
            }
        }
    }

    fun selectPeriod(index: Int) {
        _uiState.update { it.copy(selectedPeriodIndex = index) }
    }
}
