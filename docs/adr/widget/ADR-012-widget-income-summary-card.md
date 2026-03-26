# Architecture Decision Record — Widget Income vs Expenses Card

## ID
ADR-012

## Título
Implementación de tarjeta de resumen "Ingresos vs Gastos" en el Widget

## Estado
Aceptado

## Fecha
2026-03-26

## Categoría
widget

## Contexto
El usuario necesita tener una visión rápida no solo de lo que debe en cada tarjeta individual, sino de cómo sus gastos totales afectan a su presupuesto mensual reportado. Anteriormente, el widget solo mostraba una lista de tarjetas de crédito.

## Decisión
Añadir una tarjeta de resumen en la parte superior del widget que compare el ingreso mensual (proporcionado por `IncomeDao`) con el gasto total acumulado de todas las tarjetas de crédito activas.

### Detalles de Implementación:
- **Cálculo de Ingresos:** Se obtiene el total de ingresos para el mes actual (`DateUtils.getCurrentMonthYear()`) desde la base de datos de ingresos.
- **Cálculo de Gastos:** Se suma el `totalDue` (gasto del periodo + extrafinanciamientos) de todas las tarjetas de crédito mostradas en el widget.
- **Interfaz Visual (Glance):**
    - Se utiliza un color de fondo distintivo (verde brand) para diferenciarla de las tarjetas de crédito (gradientes).
    - Incluye una barra de progreso que indica el porcentaje del ingreso mensual que ya se ha gastado.
    - Se adapta a diferentes tamaños de widget (Small, Medium, Large) con layouts específicos (`IncomeSummaryCard` y `IncomeSummaryMiniCard`).

## Consecuencias
- **Positivas:**
    - Proporciona una visión holística de la salud financiera del usuario directamente desde la pantalla de inicio.
    - Fomenta un mejor control del presupuesto mensual al ver el impacto de los gastos en tiempo real.
- **Negativas:**
    - Añade una consulta adicional a la base de datos de ingresos al cargar el widget, aunque el impacto es mínimo debido al uso de Flows y Dispatchers.IO.
    - Ocupa espacio vertical en el widget, desplazando las tarjetas individuales hacia abajo.
