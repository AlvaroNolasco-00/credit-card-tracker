# ADR-017: Sistema de colores adaptativo para modo oscuro

**Fecha:** 2026-03-27
**Estado:** Aceptado
**Categoría:** ui

## Contexto

Las pantallas `DashboardScreen` y `ExpenseHistoryScreen` usaban colores hardcodeados (`TextDark`, `SoftGray`, `Color.White`, tonos crema, etc.) que no se adaptaban al modo oscuro del dispositivo. Esto resultaba en textos negros sobre fondos oscuros (invisibles) y fondos claros que rompían la cohesión visual en modo oscuro.

## Decisión

Se reemplazaron todos los colores hardcodeados con equivalentes del esquema de Material 3:

- **Textos primarios:** `TextDark` → `MaterialTheme.colorScheme.onSurface` / `onBackground`
- **Textos secundarios:** `TextGray` → `MaterialTheme.colorScheme.onSurfaceVariant`
- **Fondos de contenedores:** `Color.White`, `SoftGray`, `Color(0xFFF2F2F2)` → `MaterialTheme.colorScheme.surface`, `surfaceVariant`
- **Fondos decorativos:** `Color(0xFFFFF8E1)` (crema), `Color(0xFFEDEDED)` → `MaterialTheme.colorScheme.surfaceVariant`
- **Bordes:** `Color.Black.copy(alpha = 0.06f)` → `MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)`
- **Acentos:** Se mantienen colores como `ForestGreen` → `MaterialTheme.colorScheme.primary`

### Pantallas afectadas

- `DashboardScreen.kt`: encabezado, búsqueda, `InfoChip`, `TransactionItem`, `IncomeSetupBanner`, `SalaryUsageCard`, `BottomActionBar`
- `ExpenseHistoryScreen.kt`: `FilterChip`, items de gasto

### Colores que permanecen hardcodeados

Los textos `Color.White` sobre tarjetas de crédito (gradientes oscuros) se mantienen intencionales. Estos elementos siempre se visualizan sobre fondos de color dinámico y el contraste es garantizado por el gradiente seleccionado.

## Consecuencias

- Las pantallas ahora responden automáticamente al tema del dispositivo (luz/oscuro).
- Mejor accesibilidad: contraste suficiente en ambos modos.
- Mantenimiento simplificado: cambios al esquema de colores en `Theme.kt` se propagan automáticamente a toda la UI.
- El `SalaryUsageCard` ahora usa `surfaceVariant` uniforme para todos los estados, confiando en el color de barra (`barColor`) para comunicar el estado de severidad.
