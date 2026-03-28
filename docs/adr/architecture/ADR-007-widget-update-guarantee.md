# ADR-007: Garantizar actualización del widget tras cambios de gastos

**Fecha:** 2026-03-26
**Estado:** Supersedido por ADR-020
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

## Nota

**Esta decisión fue supersedida por ADR-020** después de descubrir que en Glance 1.0.0, `GlanceAppWidget.update()` aunque es `suspend`, solo espera hasta que el trabajo se encola (no hasta que se completa). Cuando el usuario navega hacia atrás rapidamente tras guardar un gasto, el trabajo encolado se demora o es deprioritizado por Android. La solución de ADR-020 usa broadcasts (`ACTION_APPWIDGET_UPDATE`) que se manejan en el lifecycle independiente del `GlanceAppWidgetReceiver`, garantizando actualización inmediata.
