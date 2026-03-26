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
- **Room DB** (version 2, `fallbackToDestructiveMigration`): 6 entities — `CreditCard`, `Expense`, `Category`, `NotificationConfig`, `IncomeProfile`, `IncomeEntry`
- **DAOs** return `Flow<T>` for reactive queries
- **`CreditCardRepository`** is the single data access point, aggregating all DAOs
- Default categories (Entretenimiento, Transporte, Comida, Medicina) are seeded on first DB creation

### Presentation Layer (`ui/`)
- Jetpack Compose + Material 3
- Each feature folder contains a `*Screen.kt` (composable) and a `*ViewModel.kt`
- Navigation is centralized in `ui/navigation/Navigation.kt` (single `NavHost`)
- Reusable design-system components live in `ui/components/` (`AppButton`, `AppCard`, `AppTextField`, `AppTopBar`, etc.)
- Theme defined in `ui/theme/` (Color, Type, Shape, Dimensions)

### Cross-Cutting Modules
- **DI** (`di/AppModule.kt`): Hilt `@Singleton` provides `AppDatabase` and `CreditCardRepository`
- **OCR** (`ocr/OcrProcessor.kt`): ML Kit Text Recognition. Uses a 4-tier detection strategy — keyword match → positional heuristic → last-section scan → max-amount fallback. Returns a confidence level (HIGH/MEDIUM/LOW/NONE).
- **Notifications** (`notifications/`): `ReminderScheduler` creates exact alarms; `ReminderReceiver` fires them; `BootReceiver` reschedules after device reboot.
- **Widget** (`widget/`): Glance-based home screen widget showing card summary.

## Key Conventions

- Hilt is configured at the app level (`CreditCardTrackerApp : Application()`); all ViewModels use `@HiltViewModel`.
- Camera + OCR flow: `CameraPreviewScreen` captures an image → `OcrProcessor` extracts amounts → result passed back via navigation.
- `DateUtils.kt` handles billing-cycle logic (cut-off days, payment days).
- `file_paths.xml` + `FileProvider` are used for sharing captured images externally.
