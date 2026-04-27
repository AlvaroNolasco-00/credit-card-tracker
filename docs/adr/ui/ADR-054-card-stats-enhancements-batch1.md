# ADR-054: Mejoras en Estadísticas de Uso — KPIs, Filtro de Rango, Distribución por Categoría, Tooltip Interactivo, Pagos vs Gastos e Insights Automáticos

**Fecha:** 2026-04-26  
**Estado:** Aceptado  
**Categoría:** ui, data  
**Prioridad:** High  
**Afecta:** `CardStatsScreen`, `CardStatsViewModel`, `CardStatsInsights`, `ActivityLog`, `AppDatabase`, `CreditCardRepository`

---

## Contexto

La pantalla de estadísticas de uso (`CardStatsScreen`) mostraba únicamente un gráfico de línea fijo a 6 meses, un resumen básico del período (total gastado, transacciones, pagos) y un calendario de calor. Los usuarios no podían:

- Cambiar el rango de tiempo visualizado.
- Ver comparaciones entre períodos (tendencia de gasto).
- Conocer la distribución de gastos por categoría.
- Ver el monto exacto al tocar un punto del gráfico.
- Entender la intensidad del calendario de calor sin contexto.

Investigamos la posibilidad de usar librerías de gráficos externas (Chart.js, MPAndroidChart), pero decidimos mantener el `Canvas` custom de Compose para control total del diseño y evitar dependencias adicionales.

---

## Decisión

### Opción elegida
Implementar una mejora progresiva en tres batches, empezando por fundamentos de datos y KPIs visuales.

**Batch 1 (fundamentos):**
1. **Agregar campo `amount` a `ActivityLog`** para rastrear montos de pagos de forma estructurada (antes solo estaban en la descripción como texto libre).
2. **Ampliar `PeriodStats`** con:
   - `totalPaymentsAmount`: suma de montos de pagos del período.
   - `categoryBreakdown`: top 5 categorías con monto, % y conteo.
   - `avgTransactionAmount`: promedio por transacción.
   - `creditUtilizationPercent`: % del límite de crédito usado.
3. **Agregar `PeriodsSummary`** al estado global: promedio mensual, mes con más gasto, total de transacciones y pagos, utilización promedio.
4. **Filtro de rango de tiempo**: chips `1M | 3M | 6M | 1A` que recalculan los períodos vía `DateUtils.getPeriodsRange()`.
5. **KPIs visuales**: 2-4 tarjetas de resumen arriba del gráfico (promedio, peak, tendencia %, utilización).
6. **Tooltip flotante** sobre el punto seleccionado del gráfico mostrando el monto exacto.
7. **Distribución por categoría**: sección con barras de progreso horizontales y porcentajes.
8. **Mejoras al calendario de calor**: leyenda visual, escala de intensidad basada en el máximo del período (no en el promedio), y estado vacío para días sin gastos.
9. **Fallback elegante para categorías sin color**: generar color consistente a partir del hash del nombre de la categoría, con fondo tenue e inicial en lugar de ícono SVG.

**Batch 2 (comparación y salud):**
10. **Gráfico "Pagos vs Gastos"**: barras verticales duales por período, gastos en verde y pagos en azul, con línea de selección punteada.
11. **Badge de salud del período**: "Pagado" (verde) / "Parcial" (naranja) / "Pendiente" (rojo) integrado en `PeriodDetailSection`.

**Batch 3 (insights automáticos):**
12. **Motor de insights simple**: 7 reglas de generación basadas en datos del período seleccionado:
    - Tendencia vs mes anterior (subida/bajada %).
    - Categoría dominante (>30% del total).
    - Alerta de utilización de crédito (>80% negativo, <20% positivo).
    - Estado de pagos (100% cubierto, parcial, pendiente).
    - Promedio por transacción.
    - Patrón: mes con más gasto en el rango visible.
    - Actividad baja: sin gastos registrados.
13. **Carrusel de insights**: tarjeta horizontal con auto-scroll cada 5 segundos, transiciones suaves (fade + slide), icono colorido según tipo de insight, e indicadores de página (dots).

### Por qué esta opción
- **Datos primero**: Sin `amount` en `ActivityLog`, no podíamos calcular métricas de pago confiables (parsear descripción con regex es frágil).
- **Canvas nativo**: Mantiene consistencia visual con el resto de la app, zero overhead de dependencia, y full control sobre animaciones (halo pulsante en punto seleccionado).
- **Batch incremental**: Permite probar y validar cada mejora antes de agregar complejidad (insights automáticos, gráfico de pagos vs gastos).
- **Hash-based colors**: Resuelve el problema de categorías sin color asignado sin requerir migración de datos ni modificar la entidad `Category`.

### Opciones rechazadas
**Opción A: Librería externa de gráficos (MPAndroidChart / Compose Charts)**
- ❌ Añade dependencia de ~500KB.
- ❌ Menos flexible para personalizar con el design system existente (colores `ForestGreen`, `MintGreen`).
- ❌ Mayor curva de aprendizaje para futuros maintainers.

**Opción B: Parsear monto de pago desde `ActivityLog.description` con regex**
- ❌ Frágil ante cambios de formato de descripción.
- ❌ No permite queries SQL eficientes (`SUM(amount) WHERE action='PAYMENT'`).
- ❌ Rompe si el usuario cambia el idioma o el formato de moneda.

**Opción C: Agregar campo `color` a `Category` con migración**
- ❌ Requeriría actualizar todas las categorías existentes en la base de datos.
- ❌ Aumenta el scope del batch; se puede resolver en UI sin tocar datos.

---

## Consecuencias

### Directas
- ✅ Los usuarios pueden filtrar estadísticas a 1, 3, 6 o 12 meses.
- ✅ Tendencia de gasto visible al instante (subida/bajada % vs mes anterior).
- ✅ Distribución por categoría responde "¿En qué gasto más?".
- ✅ Tooltip elimina la necesidad de ir al detalle del período para ver montos exactos.
- ✅ Leyenda del calendario mejora comprensión de la intensidad de color.
- ✅ Gráfico "Pagos vs Gastos" permite comparar cobertura de pagos visualmente.
- ✅ Badge de salud da feedback inmediato sobre el estado de pagos del período.
- ✅ Insights automáticos proporcionan análisis contextual sin esfuerzo del usuario.
- ⚠️ `ActivityLog` ahora tiene un campo nullable; logs antiguos tendrán `amount = null`.

### Técnicas
**Archivos/módulos impactados:**
- `app/src/main/java/.../data/entity/ActivityLog.kt` — Campo `amount: Double?` agregado.
- `app/src/main/java/.../data/AppDatabase.kt` — Versión 15, migración `14→15` (`ALTER TABLE activity_logs ADD COLUMN amount REAL`).
- `app/src/main/java/.../data/repository/CreditCardRepository.kt` — `logPayment()` ahora pasa `amount`.
- `app/src/main/java/.../ui/stats/CardStatsViewModel.kt` — `PeriodStats` expandido, `PeriodsSummary`, `selectMonthCount()`, regeneración de insights al cambiar período.
- `app/src/main/java/.../ui/stats/CardStatsScreen.kt` — KPIs, chips de filtro, tooltip, categorías, calendario mejorado, gráfico pagos vs gastos, badge de salud, carrusel de insights.
- `app/src/main/java/.../ui/stats/CardStatsInsights.kt` — Nuevo: motor de generación de insights con 7 reglas.

**Breaking changes:**
- Migración de base de datos v14→v15 requerida. No destructiva (solo agrega columna nullable).

### Operacionales
- Testing requerido: manual (verificar KPIs con diferentes rangos), unit tests para `CardStatsViewModel` en Batch 2.
- Documentación: Este ADR + CHANGELOG.md actualizado.
- Comunicación: N/A (feature no requiere cambios en onboarding).

---

## Implementación

### Paso a paso (Batch 1)
1. Modificar `ActivityLog` entity: agregar `amount: Double? = null`.
2. Crear `MIGRATION_14_15` en `AppDatabase`.
3. Actualizar `CreditCardRepository.logPayment()` para incluir `amount`.
4. Ampliar `PeriodStats` y crear `CategorySpend`, `PeriodsSummary` en `CardStatsViewModel`.
5. Implementar `selectMonthCount()` y recalcular stats al cambiar rango.
6. Crear composables: `RangeFilterChips`, `KpiSummaryRow`, `KpiCard`, `CategoryBreakdownSection`, `CalendarLegend`, `EmptyDayState`.
7. Modificar `StatsChartSection` para incluir tooltip posicionado sobre `LineChart`.
8. Mejorar `LineChart`: halo pulsante en punto seleccionado, líneas guía horizontales.
9. Mejorar `UsageCalendar`: escala de intensidad por máximo del período, leyenda.
10. Validar build (`./gradlew test assembleDebug`).

### Paso a paso (Batch 2)
11. Crear `PaymentsVsExpensesSection` y `PaymentsVsExpensesChart` con Canvas de barras duales.
12. Crear `PaymentHealthBadge` con estados "Pagado/Parcial/Pendiente".
13. Integrar ambos composables en `CardStatsScreen`.
14. Validar build (`./gradlew test assembleDebug`).

### Paso a paso (Batch 3)
15. Crear `CardStatsInsights.kt` con motor de generación de insights (7 reglas).
16. Agregar campo `insights: List<Insight>` a `CardStatsUiState`.
17. Calcular insights en `loadStats()` y regenerar en `selectPeriod()`.
18. Crear `InsightsCarousel` con auto-scroll, transiciones y dots.
19. Integrar carrusel entre KPIs y gráfico en `CardStatsScreen`.
20. Validar build (`./gradlew test assembleDebug`).

### Files de referencia
- PR: TBD
- Commit: Batch 1 — Estadísticas de uso: KPIs, filtro de rango, categorías y tooltip.
- Tests: `./gradlew test` — todos pasan (sin tests nuevos para stats en este batch).

---

## Validación

### Cómo verificar que la decisión se implementó correctamente
- [x] Al abrir estadísticas de una tarjeta, se ven 4 KPIs (promedio, peak, tendencia, utilización).
- [x] Tocar `1M`, `3M`, `6M`, `1A` recalcula el gráfico, KPIs e insights.
- [x] Tocar un punto del gráfico muestra tooltip con monto exacto.
- [x] Sección "Gastos por Categoría" muestra top 5 con barras de progreso.
- [x] Calendario tiene leyenda y colores proporcionales al máximo del período.
- [x] Día sin gastos muestra estado vacío al seleccionar.
- [x] Gráfico "Pagos vs Gastos" muestra barras duales por período.
- [x] Badge de salud muestra "Pagado/Parcial/Pendiente" según corresponda.
- [x] Carrusel de insights muestra tips contextuales con auto-scroll y transiciones.
- [x] Al cambiar de período, los insights se actualizan automáticamente.
- [x] Build pasa: `./gradlew test assembleDebug`.

### Métricas de éxito
- Tiempo para entender patrones de gasto < 5 segundos (antes requería navegar entre períodos manualmente).
- 0 crashes en stats screen tras el release.

---

## Notas y Aprendizajes

- `FlowRow` es experimental en la versión de Compose del proyecto; se reemplazó por un layout manual de `Column { Row { ... } }` para evitar warnings de compilación.
- `MaterialTheme.colorScheme` no es accesible dentro de `DrawScope` (Canvas); los colores deben capturarse fuera del `Canvas` como variables.
- El hash del nombre de categoría genera colores distinguibles y consistentes sin requerir persistencia.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-26 | Documento inicial — Batch 1 |

---

## Referencias

- [ADR-044](ui/ADR-044-card-usage-stats-feature.md) — Decisión precursora: creación inicial de la pantalla de estadísticas.
- [Jetpack Compose Canvas](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/drawscope/DrawScope) — Referencia de drawText y animaciones.
