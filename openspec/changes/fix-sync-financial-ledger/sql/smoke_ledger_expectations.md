# SQL Smoke Expectations — Ledger `pago_effect` (WU-1)

Strict TDD RED→GREEN evidence for the DB-first ledger. RED = behavior on the
current schema (before the migration). GREEN = behavior expected after
`*_ledger_pago_effect.sql` is applied. Smoke runs are **read-only preflight**
on linked prod (`sflhtihqdhrlryeyrzdo`); write-path asserts run locally / in a
transaction rolled back — no production DML in this WU (GGA not yet run).

## Preflight baseline (linked prod, read-only) — captured 2026-08-14

| Metric | Value | Note |
|--------|-------|------|
| total_pagos | 579 | all `tipo=Abono` |
| by_metodo | Efectivo 430 / Transferencia 148 / Tarjeta 1 | in domain |
| monto_negativo | 0 | `pagos_monto_chk` holds |
| tipo_fuera_de_dominio | 0 | `chk_pagos_tipo` clean (still NOT VALID) |
| metodo_fuera_de_dominio | 0 | `chk_pagos_metodo` clean (still NOT VALID) |
| origen_xor_violado | 0 | every pago has exactly one origen |
| anulacion_negativa | 0 | no negative Anulación remotely |
| reverso_existentes | 0 | ledger types not yet in use |
| servicios_estado_ood | 0 | only Pendiente/Entregado present |
| dispensaciones_estado_ood | 0 | only Pendiente/Entregado present |

Inventories are clean, so the expanded domain CHECKs can be added `NOT VALID`
then `VALIDATE`d without rejecting any historical row.

## RED — current schema (expected FAIL / missing)

| # | Case | Current result | Why |
|---|------|----------------|-----|
| R1 | `SELECT public.pago_effect('Abono', 100)` | ERROR: function does not exist | effect fn not created yet |
| R2 | `UPDATE servicios_extra SET estado='Anulado'` | ERROR 23514 `servicios_extra_estado_domain_chk` | domain = {Pendiente,Entregado} only |
| R3 | `UPDATE dispensaciones SET estado_entrega='Anulado'` | ERROR 23514 `dispensaciones_estado_entrega_domain_chk` | domain = {Pendiente,Entregado} only |
| R4 | `UPDATE dispensaciones SET estado_entrega='Reclamada'` | ERROR 23514 `dispensaciones_estado_entrega_domain_chk` | Reclamada not in domain |
| R5 | `INSERT pagos(tipo='Reverso', reversa_pago_id=...)` | ERROR: column `reversa_pago_id` does not exist | column/FK/unique not created |

## GREEN — after migration (expected PASS)

| # | Case | Expected result |
|---|------|-----------------|
| G1 | `pago_effect('Abono',100)` / `('Pago completo',50)` | `100` / `50` |
| G2 | `pago_effect('Reembolso',40)` / `('Reverso',60)` | `-40` / `-60` |
| G3 | `pago_effect('Anulación',100)` / `('Cualquiera',10)` / trims ` Abono ` | `0` / `0` / `10` |
| G4 | `UPDATE servicios_extra SET estado='Anulado'` | accepted (no 23514) |
| G5 | `UPDATE dispensaciones SET estado_entrega IN ('Anulado','Reclamada')` | accepted (no 23514) |
| G6 | `UPDATE pagos SET monto = -1` | still FAILS `pagos_monto_chk` (negatives never allowed) |
| G7 | two Reverso rows with same `reversa_pago_id` | second FAILS partial UNIQUE `pagos_reversa_pago_id_uidx` |
| G8 | `Reverso` with NULL `reversa_pago_id`, or non-Reverso with non-NULL | FAILS XOR CHECK `chk_pagos_reversa_link` |
| G9 | shared fixture {Abono 100, Reverso 100, Reembolso 25, Anulación 50} | `SUM(pago_effect)` net = `-25` (matches Kotlin `PagoEffect`, WU-2) |

## WU-1B — daily/cash aggregate convergence (`20260815005859`)

Shared fixture F1 for one optica/date: `Abono 100 Efectivo`, `Reverso 40
Efectivo`, `Reembolso 10 Tarjeta`, `Anulación 999 Efectivo`.

| # | Case | RED (before mig) | GREEN (after mig) |
|---|------|------------------|-------------------|
| A1 | `SUM(pago_effect)` over F1 | n/a (raw `SUM(monto)` = 1149) | net `50` |
| A2 | F1 per method | Efectivo 1139 / Tarjeta 10 | Efectivo `60` / Tarjeta `−10` |
| B1 | `recalcular_resumen_diario` cobros | `SUM(monto)` ignores tipo sign | `SUM(public.pago_effect(tipo, monto))` → `cobros_monto_total = 50` |
| B2 | pending balance paid totals | `SUM(monto)` per venta | `SUM(pago_effect)` per venta |
| B3 | dispensaciones in ventas/debt | only `Anulado` excluded | `Anulado` **and** `Reclamada` excluded |
| B4 | servicios_extra in ventas/debt | `Anulado` excluded | unchanged (`Anulado` excluded) |
| C1 | `rpc_cierre_caja_resumen` 4 sums | `THEN monto ELSE 0` / `SUM(monto)` | all via `public.pago_effect(tipo, monto)`; keys `efectivo/movil_trans/tarjeta/total` unchanged |
| D1 | security contract | — | recalcular `SECURITY DEFINER` + `search_path=''`; cierre `SECURITY INVOKER` + `search_path=public`; membership/BI guards and `authenticated`/`service_role` grants preserved |

Executable form: `supabase/tests/test_ledger_aggregate_convergence.sql` (blocks
A–D). End-to-end invocation of both functions needs an authenticated JWT
context (membership + BI role guards), so it stays in the local `db reset`
harness / CLK-LX3 verification, not in this static run.

## WU-1C — remaining read-only RPCs (`rpc_deudores`, `rpc_analisis_mensual`)

Base body: `20260716045310` (prod matches: INVOKER, `search_path=public`,
membership+BI, no `pago_effect`, raw `SUM(monto)`/`SUM(pd.monto)`, no
Anulado/Reclamada exclusion). Fixture F1 net `50` reused.

| # | Case | RED | GREEN |
|---|------|-----|-------|
| E1 | `rpc_deudores` paid | `pg.monto` / `SUM(pd.monto)` | `pago_effect` → `efecto` / `SUM(pd.efecto)` |
| E2 | `rpc_deudores` ventas | no estado filter | exclude Anulado+Reclamada (disp) / Anulado (serv) |
| F1 | proyeccion_caja paid | `SUM(monto) AS total_pagado` | `SUM(efecto) AS total_pagado` |
| F2 | proyeccion ventas | no exclusion | same exclusions as deudores |
| G1 | contract | — | INVOKER + `search_path=public` + guards + 16 JSON keys + grants |

Executable: `test_ledger_aggregate_convergence.sql` blocks E–G. Live calls need JWT.

## WU-1D — GGA correction round 1 (constraints, trigger, preflight)

Extended preflight baseline (linked prod, read-only) — captured 2026-08-14:

| Metric | Value | Note |
|--------|-------|------|
| reembolso_existentes | 0 | reclaim refunds not yet in use |
| tipos_que_invierten_signo | 0 | no row flips sign under `pago_effect` |
| pago_optica_distinta_a_padre | 0 | tenant-consistent parentage holds, so the composite FK rejects nothing historical |
| **drift_dispensaciones** | **5** | **pre-existing**: stored `monto_pagado` ≠ `SUM(effect)` |
| **drift_servicios_extra** | **14** | **pre-existing**: stored `a_cuenta` ≠ `SUM(effect)` |

Drift is **not caused by this change** — the current prod trigger already adds
`+monto` for every non-Anulación pago, and all 579 rows are `Abono`, so effect
already equals monto today. In all 19 rows `stored = monto_total`, i.e. the
balance was set to "fully paid" directly by app/backfill writes rather than by
the pagos ledger: 7 rows have **zero** pagos, and 9 servicios have duplicate
pagos summing to 2× `monto_total`. Reports already read the ledger
(`monto_total − SUM(effect)`), so the two views of "paid" disagree today. See
GGA observation O5 — resync is a decision, not part of this transaction.

RED→GREEN for the correction round (H/I/B2/F2b assertions ship in WU-1E):

| # | Case | RED (as authored before correction) | GREEN (after correction) |
|---|------|-------------------------------------|--------------------------|
| H1 | composite FK target | `UNIQUE (id, optica_id)` absent | present, FK-addressable |
| H3 | reversa FK shape | `FOREIGN KEY (reversa_pago_id) → pagos(id)` — tenant-blind | `FOREIGN KEY (reversa_pago_id, optica_id) → pagos(id, optica_id)` |
| H7 | Reverso idempotency | global partial UNIQUE | unchanged, still global (per-optica would weaken it) |
| I2 | trigger origin change | absent — parent resolved by `COALESCE(NEW.x, OLD.x)` only | `IS DISTINCT FROM` branch on both parent columns |
| I3 | origin move balances | OLD parent stranded | OLD debited in full, NEW credited in full |
| B2 | raw-cash ban precision | `LIKE '%SUM(monto)%'` — misses `SUM(pd.monto)` / `SUM( monto )`, and widening it hits `SUM(monto_total)` | anchored regex + pattern self-test (B2a/B2b) + `SUM(monto_total)` survival assert (B2c) |
| F2b | non-cash exception | unscoped ban would false-positive | count-scoped: the only 2 bare `SUM(monto)` uses must be the `gastos_operativos` expense sums |

## WU-1F — executable write-path behavior (`test_ledger_write_path_rollback.sql`)

Runs in ONE transaction that ends in `ROLLBACK`; every fixture id is prefixed
`zzt_ledger_`; expected failures are caught in plpgsql `EXCEPTION` blocks
(implicit savepoints) so a rejection never aborts the run. Nothing is committed.

| # | Case | Expected |
|---|------|----------|
| W1 | INSERT effects | Abono +, Pago completo +, Reembolso −, Anulación 0 |
| W2 | UPDATE, same origin | net-delta fast path; tipo flip to Anulación drops the credit |
| W3 | DELETE | withdraws its own effect; deleting Anulación moves no cash |
| W4 | origin move D1→D2 | D1 → 0, D2 → full effect |
| W5 | origin move dispensación→servicio | old surface → 0, new surface += new effect |
| W6 | `monto = -50` | rejected by `pagos_monto_chk` |
| W7 | Reverso w/o target; non-Reverso w/ target | both rejected by `chk_pagos_reversa_link` |
| W8 | second Reverso, same original | rejected by `pagos_reversa_pago_id_uidx` |
| W9 | Reverso in optica B → original in optica A | rejected by composite `pagos_reversa_pago_id_fkey`, SQLSTATE 23503 |
| W10 | delete a reversed original | rejected by `ON DELETE RESTRICT` |
| W11 | Anulado / Reclamada | accepted (no 23514); pagos history kept, not deleted |
| W12 | Abono 80 + Reembolso 80 | parent nets 0, no reversa link needed |

**Not executed yet.** Requires the WU-1 migrations applied to a database.
Docker daemon is down (no local shadow DB) and remote apply is gated on GGA, so
W1–W12 have static/RED evidence only.
