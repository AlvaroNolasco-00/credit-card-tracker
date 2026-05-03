# ADR-065: Guard de saldo cero en alertas de pago vencido

**Fecha:** 2026-05-02
**Estado:** Aceptado
**Categoría:** ui
**Prioridad:** Medium
**Afecta:** DashboardScreen — `OverduePaymentBanner`, `CardInfoRow`

---

## Contexto

ADR-049 introdujo `OverduePaymentBanner` e `InfoChip` overdue en `CardInfoRow`. Ambos componentes se activaban únicamente con `isPaymentOverdue == true`, sin verificar si el saldo pendiente era mayor a $0.

Escenario bug: una tarjeta con corte pasado pero sin gastos en ese período (o con gastos completamente cubiertos por `partiallyPaidAmount`) reportaba `isPaymentOverdue = true` con `cutPeriodTotal = 0.0` y `extraFinancingPayment = 0.0`. Resultado: el banner rojo y el chip "Vencido hace N día(s)" aparecían con saldo de $0.00, confundiendo al usuario.

ADR-053 ya aplicó la misma lógica (`saldo > 0`) a `PayBalanceCard`. Esta decisión es la extensión coherente de ese patrón a los otros dos componentes de alerta.

---

## Decisión

### Opción elegida

Agregar guard `remaining > 0.0` antes de mostrar las alertas visuales de pago vencido en `DashboardScreen`:

**1. `OverduePaymentBanner` (línea ~291):**
```kotlin
// Antes
if (selectedCard != null && selectedCard.isPaymentOverdue) {

// Después
if (selectedCard != null && selectedCard.isPaymentOverdue &&
    (selectedCard.cutPeriodTotal + selectedCard.extraFinancingPayment - selectedCard.partiallyPaidAmount) > 0.0) {
```

**2. `CardInfoRow` — rama overdue del `InfoChip` (línea ~719):**
```kotlin
// Antes
if (state.isPaymentOverdue) {

// Después
val overdueRemaining = state.cutPeriodTotal + state.extraFinancingPayment - state.partiallyPaidAmount
if (state.isPaymentOverdue && overdueRemaining > 0.0) {
    // chip "Vencido hace N día(s)"
} else if (state.isPaidThisCycle || (state.isPaymentOverdue && overdueRemaining <= 0.0)) {
    // chip "Saldo pagado"
```

El cálculo `remaining` usa la misma fórmula que `PayBalanceCard`: `cutPeriodTotal + extraFinancingPayment - partiallyPaidAmount`.

### Por qué esta opción

- Consistente con ADR-053: misma fórmula de saldo, misma lógica de guard, cero duplicación conceptual.
- No modifica `isPaymentOverdue` en ViewModel — la condición lógica de vencimiento sigue siendo correcta; solo la presentación la filtra.
- Cuando `remaining <= 0.0` y la tarjeta está "técnicamente vencida", cae al estado "Saldo pagado" — UX más limpia que no mostrar ningún estado.
- Sin cambios de base de datos, entidades ni interfaces.

### Opciones rechazadas

**Opción A: Corregir `isPaymentOverdue` en el ViewModel para retornar `false` cuando saldo = 0**
- ❌ Acopla lógica de negocio con lógica de presentación.
- ❌ `isPaymentOverdue` describe un estado temporal real (la fecha venció); forzarlo a `false` ocultaría información para notificaciones y futuros reportes.

**Opción B: Agregar campo `overdueRemaining` a `CardDashboardState`**
- ❌ Redundante — la fórmula ya existe en `PayBalanceCard` y es trivial de calcular en la UI.
- ❌ Scope mayor que el bug requiere.

---

## Consecuencias

### Directas
- `OverduePaymentBanner` no aparece si el saldo del ciclo es $0.00.
- `InfoChip` "Vencido hace N día(s)" no aparece si saldo = $0.00; muestra "Saldo pagado" en su lugar.
- `PayBalanceCard` ya tenía este guard (ADR-053) — comportamiento ahora consistente en los tres componentes.

### Técnicas

**Archivos/módulos impactados:**
- `ui/dashboard/DashboardScreen.kt` — condición `OverduePaymentBanner` y rama overdue de `CardInfoRow`.

**Breaking changes:**
- Ninguno. Solo lógica de visibilidad en la capa de presentación.

### Operacionales
- Usuarios con tarjetas de saldo $0 en período vencido ya no ven alerta falsa.
- No afecta notificaciones (calculadas en `ReminderScheduler` vía `DateUtils`, no en la UI).

---

## Implementación

### Paso a paso

1. ✅ Agregar guard `remaining > 0.0` en condición de `OverduePaymentBanner` en `DashboardScreen`.
2. ✅ Extraer `overdueRemaining` local en `CardInfoRow`; guard en rama overdue del `InfoChip`; extender `else if` para capturar caso `overdue && remaining <= 0.0` → "Saldo pagado".

### Files de referencia

- `ui/dashboard/DashboardScreen.kt` — líneas ~291 y ~719

---

## Validación

### Cómo verificar que la decisión se implementó correctamente

- [ ] Tarjeta con cutOff pasado, paymentDue pasado, pero sin gastos en el período → no aparece banner rojo ni chip "Vencido"
- [ ] Tarjeta con cutOff pasado, paymentDue pasado, con saldo > $0 → banner rojo + chip "Vencido" aparecen normalmente
- [ ] Tarjeta con saldo completamente cubierto por `partiallyPaidAmount` → chip muestra "Saldo pagado"
- [ ] `PayBalanceCard` no aparece para tarjetas con saldo $0 (comportamiento previo de ADR-053 intacto)

### Métricas de éxito
- Cero reportes de alerta de vencimiento falsa para saldo $0.

---

## Notas y Aprendizajes

El patrón `saldo > 0` como guard de alertas de pago fue establecido en ADR-053 para `PayBalanceCard` pero no se aplicó consistentemente a los otros componentes de alerta introducidos en ADR-049. Esta decisión completa la cobertura.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-05-02 | Decisión inicial — guard de saldo cero en alertas overdue |

---

## Referencias

- [ADR-049](ui/ADR-049-overdue-payment-alerts.md) — Alertas visuales de pago vencido (componentes afectados).
- [ADR-053](ui/ADR-053-overdue-payment-fix-and-retroactive-date.md) — Establece guard `saldo > 0` en `PayBalanceCard`.
- [CHANGELOG.md](../../CHANGELOG.md) — Entrada de release.
