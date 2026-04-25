# ADR-051: Gastos Recurrentes — Cargos periódicos asociados a tarjeta de crédito

**Fecha:** 2026-04-24
**Estado:** Aceptado
**Categoría:** data, ui
**Prioridad:** High
**Afecta:** Entity, DAO, Repository, ViewModel, Navigation, Dashboard

---

## Contexto

Los usuarios necesitan registrar cargos recurrentes conocidos (ej: plan de teléfono, streaming, membresías) que se debitan periódicamente de su tarjeta de crédito. Actualmente la app solo permite registrar gastos manuales uno por uno. El需求 es:

- Registrar un gasto recurrente asociado a una tarjeta específica
- El usuario puede o **no** conocer la fecha exacta del cobro
  - Si conoce el día: el sistema lo usa para filtro y referencia
  - Si **no** conoce la fecha: se aplica una vez por cada corte del período (sin importar la fecha exacta dentro del período)
- Asignar categorías (igual que gastos normales) para budgeting
- El monto total de gastos recurrentes se suma al total de la tarjeta en el dashboard
- Los gastos recurrentes aparecen como línea de breakdown en el saldo del período

Investigación previa: El sistema ya tiene `IncomeEntry.isRecurring` como modelo conceptual. El patrón de `Expense` + `ExpenseCategory` + `Category` existe para categorías.

---

## Decisión

### Opción elegida: Entidad separada `RecurringExpense`

Se crea una entidad nueva independiente (`RecurringExpense`) para gastos recurrentes, con FK a `CreditCard` y una tabla junction `RecurringExpenseCategory` para categorías.

### Por qué esta opción

- **Aislamiento clara**: Un gasto normal vs uno recurrente tiene semántica diferente (uno es instantáneo, el otro es un template).
- **Escalabilidad**: Si en el futuro se necesita "pausar" o "desactivar" un recurrente sin borrarlo, `isActive` está listo.
- **No rompe el modelo Expense**: Los gastos con MSI, receipt, OCR se manejan en `Expense` normal — son instancias concretas, no templates.
- **Pattern familiar**: Es análogo a `IncomeEntry.isRecurring` pero con las complejidades adicionales de categorías y asociación a tarjeta.

### Opciones rechazadas

**Opción A: Agregar campo `isRecurring` a `Expense`**
- ❌ Un expense con `isRecurring=true` necesitaría `dayOfMonth`, `cardId` (ya lo tiene), y lógica de "instanciación" en cada corte — el modelo se ensucia.
- ❌ Rompe con MSI (un gasto MSI es una instancia concreta, no un template).
- ❌ La query de "gastos del período" se contaminaría con lógica condicional.

**Opción B:复用 `IncomeEntry` con tipo "EXPENSE"**
- ❌ Semánticamente incorrecto — income y expense son flujos opuestos.
- ❌ Las categorías de income son diferentes de gastos.

---

## Consecuencias

### Directas
- ✅ Los usuarios pueden registrar cargos recurrentes con descripción, monto, día opcional y categorías
- ✅ El total del período en dashboard incluye gastos recurrentes aplicables
- ✅ Un ícono de "repeat" en cada tarjeta del dashboard da acceso directo a la pantalla de gastos recurrentes
- ✅ La creación de un gasto recurrente no genera un expense concreto — es un registro de template que infuye el cálculo

### Técnicas
**Archivos/módulos impactados:**
- `data/entity/RecurringExpense.kt` — Nueva entidad
- `data/entity/RecurringExpenseCategory.kt` — Junction table
- `data/entity/RecurringExpenseWithCategories.kt` — Relation wrapper
- `data/dao/RecurringExpenseDao.kt` — DAO con queries de activo por tarjeta
- `data/AppDatabase.kt` — Migration 12→13, nuevas entidades, nuevo DAO
- `di/AppModule.kt` — Provider para RecurringExpenseDao
- `data/repository/CreditCardRepository.kt` — Métodos CRUD + getAllRecurringExpenses
- `ui/recurring/RecurringExpensesViewModel.kt` — State + lógica de guardado/borrado
- `ui/recurring/RecurringExpensesScreen.kt` — Lista de gastos recurrentes
- `ui/recurring/AddEditRecurringExpenseScreen.kt` — Formulario con toggle de día opcional
- `ui/navigation/Navigation.kt` — 3 nuevas rutas
- `ui/dashboard/DashboardScreen.kt` — Icono repeat en CreditCardPagerItem
- `ui/dashboard/DashboardViewModel.kt` — Cálculo de recurringExpensesTotal en cada estado
- `util/DateUtils.kt` — Helper `isRecurringExpenseApplicable(dayOfMonth, periodStart, periodEnd)`

**Breaking changes:**
- DB Migration 12→13 (no destructiva — solo crea tablas y Índices)
- Repository: constructor recibe nuevo parámetro `recurringExpenseDao`

### Operacionales
- Testing requerido: unit (ViewModel), manual (pantalla completa)
- Documentación: Este ADR + CHANGELOG.md

---

## Implementación

### Paso a paso

1. Crear `RecurringExpense`, `RecurringExpenseCategory`, `RecurringExpenseWithCategories`
2. Crear `RecurringExpenseDao` con queries Flow
3. Migration 12→13 en `AppDatabase.kt`, agregar entidades al array, abstract fun dao
4. Actualizar `AppModule.kt` con provider para DAO
5. Actualizar `CreditCardRepository` con CRUD methods + `getAllRecurringExpenses()`
6. Crear `RecurringExpensesViewModel` con state y funciones de save/delete
7. Crear `RecurringExpensesScreen` (lista) + `AddEditRecurringExpenseScreen` (formulario)
8. Agregar 3 rutas en `Navigation.kt`
9. Agregar `onRecurringExpensesClick` en `DashboardScreen` + icono repeat en `CreditCardPagerItem`
10. Agregar `recurringExpensesTotal` en `CardDashboardState` + lógica en `loadDashboard()`
11. Agregar `isRecurringExpenseApplicable` en `DateUtils`

### Files de referencia

- PR: esta rama `feature/recurring-expenses`
- Entidad análoga: `IncomeEntry.kt` (recurring pattern)
- Categorías: `ExpenseCategory.kt` + `Category.kt`

---

## Validación

### Cómo verificar que la decisión se implementó correctamente

- [ ] Puedo crear un gasto recurrente sin fecha (dayOfMonth=null) asociado a una tarjeta
- [ ] Puedo crear un gasto recurrente con fecha fija (dayOfMonth=15) asociado a una tarjeta
- [ ] Puedo asignar categorías a un gasto recurrente
- [ ] El total del dashboard refleja el monto recurrente en el período correcto
- [ ] El ícono de "repeat" aparece en la tarjeta del dashboard y navega a la pantalla de gastos recurrentes
- [ ] Puedo editar y eliminar un gasto recurrente
- [ ] Al borrar una tarjeta, los gastos recurrentes quedan con cardId=null (no crashea)

### Métricas de éxito

- Sin regresión en existentes: los gastos normales siguen funcionando igual
- El cálculo de `totalSpent` en dashboard incluye `recurringExpensesTotal` correctamente
- La migración 12→13 no destructiva funciona en device con DB existente

---

## Notas y Aprendizajes

- La decisión de usar entidad separada (no campo `isRecurring` en Expense) fue correcta porque preserva lasemántica de "template vs instancia concreta" — MSI y OCR son instancias, no templates.
- El caso `dayOfMonth=null` ("sin fecha conocida") es intencionalmente la opción por defecto del toggle OFF, porque el usuario puede no saber la fecha exacta del cargo.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-24 | Documento inicial — feature completa de gastos recurrentes |

---

## Referencias

- [ADR-014](architecture/ADR-014-msi-period-amount-fix.md) — Patrón de campos adicionales en entidad (referencia de estilo)
- `docs/adr/MAINTAINER.md` — Pattern 2: Feature nueva UI + Widget (estructura de implementación)
