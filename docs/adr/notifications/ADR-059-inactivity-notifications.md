# ADR-059: Notificaciones de Inactividad — Recordatorios tras 3 y 7 días sin usar la app

**Fecha:** 2026-04-27
**Estado:** Aceptado
**Categoría:** notifications, ui
**Prioridad:** Medium
**Afecta:** notifications, MainActivity, DashboardScreen, UserPreferencesRepository, Navigation

---

## Contexto

Los usuarios que dejan de usar la app por varios días pierden el hábito de registrar gastos y terminamos con datos desactualizados. No existe actualmente ningún mecanismo de re-engagement pasivo. Investigamos:

1. **Firebase Cloud Messaging** (push remoto) — Requiere backend, overkill para este feature.
2. **WorkManager periódico** — Menos preciso que AlarmManager; no garantiza exactitud en horario.
3. **AlarmManager exacto + BroadcastReceiver** — Reutiliza infraestructura existente de recordatorios de tarjeta; preciso; no requiere backend.

---

## Decisión

### Opción elegida
Usar `AlarmManager.setExactAndAllowWhileIdle()` con un `BroadcastReceiver` dedicado (`InactivityReminderReceiver`) que dispare notificaciones locales en 3 y 7 días desde la última apertura de la app. El contador se reinicia en `MainActivity.onResume()`.

### Por qué esta opción
- Reutiliza el patrón ya probado de `ReminderScheduler` / `ReminderReceiver`.
- No requiere backend ni permisos adicionales.
- Sobrevive a reinicios del dispositivo (BootReceiver reschedules).
- Totalmente configurable por el usuario con toggle en Settings.

### Opciones rechazadas
**FCM (Firebase Cloud Messaging)**
- ❌ Requiere backend y costos operacionales.
- ❌ Overkill para un simple recordatorio de inactividad.

**WorkManager periódico**
- ❌ No garantiza horario exacto (minimum 15 min interval).
- ❌ Mayor overhead de batería para algo que solo necesita 2 disparos.

---

## Consecuencias

### Directas
- ✅ Re-engagement pasivo sin backend.
- ✅ Usuario puede desactivar desde Settings (default: habilitado).
- ✅ Al abrir la app, las alarmas se reprograman automáticamente.

### Técnicas
**Archivos/módulos impactados:**
- `UserPreferencesRepository.kt` — Prefs `inactivity_notifications_enabled`, `last_app_open`
- `InactivityReminderScheduler.kt` — Nuevo: gestión de alarmas
- `InactivityReminderReceiver.kt` — Nuevo: disparo de notificación
- `NotificationHelper.kt` — Nuevo canal `inactivity_channel` + método `showInactivityNotification()`
- `BootReceiver.kt` — Reprograma alarmas de inactividad tras reinicio
- `MainActivity.kt` — `onResume()` actualiza `lastAppOpen` + reschedules
- `DashboardScreen.kt` — Icono Settings en header
- `Navigation.kt` — Ruta `settings`
- `SettingsScreen.kt` / `SettingsViewModel.kt` — Nuevos: toggle de configuración
- `AndroidManifest.xml` — Registro del receiver

### Operacionales
- Testing requerido: manual (device/emulator)
- Documentación: este ADR + CHANGELOG

---

## Implementación

### Paso a paso
1. Extender `UserPreferencesRepository` con prefs de inactividad.
2. Crear `InactivityReminderScheduler` con 2 alarmas (día 3, día 7).
3. Crear `InactivityReminderReceiver` que reciba y dispare notificación.
4. Extender `NotificationHelper` con canal separado y copy diferenciado.
5. Actualizar `BootReceiver` para reschedular alarmas de inactividad.
6. En `MainActivity.onResume()`, actualizar timestamp y reprogramar.
7. Crear `SettingsScreen` + `SettingsViewModel` con toggle.
8. Agregar icono Settings en `DashboardScreen` y ruta en `Navigation`.
9. Registrar receiver en `AndroidManifest.xml`.

---

## Validación

- [ ] Al abrir la app, `last_app_open` se actualiza en SharedPreferences.
- [ ] Al cerrar la app, alarmas para día 3 y día 7 están programadas.
- [ ] Al reiniciar el dispositivo, BootReceiver reprograma ambas alarmas.
- [ ] Al desactivar el toggle en Settings, las alarmas se cancelan.
- [ ] Build pasa: `./gradlew build test`

---

## Notas y Aprendizajes

- Usar `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE` en `PendingIntent` para compatibilidad con Android 12+.
- El canal de inactividad usa `IMPORTANCE_DEFAULT` (no HIGH) para no ser intrusivo.
- No se requiere migración de base de datos; todo vive en SharedPreferences.

---

## Referencias

- [ADR-049](ui/ADR-049-overdue-payment-alerts.md) — Patrón similar con AlarmManager
- [Android AlarmManager docs](https://developer.android.com/reference/android/app/AlarmManager)
