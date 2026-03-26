# ADR-009: Barra de progreso continua en lugar de segmentada

**Fecha:** 2026-03-26
**Estado:** Aceptado
**Categoría:** widget

## Contexto

La barra de progreso del widget mostraba 10 segmentos discretos (0%, 10%, 20%, ..., 100%), haciendo que valores intermedios como 88% se redondearan al segmento más cercano (90%), lo que no reflejaba con precisión el uso real de la tarjeta.

La pantalla principal (dashboard) usa una `LinearProgressIndicator` continua que llena el espacio proporcionalmente, mostrando valores exactos. El widget debería mantener coherencia visual.

## Decisión

Reemplazar `SegmentedProgressBar` (10 boxes discretos) por una barra continua usando 2 Boxes en un Row:
- Box 1: ocupa `progress` del ancho (color: rojo/amarillo/blanco según umbral)
- Box 2: ocupa `(1 - progress)` del ancho (color: gris claro, estado vacío)

```kotlin
@Composable
private fun SegmentedProgressBar(progress: Float, modifier: GlanceModifier = GlanceModifier) {
    val fillColor = when {
        progress >= 0.95f -> Color(0xFFFF6B6B)
        progress >= 0.80f -> Color(0xFFFFCC80)
        else              -> Color.White
    }
    Row(modifier = modifier) {
        Box(
            modifier = GlanceModifier
                .defaultWeight(progress)
                .fillMaxHeight()
                .background(ColorProvider(fillColor))
        ) {}
        if (progress < 1f) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight(1f - progress)
                    .fillMaxHeight()
                    .background(ColorProvider(Color.White.copy(alpha = 0.25f)))
            ) {}
        }
    }
}
```

## Consecuencias

- El widget ahora refleja con precisión cualquier valor de progreso (0.0–1.0) sin redondeo discreto.
- Visual coherencia con el dashboard: ambos usan barras continuas.
- Simplificación del código: 2 Boxes en lugar de 10 boxes + 9 spacers.
- Los umbrales de color (rojo >95%, amarillo >80%) se mantienen igual.
- Si el progreso es exactamente 100%, se omite el Box vacío.
