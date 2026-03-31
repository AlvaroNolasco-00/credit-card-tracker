# ADR-027: Bloquear fechas futuras en DatePicker de gastos

**Fecha:** 2026-03-31
**Estado:** Aceptado
**Categoría:** ui

## Contexto

En `AddExpenseScreen` el usuario puede abrir un `DatePickerDialog` para seleccionar la fecha de transacción, pero actualmente no hay restricción que impida elegir una fecha futura. Esto permite registrar/filtrar gastos con fechas inválidas para el dominio del historial.

Además, en `ExpenseSearchScreen` los `DatePicker` usados para rango de fechas (desde/hasta) tampoco impedían seleccionar fechas futuras, generando rangos incoherentes.

## Decisión

Se aplica la restricción a nivel de `DatePickerState` usando `selectableDates` (Material3):

- Implementar `SelectableDates` para habilitar únicamente fechas `<= hoy` (hoy y pasadas) comparando contra `LocalDate.now(ZoneId.systemDefault())`.
- Clampear la fecha inicial (`initialSelectedDateMillis`) cuando el estado actual trae un valor futuro, para evitar que el picker arranque preseleccionando una fecha no válida.

Esta decisión se implementa en:

- `AddExpenseScreen`: `DatePickerDialog` del campo “Fecha de transacción”.
- `ExpenseSearchScreen`: pickers de “Desde” y “Hasta” del rango.

## Consecuencias

- Las fechas futuras aparecen deshabilitadas en ambos `DatePicker` y el usuario no puede seleccionarlas.
- En modo edición o si el estado de búsqueda llegara con fechas futuras, esas fechas se ajustan automáticamente a `hoy` (inicio de día).
- La validación depende de la zona horaria del dispositivo (`ZoneId.systemDefault()`); cambios de zona horaria pueden afectar el día “hoy” según el dispositivo.
