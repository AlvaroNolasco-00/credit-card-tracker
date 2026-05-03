# ADR-069: Full Redesign del Widget — Jerarquía, Semántica y Usabilidad

**Fecha:** 2026-05-02
**Estado:** Aceptado
**Categoría:** widget
**Prioridad:** High
**Afecta:** CreditCardWidget.kt, WidgetColorScheme.kt

---

## Contexto

El widget de tarjetas tenía varios problemas de usabilidad identificados tras su uso real:

1. **Small layout demasiado escaso** — Solo mostraba nombre de tarjeta y fecha de corte/pago, sin montos. El usuario debía abrir la app para ver cuánto debía.
2. **Botón "+" demasiado pequeño** — 20×14dp en medium, 24×16dp en large. Difícil de tocar en el widget sin feedback háptico.
3. **Sin crédito disponible** — Solo se mostraba el gasto total, no cuánto crédito quedaba disponible.
4. **Jerarquía visual plana** — Los datos importantes (monto, urgencia) no destacaban sobre los secundarios (banco, dígitos).
5. **Progress bar sin semántica de color** — Solo rojo (≥95%) y amber (≥80%). Faltaba verde para zona segura y amarillo para atención.
6. **Empty state genérico** — Texto plano sin icono ni CTA claro.
7. **Income card sin progreso semántico** — Usaba blanco siempre, sin indicar si el presupuesto se estaba agotando.

---

## Decisión

Rediseñar los tres layouts del widget (Small, Medium, Large) mejorando jerarquía visual, agregando datos faltantes y usando colores semánticos en progress bars y badges.

### Opción elegida
Full redesign respetando colores de tarjeta seleccionados por el usuario como fondo de cada card row.

### Por qué esta opción
- **Información crítica visible siempre** — El monto total (`$12,345`) aparece en todos los tamaños, incluso Small
- **Badge de fecha con color semántico** — Rojo si ≤3 días, verde si >7 días, neutro si está cerca
- **Progress bar con 4 zonas de color** — Verde (<50%), amarillo (50-70%), amber (70-85%), rojo (>85%)
- **Crédito disponible visible en Large** — "Disp: $37,655" como texto secundario
- **Botón "+" agrandado** — 28×18dp en todos los layouts para mejor tocabilidad
- **Empty state con icono** — Box con "+" como placeholder visual + texto "Sin tarjetas aún · Toca para agregar"
- **Income card con color semántico** — Progress bar y texto "%" usan el mismo sistema de 4 colores
- **Colores de tarjeta del usuario respetados** — Las card rows mantienen `cardColor()` como fondo

### Opciones rechazadas
**Opción A: Mantener layout actual y solo cambiar colores**
- ❌ No resuelve la falta de montos en Small ni la jerarquía plana

**Opción B: Widget minimalista sin botón "+" (solo al abrir la app)**
- ❌ El botón "+" es el principal punto de entrada para gastos rápidos; perderlo impacta negativamente la experiencia

**Opción C: Usar gradientes en vez de colores sólidos para fondos de tarjeta**
- ❌ Incrementa complejidad visual y Glance tiene soporte limitado para gradientes

---

## Consecuencias

### Directas
- ✅ Todos los tamaños muestran el monto total de la tarjeta
- ✅ El crédito disponible es visible en el layout Large
- ✅ Los badges de fecha usan color de fondo semántico (rojo/verde/neutro)
- ✅ Progress bars con 4 colores: verde, amarillo, amber, rojo
- ✅ Botón "+" más grande y fácil de tocar
- ✅ Empty state con icono visual y texto más amigable
- ⚠️ El layout Medium incrementó altura de 72dp a 80dp para acomodar la jerarquía de 4 filas

### Técnicas
**Archivos/módulos impactados:**
- `app/src/main/java/.../widget/CreditCardWidget.kt` — Rediseño completo de SmallLayout, MediumLayout, LargeLayout, EmptyState, IncomeSummaryCard, CreditUsageProgressBar; nueva composable DateBadge; nueva helper function semanticProgressColor; nuevos campos computados WidgetCardData.availableCredit y WidgetIncomeData.remaining
- `app/src/main/.../widget/WidgetColorScheme.kt` — 7 nuevos colores: progressSafe, progressAttention, progressWarning, progressDanger, urgentBadgeBg, safeBadgeBg, availableColor
- `docs/adr/widget/ADR-069-widget-full-redesign.md` — Este registro

**Breaking changes:**
- Ninguno. Las data classes `WidgetCardData` y `WidgetIncomeData` agregan propiedades computadas sin cambiar la estructura existente

### Operacionales
- Testing requerido: manual en dispositivo con widget en 3 tamaños
- Documentación: Este ADR + CHANGELOG
- Comunicación: Solo desarrollo

---

## Implementación

### Paso a paso
1. Agregar colores semánticos a `WidgetColorScheme.kt` (7 nuevas constantes)
2. Refactorizar `CreditUsageProgressBar` para usar `semanticProgressColor()` con 4 zonas
3. Rediseñar `SmallLayout` — quitar header "Tarjetas", agregar monto en cada row, mover fecha debajo
4. Rediseñar `MediumLayout` — banco + botón "+" agrandado en fila 1, nombre + dígitos en fila 2, monto/limite + badge en fila 3, progress bar en fila 4
5. Rediseñar `LargeLayout` — monto hero, disponible, progress bar semántico, badge con color
6. Agregar composable `DateBadge` con color semántico (rojo urgente, verde seguro)
7. Mejorar `EmptyState` con Box icon + texto descriptivo
8. Agregar `semanticProgressColor()` como helper global
9. Compilar + testear

### Files de referencia
- Branch: `feature/widget-redesign`
- Commit: _(pendiente)_
- Tests: `./gradlew test` (build + tests pasan)

---

## Validación

### Cómo verificar que la decisión se implementó correctamente
- [ ] Widget en tamaño Small (2×2) muestra nombre, monto y fecha de cada tarjeta
- [ ] Widget en tamaño Medium (4×2) muestra bank, name, dígitos, monto/límite, badge y progress bar
- [ ] Widget en tamaño Large (4×4) muestra bank, "+" agrandado, monto hero, disponible, progress bar, nombre y badge
- [ ] Progress bar cambia de verde → amarillo → amber → rojo según el progreso
- [ ] Badge de fecha tiene fondo rojo si ≤3 días, verde si >7 días
- [ ] Botón "+" mide al menos 28×18dp
- [ ] Empty state muestra icono "+" dentro de Box + texto
- [ ] Income card usa colores semánticos en progress bar y porcentaje
- [ ] `./gradlew test` pasa sin errores

### Métricas de éxito
- Usuario ve montos sin abrir la app
- Badge de urgencia es inmediatamente reconocible por color
- Botón "+" es fácil de tocar sin error

---

## Notas y Aprendizajes

- Glance no soporta ImageVector ni drawable resources como iconos composables; usar Box con texto "+" es el workaround estándar
- Los colores semánticos deben estar en `WidgetColorScheme` como `Color` (no `ColorProvider`) para poder usarse dentro de `ColorProvider()` en runtime
- La altura del Medium layout se ajustó a 80dp para acomodar 4 filas de contenido sin sacrificar legibilidad

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-05-02 | Documento inicial |

---

## Referencias

- [ADR-012](widget/ADR-012-widget-income-summary-card.md) — Income Summary Card (precursora)
- [ADR-010](widget/ADR-010-widget-visual-refinement.md) — Refinamiento visual previo
- [ADR-009](widget/ADR-009-continuous-progress-bar.md) — Progress bar continua
- [ADR-008](widget/ADR-008-widget-card-data-computed-properties.md) — Propiedades computadas en WidgetCardData
