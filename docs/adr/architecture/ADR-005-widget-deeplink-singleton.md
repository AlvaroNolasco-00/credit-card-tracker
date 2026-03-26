# ADR-005: WidgetDeepLink — singleton StateFlow para deep linking widget → app

**Fecha:** 2026-03-25
**Estado:** Aceptado
**Categoría:** architecture

## Contexto

Al tocar el botón "+" en el widget (ADR-002), `MainActivity` recibe un `Intent` con `"card_id"` como extra. Necesitábamos un mecanismo para que `Navigation()` (composable) reaccionara a ese intent y navegara a `add_expense/{cardId}`, **incluso si la Activity ya estaba corriendo** (caso `onNewIntent`).

Las alternativas evaluadas:

| Opción | Problema |
|--------|----------|
| Pasar `initialCardId` como parámetro a `Navigation()` | No reacciona si la Activity ya está en memoria y llega `onNewIntent` |
| Hilt ViewModel compartido | Acoplamiento fuerte entre `MainActivity` y `Navigation`; ViewModel vive en el scope de la Activity, no en el composable |
| `savedStateHandle` en `NavBackStackEntry` | Solo disponible dentro de una ruta activa, no antes de navegar |
| **`object WidgetDeepLink` con `MutableStateFlow`** | ✓ Sobrevive a `onCreate`/`onNewIntent`; observable desde cualquier composable |

## Decisión

Crear `widget/WidgetDeepLink.kt` como singleton Kotlin (`object`):

```kotlin
object WidgetDeepLink {
    private val _pendingCardId = MutableStateFlow<Int?>(null)
    val pendingCardId = _pendingCardId.asStateFlow()
    fun navigate(cardId: Int) { _pendingCardId.value = cardId }
    fun consume() { _pendingCardId.value = null }
}
```

**Flujo completo:**

1. Widget tap "+" → `Intent` con `putExtra("card_id", cardId)` hacia `MainActivity`
2. `MainActivity.onCreate` / `onNewIntent` → `WidgetDeepLink.navigate(cardId)`
3. `Navigation.kt` colecta `pendingCardId` con `collectAsState()`
4. `LaunchedEffect(pendingCardId)` detecta valor no-null → `consume()` + `navController.navigate("add_expense/$cardId")`

**Consumo inmediato** (`consume()` antes de navegar) evita que una recomposición posterior vuelva a disparar la navegación.

## Consecuencias

- El singleton vive mientras el proceso de la app esté vivo. Si el proceso muere y se reinicia por el intent del widget, `onCreate` lo repoblará antes de que `Navigation` se suscriba.
- El patrón "consume-once StateFlow" es equivalente a un `Channel(CONFLATED)` pero más simple para este caso de un solo consumidor.
- **No usar** este mecanismo para deep links que lleguen de fuentes externas (links web, notificaciones); esos deben usar `NavDeepLink` de Jetpack Navigation para mantener el back stack correcto.
- Si en el futuro se agregan múltiples tipos de deep links del widget, considerar migrar a un `sealed class WidgetAction` en el flow.
