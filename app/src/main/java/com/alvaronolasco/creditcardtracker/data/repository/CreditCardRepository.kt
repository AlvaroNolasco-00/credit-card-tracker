package com.alvaronolasco.creditcardtracker.data.repository

import com.alvaronolasco.creditcardtracker.data.dao.*
import com.alvaronolasco.creditcardtracker.data.SyncManager
import com.alvaronolasco.creditcardtracker.data.dao.*
import com.alvaronolasco.creditcardtracker.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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
    private val recurringExpenseDao: RecurringExpenseDao,
    private val syncQueueDao: SyncQueueDao,
    private val syncManager: SyncManager,
    private val applicationScope: CoroutineScope
) {
    private fun enqueueSync(type: String, id: Int, action: String = "UPSERT") {
        applicationScope.launch {
            syncQueueDao.enqueue(type, id, action)
            syncManager.requestSync(debounceMs = 2000L)
        }
    }

    // Cards
    fun getAllCards(): Flow<List<CreditCard>> = cardDao.getAllCards()
    suspend fun getCardById(id: Int): CreditCard? = cardDao.getCardById(id)
    suspend fun insertCard(card: CreditCard): Int {
        val withTs = card.copy(updatedAt = System.currentTimeMillis())
        val cardId = cardDao.insertCard(withTs).toInt()
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
        enqueueSync("CARD", cardId)
        return cardId
    }
    suspend fun updateCard(card: CreditCard) {
        val updated = card.copy(updatedAt = System.currentTimeMillis())
        cardDao.updateCard(updated)
        activityLogDao.insertLog(
            ActivityLog(category = "CARD", action = "UPDATED", description = "Tarjeta '${card.name}' actualizada", entityId = card.id, entityType = "CARD")
        )
        enqueueSync("CARD", card.id)
    }
    suspend fun deleteCard(card: CreditCard) {
        cardDao.deleteCard(card)
        activityLogDao.insertLog(
            ActivityLog(category = "CARD", action = "DELETED", description = "Tarjeta '${card.name}' eliminada", entityId = card.id, entityType = "CARD")
        )
        enqueueSync("CARD", card.id, "DELETE")
    }

    suspend fun logPayment(cardId: Int, cardName: String, amount: Double) {
        activityLogDao.insertLog(
            ActivityLog(
                category = "CARD",
                action = "PAYMENT",
                description = "Pago de \$${String.format("%.2f", amount)} a tarjeta '$cardName'",
                entityId = cardId,
                entityType = "CARD",
                amount = amount
            )
        )
    }

    // Categories
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun insertCategory(category: Category): Long {
        val withTs = category.copy(updatedAt = System.currentTimeMillis())
        val id = categoryDao.insertCategory(withTs)
        activityLogDao.insertLog(
            ActivityLog(category = "CATEGORY", action = "CREATED", description = "Categoría '${category.name}' creada", entityId = id.toInt(), entityType = "CATEGORY")
        )
        enqueueSync("CATEGORY", id.toInt())
        return id
    }
    suspend fun deleteCategory(category: Category) {
        if (!category.isDefault) {
            categoryDao.deleteCategory(category)
            activityLogDao.insertLog(
                ActivityLog(category = "CATEGORY", action = "DELETED", description = "Categoría '${category.name}' eliminada", entityId = category.id, entityType = "CATEGORY")
            )
            enqueueSync("CATEGORY", category.id, "DELETE")
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
    fun hasExpenses(cardId: Int): Flow<Boolean> =
        expenseDao.hasExpenses(cardId)
    suspend fun insertExpense(expense: Expense): Long {
        val withTs = expense.copy(updatedAt = System.currentTimeMillis())
        val id = expenseDao.insertExpense(withTs)
        val desc = if (expense.description.isNotBlank()) expense.description else "Sin descripción"
        val label = if (expense.cardId == null) "Gasto personal" else "Gasto"
        activityLogDao.insertLog(
            ActivityLog(category = "EXPENSE", action = "CREATED", description = "$label '$desc' por \$${String.format("%.2f", expense.amount)} agregado", entityId = id.toInt(), entityType = "EXPENSE")
        )
        enqueueSync("EXPENSE", id.toInt())
        return id
    }
    suspend fun updateExpense(expense: Expense) {
        val updated = expense.copy(updatedAt = System.currentTimeMillis())
        expenseDao.updateExpense(updated)
        val desc = if (expense.description.isNotBlank()) expense.description else "Sin descripción"
        activityLogDao.insertLog(
            ActivityLog(category = "EXPENSE", action = "UPDATED", description = "Gasto '$desc' actualizado", entityId = expense.id, entityType = "EXPENSE")
        )
        enqueueSync("EXPENSE", expense.id)
    }
    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
        val desc = if (expense.description.isNotBlank()) expense.description else "Sin descripción"
        activityLogDao.insertLog(
            ActivityLog(category = "EXPENSE", action = "DELETED", description = "Gasto '$desc' por \$${String.format("%.2f", expense.amount)} eliminado", entityId = expense.id, entityType = "EXPENSE")
        )
        enqueueSync("EXPENSE", expense.id, "DELETE")
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
        val expense = expenseDao.getExpenseWithCategoriesById(expenseId)?.expense ?: return
        expenseDao.updateExpense(expense.copy(updatedAt = System.currentTimeMillis()))
        expenseCategoryDao.replaceExpenseCategories(expenseId, categoryIds)
        activityLogDao.insertLog(
            ActivityLog(category = "EXPENSE", action = "UPDATED", description = "Categorías del gasto #$expenseId actualizadas", entityId = expenseId, entityType = "EXPENSE")
        )
        enqueueSync("EXPENSE", expenseId)
    }

    // Configs
    fun getConfigsByCard(cardId: Int): Flow<List<NotificationConfig>> = configDao.getConfigsByCard(cardId)
    suspend fun updateConfig(config: NotificationConfig) {
        val updated = config.copy(updatedAt = System.currentTimeMillis())
        configDao.updateConfig(updated)
        activityLogDao.insertLog(
            ActivityLog(category = "NOTIFICATION", action = "UPDATED", description = "Configuración de notificación actualizada", entityId = config.id, entityType = "NOTIFICATION")
        )
        enqueueSync("NOTIFICATION_CONFIG", config.id)
    }

    // Income
    fun getIncomeProfile(): Flow<IncomeProfile?> = incomeDao.getProfile()
    suspend fun saveIncomeProfile(profile: IncomeProfile) {
        val updated = profile.copy(updatedAt = System.currentTimeMillis())
        incomeDao.upsertProfile(updated)
        activityLogDao.insertLog(
            ActivityLog(category = "INCOME", action = "UPDATED", description = "Perfil de ingresos guardado", entityType = "INCOME_PROFILE")
        )
        enqueueSync("INCOME_PROFILE", 1)
    }
    fun getAllActiveIncomeEntries(): Flow<List<IncomeEntry>> = incomeDao.getAllActiveEntries()
    fun getRecurringIncomeEntries(): Flow<List<IncomeEntry>> = incomeDao.getRecurringEntries()
    fun getIncomeEntriesForMonth(monthYear: String): Flow<List<IncomeEntry>> = incomeDao.getEntriesForMonth(monthYear)
    fun getTotalIncomeForMonth(monthYear: String): Flow<Double?> = incomeDao.getTotalIncomeForMonth(monthYear)
    suspend fun insertIncomeEntry(entry: IncomeEntry) {
        val withTs = entry.copy(updatedAt = System.currentTimeMillis())
        val id = incomeDao.insertEntry(withTs)
        activityLogDao.insertLog(
            ActivityLog(category = "INCOME", action = "CREATED", description = "Ingreso '${entry.label}' por \$${String.format("%.2f", entry.amount)} agregado", entityType = "INCOME_ENTRY")
        )
        enqueueSync("INCOME", id.toInt())
    }
    suspend fun updateIncomeEntry(entry: IncomeEntry) {
        val updated = entry.copy(updatedAt = System.currentTimeMillis())
        incomeDao.updateEntry(updated)
        activityLogDao.insertLog(
            ActivityLog(category = "INCOME", action = "UPDATED", description = "Ingreso '${entry.label}' actualizado", entityId = entry.id, entityType = "INCOME_ENTRY")
        )
        enqueueSync("INCOME", entry.id)
    }
    suspend fun deleteIncomeEntry(entry: IncomeEntry) {
        incomeDao.deleteEntry(entry)
        activityLogDao.insertLog(
            ActivityLog(category = "INCOME", action = "DELETED", description = "Ingreso '${entry.label}' eliminado", entityId = entry.id, entityType = "INCOME_ENTRY")
        )
        enqueueSync("INCOME", entry.id, "DELETE")
    }

    // Budget
    fun getBudgetItemsForMonth(monthYear: String): Flow<List<BudgetItem>> = budgetDao.getBudgetItemsForMonth(monthYear)
    suspend fun getBudgetItemForCategory(categoryId: Int, monthYear: String): BudgetItem? = budgetDao.getBudgetItemForCategory(categoryId, monthYear)
    suspend fun upsertBudgetItem(item: BudgetItem) {
        val withTs = item.copy(updatedAt = System.currentTimeMillis())
        budgetDao.upsertBudgetItem(withTs)
        activityLogDao.insertLog(
            ActivityLog(category = "BUDGET", action = "CREATED", description = "Presupuesto de \$${String.format("%.2f", item.limitAmount)} guardado para ${item.monthYear}", entityId = item.id, entityType = "BUDGET")
        )
        enqueueSync("BUDGET", withTs.id)
    }
    suspend fun deleteBudgetItem(item: BudgetItem) {
        budgetDao.deleteBudgetItem(item)
        activityLogDao.insertLog(
            ActivityLog(category = "BUDGET", action = "DELETED", description = "Presupuesto de ${item.monthYear} eliminado", entityId = item.id, entityType = "BUDGET")
        )
        enqueueSync("BUDGET", item.id, "DELETE")
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
        val withTs = expense.copy(updatedAt = System.currentTimeMillis())
        val id = recurringExpenseDao.insert(withTs).toInt()
        categoryIds.forEach { recurringExpenseDao.insertCategory(RecurringExpenseCategory(id, it)) }
        activityLogDao.insertLog(
            ActivityLog(category = "RECURRING_EXPENSE", action = "CREATED", description = "Gasto recurrente '${expense.description}' por \$${String.format("%.2f", expense.amount)} agregado", entityId = id, entityType = "RECURRING_EXPENSE")
        )
        enqueueSync("RECURRING_EXPENSE", id)
        return id
    }

    suspend fun updateRecurringExpense(expense: RecurringExpense, categoryIds: List<Int>) {
        val updated = expense.copy(updatedAt = System.currentTimeMillis())
        recurringExpenseDao.update(updated)
        recurringExpenseDao.deleteCategories(expense.id)
        categoryIds.forEach { recurringExpenseDao.insertCategory(RecurringExpenseCategory(expense.id, it)) }
        activityLogDao.insertLog(
            ActivityLog(category = "RECURRING_EXPENSE", action = "UPDATED", description = "Gasto recurrente '${expense.description}' actualizado", entityId = expense.id, entityType = "RECURRING_EXPENSE")
        )
        enqueueSync("RECURRING_EXPENSE", expense.id)
    }

    suspend fun deleteRecurringExpense(expense: RecurringExpense) {
        recurringExpenseDao.delete(expense)
        activityLogDao.insertLog(
            ActivityLog(category = "RECURRING_EXPENSE", action = "DELETED", description = "Gasto recurrente '${expense.description}' eliminado", entityId = expense.id, entityType = "RECURRING_EXPENSE")
        )
        enqueueSync("RECURRING_EXPENSE", expense.id, "DELETE")
    }

    // Activity Logs
    fun getAllActivityLogs(): Flow<List<ActivityLog>> = activityLogDao.getAllLogs()
    fun getActivityLogsByCategory(category: String): Flow<List<ActivityLog>> = activityLogDao.getLogsByCategory(category)
    fun getLogsByEntityInPeriod(entityId: Int, entityType: String, start: Long, end: Long): Flow<List<ActivityLog>> = 
        activityLogDao.getLogsByEntityInPeriod(entityId, entityType, start, end)
    suspend fun getActivityLogById(id: Int): ActivityLog? = activityLogDao.getLogById(id)
}
