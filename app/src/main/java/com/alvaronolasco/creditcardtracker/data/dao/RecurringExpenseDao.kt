package com.alvaronolasco.creditcardtracker.data.dao

import androidx.room.*
import com.alvaronolasco.creditcardtracker.data.entity.RecurringExpense
import com.alvaronolasco.creditcardtracker.data.entity.RecurringExpenseCategory
import com.alvaronolasco.creditcardtracker.data.entity.RecurringExpenseWithCategories
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {
    @Transaction
    @Query("SELECT * FROM recurring_expenses WHERE cardId = :cardId AND isActive = 1")
    fun getActiveByCard(cardId: Int): Flow<List<RecurringExpenseWithCategories>>

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1")
    fun getAllActive(): Flow<List<RecurringExpense>>

    @Transaction
    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getWithCategoriesById(id: Int): RecurringExpenseWithCategories?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringExpense: RecurringExpense): Long

    @Update
    suspend fun update(recurringExpense: RecurringExpense)

    @Delete
    suspend fun delete(recurringExpense: RecurringExpense)

    @Query("DELETE FROM recurring_expense_categories WHERE recurringExpenseId = :id")
    suspend fun deleteCategories(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(crossRef: RecurringExpenseCategory)
}
