# Architecture Decision Record — Income Card Color Change

## ID
ADR-028

## Título
Cambiar color de tarjeta de ingresos a celeste para mejor diferenciación visual

## Estado
Aceptado

## Fecha
2026-03-31

## Categoría
widget

## Contexto
La tarjeta de resumen de ingresos (`IncomeSummaryCard`) utilizaba un color verde (`#2E7D32` - Material Dark Green) que coincidía visualmente con las tarjetas de crédito verdes del usuario, causando confusión sobre cuál era el resumen de presupuesto y cuál era una tarjeta individual.

## Decisión
Cambiar el color de fondo de la tarjeta de ingresos de verde a celeste:
- **Light mode:** `#0277BD` (Material Light Blue 800)
- **Dark mode:** `#01579B` (Material Light Blue 900)

Este cambio se realiza actualizado `widget_income_brand` en `values/colors.xml` y `values-night/colors.xml`. El color celeste proporciona suficiente contraste visual para diferenciarse de cualquier tarjeta de crédito (rojo, amarillo, azul, verde, morado, naranja) manteniendo buena legibilidad del texto blanco encima.

## Consecuencias
- **Positivas:**
    - Mayor claridad visual: el usuario distingue inmediatamente entre la tarjeta de presupuesto y las tarjetas de crédito individuales.
    - Sin cambios de lógica ni performance — solo cambio de color en recursos.
    - Color celeste es distintivo pero neutral, no implica "bueno" ni "malo".
- **Negativas:**
    - Ninguna conocida. El cambio es puramente cosmético.
