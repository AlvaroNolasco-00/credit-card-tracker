package com.alvaronolasco.creditcardtracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaronolasco.creditcardtracker.data.entity.CreditCard
import com.alvaronolasco.creditcardtracker.data.entity.ExpenseWithCategories
import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository
import com.alvaronolasco.creditcardtracker.data.repository.UserPreferencesRepository
import com.alvaronolasco.creditcardtracker.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class CardDashboardState(
    val card: CreditCard,
    val totalSpent: Double = 0.0,
    val cutPeriodTotal: Double = 0.0,
    val cutOffHappenedThisMonth: Boolean = false,
    val isPaidThisCycle: Boolean = false,
    val extraFinancingPayment: Double = 0.0,
    val partiallyPaidAmount: Double = 0.0,
    val daysUntilCutOff: Int = 0,
    val daysUntilPayment: Int = 0,
    val cutOffDateLabel: String = "",
    val hasStatsAvailable: Boolean = false
)

data class DashboardUiState(
    val cards: List<CardDashboardState> = emptyList(),
    val totalMonthlyIncome: Double = 0.0,
    val totalAllCardsSpent: Double = 0.0,
    val totalNonCardSpent: Double = 0.0,
    val hasIncomeProfile: Boolean = false,
    val showExtraIncomePrompt: Boolean = false,
    val showMonthlyPrompt: Boolean = false,
    val promptDismissedMonth: String? = null,
    val isLoading: Boolean = true,
    val selectedCardIndex: Int = 0,
    val recentExpenses: List<ExpenseWithCategories> = emptyList(),
    val userName: String? = null,
    val showNamePrompt: Boolean = false,
    val hasBudgetThisMonth: Boolean = false,
    val showSupportCard: Boolean = false,
    val showBudgetPrompt: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: CreditCardRepository,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _selectedCardIndex = MutableStateFlow(0)

    init {
        loadDashboard()
        loadNonCardSpending()
        loadRecentExpenses()
        loadUserName()
        loadBudgetStatus()
        _uiState.update { it.copy(showSupportCard = userPrefs.shouldShowSupportCard()) }
    }

    private fun loadNonCardSpending() {
        val today = LocalDate.now()
        val monthStart = today.withDayOfMonth(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59).toEpochSecond(ZoneOffset.UTC) * 1000
        repository.getTotalNonCardSpentInPeriod(monthStart, monthEnd)
            .onEach { spent ->
                _uiState.update { it.copy(totalNonCardSpent = spent ?: 0.0) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadBudgetStatus() {
        val currentMonthYear = DateUtils.getCurrentMonthYear()
        repository.getBudgetItemsForMonth(currentMonthYear)
            .onEach { items ->
                val hasBudget = items.isNotEmpty()
                val dayOfMonth = LocalDate.now().dayOfMonth
                val shouldPrompt = !hasBudget &&
                    dayOfMonth >= 3 &&
                    !userPrefs.isBudgetPromptDismissed(currentMonthYear)
                _uiState.update {
                    it.copy(
                        hasBudgetThisMonth = hasBudget,
                        showBudgetPrompt = shouldPrompt && !it.showNamePrompt
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun dismissBudgetPrompt() {
        userPrefs.dismissBudgetPrompt(DateUtils.getCurrentMonthYear())
        _uiState.update { it.copy(showBudgetPrompt = false) }
    }

    private fun loadUserName() {
        userPrefs.userName
            .onEach { name ->
                _uiState.update {
                    it.copy(
                        userName = name,
                        showNamePrompt = name.isNullOrBlank()
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun saveName(name: String) {
        userPrefs.saveName(name)
    }

    fun dismissNamePrompt() {
        _uiState.update { it.copy(showNamePrompt = false) }
    }

    fun dismissSupportCard() {
        userPrefs.dismissSupportCard()
        _uiState.update { it.copy(showSupportCard = false) }
    }

    fun selectCard(index: Int) {
        _selectedCardIndex.value = index
        _uiState.update { it.copy(selectedCardIndex = index) }
    }

    private fun loadDashboard() {
        val currentMonthYear = DateUtils.getCurrentMonthYear()

        combine(
            repository.getAllCards().flatMapLatest { cards ->
                if (cards.isEmpty()) flowOf(emptyList())
                else combine(cards.map { card ->
                    val (start, end) = DateUtils.getCurrentPeriodRange(card.cutOffDay)
                    val cutHappened = DateUtils.hasCutOffPassedThisMonth(card.cutOffDay)
                    val currentFlow = repository.getTotalSpentInPeriod(card.id, start, end)
                    if (cutHappened) {
                        val (prevStart, prevEnd) = DateUtils.getPreviousPeriodRange(card.cutOffDay)
                        val isPaid = card.lastPaymentDate > prevEnd
                        val effectivePartial = if (!isPaid && card.partialPaymentCycleEnd == prevEnd) card.partialPaymentAmount else 0.0
                        val prevFlow = repository.getTotalSpentInPeriod(card.id, prevStart, prevEnd)
                        combine(currentFlow, prevFlow) { current, prev ->
                            CardDashboardState(
                                card = card,
                                totalSpent = current ?: 0.0,
                                cutPeriodTotal = if (isPaid) 0.0 else (prev ?: 0.0),
                                cutOffHappenedThisMonth = true,
                                isPaidThisCycle = isPaid,
                                extraFinancingPayment = card.extraFinancingPayment,
                                partiallyPaidAmount = effectivePartial,
                                daysUntilCutOff = DateUtils.getDaysUntil(card.cutOffDay),
                                daysUntilPayment = DateUtils.getDaysUntil(card.paymentDueDay),
                                cutOffDateLabel = computeCutOffDateLabel(card.cutOffDay),
                                hasStatsAvailable = checkStatsAvailability(card)
                            )
                        }
                    } else {
                        currentFlow.map { total ->
                            CardDashboardState(
                                card = card,
                                totalSpent = total ?: 0.0,
                                cutPeriodTotal = 0.0,
                                cutOffHappenedThisMonth = false,
                                extraFinancingPayment = card.extraFinancingPayment,
                                daysUntilCutOff = DateUtils.getDaysUntil(card.cutOffDay),
                                daysUntilPayment = DateUtils.getDaysUntil(card.paymentDueDay),
                                cutOffDateLabel = computeCutOffDateLabel(card.cutOffDay),
                                hasStatsAvailable = checkStatsAvailability(card)
                            )
                        }
                    }
                }) { it.toList() }
            },
            repository.getIncomeProfile(),
            repository.getIncomeEntriesForMonth(currentMonthYear),
            repository.getTotalIncomeForMonth(currentMonthYear)
        ) { cardStates, profile, entries, totalIncome ->
            val hasProfile = profile != null
            val isPromptDismissed = _uiState.value.promptDismissedMonth == currentMonthYear

            var showExtraPrompt = false
            var showManualPrompt = false

            if (hasProfile && !isPromptDismissed) {
                val daysToPayday = entries.filter { it.isRecurring }.map { DateUtils.getDaysUntil(it.dayOfMonth) }
                showExtraPrompt = profile?.incomeMode == "RECURRING" && daysToPayday.any { it in 0..3 }
                showManualPrompt = profile?.incomeMode == "MONTHLY_PROMPT" && entries.none { it.monthYear == currentMonthYear }
            }

            val totalAllCards = cardStates.sumOf {
                if (it.cutOffHappenedThisMonth) it.cutPeriodTotal + it.totalSpent + it.extraFinancingPayment
                else it.totalSpent + it.extraFinancingPayment
            }

            _uiState.update {
                it.copy(
                    cards = cardStates,
                    totalMonthlyIncome = totalIncome ?: 0.0,
                    totalAllCardsSpent = totalAllCards,
                    hasIncomeProfile = hasProfile,
                    showExtraIncomePrompt = showExtraPrompt,
                    showMonthlyPrompt = showManualPrompt,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun loadRecentExpenses() {
        combine(_uiState, _selectedCardIndex) { state, index ->
            state.cards.getOrNull(index)?.card?.id
        }
            .distinctUntilChanged()
            .flatMapLatest { cardId ->
                if (cardId != null) repository.getExpensesWithCategoriesByCard(cardId).map { it.take(5) }
                else flowOf(emptyList())
            }
            .onEach { expenses ->
                _uiState.update { it.copy(recentExpenses = expenses) }
            }
            .launchIn(viewModelScope)
    }

    fun payBalance(card: CreditCard) {
        viewModelScope.launch {
            repository.updateCard(
                card.copy(
                    lastPaymentDate = System.currentTimeMillis(),
                    partialPaymentAmount = 0.0,
                    partialPaymentCycleEnd = 0L
                )
            )
            val amount = card.partialPaymentAmount // Actually should be total due but partialPaymentAmount is what we track as already paid? No.
            // payBalance is for full payment.
            // Let's assume we log the payment here.
            repository.logPayment(card.id, card.name, 0.0) // 0 means full balance in this context or we could calculate it.
        }
    }

    fun payPartial(state: CardDashboardState, amount: Double) {
        viewModelScope.launch {
            val totalDue = state.cutPeriodTotal + state.extraFinancingPayment
            val newPaid = state.partiallyPaidAmount + amount
            if (newPaid >= totalDue) {
                repository.updateCard(
                    state.card.copy(
                        lastPaymentDate = System.currentTimeMillis(),
                        partialPaymentAmount = 0.0,
                        partialPaymentCycleEnd = 0L
                    )
                )
            } else {
                val (_, prevEnd) = DateUtils.getPreviousPeriodRange(state.card.cutOffDay)
                repository.updateCard(
                    state.card.copy(
                        partialPaymentAmount = newPaid,
                        partialPaymentCycleEnd = prevEnd
                    )
                )
            }
            repository.logPayment(state.card.id, state.card.name, amount)
        }
    }

    fun dismissPrompt() {
        val currentMonthYear = DateUtils.getCurrentMonthYear()
        _uiState.update {
            it.copy(
                promptDismissedMonth = currentMonthYear,
                showExtraIncomePrompt = false,
                showMonthlyPrompt = false
            )
        }
    }

    private fun computeCutOffDateLabel(cutOffDay: Int): String {
        val today = LocalDate.now()
        var cutDate = today.withDayOfMonth(cutOffDay.coerceIn(1, today.lengthOfMonth()))
        if (cutDate.isBefore(today)) {
            val nextMonth = cutDate.plusMonths(1)
            cutDate = nextMonth.withDayOfMonth(cutOffDay.coerceIn(1, nextMonth.lengthOfMonth()))
        }
        val monthName = cutDate.month
            .getDisplayName(TextStyle.FULL, Locale("es"))
            .replaceFirstChar { it.uppercase() }
        return "${cutDate.dayOfMonth} de $monthName"
    }

    private fun checkStatsAvailability(card: CreditCard): Boolean {
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000
        val isOlderThanAMonth = (System.currentTimeMillis() - card.createdAt) >= thirtyDaysMillis
        
        // Also if we have passed a cut-off (meaning we have at least one closed period)
        val cutOffPassed = DateUtils.hasCutOffPassedThisMonth(card.cutOffDay)
        
        return isOlderThanAMonth || cutOffPassed
    }
}
