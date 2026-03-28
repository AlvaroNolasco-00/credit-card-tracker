# ADR-016: Campo msiEndDate para expiración automática de planes MSI

**Fecha:** 2026-03-27
**Estado:** Aceptado
**Categoría:** data

## Contexto

ADR-013 y ADR-014 implementaron el soporte de compras MSI. Sin embargo, la query `getTotalSpentInPeriod` seguía contando la cuota MSI (`msiMonthlyAmount`) indefinidamente — una compra a 3 meses seguía apareciendo en el mes 4, 5, 6... sin fin.

El comportamiento correcto es: una compra a 3 meses realizada el 27 de marzo debe generar una cuota en los cortes de abril, mayo y junio, y dejar de contarse a partir de julio.

## Decisión

Se añade el campo `msiEndDate: Long = 0L` a la entidad `Expense`. Este valor se calcula en `ExpensesViewModel.saveExpense` sumando `msiMonths` meses a la fecha de compra usando `Calendar.add(Calendar.MONTH, msiMonths)`.

La query `getTotalSpentInPeriod` se actualiza para manejar ambos casos:

```sql
WHERE cardId = :cardId
AND (
    (msiMonths <= 1 AND date BETWEEN :startDate AND :endDate)
    OR
    (msiMonths > 1 AND date <= :endDate AND msiEndDate >= :startDate)
)
```

- **Gastos normales:** se incluyen si la fecha de compra está en el periodo (comportamiento original).
- **Gastos MSI:** se incluyen si la compra ocurrió antes o durante el periodo (`date <= endDate`) **y** el plan aún no ha expirado al inicio del periodo (`msiEndDate >= startDate`).

La versión de la base de datos sube de 5 a 6. Se usa `fallbackToDestructiveMigration`.

## Alternativas consideradas

**Calcular N meses de cuota en la pantalla:** Más complejo en UI, y duplicaría la lógica de vencimiento fuera del modelo de datos.

**Guardar solo `msiMonths` y calcular el fin en la query:** Requeriría aritmética de fechas en SQLite que Room no soporta de forma nativa.

## Consecuencias

- Las cuotas MSI se dejan de contar automáticamente después de completar los N meses.
- El dashboard y el widget reflejan el monto correcto en cada periodo sin intervención del usuario.
- Los gastos MSI existentes (con `msiEndDate = 0`) no se verán afectados en la nueva query porque `0 >= startDate` es falso para cualquier fecha real, lo que los excluirá — esto es el comportamiento esperado ya que son datos de desarrollo.
