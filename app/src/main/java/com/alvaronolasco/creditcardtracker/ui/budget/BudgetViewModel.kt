package com.alvaronolasco.creditcardtracker.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaronolasco.creditcardtracker.data.entity.BudgetItem
import com.alvaronolasco.creditcardtracker.data.entity.Category
import com.alvaronolasco.creditcardtracker.data.entity.CategorySpending
import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository
import com.alvaronolasco.creditcardtracker.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import javax.inject.Inject

data class BudgetCategoryState(
    val category: Category,
    val budgetItem: BudgetItem?,
    val actualSpending: Double
)

data class BudgetUiState(
    val monthYear: String = DateUtils.getCurrentMonthYear(),
    val categories: List<BudgetCategoryState> = emptyList(),
    val totalBudgeted: Double = 0.0,
    val totalSpent: Double = 0.0,
    val hasPreviousMonthBudget: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repository: CreditCardRepository
) : ViewModel() {

    private val _monthYear = MutableStateFlow(DateUtils.getCurrentMonthYear())
    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        _monthYear
            .flatMapLatest { monthYear ->
                val (startDate, endDate) = monthRangeMillis(monthYear)
                val prevMonth = previousMonth(monthYear)
                combine(
                    repository.getAllCategories(),
                    repository.getBudgetItemsForMonth(monthYear),
                    repository.getSpendingPerCategory(startDate, endDate),
                    repository.getBudgetItemsForMonth(prevMonth)
                ) { allCategories, budgetItems, spending, prevBudgetItems ->
                    val spendingMap = spending.associateBy { it.id }
                    val budgetMap = budgetItems.associateBy { it.categoryId }

                    val categoryStates = allCategories.map { category ->
                        BudgetCategoryState(
                            category = category,
                            budgetItem = budgetMap[category.id],
                            actualSpending = spendingMap[category.id]?.totalSpent ?: 0.0
                        )
                    }

                    val budgeted = budgetItems.sumOf { it.limitAmount }
                    val spent = categoryStates
                        .filter { it.budgetItem != null }
                        .sumOf { it.actualSpending }

                    BudgetUiState(
                        monthYear = monthYear,
                        categories = categoryStates,
                        totalBudgeted = budgeted,
                        totalSpent = spent,
                        hasPreviousMonthBudget = prevBudgetItems.isNotEmpty(),
                        isLoading = false
                    )
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun setMonth(monthYear: String) {
        _monthYear.value = monthYear
    }

    fun navigateMonth(forward: Boolean) {
        val current = YearMonth.parse(_monthYear.value)
        val next = if (forward) current.plusMonths(1) else current.minusMonths(1)
        _monthYear.value = String.format("%d-%02d", next.year, next.monthValue)
    }

    fun saveBudget(category: Category, amount: Double) {
        viewModelScope.launch {
            val existing = repository.getBudgetItemForCategory(category.id, _monthYear.value)
            repository.upsertBudgetItem(
                BudgetItem(
                    id = existing?.id ?: 0,
                    categoryId = category.id,
                    monthYear = _monthYear.value,
                    limitAmount = amount
                )
            )
        }
    }

    fun deleteBudget(budgetItem: BudgetItem) {
        viewModelScope.launch {
            repository.deleteBudgetItem(budgetItem)
        }
    }

    fun copyFromPreviousMonth() {
        viewModelScope.launch {
            val target = _monthYear.value
            val source = previousMonth(target)
            repository.copyBudgetFromMonth(source, target)
        }
    }

    private fun monthRangeMillis(monthYear: String): Pair<Long, Long> {
        val ym = YearMonth.parse(monthYear)
        val start = ym.atDay(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        val end = ym.atEndOfMonth().atTime(23, 59, 59).toEpochSecond(ZoneOffset.UTC) * 1000
        return start to end
    }

    private fun previousMonth(monthYear: String): String {
        val ym = YearMonth.parse(monthYear).minusMonths(1)
        return String.format("%d-%02d", ym.year, ym.monthValue)
    }
}
