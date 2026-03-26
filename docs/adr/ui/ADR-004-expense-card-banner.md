# ADR-004: Banner visual de tarjeta destino en AddExpenseScreen

**Fecha:** 2026-03-25
**Estado:** Aceptado
**Categoría:** ui

## Contexto

`AddExpenseScreen` no mostraba ninguna información visual sobre a qué tarjeta se estaba asociando el gasto. El usuario debía recordar desde qué tarjeta había navegado o leer el título genérico "Agregar Gasto".

Esto era especialmente confuso al llegar desde el widget (ADR-002), donde el contexto de la tarjeta se podía perder visualmente.

## Decisión

Agregar un composable `CardTargetBanner(card: CreditCard)` al tope del formulario (antes del campo Monto), que muestra:
- Fondo degradado horizontal: `Color(card.color)` → 65% oscuro del mismo color
- Fila superior: banco (10sp, blanco 70% opacidad) + dígitos "**** XXXX" (10sp, blanco 70%)
- Fila inferior: nombre de la tarjeta (15sp, bold, blanco)
- Altura fija: 72dp, `cornerRadius 16dp`

### Cambios en la capa de datos/VM

**`ExpensesUiState`**: nuevo campo `currentCard: CreditCard? = null`

**`ExpensesViewModel`**: nueva función:
```kotlin
fun loadCard(cardId: Int) {
    viewModelScope.launch {
        val card = repository.getCardById(cardId)
        _uiState.update { it.copy(currentCard = card) }
    }
}
```

**`AddExpenseScreen`**:
- `LaunchedEffect(cardId)` → llama `loadCard(cardId)` en modo nuevo gasto
- `LaunchedEffect(uiState.currentExpense)` → llama `loadCard(ewc.expense.cardId)` en modo edición
- El banner se muestra con `uiState.currentCard?.let { CardTargetBanner(it) }`; si aún no ha cargado, simplemente no se renderiza (no hay estado de loading visible).

### Regla de imports aplicada

`card.color` es `Int` (hex ARGB almacenado por Room). Para usarlo en Compose se necesita:
```kotlin
import androidx.compose.ui.graphics.Color       // Color(Int)
import androidx.compose.ui.graphics.Brush       // Brush.horizontalGradient
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import com.alvaronolasco.creditcardtracker.data.entity.CreditCard
```
Todos importados explícitamente; ninguno viene de un wildcard preexistente.

## Consecuencias

- El formulario ahora siempre tiene contexto visual de la tarjeta destino tanto en modo nuevo como edición.
- El banner carga de forma asíncrona (suspend `getCardById`); existe una ventana de ~1 frame donde no se muestra. Aceptable ya que el formulario en sí también tarda un frame en renderizarse.
- Si en el futuro se agrega un selector de tarjeta dentro del formulario, el banner deberá actualizarse en tiempo real junto con ese selector.
