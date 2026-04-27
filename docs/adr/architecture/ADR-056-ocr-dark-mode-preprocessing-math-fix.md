# ADR-056: OCR Dark-Mode Preprocessing Math Fix — Brightness Formula, Median Detection, Retry Fallback

**Fecha:** 2026-04-26  
**Estado:** Aceptado  
**Categoría:** architecture  
**Prioridad:** High  
**Afecta:** `OcrProcessor`, `OcrPreprocessingMathTest`

---

## Contexto

A pesar de los 6 fixes de ADR-055, el receipt Credicomer (fondo oscuro, texto claro, estructura `Monto\n$ 25.00`) seguía detectando `6` en lugar de `25.00`. El crop manual seguía fallando con confianza NONE.

ADR-055 corrigió la capa de **detección** (`AmountDetector`), pero el bug real estaba un nivel más abajo: en `preprocessBitmapForOcr()`. Cuando el preprocesamiento destruye la imagen antes de que ML Kit la vea, ninguna capa de detección puede ayudar.

### Root cause: fórmula matemática incorrecta en dark mode

`preprocessBitmapForOcr()` usa una `ColorMatrix` con esta fórmula para cada canal (R=G=B=v en escala de grises):

```
out = sign * v * contrast + brightness
```

Con los valores del dark mode **antes del fix** (`sign=-1`, `contrast=1.8`, `brightness=80`):

| Pixel de entrada v | Cálculo | Salida |
|---|---|---|
| 40 (fondo oscuro) | -72 + 80 | **8** (casi negro) |
| 200 (texto claro) | -360 + 80 = -280 | **0** (negro, clampeado) |
| 255 (texto blanco) | -459 + 80 = -379 | **0** (negro, clampeado) |

**Toda la imagen colapsa a negro.** ML Kit recibe un bitmap negro y devuelve texto basura (un "6" suelto de algún artefacto).

El valor correcto para invertir (fondo oscuro → blanco, texto claro → negro) requiere `brightness ≈ 255 * contrast + lightBrightness ≈ 399`, no `80`.

### Bug secundario: detección de dark mode por media sesgada

`calculateAverageBrightness()` usaba la **media** de ~2500 píxeles. El receipt Credicomer tiene un header blanco con el logo (≈20% del área, v≈255). Dependiendo de la proporción, la media podía superar 128 y clasificar la imagen como "light mode", aplicando preprocessing incorrecto al texto claro sobre fondo oscuro.

---

## Decisión

### Fix 1 — Corrección de la fórmula de brightness en dark mode

Derivar `brightness` desde el valor de light mode en lugar de usar una constante arbitraria:

```kotlin
val lightBrightness = if (isSmallImage) -40f else -60f
val brightness = if (isDarkMode) 255f * contrast + lightBrightness else lightBrightness
```

**Verificación matemática** con `contrast=1.8`, `lightBrightness=-60`, `brightness=399`:

| v | out = -1.8v + 399 | resultado |
|---|---|---|
| 40 (fondo) | -72 + 399 = 327 | 255 (blanco) ✓ |
| 200 (texto) | -360 + 399 = 39 | 39 (oscuro) ✓ |
| 255 (texto) | -459 + 399 = -60 | 0 (negro) ✓ |

El mismo principio aplica para imágenes pequeñas (`contrast=1.4`, `brightness=317`).

### Fix 2 — Detección de dark mode por mediana en lugar de media

Reemplazar la media aritmética con la mediana de las muestras:

```kotlin
samples.sort()
return samples[samples.size / 2]
```

La mediana ignora regiones brillantes minoritarias (logo, banner) y refleja el tono dominante de la imagen. No añade dependencias ni cambia la interfaz.

### Fix 3 — Retry sin filtro de color como safety net

Si el primer pase produce `Confidence.NONE` o `LOW`, reintentar con el bitmap sin el `ColorMatrix` (solo resize):

```kotlin
private suspend fun runRecognition(bitmap: Bitmap, useFilter: Boolean, logTag: String): OcrResult
```

Ambos `processImage()` y `processImageBitmap()` usan este helper. El retry se activa solo en el camino de fallo, no en producción normal. `raw.recycle()` se difiere hasta después del retry para evitar acceso a bitmap reciclado.

### Fix 4 — Regression test en JVM puro

`OcrPreprocessingMathTest.kt` verifica la fórmula del `ColorMatrix` sin SDK de Android:
- Dark mode: fondo v=40 → bright (≥200), texto v=200 → dark (≤60)
- Documenta el bug anterior: `brightness=80` colapsa todo a 0
- Light mode: simetría correcta
- 8 tests, sin Bitmap/Canvas/Context

---

## Consecuencias

### Directas
- ✅ Receipt Credicomer `Monto\n$ 25.00` detecta `25.00` con `Confidence.HIGH`
- ✅ Crop manual del área del monto funciona correctamente
- ✅ Header blanco en receipt oscuro ya no sesga la detección de modo
- ✅ Retry sin filtro recupera detección cuando preprocessing es contraproducente
- ⚠️ Retry implica una segunda llamada a ML Kit en casos de confianza baja — costo asumido

### Técnicas
**Archivos impactados:**
- `app/src/main/java/.../ocr/OcrProcessor.kt` — Fix 1, 2, 3
- `app/src/test/java/.../ocr/OcrPreprocessingMathTest.kt` — Fix 4 (nuevo)

**Breaking changes:** Ninguno. Interfaz pública sin cambios.

### Operacionales
- Testing: `./gradlew :app:testDebugUnitTest` — todos los tests pasan (BUILD SUCCESSFUL)
- Validación manual: receipt Credicomer detecta `25.00`, crop manual detecta `25.00`

---

## Por qué ADR-055 no fue suficiente

ADR-055 operó en la capa correcta (AmountDetector) pero asumió que el texto llegaba intacto a esa capa. El bug estaba en la capa anterior (preprocessing → ML Kit) y era invisible a los tests de texto puro. Los tests unitarios de `OcrAmountDetectorTest` usan `detectFromText()` que bypasea el preprocessing completamente, por eso pasaban los 24 tests mientras la app real fallaba.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-26 | Fix 1: brightness formula. Fix 2: median brightness. Fix 3: retry fallback. Fix 4: regression test |

---

## Referencias

- [ADR-055](ADR-055-ocr-receipt-edge-case-fixes.md) — Intento anterior (detector-side fixes)
- [ADR-043](ADR-043-ocr-dark-mode-and-scoped-correction.md) — Dark mode preprocessing (origen del bug)
