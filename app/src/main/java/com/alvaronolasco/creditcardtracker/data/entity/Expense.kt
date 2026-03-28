package com.alvaronolasco.creditcardtracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CreditCard::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val amount: Double,
    val description: String,
    val receiptImagePath: String? = null,
    val ocrRawText: String? = null,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val msiMonths: Int = 1,
    val msiMonthlyAmount: Double = 0.0,
    val msiEndDate: Long = 0L
)
