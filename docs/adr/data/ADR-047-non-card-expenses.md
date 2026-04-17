# ADR-047: Gastos No-Tarjeta — Débito, Transferencia, Efectivo

**Categoría:** data  
**Estado:** Aceptado  
**Fecha:** 2026-04-16

## Contexto

Hasta la versión DB 10, cada `Expense` tenía `cardId: Int` obligatorio (FK NON-NULL con CASCADE a `CreditCard`). Esto impedía registrar gastos pagados con medios no-crediticios (débito, transferencia bancaria, efectivo), que sí reducen el dinero disponible del usuario. El usuario necesitaba ver su ingreso mensual disponible real: `Ingresos − Gastos de tarjeta − Gastos personales`.

## Decisión

1. **`cardId: Int?` (nullable)** — FK con `ON DELETE SET NULL`. Gastos no-tarjeta tienen `cardId = null`.
2. **Campo `paymentMethod: String`** — enum `PaymentMethod { CREDIT_CARD, DEBIT_CARD, TRANSFER, CASH, OTHER }` almacenado como String. Default `CREDIT_CARD`.
3. **Migración DB 10→11** — recreación de tabla `expenses` (SQLite no soporta `ALTER COLUMN`) con datos existentes migrados como `CREDIT_CARD`.
4. **`AddExpenseScreen`** acepta `cardId: Int?`. Selector `PaymentMethodSelector` al tope del formulario. Si método ≠ `CREDIT_CARD`: ocultar CardTargetBanner y sección MSI; `cardId = null` al guardar.
5. **Dashboard**: `DashboardViewModel.loadNonCardSpending()` carga `getTotalNonCardSpentInPeriod` del mes calendario. `DashboardUiState.totalNonCardSpent` nuevo campo. `SalaryUsageCard` muestra "Tarjetas + Personal" vs ingresos y calcula "Disponible".
6. **Navegación**: ruta `add_personal_expense` abre `AddExpenseScreen(cardId = null)`. `BottomActionBar` tiene botón "Personal" junto a "Tarjeta".

## Alternativas descartadas

- **Boolean `isCardExpense`**: menos info para reporting futuro; no permite distinguir débito vs transferencia.
- **Entidad `PersonalExpense` separada**: duplicaría DAOs, queries y UI; no justificado para un campo extra.
- **`cardId` nullable sin `paymentMethod`**: ambiguo — no se sabe por qué es null.

## Consecuencias

- **Positivo:** registro completo de flujos de dinero; dashboard refleja disponible real; sin duplicación de UI.
- **Negativo:** migración DB requiere recrear tabla (lenta en DBs muy grandes, mitigado por `fallbackToDestructiveMigration` como safety net).
- **Impacto en código existente:** queries por-tarjeta sin cambio (filtran por `cardId`); `ExpensesViewModel.saveExpense` acepta `cardId: Int?`; `edit_expense` route pasa `cardId = null` (carga desde expense ID).

## Archivos modificados

| Archivo | Cambio |
|---------|--------|
| `data/entity/PaymentMethod.kt` | Nuevo enum |
| `data/entity/Expense.kt` | `cardId: Int?`, `paymentMethod: String`, FK `SET_NULL` |
| `data/AppDatabase.kt` | Versión 10→11, `MIGRATION_10_11` |
| `data/dao/ExpenseDao.kt` | `getNonCardExpensesInPeriod`, `getTotalNonCardSpentInPeriod` |
| `data/repository/CreditCardRepository.kt` | Expone queries + log diferenciado |
| `ui/expenses/ExpensesViewModel.kt` | `saveExpense(cardId: Int?, paymentMethod)`, `clearCurrentCard()` |
| `ui/expenses/AddExpenseScreen.kt` | `cardId: Int?`, `PaymentMethodSelector`, lógica condicional |
| `ui/dashboard/DashboardViewModel.kt` | `loadNonCardSpending()`, `totalNonCardSpent` en UiState |
| `ui/dashboard/DashboardScreen.kt` | `SalaryUsageCard` con `totalNonCardSpent`, `BottomActionBar` con "Personal" |
| `ui/navigation/Navigation.kt` | Ruta `add_personal_expense`, `onAddPersonalExpense` en Dashboard |
