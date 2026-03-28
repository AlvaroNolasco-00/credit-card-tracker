# ADR-020: Actualización de widget via broadcast en lugar de corrutinas

**Fecha:** 2026-03-28
**Estado:** Aceptado
**Categoría:** architecture

## Contexto

En ADR-007 se intentó garantizar la actualización del widget haciendo que `CreditCardWidgetReceiver.updateAllWidgets()` fuera una función `suspend` que esperaba a que `GlanceAppWidget.update()` completara. Sin embargo, el widget seguía sin actualizarse inmediatamente después de guardar o eliminar gastos — la actualización ocurría después de algunos minutos o solo cuando se quitaba y re-agregaba el widget al home.

**Raíz del problema:** Glance 1.0.0 tiene un comportamiento conocido donde `GlanceAppWidget.update()`, aunque es `suspend`, solo espera a que el trabajo se **encole** en el scope interno de Glance, no a que se **complete**. Esto significa:

1. El ViewModel llama `updateAllWidgets(context)` en `viewModelScope.launch { }`
2. El trabajo se encola en el scope interno de Glance
3. La función `suspend` retorna
4. `onSuccess()` dispara navegación hacia atrás
5. El ViewModel se limpia y `viewModelScope` se cancela
6. El trabajo encolado de Glance se demora o es deprioritizado por Android

## Decisión

Cambiar `updateAllWidgets()` de una función `suspend` que llama `GlanceAppWidget.update()` directamente, a una función regular que dispara un broadcast `ACTION_APPWIDGET_UPDATE` a `CreditCardWidgetReceiver`.

**Cambio:**
```kotlin
// Antes (ADR-007)
suspend fun updateAllWidgets(context: Context) {
    try {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(CreditCardWidget::class.java).forEach { id ->
            CreditCardWidget().update(context, id)
        }
    } catch (e: Exception) { }
}

// Ahora (ADR-020)
fun updateAllWidgets(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val ids = appWidgetManager.getAppWidgetIds(
        ComponentName(context, CreditCardWidgetReceiver::class.java)
    )
    if (ids.isNotEmpty()) {
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
            component = ComponentName(context, CreditCardWidgetReceiver::class.java)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }
}
```

Los callers en ViewModels (`ExpensesViewModel`, `CardsViewModel`, `IncomeViewModel`) pueden seguir llamando esta función sin cambios, ya que aunque ahora es regular (no `suspend`), se llama desde dentro de `viewModelScope.launch { }`, lo cual es compatible.

## Consecuencias

### ✅ Ventajas

1. **Actualización inmediata:** El broadcast es entregado de forma sincrónica (en milisegundos) a `CreditCardWidgetReceiver`, que maneja la actualización en su propio lifecycle independiente del ViewModel.

2. **Ciclo de vida independiente:** `GlanceAppWidgetReceiver.onReceive()` usa `goAsync()` internamente, lo que le permite manejar corrutinas sin estar atado al lifecycle del ViewModel.

3. **Android-idiomatic:** Los broadcasts para actualizar widgets son el patrón estándar de Android. `GlanceAppWidgetReceiver` está diseñado específicamente para recibir `ACTION_APPWIDGET_UPDATE`.

4. **Fire-and-forget seguro:** No necesita ser `suspend` porque el BroadcastReceiver maneja la actualización asincrónica de forma confiable.

### ⚠️ Trade-offs

1. **No se puede esperar:** Ya no podemos bloquear hasta que la actualización complete. Sin embargo, esto es aceptable porque:
   - El widget no necesita actualizar sincronamente con la UI
   - El usuario navega hacia atrás inmediatamente después de `onSuccess()`
   - La actualización ocurre "en background" y es visible cuando el usuario vuelve al home

## Referencias

- **ADR-007:** Intento previo de garantizar actualización del widget (usando corrutinas) — Supersedido por esta decisión
- Glance 1.0.0 documentation: `GlanceAppWidget.update()` internals
- Android AppWidget documentation: `AppWidgetManager.ACTION_APPWIDGET_UPDATE`
