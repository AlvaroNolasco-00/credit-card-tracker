# ADR-048: Catálogo de Bancos SV y Selector Dropdown

**Fecha:** 2026-04-17  
**Estado:** Aceptado  
**Categoría:** data, ui  
**Prioridad:** High  
**Afecta:** `CreditCard`, `AddEditCardScreen`, `SupportedBank`, `BankPicker`, `CardsViewModel`  

---

## Contexto

Campo `bank: String` hoy es texto libre — usuarios escriben "BAC", "bac", "Bac Credomatic" sin normalización.
Esto rompe la futura vinculación con scraper de promociones bancarias (commit `4e643d3`), que necesita identificador estable.

Investigación en `plans/investigacion_scraper.md` identificó 6 bancos emisores en El Salvador:
- Banco Agrícola, Banco Cuscatlán, BAC Credomatic, Banco Promerica, Banco Davivienda, Credicomer

Cada uno publica promociones en URLs canónicas. Se requiere:
1. Catálogo de bancos soportados (enum con id, displayName, promotionsUrl)
2. Selector UI dropdown + opción "Otro (personalizado)"
3. Campo `bankId: String?` en entidad para binding futuro con scraper
4. Migración DB v11→v12
5. Retrocompatibilidad para tarjetas legacy (auto-detect por nombre)

---

## Decisión

### Opción elegida

**Crear `SupportedBank` enum + campo `bankId` nullable en `CreditCard`**

1. Nueva entidad catálogo `SupportedBank(id, displayName, promotionsUrl)`:
   - IDs: `banco_agricola`, `banco_cuscatlan`, `bac_credomatic`, `banco_promerica`, `banco_davivienda`, `credicomer`
   - `displayName`: nombres localizados (ej. "Banco Cuscatlán")
   - `promotionsUrl`: lista canónica para scraper futuro
   - Método `fromId(id)` + `fromDisplayName(name)` para lookups

2. Agregar campo `bankId: String? = null` a `CreditCard` entity:
   - `bankId != null` → banco canónico (identificador estable)
   - `bankId == null` → banco personalizado (texto libre original en `bank`)
   - Preserva retrocompatibilidad: tarjetas legacy con texto libre siguen funcionando

3. Migración DB v11→v12:
   - `ALTER TABLE credit_cards ADD COLUMN bankId TEXT` (nullable)
   - Existing rows heredan `bankId = NULL` → modo custom

4. UI: Nuevo componente `BankPicker` (ExposedDropdownMenuBox):
   - Lista 6 bancos + opción "Otro (personalizado)"
   - Si "Otro" seleccionado → muestra `AppTextField` para texto libre
   - Si banco de lista seleccionado → campo readonly con display name

5. Refactorizar `AddEditCardScreen`:
   - Reemplazar `AppTextField(label="Banco")` por `BankPicker`
   - Estado: `selectedBankId: String?` + `customBankName: String`
   - Al cargar tarjeta legacy: auto-detect via `SupportedBank.fromDisplayName(card.bank)`
   - `effectiveBankName = SupportedBank.fromId(selectedBankId)?.displayName ?: customBankName`

6. `CardsViewModel.saveCard()`:
   - Nuevo parámetro `bankId: String?`
   - Flujo a `CreditCard(bank = bank, bankId = bankId, ...)`

### Por qué esta opción

- ✅ **Identificador estable para scraper:** `bankId` permite futura vinculación con `SupportedBank.promotionsUrl` sin coincidencias fuzzy
- ✅ **Retrocompatibilidad total:** Tarjetas legacy con `bankId = null` usan el texto en `bank` (sin requerimientos de migración de datos)
- ✅ **Auto-detect en edit:** Si carga una tarjeta antigua con `bank = "Banco Agrícola"` → intenta match con enum → asigna `bankId = "banco_agricola"` automáticamente
- ✅ **Validación temprana:** Enum en código vs texto libre → errores de tipeo detectados en compilación
- ✅ **Escalabilidad:** Si se agregan bancos → solo modificar enum (sin cambios de schema)
- ✅ **UX clara:** Dropdown + opción custom cubre 99% de tarjetas reales sin forzar cambios

### Opciones rechazadas

**Opción A: Enum puro (sin custom)**
- ❌ Fuerza migración de datos si hay tarjetas con bancos raros/privados
- ❌ Rompe experiencia: usuarios no pueden crear tarjetas de issuers no libreados

**Opción B: Solo `bank: String` normalizado**
- ❌ Scraper debe usar fuzzy matching → frágil
- ❌ Sin identificador único para vinculación

**Opción C: Tabla separada `Bank` + FK**
- ❌ Sobre-ingenierización: enum resuelve el problema actual
- ❌ Migraciones más complejas

---

## Consecuencias

### Directas

- ✅ Identifier stable (`bankId`) listo para scraper de promociones
- ✅ UX mejorada: selector visual en lugar de texto libre
- ✅ Tarjetas legacy compatibles (auto-detect en primer edit)
- ✅ Catálogo centralizado en código (versión controlada)
- ⚠️ Migración DB requerida (v11→v12, pero non-breaking)
- ❌ Scraper aún no implementado (fuera de alcance de este ADR)

### Técnicas

**Archivos creados:**
- `data/entity/SupportedBank.kt` — enum catálogo (205 líneas)
- `ui/components/BankPicker.kt` — composable dropdown + custom field (76 líneas)

**Archivos modificados:**
- `data/entity/CreditCard.kt` — `+bankId: String? = null`
- `data/AppDatabase.kt` — v11→12, `MIGRATION_11_12`, registro de migración
- `ui/cards/AddEditCardScreen.kt` — reemplazar TextField por BankPicker, auto-detect logic
- `ui/cards/CardsViewModel.kt` — parámetro `bankId: String?` en `saveCard()`

**Breaking changes:** Ninguno (campo nuevo es nullable, valor por defecto `null`)

**Migraciones:** `MIGRATION_11_12` (safe, no data loss)

### Operacionales

- Testing requerido:
  - [ ] Nueva tarjeta con banco de lista → `bankId` asignado correctamente
  - [ ] Nueva tarjeta con banco custom → `bankId = null`, `bank` = custom text
  - [ ] Editar tarjeta legacy con `bank = "Banco Agrícola"` → auto-detect asigna `bankId`
  - [ ] Widget + notificaciones siguen mostrando `card.bank` sin cambios
  - [ ] Upgrade DB de v11: existing rows heredan `bankId = NULL` sin crash

- Documentación: Este ADR + referencias en CHANGELOG

---

## Implementación

### Paso a paso

1. ✅ Crear `SupportedBank.kt` con enum + métodos lookup
2. ✅ Agregar `bankId` a `CreditCard`, crear `MIGRATION_11_12`, registrar migración
3. ✅ Crear `BankPicker.kt` con `ExposedDropdownMenuBox`
4. ✅ Refactorizar `AddEditCardScreen`: reemplazar TextField, agregar auto-detect
5. ✅ Actualizar `CardsViewModel.saveCard()` con parámetro `bankId`
6. ✅ Build successful sin errores

### Files de referencia

- **Nueva feature:** Commits `<hash>` — SupportedBank enum + BankPicker + integration
- **Scraper futuro:** Consumir `SupportedBank.promotionsUrl` + `card.bankId` para linking

---

## Validación

### Cómo verificar que la decisión se implementó correctamente

- [ ] Build compila sin errores (`./gradlew assembleDebug`)
- [ ] Nueva tarjeta: elegir "BAC Credomatic" del dropdown → `bankId = "bac_credomatic"`, `bank = "BAC Credomatic"`
- [ ] Nueva tarjeta: elegir "Otro", escribir "CREDIMÁS" → `bankId = null`, `bank = "CREDIMÁS"`
- [ ] Editar tarjeta legacy con banco: auto-detect funciona si nombre coincide (case-insensitive)
- [ ] Upgrade DB v11→v12: no crash, existing tarjetas heredan `bankId = NULL`
- [ ] Widget muestra tarjetas correctamente (sin cambios en display)
- [ ] Notificaciones siguen funcionando (usan `card.bank` string)

### Métricas de éxito

- Tarjetas canónicas tienen `bankId` asignado → listo para scraper
- 0% data loss en migración
- UX fluida: selector visual + opción custom
- Tarjetas legacy funcionan sin friction

---

## Notas y Aprendizajes

- **Auto-detect es clave:** Permite upgrade sin requerer user action (mejor UX)
- **Nullable `bankId` > non-nullable:** La flexibilidad de "otro" es crítica para casos edge
- **Promotions URL en enum:** Futura scraper puede iterar `SupportedBank.entries` + `promotionsUrl` para el crawl
- **[Future work]** Cuando scraper esté listo: crear `BankPromotion` entity, linking via `card.bankId`

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-17 | Documento inicial + implementación completa |

---

## Referencias

- [plans/investigacion_scraper.md](../../plans/investigacion_scraper.md) — Investigación de bancos SV
- [CHANGELOG.md](../../CHANGELOG.md) — Entrada [Unreleased] ADR-048
