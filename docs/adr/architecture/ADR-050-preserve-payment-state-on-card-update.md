# ADR-050: Preserve Payment State on Card Update

**Fecha:** 2026-04-17  
**Estado:** Aceptado  
**Categoría:** architecture  
**Prioridad:** High  
**Afecta:** `CardsViewModel.saveCard()`, `CreditCard` entity, Dashboard reactivity  

---

## Contexto

Al editar y guardar una tarjeta (cualquier tarjeta), el ViewModel `CardsViewModel.saveCard()` construía una nueva entidad `CreditCard` desde cero con valores de formulario únicamente. Esto causaba que campos de estado de pago como `lastPaymentDate`, `partialPaymentAmount` y `partialPaymentCycleEnd` regressaran a sus valores por defecto (0L, 0.0, 0L).

**Síntoma:** Usuario paga saldo vencido de tarjeta A. Luego edita y guarda tarjeta B. Al volver al dashboard, tarjeta A reaparece como "vencida" aunque ya fue pagada.

**Root cause:** `saveCard()` al guardar cualquier tarjeta con `existingCardId`, Room sobrescribía los campos de pago con defauts, y `DashboardViewModel.loadDashboard()` re-evaluaba `isPaid = card.lastPaymentDate > prevEnd` → false → overdue nuevamente.

---

## Decisión

Cuando se actualiza una tarjeta existente, cargar la tarjeta actual del repositorio primero y usar `.copy()` para sobrescribir solo los campos mutables del formulario, preservando todo estado transversal.

### Opción elegida

En `CardsViewModel.saveCard()`:
```kotlin
if (existingCardId != null) {
    val existing = repository.getCardById(existingCardId)
    val card = (existing ?: fallback).copy(
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
}
```

Para creación nueva, usar constructor directo (no hay estado anterior).

### Por qué esta opción

- ✅ Preserva `lastPaymentDate`, `partialPaymentAmount`, `partialPaymentCycleEnd` automáticamente
- ✅ Preserva `createdAt` (timestamp creacional)
- ✅ Aprovecha data class `.copy()` — limpiar y type-safe
- ✅ Evita redundancia: no cargar toda la tarjeta en memoria durante formulario
- ✅ Costo mínimo: carga única desde DB en `saveCard()` (evento raro)

### Opciones rechazadas

**Opción A: Agregar todos los campos de pago como parámetros a `saveCard()`**
- ❌ Expande el signature del método innecesariamente
- ❌ Requiere pasar estos fields desde UI sin usuario interactuando con ellos
- ❌ Acoplamiento UI ↔ estado transversal del backend

**Opción B: Hacer que DashboardViewModel recalcule `isPaid` cada vez**
- ❌ Ya lo hace; problema es que los datos source (Room) fueron sobrescritos
- ❌ No es un issue de lógica de dashboard sino de integridad de datos en el update

---

## Consecuencias

### Directas
- ✅ Tarjetas pagadas permanecen pagadas tras editar cualquier otra tarjeta
- ✅ Pagos parciales (`partialPaymentAmount`, `partialPaymentCycleEnd`) se preservan
- ✅ Consistency entre estado persistido y UI reactiva

### Técnicas
**Archivos impactados:**
- `app/src/main/java/com/alvaronolasco/creditcardtracker/ui/cards/CardsViewModel.kt` — Lógica de update refactorizada
- `app/src/main/java/com/alvaronolasco/creditcardtracker/ui/dashboard/DashboardViewModel.kt` — Sin cambios; flujo reactivo ahora recibe datos íntegros

**Breaking changes:** Ninguno (cambio interno, API del ViewModel idéntica).

### Operacionales
- Testing: Verificar flujo "pagar saldo vencido → editar otra tarjeta → dashboard" muestra saldo como pagado
- Documentación: Agregar nota en `CLAUDE.md` sobre esta restricción
- No hay migraciones de datos

---

## Implementación

### Paso a paso
1. Modificar `CardsViewModel.saveCard()` para cargar `existingCard` cuando `existingCardId != null`
2. Usar `.copy()` para sobrescribir solo campos de formulario
3. Mantener rama de creación nueva con constructor directo
4. Verificar manualmente en emulador (pagar tarjeta A, editar tarjeta B, dashboard)

### Files de referencia
- File: `app/src/main/java/com/alvaronolasco/creditcardtracker/ui/cards/CardsViewModel.kt` — líneas 46-84

---

## Validación

### Cómo verificar que se implementó correctamente
- [ ] Crear 2+ tarjetas
- [ ] Marcar una como vencida (dejar pasar fecha de pago sin pagar)
- [ ] Pagar desde dashboard → "Pago completado" visto
- [ ] Ir a otra tarjeta, editar campos (nombre, límite, etc.), guardar
- [ ] Volver al dashboard → tarjeta pagada sigue como pagada (no reaparece como vencida)
- [ ] Repetir 3+ veces con diferentes tarjetas

### Métricas de éxito
- Tarjetas pagadas permanecen pagadas tras N ediciones de otras tarjetas
- Dashboard no muestra alertas de vencimiento fantasma
- No hay crashes o warnings en Logcat

---

## Notas y Aprendizajes

- **Lesson 1:** Cuando un ViewModel guarda una entidad existente, debe cargar estado anterior si el update es parcial (solo algunos campos mutables). `.copy()` es el patrón limpio para esto.
- **Lesson 2:** Room flujos reactivos (`getAllCards().flatMapLatest`) pueden amplificar bugs en integridad de datos — si sobrescribes un campo con un default involuntario, el UI refleja eso inmediatamente.
- **Future work:** Considerar un patrón genérico `saveEntityPartial<T>()` en el repo si más ViewModels necesitan este comportamiento.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-17 | Documento inicial — fix para preservar payment state en card update |

---

## Referencias

- [ADR-022](../ui/ADR-022-pay-balance-button.md) — Feature de pagar saldo (usa `payPartial()` en DashboardViewModel)
- [CreditCard entity](../../../app/src/main/java/com/alvaronolasco/creditcardtracker/data/entity/CreditCard.kt) — Definición de campos
- [DashboardViewModel](../../../app/src/main/java/com/alvaronolasco/creditcardtracker/ui/dashboard/DashboardViewModel.kt) — Lógica de `isPaid` y reactiva
