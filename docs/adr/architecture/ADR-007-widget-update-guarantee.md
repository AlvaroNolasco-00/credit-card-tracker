# ADR-007: Garantizar actualización del widget tras cambios de gastos

**Fecha:** 2026-03-26
**Estado:** Aceptado
**Categoría:** architecture

## Contexto

Cuando el usuario guardaba o eliminaba un gasto, el widget a veces no se actualizaba inmediatamente. El método `CreditCardWidgetReceiver.updateAllWidgets()` era asincrónico (fire-and-forget con `CoroutineScope(Dispatchers.IO).launch`), y si el usuario navegaba hacia atrás rápidamente, la coroutine podría no ejecutarse antes de que la Activity fuera destruida.

## Decisión

Cambiar `updateAllWidgets()` de una función **asincrónica fire-and-forget** a una función **suspend** que se espera completar:

**Antes:**
```kotlin
fun updateAllWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    CoroutineScope(Dispatchers.IO).launch {  // Fire-and-forget
        manager.getGlanceIds(...).forEach { id ->
            CreditCardWidget().update(context, id)
        }
    }
}
```

**Después:**
```kotlin
suspend fun updateAllWidgets(context: Context) {
    try {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(...).forEach { id ->
            CreditCardWidget().update(context, id)
        }
    } catch (e: Exception) {
        // Silently catch widget update errors
    }
}
```

En `ExpensesViewModel`, se llama directamente dentro de `viewModelScope.launch`:

```kotlin
fun saveExpense(...) {
    viewModelScope.launch {
        // ... guardar gasto ...
        CreditCardWidgetReceiver.updateAllWidgets(context)  // Espera a que complete
        onSuccess()  // Navega DESPUÉS de que el widget se actualice
    }
}
```

## Consecuencias

- El widget **siempre** se actualiza después de guardar/eliminar un gasto.
- La navegación hacia atrás espera a que complete la actualización del widget.
- Si la actualización falla (excepción), se captura silenciosamente para no interrumpir el flujo de guardado.
- El `try/catch` garantiza que `onSuccess()` se ejecute incluso si el widget update falla (el guardado del gasto ya sucedió).
