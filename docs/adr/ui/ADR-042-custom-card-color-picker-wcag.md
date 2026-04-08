# ADR-042: Selector de color de tarjeta personalizado con WCAG Contrast Ratio

**Fecha:** 2026-04-08
**Estado:** Aceptado
**Categoría:** ui
**Prioridad:** High
**Afecta:** AddEditCardScreen, CardGradients, WidgetColorScheme, CreditCardWidget
**Supersedes:** ADR-018

---

## Contexto

El sistema de colores de tarjetas tenía varias limitaciones:
1. **Colores limitados**: Solo 7 colores predefinidos (gradients) sin opción de personalización
2. **Gradientes innecesarios**: Las tarjetas usaban gradientes verticales (Light/Mid/Dark) que no aportaban valor visual significativo
3. **Contraste hardcodeado**: ADR-018 implementó colores de texto fijos (blanco/negro) basados en reglas manuales, sin algoritmo de accesibilidad estándar
4. **Sin accesibilidad WCAG**: No se cumplía con los estándares WCAG 2.1 para contraste de texto

Los usuarios solicitaban:
- Capacidad de elegir cualquier color de tarjeta (no limitado a 7 opciones)
- Colores sólidos más simples y modernos
- Accesibilidad automática según estándares internacionales

---

## Decisión

### Opción elegida

Implementar un sistema completo de selección de color con:

1. **WCAGContrastUtil**: Nueva utilidad que implementa el algoritmo WCAG 2.1 para:
   - Calcular luminancia relativa según fórmula estándar
   - Calcular contrast ratio entre dos colores
   - Determinar color de texto óptimo (priorizando colores "familiares" al fondo que cumplan AA 4.5:1, fallback a blanco/negro)

2. **Colores sólidos**: Eliminar gradientes, mantener 7 colores base como sólidos:
   - CardRed (#FF4242), CardYellow (#FFFF42), CardBlue (#4265FF)
   - CardGreen (#42FF45), CardPurple (#D342FF), CardOrange (#FF8C42), CardDark (#2C2C3A)

3. **CardColorPicker**: Nuevo componente con:
   - Paleta de 7 colores predefinidos (círculos 44dp)
   - Botón "+" para color personalizado
   - Dialog con sliders HSV (Tono 0-360°, Saturación 0-1, Brillo 0-1)
   - Preview en tiempo real del color seleccionado
   - Display del valor hexadecimal

4. **CardGradients simplificado**:
   - `getBrushForColor()`: Retorna brush sólido (sin gradientes)
   - `getTextColorForCard()`: Usa `WCAGContrastUtil.getOptimalTextColor()` en lugar de lógica hardcodeada

### Por qué esta opción

- **Accesibilidad automática**: Algoritmo WCAG garantiza contraste AA 4.5:1 para texto normal
- **Flexibilidad**: Usuarios pueden elegir cualquier color, no limitado a 7 opciones
- **Simplicidad visual**: Colores sólidos más modernos y limpios que gradientes
- **Extensibilidad**: Sistema preparado para futuras mejoras (AAA 7:1, temas dinámicos)
- **Mantenibilidad**: Algoritmo genérico vs reglas hardcodeadas por color

### Opciones rechazadas

**Opción A: Mantener gradientes + agregar más colores**
- ❌ Complejidad innecesaria: gradientes no aportan valor visual significativo
- ❌ Dificultad para generar gradientes coherentes para colores personalizados
- ❌ Widget requiere drawables específicos por color, no escalable

**Opción B: Color picker nativo de Android (Material3)**
- ❌ Disponible solo en Android 12+, incompatibilidad con versiones anteriores
- ❌ Menos control sobre UX (preview, hex display, validación)
- ❌ Implementación HSV personalizada más consistente con diseño de app

**Opción C: Solo blanco/negro según luminancia**
- ❌ Perdemos oportunidad de usar colores familiares que mantienen identidad visual
- ❌ Solución de ADR-018 era suficiente para 7 colores, no para personalización total

---

## Consecuencias

### Directas
- ✅ Usuarios pueden elegir cualquier color de tarjeta (personalización total)
- ✅ Contraste de texto cumple WCAG AA 4.5:1 automáticamente
- ✅ Colores sólidos más limpios y modernos
- ✅ Sistema preparado para futuras mejoras de accesibilidad (AAA)
- ⚠️ ADR-018 queda obsoleto (lógica hardcodeada reemplazada por algoritmo genérico)

### Técnicas

**Archivos/módulos impactados:**
- `app/src/.../ui/theme/WCAGContrastUtil.kt` — **Nuevo**: Algoritmo WCAG 2.1
- `app/src/.../ui/theme/Color.kt` — Eliminados gradientes (_Light, _Mid, _Dark)
- `app/src/.../ui/theme/CardGradients.kt` — Simplificado, usa WCAGContrastUtil
- `app/src/.../ui/components/CardColorPicker.kt` — **Nuevo**: Componente selector
- `app/src/.../ui/cards/AddEditCardScreen.kt` — Usa CardColorPicker
- `app/src/.../widget/WidgetColorScheme.kt` — Adaptado a colores sólidos
- `app/src/.../widget/CreditCardWidget.kt` — Usa cardColor() en lugar de gradientes

**Breaking changes:**
- Tarjetas existentes con gradientes ahora se muestran como colores sólidos (migración automática, sin pérdida de datos)
- ADR-018 reemplazado por algoritmo genérico WCAG

### Operacionales
- Testing requerido: Validar contraste WCAG en dispositivo real para colores extremos
- Documentación: Actualizar screenshots en documentación de usuario
- Comunicación: Usuarios pueden notar cambio visual (gradientes → sólidos)

---

## Implementación

### Paso a paso
1. Crear `WCAGContrastUtil.kt` con funciones de luminancia y contraste WCAG
2. Actualizar `Color.kt`: Eliminar Card*Light/Mid/Dark, mantener 7 colores sólidos
3. Actualizar `CardGradients.kt`: Simplificar getBrushForColor() a sólido, reemplazar getTextColorForCard() con WCAGContrastUtil
4. Crear `CardColorPicker.kt`: Componente con paleta + dialog HSV
5. Actualizar `AddEditCardScreen.kt`: Reemplazar Row de colores con CardColorPicker
6. Actualizar `WidgetColorScheme.kt`: cardColor() retorna ColorProvider sólido
7. Actualizar `CreditCardWidget.kt`: Usar cardColor() en SmallCardRow, MiniCardRow, FullCardItem

### Files de referencia
- Commit: Implementación selector color WCAG
- Tests: `app/src/test/.../WCAGContrastUtilTest.kt` (pendiente)

---

## Validación

### Cómo verificar que la decisión se implementó correctamente
- [ ] Selector muestra 7 colores predefinidos + botón "+"
- [ ] Botón "+" abre dialog con sliders HSV funcionales
- [ ] Preview de color actualiza en tiempo real
- [ ] Display hexadecimal muestra valor correcto
- [ ] Color personalizado se guarda correctamente en tarjeta
- [ ] Texto sobre tarjeta cumple WCAG AA 4.5:1 (validar con colores extremos: #FFFFFF, #000000, #FF0000, #00FF00, #0000FF)
- [ ] Widget muestra colores sólidos (no gradientes)
- [ ] Tarjetas existentes migran a sólidos sin errores

### Métricas de éxito
- Contraste ratio >= 4.5:1 para todos los colores seleccionables
- Tiempo de respuesta de selector < 100ms
- Sin crashes en logs al cambiar color
- Usuarios pueden seleccionar cualquier color RGB

---

## Notas y Aprendizajes

- ADR-018 fue un primer paso necesario para resolver contraste en 7 colores fijos, pero no escalaba para personalización
- Algoritmo WCAG permite generar colores "familiares" (similar hue) ajustados para cumplir contraste, manteniendo identidad visual
- HSV es más intuitivo que RGB para usuarios finales (Tono, Saturación, Brillo)
- Widget Glance usa ColorProvider para colores dinámicos, no drawables estáticos
- Considerar migrar a WCAG AAA (7:1) en futuro para mejor accesibilidad

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-08 | Documento inicial |

---

## Referencias

- [ADR-018](ADR-018-card-text-color-contrast.md) — Supersede: Color dinámico hardcodeado → Algoritmo WCAG genérico
- [WCAG 2.1 Contrast](https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html) — Estándar de accesibilidad
- [Relative Luminance Formula](https://www.w3.org/WAI/GL/wiki/Relative_luminance) — Fórmula de luminancia WCAG
