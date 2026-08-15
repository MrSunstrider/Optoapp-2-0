# Proposal: Fix Inventory Double Stock Writer

## Intent

Finanzas upload is a **second writer** of sale stock movements. The local dispensación save
already decrements Room stock and inserts `SALIDA_VENTA`; the sync coordinator then calls
`rpc_adjust_montura_stock` with `tipo='venta'` for the same sale, on a **full snapshot** every
cycle. Result: phantom ledger rows, transient remote double-decrements, and a
`23505 / idx_movimientos_conflict` flood that the app swallows in silence.

Collapse to **one writer**: local save owns stock, inventario sync owns transport.

## Evidence (production `sflhtihqdhrlryeyrzdo`, 2026-08-14)

| Probe | Value |
|-------|-------|
| `montura_movimientos tipo='SALIDA_VENTA'` (local writer) | 25 rows / 25 refs |
| `montura_movimientos tipo='venta'` (RPC writer) | 24 rows / 24 refs |
| `venta` rows duplicating a `SALIDA_VENTA` on `(referencia_id, montura_id, optica_id)` | **24 / 24** |
| `venta` rows without a matching `SALIDA_VENTA` | **0** |
| `venta` rows with `nota <> 'venta_dispensacion'` | **0** |
| Ópticas affected (all tenants) | 1 (Sersa) |
| `23505` per full sync on CLK-LX3 | 24 (48 in a 2-sync buffer) |
| Readers of `tipo='venta'` (SQL or Kotlin) | **none** — `rpc_analisis_mensual` reads `SALIDA_VENTA` only |

Phantom rows prove the double decrement persisted transiently: for montura `16f5f6f4`
`SALIDA_VENTA` walks 22→21→20→19→18 while `venta` rows record 19→18, 18→17, 17→16.
`monturas.stock_actual` is currently **18**, i.e. the local value won via the inventario
monturas upsert (LWW). Correct today, by luck, not by design.

## Scope

### In Scope
- Remove the sync-time stock RPC replay from `UploadSyncCoordinator.uploadDispensaciones`
- Delete the now-dead `buildAdjustStockParams` / `parseAdjustStock*` helpers and their tests
- Room 44→45: purge local phantom `venta` rows that duplicate a `SALIDA_VENTA`
- Remote migration: purge the same phantom rows; harden `rpc_adjust_montura_stock` with a
  tenant/role guard and true idempotency for any remaining or external caller
- Deterministic stock reconstruction ordering in `syncStockFromMovimientos`
- Strict TDD; GGA before remote apply; CLK-LX3 re-verification

### Out of Scope
- Dropping `idx_movimientos_conflict` — it is the only guard that stopped repeated decrements
- Rewriting `monturas.stock_actual` values (they already match the `SALIDA_VENTA` ledger)
- Dirty-row filtering for the finanzas snapshot (separate concern)
- Retiring `rpc_adjust_montura_stock` — `optoapp-web` shares this database and may call it

## Capabilities

### New Capabilities
- `inventario-stock`: single-writer rule for sale stock, movement ledger identity,
  idempotent + tenant-guarded stock RPC, deterministic reconstruction

### Modified Capabilities
- `sync`: finanzas upload transports rows only, never mutates inventory
- `sync-state-tracking`: inventory write failures must be recorded, never swallowed

## Approach

Client first, then data. Purging remote rows before the client stops writing would let the
old build recreate them (and re-decrement, since idempotency has nothing to match). Purging
local rows first would let download restore them from remote. So: ship the client fix +
local purge, install on CLK-LX3, then apply the remote purge, then verify both sides at zero.

## Causal Invariants

1. A sale's stock effect has exactly **one** writer: the local dispensación save.
2. Sync transports movement rows; it never derives or re-applies a stock delta.
3. `(referencia_id, tipo, montura_id)` identifies one movement fact; replay is a no-op, not an error.
4. `rpc_adjust_montura_stock` refuses callers outside the óptica and never double-applies.
5. Stock reconstruction from the ledger is deterministic.
6. An inventory write that fails is reported, never swallowed.

## Affected Areas

`domain/UploadSyncCoordinator.kt`; `data/montura/MonturaInventoryCoordinator.kt`;
`data/OptoDatabase.kt` + `OptoDatabaseMigrations.kt` (44→45); `supabase/migrations/*`;
`supabase/tests/*`; `UploadSyncCoordinatorTest`.

## Chained PR Forecast

PR-1 client single-writer + Room purge (Med); PR-2 remote purge + hardened RPC (Med);
PR-3 deterministic reconstruction (Low). Split any slice >400 authored lines.

`Decision needed before apply: No`
`Chained PRs recommended: Yes`
`400-line budget risk: Medium`
Strategy assumption: `auto-chain`.

## Risks

Stale client re-uploading purged rows (M → ship client first, verify after; only one known
device); external web caller relying on the RPC (M → harden instead of drop); Room
forward-only migration (M → scoped DELETE, compensating migration if needed); purge deleting
a legitimate `venta` row (L → 0 orphans measured, predicate requires a matching `SALIDA_VENTA`).

## Rollback Plan

Client: revert PR — reintroducing the replay only restores the old noise, no data loss.
Room 44→45: forward-only; deleted rows are reconstructible from the surviving `SALIDA_VENTA`.
Remote purge: irreversible by design; snapshot the 24 rows into the evidence file first.
RPC hardening: `CREATE OR REPLACE` — restore the prior body to revert.

## Dependencies

`fix-sync-financial-ledger` is on the same branch and already at Room 44. GGA before remote
apply. CLK-LX3 available over ADB with the production keystore.

## RDD Status

**Disabled/unmanaged** — principles only; no receipt authority or kill switch.

## Success Criteria

- [ ] Finanzas upload performs zero remote stock mutations
- [ ] Zero `23505 / idx_movimientos_conflict` in a full CLK-LX3 sync
- [ ] `tipo='venta'` rows: 0 locally and 0 remotely; `SALIDA_VENTA` count unchanged
- [ ] `monturas.stock_actual` unchanged by the purge
- [ ] Hardened RPC: replay returns ok/idempotent; foreign caller denied; negative stock refused
- [ ] Stock reconstruction deterministic under equal `fecha`
- [ ] Full unit suite green; GGA clean before apply

## Assumptions

Only Sersa is affected (measured across all tenants). `optoapp-web` is treated as a possible
RPC caller, so the function is hardened rather than removed.
