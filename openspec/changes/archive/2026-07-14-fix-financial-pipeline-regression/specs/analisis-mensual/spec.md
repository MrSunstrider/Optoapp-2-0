# Delta for analisis-mensual

## MODIFIED Requirements

### R23: Supabase RPC `rpc_analisis_mensual`

MODIFIED: Restore 4 missing dashboard fields (`margen_por_categoria`, `deudores`, `proyeccion_caja`, `stock_estancado`, `valor_inventario`) that were lost in the July 13 regression. Preserve `meses_historicos` (correctly added by July 13). Data sources for restored fields SHALL NOT reference `public.ventas`.

#### R23.1: Output Fields

The function SHALL return a JSONB object with all 16 keys listed below. Core indicators (fields 1–11) are UNCHANGED from existing spec. Fields 12–16 are RESTORED:

| # | Key | Source | Status |
|---|-----|--------|--------|
| 1–11 | `ventas_mes` through `meses_historicos` | `resumen_diario`, `gastos_operativos` | UNCHANGED |
| 12 | `margen_por_categoria` | `categorias_producto` LEFT JOIN `margen_por_categoria` | RESTORED |
| 13 | `deudores` | `rpc_deudores(optica_id)` sub-call | RESTORED |
| 14 | `proyeccion_caja` | UNION ALL `dispensaciones` + `servicios_extra` (no `ventas`) | RESTORED |
| 15 | `stock_estancado` | `monturas` with ventilation data | RESTORED |
| 16 | `valor_inventario` | `monturas` SUM(costo * stock_actual) | RESTORED |

(Previously: R23 returned 11 fields — fields 12–16 were absent due to July 13 regression.)

#### Scenario: Full 16-field response

- GIVEN an optica with dispensaciones, servicios_extra, resumen_diario, and gastos for July 2026
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN the JSONB response contains all 16 keys listed above
- AND each key has a non-null value (numeric zero or empty array where no data)
- AND `meses_historicos` equals `COUNT(DISTINCT DATE_TRUNC('month', resumen_diario.fecha))` for the optica

#### Scenario: Empty month returns zeros for restored fields

- GIVEN an optica with zero data for July 2026
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN `margen_por_categoria` is an empty JSON array
- AND `deudores` is an empty JSON array
- AND `proyeccion_caja` returns zero cash flow projection values
- AND `stock_estancado` returns all monturas with `dias_sin_venta = 999`

### R26: Deprecate `rpc_resumen_financiero` and `rpc_saldo_pendiente`

MODIFIED: `rpc_saldo_pendiente` SHALL be dropped entirely (was deprecated, now removed). `rpc_resumen_financiero` SHALL remain deprecated with `COMMENT ON FUNCTION` as before.

(Previously: Both functions received deprecation comments but were kept callable.)

#### Scenario: rpc_saldo_pendiente no longer exists

- GIVEN the fix migration has been applied
- WHEN querying `information_schema.routines` for `rpc_saldo_pendiente`
- THEN zero rows are returned

#### Scenario: rpc_resumen_financiero still callable

- GIVEN the fix migration has been applied
- WHEN `rpc_resumen_financiero(...)` is called by existing callers
- THEN the function executes normally (backward compatibility preserved)
