package com.alvaronolasco.creditcardtracker.data.dao

import androidx.room.*
import com.alvaronolasco.creditcardtracker.data.entity.IncomeEntry
import com.alvaronolasco.creditcardtracker.data.entity.IncomeProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Query("SELECT * FROM income_profiles WHERE id = 1")
    fun getProfile(): Flow<IncomeProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: IncomeProfile)

    @Query("SELECT * FROM income_entries WHERE isActive = 1")
    fun getAllActiveEntries(): Flow<List<IncomeEntry>>

    @Query("SELECT * FROM income_entries WHERE isActive = 1 AND isRecurring = 1")
    fun getRecurringEntries(): Flow<List<IncomeEntry>>

    @Query("SELECT * FROM income_entries WHERE isActive = 1 AND (isRecurring = 1 OR monthYear = :monthYear)")
    fun getEntriesForMonth(monthYear: String): Flow<List<IncomeEntry>>

    @Query("SELECT SUM(amount) FROM income_entries WHERE isActive = 1 AND (isRecurring = 1 OR monthYear = :monthYear)")
    fun getTotalIncomeForMonth(monthYear: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: IncomeEntry): Long

    @Update
    suspend fun updateEntry(entry: IncomeEntry)

    @Delete
    suspend fun deleteEntry(entry: IncomeEntry)
}
