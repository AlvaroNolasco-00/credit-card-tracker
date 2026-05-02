# ADR-061: APK Filename Includes Version Name

**Fecha:** 2026-04-30
**Estado:** Aceptado
**Categoría:** architecture
**Prioridad:** Low
**Afecta:** Build system, CI/CD artifact naming

---

## Contexto

Cada build de la app genera un APK con nombre genérico (`app-debug.apk` / `app-release-unsigned.apk`). Al distribuir builds internos o subir artefactos a un registro, es difícil identificar qué versión corresponde a cada archivo sin inspeccionar el contenido o revisar los logs de CI. Se necesita un identificador visible en el nombre del archivo.

Investigamos tres opciones:
1. Configurar `outputFileName` directamente en la variante (AGP `applicationVariants`)
2. Usar `androidComponents.onVariants` con la nueva API de AGP
3. Post-procesar el APK con un task Gradle que renombre después del empaquetado

Opciones 1 y 2 fallaron en AGP 8.3.1 porque:
- `outputFileName` no está disponible en la API pública de `ApplicationVariant` en Kotlin DSL
- `setOutputFile` vía reflection arroja `SecurityException` (método restringido por AGP)
- `androidComponents.onVariants` no expone `versionName` directamente en el contexto de outputs

---

## Decisión

### Opción elegida
Registrar un `doLast` en cada task `assemble*` vía `afterEvaluate` + `applicationVariants.all`. El hook:
1. Obtiene `buildType.name` y `versionName` de la variante
2. Busca el task `assemble<Variant>` correspondiente
3. Al finalizar, lista los APKs en `outputs/apk/<buildType>/`
4. Renombra cualquier APK que no contenga ya el `versionName` al formato `app-<buildType>-<versionName>.apk`
5. Loguea éxito o fallo del `renameTo`

### Por qué esta opción
- **Funciona con AGP 8.3.1 + Kotlin DSL**: no depende de APIs internas bloqueadas
- **No interfiere con el empaquetado**: el APK se genera normalmente y se renombra después
- **Manejo de errores explícito**: `renameTo` retorna `boolean`; se loguea fallo en vez de silenciarlo
- **Idempotente**: si el APK ya tiene el versionName en el nombre, no se toca

### Opciones rechazadas

**Opción A: `outputFileName` en variante**
- ❌ `outputFileName` no es miembro de `BaseVariantOutput` en AGP 8.3.1 con Kotlin DSL
- ❌ Casting a `ApkVariantOutputImpl` arroja type mismatch o SecurityException

**Opción B: `androidComponents.onVariants`**
- ❌ La API de variantes no expone `versionName` ni `outputFileName` de forma directa en este AGP
- ❌ `SingleArtifact.APK` requiere artifact transforms más complejos para un renombre simple

---

## Consecuencias

### Directas
- ✅ Cada APK ahora incluye la versión en su nombre: `app-debug-1.0.apk`
- ✅ Fácil identificación de builds sin abrir el archivo
- ✅ Compatible con pipelines de CI que consumen `outputs/apk/*/`
- ⚠️ El renombre ocurre *después* del task `assemble`; herramientas que lean el APK en el mismo task loop deben esperar a `doLast`

### Técnicas
**Archivos/módulos impactados:**
- `app/build.gradle.kts` — Task `renameApkWithVersion` y hook en `assemble*` tasks
- `docs/adr/` — Este registro

**Breaking changes:**
- Ninguno. El APK original se renombra in-place; las referencias existentes en scripts de CI deben esperar a que `assemble` termine.

### Operacionales
- Testing requerido: manual (ejecutar `./gradlew assembleDebug` y verificar nombre en `outputs/apk/debug/`)
- Documentación: este ADR
- Comunicación: equipo de release / CI si hay scripts que hardcodean `app-debug.apk`

---

## Implementación

### Paso a paso
1. Agregar bloque `afterEvaluate` en `app/build.gradle.kts`
2. Iterar `applicationVariants.all` para obtener build type y version name
3. Encontrar el task `assemble<Variant>` y agregar `doLast` con renombre
4. Filtrar APKs que ya contengan `-${versionName}.apk` para idempotencia
5. Loguear resultado de `renameTo`

### Files de referencia
- PR: —
- Commit: —
- Build script: `app/build.gradle.kts`

---

## Validación

### Cómo verificar que la decisión se implementó correctamente
- [ ] Ejecutar `./gradlew assembleDebug` y confirmar que existe `app/build/outputs/apk/debug/app-debug-<versionName>.apk`
- [ ] Ejecutar `./gradlew assembleRelease` y confirmar que existe `app/build/outputs/apk/release/app-release-<versionName>.apk`
- [ ] Verificar que un segundo build consecutivo no falle (idempotencia)
- [ ] Verificar que el build pasa: `./gradlew build test`

### Métricas de éxito
- 100% de builds generan APK con version name incluido
- 0% de renombres fallidos silenciosos

---

## Notas y Aprendizajes

- AGP 8.3.1 con Kotlin DSL tiene limitaciones para modificar `outputFileName` directamente; el post-procesamiento es más confiable
- `afterEvaluate` es necesario porque `applicationVariants` se pobla en la fase de configuración de AGP
- `renameTo` puede fallar si el destino ya existe; el filtro de idempotencia previene esto

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-30 | Documento inicial |

---

## Referencias

- [Android Gradle Plugin DSL — ApplicationVariant](https://developer.android.com/reference/tools/gradle-api/8.3/com/android/build/api/variant/ApplicationVariant)
- [Gradle Task Lifecycle](https://docs.gradle.org/current/userguide/build_lifecycle.html)
