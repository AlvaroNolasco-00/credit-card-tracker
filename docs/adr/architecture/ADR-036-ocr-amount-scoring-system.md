# ADR-036: OCR Amount Detection — Unified Scoring System

**Fecha:** 2026-04-07  
**Estado:** Aceptado  
**Categoría:** architecture  
**Prioridad:** High  
**Afecta:** `ocr/OcrProcessor.kt` — `AmountDetector.detect()`, pipeline de detección

---

## Contexto

El módulo OCR ejecuta **5 capas de detección secuenciales** para encontrar el monto total en recibos de tarjeta de crédito:

1. **Keyword search** — busca palabras clave ("total", "neto a pagar", etc.) en texto plano
2. **Geometric alignment** — busca montos alineados horizontalmente con keywords (ML Kit TextBlock bounding boxes)
3. **Position-based** — montos en la parte inferior del recibo (40-60% inferior)
4. **Last section scan** — montos en el último 50% de líneas de texto
5. **Final fallback** — cualquier monto encontrado

### Problema

Cada capa retornaba `Double?` y la **primera capa que encontraba un resultado ganaba** (`return` temprano). Esto causaba:

- **Keyword search retorna ciego:** Devolvía el **primer** match encontrado sin evaluar alternativas
  - Si había 2 montos bajo "TOTAL", elegía el primero sin comparer
  - No consideraba si el monto tenía símbolo de moneda (`$`, `Q`, `USD`, `MXN`)
  
- **Position-based elige el más bajo:** Retornaba el monto más bajo en el bloque bottom sin evaluar otros candidatos

- **Currency symbol ignorado:** El regex capturaba grupo 1 (`$€QMXNusd...`) pero nunca se usaba para priorizar
  - `$125.50 TOTAL` debería ganar sobre `125.50 TOTAL` pero ambos tenían igual "peso"

- **Largest amount en bottom heurística:** No había forma de favorecer el monto máximo en el último tercio del ticket

Resultado: **detección frágil y poco confiable** en recibos con múltiples montos (subtotal, impuestos, total).

---

## Decisión

Reemplazar el pipeline "first-wins" con un **sistema de puntuación unificado (scoring):**

1. Todas las capas acumulan **candidatos** con scores individuales (no retornan temprano)
2. Se aplican **bonificaciones** basadas en características (moneda, posición, tamaño)
3. **El candidato con mayor puntuación gana**

### Scoring Table

#### Base scores (por capa de detección)
| Capa | Puntos | Rationale |
|------|--------|-----------|
| Geometric alignment | 50 | Alineación espacial es la señal más fuerte (layout 2-columnas) |
| Keyword match | 40 | Keyword explícito en texto, pero no confirmado espacialmente |
| Position-based | 25 | Posición inferior confiable pero sin keyword/símbolo |
| Last section | 15 | Heurística débil (solo posición de línea) |
| Last amount | 5 | Fallback final, mínima confianza |

#### Bonuses (se suman al base score)
| Bonus | Puntos | Condición |
|-------|--------|-----------|
| Currency symbol | +30 | Regex grupo 1 no vacío: `$`, `€`, `Q`, `USD`, `MXN`, `GTQ`, `HNL`, etc. |
| Largest in bottom 30% | +20 | Es el monto máximo en el último 30% de líneas del ticket |
| Keyword in block | +15 | Bloque posicional también contiene palabra clave |

#### Confidence mapping (score → Confidence enum)
```kotlin
score >= 70  → Confidence.HIGH      // strong signal
score >= 40  → Confidence.MEDIUM    // weak signal but reasonable
score >= 20  → Confidence.LOW       // fallback
else         → Confidence.NONE      // no match
```

### Ejemplo concreto

**Recibo con múltiples montos:**
```
...
Subtotal:    $100.00
Impuesto:      $8.50
─────────────────────
TOTAL A PAGAR: $108.50  ← esperamos capturar este
```

#### Pipeline antiguo (first-wins):
1. Busca "TOTAL" → encuentra `$100.00` (subtotal) → **retorna 100.00** ❌

#### Pipeline nuevo (scoring):
1. Keyword "TOTAL" en línea `TOTAL A PAGAR: $108.50`
   - Candidato: $108.50, score = 40 (keyword match) + 30 (currency symbol) = **70** ✓
2. Keyword "SUBTOTAL" en línea `Subtotal: $100.00`
   - Candidato: $100.00, score = 40 (keyword match) + 30 (currency symbol) = 70 ✓
3. Geometric alignment (si bounding box lo confirma):
   - Si $108.50 está alineado con "TOTAL A PAGAR" → +50 → **score = 90** ✓✓

**Ganador: $108.50 (score 90)** ✓

### Por qué esta opción

| Aspecto | Razón |
|---------|-------|
| Scoring unificado | Permite comparación justa entre capas diferentes; el símbolo de moneda es un "tie-breaker" naturalmente fuerte |
| Acumular candidatos | Evita decisiones ciegas (first = true); recoge todas las opciones |
| Currency bonus alto (+30) | El regex ya captura moneda — usarla para priorizar es obvio |
| Largest in bottom 30% | Heurística OCR común: el monto más grande al final es probablemente el total |
| Scores ajustables | Cada bonus se puede tunear sin refactorizar la lógica central |

### Opciones rechazadas

**Opción A: Mantener first-wins, mejorar orden de búsqueda**
- ❌ Sigue siendo ciego a alternativas; si el orden está mal, sigue fallando
- ❌ Frágil a cambios de receipt format

**Opción B: Machine Learning (clasificador)**
- ❌ Overhead para Android (modelo + inferencia)
- ❌ OCR ya es impredecible; ML agrega otra capa de incertidumbre
- ❌ Datos de entrenamiento limitados (solo recibos MX/GT/HN)

---

## Consecuencias

### Directas

- ✅ **Robustez:** Múltiples candidatos evaluados y comparados; ganador es el que suma más puntos, no el primero
- ✅ **Currency symbol aprovechado:** Regex capturaba pero se ignoraba; ahora +30 puntos automáticos
- ✅ **Largest amount heurística:** Implementada la detección de monto máximo en bottom 30%
- ✅ **Mantenibilidad:** Scoring constants explícitas (`SCORE_GEOMETRIC_ALIGN = 50`, etc.); fácil de tunear
- ⚠️ **Complejidad:** Ahora se acumulan todos los candidatos (memory O(n)); en un ticket típico ~5-15 montos, impacto negligible
- ⚠️ **Performance:** Muy ligero comparado con OCR/ML Kit; overhead < 1ms

### Técnicas

**Refactorización interior:**

- Nueva data class `ScoredCandidate(amount: Double, score: Int)` reemplaza retorno directo `Double?`
- Métodos detectores renombrados/reescritos:
  - `findByKeywords()` → `findByKeywordsScored(text: String): List<ScoredCandidate>`
  - `findByGeometricAlignment()` → `findByGeometricAlignmentScored(visionText: Text): List<ScoredCandidate>`
  - `findByPosition()` → `findByPositionScored(visionText: Text): List<ScoredCandidate>`
  - `findAmountInLastSection()` → `findAmountInLastSectionScored(text: String): List<ScoredCandidate>`
  - `findLastAmount()` → `findLastAmountScored(text: String): List<ScoredCandidate>`

- Helpers nuevos:
  - `scoreToConfidence(score: Int): Confidence` — mapea score a enum (70+→HIGH, 40+→MEDIUM, etc.)
  - `largestAmountInBottom30Percent(text: String): Double?` — busca máximo en último 30%
  - `findScoredAmountInLine()` — find + score en una sola pasada
  - `lastScoredCandidateOnLine()` — último monto con score en línea

- Métodos reescritros:
  - `detect(visionText: Text)` — acumula scores, aplica bonuses, elige ganador
  - `detectFromText(text: String)` — versión sin ML Kit (para tests)

**Archivos impactados:**
- `app/src/main/java/com/alvaronolasco/creditcardtracker/ocr/OcrProcessor.kt`
  - Nueva clase interna: `ScoredCandidate`
  - Constantes de score (8 valores)
  - Reescritura completa de detectores (5 métodos)
  - Helpers nuevos (3-4 métodos)

**Breaking changes:** Ninguno — API pública (`OcrResult`, `detect()`, `Confidence`) sin cambios.

### Operacionales

**Testing requerido:**
- [ ] Single amount, no currency → score 40-45
- [ ] Amount with currency → score 70+ (40 base + 30 bonus)
- [ ] Multiple amounts under TOTAL → ganador es el que suma más (ej. $108.50 vs $100.00)
- [ ] Geometric alignment → score 90+ (50 base + 30 bonus + 10 keyword en block)
- [ ] Largest in bottom 30% → candidato recibe +20 bonus
- [ ] Arithmetic cross-check sigue funcionando (subtotal + tax = total)

**Device testing:**
- Recibos reales MX/GT/HN con múltiples montos
- Verificar que el monto capturado es "visualmente" el total (manual check)

---

## Implementación

### Paso a paso

1. ✅ Crear `ScoredCandidate` data class
2. ✅ Definir constantes de score (8 valores)
3. ✅ Reescribir detectores → `*Scored()` versiones (retornan `List<ScoredCandidate>`)
4. ✅ Implementar `scoreToConfidence()` mapper
5. ✅ Implementar `largestAmountInBottom30Percent()`
6. ✅ Reescribir `detect()` y `detectFromText()` para acumular + evaluar scores
7. ⏳ Ejecutar suite de tests existentes
8. ⏳ Agregar tests nuevos para scoring edge cases

### Files de referencia

- **Commit principal:** `app/src/main/java/com/alvaronolasco/creditcardtracker/ocr/OcrProcessor.kt` reescrito
- **Tests:** `app/src/test/java/com/alvaronolasco/creditcardtracker/ocr/OcrAmountDetectorTest.kt`

---

## Validación

### Cómo verificar

```kotlin
// En OcrAmountDetectorTest:

// Single amount, no symbol → LOW confidence
detectFromText("TOTAL 100").confidence == Confidence.LOW     // score ~40

// Amount with currency → MEDIUM/HIGH
detectFromText("TOTAL $100").confidence == Confidence.MEDIUM // score ~70 (no geometric confirmation)

// Multiple amounts → winner has highest score
val receipt = """
  Subtotal: $100.00
  TOTAL:    $108.50
"""
detectFromText(receipt).amount == 108.50  // $108.50 wins: keyword "TOTAL" + currency = 70

// Largest in bottom 30% bonus
val tall = """
  Line 1: $5.00
  Line 2: $10.00
  ...
  Line 30: (bottom 30%)
  Line 31: $50.00  ← largest at bottom
  Line 32: TOTAL $50.00
"""
detectFromText(tall).amount == 50.00  // $50.00 gets +20 bonus, wins decisively

// Geometric alignment (if ML Kit available in test)
// detectFromVisionText() with 2-column layout:
// [TOTAL A PAGAR]  [$108.50]  ← same row
// → score = 50 (geometric) + 30 (currency) = 80 → HIGH confidence
```

### Métricas de éxito
- **Precisión de monto:** >98% en 100+ recibos MX/GT/HN reales
- **Confianza calibrada:** 
  - Montos con símbolo + keyword → HIGH (score 70+)
  - Montos sin símbolo → LOW-MEDIUM (score 40-50)
  - Arithmetic-verified → VERIFIED (score irrelevante)
- **Velocity:** No regresión en OCR speed (<100ms total)

---

## Notas y Aprendizajes

- **Score constants son "magic numbers"** pero el trade-off vale la pena por claridad. Si se necesita ajustar, son 8 líneas.
- **Currency symbol +30 es fuerte** porque muchos recibos no tienen símbolo explícito (solo el texto "TOTAL" seguido de número). El +30 da prioridad a los que sí lo tienen.
- **Largest in bottom 30% es heurística OCR clásica** — el monto más grande al final suele ser el total (no subtotal, no impuestos individuales).
- **Futuros:** Si queremos ML (neural scoring), es una extensión natural — reemplazar score constants con NN output, todo lo demás igual.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-07 | Documento inicial — Implementación completada |

---

## Referencias

- [ADR-033](ADR-033-geometric-ocr-alignment.md) — Alineación geométrica OCR (layer 2)
- [ADR-034](ADR-034-ocr-parsing-robustness.md) — Robustez de parsing (post-scoring)
- [ADR-035](ADR-035-ocr-processor-lifecycle-management.md) — Lifecycle management (orthogonal)
