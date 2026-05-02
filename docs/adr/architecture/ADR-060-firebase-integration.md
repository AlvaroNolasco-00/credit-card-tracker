# ADR-060: Integración de Firebase como Backend — Auth, Firestore y Cloud Storage

**Fecha:** 2026-04-27
**Estado:** Extendido por ADR-062
**Categoría:** architecture
**Prioridad:** High
**Afecta:** `app/build.gradle.kts`, `build.gradle.kts` (raíz), `di/`, `data/`, `ui/`, sync layer

---

## Contexto

La aplicación Credit Card Tracker ha operado como una app 100% offline desde su creación. Todos los datos (tarjetas, gastos, presupuestos, ingresos, actividad) viven en una base de datos Room SQLite local (`credit_card_tracker_db`). Las imágenes de recibos se almacenan como archivos locales. No existe backend, API REST, ni sincronización en la nube.

Esta arquitectura offline tiene ventajas claras:
- **Privacidad total**: los datos financieros del usuario nunca salen del dispositivo.
- **Funcionamiento sin conexión**: la app trabaja en aviones, áreas rurales o con datos limitados.
- **Simplicidad**: sin autenticación, sin rate limits, sin costos de infraestructura.

Sin embargo, a medida que la app madura, surgen necesidades que un backend resolvería:
1. **Sincronización multi-dispositivo**: un usuario con un teléfono y una tablet no puede compartir sus datos.
2. **Backup automático**: si el teléfono se pierde o rompe, los datos financieros se pierden permanentemente.
3. **Acceso web futuro**: eventualmente se podría construir una web app complementaria.
4. **Promociones bancarias**: el scraper-bot de Python (`scraper-bot/`) genera JSON localmente pero no alimenta la app. Un backend podría centralizar esto.
5. **Colaboración familiar**: múltiples usuarios podrían gestionar un presupuesto familiar compartido.

Se evaluaron varias opciones de backend:

- **Firebase (BaaS)**: plataforma managed de Google. Auth, Firestore (NoSQL), Storage, Functions.
- **Supabase**: alternativa open-source sobre PostgreSQL. Auth, REST/Realtime, Storage, Edge Functions.
- **Backend propio con Ktor + PostgreSQL**: control total, compartir lógica Kotlin, posible KMP.
- **Room + Sync Engine híbrido**: mantener Room como fuente de verdad local y agregar una capa de sync.

---

## Decisión

### Opción elegida
**Firebase** como backend inicial, utilizando:
- **Firebase Auth** (`firebase-auth-ktx`): autenticación anónima, email/password y Google Sign-In.
- **Cloud Firestore** (`firebase-firestore-ktx`): base de datos NoSQL documental para sincronización de entidades.
- **Cloud Storage** (`firebase-storage-ktx`): almacenamiento de imágenes de recibos en la nube.
- **Firebase Analytics** (`firebase-analytics-ktx`): métricas de uso (opcional).

### Por qué esta opción
- **Integración nativa con Android**: SDKs oficiales de Kotlin con soporte de corrutinas (`kotlinx-coroutines-play-services` ya está en dependencias).
- **Firestore offline persistence**: la base de datos se puede cachear localmente automáticamente, lo que reduce la fricción para usuarios con conectividad intermitente.
- **Costo inicial cero**: el plan Spark es gratuito para volúmenes bajos (perfecto para una app personal/familiar).
- **Sin servidores que mantener**: no necesitamos deployar ni escalar infraestructura.
- **Scraping centralizado**: Cloud Functions pueden ejecutar el scraper-bot y servir promociones a la app.
- **Hilt-friendly**: FirebaseAuth, FirebaseFirestore y FirebaseStorage son objetos singleton que se inyectan fácilmente con un `@Module`.

### Opciones rechazadas

**Opción A: Supabase**
- ❌ El proyecto ya tiene un ecosistema Google (ML Kit, Hilt via Google). Firebase reduce la fragmentación de proveedores.
- ❌ Firestore tiene mejor integración offline y sync automático en Android que Supabase Realtime.
- ✅ *Nota*: Supabase sigue siendo una alternativa válida si en el futuro se necesita un modelo relacional estricto o self-hosting.

**Opción B: Backend propio con Ktor + PostgreSQL**
- ❌ Sobre-ingeniería para la etapa actual. No necesitamos control total ni KMP todavía.
- ❌ Costo operacional (hosting, base de datos, backups) desde el día 1.
- ✅ *Nota*: Reevaluar si la app escala a miles de usuarios o se necesita una web app robusta.

**Opción C: Room + Sync Engine híbrido sobre backend propio**
- ❌ Añade complejidad innecesaria en esta fase. El sync engine es útil, pero Firebase ya ofrece persistencia offline.
- ✅ *Nota*: Si en el futuro migraramos a un backend propio, el patrón de sync engine sería el camino natural.

---

## Consecuencias

### Directas
- ✅ Los usuarios podrán autenticarse y sincronizar sus datos entre dispositivos.
- ✅ Las imágenes de recibos se respaldarán en Cloud Storage.
- ✅ Se abre la puerta para Cloud Functions y promociones bancarias centralizadas.
- ⚠️ Los datos financieros del usuario ahora residen en servidores de Google. Esto requiere una política de privacidad actualizada y consideraciones de compliance (aunque Firestore está cifrado en tránsito y en reposo por defecto).
- ❌ La app ya no es 100% offline. Aunque Firestore tiene persistencia offline, ciertas funciones (login inicial, sync de imágenes) requieren conexión.

### Técnicas
**Archivos/módulos impactados:**
- `build.gradle.kts` (raíz) — Plugin `com.google.gms.google-services`
- `app/build.gradle.kts` — Plugin y dependencias Firebase (BOM, Auth, Firestore, Storage, Analytics)
- `app/google-services.json` — Configuración del proyecto Firebase (no versionado en Git)
- `di/AppModule.kt` o nuevo `di/FirebaseModule.kt` — Proveedores Hilt para FirebaseAuth, FirebaseFirestore, FirebaseStorage
- `data/CreditCardRepository.kt` — Posiblemente extender con operaciones de sync a Firestore
- Nuevos repositorios/paquetes: `data/sync/`, `data/remote/`

**Breaking changes:**
- Ninguno en esta fase. El ADR solo cubre la *configuración* del SDK. La migración de datos de Room a Firestore será un trabajo posterior con su propio ADR.
- Se requiere agregar `google-services.json` al `.gitignore` para evitar filtrar credenciales.

### Operacionales
- Testing requerido: manual (login, verificar que Firebase se inicializa) + device (Firestore write/read)
- Documentación: este ADR, actualización de README con instrucciones para colaboradores sobre `google-services.json`
- Comunicación: los colaboradores necesitarán crear su propio proyecto Firebase de desarrollo o usar el `google-services.json` compartido de forma segura

---

## Implementación

### Paso a paso
1. Registrar app en Firebase Console con package `com.alvaronolasco.creditcardtracker`
2. Descargar `google-services.json` y colocarlo en `app/`
3. Agregar plugin `com.google.gms.google-services` en `build.gradle.kts` (raíz) y `app/build.gradle.kts`
4. Agregar dependencias Firebase BOM + Auth + Firestore + Storage + Analytics en `app/build.gradle.kts`
5. Sincronizar Gradle y verificar que compila (`./gradlew assembleDebug`)
6. Crear `di/FirebaseModule.kt` con `@Module @InstallIn(SingletonComponent::class)` proveedores para:
   - `FirebaseAuth.getInstance()`
   - `FirebaseFirestore.getInstance()`
   - `FirebaseStorage.getInstance()`
7. Implementar login anónimo como MVP (sin UI de login por ahora) para asignar un `uid` a cada instalación
8. Agregar `.gitignore` entry para `app/google-services.json`

### Files de referencia
- PR: (a crear)
- Commit: (a crear)
- Configuración Firebase: `app/google-services.json` (local, no en Git)

---

## Validación

### Cómo verificar que la decisión se implementó correctamente
- [ ] `./gradlew assembleDebug` compila sin errores tras agregar dependencias
- [ ] `FirebaseApp.initializeApp(context)` no arroja excepciones al iniciar la app
- [ ] `FirebaseAuth.getInstance().currentUser` retorna un usuario anónimo tras el primer launch
- [ ] `FirebaseFirestore.getInstance()` es inyectable vía Hilt en un ViewModel de prueba
- [ ] `FirebaseStorage.getInstance()` es inyectable vía Hilt

### Métricas de éxito
- Build time no aumenta significativamente (<10%)
- Tamaño del APK no crece desproporcionadamente (Firebase Auth + Firestore ≈ +2-3 MB)
- Sin crashes relacionados a Firebase en el startup

---

## Notas y Aprendizajes

- Firebase BOM (`firebase-bom:33.7.0`) gestiona automáticamente la compatibilidad de versiones entre los SDKs. No especificar versiones individuales reduce conflictos.
- `firebase-auth-ktx`, `firebase-firestore-ktx` y `firebase-storage-ktx` son las variantes Kotlin que exponen APIs con coroutines y lambdas más idiomáticas.
- El plugin `com.google.gms.google-services` lee `app/google-services.json` en build time y genera el `R.xml.google_app_id` necesario para runtime.
- Si en el futuro se decide migrar a Supabase o backend propio, el patrón de repositorio abstracto (`CreditCardRepository`) facilita el swap: solo cambia la implementación, no los ViewModels.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-27 | Documento inicial |

---

## Referencias

- [Firebase Android Setup Guide](https://firebase.google.com/docs/android/setup)
- [Firebase Kotlin KTX Docs](https://firebase.google.com/docs/reference/kotlin)
- [Firestore Offline Persistence](https://firebase.google.com/docs/firestore/manage-data/enable-offline)
- [ADR-005](architecture/ADR-005-widget-deeplink-singleton.md) — Patrón de singletons transversales (precedente para `FirebaseModule`)
