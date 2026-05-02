package com.alvaronolasco.creditcardtracker.data

import android.util.Log
import com.alvaronolasco.creditcardtracker.data.dao.CategoryDao
import com.alvaronolasco.creditcardtracker.data.dao.CreditCardDao
import com.alvaronolasco.creditcardtracker.data.dao.ExpenseCategoryDao
import com.alvaronolasco.creditcardtracker.data.dao.ExpenseDao
import com.alvaronolasco.creditcardtracker.data.repository.AuthRepository
import com.alvaronolasco.creditcardtracker.data.repository.AuthState
import com.alvaronolasco.creditcardtracker.data.repository.FirestoreSyncRepository
import com.alvaronolasco.creditcardtracker.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: FirestoreSyncRepository,
    private val cardDao: CreditCardDao,
    private val expenseDao: ExpenseDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
    private val categoryDao: CategoryDao,
    private val prefs: UserPreferencesRepository,
    private val applicationScope: CoroutineScope
) {
    fun start() {
        applicationScope.launch {
            authRepository.authState
                .filterIsInstance<AuthState.Authenticated>()
                .catch { Log.e("SyncManager", "auth state error", it) }
                .collect { state -> onAuthenticated(state) }
        }
    }

    private suspend fun onAuthenticated(state: AuthState.Authenticated) {
        runCatching {
            syncRepository.pushUserProfile(state.uid, state.email, state.isAnonymous)
        }.onFailure { Log.w("SyncManager", "pushUserProfile failed", it) }

        if (state.isAnonymous) return

        val lastSyncedUid = prefs.getLastSyncedUid()
        if (state.uid == lastSyncedUid) return

        Log.d("SyncManager", "uid changed ${lastSyncedUid ?: "null"} → ${state.uid}, pulling data")
        runCatching { pullAndReplace(state.uid) }
            .onSuccess { prefs.setLastSyncedUid(state.uid) }
            .onFailure { Log.e("SyncManager", "pull failed", it) }
    }

    private suspend fun pullAndReplace(uid: String) {
        val snapshot = syncRepository.pullAllUserData()

        expenseCategoryDao.deleteAll()
        expenseDao.deleteAll()
        categoryDao.deleteAllNonDefault()
        cardDao.deleteAll()

        snapshot.cards.forEach { cardDao.insertCard(it) }
        snapshot.categories.forEach { categoryDao.insertCategory(it) }
        snapshot.expenses.forEach { expenseDao.insertExpense(it) }
        snapshot.expenseCategories.forEach { expenseCategoryDao.insert(it) }

        Log.d("SyncManager", "pull complete: ${snapshot.cards.size} cards, ${snapshot.expenses.size} expenses")
    }
}
