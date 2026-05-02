package com.alvaronolasco.creditcardtracker.data.repository

import android.util.Log
import com.alvaronolasco.creditcardtracker.data.entity.Category
import com.alvaronolasco.creditcardtracker.data.entity.CreditCard
import com.alvaronolasco.creditcardtracker.data.entity.Expense
import com.alvaronolasco.creditcardtracker.data.entity.ExpenseCategory
import com.alvaronolasco.creditcardtracker.data.firestore.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class SyncSnapshot(
    val cards: List<CreditCard>,
    val expenses: List<Expense>,
    val expenseCategories: List<ExpenseCategory>,
    val categories: List<Category>
)

@Singleton
class FirestoreSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) {
    private fun userDoc() = authRepository.currentUid()
        ?.let { firestore.collection("users").document(it) }

    suspend fun pushCard(card: CreditCard) {
        userDoc()?.collection("cards")?.document(card.id.toString())
            ?.set(card.toFirestoreMap())?.await()
    }

    suspend fun deleteCard(id: Int) {
        userDoc()?.collection("cards")?.document(id.toString())?.delete()?.await()
    }

    suspend fun pushExpense(expense: Expense, categoryIds: List<Int>) {
        userDoc()?.collection("expenses")?.document(expense.id.toString())
            ?.set(expense.toFirestoreMap(categoryIds))?.await()
    }

    suspend fun deleteExpense(id: Int) {
        userDoc()?.collection("expenses")?.document(id.toString())?.delete()?.await()
    }

    suspend fun pushCategory(category: Category) {
        userDoc()?.collection("categories")?.document(category.id.toString())
            ?.set(category.toFirestoreMap())?.await()
    }

    suspend fun deleteCategory(id: Int) {
        userDoc()?.collection("categories")?.document(id.toString())?.delete()?.await()
    }

    suspend fun pushUserProfile(uid: String, email: String?, isAnonymous: Boolean) {
        val profileData = mapOf(
            "uid" to uid,
            "email" to email,
            "isAnonymous" to isAnonymous,
            "lastLoginAt" to System.currentTimeMillis()
        )
        firestore.collection("users").document(uid)
            .set(profileData).await()
    }

    suspend fun pullAllUserData(): SyncSnapshot = coroutineScope {
        val uid = authRepository.currentUid() ?: return@coroutineScope SyncSnapshot(emptyList(), emptyList(), emptyList(), emptyList())
        val userRef = firestore.collection("users").document(uid)

        val cardsDeferred = async {
            userRef.collection("cards").get().await().documents
                .mapNotNull { it.data?.toCreditCard() }
        }
        val expensesDeferred = async {
            userRef.collection("expenses").get().await().documents
                .mapNotNull { doc ->
                    doc.data?.let { data ->
                        data.toExpense()?.let { expense ->
                            Pair(expense, data.toCategoryIds())
                        }
                    }
                }
        }
        val categoriesDeferred = async {
            userRef.collection("categories").get().await().documents
                .mapNotNull { it.data?.toCategory() }
        }

        val cards = cardsDeferred.await()
        val expensePairs = expensesDeferred.await()
        val categories = categoriesDeferred.await()

        val expenses = expensePairs.map { it.first }
        val expenseCategories = expensePairs.flatMap { (expense, catIds) ->
            catIds.map { ExpenseCategory(expense.id, it) }
        }

        SyncSnapshot(cards, expenses, expenseCategories, categories)
    }
}
