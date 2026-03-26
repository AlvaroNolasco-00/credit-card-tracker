package com.alvaronolasco.creditcardtracker.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ExpenseWithCategories(
    @Embedded val expense: Expense,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ExpenseCategory::class,
            parentColumn = "expenseId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<Category>
)
