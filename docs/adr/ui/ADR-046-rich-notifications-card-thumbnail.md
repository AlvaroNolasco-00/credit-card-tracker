# ADR-046 — Notificaciones ricas con miniatura de tarjeta personalizada

**Categoría:** ui  
**Estado:** Aceptado  
**Fecha:** 2026-04-11  
**Relacionado con:** ADR-019 (toggles de notificación), ADR-017 (sistema de colores)

---

## Contexto

Las notificaciones de corte y pago usaban un diseño genérico:
- Ícono de sistema (`android.R.drawable.ic_dialog_info`)
- Título plano: `"Tarjeta Visa Signature — BBVA"`
- Mensaje plano: `"Tu fecha de pago es en 3 día(s)"`
- Sin color, sin identidad visual de la tarjeta
- No se distinguía visualmente entre diferentes tarjetas del usuario

El usuario solicitó elevar la calidad visual de las notificaciones incorporando la identidad de cada tarjeta (colores, banco, últimos 4 dígitos) de forma similar a una miniatura de la tarjeta creada en la app.

---

## Decisión

**Generar un Bitmap de la mini tarjeta en tiempo de notificación usando Android Canvas, y mostrarlo como `largeIcon` (vista colapsada) y como ImageView en un RemoteViews expandido.**

### Arquitectura de la solución

```
ReminderScheduler          ReminderReceiver            NotificationHelper
      │                          │                            │
      │  Intent extras:          │                            │
      │  cardName, bank,         │  CardNotificationData      │  CardBitmapHelper
      │  lastFour, cardColor, ──►│ ────────────────────────►  │ ─────────────────►
      │  type, daysBefore,       │                            │  Bitmap (Canvas)
      │  eventDate               │                            │
      │                          │                            │  largeIcon (collapsed)
      │                          │                            │  RemoteViews (expanded)
```

### Componentes nuevos

| Archivo | Rol |
|---------|-----|
| `CardBitmapHelper.kt` | Genera bitmap de la tarjeta con Canvas (gradiente, chip dorado, texto) |
| `CardNotificationData` | Data class que agrupa todos los datos necesarios para la notificación |
| `notification_expanded.xml` | Layout RemoteViews para vista expandida (ImageView + detalles) |
| `ic_notification_card.xml` | Vector monochrome (blanco/transparente) para el status bar |
| `notification_card_background.xml` | Shape redondeada como placeholder del thumbnail |

### Diseño de la miniatura (CardBitmapHelper)

- **Dimensiones por defecto:** 240×150 px (relación 16:10, similar a tarjeta física)
- **Fondo:** LinearGradient en 3 pasos: lighten(color, 1.15) → color → darken(color, 0.50)
- **Shimmer:** dos círculos translúcidos (alpha=25) en la zona superior derecha
- **Texto banco:** uppercase, 13.5% de altura, alpha=190
- **Texto nombre:** bold, 16.5% de altura, blanco opaco
- **Chip EMV:** `#D4AF37` (dorado) con línea divisoria horizontal `#B8860B`
- **Últimos 4 dígitos:** fuente monospace, "•••• XXXX", alpha=210

### Vista de notificación

**Colapsada:**
- `setSmallIcon(R.drawable.ic_notification_card)` — ícono propio, no genérico de sistema
- `setLargeIcon(bitmap)` — miniatura de la tarjeta (160×100 px generados como 16:10)
- `setColor(cardColor)` — acento en el encabezado del sistema (Android 6+)
- Título: `"Pago · Visa Signature"` o `"Corte · Visa Signature"`
- Texto: `"En 3 días — viernes, 18 de abril"`

**Expandida** (`DecoratedCustomViewStyle` + `setCustomBigContentView`):
- Izquierda: miniatura de tarjeta 96dp×60dp (bitmap 240×150)
- Derecha: badge de tipo (CORTE/PAGO en color de la tarjeta), nombre+banco, mensaje, fecha

### Datos pasados por el Intent (ReminderScheduler → ReminderReceiver)

| Extra | Tipo | Antes | Ahora |
|-------|------|-------|-------|
| `cardName` | String | Parte de `title` | Campo independiente |
| `bank` | String | Parte de `title` | Campo independiente |
| `lastFour` | String | — | Nuevo |
| `cardColor` | Int | — | Nuevo |
| `notificationType` | String | — | Nuevo |
| `daysBefore` | Int | — | Nuevo |
| `eventDate` | String | — | Nuevo (formateado en Scheduler) |
| `id` | Int | `id` | Sin cambio |

Los campos `title` y `message` genéricos fueron eliminados; la lógica de presentación vive ahora en `NotificationHelper`.

---

## Alternativas consideradas

### A. NotificationCompat.BigPictureStyle
Permite agregar una imagen grande debajo del contenido estándar. Descartada porque:
- La imagen ocupa todo el ancho, pero el contenido de texto queda como el template estándar
- No permite layout lateral (imagen izquierda + texto derecho)
- Menos control sobre la composición visual

### B. Completamente custom con `setCustomContentView` y `setCustomBigContentView`
Posible, pero en Android 12+ las notificaciones con `setCustomContentView` pierden el estilo adaptativo del sistema (bordes, colores, modo oscuro). `DecoratedCustomViewStyle` mantiene el encabezado estándar del sistema y solo personaliza el cuerpo.

### C. Glance (Compose para RemoteViews)
Disponible desde Compose 1.1, pero con restricciones: no soporta todos los casos de uso de notificaciones, añade dependencia pesada, y la API de notificaciones con Glance aún no es estable. Descartado.

---

## Consecuencias

**Positivas:**
- Notificaciones visualmente diferenciadas por tarjeta (color, banco, número)
- El usuario identifica de un vistazo de cuál tarjeta es el aviso sin abrir la app
- Vista expandida compacta que no requiere entrar a la app para el contexto clave
- Sin dependencias adicionales — usa únicamente APIs de Android estándar

**Negativas / riesgos:**
- `CardBitmapHelper.generate()` se ejecuta en el proceso de `BroadcastReceiver` (hilo principal)
  - Mitigado: el bitmap es pequeño (240×150, ~140 KB sin comprimir), la operación tarda <5 ms
- En Android 12+ la apariencia de notificaciones cambió; `DecoratedCustomViewStyle` garantiza compatibilidad, pero los colores de texto en `notification_expanded.xml` dependen del tema del sistema
- El bitmap es regenerado en cada disparo del alarm — no hay caché entre reintentos, lo que es correcto dado que los datos de la tarjeta pueden cambiar

---

## Archivos modificados/creados

| Archivo | Tipo |
|---------|------|
| `notifications/CardBitmapHelper.kt` | Nuevo |
| `notifications/NotificationHelper.kt` | Modificado |
| `notifications/ReminderScheduler.kt` | Modificado |
| `notifications/ReminderReceiver.kt` | Modificado |
| `res/layout/notification_expanded.xml` | Nuevo |
| `res/drawable/ic_notification_card.xml` | Nuevo |
| `res/drawable/notification_card_background.xml` | Nuevo |
