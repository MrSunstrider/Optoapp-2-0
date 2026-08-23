# Delta for indicadores-negocio

## MODIFIED Requirements

### Requirement: ObtenerAnalisisMensualUseCase Offline Fallback

A Hilt UseCase SHALL exist. Online: calls `rpc_analisis_mensual`. Offline (IOException): falls back to `resumenDiarioDao.getByOpticaAndMonth()` + client-side SUM for ventas/cobros, sets `esOffline = true`, and SHALL compose COGS from `ventasCostoTotal` month sum and Gastos from local `gastos_operativos` for the month when those sources exist; otherwise leave zeros with offline flag. MUST NOT call or modify `PagoEffect`.

(Previously: offline indicators 5–8 returned 0/empty with offline flag only; margin/COGS/gastos composition skipped.)

#### Scenario: Online returns RPC data

- GIVEN device has network
- WHEN `invoke('o1', '2026-07-01')` is called
- THEN RPC is called and mapped to `AnalisisMensual`

#### Scenario: Offline composes COGS and gastos when data present

- GIVEN offline, resumen month ventas_costo_total sum=300, gastos month sum=100
- WHEN `invoke` falls back
- THEN `esOffline` is true AND composed COGS/gastos reflect those sums (or P&L consumers can read them)

#### Scenario: Offline without gastos still flags offline

- GIVEN offline and no gastos rows
- WHEN fallback runs
- THEN `esOffline` is true AND gastos remain 0 without crashing
