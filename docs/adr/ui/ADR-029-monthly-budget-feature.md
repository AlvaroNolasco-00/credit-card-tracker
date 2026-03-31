# ADR-029: Presupuesto mensual por categoría

**Fecha:** 2026-03-31
**Estado:** Aceptado
**Categoría:** ui

## Contexto

El usuario necesita definir un presupuesto mensual por categoría de gasto y comparar los gastos reales contra esos límites. Actualmente la app no tiene ninguna funcionalidad de presupuesto. Las categorías ya existen como entidad global (`Category`) y se asignan a gastos vía la tabla junction `ExpenseCategory`.

## Decisión

### Modelo de datos

Se introduce la entidad `BudgetItem` (tabla `budget_items`, DB versión 9):
- Un registro por combinación `(categoryId, monthYear)` con un `UNIQUE INDEX` que previene duplicados.
- `monthYear` sigue el formato `"YYYY-MM"` ya establecido por `IncomeEntry`.
- FK a `Category` con `CASCADE DELETE` para limpieza automática si se elimina una categoría.
- No se crea una entidad "Budget" padre; la granularidad por categoría es suficiente y más flexible.

Se eligió **un ítem por categoría por mes** sobre alternativas como presupuesto global único, porque permite comparaciones precisas por categoría y es más útil para el usuario.

### Query de gasto por categoría

Se agrega `getSpendingPerCategory(startDate, endDate)` en `ExpenseDao` que usa el mismo patrón MSI que `getTotalSpentInPeriod`: respeta MSI activos usando `msiMonthlyAmount` en lugar de `amount` cuando `msiMonths > 1`. Devuelve `CategorySpending` (data class plana, no entidad Room).

El rango de fechas del presupuesto es el **mes calendario completo** (primer al último día del mes), no el ciclo de corte, porque el presupuesto es una herramienta personal desacoplada del ciclo bancario.

### BudgetViewModel

Estado reactivo combinado (`combine()`) de 4 fuentes:
1. `getAllCategories()` — lista completa de categorías
2. `getBudgetItemsForMonth(monthYear)` — límites del mes seleccionado
3. `getSpendingPerCategory(startDate, endDate)` — gastos reales del mes
4. `getBudgetItemsForMonth(prevMonth)` — para detectar si hay presupuesto previo copiable

El mes seleccionado se maneja via `_monthYear: MutableStateFlow<String>` con `flatMapLatest` para recargar las 4 fuentes al cambiar de mes.

### BudgetScreen (pantalla única)

Pantalla accesible desde el dashboard vía ruta `"budget"`. Contiene:
- Selector de mes con chevrons (sin restricción de fecha futura — el usuario puede planear anticipadamente)
- `BudgetSummaryCard`: gasto total vs presupuesto total del mes con barra de progreso animada (verde → amarillo → rojo)
- Botón "Copiar del mes anterior" condicional (solo si el mes actual no tiene presupuesto pero el anterior sí)
- Lista de categorías dividida en "Con presupuesto" y "Sin presupuesto", cada fila con barra de progreso individual
- Dialog `BudgetEditDialog` para crear/actualizar/eliminar el presupuesto de una categoría

### Integración en Dashboard

Se agrega `BudgetSectionCard` entre la sección de ingresos y la sección de transacciones. Muestra texto diferente según si ya existe presupuesto para el mes actual (`hasBudgetThisMonth: Boolean` en `DashboardUiState`).

## Consecuencias

- DB versión 8 → 9 con migración `MIGRATION_8_9` explícita.
- `CreditCardRepository` y `AppModule` actualizados para exponer el nuevo DAO.
- El `DashboardViewModel` agrega un flow extra (`loadBudgetStatus`) que carga solo cuando cambia la lista de budget items del mes actual — overhead mínimo.
- El presupuesto no está atado al ciclo de corte de ninguna tarjeta, lo que facilita su uso para usuarios con múltiples tarjetas con cortes distintos.
