# ADR-058: Contraste de colores en gráficos de CardStatsScreen para Dark Mode

**Fecha:** 2026-04-27
**Estado:** Aceptado
**Categoría:** ui
**Prioridad:** High
**Afecta:** `CardStatsScreen.kt`

---

## Contexto

El feature de estadísticas de uso (`CardStatsScreen`) tiene múltiples gráficos y visualizaciones que usan colores hardcodeados del sistema de diseño. En particular, `ForestGreen = #1E2C22` (un verde muy oscuro) y `MintGreen = #D8ECE4` (verde claro) funcionan bien en modo claro sobre fondos claros, pero en modo oscuro fallan por contraste insuficiente:

- `ForestGreen` sobre `BackgroundDark = #141A16` tiene relación de contraste ~1.6:1 (prácticamente invisible)
- El fondo de gradiente `MintGreen.copy(alpha=0.15)` sobre superficie oscura pierde toda definición
- Los colores de categorías generados por hash usaban rango `80-200` que produce colores demasiado oscuros para fondos oscuros
- Los bordes blancos de los dots en `LineChart` desaparecen sobre fondos oscuros

Se identificaron 18+ puntos de uso hardcodeado de estos colores en el archivo.

---

## Decisión

### Opción elegida: Adaptive color switching via `isSystemInDarkTheme()`

Por cada componente visual, definir colores contextuales basados en el tema del sistema:

| Elemento | Light mode | Dark mode |
|----------|-----------|-----------|
| Chart line/dots/selection halo | `ForestGreen` (#1E2C22) | `Color(0xFF66BB6A)` (bright green #66BB6A) |
| Chart fill gradient | `MintGreen` alpha 0.15 | `#66BB6A` alpha 0.08 |
| Dot outline (white circle) | `Color.White` | `#1B241E` (SurfaceDark surface) |
| Payment bars | `#2196F3` | `#64B5F6` (lighter blue) |
| Chart guide lines alpha | 0.08 | 0.15 |
| Category hash colors | `coerceIn(80, 200)` | `coerceIn(140, 255)` |
| Calendar high intensity | `ForestGreen` | `#66BB6A` |
| Calendar soft intensity | `MintGreen` | `#A5D6A7` |
| Category icon background alpha | 0.15 | 0.25 |
| PeriodDetail icon bg | `MintGreen` | `#2A3F30` |
| PeriodDetail icon tint | `ForestGreen` | `#66BB6A` |
| FilterChip selected accent | `ForestGreen` | `#66BB6A` |

### Por qué esta opción

- ✅ No requiere crear nuevos tokens de color — reutiliza existentes con lógica adaptativa
- ✅ Cada componente recibe el color correcto sin afectar otros componentes
- ✅ Mantiene coherencia con el sistema de diseño existente
- ✅ Simple de implementar y mantener — un solo `isSystemInDarkTheme()` por sección
- ⚠️ Trade-off: switch condicional en lugar de token unificado — se considera acceptable dado que los colores de gráficos son muy contextuales

### Opciones rechazadas

**Opción A: Crear nuevos tokens `ChartGreenLight`/`ChartGreenDark`**
- ❌ Agrega 4+ nuevos colores al sistema de diseño para uso exclusivo de gráficos
- ❌ Aumenta la superficie de mantenimiento del sistema de colores

**Opción B: Usar Dynamic Color de Material 3**
- ❌ El tema desactiva dynamic color explícitamente para mantener identidad del diseño system

---

## Consecuencias

### Directas
- ✅ Gráficos perfectamente visibles en ambos modos
- ✅ Contraste WCAG AA/AAA cumplido en todas las combinaciones de color
- ✅ Eliminación de "puntos fantasma" (dots invisibles en línea)
- ✅ Legend colors en PaymentsVsExpenses legible en dark mode

### Técnicas
**Archivos/módulos impactados:**
- `app/src/.../ui/stats/CardStatsScreen.kt` — Adaptive colors para 18+ puntos de uso

**Breaking changes:**
- Ninguno — cambio puramente visual, retrocompatible

### Operacionales
- Testing requerido: manual — verificar cada gráfico en light y dark mode
- Documentación: este ADR

---

## Implementación

### Paso a paso
1. Agregar `import androidx.compose.foundation.isSystemInDarkTheme` si no existe
2. Por cada sección composable, declarar variables `isDark = isSystemInDarkTheme()` y colores adaptativos
3. Reemplazar todos los `ForestGreen` hardcodeados con versión adaptativa
4. Reemplazar rangos de hash de categoría `coerceIn(80,200)` → `coerceIn(if(isDark) 140 else 80, if(isDark) 255 else 200)`
5. Build y verificación

### Files de referencia
- `app/src/.../ui/stats/CardStatsScreen.kt` — Componentes afectados
- `app/src/.../ui/theme/Color.kt` — Definición de `ForestGreen`/`MintGreen`

---

## Validación

### Cómo verificar que la decisión se implementó correctamente
- [ ] LineChart: línea verde visible tanto en light como dark mode
- [ ] LineChart: puntos con borde visible sobre línea
- [ ] PaymentsVsExpensesChart: barras de gastos Y pagos visibles en dark mode
- [ ] CalendarLegend: colores de leyenda различиbles en dark mode
- [ ] CategoryBreakdown: colores de categoría brillantes y contrastados en dark mode
- [ ] PeriodDetailSection: ícono dePayments en círculo visible en ambos modos
- [ ] FilterChips: chip "1M/3M/6M/1A" seleccionado visible en dark mode
- [ ] Tooltip de chart: texto legible con fondo contrastado

### Métricas de éxito
- Relación de contraste mínimo 3:1 para elementos gráficos grandes (dashboard)
- Relación de contraste mínimo 4.5:1 para texto de tooltip
- Sin elementos "invisibles" en ningún modo

---

## Notas y Aprendizajes

- [Aprendizaje 1] `ForestGreen = #1E2C22` parece un color "de éxito" pero es demasiado oscuro para fondos oscuros — al evaluar colores para gráficos, siempre considerar ambos temas
- [Aprendizaje 2] El gradiente de área bajo la línea (`Brush.verticalGradient`) necesita alpha diferente según el tema para no "desaparecer" en dark mode
- [Aprendizaje 3] Los dots con borde blanco (`drawCircle(color=Color.White)`) funcionan en fondos claros pero necesitan color de outline oscuro en modo oscuro
- [Future work] Considerar extraer colores de gráficos a un objeto `ChartColors` para centralizar la lógica adaptativa si más pantallas incorporan gráficos similares

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-27 | Documento inicial — Contraste de gráficos CardStatsScreen en dark mode |

---

## Referencias

- [ADR-017](../architecture/ADR-017-dark-mode-color-system.md) — Sistema de colores adaptativo (precursor)
- [ADR-018](../ui/ADR-018-card-text-color-contrast.md) — Color dinámico de texto en tarjetas según fondo