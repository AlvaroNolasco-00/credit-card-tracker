package com.alvaronolasco.creditcardtracker.data.dao

import androidx.room.*
import com.alvaronolasco.creditcardtracker.data.entity.CategorySpending
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

    @Transaction
    @Query("SELECT * FROM expenses WHERE cardId = :cardId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesWithCategoriesByCardInPeriod(cardId: Int, startDate: Long, endDate: Long): Flow<List<ExpenseWithCategories>>

    @Query("""
        SELECT SUM(CASE WHEN msiMonths > 1 THEN msiMonthlyAmount ELSE amount END)
        FROM expenses
        WHERE cardId = :cardId
        AND (
            (msiMonths <= 1 AND date BETWEEN :startDate AND :endDate)
            OR
            (msiMonths > 1 AND date <= :endDate AND msiEndDate >= :startDate)
        )
    """)
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

    @Transaction
    @Query("SELECT * FROM expenses WHERE cardId IS NULL AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getNonCardExpensesInPeriod(startDate: Long, endDate: Long): Flow<List<ExpenseWithCategories>>

    @Query("SELECT SUM(amount) FROM expenses WHERE cardId IS NULL AND date BETWEEN :startDate AND :endDate")
    fun getTotalNonCardSpentInPeriod(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesOnce(): List<Expense>

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Query("""
        SELECT c.id, c.name, c.icon, c.isDefault, c.createdAt,
               COALESCE(SUM(CASE WHEN e.msiMonths > 1 THEN e.msiMonthlyAmount ELSE e.amount END), 0) AS totalSpent
        FROM categories c
        LEFT JOIN expense_categories ec ON c.id = ec.categoryId
        LEFT JOIN expenses e ON ec.expenseId = e.id
            AND (
                (e.msiMonths <= 1 AND e.date BETWEEN :startDate AND :endDate)
                OR
                (e.msiMonths > 1 AND e.date <= :endDate AND e.msiEndDate >= :startDate)
            )
        GROUP BY c.id
        ORDER BY c.isDefault DESC, c.name ASC
    """)
    fun getSpendingPerCategory(startDate: Long, endDate: Long): Flow<List<CategorySpending>>
}
