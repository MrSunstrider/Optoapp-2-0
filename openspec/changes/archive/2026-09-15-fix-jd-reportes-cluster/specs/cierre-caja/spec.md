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

### Requirement: Exclusive rpc_cierre_caja_resumen date upper bound

`public.rpc_cierre_caja_resumen` MUST keep the half-open interval `[p_from, p_to)` (`fecha >= p_from AND fecha < p_to`). This matches optoweb callers that pass `toExclusive` / `endExclusive`. Same calendar day MUST use `p_from = D` and `p_to = D + 1`. A documentation migration MAY only `COMMENT ON FUNCTION`; already-applied SQL history MUST NOT be rewritten to inclusive semantics.

#### Scenario: Same-day range uses exclusive end

- GIVEN pagos exist for optica `o1` on date `D`
- WHEN `rpc_cierre_caja_resumen('o1', D, D + 1)` is called
- THEN the result MUST include those pagos for day `D`
- AND `rpc_cierre_caja_resumen('o1', D, D)` MUST return zero pagos for that filter

#### Scenario: Multi-day half-open end

- GIVEN pagos on `D` and on `D+1`
- WHEN `rpc_cierre_caja_resumen('o1', D, D+1)` is called
- THEN only pagos from day `D` MUST be included (upper bound exclusive)
