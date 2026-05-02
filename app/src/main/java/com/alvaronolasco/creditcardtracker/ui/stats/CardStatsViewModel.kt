package com.alvaronolasco.creditcardtracker.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class CategorySpend(
    val categoryName: String,
    val amount: Double,
    val percentage: Float,
    val transactionCount: Int
)

data class PeriodStats(
    val startDate: Long,
    val endDate: Long,
    val periodLabel: String,
    val totalExpenses: Double,
    val expensesCount: Int,
    val paymentsCount: Int,
    val totalPaymentsAmount: Double,
    val expensesByDay: Map<LocalDate, Double>,
    val expenseDetails: List<ExpenseWithCategories>,
    val categoryBreakdown: List<CategorySpend>,
    val avgTransactionAmount: Double,
    val creditUtilizationPercent: Float
)

data class PeriodsSummary(
    val avgMonthlyExpense: Double,
    val maxMonthlyExpense: Double,
    val maxExpensePeriodLabel: String,
    val totalTransactions: Int,
    val totalPaymentsMade: Int,
    val overallCreditUtilization: Float
)

data class CardStatsUiState(
    val card: CreditCard? = null,
    val periods: List<PeriodStats> = emptyList(),
    val selectedPeriodIndex: Int = -1,
    val isLoading: Boolean = true,
    val monthCount: Int = 6,
    val summary: PeriodsSummary? = null,
    val insights: List<Insight> = emptyList()
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
            _uiState.update { it.copy(isLoading = true) }
            val card = repository.getCardById(cardId)
            if (card == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val monthCount = _uiState.value.monthCount
            val ranges = DateUtils.getPeriodsRange(card.cutOffDay, monthCount)

            val periodStatsList = coroutineScope {
                ranges.map { (start, end) ->
                    async {
                        val expenses = repository.getExpensesWithCategoriesInPeriod(cardId, start, end).first()
                        val logs = repository.getLogsByEntityInPeriod(cardId, "CARD", start, end).first()

                        val payments = logs.filter { it.action == "PAYMENT" }
                        val totalPaymentsCount = payments.size
                        val totalPaymentsAmount = payments.sumOf { it.amount ?: 0.0 }

                        val total = expenses.sumOf { it.expense.amount }
                        val count = expenses.size

                        val byDay = expenses.groupBy {
                            Instant.ofEpochMilli(it.expense.date).atZone(ZoneOffset.UTC).toLocalDate()
                        }.mapValues { (_, group) -> group.sumOf { it.expense.amount } }

                        val categoryBreakdown = expenses
                            .flatMap { ewc -> ewc.categories.map { cat -> cat.name to ewc.expense.amount } }
                            .groupBy { it.first }
                            .mapValues { (_, list) -> list.sumOf { it.second } }
                            .toList()
                            .sortedByDescending { it.second }
                            .take(5)
                            .map { (name, amount) ->
                                CategorySpend(
                                    categoryName = name,
                                    amount = amount,
                                    percentage = if (total > 0) (amount / total).toFloat() else 0f,
                                    transactionCount = expenses.count { it.categories.any { c -> c.name == name } }
                                )
                            }

                        val avgTx = if (count > 0) total / count else 0.0
                        val utilization = if (card.creditLimit > 0) (total / card.creditLimit).toFloat() else 0f

                        val startLocalDate = Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate()
                        val monthName = startLocalDate.month.getDisplayName(TextStyle.SHORT, Locale("es"))
                        val label = "${startLocalDate.year % 100} $monthName"

                        PeriodStats(
                            startDate = start,
                            endDate = end,
                            periodLabel = label,
                            totalExpenses = total,
                            expensesCount = count,
                            paymentsCount = totalPaymentsCount,
                            totalPaymentsAmount = totalPaymentsAmount,
                            expensesByDay = byDay,
                            expenseDetails = expenses,
                            categoryBreakdown = categoryBreakdown,
                            avgTransactionAmount = avgTx,
                            creditUtilizationPercent = utilization.coerceIn(0f, 1f)
                        )
                    }
                }.awaitAll()
            }

            val summary = if (periodStatsList.isNotEmpty()) {
                val avg = periodStatsList.sumOf { it.totalExpenses } / periodStatsList.size
                val maxPeriod = periodStatsList.maxByOrNull { it.totalExpenses }
                PeriodsSummary(
                    avgMonthlyExpense = avg,
                    maxMonthlyExpense = maxPeriod?.totalExpenses ?: 0.0,
                    maxExpensePeriodLabel = maxPeriod?.periodLabel ?: "",
                    totalTransactions = periodStatsList.sumOf { it.expensesCount },
                    totalPaymentsMade = periodStatsList.sumOf { it.paymentsCount },
                    overallCreditUtilization = periodStatsList.map { it.creditUtilizationPercent }.average().toFloat()
                )
            } else null

            val selectedIndex = if (periodStatsList.isNotEmpty()) periodStatsList.size - 1 else -1
            val insights = CardStatsInsights.generateInsights(
                periods = periodStatsList,
                selectedIndex = selectedIndex,
                cardName = card.name
            )

            _uiState.update {
                it.copy(
                    card = card,
                    periods = periodStatsList,
                    selectedPeriodIndex = selectedIndex,
                    summary = summary,
                    insights = insights,
                    isLoading = false
                )
            }
        }
    }

    fun selectPeriod(index: Int) {
        val currentState = _uiState.value
        if (index == currentState.selectedPeriodIndex) return
        
        val newInsights = CardStatsInsights.generateInsights(
            periods = currentState.periods,
            selectedIndex = index,
            cardName = currentState.card?.name ?: ""
        )
        _uiState.update { it.copy(selectedPeriodIndex = index, insights = newInsights) }
    }

    fun selectMonthCount(count: Int) {
        if (count != _uiState.value.monthCount) {
            _uiState.update { it.copy(monthCount = count, selectedPeriodIndex = -1) }
            loadStats()
        }
    }
}
