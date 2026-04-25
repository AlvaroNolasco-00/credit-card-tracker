# AGENTS.md

## Build & Test Commands

```bash
./gradlew build          # Full build (APK)
./gradlew test           # Unit tests (JVM)
./gradlew connectedTest  # Instrumented tests (device/emulator)
./gradlew assembleDebug  # Debug APK only
```

**Single test:**
```bash
./gradlew test --tests "com.alvaronolasco.creditcardtracker.ocr.OcrAmountDetectorTest"
```

## ADR System (MANDATORY)

**Every significant change requires an ADR** in `docs/adr/<category>/`:
- UI/widget features, entity changes, architecture patterns, SDK changes
- Use template from `docs/adr/TEMPLATE.md`
- Update `docs/adr/INDEX.md` after creating
- Add entry to `CHANGELOG.md` under `[Unreleased]`

**Checklist before PR:**
- [ ] ADR created with Contexto, Decisión, Consecuencias
- [ ] INDEX.md updated
- [ ] CHANGELOG.md updated
- [ ] Build passes: `./gradlew build test`

## Import Rules (CRITICAL)

When using entity fields in UI code, **explicitly import all conversion types**:
```kotlin
import androidx.compose.ui.graphics.Color        // Color(Int)
import androidx.compose.ui.graphics.Brush
// ...never rely on wildcards for conversion types
```

Wildcards allowed only for: `material3.*`, `runtime.*`, `foundation.layout.*`

## Architecture Notes

- **Room DB**: Uses migrations (`MIGRATION_X_Y`), not just `fallbackToDestructiveMigration`
- **Widget**: Uses Glance + `WidgetDeepLink` singleton (StateFlow-based navigation)
- **OCR**: ML Kit Text Recognition, managed in `ocr/OcrProcessor.kt`, has dark-mode preprocessing
- **DI**: Hilt `@Singleton` for AppDatabase, Repository, Preferences

## Project Structure

- `app/src/main/java/com/alvaronolasco/creditcardtracker/`
  - `data/` — Room entities, DAOs, repositories
  - `ui/` — Compose screens, ViewModels, navigation
  - `di/` — Hilt modules
  - `ocr/` — ML Kit processing
  - `notifications/` — AlarmManager + BroadcastReceiver
  - `widget/` — Glance-based home screen widget

## Key Files

- `CLAUDE.md` — Full project context and conventions
- `docs/adr/MAINTAINER.md` — ADR workflow details
- `CHANGELOG.md` — Must be updated with every change

---

# context-mode — MANDATORY routing rules

You have context-mode MCP tools available. These rules are NOT optional — they protect your context window from flooding.

## Think in Code — MANDATORY

When you need to analyze, count, filter, compare, search, parse, transform, or process data: **write code** that does the work via `context-mode_ctx_execute(language, code)` and `console.log()` only the answer. Do NOT read raw data into context to process mentally. Write robust, pure JavaScript — no npm dependencies, only Node.js built-ins. Use `try/catch`, handle `null`/`undefined`.

## BLOCKED commands

### curl / wget — BLOCKED
Any shell command containing `curl` or `wget` will be intercepted. Use:
- `context-mode_ctx_fetch_and_index(url, source)` to fetch web pages
- `context-mode_ctx_execute(language: "javascript", code: "const r = await fetch(...)")` for HTTP

### Inline HTTP — BLOCKED
Any shell command with `fetch('http`, `requests.get(`, `http.get(` will be intercepted. Use sandbox instead.

## REDIRECTED tools

### Shell (>20 lines output)
Shell is ONLY for: `git`, `mkdir`, `rm`, `mv`, `cd`, `ls`, `npm`, `./gradlew`.
For analysis, use:
- `context-mode_ctx_batch_execute(commands, queries)` — multiple commands + search in ONE call
- `context-mode_ctx_execute(language: "shell", code: "...")` — sandbox execution

### File reading (for analysis)
If reading to **edit** → reading is correct.
If reading to **analyze/summarize** → use `context-mode_ctx_execute_file(path, language, code)`

### grep / search
Use `context-mode_ctx_execute(language: "shell", code: "grep ...")` in sandbox. Only summary enters context.

## Tool hierarchy

1. **GATHER**: `context-mode_ctx_batch_execute` — Primary. Multiple commands + search in ONE call.
2. **FOLLOW-UP**: `context-mode_ctx_search(queries)` — Query indexed content.
3. **PROCESSING**: `context-mode_ctx_execute(language, code)` — Sandbox. Only stdout enters context.
4. **WEB**: `context-mode_ctx_fetch_and_index(url, source)` then `context-mode_ctx_search(queries)`
5. **INDEX**: `context-mode_ctx_index(content, source)` — Store in FTS5 knowledge base.

## ctx commands

| Command | Action |
|---------|--------|
| `ctx stats` | Display context savings |
| `ctx doctor` | Run diagnostics |
| `ctx upgrade` | Upgrade context-mode |
| `ctx purge` | Delete all indexed content |

After /clear or /compact: knowledge base and session stats are preserved.
