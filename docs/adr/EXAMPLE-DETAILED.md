# Ejemplo Detallado: ADR completo y bien documentado

Este documento es un **ejemplo de referencia** que muestra cómo debería verse un ADR de **máxima calidad** en nuestro proyecto.

---

# ADR-029: Presupuesto mensual por categoría con comparación visual

**Fecha:** 2026-03-31  
**Estado:** Aceptado  
**Categoría:** ui  
**Prioridad:** High  
**Afecta:** Data layer (nuevas entidades), Presentation (nueva pantalla), Widget (actualización de datos)

---

## Contexto

### El problema
Usuarios de la app reportaban **no tener forma clara de controlar si sus gastos reales** están dentro de límites saludables. Actualmente:
- No hay noción de "presupuesto" en la app
- Imposible comparar gasto esperado vs real
- Usuarios con hábitos de gasto caótico no tienen manera de disciplinarse

### Conversaciones previas
Se investigaron tres enfoques:
1. **Presupuesto global único** (1 valor para toda la app) — Rechazado: demasiado simplista
2. **Presupuesto por tarjeta** — Rechazado: usuarios tienen múltiples tarjetas, necesitan límite por categoría
3. **Presupuesto por categoría** — Aceptado: máxima flexibilidad y control

### Restricciones
- Presupuesto debe ser **configurable por usuario** sin código
- Presupuesto debe persistir en **Room database** para continuidad
- Interfaz debe ser **simple**: máx 3 interacciones para configurar límite
- Performance: cálculo de "vs presupuesto" debe ser **<100ms** en dashboard

### Alternativas consideradas

**A) Presupuesto semanal**
- ❌ Demasiado granular, usuarios no piensan semanalmente
- ❌ Requeriría refactorizar lógica de cortes

**B) Presupuesto trimestral**
- ❌ Período demasiado largo, no hay feedback frecuente
- ❌ Impacto en UI para periodos variables

**C) Presupuesto mensual + record histórico**
- ✅ Elegido: granularidad ideal, alígna con ciclos de tarjetas
- Permite futura analytics de tendencias mes a mes

---

## Decisión

### Opción elegida: Presupuesto mensual con resetting automático

Implementamos un sistema donde:
1. **Entidad `BudgetProfile`** (nueva): almacena límite de gasto por categoría
2. **Resetting automático**: presupuesto se resetea cada 1º del mes a las 00:00
3. **Comparación visual**: dashboard muestra barra de progreso + % de presupuesto usado
4. **Recordatorio único**: diálogo emergente 1x por mes si usuario excede presupuesto

### Estructura de datos
```kotlin
@Entity(tableName = "budget_profiles")
data class BudgetProfile(
    @PrimaryKey
    val categoryId: Long,
    
    val monthlyLimit: Double,
    val isActive: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val lastResetDate: Long  // Fecha del último reset (para verificar si es nuevo mes)
)
```

### Por qué esta opción
- ✅ **Alineación con facturas:** El mes es el ciclo natural de las tarjetas de crédito
- ✅ **Simplicidad:** Usuario entiende "gasto máximo por mes"
- ✅ **Escalabilidad:** Podríamos agregar presupuesto trimestral/anual sin romper base de datos
- ✅ **Performance:** Cálculo O(n) donde n = número de categorías (~5) = <1ms
- ⚠️ **Trade-off:** No hay presupuesto semanal (si es crítico futuro, ADR nueva)
- ⚠️ **Trade-off:** Resetting automático = complejidad con timezone (usa UTC)

### Opciones rechazadas

**Opción A: Sin presupuesto (solo tracking)**
- ❌ No responde a la demanda del usuario: "¿cómo sé si estoy gastando mucho?"
- ❌ No diferencia entre usuarios disciplinados y descontrolados

**Opción B: Presupuesto global único**
- ❌ Ignora que usuarios gastan diferente por categoría
- ❌ Usuario con $1000 de comida pero $50 de medicina vería falsa alarma

**Opción C: Presupuesto por tarjeta**
- ❌ Usuarios pueden tener múltiples tarjetas, presupuesto es por categoría
- ❌ La tarjeta no es la unidad lógica aquí

---

## Consecuencias

### Directo (usuario visible)

**Beneficios:**
- ✅ Usuario puede configurar límites mensuales por categoría (Comida, Transporte, etc)
- ✅ Dashboard muestra barra visual: "Has usado el 75% de presupuesto en Comida"
- ✅ Recordatorio amistoso si excedes (1x por mes máximo)
- ✅ Funciona automáticamente sin que usuario haga nada (resetting)

**Limitaciones:**
- ⚠️ Solo presupuesto mensual (no semanal/trimestral por ahora)
- ⚠️ Resetting está en UTC, puede no alinear 100% con zona horaria del usuario (future fix: ADR-NNN)

### Técnicas

**Nuevos archivos:**
```
app/src/main/java/com/alvaronolasco/creditcardtracker/
├── data/entity/BudgetProfile.kt                    # NEW: Entidad
├── data/dao/BudgetProfileDao.kt                    # NEW: DAO con queries
├── data/repository/BudgetRepository.kt             # NEW: Business logic
├── ui/budget/BudgetScreen.kt                       # NEW: Pantalla de config
├── ui/budget/BudgetViewModel.kt                    # NEW: ViewModel
└── ui/dashboard/BudgetReminderDialog.kt            # NEW: Diálogo de recordatorio
```

**Archivos modificados:**
```
├── data/database/AppDatabase.kt                    # +BudgetProfile entity
├── data/database/MIGRATION_8_9.kt                  # NEW: Migration SQL
├── data/repository/CreditCardRepository.kt         # +getBudgetForCategory()
├── di/AppModule.kt                                 # +provideBudgetRepository()
├── ui/dashboard/DashboardScreen.kt                 # +BudgetCard composable
└── ui/dashboard/DashboardViewModel.kt              # +collectBudgetStatus()
```

**Base de datos:**
```sql
-- MIGRATION_8_9.kt
CREATE TABLE budget_profiles (
    categoryId INTEGER PRIMARY KEY,
    monthlyLimit REAL NOT NULL,
    isActive INTEGER NOT NULL DEFAULT 1,
    createdAt INTEGER NOT NULL,
    lastResetDate INTEGER NOT NULL,
    FOREIGN KEY (categoryId) REFERENCES categories(id)
);

CREATE INDEX idx_budget_active ON budget_profiles(isActive, lastResetDate);
```

**Breaking changes:**
- ❌ Ninguno: BudgetProfile es nueva, no afecta estructuras existentes

**Dependencias nuevas:**
- ❌ Ninguna: no requiere librerías externas

### Operacionales

**Testing requerido:**
- Unit: `BudgetRepositoryTest` — verificar cálculo de % y resetting
- UI: `BudgetScreenTest` — flujo de creación de presupuesto
- Integration: `DashboardBudgetTest` — reminder dispara 1x por mes máximo
- Device: Manual en emulador con reloj simulado (verificar resetting a las 00:00)

**Documentación:**
- ✅ Este ADR documenta por qué/cómo
- ✅ Comentarios inline en BudgetRepository para lógica de resetting
- ✅ CLAUDE.md: agregar "BudgetProfile" a lista de entidades

**Comunicación:**
- ✅ Feature note: "Presupuesto mensual por categoría" para release notes
- ✅ Usuarios nuevos verán onboarding de presupuesto inicial
- ✅ Usuarios existentes: presupuesto opcional (no forzado)

---

## Implementación

### Paso a paso (en orden de dependencias)

**1. Database + Entity**
```kotlin
// data/entity/BudgetProfile.kt
@Entity(tableName = "budget_profiles")
data class BudgetProfile(
    @PrimaryKey
    val categoryId: Long,
    val monthlyLimit: Double,
    val isActive: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val lastResetDate: Long
)
```

**2. DAO + Queries**
```kotlin
// data/dao/BudgetProfileDao.kt
@Dao
interface BudgetProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: BudgetProfile)
    
    @Query("SELECT * FROM budget_profiles WHERE isActive = 1")
    fun getActiveBudgets(): Flow<List<BudgetProfile>>
    
    @Query("SELECT * FROM budget_profiles WHERE categoryId = :categoryId")
    suspend fun getBudgetForCategory(categoryId: Long): BudgetProfile?
}
```

**3. Repository + Business Logic**
```kotlin
// data/repository/BudgetRepository.kt
@Singleton
class BudgetRepository(private val dao: BudgetProfileDao) {
    fun getActiveBudgets() = dao.getActiveBudgets()
    
    suspend fun getBudgetStatus(categoryId: Long, spentThisMonth: Double): BudgetStatus {
        val budget = dao.getBudgetForCategory(categoryId) ?: return BudgetStatus.NoLimit
        val percentage = (spentThisMonth / budget.monthlyLimit * 100).toInt()
        return BudgetStatus.Limited(percentage, budget.monthlyLimit)
    }
    
    suspend fun shouldResetBudgets(): Boolean {
        // Lógica: si cambió el mes UTC desde lastResetDate
        val now = System.currentTimeMillis()
        val lastReset = dao.getActiveBudgets().first().lastResetDate
        return getDayOfMonth(now) < getDayOfMonth(lastReset)
    }
}
```

**4. UI + ViewModel**
```kotlin
// ui/budget/BudgetViewModel.kt
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {
    val budgetStatus = budgetRepository.getActiveBudgets()
        .map { it.associate { budget -> 
            budget.categoryId to budget.monthlyLimit 
        }}
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
}
```

**5. Screens**
- `BudgetScreen.kt` — Crear/editar presupuestos
- `BudgetReminderDialog.kt` — Diálogo de reminder si excede
- Dashboard integración: mostrar barra en tarjeta por categoría

**6. Migration**
```kotlin
// MIGRATION_8_9
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE budget_profiles (
                categoryId INTEGER PRIMARY KEY,
                monthlyLimit REAL NOT NULL,
                ...
            )
        """)
    }
}
```

### Files de referencia

**PR:** [#115](https://github.com/.../pull/115) — Presupuesto mensual  
**Commits principales:**
- `a1b2c3d` — Add BudgetProfile entity + migration
- `e4f5g6h` — Implement BudgetRepository
- `i7j8k9l` — Add BudgetScreen UI
- `m0n1o2p` — Integration + tests

**Tests:**
- `app/src/test/.../data/repository/BudgetRepositoryTest.kt`
- `app/src/test/.../ui/budget/BudgetViewModelTest.kt`
- `app/src/androidTest/.../ui/budget/BudgetScreenTest.kt`

---

## Validación

### Cómo verificar que la decisión se implementó correctamente

**Unit tests:**
```bash
./gradlew test --tests "*BudgetRepositoryTest*"
./gradlew test --tests "*BudgetViewModelTest*"
```

Esperado: ✅ Todos pasan

**Device/Emulator manual:**
- [ ] Abro Dashboard → veo sección "Presupuesto"
- [ ] Hago clic "Editar presupuesto" → veo lista de categorías
- [ ] Defino límite: $100 en Comida
- [ ] Agrego gasto de $75 → dashboard muestra "75% de presupuesto"
- [ ] Agrego gasto de $50 más ($125 total) → dashboard muestra "125% de presupuesto" + badge roja
- [ ] Cambio la hora a siguiente mes (emulador) → presupuesto auto-resetea
- [ ] Cierro/reabre la app → persistencia OK

**Métricas de éxito:**
- ✅ Dashboard carga con presupuesto en <100ms
- ✅ Barra de progreso es suave (no lag)
- ✅ Resetting ocurre a las 00:00 UTC
- ✅ 0 crashes con 10+ categorías configuradas

---

## Notas y Aprendizajes

**Aprendizaje 1:** Resetting con UTC es simple pero puede confundir usuarios en otras timezones
- **Futuro:** Considerar resetting basado en timezone del dispositivo (ADR futura)

**Aprendizaje 2:** El cálculo de "% de presupuesto" debe ser rápido — la computación lazy en Jetpack Compose es crucial
- **Consejo:** Usar `Flow<BudgetStatus>.stateIn()` para evitar recálculos innecesarios

**Aprendizaje 3:** Recordatorio "excedes presupuesto" no debe ser spam
- **Consejo:** Guardar `lastReminderShownDate` para mostrar máximo 1x por mes por categoría

**No hagas:**
- ❌ No confundas presupuesto (límite planeado) con balance (deuda actual)
- ❌ No muestres presupuesto para tarjeta, siempre es por categoría
- ❌ No resetees a medianoche local — lío de timezones, usa UTC

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-03-31 | Documento inicial |
| 2026-04-05 | Feedback de PR #115: corregir resetting timezone (UTC confirmed) |

---

## Referencias

**Decisiones relacionadas:**
- [ADR-031](ui/ADR-031-budget-reminder-dialog.md) — Recordatorio de presupuesto (extensión de esta decisión)
- [ADR-016](data/ADR-016-msi-end-date.md) — Manejo de dates en expense (patrón similar)

**Recursos externos:**
- [Room Database Migrations](https://developer.android.com/reference/androidx/room/migration/Migration)
- [Jetpack Compose Performance Best Practices](https://developer.android.com/codelabs/jetpack-compose-performance)
- [StateFlow for state management](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/)

---

## Conclusión

Este ADR documenta una decisión de **alto impacto**, que:
- Introduce nueva entidad + capa de lógica
- Afecta UI (dashboard), data (presupuesto), y flujo del usuario
- Fue bien pensada (contexto claro, alternativas evaluadas, consecuencias listadas)
- Puede ser entendida por cualquiera que lea este documento en 6 meses

**Calidad de documentación:** ⭐⭐⭐⭐⭐

Todos los futuros ADRs deben aspirar a este nivel de detalles y claridad.
