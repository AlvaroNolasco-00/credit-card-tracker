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
    private val budgetDao: BudgetDao,
    private val activityLogDao: ActivityLogDao,
    private val recurringExpenseDao: RecurringExpenseDao
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
        activityLogDao.insertLog(
            ActivityLog(category = "CARD", action = "CREATED", description = "Tarjeta '${card.name}' creada", entityId = cardId, entityType = "CARD")
        )
        return cardId
    }
    suspend fun updateCard(card: CreditCard) {
        cardDao.updateCard(card)
        activityLogDao.insertLog(
            ActivityLog(category = "CARD", action = "UPDATED", description = "Tarjeta '${card.name}' actualizada", entityId = card.id, entityType = "CARD")
        )
    }
    suspend fun deleteCard(card: CreditCard) {
        cardDao.deleteCard(card)
        activityLogDao.insertLog(
            ActivityLog(category = "CARD", action = "DELETED", description = "Tarjeta '${card.name}' eliminada", entityId = card.id, entityType = "CARD")
        )
    }

    suspend fun logPayment(cardId: Int, cardName: String, amount: Double) {
        activityLogDao.insertLog(
            ActivityLog(
                category = "CARD",
                action = "PAYMENT",
                description = "Pago de \$${String.format("%.2f", amount)} a tarjeta '$cardName'",
                entityId = cardId,
                entityType = "CARD"
            )
        )
    }

    // Categories
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun insertCategory(category: Category): Long {
        val id = categoryDao.insertCategory(category)
        activityLogDao.insertLog(
            ActivityLog(category = "CATEGORY", action = "CREATED", description = "Categoría '${category.name}' creada", entityId = id.toInt(), entityType = "CATEGORY")
        )
        return id
    }
    suspend fun deleteCategory(category: Category) {
        if (!category.isDefault) {
            categoryDao.deleteCategory(category)
            activityLogDao.insertLog(
                ActivityLog(category = "CATEGORY", action = "DELETED", description = "Categoría '${category.name}' eliminada", entityId = category.id, entityType = "CATEGORY")
            )
        }
    }

    // Expenses
    fun getExpensesWithCategoriesByCard(cardId: Int): Flow<List<ExpenseWithCategories>> =
        expenseDao.getExpensesWithCategoriesByCard(cardId)
    fun getExpensesByCardInPeriod(cardId: Int, start: Long, end: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByCardInPeriod(cardId, start, end)
    fun getExpensesWithCategoriesInPeriod(cardId: Int, start: Long, end: Long): Flow<List<ExpenseWithCategories>> =
        expenseDao.getExpensesWithCategoriesByCardInPeriod(cardId, start, end)
    fun getTotalSpentInPeriod(cardId: Int, start: Long, end: Long): Flow<Double?> =
        expenseDao.getTotalSpentInPeriod(cardId, start, end)
    suspend fun insertExpense(expense: Expense): Long {
        val id = expenseDao.insertExpense(expense)
        val desc = if (expense.description.isNotBlank()) expense.description else "Sin descripción"
        val label = if (expense.cardId == null) "Gasto personal" else "Gasto"
        activityLogDao.insertLog(
            ActivityLog(category = "EXPENSE", action = "CREATED", description = "$label '$desc' por \$${String.format("%.2f", expense.amount)} agregado", entityId = id.toInt(), entityType = "EXPENSE")
        )
        return id
    }
    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
        val desc = if (expense.description.isNotBlank()) expense.description else "Sin descripción"
        activityLogDao.insertLog(
            ActivityLog(category = "EXPENSE", action = "UPDATED", description = "Gasto '$desc' actualizado", entityId = expense.id, entityType = "EXPENSE")
        )
    }
    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
        val desc = if (expense.description.isNotBlank()) expense.description else "Sin descripción"
        activityLogDao.insertLog(
            ActivityLog(category = "EXPENSE", action = "DELETED", description = "Gasto '$desc' por \$${String.format("%.2f", expense.amount)} eliminado", entityId = expense.id, entityType = "EXPENSE")
        )
    }
    suspend fun getExpenseWithCategoriesById(id: Int): ExpenseWithCategories? =
        expenseDao.getExpenseWithCategoriesById(id)
    fun getAllExpensesWithCategoriesInPeriod(start: Long, end: Long): Flow<List<ExpenseWithCategories>> =
        expenseDao.getAllExpensesWithCategoriesInPeriod(start, end)
    fun getNonCardExpensesInPeriod(start: Long, end: Long): Flow<List<ExpenseWithCategories>> =
        expenseDao.getNonCardExpensesInPeriod(start, end)
    fun getTotalNonCardSpentInPeriod(start: Long, end: Long): Flow<Double?> =
        expenseDao.getTotalNonCardSpentInPeriod(start, end)
    suspend fun setExpenseCategories(expenseId: Int, categoryIds: List<Int>) {
        expenseCategoryDao.replaceExpenseCategories(expenseId, categoryIds)
        activityLogDao.insertLog(
            ActivityLog(category = "EXPENSE", action = "UPDATED", description = "Categorías del gasto #$expenseId actualizadas", entityId = expenseId, entityType = "EXPENSE")
        )
    }

    // Configs
    fun getConfigsByCard(cardId: Int): Flow<List<NotificationConfig>> = configDao.getConfigsByCard(cardId)
    suspend fun updateConfig(config: NotificationConfig) {
        configDao.updateConfig(config)
        activityLogDao.insertLog(
            ActivityLog(category = "NOTIFICATION", action = "UPDATED", description = "Configuración de notificación actualizada", entityId = config.id, entityType = "NOTIFICATION")
        )
    }

    // Income
    fun getIncomeProfile(): Flow<IncomeProfile?> = incomeDao.getProfile()
    suspend fun saveIncomeProfile(profile: IncomeProfile) {
        incomeDao.upsertProfile(profile)
        activityLogDao.insertLog(
            ActivityLog(category = "INCOME", action = "UPDATED", description = "Perfil de ingresos guardado", entityType = "INCOME_PROFILE")
        )
    }
    fun getAllActiveIncomeEntries(): Flow<List<IncomeEntry>> = incomeDao.getAllActiveEntries()
    fun getRecurringIncomeEntries(): Flow<List<IncomeEntry>> = incomeDao.getRecurringEntries()
    fun getIncomeEntriesForMonth(monthYear: String): Flow<List<IncomeEntry>> = incomeDao.getEntriesForMonth(monthYear)
    fun getTotalIncomeForMonth(monthYear: String): Flow<Double?> = incomeDao.getTotalIncomeForMonth(monthYear)
    suspend fun insertIncomeEntry(entry: IncomeEntry) {
        incomeDao.insertEntry(entry)
        activityLogDao.insertLog(
            ActivityLog(category = "INCOME", action = "CREATED", description = "Ingreso '${entry.label}' por \$${String.format("%.2f", entry.amount)} agregado", entityType = "INCOME_ENTRY")
        )
    }
    suspend fun updateIncomeEntry(entry: IncomeEntry) {
        incomeDao.updateEntry(entry)
        activityLogDao.insertLog(
            ActivityLog(category = "INCOME", action = "UPDATED", description = "Ingreso '${entry.label}' actualizado", entityId = entry.id, entityType = "INCOME_ENTRY")
        )
    }
    suspend fun deleteIncomeEntry(entry: IncomeEntry) {
        incomeDao.deleteEntry(entry)
        activityLogDao.insertLog(
            ActivityLog(category = "INCOME", action = "DELETED", description = "Ingreso '${entry.label}' eliminado", entityId = entry.id, entityType = "INCOME_ENTRY")
        )
    }

    // Budget
    fun getBudgetItemsForMonth(monthYear: String): Flow<List<BudgetItem>> = budgetDao.getBudgetItemsForMonth(monthYear)
    suspend fun getBudgetItemForCategory(categoryId: Int, monthYear: String): BudgetItem? = budgetDao.getBudgetItemForCategory(categoryId, monthYear)
    suspend fun upsertBudgetItem(item: BudgetItem) {
        budgetDao.upsertBudgetItem(item)
        activityLogDao.insertLog(
            ActivityLog(category = "BUDGET", action = "CREATED", description = "Presupuesto de \$${String.format("%.2f", item.limitAmount)} guardado para ${item.monthYear}", entityId = item.id, entityType = "BUDGET")
        )
    }
    suspend fun deleteBudgetItem(item: BudgetItem) {
        budgetDao.deleteBudgetItem(item)
        activityLogDao.insertLog(
            ActivityLog(category = "BUDGET", action = "DELETED", description = "Presupuesto de ${item.monthYear} eliminado", entityId = item.id, entityType = "BUDGET")
        )
    }
    suspend fun copyBudgetFromMonth(sourceMonth: String, targetMonth: String) {
        budgetDao.copyBudgetFromMonth(sourceMonth, targetMonth)
        activityLogDao.insertLog(
            ActivityLog(category = "BUDGET", action = "CREATED", description = "Presupuesto copiado de $sourceMonth a $targetMonth", entityType = "BUDGET")
        )
    }
    fun getSpendingPerCategory(startDate: Long, endDate: Long): Flow<List<CategorySpending>> = expenseDao.getSpendingPerCategory(startDate, endDate)

    // Recurring Expenses
    fun getRecurringExpensesByCard(cardId: Int): Flow<List<RecurringExpenseWithCategories>> =
        recurringExpenseDao.getActiveByCard(cardId)

    fun getAllRecurringExpenses(): Flow<List<RecurringExpense>> =
        recurringExpenseDao.getAllActive()

    suspend fun getRecurringExpenseWithCategoriesById(id: Int): RecurringExpenseWithCategories? =
        recurringExpenseDao.getWithCategoriesById(id)

    suspend fun insertRecurringExpense(expense: RecurringExpense, categoryIds: List<Int>): Int {
        val id = recurringExpenseDao.insert(expense).toInt()
        categoryIds.forEach { recurringExpenseDao.insertCategory(RecurringExpenseCategory(id, it)) }
        activityLogDao.insertLog(
            ActivityLog(category = "RECURRING_EXPENSE", action = "CREATED", description = "Gasto recurrente '${expense.description}' por \$${String.format("%.2f", expense.amount)} agregado", entityId = id, entityType = "RECURRING_EXPENSE")
        )
        return id
    }

    suspend fun updateRecurringExpense(expense: RecurringExpense, categoryIds: List<Int>) {
        recurringExpenseDao.update(expense)
        recurringExpenseDao.deleteCategories(expense.id)
        categoryIds.forEach { recurringExpenseDao.insertCategory(RecurringExpenseCategory(expense.id, it)) }
        activityLogDao.insertLog(
            ActivityLog(category = "RECURRING_EXPENSE", action = "UPDATED", description = "Gasto recurrente '${expense.description}' actualizado", entityId = expense.id, entityType = "RECURRING_EXPENSE")
        )
    }

    suspend fun deleteRecurringExpense(expense: RecurringExpense) {
        recurringExpenseDao.delete(expense)
        activityLogDao.insertLog(
            ActivityLog(category = "RECURRING_EXPENSE", action = "DELETED", description = "Gasto recurrente '${expense.description}' eliminado", entityId = expense.id, entityType = "RECURRING_EXPENSE")
        )
    }

    // Activity Logs
    fun getAllActivityLogs(): Flow<List<ActivityLog>> = activityLogDao.getAllLogs()
    fun getActivityLogsByCategory(category: String): Flow<List<ActivityLog>> = activityLogDao.getLogsByCategory(category)
    fun getLogsByEntityInPeriod(entityId: Int, entityType: String, start: Long, end: Long): Flow<List<ActivityLog>> = 
        activityLogDao.getLogsByEntityInPeriod(entityId, entityType, start, end)
    suspend fun getActivityLogById(id: Int): ActivityLog? = activityLogDao.getLogById(id)
}
