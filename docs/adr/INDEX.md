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
| [ADR-007](architecture/ADR-007-widget-update-guarantee.md) | Garantizar actualización del widget tras cambios de gastos | architecture | Supersedido por ADR-020 | 2026-03-26 |
| [ADR-008](widget/ADR-008-widget-card-data-computed-properties.md) | WidgetCardData centraliza totalDue y progress como propiedades computadas | widget | Aceptado | 2026-03-26 |
| [ADR-009](widget/ADR-009-continuous-progress-bar.md) | Barra de progreso continua en lugar de segmentada | widget | Aceptado | 2026-03-26 |
| [ADR-010](widget/ADR-010-widget-visual-refinement.md) | Refinamiento visual del layout de tarjetas en widget | widget | Aceptado | 2026-03-26 |
| [ADR-011](widget/ADR-011-widget-grid-4x4-support.md) | Soporte para redimensionamiento del widget a cuadrícula 4x4 | widget | Aceptado | 2026-03-26 |
| [ADR-012](widget/ADR-012-widget-income-summary-card.md) | Implementación de tarjeta de resumen "Ingresos vs Gastos" en el Widget | widget | Aceptado | 2026-03-26 |
| [ADR-013](data/ADR-013-msi-installments.md) | Compras a Meses Sin Intereses (MSI) — campos en entidad Expense | data | Supersedido por ADR-014 | 2026-03-26 |
| [ADR-014](data/ADR-014-msi-period-amount-fix.md) | Corrección del cálculo de monto del periodo para compras MSI | data | Aceptado | 2026-03-27 |
| [ADR-015](ui/ADR-015-expense-date-picker.md) | Selector de fecha de transacción en AddExpenseScreen | ui | Aceptado | 2026-03-27 |
| [ADR-016](data/ADR-016-msi-end-date.md) | Campo msiEndDate para expiración automática de planes MSI | data | Aceptado | 2026-03-27 |
| [ADR-017](ui/ADR-017-dark-mode-color-system.md) | Sistema de colores adaptativo para modo oscuro | ui | Aceptado | 2026-03-27 |
| [ADR-018](ui/ADR-018-card-text-color-contrast.md) | Color dinámico de texto en tarjetas según fondo | ui | Aceptado | 2026-03-27 |
| [ADR-019](ui/ADR-019-notification-toggles.md) | Notificaciones con toggles en lista vertical | ui | Aceptado | 2026-03-27 |
| [ADR-020](architecture/ADR-020-broadcast-widget-update.md) | Actualización de widget via broadcast en lugar de corrutinas | architecture | Aceptado | 2026-03-28 |
| [ADR-021](ui/ADR-021-split-balance-post-cutoff.md) | División de saldo en dos períodos cuando el corte ya ocurrió | ui | Aceptado | 2026-03-28 |
| [ADR-022](ui/ADR-022-pay-balance-button.md) | Botón "Pagar Saldo" para liquidar el saldo del corte | ui | Aceptado | 2026-03-28 |
| [ADR-023](ui/ADR-023-change-card-in-expense.md) | Selector de tarjeta destino en AddExpenseScreen | ui | Aceptado | 2026-03-29 |
| [ADR-024](ui/ADR-024-cut-period-horizontal-layout.md) | Reorganización horizontal del saldo en tarjetas con corte activo | ui | Aceptado | 2026-03-29 |
| [ADR-025](ui/ADR-025-dynamic-user-name-greeting.md) | Saludo dinámico con nombre de usuario y bottom sheet de configuración | ui | Aceptado | 2026-03-29 |
| [ADR-026](ui/ADR-026-dialog-button-contrast.md) | Contraste de botones en modales y formularios | ui | Aceptado | 2026-03-31 |
| [ADR-027](ui/ADR-027-block-future-dates-in-expense-datepicker.md) | Bloquear fechas futuras en DatePicker de gastos | ui | Aceptado | 2026-03-31 |
| [ADR-028](widget/ADR-028-income-card-celeste-color.md) | Cambiar color de tarjeta de ingresos a celeste para mejor diferenciación | widget | Aceptado | 2026-03-31 |
| [ADR-029](ui/ADR-029-monthly-budget-feature.md) | Presupuesto mensual por categoría con comparación vs gastos reales | ui | Aceptado | 2026-03-31 |
| [ADR-030](ui/ADR-030-support-developer-feature.md) | Feature "Apoya al Desarrollador" con donaciones via enlaces externos | ui | Aceptado | 2026-03-31 |
| [ADR-031](ui/ADR-031-budget-reminder-dialog.md) | Recordatorio de presupuesto mensual en Dashboard | ui | Aceptado | 2026-04-03 |
| [ADR-032](ui/ADR-032-dashboard-reorganization.md) | Reorganización de Dashboard y Acceso a Apoyo al Desarrollador | ui | Aceptado | 2026-04-03 |
| [ADR-033](architecture/ADR-033-geometric-ocr-alignment.md) | Alineación Geométrica en Detección de Montos OCR | architecture | Aceptado | 2026-04-07 |
| [ADR-034](architecture/ADR-034-ocr-parsing-robustness.md) | OCR Amount Parsing Robustness — NumberFormat Localization & Noise Filtering | architecture | Aceptado | 2026-04-07 |
| [ADR-035](architecture/ADR-035-ocr-processor-lifecycle-management.md) | OCR Processor Lifecycle Management — Prevención de Memory Leaks | architecture | Aceptado | 2026-04-07 |
| [ADR-036](architecture/ADR-036-ocr-amount-scoring-system.md) | OCR Amount Detection — Unified Scoring System | architecture | Aceptado | 2026-04-07 |
| [ADR-037](architecture/ADR-037-ocr-image-preprocessing.md) | Preprocesamiento de Imagen para OCR — Escala de Grises + Contraste Nativo | architecture | Aceptado | 2026-04-07 |

## Regla de escritura

Cada vez que se hace un cambio significativo (nuevo feature, cambio de arquitectura, decisión de diseño UI, modificación de entidad o ruta de navegación) se debe crear un ADR **antes o inmediatamente después** de hacer el cambio. Ver las reglas de enforcement en `CLAUDE.md`.
