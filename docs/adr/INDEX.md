# Architecture Decision Records — Credit Card Tracker

Registro centralizado de todas las decisiones de arquitectura, UI, datos y navegación.
Cada ADR vive en su carpeta de categoría y se lista aquí con su estado actual.

## Categorías

| Carpeta | Scope |
|---------|-------|
| `widget/` | Widget Glance (layouts, acciones, configuración) |
| `ui/` | Pantallas y componentes de la capa de presentación |
| `architecture/` | Patrones transversales, DI, comunicación entre capas |
| `data/` | Entidades Room, DAOs, repositorio |
| `navigation/` | NavHost, rutas, deep links |

## Índice

| ID | Título | Categoría | Estado | Fecha |
|----|--------|-----------|--------|-------|
| [ADR-001](widget/ADR-001-all-cards-lazy-column.md) | Widget muestra todas las tarjetas via LazyColumn | widget | Aceptado | 2026-03-25 |
| [ADR-002](widget/ADR-002-quick-expense-button.md) | Botón "+" por tarjeta en widget para gasto rápido | widget | Aceptado | 2026-03-25 |
| [ADR-003](widget/ADR-003-full-width-grid.md) | Widget ocupa 4 columnas del grid (ancho completo) | widget | Aceptado | 2026-03-25 |
| [ADR-004](ui/ADR-004-expense-card-banner.md) | Banner visual de tarjeta destino en AddExpenseScreen | ui | Aceptado | 2026-03-25 |
| [ADR-005](architecture/ADR-005-widget-deeplink-singleton.md) | WidgetDeepLink: singleton StateFlow para deep linking | architecture | Aceptado | 2026-03-25 |
| [ADR-006](widget/ADR-006-progress-bar-spending.md) | Barra de progreso de gastos vs límite en widget | widget | Aceptado | 2026-03-25 |
| [ADR-007](architecture/ADR-007-widget-update-guarantee.md) | Garantizar actualización del widget tras cambios de gastos | architecture | Aceptado | 2026-03-26 |
| [ADR-008](widget/ADR-008-widget-card-data-computed-properties.md) | WidgetCardData centraliza totalDue y progress como propiedades computadas | widget | Aceptado | 2026-03-26 |
| [ADR-009](widget/ADR-009-continuous-progress-bar.md) | Barra de progreso continua en lugar de segmentada | widget | Aceptado | 2026-03-26 |

## Regla de escritura

Cada vez que se hace un cambio significativo (nuevo feature, cambio de arquitectura, decisión de diseño UI, modificación de entidad o ruta de navegación) se debe crear un ADR **antes o inmediatamente después** de hacer el cambio. Ver las reglas de enforcement en `CLAUDE.md`.
