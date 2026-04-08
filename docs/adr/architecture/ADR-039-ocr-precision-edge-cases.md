# ADR-039: OCR Precision Edge Cases — Rightmost Tolerance, Long Number Filtering & Bottom 30% Unification

**Fecha:** 2026-04-07  
**Estado:** Aceptado  
**Categoría:** architecture  
**Prioridad:** High  
**Afecta:** `ocr/OcrProcessor.kt` — `findColumnAlignedAmountsScored()`, `looksLikeNonMonetary()`, `largestAmountInBottom30Percent()`

---

## Contexto

El análisis de código de `OcrProcessor.kt` reveló 3 inconsistencias/bugs que causaban falsos negativos y positivos en la detección de montos OCR. Estos problemas no estaban documentados en los ADRs previos (ADR-033 a ADR-038).

**Problemas observados:**

1. **Bonus "rightmost" en columna alineada usa comparación exacta de píxeles**  
   - Línea 438: `box.right == columnGroup.value.maxOf { it.third!!.right }`
   - Si hay diferencia de 1px entre montos, ninguno recibe el bonus +15
   - ML Kit bounding boxes tienen variación de ±2-3px incluso en texto perfectamente alineado

2. **Filtro de 6+ dígitos sin separadores es demasiado agresivo**  
   - Línea 602: `if (cleanNum.length >= 6 && !matchStr.contains(".") && !matchStr.contains(",")) return true`
   - Rechaza montos válidos como `100000` (cien mil) en mercados con monedas de baja denominación (MXN, GTQ, HNL)
   - No considera contexto de símbolo de moneda o keyword de total

3. **Definición dual de "bottom 30%" es inconsistente**  
   - `largestAmountInBottom30Percent()` (L564): usa **número de líneas** de texto
   - `findColumnAlignedAmountsScored()` (L432): usa **coordenadas Y en píxeles**
   - Dos métodos diferentes pueden clasificar diferentes montos como "bottom 30%" en el mismo ticket
   - Inconsistencia de scoring entre capas de detección

**Impacto:**
- Falsos negativos: montos válidos grandes rechazados por filtro de 6 dígitos
- Falsos positivos: bonus de rightmost no se aplica cuando debería
- Inconsistencia: scoring no determinista entre layers

---

## Decisión

Implementar 3 correcciones específicas sin alterar la arquitectura existente:

### Opción elegida: Fixes puntuales por capa

#### 1. Bonus rightmost con tolerancia de píxeles

**Cambio:** Comparación exacta (`==`) → rango con tolerancia de 15px

```kotlin
// Línea 438 — ANTES:
val isRightmost = box.right == columnGroup.value.maxOf { it.third!!.right }

// DESPUÉS:
val maxRight = columnGroup.value.maxOf { it.third!!.right }
val isRightmost = box.right >= maxRight - 15  // Tolerancia de 15px
```

**Razón:** ML Kit bounding boxes tienen variación natural de ±2-3px. 15px es conservador pero suficiente para capturar montos visualmente alineados.

#### 2. Filtro de 6+ dígitos condicional por contexto

**Cambio:** Solo rechazar números largos si NO tienen contexto monetario

```kotlin
// Líneas 601-607 — ANTES:
val cleanNum = matchStr.replace("[^0-9]".toRegex(), "")
if (cleanNum.length >= 6 && !matchStr.contains(".") && !matchStr.contains(",")) return true

// DESPUÉS:
val cleanNum = matchStr.replace("[^0-9]".toRegex(), "")
val hasLongNumber = cleanNum.length >= 6 && !matchStr.contains(".") && !matchStr.contains(",")

if (hasLongNumber) {
    // Verificar si tiene símbolo de moneda o está cerca de keyword de total
    val hasCurrencySymbol = matchStr.matches(Regex(""".*[$€£¥₣₹Q].*""")) ||
                             contextStr.contains("$", ignoreCase = true) ||
                             contextStr.contains("Q", ignoreCase = true) ||
                             contextStr.contains("USD", ignoreCase = true) ||
                             contextStr.contains("MXN", ignoreCase = true) ||
                             contextStr.contains("GTQ", ignoreCase = true) ||
                             contextStr.contains("HNL", ignoreCase = true)
    
    val hasTotalKeyword = totalKeywords.any { contextStr.contains(it, ignoreCase = true) }
    
    // Si NO tiene moneda ni keyword, probablemente es ID/código postal
    if (!hasCurrencySymbol && !hasTotalKeyword) return true
}
```

**Razón:** Montos como `100000` son válidos en mercados latinoamericanos (cien mil pesos, quetzales, etc.). El filtro debe distinguir entre IDs (sin contexto) y montos (con símbolo/keyword).

#### 3. Unificar cálculo de "bottom 30%" en función compartida

**Cambio:** Crear función utilitaria `isInBottom30Percent()` con sealed class para ambos modos

```kotlin
// NUEVO: Agregar después de bonus constants (L149)
private sealed class Bottom30Mode {
    data class ByLineIndex(val lineIndex: Int, val totalLines: Int) : Bottom30Mode()
    data class ByPixelY(val yTop: Int, val maxY: Int) : Bottom30Mode()
}

private fun isInBottom30Percent(mode: Bottom30Mode): Boolean {
    return when (mode) {
        is Bottom30Mode.ByLineIndex -> {
            val thresholdIndex = (mode.totalLines * 0.70).toInt()
            mode.lineIndex >= thresholdIndex
        }
        is Bottom30Mode.ByPixelY -> {
            val thresholdY = (mode.maxY * 0.70).toInt()
            mode.yTop >= thresholdY
        }
    }
}

// REFACTORIZAR: largestAmountInBottom30Percent() (L588)
private fun largestAmountInBottom30Percent(text: String): Double? {
    val lines = text.split("\n")
    return lines
        .mapIndexed { index, line -> index to line }
        .filter { (index, _) ->
            isInBottom30Percent(Bottom30Mode.ByLineIndex(index, lines.size))
        }
        .flatMap { (_, line) ->
            amountRegex.findAll(line)
                .filter { !looksLikeNonMonetary(it.value, line) }
                .mapNotNull { parseAmount(it.groupValues[2]) }
        }
        .maxOrNull()
}

// REFACTORIZAR: findColumnAlignedAmountsScored() (L461)
// ANTES:
val isInBottom30 = box!!.top > bottom30Threshold

// DESPUÉS:
val isInBottom30 = isInBottom30Percent(Bottom30Mode.ByPixelY(box!!.top, maxBottom))
```

**Razón:** Unifica el cálculo matemático del "bottom 30%" en un solo lugar. Si se necesita ajustar el threshold (ej. 70% → 75%), solo se cambia en una función.

### Por qué estas opciones

| Mejora | Razón |
|--------|-------|
| Tolerancia 15px | ML Kit bounding boxes tienen variación natural; comparación exacta es demasiado estricta |
| Filtro condicional | Montos grandes son válidos en mercados objetivo; contexto distingue montos de IDs |
| Función unificada | DRY principle; single source of truth para cálculo de bottom 30% |

### Opciones rechazadas

**Opción A: Eliminar filtro de 6 dígitos completamente**
- ❌ Aumentaría falsos positivos (códigos postales, IDs largos)
- ✅ Filtro condicional es más preciso

**Opción B: Usar comparación con tolerancia dinámica basada en altura de línea**
- ❌ Overkill para este caso; 15px fijo es suficiente
- ✅ Si se necesita más precisión, se puede tunear en el futuro

**Opción C: Mantener dos cálculos separados de bottom 30%**
- ❌ Duplicación de lógica; riesgo de divergencia
- ✅ Función unificada es más mantenible

---

## Consecuencias

### Directas

- ✅ **Bonus rightmost aplicado correctamente:** Montos visualmente alineados reciben +15 puntos
- ✅ **Menos falsos negativos:** Montos grandes (100000+) con contexto monetario ya no son rechazados
- ✅ **Consistencia de bottom 30%:** Ambos layers usan el mismo cálculo matemático
- ✅ **Mantenibilidad:** Cálculo de bottom 30% en una sola función; fácil de ajustar
- ⚠️ **Complejidad:** +25 líneas (sealed class + función utilitaria + refactorización)
- ⚠️ **Filtro condicional más laxo:** Puede permitir algunos IDs largos que tengan símbolo de moneda (edge case)

### Técnicas

**Archivos/módulos impactados:**
- `app/src/main/java/com/alvaronolasco/creditcardtracker/ocr/OcrProcessor.kt`
  - Línea 438: Bonus rightmost con tolerancia
  - Líneas 150-173: Nueva sealed class `Bottom30Mode` + función `isInBottom30Percent()`
  - Líneas 588-601: `largestAmountInBottom30Percent()` refactorizado
  - Línea 461: `findColumnAlignedAmountsScored()` usa función unificada
  - Líneas 602-620: `looksLikeNonMonetary()` filtro de 6 dígitos condicional

**Breaking changes:** Ninguno — API pública sin cambios.

### Operacionales

**Testing requerido:**
- [ ] Tickets con montos alineados visualmente → verificar que rightmost bonus se aplica
- [ ] Tickets con montos grandes (100000+) → verificar que se capturan si tienen moneda/keyword
- [ ] Tickets con IDs largos (códigos postales, folios) → verificar que se siguen filtrando
- [ ] Tickets de prueba → verificar que scoring es consistente entre layers

**Device testing:**
- Recibos reales MX/GT/HN con montos grandes
- Recibos con layout de 2 columnas (verificar rightmost bonus)

---

## Implementación

### Paso a paso

1. ✅ Modificar línea 438: bonus rightmost con tolerancia 15px
2. ✅ Agregar sealed class `Bottom30Mode` (L150-156)
3. ✅ Agregar función `isInBottom30Percent()` (L158-173)
4. ✅ Refactorizar `largestAmountInBottom30Percent()` (L588-601)
5. ✅ Refactorizar `findColumnAlignedAmountsScored()` (L461)
6. ✅ Modificar `looksLikeNonMonetary()` filtro 6 dígitos condicional (L602-620)
7. ⏳ Agregar tests unitarios para edge cases
8. ⏳ Validar en dispositivo real

### Files de referencia

- **Cambios principales:** `app/src/main/java/com/alvaronolasco/creditcardtracker/ocr/OcrProcessor.kt`
- **Tests:** `app/src/test/java/com/alvaronolasco/creditcardtracker/ocr/OcrAmountDetectorTest.kt`

---

## Validación

### Cómo verificar

```kotlin
// En OcrAmountDetectorTest:

// Bonus rightmost tolerance
// Mock visionText con 2 montos en misma fila, X: 200 y X: 201 (diferencia 1px)
// → Ambos deberían recibir bonus (antes: ninguno)

// Filtro 6 dígitos condicional
detectFromText("TOTAL 100000").amount == 100000.0  // ✓ Capturado (tiene keyword)
detectFromText("C.P. 28001 123456").amount == null  // ✓ Filtrado (sin moneda/keyword)
detectFromText("$100000").amount == 100000.0  // ✓ Capturado (tiene símbolo)

// Bottom 30% unification
// Verificar que largestAmountInBottom30Percent() y findColumnAlignedAmountsScored()
// usan el mismo threshold (70% del total)
```

### Métricas de éxito

- Bonus rightmost aplicado en >90% de casos visualmente alineados
- Falsos negativos de montos grandes: reducción >50%
- Consistencia de bottom 30%: ambos layers clasifican mismos montos
- Sin regresión en tickets que ya funcionaban

---

## Notas y Aprendizajes

- **Comparación exacta vs tolerancia:** En coordenadas de píxeles de ML Kit, la variación natural de ±2-3px hace que `==` sea demasiado estricto. 15px es un buen balance entre precisión y robustez.
- **Contexto es clave:** El filtro de 6 dígitos es más preciso cuando considera el contexto circundante (símbolo de moneda, keyword). Un número largo aislado es probablemente un ID; un número largo con "$" es un monto.
- **DRY principle:** El cálculo de "bottom 30%" estaba duplicado en dos lugares. Unificarlo reduce riesgo de bugs futuros y hace más fácil ajustar el threshold.
- **Sealed class para modos:** `Bottom30Mode` es un patrón idiomático de Kotlin para representar múltiples variantes de un cálculo con type safety.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-07 | Documento inicial — Implementación completada |

---

## Referencias

- [ADR-038](ADR-038-ocr-accuracy-improvements.md) — OCR Accuracy Improvements (precursor)
- [ADR-036](ADR-036-ocr-amount-scoring-system.md) — Scoring System (base)
- [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition) — Bounding boxes precisión
