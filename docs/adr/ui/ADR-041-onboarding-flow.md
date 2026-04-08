# ADR-041: Onboarding Flow — Presentación de funcionalidades al primer lanzamiento

**Fecha:** 2026-04-08  
**Estado:** Aceptado  
**Categoría:** ui  
**Prioridad:** High  
**Afecta:** `MainActivity`, `Navigation`, `UserPreferencesRepository`, nuevo paquete `ui/onboarding/`

---

## Contexto

La app no tenía ningún flujo de bienvenida. El primer lanzamiento iba directo al dashboard vacío sin contexto sobre qué hace la app ni cómo empezar. Usuarios nuevos debían descubrir todas las funcionalidades por exploración propia, lo cual dificulta la adopción y reduce el valor percibido.

La única interacción de "primera vez" existente era el `NameSetupBottomSheet` en el dashboard, que recoge el nombre del usuario pero no explica nada sobre las funcionalidades.

---

## Decisión

### Opción elegida
Pantalla de onboarding multi-paso con `HorizontalPager` de 7 páginas (6 de features + 1 de nombre/bienvenida). Se muestra únicamente en el primer lanzamiento, controlado por la preferencia `onboarding_completed` en SharedPreferences.

La última página integra el input de nombre del usuario (anteriormente en `NameSetupBottomSheet`), creando un único flujo de primera vez.

### Por qué esta opción
- `HorizontalPager` ya está en uso en `DashboardScreen`, sin dependencias nuevas
- Lectura síncrona de `isOnboardingCompleted()` en `MainActivity.onCreate()` determina el `startDestination` antes de la primera composición — patrón estándar de Android sin splash screen adicional
- Integrar el nombre en la última página elimina la duplicación con el bottom sheet del dashboard (que queda como fallback si el usuario omite el campo)
- `popUpTo("onboarding") { inclusive = true }` garantiza que el back button no vuelva al onboarding tras completarlo

### Opciones rechazadas
**Onboarding como overlay/dialog:**
- ❌ No permite navegación swipe natural entre páginas
- ❌ Difícil de integrar con el input de nombre

**Splash screen con check de onboarding:**
- ❌ Añade latencia visual innecesaria
- ❌ Complejidad extra (nuevo Activity o animaciones de transición)

---

## Consecuencias

### Directas
- ✅ Usuarios nuevos entienden las 6 funcionalidades clave antes de ver el dashboard
- ✅ El input de nombre queda en un contexto más apropiado (cierre del onboarding)
- ✅ "Omitir" disponible en cualquier página excepto la última para usuarios que ya conocen la app (reinstalaciones)
- ⚠️ Si el usuario ya tiene la app instalada con `onboarding_completed = false` (dato no existente), verá el onboarding en la próxima actualización — comportamiento correcto e intencionado

### Técnicas
**Archivos/módulos creados:**
- `ui/onboarding/OnboardingScreen.kt` — Pantalla completa con HorizontalPager
- `ui/onboarding/OnboardingViewModel.kt` — Estado de paginación y lógica de completado
- `ui/onboarding/OnboardingPageData.kt` — Data class y lista de 7 páginas

**Archivos/módulos modificados:**
- `data/repository/UserPreferencesRepository.kt` — `isOnboardingCompleted()`, `setOnboardingCompleted()`
- `MainActivity.kt` — Inyecta repo, determina startDestination
- `ui/navigation/Navigation.kt` — Parámetro `startDestination`, ruta `"onboarding"`

**Breaking changes:**
- Ninguno. `NameSetupBottomSheet` en el dashboard permanece como fallback para usuarios que omiten el nombre en onboarding.

### Operacionales
- Testing requerido: manual en dispositivo/emulador, primer y segundo lanzamiento
- Sin migraciones de datos necesarias

---

## Implementación

### Paso a paso
1. `UserPreferencesRepository` — agregar `KEY_ONBOARDING_COMPLETED`, `isOnboardingCompleted()`, `setOnboardingCompleted()`
2. Crear `OnboardingPageData.kt` con data class y lista de páginas
3. Crear `OnboardingViewModel.kt` con estado y lógica de completado
4. Crear `OnboardingScreen.kt` con HorizontalPager, PageIndicator, botones Siguiente/Omitir/Comenzar
5. `MainActivity.kt` — inyectar repo, computar startDestination antes de setContent
6. `Navigation.kt` — aceptar `startDestination: String`, agregar ruta `"onboarding"`

---

## Validación

- [ ] Primer lanzamiento: aparece onboarding (7 páginas)
- [ ] Swipe y botón "Siguiente" avanzan páginas
- [ ] "Omitir" en cualquier página (excepto la última) va al dashboard y marca completado
- [ ] "Comenzar" en la última página guarda el nombre (si hay) y va al dashboard
- [ ] Segundo lanzamiento: va directo al dashboard
- [ ] Si se omite nombre: `NameSetupBottomSheet` aparece en dashboard
- [ ] Si se ingresa nombre: `NameSetupBottomSheet` no aparece

---

## Cambios posteriores

### 2026-04-08 — Acceso al onboarding desde SupportScreen

Agregada opción "Ver tour de la app" en `SupportScreen` para permitir a usuarios ya registrados revisar el onboarding en cualquier momento.

**Cambios:**
- `SupportScreen.kt` — Nueva tarjeta `AppTourCard` al inicio de la lista
- `Navigation.kt` — Lógica de `onFinished` adaptada: si hay pantalla anterior, hace `popBackStack()` (vuelve a SupportScreen); si no (primer lanzamiento), va al dashboard con `popUpTo inclusive`

**Comportamiento:**
- Primer lanzamiento: Onboarding → Dashboard
- Desde SupportScreen: Onboarding → vuelve a SupportScreen (no interrumpe el flujo)

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-08 | Documento inicial |
| 2026-04-08 | Agregada opción de acceso desde SupportScreen |
