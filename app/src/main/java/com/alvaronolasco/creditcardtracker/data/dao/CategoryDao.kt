package com.alvaronolasco.creditcardtracker.data.dao

import androidx.room.*
import com.alvaronolasco.creditcardtracker.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories WHERE isDefault = 1")
    suspend fun getDefaultCategories(): List<Category>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesOnce(): List<Category>

    @Query("DELETE FROM categories WHERE isDefault = 0")
    suspend fun deleteAllNonDefault()
}
