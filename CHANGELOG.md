# Changelog — Credit Card Tracker

Bitácora central de cambios, features, bug fixes y decisiones del proyecto.  
Basado en [Keep a Changelog](https://keepachangelog.com/) + [Semantic Versioning](https://semver.org/).

**Nota:** Cada cambio significativo tiene un ADR asociado en `docs/adr/`. Ver [INDEX.md](docs/adr/INDEX.md).

---

## [Unreleased]

### Fixed
- 🐛 **OCR Dark-Mode Preprocessing — fórmula de brightness corregida (ADR-056)**
  - Root cause: `brightness=80` en dark mode colapsaba toda la imagen a negro (`out = -1.8*200 + 80 = -280 → 0`). ML Kit recibía bitmap vacío, retornaba basura como "6".
  - Fix 1: `brightness = 255 * contrast + lightBrightness` (≈399 para imágenes grandes, ≈317 para pequeñas). Fondo oscuro (v=40) → blanco ✓, texto claro (v=200) → oscuro ✓.
  - Fix 2: `calculateAverageBrightness()` usa mediana en lugar de media. Headers blancos (logo Credicomer) ya no sesgan la detección de dark mode.
  - Fix 3: retry sin filtro de color si primer pase retorna `NONE`/`LOW`. `raw.recycle()` diferido hasta después del retry.
  - Fix 4: `OcrPreprocessingMathTest.kt` — 8 tests JVM puros que validan la fórmula del ColorMatrix sin SDK Android.
  - Archivos: `OcrProcessor.kt`, `OcrPreprocessingMathTest.kt` (nuevo)
- ✅ OCR Amount Detector — fixes de regex, scoring y filtros para pasar 24/24 tests (ADR-052)
  - `amountRegex`: ahora captura números de 4+ dígitos sin separador de miles (`12500.00`)
  - `correctOcrErrors`: removida corrección `L→1` que corrompía moneda Lempira (`L300.50`)
  - `SCORE_KEYWORD_MATCH`: subido de 40 a 50 para alcanzar `Confidence.HIGH` con bonus bottom30
  - `SCORE_LAST_AMOUNT`: subido de 5 a 20 para alcanzar `Confidence.MEDIUM` en fallback
  - Eliminadas penalizaciones `-5`/`-10` en candidatos cercanos a keywords
  - `idKeywords`: agregados `tel`, `telefono`, `teléfono`, `cel`, `celular`, `fax` para filtrar números telefónicos
- 🐛 OCR Amount Detection — fixes de filtrado de fechas, tarjetas, normalización OCR de keywords, boost de proximidad y preprocessing de imágenes pequeñas (ADR-055)
  - `looksLikeNonMonetary`: `datePatterns` ahora verifica `contextStr` completo en lugar de solo `matchStr`
  - `cardNumberPatterns`: nuevo filtro para números de tarjeta enmascarados (`**** 4399`) y completos
  - `normalizeForKeywords`: corrección OCR inversa (0→O, 1→l, 5→S) aplicada antes de keyword matching
  - `findAmountsWithCurrencyNearKeywordsScored`: nueva capa de detección con score 60 para montos con símbolo de moneda cerca de keywords
  - `preprocessBitmapForOcr`: agregado `minDim=512` para upscaling de crops pequeños; ML Kit necesita texto de ~18-20px mínimo
  - `preprocessBitmapForOcr`: contraste/brightness más conservadores para imágenes <1000px (evita destruir bordes anti-aliased)
  - Logging agregado para debuggear dimensiones de entrada/salida y texto de ML Kit
- 🐛 **Pago vencido no permitía registrar pago cuando hoy < día de corte del mes actual (ADR-053)**
  - Root cause: `DashboardViewModel.loadDashboard()` solo calculaba `isPaid`/`cutPeriodTotal` cuando `hasCutOffPassedThisMonth() == true`, ocultando el botón Pagar entre el vencimiento y el día de corte.
  - Fix: Siempre calcular período anterior, `isPaid`, `prevFlow` y `overduedays` para todas las tarjetas. `cutOffHappenedThisMonth` se mantiene solo para el split visual (ADR-021).
  - `DateUtils.getPreviousPeriodRange()`: corregido cálculo del fin del período anterior usando `getCurrentPeriodRange()` como ancla (evita retornar el período actual como "anterior" cuando `today < cutOffDay`).
  - `DateUtils.getDaysOverduePayment()`: eliminada guarda `if (!hasCutOffPassedThisMonth) return 0` que enmascaraba el vencimiento real.
  - `DashboardScreen`: condición de visibilidad de `PayBalanceCard` ahora usa `>= 0.0` en lugar de `> 0.0`, permitiendo registrar pago cuando el saldo del corte es $0 pero el pago está vencido (ej: gastos nuevos post-corte, ciclo anterior sin gastos).
  - `PayBalanceCard`: diálogo permite confirmar pago de $0.00 cuando `remaining == 0`, para limpiar el estado de vencido sin saldo pendiente.

### Added
- ✅ Mejoras en Estadísticas de Uso — Batch 1, 2 & 3: KPIs, filtro de rango, distribución por categoría, tooltip interactivo, pagos vs gastos, badge de salud e insights automáticos (ADR-054)
  - `ActivityLog`: nuevo campo `amount: Double?` para rastrear montos de pagos de forma estructurada
  - Migración DB v14→v15: `ALTER TABLE activity_logs ADD COLUMN amount REAL`
  - `CreditCardRepository.logPayment()`: ahora persiste el monto en el log
  - `PeriodStats` expandido: `totalPaymentsAmount`, `categoryBreakdown`, `avgTransactionAmount`, `creditUtilizationPercent`
  - `PeriodsSummary`: promedio mensual, mes con más gasto, total transacciones/pagos, utilización promedio
  - Filtro de rango de tiempo: chips `1M | 3M | 6M | 1A` recalculan períodos dinámicamente
  - KPIs visuales: promedio mensual, mes peak, tendencia % vs anterior, utilización de crédito con semáforo de color
  - Tooltip flotante sobre puntos del gráfico mostrando monto exacto
  - Sección "Gastos por Categoría": top 5 con barras de progreso, porcentajes y montos
  - Calendario de calor mejorado: leyenda visual, escala por máximo del período, estado vacío para días sin gastos
  - Fallback de color para categorías: generación consistente vía hash del nombre
  - Gráfico "Pagos vs Gastos": barras duales por período comparando gastos (verde) vs pagos (azul)
  - Badge de salud del período: "Pagado" / "Parcial" / "Pendiente" con semáforo de color
  - Motor de insights automáticos: 7 reglas de generación (tendencia, categoría dominante, utilización, pagos, promedio, patrón, actividad baja)
  - Carrusel de insights con auto-scroll cada 5 segundos, transiciones suaves e indicadores de página
- ✅ Gastos recurrentes (ADR-051)
  - Entity `RecurringExpense` con FK a `CreditCard`, campos `amount`, `description`, `dayOfMonth` (nullable), `isActive`
  - Junction table `RecurringExpenseCategory` para categorías (mismo patrón que `ExpenseCategory`)
  - DAO `RecurringExpenseDao` con queries Flow: `getActiveByCard`, `getAllActive`
  - Migración DB v12→v13: crea tablas `recurring_expenses` y `recurring_expense_categories` con índices
  - Repository: `insertRecurringExpense`, `updateRecurringExpense`, `deleteRecurringExpense`, `getAllRecurringExpenses()`
  - `RecurringExpensesScreen`: lista de gastos recurrentes por tarjeta con overview card y FAB
  - `AddEditRecurringExpenseScreen`: formulario con toggle "¿Conoces el día de cobro?" + chips de categorías
  - Dashboard: ícono `Repeat` en `CreditCardPagerItem` para acceso directo a gastos recurrentes
  - `DashboardViewModel`: `recurringExpensesTotal` en `CardDashboardState`; se suma al `totalSpent` del período
  - `DateUtils.isRecurringExpenseApplicable()`: si `dayOfMonth=null` aplica siempre al período; si tiene día, verifica si cae dentro del rango de fechas
  - Navegación: 3 rutas nuevas (`recurring_expenses/{cardId}`, `add_recurring_expense/{cardId}`, `edit_recurring_expense/{recurringId}`)
- ✅ Gastos no-tarjeta: débito, transferencia, efectivo, otros (ADR-047)
  - `PaymentMethod` enum (`CREDIT_CARD`, `DEBIT_CARD`, `TRANSFER`, `CASH`, `OTHER`)
  - `Expense.cardId` ahora nullable; `paymentMethod: String` nuevo campo
  - Migración DB 10→11: recreación de tabla `expenses` con FK `ON DELETE SET NULL`
  - `AddExpenseScreen`: selector de método de pago al inicio; si no es crédito, oculta CardTargetBanner y MSI
  - Dashboard: botón "Personal" en `BottomActionBar`; ruta `add_personal_expense`
  - `SalaryUsageCard` muestra "Tarjetas + Personal", línea "Disponible" en verde/rojo
  - `DashboardViewModel.loadNonCardSpending()` carga total mensual de gastos personales
- ✅ Notificaciones ricas de corte/pago con miniatura de tarjeta personalizada (ADR-046)
  - Vista colapsada: ícono propio `ic_notification_card`, large icon con la mini tarjeta en el color de la tarjeta, acento de color en el encabezado del sistema
  - Vista expandida: layout `notification_expanded.xml` con thumbnail de tarjeta (gradiente + chip dorado + banco + ••••XXXX) + badge CORTE/PAGO, nombre, días restantes y fecha formateada
  - `CardBitmapHelper` genera el bitmap con Canvas (gradiente tricolor, shimmer, chip EMV, texto monospace para dígitos)
  - `ReminderScheduler` calcula y pasa la fecha del evento formateada en español ("viernes, 18 de abril")
  - `CardNotificationData` centraliza todos los datos de presentación; elimina los `title`/`message` genéricos anteriores
- ✅ Onboarding de primer lanzamiento con HorizontalPager de 7 páginas (ADR-041)
  - 6 páginas de features: tarjetas, gastos, OCR, recordatorios, presupuesto, ingresos/widget
  - Última página integra el input de nombre del usuario
  - Botón "Omitir" (páginas 1–6) y "Comenzar" (última página)
  - Solo se muestra una vez; preferencia `onboarding_completed` controla el flujo
  - `startDestination` determinado en `MainActivity` antes de la primera composición
- ✅ Opción "Ver tour de la app" en SupportScreen para revisar onboarding en cualquier momento
  - Acceso sin interrumpir el flujo: regresa a SupportScreen al terminar en lugar de ir al dashboard
- ✅ Mejora UX en pantalla de gasto: overlay oscuro + spinner + texto "Analizando recibo..." durante procesamiento OCR
  - Botones "Tomar Foto" y "Galería" deshabilitados mientras OCR está en curso
  - Botón "Guardar Gasto" deshabilitado durante OCR (previene guardado prematuro)
  - Previene flujos paralelos y race conditions
- ✅ Exclusiones mejoradas en `looksLikeNonMonetary()`: porcentajes (IVA, descuentos), códigos postales (C.P., ZIP) y cantidades de ítems ("2 x $25")
- ✅ **Fecha retroactiva en diálogo de pago (ADR-053)**
  - `PayBalanceCard`: agregado `DatePickerDialog` de Material3 para elegir la fecha real del pago.
  - Restricción de fechas: solo permite hoy y fechas pasadas (`SelectableDates`).
  - `DashboardViewModel.payPartial()` y `payBalance()` aceptan `paymentDate: Long` (default `System.currentTimeMillis()`).
  - `lastPaymentDate` ahora refleja la fecha elegida por el usuario, no siempre el momento actual.
  - Tests unitarios: `DateUtilsTest.kt` con 44 casos de borde para `getCurrentPeriodRange`, `getPreviousPeriodRange`, `getDaysOverduePayment`, `getPaymentDueDateForCurrentCycle`.
- ✅ Sistema de pesos (scoring) unificado para detección OCR: acumula candidatos de 6 capas y elige ganador por puntuación
  - Base scores por capa (Geometric 50, Column 35, Keyword 40, Position 25, LastSection 15, Fallback 5)
  - Bonificaciones: símbolo de moneda (+30), monto máximo en bottom 30% (+20), keyword en bloque (+15)
  - Mapeo score → Confidence (70+→HIGH, 40+→MEDIUM, 20+→LOW)
- ✅ Preprocesamiento nativo de imagen en OCR: escala de grises + amplificación de contraste para mejorar precisión en recibos de bajo contraste (+15-20% accuracy)
- ✅ Corrección de caracteres OCR (post-procesamiento): O→0, l/L/I→1, S→5, B→8, Z/2 para mitigar errores de ML Kit
- ✅ Detección de columna de precios (Layer 2.5): detecta montos alineados verticalmente en tickets sin keywords explícitos
- ✅ Filtro de confidence ML Kit: ignora líneas/bloques con confianza < 0.5 para reducir ruido en áreas borrosas
- ✅ Catálogo de Bancos SV y Selector Dropdown (ADR-048)
  - `SupportedBank` enum con 6 bancos: Agrícola, Cuscatlán, BAC Credomatic, Promerica, Davivienda, Credicomer
  - Cada banco tiene `id`, `displayName` y `promotionsUrl` (para scraper futuro)
- ✅ Alertas visuales y notificaciones de pago vencido (ADR-049)
  - Detección de pago vencido: `DateUtils.getDaysOverduePayment()` calcula días desde vencimiento del pago del ciclo actual
  - UI en Dashboard: banner rojo compacto, `InfoChip` con ícono Error rojo, `PayBalanceCard` con borde/ícono rojo y título de urgencia
  - Notificaciones push escalonadas: 3 alarmas (día+1, +4, +7 tras payment due) con badge "VENCIDO" rojo
  - `ReminderReceiver` con `@AndroidEntryPoint` verifica DB antes de notificar (si usuario pagó manualmente, no notifica)
  - Cancelación automática de alarmas al registrar pago completo
  - Campo `bankId: String?` en `CreditCard` entity — `null` para bancos personalizados
  - `BankPicker` composable: dropdown con 6 bancos + opción "Otro (personalizado)" con texto libre
  - Migración DB v11→v12: `ALTER TABLE credit_cards ADD COLUMN bankId TEXT`
  - Auto-detect en edit: carga de tarjeta legacy intenta match con `SupportedBank.fromDisplayName()`
  - Retrocompatibilidad total: tarjetas legacy con `bankId = null` usan `bank` string original

### Changed
- 🔧 **OCR Detection Pipeline:** Reemplazo de "first-wins" con scoring unificado — todas las capas acumulan candidatos
  - Métodos detectores ahora retornan `List<ScoredCandidate>` en lugar de `Double?`
  - Currency symbol en regex (grupo 1) ahora se usa para priorizar (+30 puntos automáticos)
  - Métodos renombrados: `findByKeywords()` → `findByKeywordsScored()`, etc.
- 🔧 **OCR Parsing:** Reemplazo de normalización manual con `java.text.NumberFormat` de multi-locale — soporta dinámicamente 1,250.50 (US/MX) y 1.250,50 (EU) sin hardcoding
- 📝 Optimización de `parseAmount()`: 25 líneas → 11, uso de `ParsePosition` para garantizar parseo completo

### Fixed
- 🐛 **Payment state reset on card edit (ADR-050):** Guardar edición en cualquier tarjeta no restablecía pagos de otras tarjetas
  - Root cause: `CardsViewModel.saveCard()` creaba `CreditCard` nueva sin preservar `lastPaymentDate`, `partialPaymentAmount`, `partialPaymentCycleEnd`
  - Solución: Cargar tarjeta existente del repo + usar `.copy()` para sobrescribir solo campos del formulario
  - Efecto: Tarjetas pagadas permanecen pagadas tras editar cualquier otra tarjeta; pagos parciales se preservan
  - **File:** `app/src/main/java/com/alvaronolasco/creditcardtracker/ui/cards/CardsViewModel.kt` (lines 59–85)
- 🐛 Ruido de OCR: Porcentajes (ej. "16%") y códigos postales ya no se capturan como montos válidos
- 🐛 Robustez: Eliminados edge cases en parsing de montos con separadores inconsistentes
- 🐛 Detección múltiple: Si hay 2+ montos bajo "TOTAL", ahora se elige el ganador por scoring (no el primero)
- 🐛 **ImageCropCanvas — Refactorización de gestos (ADR-045):** Eliminación del comportamiento errático al cambiar entre modos. Arquitectura anterior tenía dos `pointerInput` compitiendo en el mismo `Box` + hack `PointerEventPass.Initial` frágil. Solución final: un único `Canvas` con un único `pointerInput(isDrawMode)` que usa `if/else` — cuando `isDrawMode` cambia, Compose cancela y reinicia la coroutine desde cero, garantizando que solo un detector de gestos corre a la vez sin estado residual. Dibujo usa `awaitFirstDown()` + `drag()`; zoom/pan usa `detectTransformGestures`. Imagen y rectángulo de selección se dibujan en el mismo Canvas (mismas coordenadas, sin desfase para el crop).
  **File:** `app/src/main/java/com/alvaronolasco/creditcardtracker/ui/expenses/AddExpenseScreen.kt` (líneas 994–1072)

**ADRs:**
- [ADR-053](docs/adr/ui/ADR-053-overdue-payment-fix-and-retroactive-date.md) — Bugfix pago vencido + fecha retroactiva
- [ADR-051](docs/adr/ui/ADR-051-recurring-expenses.md) — Gastos recurrentes
- [ADR-050](docs/adr/architecture/ADR-050-preserve-payment-state-on-card-update.md) — Preservar estado de pago al actualizar tarjeta
- [ADR-049](docs/adr/ui/ADR-049-overdue-payment-alerts.md) — Alertas visuales de pago vencido
- [ADR-048](docs/adr/data/ADR-048-bank-catalog-and-selector.md) — Catálogo de Bancos SV
- [ADR-047](docs/adr/data/ADR-047-non-card-expenses.md) — Gastos no-tarjeta
- [ADR-046](docs/adr/ui/ADR-046-rich-notifications-card-thumbnail.md) — Notificaciones ricas con miniatura de tarjeta
- [ADR-045](docs/adr/ui/ADR-045-image-crop-canvas-layered-architecture.md) — Refactorización ImageCropCanvas con arquitectura de capas
- [ADR-041](docs/adr/ui/ADR-041-ocr-loading-state-ux.md) — OCR Loading State UX Improvement
- [ADR-038](docs/adr/architecture/ADR-038-ocr-accuracy-improvements.md) — OCR Accuracy Improvements
- [ADR-036](docs/adr/architecture/ADR-036-ocr-amount-scoring-system.md) — Unified scoring system
- [ADR-034](docs/adr/architecture/ADR-034-ocr-parsing-robustness.md) — Robustez de parsing
- [ADR-033](docs/adr/architecture/ADR-033-geometric-ocr-alignment.md) — Alineación geométrica
- [ADR-037](docs/adr/architecture/ADR-037-ocr-image-preprocessing.md) — Preprocesamiento de imagen

**Archivos:** 
- `app/src/main/java/com/alvaronolasco/creditcardtracker/ui/expenses/AddExpenseScreen.kt` — Mejora de UX del estado de carga OCR
- `app/src/main/java/com/alvaronolasco/creditcardtracker/ocr/OcrProcessor.kt`

### Deprecated
- 

### Removed
- 

### Security
- 🔒 Implementar `Closeable` en `OcrProcessor` para prevenir memory leaks del ML Kit TextRecognizer. Llamada explícita a `.close()` en `ViewModel.onCleared()`. **[ADR-035](docs/adr/architecture/ADR-035-ocr-processor-lifecycle-management.md)**

---

## [2.1.0] — 2026-04-06

**Tema:** Reorganización del Dashboard y mejoras de UI

### Added
- ✅ Feature "Apoya al Desarrollador" con acceso vía botón en Dashboard
- ✅ Pager de tarjetas en Dashboard (deslizamiento horizontal)
- ✅ Soporte para botón "Agregar Tarjeta" en el pager

**ADRs:** [ADR-030](docs/adr/ui/ADR-030-support-developer-feature.md), [ADR-032](docs/adr/ui/ADR-032-dashboard-reorganization.md)

### Changed
- 📝 Reorganización visual del Dashboard: Card pager desplazó secciones
- 📝 Acceso al usuario/config mediante saludo interactivo en header

**Archivo:** `app/src/main/java/com/alvaronolasco/creditcardtracker/ui/dashboard/DashboardScreen.kt`

### Fixed
- 🐛 Mejora de OCR: keywords expandidos para detección de montos
- 🐛 Refinamiento de heurística para total en recibos

**ADR:** [ADR mejorado de OCR pendiente]  
**Tests:** `OcrAmountDetectorTest.kt`

---

## [2.0.0] — 2026-03-31

**Tema:** Presupuesto mensual, mejoras de UX y estilos

### Added
- ✅ Presupuesto mensual por categoría
- ✅ Comparación visual: gastos vs presupuesto (barras/porcentajes)
- ✅ Recordatorio de presupuesto en Dashboard (1x por mes)
- ✅ Color de tarjeta de ingresos: celeste para mejor diferenciación

**ADRs:** 
- [ADR-029](docs/adr/ui/ADR-029-monthly-budget-feature.md) — Presupuesto
- [ADR-031](docs/adr/ui/ADR-031-budget-reminder-dialog.md) — Recordatorio
- [ADR-028](docs/adr/widget/ADR-028-income-card-celeste-color.md) — Color ingresos

### Changed
- 📝 Base de datos: nueva entidad `BudgetProfile` con CRUD completo
- 📝 IncomeProfile: renamed `monthlyBudget` (genérico) → estructura relacional
- 📝 AddExpenseScreen: agregadas opciones de categoría al formulario

**DB Migration:** `MIGRATION_8_9`

### Fixed
- 🐛 DatePicker: bloqueadas fechas futuras (solo pasado)
- 🐛 Contraste de botones en diálogos mejorado

**ADR:** [ADR-027](docs/adr/ui/ADR-027-block-future-dates-in-expense-datepicker.md)

### Known Issues
- ⚠️ Widget es lento con 5+ tarjetas en algunos dispositivos (Pixel 3a)
  - Planned fix: [ADR-FUTURE-widget-performance.md]

---

## [1.3.0] — 2026-03-28

**Tema:** Nuevo sistema de balance split y pagos

### Added
- ✅ Balance dividido: saldo anterior + saldo nuevo (post-corte)
- ✅ Botón "Pagar Saldo" para liquidar saldo del corte
- ✅ Selector de tarjeta destino en AddExpenseScreen

**ADRs:**
- [ADR-021](docs/adr/ui/ADR-021-split-balance-post-cutoff.md)
- [ADR-022](docs/adr/ui/ADR-022-pay-balance-button.md)
- [ADR-023](docs/adr/ui/ADR-023-change-card-in-expense.md)

### Changed
- 📝 CreditCard entity: nuevos campos `paymentRecords`, `balanceBefore`, `balanceAfter`
- 📝 Layout horizontal: mejora visual del balance split
- 📝 Notificaciones: refactor para soportar reminders de pago

**DB Migration:** `MIGRATION_7_8`  
**Files:** 
- `app/src/main/java/com/alvaronolasco/creditcardtracker/data/entity/CreditCard.kt`
- `app/src/main/java/com/alvaronolasco/creditcardtracker/ui/cards/CardDetailScreen.kt`

### Fixed
- 🐛 Cálculo de balance para tarjetas con corte reciente

---

## [1.2.0] — 2026-03-27

**Tema:** Dark mode, OCR mejorado, navegación

### Added
- ✅ Modo oscuro con colores adaptativos
- ✅ Color dinámico de texto según fondo (WCAG AA compliance)
- ✅ Selector de fecha de transacción en AddExpenseScreen
- ✅ Sistema de notificaciones con toggles

**ADRs:**
- [ADR-017](docs/adr/ui/ADR-017-dark-mode-color-system.md) — Dark mode
- [ADR-018](docs/adr/ui/ADR-018-card-text-color-contrast.md) — Contraste texto
- [ADR-015](docs/adr/ui/ADR-015-expense-date-picker.md) — Date picker
- [ADR-019](docs/adr/ui/ADR-019-notification-toggles.md) — Notificaciones

### Changed
- 📝 Theme: refactorización completa en `ui/theme/`
- 📝 OCR: mejora de detección de montos (4 niveles de confianza)
- 📝 AddExpenseScreen: ahora es un formulario con date picker integrado

### Fixed
- 🐛 MSI: cálculo correcto de período y monto
- 🐛 Widget: propiedad `totalDue` ahora es computada correctamente

**ADRs:** [ADR-013](docs/adr/data/ADR-013-msi-installments.md), [ADR-014](docs/adr/data/ADR-014-msi-period-amount-fix.md)

**DB Migration:** `MIGRATION_6_7`

---

## [1.1.0] — 2026-03-26

**Tema:** Widget mejorado, nuevo sistema de actualización

### Added
- ✅ Widget: soporte para redimensionamiento a 4x4
- ✅ Widget: tarjeta de resumen "Ingresos vs Gastos"
- ✅ Broadcast system: actualización garantizada del widget tras cambios

**ADRs:**
- [ADR-011](docs/adr/widget/ADR-011-widget-grid-4x4-support.md)
- [ADR-012](docs/adr/widget/ADR-012-widget-income-summary-card.md)
- [ADR-020](docs/adr/architecture/ADR-020-broadcast-widget-update.md) — Broadcast (mejora de ADR-007)

### Changed
- 📝 Widget: barra de progreso ahora es continua (no segmentada)
- 📝 Architecture: refactor de `WidgetCardData` con propiedades computadas
- 📝 Broadcast: nuevo sistema de comunicación entre capas

**Files:**
- `app/src/main/java/com/alvaronolasco/creditcardtracker/widget/` — Refactorización completa
- `app/src/main/java/com/alvaronolasco/creditcardtracker/notifications/WidgetUpdateBroadcast.kt` — Nuevo

### Deprecated
- [ADR-007](docs/adr/architecture/ADR-007-widget-update-guarantee.md) — Supersedido por ADR-020

---

## [1.0.0] — 2026-03-25

**Tema:** Versión inicial con features core

### Added
- ✅ MVVM + Clean Architecture (Data, Presentation, DI)
- ✅ Room database con 7 entidades
- ✅ Dashboard con tarjetas de crédito
- ✅ Agregar/eliminar gastos con OCR
- ✅ Categorías predefinidas (Entretenimiento, Transporte, Comida, Medicina)
- ✅ Widget Glance: vista rápida de tarjetas y gastos
- ✅ Deep linking desde widget al app
- ✅ Notificaciones de recordatorio
- ✅ Sistema ADR con 6 decisiones iniciales

**ADRs (v1.0):**
- [ADR-001](docs/adr/widget/ADR-001-all-cards-lazy-column.md) — LazyColumn en widget
- [ADR-002](docs/adr/widget/ADR-002-quick-expense-button.md) — Botón + en widget
- [ADR-003](docs/adr/widget/ADR-003-full-width-grid.md) — Widget ancho completo
- [ADR-004](docs/adr/ui/ADR-004-expense-card-banner.md) — Banner en form
- [ADR-005](docs/adr/architecture/ADR-005-widget-deeplink-singleton.md) — DeepLink singleton
- [ADR-006](docs/adr/widget/ADR-006-progress-bar-spending.md) — Progress bar

### Technical Details
- **Language:** Kotlin 1.9.22
- **Compile SDK:** 34
- **Min SDK:** 26 (Android 8.0)
- **Framework:** Jetpack Compose + Material 3
- **DI:** Hilt
- **Database:** Room v8
- **Image Processing:** ML Kit Text Recognition

### Known Limitations
- ⚠️ Widget: solo 3 tarjetas visibles (mejora planificada en v1.1)
- ⚠️ OCR: confianza media en recibos mal escaneados

---

## Guía de uso de este changelog

### Para el desarrollador
1. **Cada vez que haces un commit significativo:**
   - Agregar entrada a `[Unreleased]` bajo la categoría apropiada
   - Crear/actualizar ADR asociado (ver `CLAUDE.md`)
   - Incluir referencia al ADR en el changelog

2. **Formato de entradas:**
   ```
   - [Emoji] Descripción breve (máx 1 línea)
   **File/Módulo:** `ruta/al/archivo.kt`
   **ADR:** [ADR-NNN](...)
   ```

3. **Antes de hacer release:**
   - Mover `[Unreleased]` a `[VERSION] — YYYY-MM-DD`
   - Actualizar INDEX.md en ADRs
   - Crear git tag: `git tag -a vX.Y.Z -m "Release X.Y.Z"`

### Categorías de cambios
- **Added:** Nuevas features o capacidades
- **Changed:** Cambios en features existentes o refactorización
- **Fixed:** Bug fixes
- **Deprecated:** Features que serán removidas próximamente
- **Removed:** Features removidas
- **Security:** Fixes de seguridad

### Emojis para escaneo rápido
- ✅ Adición (Added)
- 📝 Cambio (Changed)
- 🐛 Fix (Fixed)
- ⚠️ Deprecado (Deprecated)
- ❌ Removido (Removed)
- 🔒 Seguridad (Security)

---

## Versionado semántico

**MAJOR.MINOR.PATCH**

- **MAJOR:** Cambios incompatibles (breaking changes, nueva arquitectura)
- **MINOR:** Nuevas features backwards-compatible
- **PATCH:** Bug fixes

Ejemplo: `1.2.3`
- `1` = Major (cambios arquitectónicos)
- `2` = Minor (nuevas features)
- `3` = Patch (bug fixes)
