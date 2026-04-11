# ADR-045: Refactorización ImageCropCanvas — Un solo pointerInput con if/else por modo

**Fecha:** 2026-04-11  
**Estado:** Aceptado  
**Categoría:** ui  
**Prioridad:** High  
**Afecta:** `AddExpenseScreen.kt`, `ImageCropCanvas`, OCR feature

---

## Contexto

La función `ImageCropCanvas` (pantalla de recorte de imagen OCR) tenía un comportamiento errático al cambiar entre los modos "Zoom/Mover" e "Seleccionar":

1. **Dos detectores de gestos compitiendo** en el mismo `Box` padre:
   - `pointerInput(isDrawMode)` para zoom/pan
   - `pointerInput(isDrawMode)` para dibujo con hack de `PointerEventPass.Initial`

2. **Hack `PointerEventPass.Initial`**: El handler de dibujo interceptaba eventos en el Initial pass para ganar prioridad sobre `detectTransformGestures`. Frágil y difícil de mantener.

3. **Problema observable**: Al cambiar de modo, el estado del gesto anterior "se filtraba" — la imagen se desplazaba al iniciar un dibujo, o el rectángulo aparecía en posiciones incorrectas.

**Restricciones:**
- La funcionalidad de crop (conversión canvas → bitmap) debe permanecer exacta
- Las coordenadas de selección deben alinearse con `imageRect`
- Zoom/pan y dibujo no deben interferir entre sí
- **Estabilidad Visual**: La imagen no debe "saltar" ni cambiar de tamaño mientras el usuario arrastra el rectángulo de selección.

### Problema de Estabilidad (Bug de Layout Shift)

Incluso con la arquitectura de un solo Canvas, se detectó un error donde la imagen "saltaba" o se desplazaba erráticamente mientras el usuario dibujaba el rectángulo. 

**Causa del bug**:
1. El botón "Limpiar" aparecía condicionalmente vía `if (hasSelection)`.
2. Al arrastrar el rectángulo > 10px, `hasSelection` pasaba a true y el botón aparecía.
3. El `TextButton` tiene una altura mayor (~48dp) que los chips de modo (~32dp).
4. La fila de controles crecía, obligando al Canvas (con `.weight(1f)`) a **encogerse**.
5. El cambio de tamaño del Canvas disparaba un recalculo de `baseFitSize`, moviendo la imagen visualmente durante el gesto de dibujo.

### Intento fallido: Dos Canvas apilados

Se intentó separar en dos composables `Canvas` dentro de un `Box`:
- Canvas 1 (abajo): imagen + `pointerInput` para zoom/pan
- Canvas 2 (arriba, overlay): rectángulo + `pointerInput` para dibujo

**Por qué falló**: En Compose, dos composables hermanos apilados en un `Box` no se comportan igual que dos `.pointerInput()` en cadena sobre el mismo composable. Cuando el Canvas superior hace `return@pointerInput` sin consumir eventos, no hay garantía de que éstos lleguen al Canvas de abajo — el sistema de dispatch para hermanos superpuestos es diferente al de modificadores en serie. Resultado: zoom/pan dejó de funcionar completamente.

---

## Decisión

**Un único `Canvas` con un único `pointerInput(isDrawMode)`** que usa `if/else` para ramificar entre los dos modos.

### Opción elegida

```kotlin
Canvas(
    modifier = Modifier
        .fillMaxSize()
        .pointerInput(isDrawMode) {
            if (isDrawMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    selStart = down.position; selEnd = down.position
                    down.consume()
                    drag(down.id) { change -> selEnd = change.position; change.consume() }
                }
            } else {
                detectTransformGestures { centroid, pan, zoom, _ -> /* zoom/pan */ }
            }
        }
) {
    // Dibuja imagen EN PRIMER LUGAR, luego el rectángulo encima
    drawImage(...)
    if (s != null && e != null) { drawRect(...) }
}
```

**El mecanismo clave**: `pointerInput(isDrawMode)` — cuando `isDrawMode` cambia, Compose cancela la coroutine y la reinicia desde cero. El nuevo estado ejecuta solo el branch correcto (`if` o `else`), sin ninguna coroutine residual del modo anterior.

### Refinamiento para Estabilidad (Layout Invariante)

Para eliminar los saltos visuales, se aplicaron tres reglas de diseño defensivo:

1. **Altura de Control Fija**: El `Row` de chips y botones tiene ahora un `.height(48.dp)` explícito. Esto garantiza que el Canvas subyacente nunca cambie de tamaño, independientemente de qué botones aparezcan.
2. **Visibilidad vía Alpha**: En lugar de `if (hasSelection) { Button(...) }`, se usa `Modifier.alpha(if (hasSelection) 1f else 0f)`. El botón siempre ocupa espacio en el layout, evitando "sacudidas" cuando el usuario empieza a dibujar.
3. **Centrado Proporcional**: Se añadió lógica en un `LaunchedEffect(composableSize)` para que, si el canvas llegara a cambiar de tamaño (por ejemplo, al rotar el dispositivo), el `imageCenter` se ajuste proporcionalmente, manteniendo la imagen en la misma posición relativa.

### Por qué esta opción

- ✅ **Coroutine única**: Solo una rama corre a la vez — imposible que los detectores interfieran
- ✅ **Reset limpio**: El key `isDrawMode` garantiza que al cambiar de modo, el detector anterior se cancela completamente antes de que el nuevo inicie
- ✅ **API idiomática**: `awaitFirstDown()` + `drag()` reemplaza el hack `PointerEventPass.Initial`
- ✅ **Coordinadas garantizadas**: Un solo Canvas → el rectángulo y la imagen comparten el mismo espacio de coordenadas por definición
- ✅ **Sin overhead**: Un composable en lugar de dos

### Opciones rechazadas

**Opción A: Dos Canvas apilados en Box**
- ❌ Hermanos en Box no garantizan pass-through de eventos entre `pointerInput`
- ❌ Probado: zoom/pan dejó de funcionar al ser bloqueado por el Canvas superior

**Opción B: `PointerEventPass.Initial` hack (implementación original)**
- ❌ Frágil: depende del order de dispatch interno de Compose
- ❌ Estado de gesto se filtraba entre modos al cambiar rápido

**Opción C: `pointerInteropFilter`**
- ❌ API legada, menos control sobre eventos de Compose

---

## Consecuencias

### Directas

✅ **Zoom/pan funciona** — Un solo `pointerInput` activo en modo pan  
✅ **Dibujo estable** — Un solo `pointerInput` activo en modo dibujo  
✅ **Sin glitches al cambiar modo** — Coroutine se cancela y reinicia limpiamente  
✅ **Crop correcto** — Un Canvas = un espacio de coordenadas, sin desfases

### Técnicas

**Archivos impactados:**
- `app/src/main/java/com/alvaronolasco/creditcardtracker/ui/expenses/AddExpenseScreen.kt:994–1072`
  - `ImageCropCanvas`: un Canvas, un `pointerInput(isDrawMode)` con `if/else`

**Cambios estructurales:**
- Eliminados los dos `pointerInput` del `Box` padre (implementación original)
- Eliminado el intento de dos Canvas apilados
- Canvas único dibuja imagen primero, rectángulo de selección encima

**Sin breaking changes:**
- Firma de `ImageCropCanvas(...)` sin cambios
- Lógica de crop sin cambios (coordenadas en el mismo espacio)
- `onCropConfirm`, `onCancel` funcionan idénticamente

### Operacionales

- **Testing requerido**: Manual en emulador/dispositivo
- **Documentación**: Este ADR

---

## Implementación

### Cambios realizados

1. ✅ Un solo `Canvas` con `fillMaxSize()`
2. ✅ Un único `pointerInput(isDrawMode)` — key reinicia la coroutine al cambiar modo
3. ✅ `if (isDrawMode)` → `awaitEachGesture` + `awaitFirstDown()` + `drag()`
4. ✅ `else` → `detectTransformGestures` (zoom/pan sin competencia)
5. ✅ Canvas dibuja imagen y luego el rectángulo de selección encima (misma draw call, mismo coordinate space)

### Archivo de referencia

- Líneas 994–1072 de `AddExpenseScreen.kt`

---

## Validación

- [ ] Build `./gradlew assembleDebug` sin errores
- [ ] Zoom/pan funciona suavemente en modo "Zoom/Mover"
- [ ] Dibujar rectángulo en modo "Seleccionar" sin desplazar imagen
- [ ] Cambiar rápidamente entre modos varias veces → sin comportamiento extraño
- [ ] "Validar área" recorta correctamente el área seleccionada

---

## Notas y Aprendizajes

- **Hermanos vs modificadores**: En Compose, dos `.pointerInput()` en cadena sobre el mismo modificador se comunican diferente a dos composables hermanos apilados en Box. Los modificadores en cadena comparten el dispatch; los hermanos no.
- **Key de pointerInput**: `pointerInput(key)` es la forma idiomática de resetear detectores de gestos — cuando el key cambia, la coroutine se cancela y reinicia. No necesitas `PointerEventPass.Initial` si usas el key correctamente con `if/else`.
- **Un Canvas es suficiente**: El orden de `drawImage` → `drawRect` dentro del mismo Canvas garantiza que el rectángulo esté encima de la imagen, con coordenadas idénticas.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-11 | Documento inicial |
| 2026-04-11 | Corrección: intento de dos Canvas falló; solución final es un Canvas con if/else |
| 2026-04-11 | Mejora de estabilidad: fila de altura fija y alpha-visibility para evitar layout shifts durante el dibujo |

---

## Referencias

- [ADR-035](../architecture/ADR-035-ocr-processor-lifecycle-management.md) — OCR Processor Lifecycle Management
- [Compose PointerInput Docs](https://developer.android.com/jetpack/compose/input/pointer)
