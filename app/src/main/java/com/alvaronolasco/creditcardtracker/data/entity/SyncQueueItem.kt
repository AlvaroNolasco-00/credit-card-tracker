package com.alvaronolasco.creditcardtracker.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_queue",
    indices = [Index(value = ["entityType", "entityId"])]
)
data class SyncQueueItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityType: String,
    val entityId: Int,
    val action: String,
    val attemptCount: Int = 0,
    val lastAttempt: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
