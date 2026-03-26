# Architecture Decision Record — Widget Grid 4x4 Support

## ID
ADR-011

## Título
Soporte para redimensionamiento del widget a cuadrícula 4x4

## Estado
Aceptado

## Fecha
2026-03-26

## Categoría
widget

## Contexto
El widget de tarjetas de crédito estaba configurado originalmente con una altura objetivo de 2 celdas (`targetCellHeight="2"`) y dimensiones máximas de redimensionamiento limitadas a 500dp x 300dp. Esto impedía que en algunos lanzadores (launchers) de Android se pudiera expandir el widget a una cuadrícula completa de 4x4, limitando la visibilidad de múltiples tarjetas simultáneamente en pantallas grandes.

## Decisión
Modificar la configuración del `appwidget-provider` en `app/src/main/res/xml/credit_card_widget_info.xml` para permitir una mayor flexibilidad de tamaño y asegurar el soporte para una cuadrícula de 4x4.

### Cambios realizados:
| Atributo | Original | Nuevo | Razón |
|----------|----------|-------|-------|
| `minWidth` | `250dp` | `200dp` | Mayor compatibilidad con rejillas de launcher más densas. |
| `targetCellHeight` | `2` | `4` | Sugiere al launcher ocupar 4 celdas de alto por defecto o permitirlo fácilmente. |
| `maxResizeWidth` | `500dp` | `600dp` | Soporte para pantallas más anchas. |
| `maxResizeHeight` | `300dp` | `600dp` | Permite expandir el widget verticalmente hasta completar el 4x4. |

## Consecuencias
- **Positivas:**
    - Los usuarios pueden ahora aprovechar todo el alto de la pantalla para ver más tarjetas sin necesidad de scroll excesivo.
    - El widget se adapta mejor a dispositivos con diferentes densidades de cuadrícula (grids).
- **Negativas:**
    - Un widget de 4x4 ocupa gran parte de la pantalla de inicio, por lo que es una decisión del usuario redimensionarlo si así lo desea.
