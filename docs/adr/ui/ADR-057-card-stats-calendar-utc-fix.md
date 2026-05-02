# ADR-057: Card Stats Calendar Date Bugfix — UTC Timezone Alignment

**Fecha:** 2026-04-26
**Estado:** Aceptado
**Categoría:** ui
**Prioridad:** High
**Afecta:** `CardStatsScreen.kt`, `CardStatsViewModel.kt`

---

## Contexto

Usuarios en zonas horarias con offset negativo (ej. UTC-6) reportaron que al registrar un gasto con fecha 25, el calendario de estadísticas lo marcaba como 24. El root cause era una inconsistencia de zona horaria en la conversión de timestamps a `LocalDate`:

- `DateUtils.getPeriodsRange()` y `DatePicker` de Material3 generan/guardan timestamps en **UTC** (00:00 UTC de la fecha seleccionada).
- `CardStatsViewModel` y `CardStatsScreen` convertían esos timestamps a `LocalDate` usando `ZoneId.systemDefault()`.
- En UTC-6, 25 abril 00:00 UTC = 24 abril 18:00 local → `toLocalDate()` retornaba 24.

Adicionalmente, `expensesByDay` usaba `Map<Int, Double>` (solo `dayOfMonth`), lo que causaba colisiones cuando un período cruzaba dos meses (ej. un gasto del 25 de marzo también marcaba el 25 de abril).

---

## Decisión

### Opción elegida
1. Cambiar todas las conversiones de timestamp → `LocalDate` en `CardStatsViewModel` y `CardStatsScreen` para usar `ZoneOffset.UTC`, alineándose con `DateUtils` y el `DatePicker`.
2. Cambiar el tipo de `expensesByDay` de `Map<Int, Double>` a `Map<LocalDate, Double>` para eliminar ambigüedad de mes.

### Por qué esta opción
- **Consistencia:** Todos los timestamps de fecha en la app (DatePicker, DateUtils) se manejan como UTC. La lectura debe ser simétrica.
- **Precisión:** Elimina el desfase de 1 día en zonas con offset negativo.
- **Corrección de colisión:** `LocalDate` como clave distingue `2025-03-25` de `2025-04-25`.

### Opciones rechazadas
**Opción A: Cambiar DateUtils y DatePicker para usar `ZoneId.systemDefault()`**
- ❌ Requeriría tocar múltiples módulos (`DateUtils`, `AddExpenseScreen`, queries de Room) y podría desfasar datos históricos ya guardados en UTC.

**Opción B: Solo corregir `expensesByDay` a `Map<LocalDate, Double>` manteniendo `systemDefault()`**
- ❌ No resolvería el desfase de zona horaria; el calendario seguiría mostrando fechas incorrectas.

---

## Consecuencias

### Directas
- ✅ Fechas de gastos en calendario de estadísticas coinciden exactamente con la fecha seleccionada por el usuario.
- ✅ Períodos que cruzan dos meses ya no marcan días duplicados.
- ⚠️ La hora mostrada en detalle de gasto del día ahora es 00:00 UTC (coherente con el modelo de solo-fecha).

### Técnicas
**Archivos/módulos impactados:**
- `app/src/main/java/.../ui/stats/CardStatsViewModel.kt` — `PeriodStats.expensesByDay` tipo + agrupación UTC
- `app/src/main/java/.../ui/stats/CardStatsScreen.kt` — `UsageCalendar`, filtro de día, hora de gasto

**Breaking changes:**
- Ninguno en contratos públicos o DB. `PeriodStats` es un data class interno del ViewModel.

### Operacionales
- Testing requerido: manual en dispositivo con zona horaria UTC-6 o similar.
- Verificar que gastos del picker aparezcan en el día correcto del calendario.

---

## Implementación

### Paso a paso
1. Cambiar `expensesByDay: Map<Int, Double>` → `Map<LocalDate, Double>` en `PeriodStats`.
2. Reemplazar `ZoneId.systemDefault()` por `ZoneOffset.UTC` en:
   - `CardStatsViewModel.kt`: agrupación `byDay`.
   - `CardStatsScreen.kt`: `UsageCalendar` (`startDate`, `endDate`), filtro `dayExpenses`, `expenseTime`.
3. Actualizar `UsageCalendar` para usar `day` (`LocalDate`) directamente como clave en lugar de `day.dayOfMonth`.

### Files de referencia
- Commit: `adr-057-stats-calendar-utc-fix`

---

## Validación

### Cómo verificar que la decisión se implementó correctamente
- [ ] Registrar gasto el día 25 con zona horaria UTC-6.
- [ ] Abrir estadísticas → calendario del período.
- [ ] Confirmar que el día 25 está resaltado (intensity/color) y el 24 no.
- [ ] Seleccionar día 25 → lista de gastos del día muestra el gasto registrado.
- [ ] Verificar período que cruce meses (ej. corte día 15) → gasto del 20 del mes anterior no marca el 20 del mes actual.

---

## Notas y Aprendizajes

- Cuando una app almacena fechas como timestamps epoch-millis pero conceptualmente representan "días locales" (sin hora), es crítico que la escritura y lectura usen la misma referencia de zona. Material3 DatePicker trabaja en UTC; cualquier conversión posterior con `systemDefault()` introduce desfase en offsets negativos.
- `Map<Int, Double>` con `dayOfMonth` es inseguro para rangos que abarcan más de un mes. Preferir siempre `LocalDate` o un tipo que incluya año-mes-día.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-26 | Documento inicial |

---

## Referencias

- [ADR-044](ui/ADR-044-card-usage-stats-feature.md) — Feature original de estadísticas de uso
- [ADR-054](ui/ADR-054-card-stats-enhancements-batch1.md) — Mejoras en estadísticas (KPIs, calendario)
- [Google Material3 DatePicker docs](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#DatePicker(androidx.compose.material3.DatePickerState,androidx.compose.ui.Modifier,kotlin.Function0,kotlin.Function0,kotlin.Boolean,androidx.compose.material3.DatePickerColors)) — Nota sobre UTC millis
