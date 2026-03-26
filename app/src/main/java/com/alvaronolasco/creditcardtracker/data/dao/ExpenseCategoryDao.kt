package com.alvaronolasco.creditcardtracker.data.dao

import androidx.room.*
import com.alvaronolasco.creditcardtracker.data.entity.ExpenseCategory

@Dao
interface ExpenseCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ec: ExpenseCategory)

    @Query("DELETE FROM expense_categories WHERE expenseId = :expenseId")
    suspend fun deleteByExpenseId(expenseId: Int)

    @Transaction
    suspend fun replaceExpenseCategories(expenseId: Int, categoryIds: List<Int>) {
        deleteByExpenseId(expenseId)
        categoryIds.forEach { catId ->
            insert(ExpenseCategory(expenseId = expenseId, categoryId = catId))
        }
    }
}
