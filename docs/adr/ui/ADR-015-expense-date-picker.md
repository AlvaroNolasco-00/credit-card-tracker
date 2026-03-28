# ADR-015: Selector de fecha de transacción en AddExpenseScreen

**Fecha:** 2026-03-27
**Estado:** Aceptado
**Categoría:** ui

## Contexto

Al agregar un gasto, la fecha se fijaba siempre a `System.currentTimeMillis()`, impidiendo registrar compras pasadas. Los usuarios necesitan poder ingresar la fecha real de la transacción para mantener un historial preciso, especialmente cuando registran gastos con retraso.

## Decisión

Se añade un `DatePickerSection` composable en `AddExpenseScreen` entre el campo de descripción y la sección MSI. Muestra la fecha seleccionada formateada (`dd MMM yyyy` en español) y abre un `DatePickerDialog` de Material3 al tocar el ícono de calendario.

- El estado `selectedDateMillis` se inicializa con `System.currentTimeMillis()` (hoy).
- En modo edición, `selectedDateMillis` se pre-carga desde `expense.date` en el `LaunchedEffect`.
- El botón "Guardar/Actualizar" usa `selectedDateMillis` en lugar de calcular la fecha en el momento del guardado.
- El `DatePickerDialog` usa `rememberDatePickerState` con la fecha actual como valor inicial.

## Alternativas consideradas

**Campo de texto manual:** Requeriría validación de formato y es más propenso a errores del usuario.

**Bottom sheet con calendario custom:** Más complejo, sin beneficio adicional sobre el `DatePickerDialog` nativo de Material3.

## Consecuencias

- Los usuarios pueden registrar compras con fecha retroactiva, mejorando la precisión del historial.
- La fecha de transacción se refleja correctamente en los cálculos del periodo del dashboard y el widget.
- En modo edición, la fecha existente se preserva correctamente y puede modificarse.
