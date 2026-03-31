package com.alvaronolasco.creditcardtracker.data.entity

data class CategorySpending(
    val id: Int,
    val name: String,
    val icon: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val totalSpent: Double
)
