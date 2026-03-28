# ADR-022: Botón "Pagar Saldo" en Dashboard

**Fecha:** 2026-03-28
**Estado:** Aceptado
**Categoría:** ui

## Contexto

Después del corte de la tarjeta, la app muestra el saldo del período anterior como "Saldo del corte". El usuario no tenía forma de registrar que ya realizó el pago a la tarjeta, por lo que el saldo seguía apareciendo aunque ya hubiera sido liquidado.

## Decisión

Se agregó un campo `lastPaymentDate: Long` a la entidad `CreditCard` (migración 6 → 7). Cuando el usuario pulsa "Pagar" en la tarjeta de acción `PayBalanceCard`:

1. Se guarda `System.currentTimeMillis()` en `lastPaymentDate`.
2. El ViewModel detecta en `loadDashboard` si `lastPaymentDate` cae dentro del rango del período anterior (`prevStart..prevEnd`). Si es así, `isPaidThisCycle = true` y `cutPeriodTotal` se reporta como `0.0`.
3. La UI oculta `PayBalanceCard` y muestra "Saldo pagado" en el chip de pago del `CardInfoRow`.

Se eligió almacenar la fecha en la entidad existente (en lugar de crear una entidad `PaymentRecord`) para mantener la simplicidad: no se requiere historial de pagos por ahora, solo el estado del ciclo actual.

Se usa un `AlertDialog` de confirmación antes de ejecutar el pago para evitar accionamientos accidentales.

## Consecuencias

- La barra de progreso y el total de tarjetas en `SalaryUsageCard` reflejan el balance ya pagado (0 del corte anterior).
- Si en el futuro se requiere historial de pagos, se deberá crear una entidad `PaymentRecord` y marcar este ADR como supersedido.
- La migración `MIGRATION_6_7` preserva los datos existentes mediante `ALTER TABLE ... ADD COLUMN ... DEFAULT 0`.
