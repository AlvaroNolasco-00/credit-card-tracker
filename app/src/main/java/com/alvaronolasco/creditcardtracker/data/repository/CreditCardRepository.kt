package com.alvaronolasco.creditcardtracker.data.repository

import com.alvaronolasco.creditcardtracker.data.dao.*
import com.alvaronolasco.creditcardtracker.data.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CreditCardRepository @Inject constructor(
    private val cardDao: CreditCardDao,
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
    private val configDao: NotificationConfigDao,
    private val incomeDao: IncomeDao,
    private val budgetDao: BudgetDao
) {
    // Cards
    fun getAllCards(): Flow<List<CreditCard>> = cardDao.getAllCards()
    suspend fun getCardById(id: Int): CreditCard? = cardDao.getCardById(id)
    suspend fun insertCard(card: CreditCard): Int {
        val cardId = cardDao.insertCard(card).toInt()
        val defaultConfig = listOf(0, 1, 3, 5).flatMap { days ->
            listOf(
                NotificationConfig(cardId = cardId, type = "CUT_OFF", daysBefore = days),
                NotificationConfig(cardId = cardId, type = "PAYMENT", daysBefore = days)
            )
        }
        configDao.insertConfigs(defaultConfig)
        return cardId
    }
    suspend fun updateCard(card: CreditCard) = cardDao.updateCard(card)
    suspend fun deleteCard(card: CreditCard) = cardDao.deleteCard(card)

    // Categories
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)
    suspend fun deleteCategory(category: Category) {
        if (!category.isDefault) {
            categoryDao.deleteCategory(category)
        }
    }

    // Expenses
    fun getExpensesWithCategoriesByCard(cardId: Int): Flow<List<ExpenseWithCategories>> =
        expenseDao.getExpensesWithCategoriesByCard(cardId)
    fun getExpensesByCardInPeriod(cardId: Int, start: Long, end: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByCardInPeriod(cardId, start, end)
    fun getTotalSpentInPeriod(cardId: Int, start: Long, end: Long): Flow<Double?> =
        expenseDao.getTotalSpentInPeriod(cardId, start, end)
    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)
    suspend fun getExpenseWithCategoriesById(id: Int): ExpenseWithCategories? =
        expenseDao.getExpenseWithCategoriesById(id)
    fun getAllExpensesWithCategoriesInPeriod(start: Long, end: Long): Flow<List<ExpenseWithCategories>> =
        expenseDao.getAllExpensesWithCategoriesInPeriod(start, end)
    suspend fun setExpenseCategories(expenseId: Int, categoryIds: List<Int>) =
        expenseCategoryDao.replaceExpenseCategories(expenseId, categoryIds)

    // Configs
    fun getConfigsByCard(cardId: Int): Flow<List<NotificationConfig>> = configDao.getConfigsByCard(cardId)
    suspend fun updateConfig(config: NotificationConfig) = configDao.updateConfig(config)

    // Income
    fun getIncomeProfile(): Flow<IncomeProfile?> = incomeDao.getProfile()
    suspend fun saveIncomeProfile(profile: IncomeProfile) = incomeDao.upsertProfile(profile)
    fun getAllActiveIncomeEntries(): Flow<List<IncomeEntry>> = incomeDao.getAllActiveEntries()
    fun getRecurringIncomeEntries(): Flow<List<IncomeEntry>> = incomeDao.getRecurringEntries()
    fun getIncomeEntriesForMonth(monthYear: String): Flow<List<IncomeEntry>> = incomeDao.getEntriesForMonth(monthYear)
    fun getTotalIncomeForMonth(monthYear: String): Flow<Double?> = incomeDao.getTotalIncomeForMonth(monthYear)
    suspend fun insertIncomeEntry(entry: IncomeEntry) = incomeDao.insertEntry(entry)
    suspend fun updateIncomeEntry(entry: IncomeEntry) = incomeDao.updateEntry(entry)
    suspend fun deleteIncomeEntry(entry: IncomeEntry) = incomeDao.deleteEntry(entry)

    // Budget
    fun getBudgetItemsForMonth(monthYear: String): Flow<List<BudgetItem>> = budgetDao.getBudgetItemsForMonth(monthYear)
    suspend fun getBudgetItemForCategory(categoryId: Int, monthYear: String): BudgetItem? = budgetDao.getBudgetItemForCategory(categoryId, monthYear)
    suspend fun upsertBudgetItem(item: BudgetItem) = budgetDao.upsertBudgetItem(item)
    suspend fun deleteBudgetItem(item: BudgetItem) = budgetDao.deleteBudgetItem(item)
    suspend fun copyBudgetFromMonth(sourceMonth: String, targetMonth: String) = budgetDao.copyBudgetFromMonth(sourceMonth, targetMonth)
    fun getSpendingPerCategory(startDate: Long, endDate: Long): Flow<List<CategorySpending>> = expenseDao.getSpendingPerCategory(startDate, endDate)
}
