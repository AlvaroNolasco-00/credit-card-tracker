package com.alvaronolasco.creditcardtracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alvaronolasco.creditcardtracker.data.entity.ActivityLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs WHERE category = :category ORDER BY timestamp DESC")
    fun getLogsByCategory(category: String): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs WHERE id = :id")
    suspend fun getLogById(id: Int): ActivityLog?

    @Insert
    suspend fun insertLog(log: ActivityLog)

    @Query("SELECT * FROM activity_logs WHERE entityId = :entityId AND entityType = :entityType AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getLogsByEntityInPeriod(entityId: Int, entityType: String, start: Long, end: Long): Flow<List<ActivityLog>>

    @Query("DELETE FROM activity_logs WHERE timestamp < :before")
    suspend fun deleteLogsBefore(before: Long)
}
