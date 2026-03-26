package com.alvaronolasco.creditcardtracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_entries")
data class IncomeEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val amount: Double,
    val dayOfMonth: Int,
    val isRecurring: Boolean,
    val type: String, // "BASE", "COMMISSION", "BONUS", "OTHER"
    val monthYear: String? = null, // "YYYY-MM" for non-recurring
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
