# ADR-062: Firebase Auth UI + Sync Básico a Firestore

**Fecha:** 2026-05-01
**Estado:** Aceptado
**Categoría:** architecture
**Prioridad:** High
**Afecta:** auth, data, ui, navigation, settings

---

## Contexto

ADR-060 (2026-04-27) configuró Firebase (BOM 33.7.0, plugins, google-services.json) pero su MVP era solo login anónimo sin UI. El usuario solicitó implementar auth completa con UI visible + sincronización básica de datos por usuario.

**Requerimientos definidos:**
- Login + Register con email/password
- Google Sign-In (Credential Manager API, no GoogleSignInClient legacy)
- Reset de contraseña (sendPasswordResetEmail)
- Sesión anónima automática al primer launch (cero fricción)
- Upgrade voluntario: anónimo → email/Google via linking
- Sync básico de tarjetas, gastos y categorías a Firestore por uid

**Estado previo:** INTERNET permission faltante, sin FirebaseModule, sin código de auth, sin pantallas, sin sync.

---

## Decisión

### Modelo de auth: anónimo-first con upgrade

```
App start → FirebaseAuth.currentUser
  null               → signInAnonymously() → Dashboard
  isAnonymous=true   → Dashboard (CTA en Settings → "Vincular cuenta")
  isAnonymous=false  → Dashboard (Settings muestra email + "Cerrar sesión")
```

- Register desde sesión anónima → `linkWithCredential(EmailAuthProvider.getCredential(...))` → uid se preserva, datos vinculados
- Sign-in a cuenta existente → nuevo uid; si difiere del `KEY_LAST_SYNCED_UID`, wipe Room + pull Firestore
- Sign-out → `auth.signOut()` + `signInAnonymously()` (nuevo uid anónimo; Room local permanece hasta próximo login)

### Modelo de sync: write-through, Room = fuente de verdad

```
Repository.insertCard(card) {
    cardDao.insertCard(card)                         // immediate, source of truth
    applicationScope.launch { syncRepo.pushCard(it) } // best-effort, queued offline
}
```

- Lecturas: solo Room (Flows reactivos sin cambios)
- Firestore offline persistence ON: writes se encolan offline, SDK reintenta al reconectar
- Pull inicial: al detectar `uid != lastSyncedUid` y `!isAnonymous` → wipe + pull las 3 colecciones
- Errores de sync: `runCatching` silencioso; nunca rompen la UX

### Entidades sincronizadas (Phase 1)

- `CreditCard`, `Expense` (+ `categoryIds` inline), `Category`

**Fuera de alcance (Phase 2 → ADR-063 futuro):**
- `IncomeProfile`, `IncomeEntry`, `BudgetItem`, `RecurringExpense`
- `ActivityLog` (audit local), `NotificationConfig` (device-specific), `CategorySpending` (vista computada)

### Estructura Firestore

```
users/{uid}
  /profile               (doc): { uid, email, isAnonymous, lastLoginAt }
  /cards/{cardId}             : { ...CreditCard todos los campos }
  /expenses/{expenseId}       : { ...Expense todos los campos, categoryIds: [Int] }
  /categories/{catId}         : { ...Category todos los campos }
```

IDs: `Int.id` de Room como String en docId. Last-writer-wins en colisiones cross-device (aceptable para Phase 1).

### Por qué esta opción

- **Anónimo-first**: cero fricción, app funciona desde el primer launch sin requerir registro
- **Linking vs nuevo usuario**: al vincular cuenta anónima a email, uid no cambia → Firestore data persiste automáticamente
- **write-through async**: Room es source of truth siempre → app funciona offline sin degradación
- **Credential Manager** (no `GoogleSignInClient` legacy): recomendado por Google desde 2024 para API 16+
- **applicationScope para sync**: sobrevive a navegación/recomposición; errores silenciosos no afectan UX

### Opciones rechazadas

**Auth obligatoria (gate login)**
- ❌ Rompe a todos los usuarios existentes que no tienen cuenta
- ❌ Contra la filosofía "offline-first" del app

**WorkManager para sync periódico**
- ❌ Eventual consistency; Firestore offline SDK ya maneja reintento automático, WorkManager es redundante

**Firestore como source of truth (reads desde Firestore)**
- ❌ Requiere manejo complejo de conflictos
- ❌ App se rompe offline sin cache explícita
- ❌ Latencia en lecturas vs Room local

---

## Consecuencias

### Directas
- ✅ Usuarios pueden sincronizar tarjetas y gastos entre dispositivos
- ✅ Registro/login no bloquea el app — siempre hay al menos sesión anónima
- ✅ Google Sign-In + email/password + reset de contraseña disponibles desde Settings
- ✅ App funciona 100% offline (Room + Firestore offline persistence)
- ⚠️ Income, Budget y RecurringExpenses aún son locales hasta ADR-063

### Técnicas

**Archivos creados:**
- `di/FirebaseModule.kt` — providers FirebaseAuth, FirebaseFirestore, CredentialManager
- `data/repository/AuthRepository.kt` — auth state flow, signIn/signUp/signOut, Google Sign-In, reset
- `data/repository/FirestoreSyncRepository.kt` — push/delete/pull por entidad
- `data/firestore/Mappers.kt` — extensiones toFirestoreMap / toEntity para CreditCard, Expense, Category
- `data/SyncManager.kt` — observa authState, detecta uid change, wipe + pull
- `ui/auth/AuthLandingScreen.kt`, `LoginScreen.kt`, `LoginViewModel.kt`
- `ui/auth/RegisterScreen.kt`, `RegisterViewModel.kt`
- `ui/auth/ForgotPasswordScreen.kt`, `ForgotPasswordViewModel.kt`
- `ui/components/PasswordTextField.kt` — wrapper AppTextField con toggle visibilidad

**Archivos modificados:**
- `app/build.gradle.kts` — deps play-services-auth + Credential Manager
- `AndroidManifest.xml` — INTERNET + ACCESS_NETWORK_STATE permissions
- `app/src/main/res/values/strings.xml` — placeholder `default_web_client_id`
- `CreditCardTrackerApp.kt` — inyecta SyncManager, llama `syncManager.start()`
- `MainActivity.kt` — inyecta AuthRepository, `runBlocking { authRepo.ensureSignedIn() }`
- `data/repository/CreditCardRepository.kt` — inyecta FirestoreSyncRepository + CoroutineScope, write-through hooks
- `data/repository/UserPreferencesRepository.kt` — `KEY_LAST_SYNCED_UID` key
- `di/AppModule.kt` — `provideRepository` incluye nuevos params + `@Singleton`
- `ui/components/AppTextField.kt` — params: isError, errorText, visualTransformation, leadingIcon
- `ui/navigation/Navigation.kt` — rutas: auth_landing, login, register, forgot_password
- `ui/settings/SettingsScreen.kt` + `SettingsViewModel.kt` — sección Cuenta
- DAOs: `CreditCardDao`, `ExpenseDao`, `ExpenseCategoryDao`, `CategoryDao` — `deleteAll()` + `getAllOnce()`

### Acción manual del usuario (no automatizable)

1. **SHA-1 fingerprint** para Google Sign-In:
   ```bash
   ./gradlew signingReport
   ```
   SHA1 de `:app:debug` → Firebase Console → Project Settings → Android app → Add fingerprint

2. **Re-descargar `google-services.json`** después de agregar SHA-1 (incluirá `default_web_client_id`). Reemplazar `app/google-services.json`.

3. **Habilitar providers** en Firebase Console → Authentication → Sign-in method:
   - Email/Password: ON
   - Google: ON
   - Anonymous: ON

4. **Reglas Firestore** (Console → Firestore → Rules):
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{uid}/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == uid;
       }
     }
   }
   ```

---

## Buenas prácticas aplicadas

- Passwords nunca almacenados: FirebaseAuth gestiona persistencia local cifrada
- Errores mapeados a `AuthError` sealed class (no se expone FirebaseAuthException a la UI)
- Validación cliente antes de hit a Firebase (email regex, password ≥8 chars, match)
- Loading states: botones disabled + AppLoadingIndicator durante operaciones in-flight
- authState como `Flow<AuthState>` via `callbackFlow + addAuthStateListener` (no polling)
- applicationScope para sync (no viewModelScope): sobrevive recomposición y navegación

---

## Validación

- [ ] Primer launch: Firebase Console > Authentication muestra usuario anónimo
- [ ] Settings → "Vincular cuenta" → Register → uid igual en Firebase Console (link exitoso)
- [ ] Crear tarjeta → Firestore Console muestra `users/{uid}/cards/{id}`
- [ ] Modo avión → crear gasto → reactivar wifi → aparece en Firestore
- [ ] Logout → nuevo uid anónimo en Firebase Console
- [ ] Login con email previo → wipe Room + pull → datos de la cuenta reaparecen
- [ ] Login en segundo dispositivo → datos sincronizados a Room local
- [ ] Forgot password → email recibido en bandeja
- [ ] Google Sign-In funciona (requiere SHA-1 configurado en paso manual #1)

---

## Notas y Aprendizajes

- `ensureSignedIn()` usa `runBlocking` en `MainActivity.onCreate` — es rápido porque FirebaseAuth cachea la sesión localmente; no bloquea el hilo principal en condiciones normales
- Para pull inicial, SyncManager inserta directo en DAOs (no via CreditCardRepository) para evitar loop de write-through sync
- `default_web_client_id` en `strings.xml` es un placeholder; el google-services plugin lo sobreescribe con el valor real cuando Google Sign-In está habilitado en Firebase Console y el SHA-1 está registrado

---

## Referencias

- [ADR-060](ADR-060-firebase-integration.md) — Configuración inicial de Firebase (Extendido por este ADR)
- [Android Credential Manager docs](https://developer.android.com/identity/sign-in/credential-manager)
- [Firebase Auth — Anonymous sign-in with account linking](https://firebase.google.com/docs/auth/android/anonymous-auth)
