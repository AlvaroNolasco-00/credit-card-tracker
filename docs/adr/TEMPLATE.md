# ADR-NNN: [Título descriptivo]

**Fecha:** YYYY-MM-DD  
**Estado:** Aceptado | Deprecado | Supersedido por ADR-XXX  
**Categoría:** widget | ui | data | navigation | architecture  
**Prioridad:** Critical | High | Medium | Low  
**Afecta:** [Componentes/módulos afectados]  

---

## Contexto

Explica el problema o necesidad que motivó esta decisión. Incluye:
- El estado actual o problema identificado
- Por qué es importante resolverlo
- Restricciones o limitaciones que condicionan la solución
- Conversaciones o investigación previa

**Ejemplo:**
```
El widget actualmente muestra solo 3 tarjetas máximo. Los usuarios con 5+ 
tarjetas no pueden acceder a todas. Investigamos tres opciones:
1. LazyColumn (scroll vertical)
2. Pager (deslizable horizontal)
3. Expandible (accordion)
```

---

## Decisión

Describe **qué** se decide y **por qué** se elige esa opción específica.

### Opción elegida
Describe la solución adoptada en detalle.

### Por qué esta opción
- Ventaja 1: [descripción]
- Ventaja 2: [descripción]
- Trade-off 1: [descripción del costo/limitación]

### Opciones rechazadas
**Opción A: [nombre]**
- ❌ Razón de rechazo
- ❌ Otra razón

**Opción B: [nombre]**
- ❌ Razón de rechazo

---

## Consecuencias

### Directas
- ✅ Beneficio observable
- ✅ Otro beneficio
- ⚠️ Implicación operacional
- ❌ Costo o limitación

### Técnicas
**Archivos/módulos impactados:**
- `app/src/.../Widget.kt` — Cambio en layout
- `app/src/.../WidgetData.kt` — Estructura de datos
- `docs/adr/` — Este registro

**Breaking changes:**
- (Si aplica) Cambios en contratos públicos
- (Si aplica) Migraciones de datos necesarias

### Operacionales
- Testing requerido: [manual/automated/device]
- Documentación: [actualizar qué]
- Comunicación: [stakeholders afectados]

---

## Implementación

### Paso a paso
1. Crear `WidgetCardList.kt` con LazyColumn
2. Refactorizar `WidgetProvider.kt` para usar nueva composable
3. Agregar tests en `WidgetCardListTest.kt`
4. Validar en dispositivo con 5+ tarjetas

### Files de referencia
- PR: [#123](https://github.com/.../pull/123)
- Commit: `abc123d` — Descripción del cambio principal
- Tests: `app/src/test/.../WidgetCardListTest.kt`

---

## Validación

### Cómo verificar que la decisión se implementó correctamente
- [ ] Reproduzco con 5+ tarjetas y el scroll funciona
- [ ] Los límites de gastos aún se muestran correctamente
- [ ] El widget no crashea al agregar/eliminar tarjeta
- [ ] Performance: widget carga en <500ms

### Métricas de éxito
- Usuarios con 5+ tarjetas pueden acceder a todas
- Tiempo de scroll es fluido (60 fps)
- Sin crashes en logs

---

## Notas y Aprendizajes

- [Aprendizaje 1] Si necesitas hacer X, asegúrate de [detalle]
- [Aprendizaje 2] Evita [patrón ineficiente] porque [razón]
- [Future work] Considera migrar a Pager en la próxima iteración si [condición]

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-06 | Documento inicial |
| 2026-04-10 | Actualizado tras feedback de PR #125 |

---

## Referencias

- [ADR-XXX](../path/ADR-XXX-related.md) — Decisión relacionada (precursor/sucesor)
- [Google ADR Template](https://google.aip.dev/decisions/) — Formato base
- [Blog sobre LazyColumn](https://developer.android.com) — Implementación de referencia
