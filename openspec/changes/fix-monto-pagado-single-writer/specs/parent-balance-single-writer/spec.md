# Spec — parent-balance-single-writer

## REQ-1 — Trigger idempotente (disp + serv)
`trg_pagos_update_monto_pagado` MUST set parent balances to `COALESCE(SUM(pago_effect(tipo, monto)), 0)` for the affected `dispensacion_id` and/or `servicio_extra_id`. MUST NOT increment a pre-seeded cache.

### Scenario: upsert parent then insert matching Abono
- **GIVEN** dispensación `monto_pagado = 100` and servicio `a_cuenta = 100`
- **WHEN** an Abono 100 is inserted for each parent
- **THEN** both caches remain 100, not 200

### Scenario: origin move recomputes both parents
- **GIVEN** a pago linked to parent A
- **WHEN** it moves to parent B
- **THEN** A and B both equal their current SUM(pago_effect)

## REQ-2 — Cierre Pagado/Saldo from ledger when same-day cobros exist
Cierre venta cards and `saldoPendiente` MUST use `PagoEffect` sums of the day's pagos for that entity id when present; otherwise the entity cache (prior-day payments).

### Scenario: OT 4582 doubled cache
- **GIVEN** `montoPagado = 200`, one same-day Abono 100, total 170
- **WHEN** Cierre renders the venta card / saldoPendiente
- **THEN** Pagado = 100, Saldo = 70, saldoPendiente includes 70

## REQ-3 — Homogeneidad
Dispensaciones and servicios extra MUST share the same trigger policy and the same Cierre ledger preference. No OT-specific branches.
