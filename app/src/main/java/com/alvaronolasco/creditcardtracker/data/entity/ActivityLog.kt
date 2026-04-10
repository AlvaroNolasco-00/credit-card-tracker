package com.alvaronolasco.creditcardtracker.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_logs",
    indices = [Index("category"), Index("timestamp")]
)
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val action: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val entityId: Int? = null,
    val entityType: String? = null
)
