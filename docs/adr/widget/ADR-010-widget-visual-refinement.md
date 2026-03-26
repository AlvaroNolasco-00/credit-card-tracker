# Architecture Decision Record — Widget Visual Refinement

## ID
ADR-010

## Título
Refinamiento visual del layout de tarjetas en el widget Glance

## Estado
Aceptado

## Fecha
2026-03-26

## Categoría
widget

## Contexto
El layout de las tarjetas en el widget Glance presentaba varios problemas de visualización:
1. El "badge" de información de pago/corte (en la parte inferior) aparecía demasiado pegado a la barra de progreso de gastos.
2. En algunos dispositivos, el contenido inferior se veía recortado o fuera del área oscura de la tarjeta debido a una altura insuficiente del contenedor.
3. El diseño se sentía "apretado" y poco generoso con los espacios internos.

## Decisión
Aumentar las dimensiones y mejorar el espaciado interno de las tarjetas en los layouts `MEDIUM` y `LARGE` del widget.

### Detalles de la decisión:
- **Vista Completa (`FullCardItem`):**
    - Aumentar altura a **120dp**.
    - Aumentar padding vertical a **12dp**.
    - Incrementar los `Spacer` alrededor de la barra de progreso a **10dp** para asegurar separación visual con el badge.
- **Vista Mini (`MiniCardRow`):**
    - Aumentar altura a **72dp** y padding vertical a **10dp**.

## Consecuencias
- **Positivas:**
    - El contenido ya no se recorta.
    - El badge inferior es claramente legible y está bien separado del elemento anterior.
    - Mejora general de la estética y legibilidad del widget.
- **Negativas:**
    - Se reduce ligeramente la cantidad de contenido visible a la vez en el widget (menos tarjetas visibles simultáneamente), aunque el uso de `LazyColumn` mitiga este impacto permitiendo el scroll.
