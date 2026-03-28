# ADR-014: Corrección del cálculo de monto del periodo para compras MSI

**Fecha:** 2026-03-27
**Estado:** Aceptado
**Categoría:** data

## Contexto

ADR-013 introdujo los campos `msiMonths` y `msiMonthlyAmount` en la entidad `Expense` para compras a meses sin intereses. Sin embargo, en las consecuencias de ADR-013 se documentó erróneamente que `getTotalSpentInPeriod` debía seguir sumando `amount` (precio total) porque era "correcto para el seguimiento del límite de crédito".

Esto resultó en que el dashboard mostrara la **compra completa** como monto a pagar del periodo en lugar de la **cuota mensual**, lo que es incorrecto: una compra de $12,000 a 12 meses debería mostrar $1,000 como cargo del periodo, no $12,000.

## Decisión

Se modifica la query `getTotalSpentInPeriod` en `ExpenseDao` para usar `msiMonthlyAmount` cuando `msiMonths > 1`, y `amount` en caso contrario:

```sql
SELECT SUM(CASE WHEN msiMonths > 1 THEN msiMonthlyAmount ELSE amount END)
FROM expenses
WHERE cardId = :cardId AND date BETWEEN :startDate AND :endDate
```

Esta query aplica tanto al dashboard como al widget (ambos usan el mismo DAO method).

## Alternativas consideradas

**Crear un segundo método DAO `getPaymentDueInPeriod`:** Mantendría `getTotalSpentInPeriod` con el comportamiento anterior para tracking de límite de crédito. Se descartó porque actualmente no hay ningún lugar en la app que requiera el monto total de compras MSI para tracking de límite; solo se necesita el monto a pagar.

## Consecuencias

- El dashboard y el widget ahora muestran la cuota mensual de compras MSI en lugar del precio total.
- Si en el futuro se requiere tracking del límite de crédito con el precio total de compras MSI, se deberá crear una query separada.
- ADR-013 queda supersedido en su sección de consecuencias respecto al comportamiento de `getTotalSpentInPeriod`.
