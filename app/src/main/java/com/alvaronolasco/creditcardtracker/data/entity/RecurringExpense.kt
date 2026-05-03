package com.alvaronolasco.creditcardtracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_expenses",
    foreignKeys = [
        ForeignKey(
            entity = CreditCard::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("cardId")]
)
data class RecurringExpense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val amount: Double,
    val description: String,
    val dayOfMonth: Int? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
