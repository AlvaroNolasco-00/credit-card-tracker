# ADR-032: Reorganización de Dashboard y Acceso a Apoyo al Desarrollador

**Fecha:** 2026-04-03
**Estado:** Aceptado
**Categoría:** ui

## Contexto

El botón de "Agregar tarjeta" (un ícono de suma verde) se encontraba en el encabezado del `DashboardScreen`. El usuario consideró que este elemento estaba "fuera de contexto" en la parte superior y prefería una integración más natural con el flujo de tarjetas. Además, se buscaba dar mayor visibilidad a la opción de "Apoya al desarrollador" integrándola en la interfaz principal.

## Decisión

Se han realizado los siguientes cambios estructurales en el `DashboardScreen`:

1.  **Reubicación de "Agregar tarjeta"**:
    *   Se eliminó el botón `IconButton` con el ícono `Add` del encabezado.
    *   Se extendió el `HorizontalPager` de tarjetas agregando una página adicional al final (`uiState.cards.size + 1`).
    *   Se creó el componente `AddCardTile` que se muestra como la última página del pager. Este componente tiene la misma relación de aspecto que las tarjetas de crédito reales, con un borde degradado (`ForestGreen` a `MintGreen`) y un ícono de suma central, manteniendo la consistencia visual.
    *   La navegación de los indicadores de página (dots) se limitó a las tarjetas reales para evitar confusión visual con la tarjeta de acción.

2.  **Integración de "Apoya al desarrollador" en el Encabezado**:
    *   El espacio anteriormente ocupado por el botón de agregar tarjeta en el encabezado ahora contiene un botón de acceso directo a la función de apoyo.
    *   Se utiliza un `IconButton` con el ícono `Favorite` en color `ForestGreen` sobre un fondo `SoftLime` circular.
    *   Al presionar este botón se invoca directamente la acción `onSupportClick`.

## Consecuencias

*   **Mejora UX**: La acción de agregar una tarjeta ahora se siente como parte del carrusel de tarjetas, lo cual es más intuitivo para el usuario ("quiero otra tarjeta").
*   **Visibilidad de Apoyo**: La función de donación/apoyo ahora tiene un lugar privilegiado y constante en el encabezado, aumentando la probabilidad de conversión sin ser intrusiva.
*   **Seguridad de Estado**: El `selectedCard` en `DashboardScreen` se obtiene de forma segura mediante `getOrNull`, por lo que cuando el pager está en la última posición (la de "Agregar otra tarjeta"), las secciones dependientes de una tarjeta seleccionada (como la fila de corte o el botón de pago) simplemente no se renderizan, evitando errores de índice.
