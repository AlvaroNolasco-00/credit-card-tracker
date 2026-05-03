package com.alvaronolasco.creditcardtracker.data

import android.util.Log
import com.alvaronolasco.creditcardtracker.data.dao.*
import com.alvaronolasco.creditcardtracker.data.entity.*
import com.alvaronolasco.creditcardtracker.data.firestore.*
import com.alvaronolasco.creditcardtracker.data.repository.AuthRepository
import com.alvaronolasco.creditcardtracker.data.repository.AuthState
import com.alvaronolasco.creditcardtracker.data.repository.FirestoreSyncRepository
import com.alvaronolasco.creditcardtracker.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val syncRepository: FirestoreSyncRepository,
    private val syncQueueDao: SyncQueueDao,
    private val authRepository: AuthRepository,
    private val prefs: UserPreferencesRepository,
    private val cardDao: CreditCardDao,
    private val expenseDao: ExpenseDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val incomeDao: IncomeDao,
    private val recurringExpenseDao: RecurringExpenseDao,
    private val configDao: NotificationConfigDao,
    private val migrationHelper: MigrationHelper,
    private val applicationScope: CoroutineScope
) {
    private var syncJob: Job? = null
    private var debounceJob: Job? = null

    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val TAG = "SyncManager"
    }

    fun start() {
        applicationScope.launch {
            authRepository.authState
                .filterIsInstance<AuthState.Authenticated>()
                .collect { state -> onAuthenticated(state) }
        }
    }

    fun requestSync(debounceMs: Long = 2000L) {
        debounceJob?.cancel()
        debounceJob = applicationScope.launch {
            delay(debounceMs)
            val uid = authRepository.currentUid() ?: return@launch
            syncAll(uid)
        }
    }

    private suspend fun onAuthenticated(state: AuthState.Authenticated) {
        syncRepository.pushUserProfile(state.uid, state.email, state.isAnonymous)
        if (!state.isAnonymous) {
            migrationHelper.migrateIfNeeded()
            syncAll(state.uid)
        }
    }

    private fun syncAll(uid: String) {
        syncJob?.cancel()
        syncJob = applicationScope.launch {
            runCatching {
                processQueue(uid)
                syncFromCloud(uid)
            }.onFailure { Log.e(TAG, "syncAll failed", it) }
        }
    }

    private suspend fun processQueue(uid: String) {
        val queue = syncQueueDao.getAll()
        if (queue.isEmpty()) return

        Log.d(TAG, "Processing ${queue.size} queued items")

        for (item in queue) {
            if (item.attemptCount >= MAX_ATTEMPTS) {
                Log.w(TAG, "Dropping ${item.entityType}:${item.entityId} after $MAX_ATTEMPTS failures")
                syncQueueDao.dequeue(item)
                continue
            }
            val result = runCatching { pushItem(uid, item) }
            if (result.isSuccess) {
                syncQueueDao.dequeue(item)
            } else {
                Log.w(TAG, "Failed to sync ${item.entityType}:${item.entityId}", result.exceptionOrNull())
                syncQueueDao.incrementAttempt(item.id)
            }
        }
    }

    private suspend fun pushItem(uid: String, item: SyncQueueItem) {
        val docId = if (item.entityType == "INCOME_PROFILE") uid else "${uid}_${item.entityId}"
        val collection = getCollection(item.entityType)

        when (item.action) {
            "DELETE" -> syncRepository.deleteEntity(collection, docId)
            "UPSERT" -> {
                val data = buildFirestoreMap(uid, item)
                syncRepository.pushEntity(collection, docId, data)
            }
        }
    }

    private fun getCollection(entityType: String): String = when (entityType) {
        "CARD" -> "cards"
        "EXPENSE" -> "expenses"
        "CATEGORY" -> "categories"
        "BUDGET" -> "budgets"
        "INCOME" -> "incomes"
        "INCOME_PROFILE" -> "income_profiles"
        "RECURRING_EXPENSE" -> "recurring_expenses"
        "NOTIFICATION_CONFIG" -> "notification_configs"
        else -> error("Unknown entityType: $entityType")
    }

    private suspend fun buildFirestoreMap(uid: String, item: SyncQueueItem): Map<String, Any?> {
        return when (item.entityType) {
            "CARD" -> {
                val card = cardDao.getCardById(item.entityId) ?: error("Card ${item.entityId} not found")
                card.toFirestoreMap(uid)
            }
            "EXPENSE" -> {
                val ewc = expenseDao.getExpenseWithCategoriesById(item.entityId)
                val expense = ewc?.expense ?: error("Expense ${item.entityId} not found")
                expense.toFirestoreMap(uid, ewc.categories.map { it.id })
            }
            "CATEGORY" -> {
                val cat = categoryDao.getCategoryById(item.entityId) ?: error("Category ${item.entityId} not found")
                cat.toFirestoreMap(uid)
            }
            "BUDGET" -> {
                val budget = budgetDao.getBudgetItemById(item.entityId) ?: error("Budget ${item.entityId} not found")
                budget.toFirestoreMap(uid)
            }
            "INCOME" -> {
                val income = incomeDao.getIncomeEntryById(item.entityId) ?: error("Income ${item.entityId} not found")
                income.toFirestoreMap(uid)
            }
            "INCOME_PROFILE" -> {
                val profile = incomeDao.getProfileOnce() ?: error("IncomeProfile not found")
                profile.toFirestoreMap(uid)
            }
            "RECURRING_EXPENSE" -> {
                val rew = recurringExpenseDao.getWithCategoriesById(item.entityId)
                val rec = rew?.recurringExpense ?: error("RecurringExpense ${item.entityId} not found")
                rec.toFirestoreMap(uid, rew.categories.map { it.id })
            }
            "NOTIFICATION_CONFIG" -> {
                val config = configDao.getConfigById(item.entityId) ?: error("NotificationConfig ${item.entityId} not found")
                config.toFirestoreMap(uid)
            }
            else -> error("Unknown entityType: ${item.entityType}")
        }
    }

    private suspend fun syncFromCloud(uid: String) {
        val lastSync = prefs.getLastSyncTimestamp()
        val now = System.currentTimeMillis()

        if (lastSync == 0L) {
            Log.d(TAG, "First sync — pulling all data for uid=$uid")
        }

        syncCards(uid, lastSync)
        syncExpenses(uid, lastSync)
        syncCategories(uid, lastSync)
        syncBudgets(uid, lastSync)
        syncIncomes(uid, lastSync)
        syncIncomeProfile(uid, lastSync)
        syncRecurringExpenses(uid, lastSync)
        syncNotificationConfigs(uid, lastSync)

        prefs.setLastSyncTimestamp(now)
        Log.d(TAG, "Pull complete at $now")
    }

    private fun shouldProcess(cloudUpdatedAt: Long, localUpdatedAt: Long?): SyncDecision {
        if (localUpdatedAt == null) return SyncDecision.INSERT
        return when {
            cloudUpdatedAt > localUpdatedAt -> SyncDecision.UPDATE
            localUpdatedAt > cloudUpdatedAt -> SyncDecision.ENQUEUE
            else -> SyncDecision.SKIP
        }
    }

    private enum class SyncDecision { INSERT, UPDATE, ENQUEUE, SKIP }

    private suspend fun syncCards(uid: String, lastSync: Long) {
        val docs = syncRepository.pullCollection("cards", uid)
        for (data in docs) {
            val card = data.toCard() ?: continue
            if (card.updatedAt < lastSync) continue
            val existing = cardDao.getCardById(card.id)
            when (shouldProcess(card.updatedAt, existing?.updatedAt)) {
                SyncDecision.INSERT -> cardDao.insertCard(card)
                SyncDecision.UPDATE -> cardDao.updateCard(card)
                SyncDecision.ENQUEUE -> syncQueueDao.enqueue("CARD", card.id, "UPSERT")
                SyncDecision.SKIP -> {}
            }
        }
    }

    private suspend fun syncExpenses(uid: String, lastSync: Long) {
        val docs = syncRepository.pullCollection("expenses", uid)
        for (data in docs) {
            val pair = data.toExpenseWithCatIds() ?: continue
            val (expense, catIds) = pair
            if (expense.updatedAt < lastSync) continue
            val existing = expenseDao.getExpenseWithCategoriesById(expense.id)
            when (shouldProcess(expense.updatedAt, existing?.expense?.updatedAt)) {
                SyncDecision.INSERT -> {
                    expenseDao.insertExpense(expense)
                    catIds.forEach { expenseCategoryDao.insert(ExpenseCategory(expense.id, it)) }
                }
                SyncDecision.UPDATE -> {
                    expenseDao.updateExpense(expense)
                    expenseCategoryDao.replaceExpenseCategories(expense.id, catIds)
                }
                SyncDecision.ENQUEUE -> syncQueueDao.enqueue("EXPENSE", expense.id, "UPSERT")
                SyncDecision.SKIP -> {}
            }
        }
    }

    private suspend fun syncCategories(uid: String, lastSync: Long) {
        val docs = syncRepository.pullCollection("categories", uid)
        for (data in docs) {
            val cat = data.toCategory() ?: continue
            if (cat.updatedAt < lastSync) continue
            val existing = categoryDao.getCategoryById(cat.id)
            when (shouldProcess(cat.updatedAt, existing?.updatedAt)) {
                SyncDecision.INSERT -> categoryDao.insertCategory(cat)
                SyncDecision.UPDATE -> {
                    categoryDao.deleteCategory(existing!!)
                    categoryDao.insertCategory(cat)
                }
                SyncDecision.ENQUEUE -> syncQueueDao.enqueue("CATEGORY", cat.id, "UPSERT")
                SyncDecision.SKIP -> {}
            }
        }
    }

    private suspend fun syncBudgets(uid: String, lastSync: Long) {
        val docs = syncRepository.pullCollection("budgets", uid)
        for (data in docs) {
            val budget = data.toBudgetItem() ?: continue
            if (budget.updatedAt < lastSync) continue
            val existing = budgetDao.getBudgetItemById(budget.id)
            when (shouldProcess(budget.updatedAt, existing?.updatedAt)) {
                SyncDecision.INSERT -> budgetDao.upsertBudgetItem(budget)
                SyncDecision.UPDATE -> budgetDao.upsertBudgetItem(budget)
                SyncDecision.ENQUEUE -> syncQueueDao.enqueue("BUDGET", budget.id, "UPSERT")
                SyncDecision.SKIP -> {}
            }
        }
    }

    private suspend fun syncIncomes(uid: String, lastSync: Long) {
        val docs = syncRepository.pullCollection("incomes", uid)
        for (data in docs) {
            val income = data.toIncomeEntry() ?: continue
            if (income.updatedAt < lastSync) continue
            val existing = incomeDao.getIncomeEntryById(income.id)
            when (shouldProcess(income.updatedAt, existing?.updatedAt)) {
                SyncDecision.INSERT -> incomeDao.insertEntry(income)
                SyncDecision.UPDATE -> incomeDao.updateEntry(income)
                SyncDecision.ENQUEUE -> syncQueueDao.enqueue("INCOME", income.id, "UPSERT")
                SyncDecision.SKIP -> {}
            }
        }
    }

    private suspend fun syncIncomeProfile(uid: String, lastSync: Long) {
        val docs = syncRepository.pullCollection("income_profiles", uid)
        for (data in docs) {
            val profile = data.toIncomeProfile() ?: continue
            if (profile.updatedAt < lastSync) continue
            val existing = incomeDao.getProfileOnce()
            when (shouldProcess(profile.updatedAt, existing?.updatedAt)) {
                SyncDecision.INSERT -> incomeDao.upsertProfile(profile)
                SyncDecision.UPDATE -> incomeDao.upsertProfile(profile)
                SyncDecision.ENQUEUE -> syncQueueDao.enqueue("INCOME_PROFILE", 1, "UPSERT")
                SyncDecision.SKIP -> {}
            }
        }
    }

    private suspend fun syncRecurringExpenses(uid: String, lastSync: Long) {
        val docs = syncRepository.pullCollection("recurring_expenses", uid)
        for (data in docs) {
            val pair = data.toRecurringExpenseWithCatIds() ?: continue
            val (rec, catIds) = pair
            if (rec.updatedAt < lastSync) continue
            val existing = recurringExpenseDao.getWithCategoriesById(rec.id)
            when (shouldProcess(rec.updatedAt, existing?.recurringExpense?.updatedAt)) {
                SyncDecision.INSERT -> {
                    recurringExpenseDao.insert(rec)
                    catIds.forEach { recurringExpenseDao.insertCategory(RecurringExpenseCategory(rec.id, it)) }
                }
                SyncDecision.UPDATE -> {
                    recurringExpenseDao.update(rec)
                    recurringExpenseDao.deleteCategories(rec.id)
                    catIds.forEach { recurringExpenseDao.insertCategory(RecurringExpenseCategory(rec.id, it)) }
                }
                SyncDecision.ENQUEUE -> syncQueueDao.enqueue("RECURRING_EXPENSE", rec.id, "UPSERT")
                SyncDecision.SKIP -> {}
            }
        }
    }

    private suspend fun syncNotificationConfigs(uid: String, lastSync: Long) {
        val docs = syncRepository.pullCollection("notification_configs", uid)
        for (data in docs) {
            val config = data.toNotificationConfig() ?: continue
            if (config.updatedAt < lastSync) continue
            val existing = configDao.getConfigById(config.id)
            when (shouldProcess(config.updatedAt, existing?.updatedAt)) {
                SyncDecision.INSERT -> configDao.insertConfigs(listOf(config))
                SyncDecision.UPDATE -> configDao.updateConfig(config)
                SyncDecision.ENQUEUE -> syncQueueDao.enqueue("NOTIFICATION_CONFIG", config.id, "UPSERT")
                SyncDecision.SKIP -> {}
            }
        }
    }
}
