# ADR-003: Widget ocupa 4 columnas del grid (ancho completo)

**Fecha:** 2026-03-25
**Estado:** Aceptado
**Categoría:** widget

## Contexto

El provider XML del widget tenía `android:targetCellWidth="3"`, lo que hacía que el widget se colocara por defecto en 3 de las 4 columnas del grid estándar de Android. El usuario no podía usar el ancho completo de la pantalla sin redimensionar manualmente.

## Decisión

Modificar `res/xml/credit_card_widget_info.xml`:

| Atributo | Antes | Después | Razón |
|----------|-------|---------|-------|
| `targetCellWidth` | `3` | `4` | Ocupa el grid completo por defecto |
| `minWidth` | `180dp` | `250dp` | Activa layout Medium/Large en lugar de Small al colocarse |
| `maxResizeWidth` | `380dp` | `500dp` | Permite expansión en pantallas grandes (tablets, foldables) |

## Consecuencias

- `targetCellWidth` requiere **API 31+** (Android 12). En Android 11 e inferior, el launcher usa `minWidth` para calcular celdas. Con `minWidth=250dp` en un grid de ~70-80dp/celda, se mapea a ~3-4 columnas según el launcher.
- Los usuarios que ya tienen el widget colocado **no verán el cambio automáticamente**; deben quitarlo y re-añadirlo para que tome el nuevo tamaño por defecto.
- El layout `SizeMode.Responsive` en `CreditCardWidget.kt` sigue activo; `250dp` de ancho activa correctamente los breakpoints Medium (`≥250dp`) y Large (`≥250dp, ≥240dp de alto`).
