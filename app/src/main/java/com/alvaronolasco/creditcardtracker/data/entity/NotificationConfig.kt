package com.alvaronolasco.creditcardtracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_configs",
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
data class NotificationConfig(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val type: String, // "CUT_OFF" or "PAYMENT"
    val daysBefore: Int, // 0, 1, 3, 5, etc.
    val enabled: Boolean = true
)
