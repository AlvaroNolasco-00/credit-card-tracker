# Credit Card Tracker — Project Context

Senior developer companion for the **Credit Card Tracker** Android application. This project is a robust, production-ready tool for managing credit card finances, tracking expenses via OCR, and monitoring monthly budgets.

## Project Overview
- **Type:** Android Application (Kotlin 1.9.22, Jetpack Compose, Hilt, Room)
- **Architecture:** MVVM + Clean Architecture (Data, Presentation, Cross-cutting)
- **Target Platform:** Android (compileSdk 34, minSdk 26, targetSdk 34)
- **Key Modules:**
  - **Data Layer:** Room DB (v10+) with 11 entities (CreditCard, Expense, Category, IncomeProfile, BudgetItem, etc.). Reactive DAOs returning `Flow<T>`.
  - **UI Layer:** Jetpack Compose + Material 3. Modular feature folders (`dashboard`, `cards`, `expenses`, `onboarding`, etc.). Centralized navigation in `Navigation.kt`.
  - **OCR Engine:** Custom ML Kit-based processor with geometric scoring, adaptive dark-mode preprocessing, and scoped character correction.
  - **Widgets:** Home screen integration using Jetpack Glance with `WidgetDeepLink` singleton navigation.
  - **Scraper Bot:** Python-based tool for extracting bank promotions from the El Salvador market.

## Building and Running
All build operations use the Gradle wrapper:

```bash
./gradlew build          # Full build and APK generation
./gradlew test           # Run JVM unit tests
./gradlew connectedTest  # Run instrumented tests on device/emulator
./gradlew assembleDebug  # Quick build for debug APK
```

### OCR Testing
To test the OCR logic specifically:
```bash
./gradlew test --tests "com.alvaronolasco.creditcardtracker.ocr.OcrAmountDetectorTest"
```

## Architecture & Data Flow

### Data Layer (`data/`)
- **`CreditCardRepository`**: Single access point for all DAOs.
- **`UserPreferencesRepository`**: Manages user session and naming via `StateFlow`.
- **Entities**: Includes `CreditCard`, `Expense`, `Category`, `ExpenseCategory` (junction), `NotificationConfig`, `IncomeProfile`, `IncomeEntry`, `ActivityLog`, `BudgetItem`, `CategorySpending`, `ExpenseWithCategories`.

### Presentation Layer (`ui/`)
- Each feature folder contains a `*Screen.kt` and a `*ViewModel.kt`.
- **Common Components**: Located in `ui/components/` (`AppButton`, `AppCard`, `AppTextField`, `CardColorPicker`).
- **Theme**: Defined in `ui/theme/` (Color, Type, Shape, Dimensions).

## Development Conventions

### 1. ADR-Driven Development (CRITICAL)
All architectural, UI, or data decisions **must** be documented in `docs/adr/` before implementation.
- **Registry**: `docs/adr/INDEX.md`
- **Workflow**: Create ADR → Update Index → Update CHANGELOG → Implement.
- **PR Checklist**:
  - [ ] ADR created using `TEMPLATE.md`.
  - [ ] `INDEX.md` and `CHANGELOG.md` updated.
  - [ ] Cross-references between ADR and Code verified.

### 2. Implementation Standards
- **Dependency Injection**: Hilt configured at App level (`CreditCardTrackerApp`). All ViewModels use `@HiltViewModel`.
- **OCR Lifecycle**: `OcrProcessor` must implement `Closeable` and be closed in `ViewModel.onCleared()` to prevent memory leaks.
- **Business Logic**: `DateUtils.kt` handles critical billing-cycle and payment-day logic.

### 3. Strict Import Rules
**Wildcard imports are prohibited** except for: `androidx.compose.material3.*`, `androidx.compose.runtime.*`, and `androidx.compose.foundation.layout.*`.

**Entity-to-UI Conversions**: All required imports for conversions must be explicit.
Example for `card.color: Int` to Compose:
```kotlin
import androidx.compose.ui.graphics.Color        // Color(Int)
import androidx.compose.ui.graphics.Brush        // if using gradients
```

### 4. Database Migrations
Migrations (e.g., `MIGRATION_10_11`) are mandatory for schema changes. `fallbackToDestructiveMigration` is a safety net only.

## Key Project Areas

### OCR Engine (`ocr/`)
Sophisticated scoring system (Geometric, Column, Keyword, Position) for detecting receipt amounts:
- **Preprocessing**: Grayscale + Contrast amplification + Adaptive Dark Mode detection.
- **Post-processing**: Scoped character correction (O→0, I→1, S→5) and cached `NumberFormat` localization.

### Notifications (`notifications/`)
- `ReminderScheduler`: Schedules exact alarms for payment/cut-off dates.
- `NotificationHelper`: Manages channels and rich notification layouts (RemoteViews + Canvas thumbnails).

### Scraper Bot (`scraper-bot/`)
- Python scripts using `Playwright` to extract promotions from 6 banks (Agrícola, Cuscatlán, BAC, etc.).

## Recent Focus
Latest updates (v2.2.0+) include:
- Non-card expense tracking with `PaymentMethod` enum.
- Rich notifications with custom Canvas-generated card thumbnails.
- 7-page Onboarding HorizontalPager.
- Bank catalog and selector for SV Banks.
