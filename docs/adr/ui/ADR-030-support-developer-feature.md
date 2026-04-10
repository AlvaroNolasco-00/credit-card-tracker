# ADR-030: Feature "Apoya al Desarrollador" — Donaciones con enlaces externos

**Fecha:** 2026-03-31
**Estado:** Aceptado
**Categoría:** ui

## Contexto

Se necesitaba una forma de recibir apoyo voluntario (donaciones) de los usuarios de la app, integrada orgánicamente con la UI/UX. El requerimiento explícito fue que no fuera "un simple botón", sino algo coherente con la identidad financiera de la app.

## Decisión

Se implementó una tarjeta de soporte en el dashboard (`SupportDeveloperCard`) que aparece de forma controlada por frecuencia (primera aparición a los 7 días de uso, se puede dismissear por 30 días). Al tocarla, navega a `SupportScreen` — una pantalla dedicada con:

- Header animado (icono de corazón con efecto de pulso)
- Mensaje del desarrollador en `AppCard`-style card
- Tres tiers de donación ("Un café", "Un almuerzo", "Un día de internet") presentados como tarjetas de transacción — jugando con la identidad financiera de la app
- Sección secundaria: calificar en Play Store + compartir la app

### Mecanismo de pago: enlaces externos vía `Intent.ACTION_VIEW`

Se eligieron enlaces externos (Buy Me a Coffee / PayPal.me) en lugar de Google Play Billing por:
- **Simplicidad**: cero dependencias nuevas, un solo `Intent.ACTION_VIEW` por tier
- **Costo de integración**: Google Play Billing requiere SDK, Play Console setup, productos In-App configurados, verificación de compras y manejo de estados de compra
- **Naturaleza voluntaria**: las donaciones son tips voluntarios, no bienes digitales; los enlaces externos son el estándar del sector para apps indie
- **Política de Play Store**: los enlaces externos para donaciones voluntarias a apps gratuitas están permitidos

### Visibilidad controlada por frecuencia

La tarjeta no aparece siempre para no ser intrusiva:
- Primera aparición: después de 7 días desde el primer lanzamiento
- Dismissable: el usuario puede cerrarla; no reaparece por 30 días
- Estado gestionado en `UserPreferencesRepository` via `SharedPreferences` (sin Room — es metadata de UX, no datos de negocio)

## Consecuencias

- Se extiende `UserPreferencesRepository` con tres nuevas preference keys: `first_launch_at`, `support_dismissed_at`, y el método `shouldShowSupportCard()`
- Se agrega `showSupportCard: Boolean` a `DashboardUiState` y `dismissSupportCard()` a `DashboardViewModel`
- Nuevo archivo: `ui/support/SupportScreen.kt` (~250 líneas, stateless — sin ViewModel)
- Nueva ruta de navegación: `"support_developer"`
- Las URLs de donación son constantes en `SupportScreen.kt` — deben actualizarse con la URL real antes de publicar en producción
- Sin cambios a Room, sin nuevas entidades, sin migraciones de base de datos

---

## Actualización: ADR-030-A — Wompi como método de pago alternativo (2026-04-10)

### Contexto

El método de pago PayPal tenía una barrera: requería que el donante tuviera una cuenta de PayPal. Usuarios sin cuenta no podían donar. Además, en El Salvador hay alternativas locales más accesibles.

### Decisión

Se agregó Wompi (pasarela de pago local de Banco Agrícola) como método de pago alternativo:

- **Selector visual**: Chips interactivos para alternar entre "💳 Tarjeta (Wompi)" y "🅿️ PayPal (Cuenta)"
- **Wompi como default**: Seleccionado por defecto para eliminar la barrera de "necesitas cuenta"
- **Enlaces reutilizables**: 3 enlaces Wompi para $3/$5/$10 USD (sin backend, solo redirección a browser)
- **Montos ajustados**: Wompi usa montos ligeramente diferentes ($3 vs $2) por configuración local
- **Wompi acepta**: Tarjetas Visa/Mastercard + Bitcoin (donante paga, desarrollador recibe USD en cuenta bancaria)

### Racional

- **Wompi no requiere cuenta del donante**: Solo tarjeta de crédito/débito
- **Local**: Operado por Banco Agrícola en El Salvador, más accesible para usuarios locales
- **Comisión**: 3.5% por transacción (similar a PayPal)
- **Sin backend**: Usa enlaces de pago reutilizables, igual que PayPal — cero código de servidor adicional
- **Flexibilidad**: Usuario elige su método preferido

### Implementación

- Nuevo enum `PaymentMethod` (Wompi, PayPal)
- URLs de Wompi como constantes: `WOMPI_URL_COFFEE`, `WOMPI_URL_LUNCH`, `WOMPI_URL_INTERNET`
- Nuevo composable `PaymentMethodChip` para el selector
- `DonationTiersSection` recibe `selectedPaymentMethod` y `onPaymentMethodChange`
- Estado local `selectedPaymentMethod` en `SupportScreen` (default: Wompi)
- UI dinámica: montos y textos actualizados según método seleccionado

### Consecuencias

- `SupportScreen.kt` crece de ~250 a ~480 líneas
- Mayor conversión potencial al eliminar barrera de cuenta PayPal
- Mantén compatibilidad con PayPal para usuarios que lo prefieran
- Sin cambios a arquitectura, Room, o capas de datos
