# ADR-031: Recordatorio de presupuesto mensual en Dashboard

**Fecha:** 2026-04-03
**Estado:** Aceptado
**Categoría:** ui

## Contexto

Los usuarios pueden llegar al inicio de un nuevo mes sin haber definido su presupuesto mensual. Sin un presupuesto activo pierden visibilidad sobre el control de sus finanzas. Se necesita un mecanismo proactivo que los invite a crear su presupuesto sin ser invasivo.

## Decisión

Se muestra un `AlertDialog` (`BudgetReminderDialog`) en `DashboardScreen` cuando se cumplen todas las condiciones:

1. El mes actual tiene 3 o más días transcurridos (`LocalDate.now().dayOfMonth >= 3`)
2. No hay ningún `BudgetItem` para el mes actual
3. El usuario no ha descartado el diálogo en este mes (verificado via `UserPreferencesRepository`)
4. El `NameSetupBottomSheet` no está visible (para no apilar modales)

El diálogo ofrece dos acciones:
- **"Crear presupuesto"** → descarta el prompt y navega a `BudgetScreen`
- **"Más tarde"** → descarta el prompt persistentemente hasta el siguiente mes

El descarte se persiste en `SharedPreferences` con clave `budget_prompt_dismissed_month` almacenando el valor `YYYY-MM` del mes actual, de modo que el prompt no reaparece en el mismo mes.

## Consecuencias

- `UserPreferencesRepository` agrega `dismissBudgetPrompt(monthYear)` e `isBudgetPromptDismissed(monthYear)`.
- `DashboardUiState` agrega el campo `showBudgetPrompt: Boolean`.
- `DashboardViewModel` agrega `dismissBudgetPrompt()` y extiende `loadBudgetStatus()` con la lógica de activación.
- El diálogo se suprime automáticamente si `showNamePrompt` está activo (guarda tanto en ViewModel como en Screen).
- El grace period de 3 días evita que el prompt aparezca el primer día del mes cuando el usuario aún no ha tenido tiempo de configurar el presupuesto.
