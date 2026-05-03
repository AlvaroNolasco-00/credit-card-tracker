# ADR-067: Migración a AndroidX ExifInterface para compatibilidad de orientación de imágenes

**Fecha:** 2026-05-02
**Estado:** Aceptado
**Categoría:** architecture
**Prioridad:** Low
**Afecta:** `app/build.gradle.kts`, `AddExpenseScreen.kt`

---

## Contexto

`AddExpenseScreen.kt` usa `ExifInterface` para leer la orientación de imágenes seleccionadas desde la galería y rotarlas correctamente antes de enviarlas al pipeline de OCR. El import original era `android.media.ExifInterface`, disponible desde API 24 para imágenes JPEG y API 5 para formatos limitados.

Aunque `minSdk = 26` cubre la API 24, la implementación del framework varía entre versiones de Android y carece de soporte para algunos formatos y tags. La biblioteca `androidx.exifinterface:exifinterface` proporciona una implementación unificada, más robusta y con correcciones de bugs backporteadas.

---

## Decisión

### Opción elegida

Reemplazar `android.media.ExifInterface` por `androidx.exifinterface.media.ExifInterface` y agregar la dependencia `androidx.exifinterface:exifinterface:1.3.7` en `app/build.gradle.kts`.

### Por qué esta opción

- ✅ Implementación unificada y mantenida por AndroidX — recibe fixes sin depender de actualizaciones del SO.
- ✅ API consistente en todos los niveles de SDK soportados.
- ✅ Reduce acoplamiento con clases del framework cuando existe una alternativa de AndroidX estable.

### Opciones rechazadas

**Opción A: Mantener `android.media.ExifInterface` nativo**
- ❌ Comportamiento inconsistente en diferentes versiones de Android.
- ❌ Menor soporte de tags EXIF.

---

## Consecuencias

### Directas

- ✅ El pipeline de OCR recibe imágenes con orientación corregida de forma fiable.
- ✅ Build incluye una nueva dependencia AndroidX (~80 KB adicionales en el APK).

### Técnicas

**Archivos/módulos impactados:**

| Archivo | Cambio |
|---------|--------|
| `app/build.gradle.kts` | `implementation("androidx.exifinterface:exifinterface:1.3.7")` |
| `AddExpenseScreen.kt` | Import cambiado de `android.media.ExifInterface` a `androidx.exifinterface.media.ExifInterface` |

**Breaking changes:** Ninguno. La API pública de `ExifInterface` de AndroidX es un drop-in replacement.

### Operacionales

- Testing: `./gradlew build test`
- Documentación: este ADR, `CHANGELOG.md` y `docs/adr/INDEX.md`

---

## Implementación

### Paso a paso

1. Agregar dependencia en `app/build.gradle.kts`.
2. Reemplazar import en `AddExpenseScreen.kt`.
3. Verificar build y tests.

---

## Validación

- [ ] `./gradlew build test` pasa sin errores.
- [ ] Seleccionar imagen rotada desde galería → OCR la procesa con orientación correcta.

---

## Notas y Aprendizajes

- Migrar dependencias del framework a AndroidX es una práctica recomendada incluso cuando `minSdk` ya cubre la API nativa, porque AndroidX ofrece paridad y fixes más rápidos.

---

## Referencias

- [AndroidX ExifInterface release notes](https://developer.android.com/jetpack/androidx/releases/exifinterface)
