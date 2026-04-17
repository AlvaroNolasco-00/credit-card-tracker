package com.alvaronolasco.creditcardtracker.data.entity

enum class PaymentMethod(val label: String) {
    CREDIT_CARD("Tarjeta de crédito"),
    DEBIT_CARD("Tarjeta de débito"),
    TRANSFER("Transferencia"),
    CASH("Efectivo"),
    OTHER("Otro")
}
