# ADR-001: Widget muestra todas las tarjetas via LazyColumn

**Fecha:** 2026-03-25
**Estado:** Aceptado
**Categoría:** widget

## Contexto

El widget Glance tenía un límite fijo en la cantidad de tarjetas mostradas por layout:
- SmallLayout: `.take(2)` + texto "+ X más"
- MediumLayout: `.take(2)` + texto "+ X más"
- LargeLayout: `.take(3)` + texto "+ X más"

El usuario con más de 3 tarjetas no podía ver todas desde el widget, obligándolo a abrir la app.

## Decisión

Reemplazar los `Column + forEach + .take(N)` de los tres layouts por `LazyColumn + items()` de `androidx.glance.appwidget.lazy`, eliminando completamente los límites y el texto de overflow.

Cambios en `CreditCardWidget.kt`:
- Importar `androidx.glance.appwidget.lazy.LazyColumn` y `androidx.glance.appwidget.lazy.items`
- `SmallLayout`: `LazyColumn` dentro de la Column header, sin `take()`
- `MediumLayout`: `LazyColumn` a pantalla completa, sin `take()`
- `LargeLayout`: `LazyColumn` a pantalla completa, sin `take()`
- Eliminar todos los bloques `if (cards.size > N) { Text("+ X mas") }`
- Pasar `context: Context` a todos los layouts y composables de tarjeta (preparación para ADR-002)

## Consecuencias

- El widget es ahora un listado scrollable; el usuario puede deslizar verticalmente para ver todas sus tarjetas.
- Se elimina la deuda visual de "X más" que confundía cuántas tarjetas había realmente.
- `itemId = { it.card.id.toLong() }` garantiza reciclaje eficiente en RemoteViews.
- Glance 1.0.0 soporta `LazyColumn` y múltiples acciones por item; verificado con la versión en `app/build.gradle.kts`.
