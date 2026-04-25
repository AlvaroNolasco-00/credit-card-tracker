package com.alvaronolasco.creditcardtracker.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class RecurringExpenseWithCategories(
    @Embedded val recurringExpense: RecurringExpense,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RecurringExpenseCategory::class,
            parentColumn = "recurringExpenseId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<Category>
)
