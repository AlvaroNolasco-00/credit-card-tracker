# ADR-026: Contraste de botones en modales y formularios

**Fecha:** 2026-03-31
**Estado:** Aceptado
**Categoría:** ui

## Contexto

En modo oscuro, los botones de acción de todos los `AlertDialog` y `DatePickerDialog` de la app eran invisibles. El problema raíz es que `PrimaryDark = ForestGreen (#1E2C22)`, un verde muy oscuro, que `TextButton` usa como `contentColor` por defecto. Al renderizarse sobre el fondo oscuro del dialog (`SurfaceDark = #1B241E`), el texto desaparecía por falta de contraste.

Archivos afectados: `AddExpenseScreen.kt`, `DashboardScreen.kt`, `AddEditCardScreen.kt`, `ExpenseSearchScreen.kt`.

Adicionalmente, el `DatePicker` dentro de los `DatePickerDialog` usaba `primary` para resaltar el día seleccionado y el día actual, con el mismo problema de contraste.

## Decisión

Se estandarizó el color de todos los botones de acción en modales mediante override explícito del parámetro `colors` en cada `TextButton`:

- **Botones de acción positiva** (Aceptar, OK, Crear, Confirmar): `contentColor = MaterialTheme.colorScheme.secondary` → `SoftLime (#B6D491)`, visible sobre fondos oscuros.
- **Botones neutros/cancelar**: `contentColor = MaterialTheme.colorScheme.onSurfaceVariant` → `#B0B0B0`, tono neutro legible.
- **Botones destructivos** (Eliminar): mantienen `contentColor = MaterialTheme.colorScheme.error` ya establecido anteriormente.

Para los `DatePicker`, se sobrescriben los colores de selección via `DatePickerDefaults.colors()`:
```kotlin
DatePickerDefaults.colors(
    selectedDayContainerColor = MaterialTheme.colorScheme.secondary,
    selectedDayContentColor  = MaterialTheme.colorScheme.onSecondary,
    todayDateBorderColor     = MaterialTheme.colorScheme.secondary,
    todayContentColor        = MaterialTheme.colorScheme.secondary
)
```

Dentro del card picker de `AddExpenseScreen`, el ítem seleccionado en la lista también se corrigió de `primary` a `secondary`.

Se decidió **no cambiar `PrimaryDark`** en el tema global para evitar efectos colaterales en los componentes `Button` (filled) que sí tienen contraste correcto al usar `primary` como `containerColor` con texto blanco.

## Consecuencias

- Todos los botones de acción en modales son visibles tanto en light como en dark mode.
- El patrón queda establecido: cualquier `TextButton` dentro de un dialog debe llevar `colors` explícito — nunca depender del `primary` heredado.
- El día seleccionado y el día actual en todos los `DatePicker` de la app son visibles en modo oscuro.
