package com.alvaronolasco.creditcardtracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
data class CreditCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val bank: String,
    val lastFourDigits: String,
    val color: Int, // Hex color as Int
    val cutOffDay: Int, // 1-31
    val paymentDueDay: Int, // 1-31
    val creditLimit: Double,
    val extraFinancingPayment: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastPaymentDate: Long = 0L,
    val partialPaymentAmount: Double = 0.0,
    val partialPaymentCycleEnd: Long = 0L,
    val bankId: String? = null
)
