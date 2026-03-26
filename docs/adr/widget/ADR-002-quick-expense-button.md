# ADR-002: Botón "+" por tarjeta en widget para gasto rápido

**Fecha:** 2026-03-25
**Estado:** Aceptado
**Categoría:** widget

## Contexto

El widget solo abría la pantalla principal de la app al hacer tap. No había forma de ir directamente al formulario de nuevo gasto para una tarjeta específica sin navegar manualmente dentro de la app.

El usuario quería una acción rápida por tarjeta: tap en "+" → formulario de gasto ya asociado a esa tarjeta.

## Decisión

Agregar un botón "+" clickable en cada tarjeta del widget (layouts Medium y Large). El botón dispara un `Intent` hacia `MainActivity` con el extra `"card_id"` = `data.card.id`.

### Implementación en el widget (`CreditCardWidget.kt`)

```kotlin
val addExpenseIntent = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    putExtra("card_id", data.card.id)
}
```

- **FullCardItem** (LargeLayout): botón `24×16dp`, `cornerRadius 8dp`, reemplaza el chip decorativo en la fila del banco (top-right).
- **MiniCardRow** (MediumLayout): botón `20×14dp`, `cornerRadius 6dp`, junto a los dígitos de la tarjeta.
- **SmallCardRow** (SmallLayout): sin botón (espacio insuficiente).

### Recepción en la app

Ver ADR-005 para el mecanismo de deep link (`WidgetDeepLink` singleton).

## Consecuencias

- El tap en "+" y el tap en el fondo del widget son acciones independientes (el inner `clickable` tiene prioridad sobre el outer en Glance/RemoteViews).
- SmallLayout no tiene botón; se asume que el usuario que elige un widget tan compacto prefiere la vista de resumen.
- En Android 12+ con widgets redondeados, el área de tap del botón puede ser pequeña; se prefirió el diseño compacto sobre uno más grande para no romper el layout de la tarjeta.
