# ADR-024: Reorganización horizontal del saldo en tarjetas con corte activo

**Fecha:** 2026-03-29
**Estado:** Aceptado
**Categoría:** ui

## Contexto

En `CreditCardPagerItem`, cuando `cutOffHappenedThisMonth` es `true`, el layout apilaba cuatro textos en la columna izquierda:
- "Saldo del corte" (label)
- Monto del corte
- "Período actual" (label)
- Monto del período actual

Esto sumado a la columna derecha (Límite) ocupaba demasiada altura vertical dentro del card con aspect ratio fijo `1.586f`, comprimiendo el espacio disponible y causando que el nombre y dígitos de la tarjeta se cortaran en la parte inferior.

## Decisión

Reorganizar la información de balance en **tres columnas horizontales de igual peso** cuando hay corte activo, en lugar de apilar verticalmente:

```
[Saldo corte]   [Período actual]   [Límite]
   $X.xx            $Y.yy          $Z.zz
```

Reducir tamaños de fuente en modo corte (14.sp para montos vs 18.sp en modo normal) para mantener proporciones visuales consistentes.

## Consecuencias

- ✅ El nombre y dígitos de la tarjeta permanecen siempre visibles
- ✅ El layout horizontal ocupa la misma altura que el modo normal (2 líneas: label + monto)
- ✅ El `Spacer(weight(1f))` mantiene su espacio distribuidor correctamente
- ✅ El aspect ratio `1.586f` de la tarjeta no se altera
- ✅ Mejor distribución visual del espacio cuando hay múltiples períodos de balance
