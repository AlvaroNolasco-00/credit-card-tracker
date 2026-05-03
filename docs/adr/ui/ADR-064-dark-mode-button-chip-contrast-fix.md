# ADR-064: Corrección de contraste en botones, chips y componentes para Dark Mode

**Fecha:** 2026-05-02
**Estado:** Aceptado
**Categoría:** ui
**Prioridad:** High
**Afecta:** `AppChip.kt`, `AppButton.kt`, `AppCard.kt`, `DashboardScreen.kt`

---

## Contexto

Múltiples componentes de la UI usan colores hardcodeados que fallan en dark mode:

1. **AppChip.kt**: `SoftGray` (#F2F2F2) como fondo y `TextDark` (#1A1A1A) como texto — en dark mode el fondo es demasiado brillante y el texto oscuro es invisible sobre él.

2. **DashboardScreen.kt**: 
   - Contenedor de tarjetas usa `MintGreen` (#D8ECE4) — bloque brillante en dark mode
   - Botón "Agregar otra tarjeta" usa `ForestGreen` (#1E2C22) como texto — invisible sobre fondo oscuro
   - Iconos en botones circulares usan `ForestGreen` — muy oscuro en dark mode

3. **AppButton.kt**: `MintButton` usa `MintGreen` como fondo y `ForestGreen` como texto — fondo demasiado brillante en dark mode.

4. **AppCard.kt**: Border usa `Color.Black.copy(alpha = 0.05f)` — invisible en dark mode.

Los ADRs previos (ADR-017, ADR-058) establecieron el patrón de usar `isSystemInDarkTheme()` para colores adaptativos, pero no se aplicó a todos los componentes.

---

## Decisión

### Opción elegida: Adaptive color switching via `isSystemInDarkTheme()`

Siguiendo el patrón establecido en ADR-058, aplicar colores adaptativos a cada componente afectado:

| Componente | Light Mode | Dark Mode |
|------------|-----------|-----------|
| **AppChip containerColor** | `SoftGray` (#F2F2F2) | `MaterialTheme.colorScheme.surfaceVariant` |
| **AppChip labelColor** | `TextDark` (#1A1A1A) | `MaterialTheme.colorScheme.onSurfaceVariant` |
| **AppChip selectedContainer** | `ForestGreen` (#1E2C22) | `Color(0xFF66BB6A)` (bright green) |
| **AppButton MintButton container** | `MintGreen` (#D8ECE4) | `Color(0xFF2A3F30)` (dark green) |
| **AppButton MintButton content** | `ForestGreen` (#1E2C22) | `Color(0xFF66BB6A)` (bright green) |
| **AppCard border** | `Color.Black.copy(0.05f)` | `MaterialTheme.colorScheme.outline.copy(0.15f)` |
| **Dashboard card container** | `MintGreen` (#D8ECE4) | `Color(0xFF1B241E)` (SurfaceDark) |
| **Dashboard "Add card" text** | `ForestGreen` (#1E2C22) | `Color(0xFF66BB6A)` (bright green) |
| **Dashboard icon buttons tint** | `ForestGreen` (#1E2C22) | `Color(0xFF66BB6A)` (bright green) |
| **Dashboard "Add card" icon bg** | `MintGreen.copy(0.7f)` | `Color(0xFF2A3F30)` (dark green) |

### Por qué esta opción

- ✅ Consistente con ADR-017 y ADR-058
- ✅ No requiere nuevos tokens de color
- ✅ Cada componente recibe el color correcto sin afectar otros
- ✅ Simple de mantener — un solo `isSystemInDarkTheme()` por componente

### Opciones rechazadas

**Opción A: Crear tokens globales `MintButtonDark`, `ChipBackgroundDark`**
- ❌ Agrega complegidad al sistema de colores
- ❌ Los colores son contextuales, no universales

**Opción B: Usar Dynamic Color de Material 3**
- ❌ El tema desactiva dynamic color explícitamente (ADR-017)

---

## Consecuencias

### Directas
- ✅ Todos los botones, chips y bordes visibles en ambos modos
- ✅ Texto legible en todas las combinaciones de color
- ✅ Experiencia visual consistente en light y dark mode

### Técnicas
**Archivos/módulos impactados:**
- `app/src/.../ui/components/AppChip.kt` — Colores adaptativos en FilterChip
- `app/src/.../ui/components/AppButton.kt` — MintButton adaptativo
- `app/src/.../ui/components/AppCard.kt` — Border visible en dark mode
- `app/src/.../ui/dashboard/DashboardScreen.kt` — 5 cambios de colores

**Breaking changes:**
- Ninguno — cambio puramente visual, retrocompatible

### Operacionales
- Testing requerido: manual — verificar cada componente en light y dark mode
- Documentación: este ADR

---

## Implementación

### Paso a paso
1. Agregar `import androidx.compose.foundation.isSystemInDarkTheme` donde no exista
2. Por cada componente, declarar `isDark = isSystemInDarkTheme()` y colores adaptativos
3. Reemplazar colores hardcodeados con versión adaptativa
4. Build y verificación

### Files de referencia
- `app/src/.../ui/components/AppChip.kt` — Líneas 38-43
- `app/src/.../ui/components/AppButton.kt` — Líneas 50-67
- `app/src/.../ui/components/AppCard.kt` — Línea 18
- `app/src/.../ui/dashboard/DashboardScreen.kt` — Líneas 231, 650, 663, 669, 678

---

## Validación

### Cómo verificar que la decisión se implementó correctamente
- [ ] AppChip: texto visible en ambos modos (seleccionado y no seleccionado)
- [ ] MintButton: fondo y texto visibles en ambos modos
- [ ] AppCard: border visible en dark mode
- [ ] Dashboard: contenedor de tarjetas no es un bloque brillante en dark mode
- [ ] Dashboard: texto "Agregar otra tarjeta" visible en dark mode
- [ ] Dashboard: iconos en botones circulares visibles en dark mode

### Métricas de éxito
- Relación de contraste mínimo 4.5:1 para texto
- Relación de contraste mínimo 3:1 para elementos gráficos grandes
- Sin elementos "invisibles" en ningún modo

---

## Notas y Aprendizajes

- [Aprendizaje 1] `ForestGreen = #1E2C22` parece un color de marca pero es demasiado oscuro para fondos oscuros — siempre considerar ambos temas
- [Aprendizaje 2] `SoftGray = #F2F2F2` y `MintGreen = #D8ECE4` son colores "claros" que funcionan bien en light mode pero son demasiado brillantes en dark mode
- [Future work] Considerar extraer colores de componentes comunes a un objeto `ComponentColors` si más pantallas necesitan los mismos adaptativos

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-05-02 | Documento inicial — Corrección de contraste en botones, chips y componentes |

---

## Referencias

- [ADR-017](ADR-017-dark-mode-color-system.md) — Sistema de colores adaptativo (precursor)
- [ADR-058](ADR-058-chart-colors-dark-mode-contrast.md) — Contraste de gráficos CardStatsScreen (patrón implementado)
- [ADR-026](ADR-026-dialog-button-contrast.md) — Contraste de botones en modales
