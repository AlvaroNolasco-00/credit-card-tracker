# ADR-019: Notificaciones con toggles en lista vertical

**Fecha:** 2026-03-27
**Estado:** Aceptado
**Categoría:** ui

## Contexto

En `NotificationSettingsSection`, los intervalos de notificación (El día, 1 día antes, 3 días antes, 5 días antes) se mostraban como `FilterChip` en una fila horizontal compacta. Esto es poco legible en móviles y no comunica claramente que son opciones ON/OFF independientes.

## Decisión

Se reemplaza el layout horizontal de chips por una lista vertical donde cada intervalo es una fila con:
- Label descriptivo del intervalo
- `Switch` a la derecha para activar/desactivar

### Cambios visuales

- Se mantienen dos secciones (Corte / Pago) en tarjetas separadas
- Header mejorado: "Recordatorios de Corte" / "Recordatorios de Pago"
- Separadores `HorizontalDivider` entre opciones
- Labels más claros: "El mismo día" en lugar de "El día"
- Cada `Switch` usa colores del esquema Material (primary para activado)

### Estructura de datos

Sigue usando `NotificationConfig.enabled` como booleano sin cambios en la persistencia.

## Consecuencias

- UX más clara: cada intervalo es una opción discretay visible
- Mejor legibilidad en pantallas pequeñas
- Patrón familiar: lista de opciones con toggles es estándar en Android
- El ícono y header refuerzan la agrupación por tipo (Corte/Pago)
