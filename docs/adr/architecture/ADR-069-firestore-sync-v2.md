# ADR-069: Firestore Sync v2 — Root Collections + Incremental Sync

**Fecha:** 2026-05-02
**Estado:** Aceptado
**Categoría:** architecture | data
**Prioridad:** High
**Afecta:** data, SyncManager, Mappers, FirestoreSyncRepository, 8 Room entities, DAOs

---

## Contexto

ADR-062 implementó sync básico a Firestore bajo subcollections `users/{uid}/cards`, `users/{uid}/expenses`, `users/{uid}/categories`. Evaluamos la arquitectura actual y detectamos:

### Problemas del modelo actual
1. **Subcollections**: dificultan queries cross-user y escalabilidad horizontal
2. **pullAndReplace destructivo**: `DeleteAll → InsertAll` pierde datos locales
3. **Fire-and-forget silencioso**: `syncLaunch { runCatching { push } }` traga errores sin cola de reintentos
4. **Sin `updatedAt`**: no hay forma de resolver conflictos o hacer sync incremental
5. **Solo 3 entidades syncadas**: presupuestos, ingresos, gastos recurrentes y notificaciones no se sincronizan

### Requerimientos
- Root collections en Firestore (no subcollections)
- Sync incremental con resolución de conflictos (last-writer-wins)
- Cola de pendientes con reintentos (max 5 intentos)
- Event-based sync (no periódica): auth, CRUD completado, app foreground
- 8 entidades syncadas (incluyendo nuevas)
- Categorías default también se sincronizan

---

## Decisión

### Opción elegida
Migrar a **10 colecciones raíz** con `userId` field + **incremental sync** vía `updatedAt` + **sync_queue** Room table.

### Arquitectura Sync v2

```
CRUD en Room → enqueueSync(entityType, entityId, action)
                     ↓
              SyncQueue (Room)
                     ↓
          SyncManager.processQueue()
                     ↓
        Firestore root collections (pushes)
                     ↓
          SyncManager.syncFromCloud()
        (pull incremental por updatedAt)
```

### Colecciones Firestore

| Colección | Doc ID | Incluye |
|-----------|--------|---------|
| `users` | `{uid}` | perfil de usuario |
| `cards` | `{uid}_{id}` | CreditCard + userId + updatedAt |
| `expenses` | `{uid}_{id}` | Expense + userId + updatedAt + categoryIds[] |
| `categories` | `{uid}_{id}` | Category + userId + updatedAt |
| `budgets` | `{uid}_{id}` | BudgetItem + userId + updatedAt |
| `incomes` | `{uid}_{id}` | IncomeEntry + userId + updatedAt |
| `income_profiles` | `{uid}` | IncomeProfile + userId + updatedAt |
| `recurring_expenses` | `{uid}_{id}` | RecurringExpense + userId + updatedAt + categoryIds[] |
| `notification_configs` | `{uid}_{id}` | NotificationConfig + userId + updatedAt |

### Algoritmo incremental

```
syncAll(uid):
  1. PUSH: processQueue()
     - Leer sync_queue ordenado por createdAt
     - Por cada item:
       - UUID → Firestore doc ID = {uid}_{entityId}
       - DELETE → firestore.doc(id).delete()
       - UPSERT → leer Room, toFirestoreMap(uid), firestore.doc(id).set()
       - Éxito → dequeue
       - Falla → incrementAttempt (max 5)

  2. PULL: syncFromCloud()
     - lastSync = prefs.getLastSyncTimestamp()
     - Para cada colección:
       - Query: .whereEqualTo("userId", uid)
       - Para cada doc:
         - NO existe en Room → insert
         - cloud.updatedAt > local.updatedAt → update local
         - local.updatedAt >= cloud.updatedAt → enqueue push
     - prefs.setLastSyncTimestamp(now())
```

### Conflict resolution
- **Last-writer-wins** por `updatedAt`
- Empate (`local.updatedAt == cloud.updatedAt`) → gana local (se encola push)
- Sincronización es **unidireccional por sentido**: push local, pull cloud
- Sin merge semántico (suficiente para app personal monousuario)

### Triggers de sync (event-based, no periódico)
1. **Auth**: `onAuthenticated()` → `syncAll(uid)`
2. **CRUD completado**: `enqueueSync()` + `requestSync(debounceMs=2000)`
3. **App foreground**: `requestSync(debounceMs=30000)`

---

## Consecuencias

### Directas
- ✅ Root collections: queries más flexibles, sin límite de profundidad
- ✅ Sync incremental: no se pierden datos locales en pull
- ✅ Cola de reintentos: hasta 5 intentos antes de descartar
- ✅ `updatedAt` en todas las entidades: permite resolver conflictos
- ✅ 8 entidades sincronizadas (vs 3 anteriores)
- ⚠️ Composite index `userId ASC` necesario en cada colección (Firestore lo crea automáticamente)

### Técnicas

**Archivos nuevos:**
- `data/entity/SyncQueueItem.kt` — Room entity para cola de sync
- `data/dao/SyncQueueDao.kt` — DAO para sync_queue
- `data/firestore/MigrationHelper.kt` — Migración desde subcollections viejas

**Archivos modificados:**
- 8 entidades Room: `+updatedAt` field
- 5 DAOs: `+getById` queries para pull
- `data/firestore/Mappers.kt` — Mappers para 8 entidades
- `data/FirestoreSyncRepository.kt` — Root collections API
- `data/SyncManager.kt` — Algoritmo incremental
- `data/repository/CreditCardRepository.kt` — enqueueSync
- `data/repository/UserPreferencesRepository.kt` — lastSyncTimestamp
- `data/AppDatabase.kt` — Migration 15→16 + SyncQueueDao
- `di/AppModule.kt` — SyncQueueDao provider

**Breaking changes:**
- Subcollections `users/{uid}/cards` etc. dejan de usarse
- `SyncSnapshot`, `pullAllUserData()` se eliminan
- `last_synced_uid` se reemplaza por `last_sync_timestamp`

### Operacionales
- Testing: `./gradlew test`
- Migración automática Room 15→16 en primer launch
- Firebase Security Rules deben cubrir root collections

---

## Implementación

1. Crear `SyncQueueItem.kt` + `SyncQueueDao.kt`
2. Añadir `updatedAt` a 8 entities Room
3. Reescribir `Mappers.kt` con userId + updatedAt
4. Reescribir `FirestoreSyncRepository.kt` con root collections
5. Reescribir `SyncManager.kt` con algoritmo incremental
6. Modificar `CreditCardRepository.kt` (enqueueSync en CRUDs)
7. Añadir `lastSyncTimestamp` a `UserPreferencesRepository`
8. Migration 15→16 en `AppDatabase.kt`
9. Proveer `SyncQueueDao` en `AppModule.kt`
10. Actualizar INDEX.md y CHANGELOG.md

---

## Estado anterior

Esta ADR depreca/supersede el modelo de sync definido en ADR-062 para la parte de subcollections y escribir los mappers respectivos.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-05-02 | Documento inicial |

---

## Referencias

- [ADR-062](../architecture/ADR-062-firebase-auth-ui-and-sync.md) — Sync básico anterior
- [Firestore Data Model](https://firebase.google.com/docs/firestore/data-model)
- [Room Migrations](https://developer.android.com/training/data-storage/room/migrating)
