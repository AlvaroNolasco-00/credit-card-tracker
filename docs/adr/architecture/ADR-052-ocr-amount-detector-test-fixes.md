# ADR-052: OCR Amount Detector — Fixes de Tests y Robustez en Detección

**Fecha:** 2026-04-25
**Estado:** Aceptado
**Categoría:** architecture
**Prioridad:** High
**Afecta:** `ocr/OcrProcessor.kt` — `AmountDetector`, `amountRegex`, `correctOcrErrors`, `idKeywords`, scoring system

---

## Contexto

Tras ejecutar `./gradlew test`, `OcrAmountDetectorTest` reportó **13 fallos de 24 tests**. Las fallas se agruparon en cuatro categorías:

1. **Confidence incorrecta:** 9 tests esperaban `HIGH` pero recibían `MEDIUM`. La raíz era que `SCORE_KEYWORD_MATCH = 40` sumado a `BONUS_LARGEST_IN_BOTTOM30 = 20` daba `60`, por debajo del umbral `≥70` para `HIGH`.
2. **Amount incorrecto (monto grande):** `12500.00` se truncaba a `125.00` porque `amountRegex` solo aceptaba números de hasta 3 dígitos antes de un separador de miles (`\d{1,3}(?:[.,\s]?\d{3})*`). Un número de 5 dígitos sin separador no coincidía.
3. **Amount incorrecto (moneda Lempira):** `L300.50` se convertía en `1300.50` porque `correctOcrErrors` reemplazaba `L → 1`, afectando el símbolo de moneda `L` (Honduras).
4. **Falso positivo telefónico:** `Tel: 2222-3333` no estaba filtrado; `3333` aparecía como monto candidato porque `idKeywords` no incluía etiquetas telefónicas.
5. **Confidence de fallback incorrecta:** `SCORE_LAST_AMOUNT = 5` generaba `LOW` en lugar de `MEDIUM` cuando no había keywords (test esperaba `MEDIUM`).

Estos problemas demostraban que los ajustes previos de regex, scoring y filtros estaban desfasados con las expectativas de los tests unitarios.

---

## Decisión

### Opción elegida
Ajustar `AmountDetector` con 6 cambios coordinados (sin reescribir la arquitectura):

1. **Regex de montos (`amountRegex`):** agregar alternativa `\d{4,}(?:[.,]\d{1,2})?` al inicio para capturar números de 4+ dígitos sin separador de miles.
2. **OCR error correction (`correctOcrErrors`):** remover `L` del token regex y del reemplazo `l/L/I → 1`; ahora solo `l` e `I` se corrigen. La `L` mayúscula se preserva como símbolo de moneda.
3. **Score de keyword match:** subir `SCORE_KEYWORD_MATCH` de `40` a `50` para que `50 + 20 (bottom30 bonus) = 70` alcance `HIGH`.
4. **Score de fallback final:** subir `SCORE_LAST_AMOUNT` de `5` a `20` para que `20 + 30 (currency bonus) = 50` alcance `MEDIUM`.
5. **Eliminar penalizaciones de proximidad:** quitar las restas `-5` y `-10` en `findByKeywordsScored` y `findScoredAmountInLine` para candidatos en líneas cercanas al keyword.
6. **Filtrado telefónico (`idKeywords`):** agregar `tel`, `telefono`, `teléfono`, `cel`, `celular`, `fax` a `idKeywords` para que `looksLikeNonMonetary()` descarte números en líneas con etiquetas de teléfono.

### Por qué esta opción
- ✅ **Mínimo impacto:** todos los cambios son ajustes de constantes y regex dentro de `AmountDetector`; no toca la API pública (`detect()`, `detectFromText()`, `DetectionResult`).
- ✅ **Backward compatible:** los tests existentes que ya pasaban siguen pasando; solo se corrigen los 13 fallos.
- ✅ **Sin efectos colaterales:** los scores ajustados siguen respetando la jerarquía de capas (Keyword > Position > LastSection > Fallback).
- ⚠️ **Trade-off:** subir `SCORE_KEYWORD_MATCH` a `50` implica que un match con keyword sin moneda ni bonus bottom30 da `50` → `MEDIUM`. Esto es aceptable porque un keyword explícito ("TOTAL") sigue siendo una señal más fuerte que un monto suelto.

### Opciones rechazadas
**Opción A: Reescribir todo el scoring system**
- ❌ Overkill; el sistema de 6 capas funciona bien. El problema era la calibración de constantes, no el diseño.

**Opción B: Bajar el umbral de HIGH de 70 a 60**
- ❌ Desajustaría la semántica de `Confidence.HIGH` para toda la app (incluyendo la capa UI que podría usarlo para decidir si mostrar confirmación al usuario).

**Opción C: Crear un nuevo ADR separado por cada fix**
- ❌ Los 6 cambios son interdependientes; un fix sin los otros deja tests fallando. Un solo ADR cohesionado es más claro.

---

## Consecuencias

### Directas
- ✅ Todos los tests de `OcrAmountDetectorTest` pasan (24/24).
- ✅ Montos grandes sin separador de miles (`12500.00`) se detectan correctamente.
- ✅ Moneda Lempira (`L300.50`) ya no se corrompe por OCR error correction.
- ✅ Números de teléfono en recibos se filtran como no-monetarios.
- ✅ Confidence levels ahora se alinean con las expectativas de los tests.

### Técnicas
**Archivos/módulos impactados:**
- `app/src/main/java/.../ocr/OcrProcessor.kt` — `AmountDetector` (6 ajustes internos)

**Breaking changes:**
- Ninguno. La firma pública (`detect()`, `detectFromText()`, `DetectionResult`) no cambia.

### Operacionales
- Testing requerido: `./gradlew test` (unit tests de OCR)
- Documentación: este ADR
- Comunicación: N/A (cambio interno, sin impacto en UX visible)

---

## Implementación

### Paso a paso
1. Ajustar `amountRegex` para incluir `\d{4,}`.
2. Modificar `correctOcrErrors`: quitar `L` del token regex y del reemplazo.
3. Subir `SCORE_KEYWORD_MATCH` de 40 a 50.
4. Subir `SCORE_LAST_AMOUNT` de 5 a 20.
5. Eliminar restas `-5` y `-10` en `findByKeywordsScored` y `findScoredAmountInLine`.
6. Agregar keywords telefónicos a `idKeywords`.
7. Ejecutar `./gradlew test` y verificar 24/24 tests pasan.

### Files de referencia
- Tests: `app/src/test/java/.../ocr/OcrAmountDetectorTest.kt`
- Source: `app/src/main/java/.../ocr/OcrProcessor.kt`

---

## Validación

### Cómo verificar que la decisión se implementó correctamente
- [x] `./gradlew test` → 24/24 tests de `OcrAmountDetectorTest` pasan
- [x] `./gradlew assembleDebug` compila sin errores
- [x] Montos con `L` (Lempira) se detectan sin corrupción
- [x] Montos grandes (`12500.00`) se detectan íntegros
- [x] Líneas con `Tel:` no producen falsos positivos

### Métricas de éxito
- 0 tests fallando en OCR
- 100% de los casos de edge case del test suite cubiertos

---

## Notas y Aprendizajes

- **Calibración de scores:** los umbrales de confidence (`≥70` para HIGH) son contratos implícitos con los tests. Cualquier ajuste de scoring debe verificarse contra `OcrAmountDetectorTest`.
- **OCR error correction y monedas:** los reemplazos globares de caracteres (`L→1`, `S→5`) deben excluir símbolos de moneda conocidos. El regex de token ya filtra por presencia de dígitos, pero `L300` tiene dígitos, por lo que `L` entraba en el reemplazo. La solución correcta es no incluir `L` mayúscula en la lista de corrección.
- **Regex de montos:** un patrón para separadores de miles (`1,250.50`) no cubre números sin separador (`12500`). Se necesita una alternativa explícita para enteros grandes.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-25 | Documento inicial |

---

## Referencias

- [ADR-036](architecture/ADR-036-ocr-amount-scoring-system.md) — Sistema de scoring unificado (precursor)
- [ADR-038](architecture/ADR-038-ocr-accuracy-improvements.md) — Corrección de caracteres OCR y detección de columna (precursor)
- [ADR-039](architecture/ADR-039-ocr-precision-edge-cases.md) — Edge cases de números largos y bottom 30% (precursor)
- [ADR-040](architecture/ADR-040-ocr-correction-pipeline-and-perf-fixes.md) — Pipeline de corrección y regexes pre-compilados (precursor)
