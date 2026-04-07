# ADR-035: OCR Processor Lifecycle Management — Prevención de Memory Leaks

**Fecha:** 2026-04-07  
**Estado:** Aceptado  
**Categoría:** architecture  
**Prioridad:** High  
**Afecta:** `OcrProcessor`, `ExpensesViewModel`, OCR Flow  

---

## Contexto

El cliente `TextRecognition` de ML Kit mantiene recursos significativos en memoria (RAM/GPU) para procesar imágenes de manera eficiente. Sin gestión explícita del ciclo de vida, el uso continuo del OCR puede causar:

- **Memory Leaks:** El `recognizer` nunca se libera aunque la pantalla de OCR se destruya
- **OutOfMemoryError:** Acumulación de recursos no recolectados en el garbage collector
- **Degradación de performance:** La app se ralentiza con cada uso del OCR

**Investigación previa:**
- ML Kit documentation sugiere llamar a `.close()` en el recognizer cuando ya no se use
- Android best practice: liberar recursos pesados vinculados al lifecycle de componentes (ViewModel, Activity)
- Patrón: Implementar `Closeable` es el estándar de Java para recursos que deben ser explícitamente liberados

---

## Decisión

### Opción elegida

Implementar `Closeable` en `OcrProcessor` y delegar explícitamente la liberación de recursos al `onCleared()` del `ViewModel` que lo usa.

```kotlin
// OcrProcessor.kt
class OcrProcessor(private val context: Context) : Closeable {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun close() {
        recognizer.close()
    }
}

// ExpensesViewModel.kt
@HiltViewModel
class ExpensesViewModel @Inject constructor(...) : ViewModel() {
    private val ocrProcessor = OcrProcessor(context)

    override fun onCleared() {
        super.onCleared()
        ocrProcessor.close()
    }
}
```

### Por qué esta opción

- ✅ **Estándar de Java:** `Closeable` es la interfaz canónica para recursos que requieren limpieza explícita
- ✅ **Integración con lifecycle:** `onCleared()` es el punto exacto en que Android destruye el ViewModel (navegación, fin de actividad, etc.)
- ✅ **No requiere cambios en la API pública:** El usuario del `OcrProcessor` sigue llamando a `processImage()` normalmente; la limpieza es transparente
- ✅ **Portabilidad:** Si se necesita usar `OcrProcessor` en otro ViewModel u Activity, la interfaz `Closeable` lo hace obvio que necesita limpieza
- ✅ **Try-with-resources compatible:** Futuro soporte para sintaxis `try (OcrProcessor ocr = ...) { ... }`

### Opciones rechazadas

**Opción A: Usar `@onCleared()` en `OcrProcessor` directamente**
- ❌ `OcrProcessor` no es un ViewModel, no tiene acceso a `onCleared()`
- ❌ Acoplamiento innecesario a Android lifecycle en una clase de negocio

**Opción B: Usar `SafeCloseable` / destructor personalizado**
- ❌ Kotlin no garantiza cuando se llama al destructor (no determinístico)
- ❌ Los recursos podrían no liberarse a tiempo bajo presión de memoria

**Opción C: Usar un singleton `OcrProcessor` que nunca se cierra**
- ❌ Incumple best practice de liberación de recursos
- ❌ Consumo permanente de memoria incluso si el usuario no usa OCR

---

## Consecuencias

### Directas

- ✅ **Eliminación de memory leak:** El `recognizer` se libera cuando el ViewModel se destruye
- ✅ **Mejor estabilidad:** Menos riesgo de `OutOfMemoryError` en uso continuo
- ✅ **Performance:** Recuperación ágil de memoria GPU/RAM para otros procesos
- ⚠️ **Responsabilidad del caller:** Quien instancie `OcrProcessor` ahora **debe** llamar a `.close()` cuando termine

### Técnicas

**Archivos/módulos impactados:**

| Archivo | Cambio |
|---------|--------|
| `app/src/.../ocr/OcrProcessor.kt` | Implementa `Closeable`, sobrecarga `close()` |
| `app/src/.../ui/expenses/ExpensesViewModel.kt` | Agrega `onCleared()` para llamar a `close()` |
| `docs/adr/INDEX.md` | Registro de esta decisión |
| `CHANGELOG.md` | Entry en sección [Security] |

**Breaking changes:**
- No hay cambios de API pública. Código existente que llama a `processImage()` sigue funcionando sin cambios.
- Nuevo: Si alguien crea un segundo ViewModel con `OcrProcessor`, debe implementar `onCleared()` de la misma forma.

### Operacionales

- **Testing requerido:** 
  - ✅ Verificar que `onCleared()` se invoca al navegar de vuelta desde ExpensesScreen
  - ✅ Profiler de memoria: confirmar liberación de recursos
  - ✅ Stress test: múltiples OCR -> navegación -> OCR nuevamente
  
- **Documentación:** 
  - ✅ Este ADR sirve como documentación del patrón para futuros ViewModels que usen recursos pesados

- **Comunicación:** 
  - Soporte al QA/Testing para validar que no hay memory leaks en sesiones largas

---

## Implementación

### Paso a paso

1. **Modificar `OcrProcessor.kt`:**
   - Agregar `: Closeable` a la clase
   - Implementar `override fun close()` que llama a `recognizer.close()`

2. **Modificar `ExpensesViewModel.kt`:**
   - Agregar `override fun onCleared()` que llama a `ocrProcessor.close()`

3. **Validar en dispositivo:**
   - Abrir ExpensesScreen → Iniciar OCR → Navegar atrás
   - Verificar en Android Profiler que memoria se libera

4. **Actualizar documentación:**
   - Este ADR (hecho ✅)
   - INDEX.md con entrada de ADR-035
   - CHANGELOG.md con nota de seguridad

### Files de referencia

- Commit: `<próximo commit OCR lifecycle management>`
- Implementación en: 
  - `app/src/main/java/.../ocr/OcrProcessor.kt`
  - `app/src/main/java/.../ui/expenses/ExpensesViewModel.kt`

---

## Validación

### Cómo verificar

- [ ] Navego a ExpensesScreen → inicio OCR → vuelvo atrás → verifico en logcat que `recognizer.close()` es llamado
- [ ] Android Profiler (Memory tab): memoria de OCR se libera tras regresar de ExpensesScreen
- [ ] Stress test: 5+ ciclos OCR → back → OCR sin OutOfMemoryError
- [ ] `gradle test` pasa sin regresiones

### Métricas de éxito

- Cero OutOfMemoryError en sesiones de OCR extendidas
- Memory footprint estable incluso con múltiples ciclos de OCR
- Tiempo de garbage collection reducido (menos acumulación de objetos muertos)

---

## Notas y Aprendizajes

- **Patrón reutilizable:** Si en el futuro se agregan otros recursos pesados (WebView, ExoPlayer, etc.), seguir el mismo patrón `Closeable` + `onCleared()`
- **Kotlin Coroutines:** Los `ViewModel` con Hilt automáticamente invocan `onCleared()`, así que no se necesita limpieza manual en `viewModelScope`
- **Try-with-resources:** En futuro, si se usan `OcrProcessor` en Activity o Composable directamente, considerar usar `try (...) { }` de Java 7+

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-07 | Documento inicial — implementación de `Closeable` + `onCleared()` |

---

## Referencias

- [Google ML Kit — TextRecognition cleanup](https://developers.google.com/ml-kit/vision/text-recognition)
- [Android ViewModel.onCleared()](https://developer.android.com/reference/androidx/lifecycle/ViewModel#onCleared())
- [Java Closeable interface](https://docs.oracle.com/javase/8/docs/api/java/lang/AutoCloseable.html)
- [ADR-034](../ADR-034-ocr-parsing-robustness.md) — OCR Parsing Robustness (decisión previa en OCR)
