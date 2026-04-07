# ADR-033: Alineación Geométrica en Detección de Montos OCR

**Fecha:** 2026-04-07  
**Estado:** Aceptado  
**Categoría:** architecture  
**Prioridad:** High  
**Afecta:** `ocr/OcrProcessor.kt`, `AmountDetector`  

---

## Contexto

La detección de montos en recibos es crítica para el rastreo de gastos. Actualmente, ML Kit fragmenta el texto reconocido en `TextBlock`s que a menudo están separados espacialmente.

**Problema observado:**
En recibos con layout de dos columnas (ej. "TOTAL" a la izquierda, "1,500.00" a la derecha), ML Kit genera:
- `TextLine 1`: "TOTAL" (X: 10-50, Y: 100-110)
- `TextLine 2`: "1,500.00" (X: 200-250, Y: 100-110)

El algoritmo anterior (`Layer 1: Keyword Match`) busca "TOTAL" en el texto agregado, pero **no lo encuentra en la misma línea** porque los caracteres están en diferentes `TextBlock`s. Luego cae a búsquedas posicionales menos confiables (Layer 2-4).

**Restricción:**
No podemos modificar cómo ML Kit tokeniza (es una caja negra), pero **sí podemos usar las coordenadas de bounding boxes** para correlacionar líneas que están alineadas horizontalmente.

---

## Decisión

### Opción elegida: Alineación Geométrica con Bounding Boxes (Layer 1.5)

Implementar una capa de detección entre la búsqueda de palabras clave (Layer 1) y análisis posicional (Layer 2) que:

1. **Itera sobre todos los `TextLine`s** en los `TextBlock`s retornados por ML Kit
2. **Para cada línea que contiene una palabra clave** (ej. "TOTAL"):
   - Extrae el Y-center: `(boundingBox.top + boundingBox.bottom) / 2`
   - Define tolerancia vertical: `lineHeight × 1.2` (permite ligero desalineamiento)
3. **Busca otras líneas** cuyo Y-center cae dentro de la tolerancia (mismo "row" en el recibo)
4. **Filtra por posición horizontal**: solo líneas a la derecha o superpuestas (`box.left >= keywordBox.left`)
5. **Retorna la cantidad** del candidato más a la derecha (columna de monto en recibos)

Asignación de puntuación:
- Base score: `SCORE_GEOMETRIC_ALIGN = 50` (muy alto, porque es información espacial directa)
- Bonus de símbolo de moneda: `+BONUS_CURRENCY_SYMBOL = 30` (si la línea contiene $, Q, USD, etc.)

### Por qué esta opción

- ✅ **Confianza espacial**: Usa información directa de ML Kit (bounding boxes), no heurística de string
- ✅ **Resiliente a layout**: Funciona incluso si "TOTAL" y monto están en `TextBlock`s separados (caso común)
- ✅ **Orden de prioridad correcto**: Se ejecuta *antes* de análisis posicional (que es más frágil)
- ✅ **Integrarse con sistema de puntuación**: Usa arquitectura `ScoredCandidate` para ordenar candidatos, permitiendo que Subtotal+Tax verifique arithmetically

### Opciones rechazadas

**Opción A: Mejorar Layer 1 (búsqueda de palabras clave en texto agregado)**
- ❌ No captura correlaciones espaciales — el texto agregado no preserva las coordenadas
- ❌ Requeriría cambios complejos en preprocesamiento

**Opción B: Confiar únicamente en análisis posicional (Layer 2)**
- ❌ Menos preciso: solo usa la ubicación Y en la imagen, no correlación con palabras clave
- ❌ Propenso a falsos positivos (ej. IVA/impuestos en bottom 40%)

---

## Consecuencias

### Directas

- ✅ Detecta correctamente montos en recibos con dos columnas (patrón muy común en Latinoamérica)
- ✅ Reduce caídas a layers de menor confianza (MEDIUM, LOW) — aumenta `Confidence.HIGH`
- ✅ Mantiene retrocompatibilidad: Layer 1 sigue funcionando, Layer 1.5 complementa
- ⚠️ Requiere ML Kit Text Recognition (debe estar disponible)
- ❌ Complejidad O(n²) en casos de muchos `TextLine`s (raro en recibos reales: ~30-50 líneas)

### Técnicas

**Archivos/módulos impactados:**
- `app/src/main/java/com/alvaronolasco/creditcardtracker/ocr/OcrProcessor.kt` — Nuevo método `findByGeometricAlignmentScored()`
- Scoring system: `SCORE_GEOMETRIC_ALIGN` (50 puntos base)

**Breaking changes:**
- Ninguno. La arquitectura de `detect()` ya usa `ScoredCandidate` y puede integrar este layer sin afectar APIs externas.

### Operacionales

- Testing requerido: Unit tests con mocks de `visionText` de múltiples layouts
- Documentación: Este ADR + comentarios en código
- Comunicación: Mejora interna, sin impacto visible en UI

---

## Implementación

### Paso a paso

1. Crear método `findByGeometricAlignmentScored(visionText: Text): List<ScoredCandidate>`
2. Llamarlo en `detect()` entre Layer 1 y Layer 2 (ya hecho)
3. Validar en recibos reales de 2+ monedas (Q, USD, etc.)
4. Ajustar `verticalTolerance = lineHeight × 1.2` según testing si es necesario

### Files de referencia

- Implementación: `app/src/main/java/com/alvaronolasco/creditcardtracker/ocr/OcrProcessor.kt:335-382`
- Pruebas unitarias: `app/src/test/java/com/alvaronolasco/creditcardtracker/ocr/OcrAmountDetectorTest.kt`
- Constantes: líneas 137 (`SCORE_GEOMETRIC_ALIGN`)

---

## Validación

### Cómo verificar

- [ ] Paso dos recibos con "TOTAL" y monto en columnas separadas → detecta correctamente
- [ ] El monto retornado tiene `Confidence.HIGH` (score ≥ 70)
- [ ] Si el monto tiene símbolo de moneda (Q, USD), la puntuación es aún mayor
- [ ] Recibos sin separación espacial (TOTAL y monto en la misma línea) aún funcionan (Layer 1)

### Métricas de éxito

- Tasa de detección correcta en batch de 20+ recibos de 2+ monedas: ≥ 95%
- Confianza promedio: > 75% (HIGH)
- Sin degradación en casos que ya funcionaban

---

## Notas y Aprendizajes

- **Tolerancia vertical**: `lineHeight × 1.2` es conservador pero probado. Si hay casos de montos ligeramente más abajo/arriba que la línea de keyword, aumentar a `1.5` o hacer dinámico según `lineHeight`.
- **Orden de keywords**: Mantener `totalKeywords` con multi-palabra primero ("total a pagar") para evitar matches parciales en "total general".
- **Futuro**: Si los layouts evolucionan a 3+ columnas (ej. Qty | Precio | Subtotal), considerar búsqueda de múltiples alineaciones simultáneamente.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-07 | Documento inicial — implementación de alineación geométrica |

---

## Referencias

- [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition) — Bounding boxes y estructura
- ADR-[anterior OCR] — (si existe) decisiones previas sobre OCR
- Issue: Detección de montos en recibos con layout de 2 columnas
