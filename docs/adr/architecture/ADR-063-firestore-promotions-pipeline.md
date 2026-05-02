# ADR-063: Pipeline de Promociones Bancarias — Scraper Python + Colección Firestore

**Fecha:** 2026-05-02  
**Estado:** Aceptado  
**Categoría:** architecture  
**Prioridad:** High  
**Afecta:** `scraper-bot/`, Firestore (`promotions/`), futura integración Android

---

## Contexto

La app necesita mostrar promociones vigentes de los 6 bancos SV soportados (Agrícola, Cuscatlán, BAC, Promerica, Davivienda, Credicomer). Cada banco publica sus promos en su portal web con formatos heterogéneos: JSON API (Agrícola), GraphQL intercept (Cuscatlán) y DOM scraping (BAC, Promerica, Davivienda, Credicomer).

Se necesita una pipeline que:
1. Extraiga las promos de cada banco periódicamente.
2. Las publique en un lugar accesible para la app Android.
3. Permita deduplique entre corridas y soft-delete de promos que desaparecen.

El proyecto ya tiene Firebase integrado (ADR-060, ADR-062) con Firestore y Auth, lo que hace natural usarlo como backend de este pipeline.

---

## Decisión

### Opción elegida

**Scraper Python (Admin SDK) → colección top-level `promotions/{promoId}` en Firestore.**

- El scraper `scraper-bot/main.py` corre **manualmente** desde local.
- Autentica con **Service Account JSON** via `GOOGLE_APPLICATION_CREDENTIALS` (Admin SDK, ignora Firestore Security Rules).
- Escribe en colección `promotions/{promoId}` con **UPSERT batched** (`set(merge=True)`).
- Doc ID = `sha1(bank + merchant + title)[:20]` — estable entre runs, permite dedupe sin lectura previa.
- Al final de cada banco: **soft delete** de docs con `last_seen_at < run_started_at` → `is_active = false`.
- **JSON local** (`output/<bank>_<ts>.json`) se mantiene como backup de debug.
- La app Android lee con cualquier usuario autenticado (incl. anónimo) vía Security Rules.

### Schema del documento

```json
{
  "promo_id":     "<sha1-20>",
  "bank":         "Banco Agricola",
  "bank_slug":    "agricola",
  "merchant":     "Cinemark",
  "title":        "2x1 entradas martes",
  "description":  "...",
  "category":     "Entretenimiento",
  "valid_until":  "2026-12-31",
  "days":         "1,2,3,4,5",
  "benefit":      "50% descuento",
  "image":        "https://...",
  "is_active":    true,
  "first_seen_at": <Timestamp>,
  "last_seen_at":  <Timestamp>,
  "scraped_at":    <Timestamp>,
  "run_id":       "2026-05-02_14-30-00"
}
```

### Por qué esta opción

- **Reutiliza infraestructura existente** — Firestore ya está en el proyecto (ADR-060).
- **UPSERT idempotente** — correr el scraper N veces no duplica docs.
- **Histórico de presencia** — `first_seen_at` / `last_seen_at` / `is_active` permite analytics y detección de promos nuevas vs eliminadas.
- **Desacoplado del ciclo de release Android** — promos se actualizan sin publicar un APK nuevo.
- **Lectura offline** — Firestore persistence ya habilitado en `FirebaseModule.kt:26` (`setPersistenceEnabled`).
- **Service Account** evita gestión de tokens client-side en el scraper.

### Opciones rechazadas

**REST API propia (Firebase Functions + Firestore)**
- ❌ Requiere deploy de Functions adicional.
- ❌ Mayor costo y complejidad de mantenimiento para dato que cambia poco.

**SQLite local en la app + sync manual**
- ❌ No hay mecanismo de push desde servidor.
- ❌ App requiere update para recibir promos nuevas.

**Archivo JSON en Firebase Storage**
- ❌ Sin capacidad de query (no puedes filtrar por banco, categoría, `is_active`).
- ❌ Sin dedupe ni historial.

**Hard delete en lugar de soft delete**
- ❌ Pierde historial de presencia.
- ❌ No permite analytics de qué promos aparecen/desaparecen cada semana.

---

## Consecuencias

### Directas
- ✅ App Android puede leer promos sin depender de APIs de bancos en runtime.
- ✅ Promos actualizables independientemente del ciclo de release.
- ✅ Dedupe garantizado por hash estable de (bank, merchant, title).
- ✅ `is_active=false` previene que promos expiradas aparezcan en la app.
- ⚠️ Corrida manual — depende del desarrollador para refrescar datos. No hay automatización.
- ⚠️ `valid_until` heterogéneo entre bancos (formato string libre) — normalización futura necesaria.

### Técnicas

**Archivos nuevos / modificados:**
- `scraper-bot/main.py` — recibe `PromotionStorage`, elimina `save_json` interno
- `scraper-bot/storage/base.py` — ABC `PromotionStorage`
- `scraper-bot/storage/json_storage.py` — backup local
- `scraper-bot/storage/firestore_storage.py` — Admin SDK writer con batched writes + soft delete
- `scraper-bot/storage/composite_storage.py` — delega a JSON + Firestore
- `scraper-bot/utils/promo_id.py` — `make_promo_id()` SHA1
- `scraper-bot/requirements.txt` — añade `firebase-admin`, `python-dotenv`, `tenacity`, `playwright`
- `scraper-bot/firestore.rules` — reglas de referencia para la colección
- `scraper-bot/.env.example` — `GOOGLE_APPLICATION_CREDENTIALS`, `FIREBASE_PROJECT_ID`

**Firestore Security Rules a agregar:**
```
match /promotions/{promoId} {
  allow read: if request.auth != null;
  allow write: if false;
}
```

**Breaking changes:**
- Ninguno. La colección `promotions` es nueva. No afecta colecciones `users/{uid}/...` existentes.

### Operacionales
- Service account JSON debe obtenerse de Firebase Console → Settings → Service Accounts y guardarse en `scraper-bot/config/service-account.json` (gitignored).
- Corrida manual: `cd scraper-bot && python main.py`.
- Verificación: Firestore Console → colección `promotions` → docs con `is_active=true`.

### Pendiente (follow-up)
- Modelo Kotlin `Promotion` + `PromotionsRepository` en app Android para leer la colección.
- Pantalla / sección en app para mostrar promos al usuario filtradas por banco o tarjeta.
- Automatización del scraper (GitHub Actions cron o similar).
- Normalización de `valid_until` a ISO 8601 en el scraper.

---

## Implementación

### Paso a paso completado
1. ✅ Crear `storage/` con `base.py`, `json_storage.py`, `firestore_storage.py`, `composite_storage.py`
2. ✅ Crear `utils/promo_id.py` con `make_promo_id()`
3. ✅ Refactorizar `main.py` — inyección de `PromotionStorage`, eliminar `save_json`
4. ✅ Actualizar `requirements.txt`
5. ✅ Crear `.env.example`, `.gitignore`, `firestore.rules`
6. ✅ Actualizar `README.md` con instrucciones de Firebase setup

### Setup requerido antes de primera corrida
1. Descargar service account: Firebase Console → `credit-card-3848f` → Settings → Service Accounts → Generate new private key
2. Guardar como `scraper-bot/config/service-account.json`
3. `cp .env.example .env`
4. `pip install -r requirements.txt && playwright install chromium`
5. Aplicar `firestore.rules` en Firebase Console

---

## Validación

- [ ] `python main.py` termina sin error
- [ ] Colección `promotions` existe en Firestore con docs de los 6 bancos
- [ ] Correr dos veces seguidas no duplica docs (UPSERT idempotente)
- [ ] `first_seen_at` del primer run se preserva en runs posteriores
- [ ] Doc con `last_seen_at` viejo queda con `is_active=false` tras una corrida
- [ ] Archivos JSON en `output/` se siguen generando igual que antes
- [ ] Cliente Android con auth anónimo puede leer `/promotions` (Firestore Console → Rules Playground)
- [ ] Intento de write desde cliente falla con `PERMISSION_DENIED`

---

## Notas y Aprendizajes

- `merge=True` en `batch.set()` no preserva campos no incluidos en el payload — por eso `first_seen_at` se excluye del payload en docs existentes (detectados via `db.get_all()` antes del batch).
- `finalize_bank()` usa `last_seen_at < run_started_at` para detectar stale. El `run_started_at` debe capturarse **antes** de las escrituras, no después.
- El batch limit de Firestore es 500 ops/batch — `firestore_storage.py` divide automáticamente.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-05-02 | Documento inicial |

---

## Referencias

- [ADR-060](ADR-060-firebase-integration.md) — Integración Firebase base (Auth + Firestore + Storage)
- [ADR-062](ADR-062-firebase-auth-ui-and-sync.md) — Firebase Auth UI y sync de datos de usuario
- [Firebase Admin SDK — Python](https://firebase.google.com/docs/admin/setup)
- [Firestore Batched Writes](https://firebase.google.com/docs/firestore/manage-data/transactions#batched-writes)
