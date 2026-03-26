# ADR-006: Barra de progreso de gastos en widget

**Fecha:** 2026-03-25
**Estado:** Aceptado
**Categoría:** widget

## Contexto

El widget mostraba el monto gastado y el límite de la tarjeta, pero no había ninguna indicación visual inmediata de qué tan cerca estaba el usuario de alcanzar su límite de crédito.

## Decisión

Agregar una barra de progreso segmentada en los layouts Large y Medium del widget.

### Por qué segmentada y no `LinearProgressIndicator`

Glance 1.0.0 no expone un composable `LinearProgressIndicator`. La única forma de tener una barra con relleno proporcional en RemoteViews es:
1. Usar `AndroidRemoteViews` con un `ProgressBar` nativo — complejo, pierde coherencia visual.
2. Usar una fila de N boxes con `defaultWeight()` — cada uno ocupa 1/N del ancho total, sin necesitar conocer el ancho real en dp.

Se eligió la opción 2 con **10 segmentos** separados por 2dp de espacio, dando una resolución del 10% por segmento, suficiente para este caso de uso.

### Lógica de progreso

```kotlin
val progress = if (data.card.creditLimit > 0)
    (data.totalSpent / data.card.creditLimit).toFloat().coerceIn(0f, 1f)
else 0f
```

`data.totalSpent` es el gasto acumulado en el período de corte actual (ya calculado en `provideGlance`).

### Color de alerta de la barra

| Rango | Color | Significado |
|-------|-------|-------------|
| `< 80%` | Blanco | Uso normal |
| `80–95%` | `#FFCC80` (ámbar) | Atención, acercándose al límite |
| `> 95%` | `#FF6B6B` (rojo suave) | Límite casi alcanzado |

Los colores contrastan bien sobre cualquier color de tarjeta ya que se muestran sobre el gradiente oscuro del fondo.

### Cambios de dimensiones

| Componente | Antes | Después | Razón |
|------------|-------|---------|-------|
| `FullCardItem` height | 76dp | 84dp | Espacio para barra (4dp) + separadores (5dp arriba, ~3dp spacer) |
| `MiniCardRow` height | 48dp | 56dp | Espacio para barra (4dp) + padding inferior (4dp) |
| `MiniCardRow` estructura | `Box(padding)` → `Row` | `Box` → `Column(Row + bar + spacer)` | Necesario para apilar la barra bajo el contenido |

### Import requerido

```kotlin
import kotlin.math.roundToInt  // para (progress * 10).roundToInt()
```

## Consecuencias

- Los layouts del widget son ligeramente más altos; usuarios con widgets ya colocados verán el cambio tras la próxima actualización automática o manual.
- La barra del SmallLayout no se agrega: a ~110dp de ancho, el espacio es insuficiente para que 10 segmentos sean distinguibles.
- Si `creditLimit == 0` (tarjeta sin límite configurado), el progreso se fija en 0 y la barra aparece vacía.
