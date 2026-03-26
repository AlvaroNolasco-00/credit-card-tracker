# ADR-008: WidgetCardData centraliza totalDue y progress como propiedades computadas

**Fecha:** 2026-03-26
**Estado:** Aceptado
**Categoría:** widget

## Contexto

La barra de progreso del widget (`SegmentedProgressBar`) requiere dos valores derivados:

1. `totalDue` = gastos del período + extrafinanciamiento de la tarjeta
2. `progress` = `totalDue / creditLimit`, acotado a [0, 1]

Antes de este ADR, cada componente (`MiniCardRow`, `FullCardItem`) calculaba estos valores de forma independiente con variables locales. Esto generó dos problemas:

- El extrafinanciamiento (`extraFinancingPayment`) fue omitido en el cálculo inicial del widget (bug), mientras que el dashboard lo incluía correctamente desde el principio.
- La lógica duplicada hacía que cualquier cambio futuro en la fórmula requiriera actualizaciones en múltiples sitios.

## Decisión

Mover `totalDue` y `progress` como propiedades computadas (`val`) directamente en el `data class WidgetCardData`. Los componentes solo consumen `data.progress` sin conocer la fórmula.

```kotlin
data class WidgetCardData(
    val card: CreditCard,
    val totalSpent: Double,
    val dateInfo: WidgetDateInfo
) {
    val totalDue: Double = totalSpent + card.extraFinancingPayment
    val progress: Float = if (card.creditLimit > 0)
        (totalDue / card.creditLimit).toFloat().coerceIn(0f, 1f)
    else 0f
}
```

## Consecuencias

- La fórmula de uso de tarjeta vive en un solo lugar: cambiar `totalDue` o `progress` se propaga automáticamente a todos los layouts del widget.
- `MiniCardRow` y `FullCardItem` eliminan sus variables locales y pasan `data.progress` directamente a `SegmentedProgressBar`.
- El comportamiento es ahora consistente con el dashboard, que usa `totalDue = totalSpent + extraFinancingPayment` (ver `DashboardScreen.kt:296`).
- Si en el futuro se añaden más campos que afecten el uso (e.g. reservas pendientes), el cambio es en `WidgetCardData` únicamente.
