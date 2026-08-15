# Delta for analisis-negocio

## ADDED Requirements

### Requirement: SQL Payment Aggregates Use PagoEffect

SQL paths that aggregate `public.pagos` for business analysis (`recalcular_resumen_diario` cobros, debtor paid totals, and any RPC summing pagos cash) MUST apply the same PagoEffect matrix as Kotlin. `cobros_monto_total` and equivalent paid sums MUST NOT be a raw `SUM(monto)` that ignores tipo sign rules. Anulado/Reclamada sales MUST remain excluded from active sales/debt as already required.

#### Scenario: resumen_diario cobros match PagoEffect

- GIVEN optica O on date D with Abono 100, Reverso 30, Reembolso 20, Anulación 10
- WHEN `recalcular_resumen_diario(O, D)` runs
- THEN `cobros_monto_total` MUST equal `50`
- AND Anulación MUST contribute `0`

#### Scenario: Kotlin↔SQL convergence for BI inputs

- GIVEN the shared PagoEffect golden fixture for optica O and month M
- WHEN Kotlin offline aggregation and SQL RPC/resumen paths compute cobros for that window
- THEN both MUST return identical nets

#### Scenario: Negative control — Anulado sale excluded from debt after reverse

- GIVEN a dispensacion/servicio set to Anulado with linked Reverso covering prior Abonos
- WHEN `rpc_deudores` (or equivalent debt listing) runs
- THEN that sale MUST NOT appear as positive outstanding debt
- AND other optics’ debtors MUST be unaffected

#### Scenario: Idempotent recalculo after ledger fix

- GIVEN `recalcular_resumen_diario` already ran once for (O, D)
- WHEN it runs again with unchanged pagos
- THEN exactly one resumen row MUST exist for (O, D)
- AND `cobros_monto_total` MUST remain the PagoEffect net
