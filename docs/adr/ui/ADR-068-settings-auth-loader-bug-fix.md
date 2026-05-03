# ADR-068: Fix loader infinito en Settings → Cuenta

- **Estado**: Aceptado
- **Fecha**: 2026-05-02
- **Categoría**: ui

## Contexto

La sección "Cuenta" en `SettingsScreen` mostraba un `CircularProgressIndicator` con el texto "Cargando sesión..." que nunca resolvía. Se identificó una cadena de tres bugs:

1. **Bug UI** — `AccountSection` usaba `else` en el `when (authState)`, capturando tanto `AuthState.Loading` como `AuthState.Unauthenticated` con el mismo spinner. Cualquier estado distinto a `Authenticated` resultaba en loader eterno.

2. **Silent failure** — `AuthRepository.ensureSignedIn()` envolvía `signInAnonymously().await()` en `runCatching {}` sin loggear ni propagar el fallo. Si el sign-in anónimo fallaba (sin red, Firebase caído, primer launch), `currentUser` quedaba `null`, el listener emitía `Unauthenticated`, y el Bug 1 activaba el spinner eterno.

3. **ANR risk** — `MainActivity.onCreate()` llamaba `runBlocking { authRepository.ensureSignedIn() }`, bloqueando el main thread mientras esperaba respuesta de Firebase. Con red lenta podía disparar un ANR.

## Decisión

### 1. Separar los 3 estados en `AccountSection`

Reemplazar la rama `else` por tres ramas explícitas en el `when`:
- `AuthState.Loading` → spinner + "Cargando sesión..."
- `AuthState.Unauthenticated` → icono `CloudOff` + mensaje de error + botón "Reintentar conexión"
- `AuthState.Authenticated` → comportamiento existente (anónima/vinculada)

Agregar parámetro `onRetry: () -> Unit` a `AccountSection`.

### 2. `ensureSignedIn` devuelve `Result` y loggea fallos

```kotlin
suspend fun ensureSignedIn(): Result<Unit> {
    if (auth.currentUser != null) return Result.success(Unit)
    return runCatching {
        auth.signInAnonymously().await()
        Unit
    }.onFailure { Log.e("AuthRepository", "Anonymous sign-in failed", it) }
}
```

### 3. Mover sign-in anónimo fuera del main thread

Eliminar `runBlocking { ensureSignedIn() }` de `MainActivity`. Mover la llamada a `CreditCardTrackerApp.onCreate()` usando el `applicationScope` ya provisto por `AppModule` (SupervisorJob):

```kotlin
applicationScope.launch { authRepository.ensureSignedIn() }
```

### 4. Botón "Reintentar conexión"

`SettingsViewModel` expone `retrySignIn()` que relanza `ensureSignedIn()`. El botón en la rama `Unauthenticated` lo invoca, permitiendo al usuario reintentar sin reiniciar la app.

## Consecuencias

**Positivas:**
- UI nunca queda atascada en loader — cada estado tiene representación visual correcta
- Main thread libre durante sign-in (sin riesgo de ANR)
- Fallos de sign-in anónimo ahora visibles en Logcat (`AuthRepository: Anonymous sign-in failed`)
- Usuario puede reintentar manualmente cuando recupera conexión

**Negativas / Trade-offs:**
- Posible flash breve `Loading → Unauthenticated` si la red falla rápido en primer launch
- La sincronización de datos locales generados mientras `currentUser == null` sigue pendiente (issue separado, fuera del alcance de este ADR)

## Archivos modificados

- `app/src/main/java/com/alvaronolasco/creditcardtracker/data/repository/AuthRepository.kt`
- `app/src/main/java/com/alvaronolasco/creditcardtracker/MainActivity.kt`
- `app/src/main/java/com/alvaronolasco/creditcardtracker/CreditCardTrackerApp.kt`
- `app/src/main/java/com/alvaronolasco/creditcardtracker/ui/settings/SettingsViewModel.kt`
- `app/src/main/java/com/alvaronolasco/creditcardtracker/ui/settings/SettingsScreen.kt`
