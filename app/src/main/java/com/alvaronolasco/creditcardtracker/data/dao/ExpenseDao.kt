package com.alvaronolasco.creditcardtracker.data.dao

import androidx.room.*
import com.alvaronolasco.creditcardtracker.data.entity.Expense
import com.alvaronolasco.creditcardtracker.data.entity.ExpenseWithCategories
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Transaction
    @Query("SELECT * FROM expenses WHERE cardId = :cardId ORDER BY date DESC")
    fun getExpensesWithCategoriesByCard(cardId: Int): Flow<List<ExpenseWithCategories>>

    @Query("SELECT * FROM expenses WHERE cardId = :cardId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByCardInPeriod(cardId: Int, startDate: Long, endDate: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE cardId = :cardId AND date BETWEEN :startDate AND :endDate")
    fun getTotalSpentInPeriod(cardId: Int, startDate: Long, endDate: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseWithCategoriesById(id: Int): ExpenseWithCategories?

    @Transaction
    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getAllExpensesWithCategoriesInPeriod(startDate: Long, endDate: Long): Flow<List<ExpenseWithCategories>>
}
