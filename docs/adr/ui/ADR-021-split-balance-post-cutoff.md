# ADR-021: División de saldo en dos períodos cuando el corte ya ocurrió

**Fecha:** 2026-03-28
**Estado:** Aceptado
**Categoría:** ui

## Contexto

El usuario no podía distinguir si el saldo mostrado en la tarjeta correspondía al período de corte pasado (lo que debe pagar en el estado de cuenta) o a los gastos nuevos acumulándose en el período vigente. Esto generaba confusión después de que el día de corte ya había pasado en el mes.

## Decisión

Cuando el día de corte ya pasó en el mes actual (`hasCutOffPassedThisMonth`), la tarjeta en el dashboard muestra dos balances diferenciados:

- **Saldo del corte**: suma de gastos del período anterior (desde el corte del mes pasado hasta el día antes del corte de este mes) + `extraFinancingPayment`
- **Período actual**: gastos acumulados desde el corte de este mes hasta hoy

Cuando el corte aún no ha pasado, se sigue mostrando un único "Saldo" como antes.

Se añadieron dos métodos a `DateUtils`:
- `hasCutOffPassedThisMonth(cutOffDay)`: retorna `true` si hoy >= día de corte en el mes actual
- `getPreviousPeriodRange(cutOffDay)`: retorna el rango de timestamps del período anterior al vigente

`CardDashboardState` recibió dos nuevos campos: `cutPeriodTotal` y `cutOffHappenedThisMonth`.

El `totalAllCards` del dashboard y la barra de progreso de la tarjeta usan la suma de ambos balances cuando el corte ya ocurrió.

## Consecuencias

- El usuario puede distinguir claramente qué debe pagar (saldo del corte) y cuánto lleva gastado en el nuevo período.
- Se agrega una consulta adicional a Room por tarjeta cuando el corte ya pasó (flujo reactivo, impacto mínimo).
- El font size de los montos se reduce de 18sp a 15sp en el modo split para que quepan ambas filas en la tarjeta.
