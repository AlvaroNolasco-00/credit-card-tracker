package com.alvaronolasco.creditcardtracker.data.firestore

import com.alvaronolasco.creditcardtracker.data.entity.Category
import com.alvaronolasco.creditcardtracker.data.entity.CreditCard
import com.alvaronolasco.creditcardtracker.data.entity.Expense
import com.alvaronolasco.creditcardtracker.data.entity.ExpenseCategory

fun CreditCard.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "bank" to bank,
    "lastFourDigits" to lastFourDigits,
    "color" to color,
    "cutOffDay" to cutOffDay,
    "paymentDueDay" to paymentDueDay,
    "creditLimit" to creditLimit,
    "extraFinancingPayment" to extraFinancingPayment,
    "createdAt" to createdAt,
    "lastPaymentDate" to lastPaymentDate,
    "partialPaymentAmount" to partialPaymentAmount,
    "partialPaymentCycleEnd" to partialPaymentCycleEnd,
    "bankId" to bankId
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toCreditCard(): CreditCard? = runCatching {
    CreditCard(
        id = (get("id") as? Long)?.toInt() ?: return@runCatching null,
        name = get("name") as? String ?: return@runCatching null,
        bank = get("bank") as? String ?: "",
        lastFourDigits = get("lastFourDigits") as? String ?: "",
        color = (get("color") as? Long)?.toInt() ?: 0,
        cutOffDay = (get("cutOffDay") as? Long)?.toInt() ?: 1,
        paymentDueDay = (get("paymentDueDay") as? Long)?.toInt() ?: 1,
        creditLimit = (get("creditLimit") as? Number)?.toDouble() ?: 0.0,
        extraFinancingPayment = (get("extraFinancingPayment") as? Number)?.toDouble() ?: 0.0,
        createdAt = get("createdAt") as? Long ?: System.currentTimeMillis(),
        lastPaymentDate = get("lastPaymentDate") as? Long ?: 0L,
        partialPaymentAmount = (get("partialPaymentAmount") as? Number)?.toDouble() ?: 0.0,
        partialPaymentCycleEnd = get("partialPaymentCycleEnd") as? Long ?: 0L,
        bankId = get("bankId") as? String
    )
}.getOrNull()

fun Expense.toFirestoreMap(categoryIds: List<Int> = emptyList()): Map<String, Any?> = mapOf(
    "id" to id,
    "cardId" to cardId,
    "amount" to amount,
    "description" to description,
    "date" to date,
    "createdAt" to createdAt,
    "msiMonths" to msiMonths,
    "msiMonthlyAmount" to msiMonthlyAmount,
    "msiEndDate" to msiEndDate,
    "paymentMethod" to paymentMethod,
    "categoryIds" to categoryIds
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toExpense(): Expense? = runCatching {
    Expense(
        id = (get("id") as? Long)?.toInt() ?: return@runCatching null,
        cardId = (get("cardId") as? Long)?.toInt(),
        amount = (get("amount") as? Number)?.toDouble() ?: return@runCatching null,
        description = get("description") as? String ?: "",
        date = get("date") as? Long ?: return@runCatching null,
        createdAt = get("createdAt") as? Long ?: System.currentTimeMillis(),
        msiMonths = (get("msiMonths") as? Long)?.toInt() ?: 1,
        msiMonthlyAmount = (get("msiMonthlyAmount") as? Number)?.toDouble() ?: 0.0,
        msiEndDate = get("msiEndDate") as? Long ?: 0L,
        paymentMethod = get("paymentMethod") as? String ?: "CREDIT_CARD"
    )
}.getOrNull()

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toCategoryIds(): List<Int> =
    (get("categoryIds") as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()

fun Category.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "icon" to icon,
    "isDefault" to isDefault,
    "createdAt" to createdAt
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toCategory(): Category? = runCatching {
    Category(
        id = (get("id") as? Long)?.toInt() ?: return@runCatching null,
        name = get("name") as? String ?: return@runCatching null,
        icon = get("icon") as? String ?: "",
        isDefault = get("isDefault") as? Boolean ?: false,
        createdAt = get("createdAt") as? Long ?: System.currentTimeMillis()
    )
}.getOrNull()
