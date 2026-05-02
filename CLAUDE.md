# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Credit Card Tracker** — Android app (Kotlin, Jetpack Compose, Hilt, Room)

- `compileSdk 34`, `minSdk 26`, `targetSdk 34`
- Kotlin 1.9.22, KSP 1.9.22-1.0.17

## Commands

```bash
./gradlew build          # Build APK
./gradlew test           # Unit tests (JVM)
./gradlew connectedTest  # Instrumented tests (device/emulator)
./gradlew assembleDebug  # Debug APK only
```

Run a single test class:
```bash
./gradlew test --tests "com.alvaronolasco.creditcardtracker.ocr.OcrAmountDetectorTest"
```

## Architecture

**MVVM + Clean Architecture** with three layers:

### Data Layer (`data/`)
- **Room DB** (version 10, migrations via `MIGRATION_6_7` → `MIGRATION_7_8` → `MIGRATION_8_9` → `MIGRATION_9_10`, `fallbackToDestructiveMigration` as safety net): 11 entities — `CreditCard`, `Expense`, `Category`, `ExpenseCategory` (junction), `NotificationConfig`, `IncomeProfile`, `IncomeEntry`, `ActivityLog`, `BudgetItem`, `CategorySpending`, `ExpenseWithCategories`
- **DAOs** return `Flow<T>` for reactive queries — see `data/dao/`
- **`CreditCardRepository`** is the single data access point, aggregating all DAOs
- **`UserPreferencesRepository`** manages SharedPreferences (user name, etc.) via `StateFlow`
- Default categories (Entretenimiento, Transporte, Comida, Medicina) are seeded on first DB creation

### Presentation Layer (`ui/`)
- Jetpack Compose + Material 3
- Each feature folder contains a `*Screen.kt` (composable) and a `*ViewModel.kt`
- Feature modules: `dashboard/`, `cards/`, `expenses/`, `income/`, `budget/`, `activity/`, `stats/`, `onboarding/`, `support/`, `theme/`, `navigation/`, `components/`
- Navigation is centralized in `ui/navigation/Navigation.kt` (single `NavHost`) — see that file for the full route list
- Reusable design-system components live in `ui/components/` (`AppButton`, `AppCard`, `AppTextField`, `AppTopBar`, `AppChip`, `AppLoadingIndicator`, `EmptyStateView`, `CardColorPicker`)
- Theme defined in `ui/theme/` (Color, Type, Shape, Dimensions)

### Cross-Cutting Modules
- **DI** (`di/AppModule.kt`): Hilt `@Singleton` provides `AppDatabase`, `CreditCardRepository`, `UserPreferencesRepository`, `SharedPreferences`, and a `CoroutineScope` with `SupervisorJob`
- **OCR** (`ocr/OcrProcessor.kt`): ML Kit Text Recognition. Unified scoring system with geometric alignment, column detection, dark-mode adaptive preprocessing, scoped character correction, pre-compiled regexes, and cached `NumberFormat`. Returns a confidence level (HIGH/MEDIUM/LOW/NONE). See ADR-033 through ADR-043.
- **Notifications** (`notifications/`): `ReminderScheduler` creates exact alarms; `ReminderReceiver` fires them; `BootReceiver` reschedules after reboot; `NotificationHelper` manages channel creation and display.
- **Widget** (`widget/`): Glance-based home screen widget. `WidgetDeepLink` is a singleton `StateFlow`-based handler for navigating into the app from widget taps.

## Key Conventions

- Hilt is configured at the app level (`CreditCardTrackerApp : Application()`); all ViewModels use `@HiltViewModel`.
- Camera + OCR flow: `CameraPreviewScreen` captures an image → `OcrProcessor` extracts amounts → result passed back via navigation.
- `DateUtils.kt` handles billing-cycle logic (cut-off days, payment days).
- `file_paths.xml` + `FileProvider` are used for sharing captured images externally.

## ADR System

All significant decisions **must** be documented in `docs/adr/` before or immediately after implementation.

**Quick reference:**
- **When:** UI/widget features, entity changes, architecture patterns, SDK changes, multi-option decisions
- **Template:** [docs/adr/TEMPLATE.md](docs/adr/TEMPLATE.md) — Structure: Contexto → Decisión → Consecuencias
- **Registry:** [docs/adr/INDEX.md](docs/adr/INDEX.md) — All 45+ decisions, searchable by category
- **Example:** [docs/adr/EXAMPLE-DETAILED.md](docs/adr/EXAMPLE-DETAILED.md) — Reference (ADR-029, ⭐⭐⭐⭐⭐ quality)
- **Maintenance:** [docs/adr/MAINTAINER.md](docs/adr/MAINTAINER.md) — Workflow, PR checklist, troubleshooting, release procedures
- **Changelog:** [CHANGELOG.md](CHANGELOG.md) — Synchronized bitácora (each entry cites ADR + files)

**File naming:** `docs/adr/<category>/ADR-NNN-slug.md` (categories: `widget/`, `ui/`, `data/`, `architecture/`, `navigation/`)

**PR checklist:**
- [ ] ADR created (use TEMPLATE.md)
- [ ] docs/adr/INDEX.md updated
- [ ] CHANGELOG.md updated in [Unreleased]
- [ ] Cross-references: ADR ↔ CHANGELOG ↔ Code

## Import Rules

### Entity property → UI conversion chain

Whenever an entity field is used in a UI file (Compose screen, widget, component), **all required imports for the conversion must be declared explicitly**. Never rely on wildcard imports (`import androidx.compose.ui.*`) to cover conversion types.

**`CreditCard.color: Int` → Compose**

Every file that converts `card.color` to a Compose color must import:
```kotlin
import androidx.compose.ui.graphics.Color        // Color(Int)
import androidx.compose.ui.graphics.Brush        // if using gradients
import androidx.compose.foundation.shape.RoundedCornerShape  // if using rounded backgrounds
```

**General rule — before using any entity field in UI:**

1. Identify the Kotlin type of the field (e.g., `Int`, `Long`, `String`).
2. Identify every conversion or wrapper needed (e.g., `Color(Int)`, `Uri.parse(String)`, `Instant.ofEpochMilli(Long)`).
3. Verify each conversion class/function is explicitly imported in the file.
4. If the import was previously provided by a wildcard (`import com.example.*`), add the explicit import anyway — wildcards are not reliable documentation.

**Wildcard imports are allowed** only for: `import androidx.compose.material3.*`, `import androidx.compose.runtime.*`, `import androidx.compose.foundation.layout.*`. All other imports must be explicit.

<!-- code-review-graph MCP tools -->
## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes` or `query_graph` instead of Grep
- **Understanding impact**: `get_impact_radius` instead of manually tracing imports
- **Code review**: `detect_changes` + `get_review_context` instead of reading entire files
- **Finding relationships**: `query_graph` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview` + `list_communities`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool | Use when |
|------|----------|
| `detect_changes` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context` | Need source snippets for review — token-efficient |
| `get_impact_radius` | Understanding blast radius of a change |
| `get_affected_flows` | Finding which execution paths are impacted |
| `query_graph` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes` | Finding functions/classes by name or keyword |
| `get_architecture_overview` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes` for code review.
3. Use `get_affected_flows` to understand impact.
4. Use `query_graph` pattern="tests_for" to check coverage.
