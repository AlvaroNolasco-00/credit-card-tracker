# ADR-049: Alertas visuales y notificaciones de pago vencido

**Fecha:** 2026-04-17  
**Estado:** Aceptado  
**Categoría:** ui, notifications  
**Prioridad:** High  
**Afecta:** DashboardScreen, ReminderScheduler, ReminderReceiver, NotificationHelper, DateUtils  

---

## Contexto

Actualmente, `DateUtils.getDaysUntil()` retorna solo valores positivos (si la fecha ya pasó, salta al mes siguiente). Esto causa que las tarjetas con **pago vencido** (fecha de pago ya transcurrida sin registrar pago) se visualicen idénticamente a tarjetas al día.

**Problema:**
- Usuario olvida reportar pago → fecha de pago pasa → app muestra día +30 (próximo mes) sin alertar
- No hay diferenciación visual entre "pago vence en 2 días" vs "pago vencido hace 3 días"
- Sin notificación push cuando la fecha se vence

**Restricciones:**
- `DateUtils.getDaysUntil()` es usado en muchos lugares → no cambiar su comportamiento
- Necesita lógica de fechas robusta: paymentDueDay puede ser < cutOffDay (salta al mes siguiente)
- Alarms via `AlarmManager` requieren exactitud de tiempo para UX confiable

---

## Decisión

### Opción elegida

Implementar detección de pago vencido mediante:

1. **Nuevas funciones en DateUtils:**
   - `getPaymentDueDateForCurrentCycle(cutOffDay, paymentDueDay): LocalDate` — calcula la fecha de pago del ciclo actual
   - `getDaysOverduePayment(cutOffDay, paymentDueDay): Int` — retorna días vencidos (0 si no vencido)

2. **Estado en CardDashboardState:**
   - `isPaymentOverdue: Boolean` — indica si hay pago vencido
   - `daysOverdue: Int` — días transcurridos desde vencimiento

3. **Alarms para OVERDUE:**
   - `ReminderScheduler.scheduleOverdueAlarm(card)` — agenda 3 alarmas (día+1, día+4, día+7 tras payment due)
   - `ReminderReceiver` con `@AndroidEntryPoint` — verifica DB antes de notificar
   - Cancela alarms al registrar pago (vía `ReminderScheduler.cancelOverdueAlarm`)

4. **UI en Dashboard:**
   - Banner rojo compacto cuando `selectedCard.isPaymentOverdue`
   - `InfoChip` de pago: fondo rojo claro + ícono Error + texto "Vencido hace N día(s)"
   - `PayBalanceCard`: borde/ícono rojo, título "¡Pago vencido! Registra ahora"

5. **Notificación push:**
   - Badge "VENCIDO" en rojo (diferente de "CORTE"/"PAGO")
   - Mensaje: "No registraste tu pago. Si ya pagaste, ábrelo en la app y regístralo"
   - Reutiliza canal `reminder_channel` (IMPORTANCE_HIGH)

### Por qué esta opción

- ✅ **No invasiva:** extiende infraestructura existente sin modificar comportamiento de `getDaysUntil()`
- ✅ **Lógica robusta:** `getPaymentDueDateForCurrentCycle` maneja cambios de mes correctamente
- ✅ **Escalado suave:** 3 alarmas (día+1, +4, +7) vs una sola → reintento sin spam
- ✅ **Verificación en receiver:** si usuario marca pago manualmente entre alarmas, no notifica
- ✅ **Visual clara:** rojo diferenciado de verde (pago OK) y ámbar (pago próximo)
- ✅ **UX: Inmediato:** banner + InfoChip se actualizan al cargar dashboard

### Opciones rechazadas

**Opción A: Modificar `getDaysUntil()` para retornar negativos**
- ❌ Cambio global → afecta todos los lugares donde se usa (riesgo de regresiones)
- ❌ Requeriría refactorizar lógica en InfoChip, PayBalanceCard, etc.

**Opción B: Una sola alarma de OVERDUE a los 7 días post-payment-due**
- ❌ Usuarios que olvidan durante 7 días no reciben alertas previas
- ❌ Peor UX: notificación llega muy tarde

**Opción C: Notificaciones en-app solo (sin push)**
- ❌ Usuario no ve si app no está abierta
- ❌ Menores tasas de conversión para registro de pago

---

## Consecuencias

### Directas

- ✅ Usuario ve instantáneamente si su tarjeta tiene pago vencido (banner + InfoChip)
- ✅ Recibe 3 notificaciones push escalonadas (día+1, +4, +7) si no registra pago
- ✅ Al registrar pago, alarmas se cancelan y UI se actualiza
- ⚠️ Si pago se registra tarde, notificación de OVERDUE puede llegar tras ya haber pagado (pero receiver verifica DB, no notifica)
- ❌ 3 alarmas por tarjeta por ciclo → mayor uso de AlarmManager (mitigado: se cancela al pagar)

### Técnicas

**Archivos/módulos impactados:**
- `util/DateUtils.kt` — `getPaymentDueDateForCurrentCycle()`, `getDaysOverduePayment()`
- `ui/dashboard/DashboardViewModel.kt` — agrega `isPaymentOverdue`, `daysOverdue` a `CardDashboardState`; inyecta `ReminderScheduler`
- `ui/dashboard/DashboardScreen.kt` — agrega `OverduePaymentBanner`, modifica `CardInfoRow` (rama overdue), actualiza `PayBalanceCard`, extiende `InfoChip` con `containerColor`
- `notifications/ReminderScheduler.kt` — agrega `scheduleOverdueAlarm()`, `cancelOverdueAlarm()`, llama desde `scheduleReminders()`
- `notifications/ReminderReceiver.kt` — cambia a `@AndroidEntryPoint`, agrega check DB para OVERDUE
- `notifications/NotificationHelper.kt` — maneja display de tipo OVERDUE (badge rojo, mensaje específico)

**Breaking changes:**
- Ninguno. Cambios compatibles; solo se añaden campos a `CardDashboardState` con valores por defecto.

### Operacionales

- **Testing requerido:**
  - Manual: crear tarjeta con fecha de pago hace 2 días, verificar banner + InfoChip rojo
  - Manual: avanzar reloj del dispositivo → verificar disparo de alarmas
  - Manual: marcar pago → verificar que alarmas se cancelan y UI se actualiza
  - Unit test: `DateUtilsTest` para `getDaysOverduePayment` en distintos escenarios (future/past, cambio de mes)

- **Documentación:** actualizar CHANGELOG.md con nueva feature

- **Comunicación:** usuarios verán automáticamente alertas — no requiere onboarding

---

## Implementación

### Paso a paso

1. ✅ Agregar `getPaymentDueDateForCurrentCycle()` y `getDaysOverduePayment()` en `DateUtils.kt`
2. ✅ Extender `CardDashboardState` con `isPaymentOverdue`, `daysOverdue`
3. ✅ Calcular overdue en `DashboardViewModel.loadDashboard()` (rama cutHappened)
4. ✅ Inyectar `ReminderScheduler` en `DashboardViewModel`, cancelar alarms en `payBalance/payPartial`
5. ✅ Agregar `TYPE_OVERDUE`, `scheduleOverdueAlarm()`, `cancelOverdueAlarm()` en `ReminderScheduler`
6. ✅ Cambiar `ReminderReceiver` a `@AndroidEntryPoint`, agregar check DB para OVERDUE
7. ✅ Actualizar `NotificationHelper` para display de badge rojo + mensaje OVERDUE
8. ✅ Agregar `OverduePaymentBanner`, `containerColor` a `InfoChip`, rama overdue en `CardInfoRow`, actualizar `PayBalanceCard` en `DashboardScreen`
9. ⏳ Crear unit tests para `DateUtils.getDaysOverduePayment()`
10. ⏳ Actualizar `CHANGELOG.md` e `INDEX.md`

### Files de referencia

- Commits principales:
  - `DateUtils.kt` — Funciones de cálculo de overdue
  - `DashboardViewModel.kt` — Lógica de estado
  - `ReminderScheduler.kt` — Scheduling de alarmas OVERDUE
  - `ReminderReceiver.kt` — Verificación en tiempo de disparo
  - `DashboardScreen.kt` — UI visual

- Tests (pendientes):
  - `util/DateUtilsTest.kt` — Casos de prueba para `getDaysOverduePayment()`

---

## Validación

### Cómo verificar que la decisión se implementó correctamente

- [ ] Crear tarjeta con cutOffDay = hace 10 días, paymentDueDay = hace 2 días
- [ ] Dashboard muestra banner rojo + InfoChip "Vencido hace 2 día(s)"
- [ ] `PayBalanceCard` muestra borde rojo, ícono rojo, título urgencia
- [ ] Adelantar reloj del sistema → alarma dispara a los tiempos programados (día+1, +4, +7)
- [ ] Notificación muestra badge "VENCIDO" en rojo, cuerpo específico
- [ ] Marcar pago → banner/InfoChip/alarmas desaparecen, UI se actualiza
- [ ] Reboot del emulador → `BootReceiver` reschedule alarmas OVERDUE correctamente
- [ ] Sin crashes en logs

### Métricas de éxito

- Usuarios con pago vencido ven alerta inmediata en dashboard
- 3 notificaciones push llegan en días +1, +4, +7 (sin duplicados)
- Al pagar, UI y alarmas se actualizan en < 1 segundo
- Tasa de registro de pago vencido mejora post-implementación

---

## Notas y Aprendizajes

- **Cálculo de payment due:** si `paymentDueDay <= cutOffDay`, la fecha de pago es el mes siguiente al cutoff → no ignorar cambios de mes
- **Verificación en receiver:** no asumir que alarm dispara → siempre verificar DB, porque el usuario pudo haber pagado manualmente entre alarmas
- **Alarms escalonados:** 3 intentos (día+1, +4, +7) son suficientes sin saturar; evita fatiga de notificaciones
- **Cancellation en pago:** siempre cancelar alarms al registrar pago, aunque sea parcial que cierre saldo

---

## Historial de cambios

| Fecha | Cambio |
|-------|--------|
| 2026-04-17 | Documento inicial |

---

## Referencias

- [ADR-022](../ui/ADR-022-pay-balance-button.md) — Botón "Pagar Saldo" (infraestructura previa)
- [ADR-046](../ui/ADR-046-rich-notifications-card-thumbnail.md) — Notificaciones ricas (extiende este ADR)
- [CHANGELOG.md](../../CHANGELOG.md) — Entrada de release
