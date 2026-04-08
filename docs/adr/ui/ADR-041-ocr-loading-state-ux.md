# ADR-041: OCR Loading State UX Improvement

**Fecha:** 2026-04-08  
**Estado:** Aceptado  
**Categoría:** ui  
**Prioridad:** Medium  
**Afecta:** `AddExpenseScreen`, `ExpensesViewModel`, `OcrProcessor`

---

## Contexto

Cuando el usuario selecciona una imagen desde cámara o galería, el `OcrProcessor` realiza el reconocimiento OCR de forma asincrónica. Durante este proceso (que puede tardar 2-5 segundos), la UI mostraba apenas un `CircularProgressIndicator` flotante sin contexto visual claro, generando incertidumbre sobre qué está pasando.

Esto fue identificado como un dolor de UX:
- El spinner flotaba sin overlay, causando ambigüedad visual
- No había comunicación textual de qué proceso se estaba ejecutando
- El usuario podía accidentalmente disparar múltiples OCRs consecutivos (galería → cámara mientras se procesaba)
- La posibilidad de guardar el gasto mientras OCR aún corría podría generar inconsistencias

---

## Decisión

Se mejora la pantalla `AddExpenseScreen` para proporcionar **retroalimentación visual clara durante el procesamiento OCR** mediante:

### Opción elegida: Overlay oscuro + spinner + texto descriptivo

1. **Overlay semitransparente (55% alpha)** sobre la imagen cuando `ocrProcessing = true`
   - Box negro con alpha 0.55 cubre toda la imagen
   - Proporciona contexto visual de que algo está sucediendo sin perder la vista del ticket

2. **Spinner blanco + texto "Analizando recibo..."**
   - `CircularProgressIndicator` de 36dp en blanco (contraste contra fondo oscuro)
   - Texto descriptivo **"Analizando recibo..."** en Material3 bodySmall, centrado, 2 líneas
   - Columna centrada en el Box para alineación perfecta

3. **Deshabilitar acciones conflictivas**
   - Botones "Tomar Foto" y "Galería" deshabilitados durante OCR
   - Botón "Guardar Gasto" deshabilitado mientras `ocrProcessing = true`
   - Previene flujos paralelos conflictivos

### Por qué esta opción
- **Claridad:** El overlay oscuro es una convención mobile estándar (Material Design) que comunica "proceso en curso"
- **Accesibilidad:** Texto descriptivo beneficia usuarios que dependen de información semántica (screen readers)
- **Prevención de bugs:** Deshabilitar botones evita race conditions (múltiples OCRs simultáneos, guardado prematuro)
- **UX consistente:** Usa el color scheme existente (Material3 dark mode aware), sin nuevos estilos hardcoded

### Opciones rechazadas

**Opción A: Mostrar modal/dialog bloqueante**
- ❌ Demasiado intrusivo, oculta completamente la imagen
- ❌ Va contra la convención de Material3 (dialogs son para decisiones, no para estados transitorios)

**Opción B: Toast/Snackbar informativo únicamente**
- ❌ Desaparece automáticamente después de N segundos, insuficiente feedback para proceso largo (2-5s)
- ❌ No previene acciones conflictivas (usuario aún puede tocar botones durante OCR)

**Opción C: Skeleton loading o placeholder animado**
- ❌ Demasiado light-weight para un proceso crítico que determina el monto del gasto
- ❌ Requeriría assets adicionales o lógica de animación compleja

---

## Consecuencias

### Directas
- ✅ Usuario recibe feedback claro de que se está procesando OCR
- ✅ Prevención de flujos paralelos (múltiples OCRs, guardado prematuro)
- ✅ Mejora de UX sin cambios en lógica de negocio
- ✅ Implementación mínima (Box + CircularProgressIndicator + Text)
- ⚠️ Oculta temporalmente la imagen durante el procesamiento (usuarios no pueden verla mientras se analiza)
- ❌ Si el OCR falla o toma mucho tiempo, el usuario verá solo el overlay sin error recovery visible

### Técnicas
**Archivos impactados:**
- `app/src/main/java/com/alvaronolasco/creditcardtracker/ui/expenses/AddExpenseScreen.kt` — Reemplazo del Box que muestra CircularProgressIndicator con versión mejorada (overlay + texto)
- `CHANGELOG.md` — Entrada en [Unreleased] → Added
- `docs/adr/INDEX.md` — Nuevo ADR

**Breaking changes:** Ninguno. El cambio es puramente visual, la API de `ExpensesViewModel.processOcr()` no cambia.

### Operacionales
- Testing requerido: Manual en dispositivo/emulator con imágenes de distintos contraste
- Documentación: Este ADR (no requiere actualización de CLAUDE.md)
- Métricas: Observar en logs si el usuario toma fotos mientras OCR está activo (debe estar bloqueado)

---

## Implementación

### Paso a paso
1. ✅ Reemplazar Box simple con Box(overlay) + Column(spinner + texto)
2. ✅ Agregar `enabled = !uiState.ocrProcessing` a botones "Tomar Foto" y "Galería"
3. ✅ Agregar `enabled = !uiState.ocrProcessing` al botón "Guardar Gasto"
4. Validar en emulator con imágenes reales (recibos, facturas)
5. Verificar que spinner + texto son legibles en modo claro y oscuro

### Archivos de referencia
- Commit: `[próximo]` — UX improvement: add OCR loading state overlay
- PR: [Pendiente]

---

## Validación

### Cómo verificar que la decisión se implementó correctamente
- [ ] Selecciono imagen desde galería, veo overlay oscuro + "Analizando recibo..."
- [ ] Los botones "Tomar Foto" y "Galería" están deshabilitados (grayed out) durante OCR
- [ ] Botón "Guardar Gasto" no se puede presionar mientras OCR está en curso
- [ ] Una vez que OCR termina, el overlay desaparece y el monto se muestra en el campo de entrada
- [ ] Spinner y texto son legibles en modo claro y oscuro

### Métricas de éxito
- Usuarios no disparan múltiples OCRs accidentalmente (botones bloqueados)
- Tiempo de exposición al estado confuso se reduce a 0 (ahora hay feedback claro)
- No hay crashes relacionados a race conditions en OCR

---

## Notas y Aprendizajes

- **Material Design Loading States:** Los overlays semitransparentes (40-55% alpha) son la convención estándar para operaciones asincrónicas
- **State Management:** Disabling buttons es más efectivo que hide/show para prevenir acciones conflictivas
- **A/B Testing:** Podría medirse si usuarios se sienten más confiados con el nuevo estado (survey post-OCR)
- **Future work:** Cuando OCR falle, mostrar error overlay diferenciado (rojo) en lugar de desaparecer silenciosamente

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-08 | Documento inicial + implementación completada |

---

## Referencias

- [ADR-036](../architecture/ADR-036-ocr-amount-scoring-system.md) — OCR Scoring System (decision previa del OCR)
- [ADR-040](../architecture/ADR-040-ocr-correction-pipeline-and-perf-fixes.md) — OCR Pipeline Fixes (optimización de performance)
- [Material Design — Progress Indicators](https://m3.material.io/components/progress-indicators) — Guía de Material 3 para loaders
- [Android Async UX Patterns](https://developer.android.com/design/patterns/loading) — Patrones recomendados de loading
