# ADR-055: OCR Receipt Edge-Case Fixes — Fechas, Tarjetas, Normalización de Keywords, Boost de Proximidad y Preprocessing de Imágenes Pequeñas

**Fecha:** 2026-04-26  
**Estado:** Aceptado  
**Categoría:** architecture  
**Prioridad:** High  
**Afecta:** `OcrProcessor`, `AmountDetector`, `OcrAmountDetectorTest`

---

## Contexto

Un receipt digital (fondo oscuro, texto blanco) de WOMPI dejó de detectarse correctamente tras los cambios de ADR-052. La estructura del receipt es:

```
Monto
$ 25.00
```

El usuario reporta dos problemas:
1. **Detección inicial incorrecta:** Se detecta un valor de `6` en lugar de `25.00`.
2. **Crop manual falla:** Al seleccionar manualmente el área del monto ("Monto" + "$ 25.00"), el OCR retorna "no se pudo detectar el total".

Tras análisis del pipeline completo (`processImage` con preprocessing + `detect` con 6 capas), se identificaron **6 bugs/gaps**:

### Problemas de detección (AmountDetector)
1. **Date filtering defectuoso:** `looksLikeNonMonetary` verifica `datePatterns` solo contra `matchStr` (el número capturado, ej: `"25"`), no contra la línea completa (`"25/04/2026"`).
2. **Números de tarjeta no filtrados:** `"**** **** **** 4399"` genera candidato `4399.0`.
3. **Keywords con errores OCR no detectados:** Si ML Kit lee `"Monto"` como `"M0nt0"`, el keyword search falla.
4. **Falta de boost para amounts con moneda cerca de keywords.**

### Problemas de preprocessing (OcrProcessor)
5. **No hay tamaño mínimo para bitmaps pequeños:** Cuando el usuario hace crop de solo el área del monto, el bitmap resultante puede ser ~200x100px. ML Kit necesita texto de ~18-20px de alto mínimo para reconocerlo. El preprocessing original solo escalaba hacia abajo imágenes >2048px, dejando las imágenes pequeñas sin cambios.
6. **Contraste/brightness agresivo en imágenes pequeñas:** Los valores fijos de `contrast=1.8` y `brightness=-60/+80` pueden destruir los bordes anti-aliased del texto en imágenes pequeñas, haciendo que ML Kit no pueda reconocer los caracteres.

---

## Decisión

Aplicar 4 fixes encadenados en `AmountDetector` sin cambiar la interfaz pública.

### Opción elegida

**Fix 1 — Date filtering sobre `contextStr`:**
Cambiar `datePatterns.any { it.containsMatchIn(matchStr) }` a `contextStr.isNotBlank() && datePatterns.any { it.containsMatchIn(contextStr) }`. Una fecha solo es relevante como filtro cuando el número aparece dentro de una fecha completa en el contexto.

**Fix 2 — Patrones de tarjeta de crédito:**
Agregar `cardNumberPatterns` con regex para números enmascarados (`**** 4399`) y completos (`1234-5678-9012-3456`). Verificar en `looksLikeNonMonetary` contra `contextStr`.

**Fix 3 — Normalización OCR para keywords:**
Crear `normalizeForKeywords(text: String)` que aplica correcciones OCR inversas (0→O, 1→l, 5→S) antes de keyword matching. Esto compensa errores comunes en receipts digitales con fondo oscuro donde letras redondeadas se confunden con dígitos.

**Fix 4 — Nueva capa de detección `findAmountsWithCurrencyNearKeywordsScored`:**
Score 60 para montos que tengan símbolo de moneda explícito en líneas dentro de 2 líneas de distancia de un keyword total. Esto captura el patrón común `Keyword\n$ Amount` en receipts digitales.

**Fix 5 — Tamaño mínimo para bitmaps pequeños (upscaling):**
Agregar `minDim = 512` en `preprocessBitmapForOcr()`. Si el bitmap de entrada es menor a 512px en cualquier dimensión, se escala hacia arriba proporcionalmente. Esto asegura que ML Kit reciba una imagen con resolución suficiente para reconocer caracteres (texto de ~18-20px mínimo).

**Fix 6 — Contraste/brightness adaptativo para imágenes pequeñas:**
Para imágenes < 1000px, usar valores más conservadores: `contrast=1.4` (vs 1.8) y `brightness=-40/+50` (vs -60/+80). Esto evita que el procesamiento agresivo destruya los bordes anti-aliased del texto en crops pequeños.

### Por qué esta opción

- **Mínima intrusión:** Los 6 fixes son adiciones o cambios localizados; no cambian la interfaz pública.
- **Sin dependencias externas:** Todo se resuelve con API nativa de Android (ColorMatrix, Canvas, Bitmap).
- **Tests cubiertos:** Los fixes de AmountDetector tienen tests unitarios; los fixes de preprocessing se validan con logging y pruebas manuales.
- **Escalable:** Las constantes (minDim, contrast, brightness) son fáciles de calibrar con datos reales.

### Opciones rechazadas

**Opción A: Usar OpenCV para mejorar preprocessing de receipts digitales**
- ❌ Agregaría dependencia nativa pesada solo para un edge case.
- ❌ El preprocessing actual ya funciona; el problema está en la detección, no en la imagen.

**Opción B: Machine Learning para clasificar líneas del receipt**
- ❌ Overkill para el problema actual.
- ❌ Requeriría entrenamiento con dataset de receipts locales.

**Opción C: No hacer nada y esperar a más datos de producción**
- ❌ El receipt WOMPI es un caso real de usuario; dejarlo roto afecta UX.
- ❌ Los 4 bugs son claramente identificables y fixeables.

---

## Consecuencias

### Directas
- ✅ Receipts digitales con estructura `Keyword\n$ Amount` ahora se detectan correctamente.
- ✅ Fechas en receipts ya no generan candidatos falsos.
- ✅ Números de tarjeta enmascarados ya no confunden al detector.
- ✅ Keywords con errores OCR (0→O, 1→l, 5→S) aún se detectan.
- ✅ Crops manuales pequeños ahora se escalan hacia arriba para asegurar resolución suficiente para ML Kit.
- ✅ Preprocessing más conservador en imágenes pequeñas evita destruir bordes de texto anti-aliased.
- ⚠️ `normalizeForKeywords` reemplaza globalmente; podría afectar textos donde `"0"` es intencional en una palabra. Trade-off aceptable.
- ⚠️ Upscaling de imágenes pequeñas puede introducir artifacts de interpolación, pero es preferible a que ML Kit no reconozca nada.

### Técnicas
**Archivos/módulos impactados:**
- `app/src/main/java/.../ocr/OcrProcessor.kt` — 4 fixes en `AmountDetector` + 2 fixes en `preprocessBitmapForOcr()`
- `app/src/test/java/.../ocr/OcrAmountDetectorTest.kt` — 7 tests nuevos
- `docs/adr/architecture/ADR-055-ocr-receipt-edge-case-fixes.md` — Este registro

**Breaking changes:**
- Ninguno. La interfaz pública (`detect`, `detectFromText`, `processImage`, `processImageBitmap`) no cambia.

### Operacionales
- Testing requerido: automated (`./gradlew test --tests OcrAmountDetectorTest`) + manual (probar crop manual en dispositivo)
- Documentación: este ADR + CHANGELOG.md
- Comunicación: N/A (fix interno)

---

## Implementación

### Paso a paso

#### Fixes de AmountDetector (líneas 230-570)
1. Agregar `cardNumberPatterns` con regex para tarjetas enmascaradas y completas.
2. Modificar `looksLikeNonMonetary` para verificar `datePatterns` y `cardNumberPatterns` contra `contextStr`.
3. Crear `normalizeForKeywords(text)` con reemplazos 0→O, 1→l, 5→S.
4. Aplicar `normalizeForKeywords` en `findByKeywordsScored` y `findByGeometricAlignmentScored`.
5. Agregar constante `SCORE_CURRENCY_NEAR_KEYWORD = 60`.
6. Crear `findAmountsWithCurrencyNearKeywordsScored(text)` con lógica de proximidad de 2 líneas.
7. Integrar nueva capa en `detect()` y `detectFromText()`.

#### Fixes de preprocessing (líneas 84-160)
8. Agregar `minDim = 512` en `preprocessBitmapForOcr()` para upscaling de imágenes pequeñas.
9. Agregar lógica de `isSmallImage` para usar valores de contraste/brightness más conservadores.
10. Agregar logging con `Log.d` para debuggear dimensiones de entrada/salida y texto de ML Kit.

#### Tests y validación
11. Agregar 7 tests unitarios en `OcrAmountDetectorTest.kt`.
12. Validar que los 24+ tests existentes siguen pasando.
13. Probar manualmente crop en dispositivo con receipt WOMPI.

### Files de referencia
- PR: (pendiente)
- Commit: (pendiente)
- Tests: `app/src/test/java/com/alvaronolasco/creditcardtracker/ocr/OcrAmountDetectorTest.kt`

---

## Validación

### Cómo verificar que la decisión se implementó correctamente
- [ ] `./gradlew test --tests OcrAmountDetectorTest` pasa todos los tests (24 existentes + 7 nuevos)
- [ ] Receipt WOMPI con estructura `Monto\n$ 25.00` detecta `25.00` con `Confidence.HIGH`
- [ ] Fechas como `"25/04/2026"` no generan candidatos de monto
- [ ] Números de tarjeta enmascarados (`"**** 4399"`) no generan candidatos de monto
- [ ] Crop manual de área pequeña (~200x100px) se escala hacia arriba y ML Kit reconoce el texto
- [ ] Logs de `OcrProcessor` muestran dimensiones de entrada/salida y texto de ML Kit para debugging

### Métricas de éxito
- 100% de tests pasando
- Zero falsos positivos en receipts con fechas/tarjetas
- Detección correcta de receipts digitales con keywords separados del monto
- Crop manual funciona en receipts digitales pequeños

---

## Notas y Aprendizajes

- `matchStr` vs `contextStr` en `looksLikeNonMonetary` es un bug class recurrente: los filtros de patrón estructural (fecha, teléfono, tarjeta) deben operar sobre el contexto completo, no solo sobre el número capturado.
- La corrección OCR debe ser bidireccional: corregir texto para amount extraction (dígitos) Y para keyword matching (letras).
- El score de la nueva capa de proximidad (60) fue elegido arbitrariamente entre `SCORE_GEOMETRIC_ALIGN` (50) y el máximo posible con bonuses (80). Se debe calibrar con dataset real si hay falsos positivos.
- **ML Kit necesita resolución mínima:** El problema del crop manual fallido revela que ML Kit Text Recognition requiere texto de ~18-20px de alto mínimo. Crops pequeños (<512px) deben escalarse hacia arriba antes de pasarlos a ML Kit.
- **Preprocessing agresivo puede destruir texto:** Valores fijos de contraste/brightness funcionan bien en imágenes grandes, pero en crops pequeños pueden destruir los bordes anti-aliased del texto. Usar valores más conservadores para imágenes <1000px.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-26 | Documento inicial — 4 fixes de AmountDetector identificados e implementados |
| 2026-04-26 | Fixes 5-6: preprocessing de imágenes pequeñas (upscaling + contraste adaptativo) |

---

## Referencias

- [ADR-052](ADR-052-ocr-amount-detector-test-fixes.md) — Precursor: fixes de regex, scoring y filtros que introdujeron el problema con receipts digitales
- [ADR-043](ADR-043-ocr-dark-mode-and-scoped-correction.md) — Corrección OCR scoped y dark mode preprocessing
- [Google ADR Template](https://google.aip.dev/decisions/) — Formato base
