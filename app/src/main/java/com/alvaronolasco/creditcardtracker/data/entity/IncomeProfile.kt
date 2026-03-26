package com.alvaronolasco.creditcardtracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_profiles")
data class IncomeProfile(
    @PrimaryKey val id: Int = 1, // Singleton
    val employmentType: String, // "EMPLOYED", "FREELANCER"
    val incomeMode: String, // "RECURRING", "MONTHLY_PROMPT"
    val createdAt: Long = System.currentTimeMillis()
)
