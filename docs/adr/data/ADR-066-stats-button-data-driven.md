# ADR-066: Botón de estadísticas basado en datos reales en lugar de heurísticas de tiempo

**Fecha:** 2026-05-02
**Estado:** Aceptado
**Categoría:** data
**Prioridad:** High
**Afecta:** `ExpenseDao`, `CreditCardRepository`, `DashboardViewModel`

---

## Contexto

El botón de estadísticas en el dashboard (`DashboardScreen.kt:586`) se mostraba condicionalmente según `hasStatsAvailable`, computado por `checkStatsAvailability(card: CreditCard)` en `DashboardViewModel.kt:308`.

Esta función usaba dos heurísticas de tiempo:

1. **`isOlderThanAMonth`**: `(System.currentTimeMillis() - card.createdAt) >= 30 días`  
   — **Stale**: `System.currentTimeMillis()` se evalúa solo cuando el Flow de Room emite. Si no hay cambios en la tabla `credit_cards`, el valor nunca se recalcula aunque pase el tiempo.

2. **`cutOffPassed`**: `DateUtils.hasCutOffPassedThisMonth(card.cutOffDay)`  
   — **Incompleto**: solo verifica si el corte de *este mes* ya pasó. Una tarjeta con corte día 25 un 2 de mayo retorna `false`, aunque tenga periodos cerrados en marzo y abril con datos.

Ambos bugs combinados causaban que `hasStatsAvailable` fuera `false` para tarjetas que sí tenían datos históricos (múltiples periodos cerrados), ocultando incorrectamente el botón de estadísticas.

Se consideraron tres alternativas:
1. Corregir solo `hasCutOffPassedThisMonth` para que mire periodos históricos
2. Agregar un ticker periódico para refrescar `System.currentTimeMillis()`
3. Reemplazar las heurísticas por una consulta real a la base de datos

---

## Decisión

### Opción elegida

**Verificar disponibilidad de estadísticas consultando datos reales en Room** mediante una query `hasExpenses(cardId)` que retorna `Flow<Boolean>`.

```sql
SELECT COUNT(*) > 0 FROM expenses WHERE cardId = :cardId
```

Este Flow se integra en el `combine` existente del `DashboardViewModel`, reemplazando completamente `checkStatsAvailability`.

### Por qué esta opción

- **Reactividad garantizada**: Room Flow re-emite automáticamente cuando se inserta/elimina/modifica cualquier expense de la tarjeta. El botón aparece/desaparece en tiempo real sin lógica adicional.
- **Fuente de verdad correcta**: si hay gastos → hay datos para estadísticas → mostrar botón. Lógica trivial y sin edge cases de tiempo.
- **Cero cambios para Firestore**: cuando se sincronice Firestore → Room en el futuro, los gastos syncados se escribirán en Room, el Flow re-emitirá, y `hasStatsAvailable` se actualizará automáticamente.
- **Elimina código muerto**: `checkStatsAvailability(card)` completo (~9 líneas) se elimina.

### Opciones rechazadas

**Opción A: Arreglar `hasCutOffPassedThisMonth` para mirar periodos históricos**
- ❌ Sigue dependiendo de heurísticas de fecha en lugar de datos reales
- ❌ No resuelve el problema de `System.currentTimeMillis()` stale
- ❌ Edge cases: tarjeta con corte día 31 en febrero, cambios de zona horaria, etc.

**Opción B: Agregar ticker periódico (ej. cada 60s) para refrescar el estado**
- ❌ Complejidad innecesaria: agrega un `flow { while(true) { emit(Unit); delay(60_000) } }` al combine
- ❌ Latencia: el botón puede tardar hasta 60s en aparecer después de que se cumpla la condición
- ❌ Gasto de recursos: evaluación periódica sin necesidad real

---

## Consecuencias

### Directas

- ✅ El botón de estadísticas aparece inmediatamente cuando una tarjeta tiene gastos, independientemente de fechas de corte o antigüedad
- ✅ El botón desaparece si se eliminan todos los gastos de una tarjeta (consistencia total)
- ✅ Compatible con sincronización Firestore futura sin cambios adicionales
- ⚠️ `checkStatsAvailability` se elimina; si otro código lo referenciaba, romperá en compilación

### Técnicas

**Archivos/módulos impactados:**

| Archivo | Cambio |
|---------|--------|
| `ExpenseDao.kt` | Nueva query `hasExpenses(cardId): Flow<Boolean>` |
| `CreditCardRepository.kt` | Nuevo método `hasExpenses(cardId)` delegando al DAO |
| `DashboardViewModel.kt` | `combine(currentFlow, prevFlow)` → `combine(currentFlow, prevFlow, hasExpensesFlow)`; se elimina `checkStatsAvailability()` |
| `docs/adr/data/ADR-066-*.md` | Este documento |

**Breaking changes:** Ninguno. La API pública de `CardDashboardState` no cambia (el campo `hasStatsAvailable` sigue existiendo, solo cambia cómo se computa).

### Operacionales

- Testing: `./gradlew test` (unit tests existentes)
- Testing manual: verificar que el botón aparece/desaparece al agregar/eliminar gastos
- Documentación: actualizar `CHANGELOG.md` y `docs/adr/INDEX.md`

---

## Implementación

### Paso a paso

1. Agregar `hasExpenses` query en `ExpenseDao`
2. Agregar `hasExpenses` wrapper en `CreditCardRepository`
3. Integrar `hasExpensesFlow` en el combine de `DashboardViewModel.loadDashboard()`
4. Reemplazar `checkStatsAvailability(card)` por el valor del flow
5. Eliminar método privado `checkStatsAvailability`
6. Crear ADR-066
7. Actualizar `INDEX.md` y `CHANGELOG.md`
8. Ejecutar `./gradlew build test` para verificar

---

## Validación

- [ ] `./gradlew build test` pasa sin errores
- [ ] Tarjeta con gastos → botón de estadísticas visible en el dashboard
- [ ] Tarjeta sin gastos → botón oculto
- [ ] Agregar primer gasto a tarjeta sin gastos → botón aparece (reactividad Flow)
- [ ] Eliminar todos los gastos → botón desaparece
- [ ] El botón navega correctamente a `CardStatsScreen`

---

## Notas y Aprendizajes

- Las heurísticas de tiempo (`System.currentTimeMillis()`) dentro de Flows son frágiles porque dependen de la frecuencia de emisión del Flow
- Una query `SELECT COUNT(*) > 0` en Room es O(1) con índice en `cardId` — no tiene impacto de performance
- El patrón de "consultar datos reales en lugar de adivinar con heurísticas" es aplicable a otros features similares

---

## Referencias

- [ADR-058](ui/ADR-058-dark-mode-system-detection.md) — Patrón `isSystemInDarkTheme()` reutilizado en componentes del dashboard
- [ADR-062](architecture/ADR-062-firebase-auth-ui-and-sync.md) — Sincronización Firestore futura compatible con este cambio
