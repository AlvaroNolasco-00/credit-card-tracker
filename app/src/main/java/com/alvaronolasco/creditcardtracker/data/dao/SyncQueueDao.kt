package com.alvaronolasco.creditcardtracker.data.dao

import androidx.room.*
import com.alvaronolasco.creditcardtracker.data.entity.SyncQueueItem

@Dao
interface SyncQueueDao {

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAll(): List<SyncQueueItem>

    @Query("""
        INSERT OR REPLACE INTO sync_queue (entityType, entityId, action, attemptCount, lastAttempt, createdAt)
        VALUES (:entityType, :entityId, :action, 0, 0, :createdAt)
    """)
    suspend fun enqueue(
        entityType: String,
        entityId: Int,
        action: String,
        createdAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE sync_queue SET attemptCount = attemptCount + 1, lastAttempt = :now WHERE id = :id")
    suspend fun incrementAttempt(id: Int, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun dequeue(item: SyncQueueItem)

    @Query("DELETE FROM sync_queue WHERE entityType = :type AND entityId = :id")
    suspend fun removeByEntity(type: String, id: Int)

    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}
