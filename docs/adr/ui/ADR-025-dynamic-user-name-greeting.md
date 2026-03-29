# ADR-025: Saludo dinámico con nombre de usuario y bottom sheet de configuración

**Fecha:** 2026-03-29
**Estado:** Aceptado
**Categoría:** ui

## Contexto

El saludo en el Dashboard mostraba el nombre "Alvaro" hardcodeado. Se necesitaba hacerlo dinámico: pedir el nombre al usuario la primera vez que abre la app y usarlo para personalizar la experiencia.

## Decisión

- Se creó `UserPreferencesRepository` que persiste el nombre en `SharedPreferences` (`user_prefs`) y expone un `StateFlow<String?>`.
- Hilt provee `SharedPreferences` como `@Singleton` en `AppModule`.
- `DashboardUiState` incorpora `userName: String?` y `showNamePrompt: Boolean`.
- `DashboardViewModel` lee el `StateFlow` del repositorio y activa `showNamePrompt` cuando el nombre es nulo o vacío.
- En `DashboardScreen` se muestra un `ModalBottomSheet` (`NameSetupBottomSheet`) cuando `showNamePrompt == true`.
- El bottom sheet incluye: ícono de persona, título amigable, campo de texto con foco automático, botón "Continuar" y opción "Ahora no" para diferir sin obligar.
- El saludo muestra "¡Hola!" si no hay nombre, o "Hola, {nombre}" cuando está configurado.

Se eligió `SharedPreferences` sobre Room porque es un único valor escalar de usuario, sin necesidad de queries ni relaciones.

## Consecuencias

- Primera apertura muestra el bottom sheet automáticamente.
- El usuario puede omitirlo con "Ahora no"; el prompt reaparece al reiniciar la app hasta que guarde el nombre.
- No se requiere migración de base de datos.
- El nombre es local al dispositivo, no se sincroniza.
