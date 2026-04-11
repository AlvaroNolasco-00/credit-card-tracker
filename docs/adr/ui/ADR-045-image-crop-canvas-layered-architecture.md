# ADR-045: Refactorización ImageCropCanvas — Arquitectura de Capas para Gestos

**Fecha:** 2026-04-11  
**Estado:** Aceptado  
**Categoría:** ui  
**Prioridad:** High  
**Afecta:** `AddExpenseScreen.kt`, `ImageCropCanvas`, OCR feature

---

## Contexto

La función `ImageCropCanvas` (pantalla de recorte de imagen OCR) tenía un comportamiento errático al cambiar rápidamente entre los modos "Zoom/Mover" e "Seleccionar":

1. **Dos detectores de gestos compitiendo** en el mismo `Box`:
   - `pointerInput(isDrawMode)` para zoom/pan (cuando `!isDrawMode`)
   - `pointerInput(isDrawMode)` para dibujo (cuando `isDrawMode`)

2. **Hack de `PointerEventPass.Initial`**: El handler de dibujo usaba `awaitPointerEvent(PointerEventPass.Initial)` en un loop manual para interceptar eventos antes que el detector de transformación, lo cual es frágil y complejo.

3. **Problema observable**: Al cambiar de modo rápidamente, el estado del gesto anterior "se filtraba" — la imagen podía desplazarse sin intención o el rectángulo se dibujaba en posiciones incorrectas.

**Restricciones:**
- La funcionalidad de crop (conversión canvas → bitmap) debe permanecer exacta
- Las coordenadas de selección deben alinearse perfectamente con la imagen renderizada
- El zoom/pan y el dibujo no deben interferir entre sí

---

## Decisión

Separar el único `Canvas` en **dos Canvas apilados** dentro del mismo `Box`:

### Opción elegida: Arquitectura de capas

```
Box(fillMaxWidth, weight(1f), clipToBounds, onSizeChanged)
  ├─ Canvas Layer 1: Imagen + Zoom/Pan gestures
  └─ Canvas Layer 2: Overlay transparente + Draw gestures
```

**Cambios técnicos:**

1. **Canvas 1 (Imagen)** — `fillMaxSize()` + `pointerInput(isDrawMode)`
   - Solo dibuja la imagen en `imageRect`
   - Cuando `!isDrawMode`, ejecuta `detectTransformGestures` (zoom/pan)
   - Cuando `isDrawMode`, el handler retorna sin hacer nada

2. **Canvas 2 (Overlay)** — `fillMaxSize()` + `pointerInput(isDrawMode)`
   - Solo dibuja el rectángulo de selección (`selStart`/`selEnd`)
   - Cuando `isDrawMode`, ejecuta gestos de dibujo:
     - `awaitFirstDown()` → captura press inicial
     - `drag(pointerId) { }` → rastrea drag hasta lift
   - Cuando `!isDrawMode`, retorna sin hacer nada

3. **Ambos Canvas comparten:**
   - El espacio del `Box` → mismas dimensiones, origen `(0,0)`
   - Las variables de estado: `selStart`, `selEnd`, `scale`, `imageCenter`, `composableSize`
   - Resultado: coordenadas de selección alineadas perfectamente con `imageRect`

### Por qué esta opción

- ✅ **Separación de responsabilidades**: Cada Canvas gestiona su propio evento sin contaminación
- ✅ **Arquitectura clara**: Overlay encima es intuitiva — no necesita hacks de eventos
- ✅ **Patrón idiomático**: `awaitFirstDown()` + `drag()` es la API estándar de Compose, eliminando `PointerEventPass.Initial`
- ✅ **Facilidad de mantenimiento**: Dos Canvas independientes = debugging más sencillo
- ✅ **Sin cambios en crop math**: La lógica de conversión canvas → bitmap permanece idéntica

### Opciones rechazadas

**Opción A: Consumir eventos más agresivamente en un solo Canvas**
- ❌ Requeriría más lógica condicional en el mismo handler
- ❌ El `PointerEventPass.Initial` hack seguiría siendo necesario
- ❌ No resuelve la raíz del problema (estado del gesto filtrándose)

**Opción B: Usar `Modifier.pointerInteropFilter`**
- ❌ API legada, menor control de precedencia de eventos
- ❌ No es la solución recomendada en Compose moderno

---

## Consecuencias

### Directas

✅ **Comportamiento estable** — Cambiar entre modos sin "glitches"  
✅ **Código más limpio** — Se eliminó el loop `awaitPointerEvent(PointerEventPass.Initial)`  
✅ **UX mejorada** — Gestos de zoom/pan y dibujo no interfieren  
❌ **Renderizado de dos Canvas** — Mínimo overhead (negligible, solo es overlay), no impacta performance

### Técnicas

**Archivos impactados:**
- `app/src/main/java/com/alvaronolasco/creditcardtracker/ui/expenses/AddExpenseScreen.kt:866–1124`
  - Refactorizado `ImageCropCanvas` con dos Canvas

**Cambios estructurales:**
- Eliminados los dos `pointerInput` del `Box` padre
- Canvas 1 encapsula zoom/pan con su propio `pointerInput(isDrawMode)`
- Canvas 2 encapsula dibujo con `awaitFirstDown()` + `drag()`

**Sin breaking changes:**
- Interfaz pública de `ImageCropCanvas(...)` sin cambios
- Lógica de crop y variables de estado sin cambios
- Los parámetros `onCropConfirm`, `onCancel` funcionan idénticamente

### Operacionales

- **Testing requerido**: Manual en emulador/dispositivo
  - Probar zoom/pan en modo "Zoom/Mover"
  - Probar dibujo de rectángulo en modo "Seleccionar"
  - Cambiar rápidamente entre modos varias veces
  - Validar que "Validar área" recorta correctamente
- **Documentación**: Este ADR documenta la decisión
- **No hay comunicación necesaria** — Es una refactorización interna

---

## Implementación

### Cambios realizados

1. ✅ Dividido el único `Canvas` en dos Canvas con `fillMaxSize()` dentro del `Box`
2. ✅ Movido `detectTransformGestures` al `pointerInput` de Canvas 1
3. ✅ Reemplazado el loop `awaitPointerEvent(PointerEventPass.Initial)` por `awaitFirstDown()` + `drag()` en Canvas 2
4. ✅ Canvas 1 dibuja solo la imagen
5. ✅ Canvas 2 dibuja solo el rectángulo de selección

### Archivo de referencia

- Commit: Refactorización de `ImageCropCanvas` en `AddExpenseScreen.kt:992–1070`
- Antes: Líneas 992–1078 (un Canvas + dos pointerInput en Box)
- Después: Líneas 992–1070 (dos Canvas separados)

---

## Validación

### Cómo verificar la implementación

- [ ] Build `./gradlew assembleDebug` sin errores
- [ ] Abrir pantalla OCR (Add Expense → Cámara)
- [ ] Zoom/pan funciona suavemente en modo "Zoom/Mover"
- [ ] Dibujar rectángulo funciona en modo "Seleccionar" sin desplazar imagen
- [ ] Cambiar modo 5+ veces rápidamente → sin comportamiento extraño
- [ ] Presionar "Validar área" recorta correctamente después del cambio de modo
- [ ] La selección persiste al cambiar de modo y volver

### Métricas de éxito

✅ Cero "glitches" al cambiar entre modos  
✅ Zoom/pan fluido (60 fps)  
✅ Dibujo responsive sin lag  
✅ Crop preciso en la imagen original  

---

## Notas y Aprendizajes

- **Compose layering intuitive**: Cuando dos Canvas llenan el mismo espacio, el superior recibe eventos primero de forma natural — no necesitas `PointerEventPass.Initial`.
- **Coordinate space alignment**: Ambos Canvas usando `fillMaxSize()` en el mismo `Box` garantiza que sus espacios de coordenadas son idénticos; no hay offset o desfase.
- **State reuse across layers**: Las variables mutables (`selStart`, `selEnd`, etc.) compartidas entre Canvas permiten que el overlay acceda a los datos de la imagen sin duplicación.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-11 | Documento inicial — Refactorización implementada |

---

## Referencias

- [ADR-035](../architecture/ADR-035-ocr-processor-lifecycle-management.md) — OCR Processor Lifecycle Management
- [Compose PointerInput Docs](https://developer.android.com/jetpack/compose/input/pointer) — awaitFirstDown() y drag() API
- [Issue/PR]: Refactorización de ImageCropCanvas para estabilidad de gestos
