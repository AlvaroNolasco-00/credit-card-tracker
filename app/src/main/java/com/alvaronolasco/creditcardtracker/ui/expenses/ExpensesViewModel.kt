package com.alvaronolasco.creditcardtracker.ui.expenses

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaronolasco.creditcardtracker.data.entity.Category
import com.alvaronolasco.creditcardtracker.data.entity.CreditCard
import com.alvaronolasco.creditcardtracker.data.entity.Expense
import com.alvaronolasco.creditcardtracker.data.entity.ExpenseWithCategories
import java.util.Calendar
import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository
import com.alvaronolasco.creditcardtracker.ocr.OcrProcessor
import com.alvaronolasco.creditcardtracker.widget.CreditCardWidgetReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpensesUiState(
    val expenses: List<ExpenseWithCategories> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val ocrResultAmount: Double? = null,
    val ocrProcessing: Boolean = false,
    val currentExpense: ExpenseWithCategories? = null,
    val currentCard: CreditCard? = null,
    val allCards: List<CreditCard> = emptyList()
)

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: CreditCardRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState = _uiState.asStateFlow()

    private val ocrProcessor = OcrProcessor(context)

    init {
        loadCategories()
        loadAllCards()
    }

    private fun loadAllCards() {
        repository.getAllCards()
            .onEach { cards ->
                _uiState.update { it.copy(allCards = cards) }
            }.launchIn(viewModelScope)
    }

    private fun loadCategories() {
        repository.getAllCategories()
            .onEach { categories ->
                _uiState.update { it.copy(categories = categories) }
            }.launchIn(viewModelScope)
    }

    fun loadExpenses(cardId: Int) {
        repository.getExpensesWithCategoriesByCard(cardId)
            .onEach { expenses ->
                _uiState.update { it.copy(expenses = expenses) }
            }.launchIn(viewModelScope)
    }

    fun loadCard(cardId: Int) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            _uiState.update { it.copy(currentCard = card) }
        }
    }

    fun loadExpense(expenseId: Int) {
        viewModelScope.launch {
            val expenseWithCategories = repository.getExpenseWithCategoriesById(expenseId)
            _uiState.update { it.copy(currentExpense = expenseWithCategories) }
        }
    }

    fun processOcr(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(ocrProcessing = true) }
            val result = ocrProcessor.processImage(uri)
            _uiState.update { it.copy(ocrResultAmount = result.detectedAmount, ocrProcessing = false) }
        }
    }

    fun saveExpense(
        cardId: Int,
        amount: Double,
        description: String,
        categoryIds: List<Int>,
        imagePath: String?,
        date: Long,
        expenseId: Int? = null,
        msiMonths: Int = 1,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val monthlyAmount = if (msiMonths > 1 && amount > 0) amount / msiMonths else 0.0
            val msiEndDate = if (msiMonths > 1) {
                Calendar.getInstance().apply {
                    timeInMillis = date
                    add(Calendar.MONTH, msiMonths)
                }.timeInMillis
            } else 0L
            val expense = Expense(
                id = expenseId ?: 0,
                cardId = cardId,
                amount = amount,
                description = description,
                receiptImagePath = imagePath,
                date = date,
                msiMonths = msiMonths,
                msiMonthlyAmount = monthlyAmount,
                msiEndDate = msiEndDate
            )
            val savedId = if (expenseId == null) {
                repository.insertExpense(expense).toInt()
            } else {
                repository.updateExpense(expense)
                expenseId
            }
            repository.setExpenseCategories(savedId, categoryIds)
            CreditCardWidgetReceiver.updateAllWidgets(context)  // Espera a que complete
            onSuccess()
        }
    }

    fun createCategory(name: String, onCreated: (Int) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertCategory(Category(name = name, icon = "custom")).toInt()
            onCreated(id)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun deleteExpense(expense: Expense, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            CreditCardWidgetReceiver.updateAllWidgets(context)  // Espera a que complete
            onSuccess()
        }
    }
}
