# Delta for recomendaciones

## MODIFIED Requirements

### R5: Rule 3 — Liquidar Stock (MEDIA)

Fires when any item `diasSinVenta > stockEstancadoAlertaDias` (default 180). Priority: MEDIA.

**Input data change**: `diasSinVenta` values now come from correct inline computation in `rpc_analisis_mensual` (see analisis-negocio delta). Previously, low-stock items received hardcoded 999 days, causing false positives. Now only genuinely unsold products (real `diasSinVenta > threshold`) trigger this rule.

(Previously: `diasSinVenta` was hardcoded to 999 for all low-stock items, causing LIQUIDAR_STOCK to fire misleadingly for recently sold products.)

#### Scenario: Items exceed threshold (unchanged logic)

- GIVEN 2 monturas with real `diasSinVenta` of 210 and 200 days, threshold 180
- WHEN `evaluarLiquidarStock` evaluates `stockEstancado`
- THEN `Recomendacion(tipo=LIQUIDAR_STOCK, prioridad=MEDIA)` is returned listing those 2 items only

#### Scenario: Recently sold items no longer trigger

- GIVEN a montura with real `diasSinVenta` = 15 days (sold last month)
- WHEN `evaluarLiquidarStock` evaluates `stockEstancado`
- THEN no LIQUIDAR_STOCK recommendation is produced for that item

#### Scenario: No stagnant items (unchanged)

- GIVEN empty `stockEstancado`
- WHEN `evaluarLiquidarStock` evaluates `stockEstancado`
- THEN null
