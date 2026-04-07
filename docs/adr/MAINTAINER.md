# Guía de Mantenimiento del Sistema ADR

Una bitácora robusta requiere disciplina y proceso. Este documento describe cómo mantener el sistema ADR y changelog del proyecto.

---

## Workflow: Cómo documentar un cambio

### 1️⃣ Identificar si es cambio significativo

**Sí, requiere ADR si:**
- [ ] Agregaste una feature visible al usuario (UI, widget, navegación)
- [ ] Cambiaste un entity field o agregaste un DAO query nuevo
- [ ] Refactorizaste un patrón importante (DI, comunicación entre capas)
- [ ] Modificaste build config, SDK version, o manifest
- [ ] Elegiste entre 2+ alternativas técnicas

**No requiere ADR si:**
- ❌ Bug fix interno que no cambia contrato público
- ❌ Cambio de comentario o renombrado local
- ❌ Optimización sin cambio de arquitectura
- ❌ Actualización de dependencias menor

---

### 2️⃣ Crear el ADR (ANTES o DURANTE implementación)

#### Paso 1: Reservar número
```bash
# Verificar siguiente número disponible
ls docs/adr/*/*ADR-*.md | sort | tail -1
# Si último es ADR-032, el próximo es ADR-033
```

#### Paso 2: Crear archivo
```bash
cd docs/adr/[CATEGORÍA]  # widget, ui, data, architecture, navigation
touch ADR-NNN-slug-descriptivo.md
```

#### Paso 3: Usar plantilla
1. Copiar estructura de `docs/adr/TEMPLATE.md`
2. Rellenar todas las secciones (mínimo: Contexto, Decisión, Consecuencias)
3. Mantener formato consistente

#### Paso 4: Actualizar INDEX.md
Agregar nueva fila a la tabla en `docs/adr/INDEX.md`:
```markdown
| [ADR-033](category/ADR-033-slug.md) | Título | category | Aceptado | 2026-04-06 |
```

---

### 3️⃣ Actualizar CHANGELOG.md

En la sección `[Unreleased]`, agregar entrada:

```markdown
### Added
- ✅ [Descripción breve de la feature]
  **ADR:** [ADR-NNN](docs/adr/category/ADR-NNN-slug.md)  
  **File:** `ruta/al/archivo.kt`
```

O si es fix/cambio:
```markdown
### Fixed
- 🐛 [Descripción del problema y cómo se solucionó]
  **ADR:** [ADR-NNN](...)  
  **Test:** `OcrAmountDetectorTest.kt`
```

---

## Checklist de Pull Request

Cuando estés listos para hacer PR, verifica:

```markdown
### Documentation
- [ ] ADR creado con todas las secciones (Contexto, Decisión, Consecuencias)
- [ ] INDEX.md actualizado con nueva entrada
- [ ] CHANGELOG.md actualizado en sección [Unreleased]
- [ ] Referencias cruzadas: ADR → CHANGELOG, CHANGELOG → ADR

### Code Quality
- [ ] Tests escritos/actualizados
- [ ] CLAUDE.md actualizado si hay cambios arquitectónicos
- [ ] Imports explícitos (ver reglas de CLAUDE.md)
- [ ] Build limpio: `./gradlew build test`

### Architecture
- [ ] Las consecuencias del ADR están reflejadas en el código
- [ ] No hay breaking changes sin documentación explícita
- [ ] DB migrations (si aplica) están numeradas y documentadas
```

---

## Estados de un ADR

### Aceptado ✅
La decisión fue tomada e implementada. Aparece en main branch.
```
**Estado:** Aceptado
```

### Deprecado 🗑️
Todavía en código pero será removido en breve. Marcar deprecation en CHANGELOG.
```
**Estado:** Deprecado
```

### Supersedido por ADR-XXX ↗️
Reemplazado por una decisión posterior. NUNCA eliminar el ADR viejo, solo marcar.
```
**Estado:** Supersedido por ADR-XXX
```

**Ejemplo:** Si cambias de Broadcast (ADR-020) a otro sistema:
1. Marcar ADR-020: `**Estado:** Supersedido por ADR-NNN`
2. Crear nuevo ADR-NNN explicando por qué y qué cambió
3. En CHANGELOG: documentar el cambio bajo la nueva versión

---

## Patrones comunes

### Pattern 1: Cambio en entity + DAO + UI

```
ADR-033: Agregar campo paymentMethod a Expense
├── Data: Expense entity (+1 field)
├── Data: ExpenseDao (nueva query getter)
├── Data: Migration_8_9 (SQL)
├── UI: AddExpenseScreen (nuevo selector)
└── CHANGELOG: +Added, +Changed (DB), *Fixed (queries)
```

**Archivos a actualizar:**
- `docs/adr/data/ADR-033-payment-method.md`
- `app/src/main/.../entity/Expense.kt`
- `app/src/main/.../dao/ExpenseDao.kt`
- `app/src/main/.../migrations/MIGRATION_8_9.kt`
- `app/src/main/.../ui/expenses/AddExpenseScreen.kt`
- `docs/adr/INDEX.md`
- `CHANGELOG.md`

### Pattern 2: Feature nueva UI + Widget

```
ADR-034: Recurring expenses
├── Data: RecurringExpense entity + DAO
├── UI: RecurringExpenseScreen + ViewModel
├── Widget: Nueva tarjeta de upcoming
├── Architecture: Integration en DI
└── CHANGELOG: +Added (UI + widget)
```

### Pattern 3: Refactoring sin cambio de API pública

```
Cambio: Refactorizar OCR processor para legibilidad
├── NO requiere ADR (es refactor interno)
├── SI requiere CHANGELOG si impacta performance o fix conocido
└── SI requiere commit message descriptivo
```

---

## Versionado de releases

Cuando estás listo para hacer release:

### 1. Actualizar CHANGELOG

```markdown
# Mover [Unreleased] a [X.Y.Z]

## [2.1.0] — 2026-04-10  # Cambiar fecha y versión
**Tema:** Descripción breve del release

[Copiar entradas de [Unreleased]]

---

## [Unreleased]
### Added
### Changed
...
```

### 2. Crear git tag
```bash
git tag -a v2.1.0 -m "Release 2.1.0: Dashboard reorganization and developer support"
```

### 3. Verificar coherencia
```bash
# ADRs marcados como Aceptado en INDEX.md deben tener entrada en CHANGELOG
# Fechas en CHANGELOG deben ser ≥ fechas de ADRs
grep "Fecha:" docs/adr/*/*ADR*.md | sort
tail -20 CHANGELOG.md
```

---

## Anti-patrones a evitar

### ❌ No hagas esto

1. **Reescribir un ADR existente**
   - ❌ Cambiar la Decisión de un ADR porque "nos arrepentimos"
   - ✅ Crear nuevo ADR con "Supersedido por ADR-XXX"

2. **Omitir cambios en CHANGELOG**
   - ❌ Hacer PR sin actualizar CHANGELOG
   - ✅ CHANGELOG es parte del "definition of done"

3. **ADRs demasiado cortos**
   - ❌ "Decidimos hacer X porque sí"
   - ✅ Explicar contexto, alternativas rechazadas, consecuencias

4. **Referencias incompletas**
   - ❌ "Ver archivo X" sin path completo
   - ✅ `app/src/main/java/com/example/File.kt`

5. **Olvidar actualizar INDEX.md**
   - ❌ Crear ADR-033 pero no agregarlo a la tabla
   - ✅ Siempre agregar fila a INDEX.md

---

## Tools y scripts útiles

### Script: Verificar completitud
```bash
#!/bin/bash
# docs/adr/verify-adr.sh

echo "🔍 Verificando sistema ADR..."

# Verificar que todo ADR está en INDEX.md
for file in docs/adr/*/*.md; do
  filename=$(basename "$file")
  if grep -q "$filename" docs/adr/INDEX.md; then
    echo "✅ $filename registrado en INDEX.md"
  else
    echo "❌ $filename NO está en INDEX.md"
  fi
done

# Verificar estructura mínima de cada ADR
echo ""
echo "🔍 Verificando estructura de ADRs..."
for file in docs/adr/*/*ADR*.md; do
  if grep -q "^# ADR-" "$file" && grep -q "**Fecha:**" "$file"; then
    echo "✅ $file tiene estructura básica"
  else
    echo "❌ $file le falta estructura"
  fi
done
```

### Script: Generate CHANGELOG entry
```bash
#!/bin/bash
# bin/changelog-entry.sh
# Uso: ./bin/changelog-entry.sh "Added new feature" "Added" "ADR-033"

DESCRIPTION=$1
CATEGORY=$2  # Added, Changed, Fixed, etc
ADR=$3

cat >> CHANGELOG.md << EOF

- [icon] $DESCRIPTION
**ADR:** [$ADR](docs/adr/...)
**File:** \`path/to/file.kt\`
EOF

echo "✅ Entrada agregada a CHANGELOG.md"
```

---

## Reviews de ADR

Cuando un PR agrega un nuevo ADR, reviewer debe verificar:

- ✅ Categoría es correcta y carpeta existe
- ✅ Número es secuencial y único
- ✅ Todas las secciones están presentes
- ✅ Contexto explica el problema claramente
- ✅ Decisión incluye alternativas rechazadas
- ✅ Consecuencias lista impactos técnicos y operacionales
- ✅ INDEX.md está actualizado
- ✅ CHANGELOG.md está actualizado
- ✅ Referencias son completas (paths, commit SHAs, links)

---

## Troubleshooting

### "Olvidé crear el ADR antes de hacer PR"
→ Crear ADR ahora, agregar a INDEX.md, actualizar CHANGELOG. El ADR será retroactivo.

### "Tengo dos ADRs con número igual"
→ Renumerar el más nuevo e inmediatamente después, actualizar INDEX.md.

### "Cambié mi decisión a mitad de la implementación"
→ Marcar ADR viejo como "Deprecado", crear nuevo ADR explicando qué cambió y por qué.

### "El CHANGELOG es demasiado largo"
→ Normal. Archiva releases viejos en `CHANGELOG.archive.md` cada 10 versiones.

---

## Referencias

- [Keep a Changelog](https://keepachangelog.com/) — Formato de changelog
- [Semantic Versioning](https://semver.org/) — Reglas de versionado
- [Google ADR Guide](https://google.aip.dev/decisions/) — Estructura de ADRs
- TEMPLATE.md — Plantilla para nuevos ADRs
- CLAUDE.md → "ADR System" — Reglas del proyecto
