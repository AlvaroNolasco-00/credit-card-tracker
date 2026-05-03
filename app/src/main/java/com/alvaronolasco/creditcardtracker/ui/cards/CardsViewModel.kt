package com.alvaronolasco.creditcardtracker.ui.cards

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaronolasco.creditcardtracker.data.entity.CreditCard
import com.alvaronolasco.creditcardtracker.data.entity.NotificationConfig
import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository
import com.alvaronolasco.creditcardtracker.notifications.ReminderScheduler
import com.alvaronolasco.creditcardtracker.widget.CreditCardWidgetReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardsUiState(
    val editingCard: CreditCard? = null,
    val notificationConfigs: List<NotificationConfig> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class CardsViewModel @Inject constructor(
    private val repository: CreditCardRepository,
    private val scheduler: ReminderScheduler,
    @ApplicationContext @Suppress("StaticFieldLeak") private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardsUiState())
    val uiState: StateFlow<CardsUiState> = _uiState.asStateFlow()

    fun loadCard(cardId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val card = repository.getCardById(cardId)
            val configs = repository.getConfigsByCard(cardId).first()
            _uiState.update { it.copy(editingCard = card, notificationConfigs = configs, isLoading = false) }
        }
    }

    fun saveCard(
        name: String,
        bank: String,
        bankId: String? = null,
        lastFour: String,
        color: Int,
        cutOff: Int,
        payment: Int,
        limit: Double,
        extraFinancingPayment: Double = 0.0,
        existingCardId: Int? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val savedId: Int
            val card: CreditCard
            if (existingCardId != null) {
                val existing = repository.getCardById(existingCardId)
                card = (existing ?: CreditCard(
                    id = existingCardId,
                    name = name,
                    bank = bank,
                    bankId = bankId,
                    lastFourDigits = lastFour,
                    color = color,
                    cutOffDay = cutOff,
                    paymentDueDay = payment,
                    creditLimit = limit,
                    extraFinancingPayment = extraFinancingPayment
                )).copy(
                    name = name,
                    bank = bank,
                    bankId = bankId,
                    lastFourDigits = lastFour,
                    color = color,
                    cutOffDay = cutOff,
                    paymentDueDay = payment,
                    creditLimit = limit,
                    extraFinancingPayment = extraFinancingPayment
                )
                repository.updateCard(card)
                savedId = existingCardId
            } else {
                card = CreditCard(
                    id = 0,
                    name = name,
                    bank = bank,
                    bankId = bankId,
                    lastFourDigits = lastFour,
                    color = color,
                    cutOffDay = cutOff,
                    paymentDueDay = payment,
                    creditLimit = limit,
                    extraFinancingPayment = extraFinancingPayment
                )
                savedId = repository.insertCard(card)
            }
            val configs = repository.getConfigsByCard(savedId).first()
            scheduler.scheduleReminders(card.copy(id = savedId), configs)
            CreditCardWidgetReceiver.updateAllWidgets(context)
            onSuccess()
        }
    }

    fun deleteCard(cardId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            if (card != null) {
                scheduler.cancelReminders(card)
                repository.deleteCard(card)
                CreditCardWidgetReceiver.updateAllWidgets(context)
                onSuccess()
            }
        }
    }

    fun toggleNotificationConfig(config: NotificationConfig) {
        viewModelScope.launch {
            val updated = config.copy(enabled = !config.enabled)
            repository.updateConfig(updated)
            _uiState.update { state ->
                state.copy(
                    notificationConfigs = state.notificationConfigs.map {
                        if (it.id == updated.id) updated else it
                    }
                )
            }
            // Re-schedule with updated configs
            val card = _uiState.value.editingCard ?: return@launch
            val allConfigs = _uiState.value.notificationConfigs.map {
                if (it.id == updated.id) updated else it
            }
            scheduler.scheduleReminders(card, allConfigs)
        }
    }
}
