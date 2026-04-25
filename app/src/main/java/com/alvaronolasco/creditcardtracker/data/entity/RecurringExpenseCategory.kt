package com.alvaronolasco.creditcardtracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "recurring_expense_categories",
    primaryKeys = ["recurringExpenseId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = RecurringExpense::class,
            parentColumns = ["id"],
            childColumns = ["recurringExpenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recurringExpenseId"), Index("categoryId")]
)
data class RecurringExpenseCategory(
    val recurringExpenseId: Int,
    val categoryId: Int
)
