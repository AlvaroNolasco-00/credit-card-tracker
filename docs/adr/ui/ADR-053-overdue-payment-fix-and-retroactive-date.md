# ADR-053: Bugfix pago vencido + fecha retroactiva

**Fecha:** 2026-04-25
**Estado:** Aceptado
**Categoría:** ui, data
**Prioridad:** High
**Afecta:** DateUtils, DashboardViewModel, DashboardScreen, PayBalanceCard

---

## Contexto

El usuario reportó un bug crítico: **cuando una tarjeta ya ha vencido su pago, no se puede registrar el pago**. Esto ocurre cuando la fecha actual está entre el vencimiento del pago y el día de corte del mes siguiente.

**Ejemplo concreto:**
- Corte el 5, pago vencido el 10.
- Hoy es **3 de enero**.
- El ciclo anterior (5 dic – 4 ene) ya cerró y su pago está vencido.
- Pero la app oculta el botón "Pagar" porque `hasCutOffPassedThisMonth(5)` retorna `false` (el 5 de ene aún no llega).
- Consecuencia: el usuario no puede registrar un pago atrasado ni marcar que ya pagó tarde con intereses.

Además, el usuario solicita poder **elegir la fecha real del pago** para casos donde olvidó registrarlo o pagó tarde.

**Restricciones:**
- `DateUtils.getDaysOverduePayment()` tiene una guarda prematura que retorna `0` cuando `today < cutOffDay`.
- `getPreviousPeriodRange()` calculaba mal el período anterior cuando `today < cutOffDay`.
- No hay selector de fecha en el diálogo de pago.

---

## Decisión

### Opción elegida

Implementar dos cambios coordinados:

1. **Corregir lógica de fechas en `DateUtils`:**
   - `getPreviousPeriodRange()`: derivar el fin del período anterior desde el **inicio del período actual** (vía `getCurrentPeriodRange()`), en lugar de calcularlo directamente desde `today.withDayOfMonth(cutOffDay)`.
   - `getDaysOverduePayment()`: eliminar la guarda `if (!hasCutOffPassedThisMonth(cutOffDay)) return 0`. Dejar que `getPaymentDueDateForCurrentCycle()` resuelva el vencimiento real en todos los casos.
   - Agregar overloads que acepten `today: LocalDate` para testabilidad.

2. **Reestructurar `DashboardViewModel.loadDashboard()`:**
   - Siempre calcular `prevStart/prevEnd`, `isPaid`, cargar `prevFlow` y `currentFlow` para **todas** las tarjetas.
   - `cutOffHappenedThisMonth` se mantiene basado en `hasCutOffPassedThisMonth()` **solo** para el split visual de la tarjeta (ADR-021), pero ya no bloquea el cálculo de saldo vencido.
   - `payBalance()` y `payPartial()` aceptan `paymentDate: Long` (default `System.currentTimeMillis()`).

3. **Actualizar `DashboardScreen`:**
   - Condición del `PayBalanceCard`: cambiar de `cutOffHappenedThisMonth && !isPaidThisCycle && saldo > 0` a `!isPaidThisCycle && saldo > 0`.
   - `PayBalanceCard`: agregar `DatePickerDialog` de Material3 para elegir la fecha del pago, con restricción de fechas pasadas/hoy.
   - Mostrar la fecha seleccionada en formato `dd/MM/yyyy` dentro del diálogo.

4. **Tests unitarios:**
   - Crear `DateUtilsTest.kt` con casos de borde para todas las funciones de período y vencimiento.

### Por qué esta opción

- ✅ **Bugfix mínimo pero completo:** corrige la raíz del problema (cálculo de períodos) sin cambiar la semántica de la UI.
- ✅ **Fecha retroactiva simple:** `DatePickerDialog` nativo de Material3, restringido a fechas pasadas/hoy.
- ✅ **No invasivo:** `paymentDate` tiene default parameter, no rompe llamadas existentes.
- ✅ **Testable:** overloads con `LocalDate` permiten TDD sin mockar `LocalDate.now()`.
- ✅ **Sin migración de DB:** no requiere cambios en entidades ni esquemas.

### Opciones rechazadas

**Opción A: Crear entidad `PaymentRecord` con historial completo**
- ❌ Requeriría migración de DB, DAO, repository, pantallas nuevas.
- ❌ Scope mucho mayor que el bug reportado.

**Opción B: Agregar campo `lateFeeAmount` (intereses de mora)**
- ❌ El usuario pidió dejar esta opción para después.
- ❌ Requeriría migración de DB y lógica adicional.

**Opción C: Cambiar `hasCutOffPassedThisMonth` para que retorne `true` cuando hay saldo vencido**
- ❌ Acoplaría lógica de UI con lógica de negocio.
- ❌ Riesgo de side-effects en otras pantallas.

---

## Consecuencias

### Directas

- ✅ El botón **Pagar** aparece siempre que haya saldo pendiente de un ciclo cerrado, incluso antes del día de corte del mes actual.
- ✅ El banner rojo de "Pago vencido" y el `InfoChip` se muestran correctamente en todos los escenarios.
- ✅ El usuario puede elegir la fecha real del pago (útil para pagos olvidados o tardíos).
- ✅ `lastPaymentDate` refleja la fecha elegida, no `System.currentTimeMillis()`.

### Técnicas

**Archivos/módulos impactados:**
- `util/DateUtils.kt` — Fix `getPreviousPeriodRange`, `getDaysOverduePayment`; overloads testables.
- `ui/dashboard/DashboardViewModel.kt` — Siempre calcular prev period; `paymentDate` en `payBalance/payPartial`.
- `ui/dashboard/DashboardScreen.kt` — Condición de visibilidad de `PayBalanceCard`; `DatePickerDialog` en diálogo.
- `app/src/test/.../util/DateUtilsTest.kt` — Tests unitarios nuevos.

**Breaking changes:**
- Ninguno. `paymentDate` tiene valor por defecto. Las funciones públicas de `DateUtils` conservan sus signatures originales.

### Operacionales

- **Testing requerido:**
  - Unit test: `DateUtilsTest` pasa (44 tests totales, 0 fallos).
  - Manual: crear tarjeta con corte vencido y verificar que el botón Pagar aparece.
  - Manual: registrar pago con fecha retroactiva y verificar que `lastPaymentDate` es correcta.

- **Documentación:** actualizar CHANGELOG.md con bugfix y feature.

---

## Implementación

### Paso a paso

1. ✅ Agregar overloads testables a `DateUtils` (`today: LocalDate` parameter).
2. ✅ Corregir `getPreviousPeriodRange`: usar `getCurrentPeriodRange` como base.
3. ✅ Corregir `getDaysOverduePayment`: eliminar guarda prematura.
4. ✅ Crear `DateUtilsTest.kt` con TDD (fallan primero, luego pasan).
5. ✅ Reestructurar `DashboardViewModel.loadDashboard()`: siempre calcular prev period.
6. ✅ Agregar `paymentDate` a `payBalance()` y `payPartial()`.
7. ✅ Actualizar `DashboardScreen`: visibilidad de `PayBalanceCard` + `DatePickerDialog`.
8. ✅ Validar build: `./gradlew test` pasa.

### Files de referencia

- `DateUtils.kt` — Corrección de cálculo de períodos y vencimiento.
- `DashboardViewModel.kt` — Lógica de estado y pagos.
- `DashboardScreen.kt` — UI del diálogo de pago con DatePicker.
- `DateUtilsTest.kt` — Tests unitarios.

---

## Validación

### Cómo verificar que la decisión se implementó correctamente

- [ ] Crear tarjeta con cutOffDay = hace 10 días, paymentDueDay = hace 2 días.
- [ ] Hoy = 3 del mes (antes del cutOffDay del mes actual).
- [ ] Dashboard muestra banner rojo + InfoChip "Vencido hace N día(s)".
- [ ] `PayBalanceCard` aparece y permite registrar pago.
- [ ] Al tocar "Pagar", el diálogo muestra selector de fecha.
- [ ] Elegir fecha retroactiva (ej. hace 5 días) y confirmar.
- [ ] `lastPaymentDate` guarda la fecha elegida, no `System.currentTimeMillis()`.
- [ ] Marcar pago → banner/InfoChip/alarmas desaparecen, UI se actualiza.
- [ ] `DateUtilsTest` pasa: 44 tests, 0 fallos.

### Métricas de éxito

- Usuarios con pago vencido pueden registrar el pago sin importar el día del mes.
- Fecha de pago retroactiva permite correcciones precisas.
- Sin regresiones en tests existentes.

---

## Notas y Aprendizajes

- **Cálculo de período anterior:** nunca derivar el fin del período anterior directamente desde `today.withDayOfMonth(cutOffDay)`. Siempre usar el inicio del período actual como ancla.
- **Guardas prematuras:** `getDaysOverduePayment` tenía una guarda que parecía "defensiva" pero enmascaraba el bug real. Es mejor dejar que la lógica de fechas resuelva el caso general.
- **DatePicker en Compose:** `rememberDatePickerState` + `SelectableDates` permite restringir fechas sin librerías externas. Requiere `@OptIn(ExperimentalMaterial3Api::class)`.
- **Default parameters:** usar `paymentDate: Long = System.currentTimeMillis()` permite agregar funcionalidad sin romper llamadas existentes.

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-25 | Documento inicial |

---

## Referencias

- [ADR-021](ui/ADR-021-split-balance-post-cutoff.md) — División de saldo post-corte (infraestructura previa).
- [ADR-022](ui/ADR-022-pay-balance-button.md) — Botón "Pagar Saldo".
- [ADR-049](ui/ADR-049-overdue-payment-alerts.md) — Alertas de pago vencido.
- [CHANGELOG.md](../../CHANGELOG.md) — Entrada de release.
