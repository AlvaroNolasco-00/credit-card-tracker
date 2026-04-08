# ADR-040: OCR Pipeline Fixes — correctOcrErrors Reubicado, Regexes Pre-compilados, NumberFormat Cacheado & ignoreWords Refinado

**Fecha:** 2026-04-07  
**Estado:** Aceptado  
**Categoría:** architecture  
**Prioridad:** High  
**Afecta:** `ocr/OcrProcessor.kt` — `AmountDetector`, `findAmountsInLine()`, `looksLikeNonMonetary()`, `parseAmount()`, `ignoreWords`

---

## Contexto

Un análisis post-ADR-038/039 detectó 4 problemas en el pipeline OCR que no habían sido abordados:

### Problema 1 — `correctOcrErrors()` era código muerto (BUG)

ADR-038 introdujo `correctOcrErrors()` para corregir confusiones de ML Kit (O→0, l→1, S→5, B→8, Z→2). Sin embargo, la función se llamaba **dentro de `parseAmount()`**, que recibe el grupo 2 del regex (`\d{1,3}...`). Ese grupo solo captura caracteres `\d` (dígitos) y separadores. Si ML Kit devolvía `"1O5.OO"`, el regex **nunca matcheaba** porque `O` no es `\d`, y la corrección se aplicaba sobre un string que ya era correcto o nunca llegaba a ejecutarse. La mejora de ADR-038 no estaba funcionando.

### Problema 2 — Regexes compilados en hot path

`looksLikeNonMonetary()` se invoca para cada match de cada línea de cada layer de detección (potencialmente cientos de veces por ticket). Internamente creaba dos objetos `Regex` en cada llamada:
- `Regex(Pattern.quote(matchStr.trim()) + """\s*%""")` — dinámico, compilado cada vez
- `Regex(""".*[$€£¥₣₹Q].*""")` — estático, compilado innecesariamente en cada llamada

### Problema 3 — `NumberFormat` instanciado por cada llamada a `parseAmount()`

`parseAmount()` creaba 5 instancias de `NumberFormat.getNumberInstance(locale)` en cada invocación. En un ticket con ~50 candidatos, eso son 250 instancias. `NumberFormat.getNumberInstance()` no es trivial — accede a recursos de localización.

### Problema 4 — `"sub"` en `ignoreWords` demasiado amplio

`ignoreWords` contenía `"sub"` con la intención de filtrar líneas de subtotal. Sin embargo, `"sub"` como substring matchea palabras legítimas como "subscription", "subway", "subasta" — filtrando falsamente líneas con montos válidos.

---

## Decisión

### Fix #1 — Centralizar corrección OCR antes del regex con `findAmountsInLine()`

Crear un helper privado que aplica `correctOcrErrors()` al texto **antes** de ejecutar `amountRegex.findAll()`:

```kotlin
private fun findAmountsInLine(text: String): Sequence<MatchResult> =
    amountRegex.findAll(correctOcrErrors(text))
```

Reemplazar **todas** las llamadas directas a `amountRegex.findAll(...)` en los layers de detección por `findAmountsInLine(...)`:

| Método | Línea (aprox.) |
|--------|---------------|
| `findByGeometricAlignmentScored()` | searchFrom |
| `findColumnAlignedAmountsScored()` | line.text |
| `findByPositionScored()` | block.text |
| `findAmountInLastSectionScored()` | line |
| `findLastAmountScored()` | line |
| `findScoredAmountInLine()` | afterKeyword |
| `lastScoredCandidateOnLine()` | line |
| `largestAmountInBottom30Percent()` | line |
| `lastValidAmountOnLine()` | line |

**Restricción clave:** `findAmountsInLine()` solo se usa para búsqueda de montos. Las búsquedas de keywords (`line.contains(keyword)`) continúan usando el texto original — de lo contrario "TOTAL" se convertiría en "T0TA1".

`correctOcrErrors()` se elimina de `parseAmount()`: su input (el grupo capturado por regex) ya solo contiene dígitos y separadores, por lo que la corrección era irrelevante ahí.

### Fix #2 — Pre-compilar regexes estáticos; reemplazar dinámico con búsqueda de string

a) El regex estático de moneda pasa a propiedad de clase:
```kotlin
private val currencyInStringRegex = Regex(""".*[$€£¥₣₹Q].*""")
```

b) El regex dinámico de porcentaje (`Pattern.quote(matchStr)`) se reemplaza por búsqueda de string directa:
```kotlin
val trimmed = matchStr.trim()
val idx = contextStr.indexOf(trimmed)
if (idx >= 0) {
    val afterMatch = contextStr.substring(idx + trimmed.length).trimStart()
    if (afterMatch.startsWith("%")) return true
}
```
Misma semántica, sin compilación de regex. El import `java.util.regex.Pattern` se elimina al quedar sin uso.

### Fix #3 — Cachear `NumberFormat` como propiedad de `AmountDetector`

```kotlin
private val localeFormatters: List<NumberFormat> = listOf(
    Locale.US, Locale("es", "MX"), Locale("es", "GT"),
    Locale("es", "HN"), Locale.GERMANY,
).map { NumberFormat.getNumberInstance(it) }
```

`parseAmount()` itera `localeFormatters` directamente. `ParsePosition` se reutiliza reseteando `pos.index = 0` en cada iteración.

**Thread safety:** `AmountDetector` se instancia localmente dentro de `processImage()` / `processImageBitmap()` — una instancia por procesamiento, sin concurrencia.

### Fix #4 — Reemplazar `"sub"` con términos explícitos en `ignoreWords`

```kotlin
private val ignoreWords = listOf(
    "precio", "ahorro", "descuento", "cambio", "su cambio", "vuelto",
    "subtotal", "sub total", "sub-total"
)
```

`"sub"` eliminado. Los términos de subtotal se escriben explícitamente — consistente con `subtotalKeywords` donde ya estaban listados.

### Por qué estas opciones

| Fix | Razón |
|-----|-------|
| Helper `findAmountsInLine()` | Centraliza la corrección en un punto; evitar repetir `correctOcrErrors()` en 9 lugares; cambio mecánico sin alterar lógica de scoring |
| String search vs regex dinámico | Misma semántica, cero overhead de compilación; `Pattern.quote()` era la única razón del import |
| Cache de `NumberFormat` | Sin costo de thread-safety; `AmountDetector` es local; gana ~4 instancias por candidato |
| Términos explícitos en ignoreWords | "sub" como substring es demasiado amplio; los subtotales ya están cubiertos por `subtotalKeywords` |

### Opciones rechazadas

**Opción A: Aplicar `correctOcrErrors()` al `fullText` antes de toda la detección**
- ❌ Corrompería keywords ("TOTAL" → "T0TA1", "SUBTOTAL" → "5U8T0TA1")
- ✅ Aplicarlo solo en búsqueda de montos es más seguro

**Opción B: ThreadLocal para `NumberFormat`**
- ❌ Overkill; `AmountDetector` ya es instancia local por procesamiento
- ✅ Campo de clase es suficiente y más simple

---

## Consecuencias

### Directas

- ✅ **Fix #1 activa la corrección OCR real:** Caracteres como O/0, l/1, S/5 ahora se corrigen antes de que el regex intente capturarlos — la mejora de ADR-038 finalmente funciona
- ✅ **Fix #2 elimina ~200 compilaciones de regex por ticket:** `looksLikeNonMonetary()` ya no aloca objetos `Regex` en hot path
- ✅ **Fix #3 elimina ~245 instancias de `NumberFormat` por ticket:** 5 instancias pre-construidas reutilizadas
- ✅ **Fix #4 elimina falsos negativos por "sub":** Líneas con "subscription" o "subway" ya no se filtran incorrectamente
- ✅ **Sin breaking changes:** API pública sin modificaciones (`OcrResult`, `detect()`, `Confidence`)
- ⚠️ **`correctOcrErrors()` más agresiva:** Ahora afecta texto más amplio (líneas completas vs grupos de dígitos). El guard `hasNearbyDigits` en la función mitiga conversiones incorrectas en texto puramente alfabético

### Técnicas

**Archivos impactados:**
- `app/src/main/java/com/alvaronolasco/creditcardtracker/ocr/OcrProcessor.kt`
  - Eliminado: `import java.util.regex.Pattern`
  - Nuevo: `findAmountsInLine()` (helper, ~2 líneas)
  - Nuevo: `currencyInStringRegex` (propiedad de clase)
  - Nuevo: `localeFormatters` (propiedad de clase)
  - Modificado: `parseAmount()` — sin `correctOcrErrors()`, usa `localeFormatters`, reutiliza `ParsePosition`
  - Modificado: `looksLikeNonMonetary()` — sin compilación de regex en body
  - Modificado: `ignoreWords` — sin `"sub"`, con subtotales explícitos
  - Modificado: 9 llamadas a `amountRegex.findAll()` → `findAmountsInLine()`

**Breaking changes:** Ninguno.

### Operacionales

**Testing requerido:**
- [ ] Ticket con `"1O5.OO"` (O en lugar de 0) → debe detectar `105.00`
- [ ] Ticket con `"S0.00"` (S en lugar de 5) → debe detectar `50.00` si hay contexto numérico
- [ ] Línea con `"Subscription Total $15.00"` → debe detectar `$15.00` (ya no filtrada por "sub")
- [ ] Línea con porcentaje `"IVA 16%"` → sigue siendo excluida por detección de `%`
- [ ] Ticket con subtotal explícito → líneas con "subtotal" siguen filtradas correctamente
- [ ] Verificar que keywords ("TOTAL", "NETO") no son corrompidos por `correctOcrErrors()`

---

## Implementación

### Paso a paso

1. ✅ Agregar `findAmountsInLine()` helper en sección OCR Error Correction
2. ✅ Eliminar llamada a `correctOcrErrors()` de `parseAmount()`
3. ✅ Reemplazar 9 llamadas `amountRegex.findAll()` → `findAmountsInLine()`
4. ✅ Agregar `currencyInStringRegex` como propiedad de clase
5. ✅ Agregar `localeFormatters` como propiedad de clase
6. ✅ Refactorizar `parseAmount()` para usar `localeFormatters` y reutilizar `ParsePosition`
7. ✅ Refactorizar `looksLikeNonMonetary()` — string search para porcentaje, `currencyInStringRegex` para moneda
8. ✅ Actualizar `ignoreWords` — eliminar `"sub"`, agregar subtotales explícitos
9. ✅ Eliminar `import java.util.regex.Pattern`
10. ⏳ Agregar/actualizar tests unitarios para caracteres OCR corregidos
11. ⏳ Validar en dispositivo real con tickets de baja calidad

---

## Validación

```kotlin
// Fix #1 — correctOcrErrors activa en búsqueda de montos
detectFromText("TOTAL 1O5.OO").amount == 105.0   // O→0 corregido antes del regex
detectFromText("TOTAL S0.00").amount == 50.0      // S→5 con contexto numérico

// Fix #4 — ignoreWords no filtra por "sub" genérico
detectFromText("Subscription Total $15.00").amount == 15.0   // ya no filtrado
detectFromText("Subtotal $100.00\nTOTAL $108.50").amount == 108.50  // subtotal sigue filtrado
```

---

## Notas y Aprendizajes

- **Corrección pre-regex vs post-regex:** La corrección de caracteres OCR debe aplicarse al texto crudo, antes de que el pattern trate de matchear. Aplicarla después del match solo opera sobre texto ya validado como dígitos.
- **Hot path awareness:** Funciones llamadas O(n·m) veces (n líneas × m layers) deben evitar allocations. `Regex()` y `NumberFormat.getNumberInstance()` son costosos; inicializarlos una vez es un win fácil.
- **Substring filters deben ser palabras completas:** `"sub"` como filtro de substring es una trampa. Preferir términos completos o boundary matching.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-07 | Documento inicial — 4 fixes implementados |

---

## Referencias

- [ADR-038](ADR-038-ocr-accuracy-improvements.md) — Introduce `correctOcrErrors()` (bug corregido aquí)
- [ADR-034](ADR-034-ocr-parsing-robustness.md) — `parseAmount()` y `looksLikeNonMonetary()` (base)
- [ADR-036](ADR-036-ocr-amount-scoring-system.md) — Scoring system (contexto de layers de detección)
