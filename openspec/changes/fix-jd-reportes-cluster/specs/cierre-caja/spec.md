# Delta for cierre-caja

## ADDED Requirements

### Requirement: Static user-facing load errors

When `CierreCajaViewModel` fails to load day data, the UI-facing `errorMessage` MUST be a fixed Spanish string that does not interpolate `e.message`, `e.localizedMessage`, or other raw exception text. The full exception MUST still be written via `Log.e` for diagnostics.

#### Scenario: Repository failure shows static message

- GIVEN the cierre load path throws (e.g. database failure with message `"DB corrupted"`)
- WHEN `CierreCajaViewModel` handles the failure
- THEN `uiState.errorMessage` MUST equal a static user-facing string
- AND `uiState.errorMessage` MUST NOT contain `"DB corrupted"` or the raw exception message
- AND `Log.e` MUST receive the original throwable (or its message) for diagnostics

### Requirement: Inclusive rpc_cierre_caja_resumen date upper bound

`public.rpc_cierre_caja_resumen` MUST treat both `p_from` and `p_to` as inclusive calendar dates. Pago (and related) filters MUST use `fecha >= p_from AND fecha <= p_to`. Same-day calls with `p_from = p_to` MUST include that day's rows when data exists. A new migration MUST `CREATE OR REPLACE` the function; already-applied history MUST NOT be edited. `COMMENT ON FUNCTION` MUST state that `p_to` is inclusive.

#### Scenario: Same-day range returns that day's pagos

- GIVEN pagos exist for optica `o1` on date `D`
- WHEN `rpc_cierre_caja_resumen('o1', D, D)` is called
- THEN the result MUST include those pagos for day `D`
- AND the result MUST NOT require callers to pass `p_to = D + 1`

#### Scenario: Multi-day inclusive end

- GIVEN pagos on `D` and on `D+1`
- WHEN `rpc_cierre_caja_resumen('o1', D, D+1)` is called
- THEN pagos from both `D` and `D+1` MUST be included
