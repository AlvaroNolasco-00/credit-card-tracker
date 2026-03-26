package com.alvaronolasco.creditcardtracker.widget

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object WidgetDeepLink {
    private val _pendingCardId = MutableStateFlow<Int?>(null)
    val pendingCardId = _pendingCardId.asStateFlow()

    fun navigate(cardId: Int) {
        _pendingCardId.value = cardId
    }

    fun consume() {
        _pendingCardId.value = null
    }
}
