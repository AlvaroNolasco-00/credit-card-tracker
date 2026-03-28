# ADR-018: Color dinámico de texto en tarjetas según fondo

**Fecha:** 2026-03-27
**Estado:** Aceptado
**Categoría:** ui

## Contexto

Los textos de las tarjetas de crédito usaban siempre `Color.White`, pero las tarjetas Verde y Amarillo tienen gradientes muy claros (ej: `#FFFF42`, `#42FF45`) donde el texto blanco es casi invisible. Se necesitaba mejor contraste.

## Decisión

Se crea una función `CardGradients.getTextColorForCard(colorInt: Int): Color` que retorna:
- **`TextDark`** (color oscuro) para tarjetas Verde y Amarillo (fondos muy claros)
- **`Color.White`** para el resto (fondos oscuros)

La función se aplica en `CreditCardPagerItem` para todos los textos de la tarjeta:
- Etiquetas ("Saldo", "Limite")
- Montos (saldo, límite, extra financing)
- Nombre de tarjeta
- Últimos 4 dígitos

El color del ícono de información "!" permanece dinámico (color de la tarjeta) en fondo blanco.

## Alternativas consideradas

**Hardcodear colores específicos por tarjeta:** Menos mantenible; la nueva función es extensible si se agregan más colores.

**Calcular contraste en tiempo de ejecución:** Innecesario para este caso específico.

## Consecuencias

- Mejor contraste y legibilidad en tarjetas Verde y Amarillo.
- Los textos se adaptan automáticamente a cualquier nuevo color de tarjeta que se agregue.
- La barra de progreso mantiene color blanco en todos los casos (suficientemente visible sobre cualquier gradiente).
