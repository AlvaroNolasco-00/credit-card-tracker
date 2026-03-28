# ADR-013: Compras a Meses Sin Intereses (MSI)

**Fecha:** 2026-03-26
**Estado:** Supersedido por ADR-014
**Categoría:** data

## Contexto

Los usuarios quieren registrar compras a meses sin intereses (MSI), una modalidad común en tarjetas de crédito mexicanas donde el precio total se divide en pagos iguales sin cargo adicional. Se necesita capturar el precio total y el número de meses para calcular el pago mensual.

## Decisión

Se añaden dos campos opcionales a la entidad `Expense` existente en lugar de crear una entidad separada `InstallmentPlan`:

- `msiMonths: Int = 1` — número de meses (1 = pago de contado, >1 = MSI)
- `msiMonthlyAmount: Double = 0.0` — monto mensual pre-calculado (`amount / msiMonths`)

El campo `amount` sigue almacenando el **precio total** de la compra.

La versión de la base de datos se incrementa de 4 a 5. Dado que el proyecto usa `fallbackToDestructiveMigration()`, no se requiere script de migración.

En `AddExpenseScreen` se añade una sección `MsiSection` con:
- Switch para activar/desactivar MSI
- Selector de meses con opciones predefinidas: 3, 6, 9, 12, 18, 24
- Resumen visual del pago mensual calculado en tiempo real

En `ExpenseHistoryScreen` se muestra un indicador de texto bajo la descripción del gasto cuando `msiMonths > 1`.

## Alternativas consideradas

**Entidad separada `InstallmentPlan`:** Más flexible para rastrear pagos individuales, pero innecesaria para el caso de uso actual que solo requiere registrar la compra y ver el pago mensual.

**Campo `msiMonths` en `CreditCard`:** No aplica porque MSI es por compra, no por tarjeta.

## Consecuencias

- La base de datos se recrea al actualizar (datos existentes se pierden en desarrollo).
- El cálculo de gastos totales del período (`getTotalSpentInPeriod`) sigue sumando `amount` (precio total), lo que es correcto para el seguimiento del límite de crédito.
- Los gastos MSI son visualmente distinguibles en el historial con su badge verde.
