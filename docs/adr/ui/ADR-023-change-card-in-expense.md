# ADR-023: Selector de tarjeta destino en AddExpenseScreen

**Fecha:** 2026-03-29
**Estado:** Aceptado
**Categoría:** ui

## Contexto

`AddExpenseScreen` recibía `cardId` como parámetro fijo de navegación. Una vez en la pantalla no había manera de reasignar el gasto a otra tarjeta, ni al crear ni al editar. El usuario necesitaba poder cambiar la tarjeta destino desde la misma pantalla.

## Decisión

- Se añadió `allCards: List<CreditCard>` a `ExpensesUiState` y se carga reactivamente desde `repository.getAllCards()` en el `init` del ViewModel.
- Se introdujo `selectedCardId` como estado local en la pantalla, inicializado con el `cardId` del parámetro (o con el `cardId` del expense en modo edición).
- El `CardTargetBanner` existente se volvió tappable (`clickable`). Al tocarlo se muestra un `AlertDialog` con la lista de tarjetas disponibles, resaltando la seleccionada con su color.
- `saveExpense` ahora usa `selectedCardId` en lugar del `cardId` del parámetro de navegación.
- Se añadió un `LaunchedEffect(selectedCardId)` para recargar `currentCard` en el ViewModel cuando el usuario cambia la selección, actualizando el banner visualmente.

## Consecuencias

- No se requirió cambio de entidades ni de DAOs.
- El banner muestra el texto "Cambiar" como affordance visual.
- En modo edición, cambiar la tarjeta reasigna el expense a la nueva tarjeta al guardar.
