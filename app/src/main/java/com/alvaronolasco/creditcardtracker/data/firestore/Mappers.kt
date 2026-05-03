package com.alvaronolasco.creditcardtracker.data.firestore

import com.alvaronolasco.creditcardtracker.data.entity.*

fun CreditCard.toFirestoreMap(uid: String): Map<String, Any?> = mapOf(
    "userId" to uid,
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
    "bankId" to bankId,
    "updatedAt" to updatedAt
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toCard(): CreditCard? = runCatching {
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
        bankId = get("bankId") as? String,
        updatedAt = get("updatedAt") as? Long ?: System.currentTimeMillis()
    )
}.getOrNull()

fun Expense.toFirestoreMap(uid: String, categoryIds: List<Int> = emptyList()): Map<String, Any?> = mapOf(
    "userId" to uid,
    "id" to id,
    "cardId" to cardId,
    "amount" to amount,
    "description" to description,
    "receiptImagePath" to receiptImagePath,
    "ocrRawText" to ocrRawText,
    "date" to date,
    "createdAt" to createdAt,
    "msiMonths" to msiMonths,
    "msiMonthlyAmount" to msiMonthlyAmount,
    "msiEndDate" to msiEndDate,
    "paymentMethod" to paymentMethod,
    "categoryIds" to categoryIds,
    "updatedAt" to updatedAt
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toExpenseWithCatIds(): Pair<Expense, List<Int>>? = runCatching {
    val expense = Expense(
        id = (get("id") as? Long)?.toInt() ?: return@runCatching null,
        cardId = (get("cardId") as? Long)?.toInt(),
        amount = (get("amount") as? Number)?.toDouble() ?: return@runCatching null,
        description = get("description") as? String ?: "",
        receiptImagePath = get("receiptImagePath") as? String,
        ocrRawText = get("ocrRawText") as? String,
        date = get("date") as? Long ?: return@runCatching null,
        createdAt = get("createdAt") as? Long ?: System.currentTimeMillis(),
        msiMonths = (get("msiMonths") as? Long)?.toInt() ?: 1,
        msiMonthlyAmount = (get("msiMonthlyAmount") as? Number)?.toDouble() ?: 0.0,
        msiEndDate = get("msiEndDate") as? Long ?: 0L,
        paymentMethod = get("paymentMethod") as? String ?: "CREDIT_CARD",
        updatedAt = get("updatedAt") as? Long ?: System.currentTimeMillis()
    )
    val catIds = (get("categoryIds") as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()
    Pair(expense, catIds)
}.getOrNull()

fun Category.toFirestoreMap(uid: String): Map<String, Any?> = mapOf(
    "userId" to uid,
    "id" to id,
    "name" to name,
    "icon" to icon,
    "isDefault" to isDefault,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toCategory(): Category? = runCatching {
    Category(
        id = (get("id") as? Long)?.toInt() ?: return@runCatching null,
        name = get("name") as? String ?: return@runCatching null,
        icon = get("icon") as? String ?: "",
        isDefault = get("isDefault") as? Boolean ?: false,
        createdAt = get("createdAt") as? Long ?: System.currentTimeMillis(),
        updatedAt = get("updatedAt") as? Long ?: System.currentTimeMillis()
    )
}.getOrNull()

fun BudgetItem.toFirestoreMap(uid: String): Map<String, Any?> = mapOf(
    "userId" to uid,
    "id" to id,
    "categoryId" to categoryId,
    "monthYear" to monthYear,
    "limitAmount" to limitAmount,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toBudgetItem(): BudgetItem? = runCatching {
    BudgetItem(
        id = (get("id") as? Long)?.toInt() ?: return@runCatching null,
        categoryId = (get("categoryId") as? Long)?.toInt() ?: return@runCatching null,
        monthYear = get("monthYear") as? String ?: return@runCatching null,
        limitAmount = (get("limitAmount") as? Number)?.toDouble() ?: return@runCatching null,
        createdAt = get("createdAt") as? Long ?: System.currentTimeMillis(),
        updatedAt = get("updatedAt") as? Long ?: System.currentTimeMillis()
    )
}.getOrNull()

fun IncomeEntry.toFirestoreMap(uid: String): Map<String, Any?> = mapOf(
    "userId" to uid,
    "id" to id,
    "label" to label,
    "amount" to amount,
    "dayOfMonth" to dayOfMonth,
    "isRecurring" to isRecurring,
    "type" to type,
    "monthYear" to monthYear,
    "isActive" to isActive,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toIncomeEntry(): IncomeEntry? = runCatching {
    IncomeEntry(
        id = (get("id") as? Long)?.toInt() ?: return@runCatching null,
        label = get("label") as? String ?: return@runCatching null,
        amount = (get("amount") as? Number)?.toDouble() ?: return@runCatching null,
        dayOfMonth = (get("dayOfMonth") as? Long)?.toInt() ?: return@runCatching null,
        isRecurring = get("isRecurring") as? Boolean ?: false,
        type = get("type") as? String ?: "OTHER",
        monthYear = get("monthYear") as? String,
        isActive = get("isActive") as? Boolean ?: true,
        createdAt = get("createdAt") as? Long ?: System.currentTimeMillis(),
        updatedAt = get("updatedAt") as? Long ?: System.currentTimeMillis()
    )
}.getOrNull()

fun IncomeProfile.toFirestoreMap(uid: String): Map<String, Any?> = mapOf(
    "userId" to uid,
    "id" to id,
    "employmentType" to employmentType,
    "incomeMode" to incomeMode,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toIncomeProfile(): IncomeProfile? = runCatching {
    IncomeProfile(
        id = (get("id") as? Long)?.toInt() ?: 1,
        employmentType = get("employmentType") as? String ?: return@runCatching null,
        incomeMode = get("incomeMode") as? String ?: return@runCatching null,
        createdAt = get("createdAt") as? Long ?: System.currentTimeMillis(),
        updatedAt = get("updatedAt") as? Long ?: System.currentTimeMillis()
    )
}.getOrNull()

fun RecurringExpense.toFirestoreMap(uid: String, categoryIds: List<Int> = emptyList()): Map<String, Any?> = mapOf(
    "userId" to uid,
    "id" to id,
    "cardId" to cardId,
    "amount" to amount,
    "description" to description,
    "dayOfMonth" to dayOfMonth,
    "isActive" to isActive,
    "createdAt" to createdAt,
    "categoryIds" to categoryIds,
    "updatedAt" to updatedAt
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toRecurringExpenseWithCatIds(): Pair<RecurringExpense, List<Int>>? = runCatching {
    val rec = RecurringExpense(
        id = (get("id") as? Long)?.toInt() ?: return@runCatching null,
        cardId = (get("cardId") as? Long)?.toInt() ?: return@runCatching null,
        amount = (get("amount") as? Number)?.toDouble() ?: return@runCatching null,
        description = get("description") as? String ?: "",
        dayOfMonth = (get("dayOfMonth") as? Long)?.toInt(),
        isActive = get("isActive") as? Boolean ?: true,
        createdAt = get("createdAt") as? Long ?: System.currentTimeMillis(),
        updatedAt = get("updatedAt") as? Long ?: System.currentTimeMillis()
    )
    val catIds = (get("categoryIds") as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()
    Pair(rec, catIds)
}.getOrNull()

fun NotificationConfig.toFirestoreMap(uid: String): Map<String, Any?> = mapOf(
    "userId" to uid,
    "id" to id,
    "cardId" to cardId,
    "type" to type,
    "daysBefore" to daysBefore,
    "enabled" to enabled,
    "updatedAt" to updatedAt
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toNotificationConfig(): NotificationConfig? = runCatching {
    NotificationConfig(
        id = (get("id") as? Long)?.toInt() ?: return@runCatching null,
        cardId = (get("cardId") as? Long)?.toInt() ?: return@runCatching null,
        type = get("type") as? String ?: return@runCatching null,
        daysBefore = (get("daysBefore") as? Long)?.toInt() ?: return@runCatching null,
        enabled = get("enabled") as? Boolean ?: true,
        updatedAt = get("updatedAt") as? Long ?: System.currentTimeMillis()
    )
}.getOrNull()
