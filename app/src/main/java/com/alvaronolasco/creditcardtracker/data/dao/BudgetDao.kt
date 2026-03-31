package com.alvaronolasco.creditcardtracker.data.dao

import androidx.room.*
import com.alvaronolasco.creditcardtracker.data.entity.BudgetItem
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget_items WHERE monthYear = :monthYear")
    fun getBudgetItemsForMonth(monthYear: String): Flow<List<BudgetItem>>

    @Query("SELECT * FROM budget_items WHERE categoryId = :categoryId AND monthYear = :monthYear LIMIT 1")
    suspend fun getBudgetItemForCategory(categoryId: Int, monthYear: String): BudgetItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgetItem(item: BudgetItem)

    @Delete
    suspend fun deleteBudgetItem(item: BudgetItem)

    @Query("""
        INSERT OR REPLACE INTO budget_items (categoryId, monthYear, limitAmount, createdAt)
        SELECT categoryId, :targetMonth, limitAmount, :createdAt
        FROM budget_items
        WHERE monthYear = :sourceMonth
          AND categoryId NOT IN (
              SELECT categoryId FROM budget_items WHERE monthYear = :targetMonth
          )
    """)
    suspend fun copyBudgetFromMonth(sourceMonth: String, targetMonth: String, createdAt: Long = System.currentTimeMillis())
}
