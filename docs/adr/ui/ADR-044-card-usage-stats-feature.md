# ADR-044: Funcionalidad de Estadísticas de Uso de Tarjeta — Gráfico Histórico y Calendario

## Estado
Aceptado

## Contexto
El usuario requiere una forma de visualizar el comportamiento de sus gastos y pagos a lo largo del tiempo para cada tarjeta de crédito. Hasta ahora, la aplicación solo mostraba el saldo actual y las transacciones recientes, sin dar una visión macro del uso histórico.

Se necesita:
1.  Un punto de acceso claro en el Dashboard.
2.  Visualización de gastos totales vs. pagos por periodo (6 meses).
3.  Interactividad para ver detalles de periodos específicos.
4.  Un calendario que muestre la distribución de gastos diarios dentro del periodo.

## Decisión

### 1. Punto de Acceso (Dashboard)
Se decidió agregar un botón de "Insights" (`Icons.Default.Insights`) directamente en el componente `CreditCardPagerItem`. 
*   **Condición de visibilidad**: Solo se muestra si la tarjeta tiene al menos 30 días de antigüedad (`createdAt`) o si ya ha pasado al menos un periodo de corte (`DateUtils.hasCutOffPassedThisMonth`). Esto evita mostrar estadísticas vacías o irrelevantes en tarjetas nuevas.

### 2. Capa de Datos
*   **Historical Periods**: Se extendió `DateUtils` con `getPeriodsRange(cutOffDay, count)` para calcular rangos de fechas precisos basados en el día de corte, retrocediendo N meses.
*   **Nuevas Consultas**: Se añadió `getExpensesWithCategoriesByCardInPeriod` a `ExpenseDao` y `getLogsByEntityInPeriod` a `ActivityLogDao` para obtener datos segmentados.
*   **Registro de Pagos**: Se mejoró `CreditCardRepository` para registrar explícitamente una acción de `PAYMENT` en los `ActivityLog` cada vez que el usuario realiza un abono, permitiendo contar los pagos realizados en cada periodo histórico.

### 3. Interfaz de Usuario (CardStatsScreen)
*   **Gráfico Personalizado**: Se implementó un `LineChart` usando `Canvas` nativo de Jetpack Compose en lugar de librerías externas. Esto garantiza un APK ligero y control total sobre las micro-interacciones (taps para seleccionar periodos con feedback visual).
*   **Uso de Color (Dark Mode)**: Se determinó el uso de `MintGreen` para áreas de resalte y `ForestGreen` para trazos principales. Ambos colores se integran con `MaterialTheme.colorScheme` para asegurar que el contraste sea apto para WCAG tanto en Light como en Dark mode.
*   **Calendario de Periodo**: Se diseñó un grid de calendario prolijo que muestra los días que componen el periodo seleccionado. Se usa una escala de opacidad en el color de fondo de cada día para representar la intensidad del gasto relativo.

### 4. Navegación
Se registró la ruta `card_stats/{cardId}` en el `NavHost` principal para permitir la navegación profunda desde el dashboard.

## Consecuencias

### Positivas
*   **Valor para el Usuario**: Mayor control y visibilidad sobre sus hábitos financieros.
*   **Rendimiento**: El uso de `Canvas` nativo mantiene la UI fluida incluso con datos históricos.
*   **Arquitectura**: Se refuerza el uso de `ActivityLog` como fuente de verdad para eventos que no son estrictamente "gastos" (como los pagos).

### Negativas
*   **Complejidad de Periodos**: El cálculo de periodos que cruzan meses naturales (ej. 15 de un mes al 14 del siguiente) requiere manejo cuidadoso en la UI del calendario.
*   **Logging Retroactivo**: Los pagos realizados antes de esta actualización no aparecerán en las estadísticas de periodos pasados, ya que no se loggeaban con la acción `PAYMENT` específica.
