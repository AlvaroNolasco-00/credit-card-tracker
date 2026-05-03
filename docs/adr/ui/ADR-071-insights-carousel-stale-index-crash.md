# ADR-071: Fix crash por índice stale en InsightsCarousel al seleccionar período con $0

**Fecha:** 2026-05-03  
**Estado:** Aceptado  
**Categoría:** ui  
**Prioridad:** Critical  
**Afecta:** `ui/stats/CardStatsScreen.kt` — `InsightsCarousel`

---

## Contexto

Al entrar a estadísticas de una tarjeta y tocar un período con $0.00 en gastos, la app crasheaba con:

```
java.lang.IndexOutOfBoundsException: Index: 1, Size: 1
  at CardStatsScreenKt$InsightsCarousel$2$2.invoke(CardStatsScreen.kt:1226)
```

Tras el crash, al reabrir la app, cualquier tap causaba el mismo crash inmediatamente.

**Causa raíz:** Race condition entre Compose recomposition y `LaunchedEffect` en `InsightsCarousel`.

Secuencia:
1. Usuario ve período con 3 insights → auto-scroll lleva `currentIndex` a 1 o 2
2. Usuario toca período con $0 → `generateInsights()` retorna lista con 1 item
3. `AnimatedContent` recompone antes que `LaunchedEffect` ejecute
4. `insights[1]` o `insights[2]` con `size=1` → `IndexOutOfBoundsException`

El `currentIndex` era `remember { mutableIntStateOf(0) }` sin reset al cambiar `insights`. El `LaunchedEffect(insights)` reiniciaba el loop pero **no reseteaba el índice** antes de que Compose recompusiera.

Crash persistía tras reabrir porque Compose recomponía el mismo árbol con el mismo `currentIndex` stale en cada interacción.

---

## Decisión

Triple defensa en `InsightsCarousel`:

1. **Early return** si `insights.isEmpty()` — evita render con lista vacía
2. **Reset `currentIndex = 0`** al inicio de `LaunchedEffect(insights)` — corta el índice stale antes del loop
3. **`coerceIn` + `getOrNull`** — safety net final contra cualquier race condition residual

### Opción elegida

```kotlin
if (insights.isEmpty()) return

var currentIndex by remember { mutableIntStateOf(0) }

LaunchedEffect(insights) {
    currentIndex = 0  // reset antes del loop
    while (insights.size > 1) {
        delay(5000)
        currentIndex = (currentIndex + 1) % insights.size
    }
}

val safeIndex = currentIndex.coerceIn(0, insights.lastIndex)

AnimatedContent(targetState = safeIndex, ...) { index ->
    val insight = insights.getOrNull(index) ?: return@AnimatedContent
```

### Opciones rechazadas

**Solo `getOrNull` sin reset:**
- ❌ Silencia el crash pero muestra insight incorrecto (del período anterior)
- ❌ No resuelve el root cause

**`key(insights) { ... }` para recrear estado:**
- ❌ Recrea todo el composable, pierde animación de transición
- ❌ Más disruptivo que necesario

---

## Consecuencias

### Directas
- ✅ Elimina `IndexOutOfBoundsException` al seleccionar período con $0
- ✅ Elimina crash persistente tras reabrir app
- ✅ El carousel muestra el primer insight del nuevo período inmediatamente
- ⚠️ Al cambiar período, el carousel vuelve al insight 0 (comportamiento esperado)

### Técnicas
**Archivos impactados:**
- `app/src/main/java/com/alvaronolasco/creditcardtracker/ui/stats/CardStatsScreen.kt` — `InsightsCarousel` (~línea 1198)

**Breaking changes:** Ninguno.

### Operacionales
- Testing requerido: manual — seleccionar períodos con y sin gastos, verificar carousel
- Sin migraciones de datos

---

## Validación

- [ ] Seleccionar período con $0 → no crashea, carousel no aparece (empty guard)
- [ ] Seleccionar período con 1 insight → no crashea, se muestra correctamente
- [ ] Seleccionar período con 3 insights, esperar auto-scroll a índice 2, cambiar a período con 1 insight → no crashea
- [ ] Reabrir app tras crash previo → no crash en primer tap

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-05-03 | Documento inicial |

---

## Referencias

- [ADR-057](ADR-057-card-stats-calendar-utc-fix.md) — Bugfix previo en CardStatsScreen
- [ADR-058](ADR-058-chart-colors-dark-mode-contrast.md) — Mejoras previas en CardStatsScreen
