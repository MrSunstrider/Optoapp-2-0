# Tasks: Fix Inventory Double Stock Writer

Budget: ≤ 400 authored lines per work unit (WU). Each WU is one reviewable commit.
Strict TDD: RED first, then GREEN. Client ships before the remote purge (see `design.md`).

## WU-1 — Client stops writing the second stock fact

- [x] 1.1 RED `InventoryStockSingleWriterTest` — assert `UploadSyncCoordinator` and its
      companion expose no `*AdjustStock*` member, and that no source line in
      `uploadDispensaciones` calls `rpc_adjust_montura_stock`.
- [x] 1.2 GREEN delete the post-chunk RPC loop, the `items` snapshot that only fed it, the
      four `*AdjustStock*` companion helpers, and the imports they alone required.
- [x] 1.3 Delete the 9 obsolete helper tests in `UploadSyncCoordinatorTest`.
- [x] 1.4 Gate: `:optoapp:testDebugUnitTest` green.

Status: GREEN — 44 authored lines (+/- net −78).

## WU-2 — Room 44→45 purges local phantoms

- [x] 2.1 RED `Migration44To45Test` — versions, `OptoDatabase.MIGRATION_44_45` registration,
      scoped DELETE, camelCase columns, `SALIDA_VENTA` correlation required.
- [x] 2.2 GREEN `MIGRATION_44_45`, `version = 45`, companion re-export, `addMigrations`.
- [x] 2.3 Gate: `:optoapp:testDebugUnitTest` green, schema `45.json` exported.

Status: GREEN — 61 authored lines.

## WU-3 — Deterministic stock reconstruction

- [x] 3.1 RED test: two movements, same `fecha`, differing `stockNuevo` → reconstruction must
      be stable across runs.
- [x] 3.2 GREEN order by `fecha`, then `updatedAt`, then `id` before taking the last.
- [x] 3.3 Gate: `:optoapp:testDebugUnitTest` green.

Status: GREEN — 38 authored lines.

## WU-4 — Remote purge + hardened RPC

- [x] 4.1 RED `supabase/tests/test_inventory_stock_single_writer.sql` — tenant guard,
      idempotent replay, insufficiency without mutation, 0 phantoms remaining,
      `SALIDA_VENTA` count and `stock_actual` unchanged, `idx_movimientos_conflict` retained.
      Verified RED against production with read-only definition assertions: all 6 failed.
- [x] 4.2 GREEN `20260815034308_inventory_single_writer_purge_and_guard.sql`
      (local filename aligned to remote MCP apply timestamp).
- [x] 4.3 GGA Round 1: both judges confirmed one CRITICAL (post-purge replay by an old build
      re-decrements stock) and one WARNING (SQL fixture fails opaquely on empty `auth.users`).
- [x] 4.4 RED for the CRITICAL, proven behaviourally against production in a rolled-back
      block: `stock 3 → 2`, phantom re-created.
- [x] 4.5 GREEN: sale aliases collapse in the identity lookup, lookup tenant-scoped, harden
      ordered before purge, fixture fails fast on empty `auth.users`. Same rolled-back probe
      now reports B1 forbidden, B2 4, B3 idempotent, B4 insufficient, B5 not_found,
      **B6 idempotent with `recorded_tipo=SALIDA_VENTA`, stock 3, phantom 0**, B7 AJUSTE applies.
- [x] 4.6 Gate: **GGA Round 2** — APPROVED. Round 1 CRITICAL and WARNING resolved.
      Remaining findings are WARNING (theoretical) / SUGGESTION only (cross-tenant
      ON CONFLICT false-ok requires UUID collision; B7 precondition; D3 static).
- [x] 4.7 Applied to production `sflhtihqdhrlryeyrzdo` as
      `inventory_single_writer_purge_and_guard`. Post-apply probe:
      `venta_total=0`, `phantom_remaining=0`, `salida_venta_total=25` (unchanged),
      `rpc_hardened=true`.

Status: GREEN — remote applied.

Accepted limits (documented in `design.md`, not fixed): same-date ties with null `updatedAt`;
`nota IS NULL` phantoms (zero in production).

## WU-5 — Device verification

- [x] 5.1 `:optoapp:assembleRelease` + `adb install -r` on CLK-LX3 (production keystore, no wipe)
      at 23:01:39 — Room 46 release (`versionCode=51`).
- [x] 5.2 Full sync ×3; logcat `clk-lx3-2026-08-14-room46-sync.log`. **0** × `23505` /
      `idx_movimientos_conflict`; **0** × real `23514`; **0** × `rpc_adjust_montura_stock`.
- [x] 5.3 Probe production: `venta=0`, `SALIDA_VENTA=25`, empty refs=0, total movimientos=37.
- [x] 5.4 UI diagnostics: Estado OK, “No hay registros con error de sincronización.”
- [x] 5.5 Evidence: `evidence/clk-lx3-room46-verify-2026-08-14.md` + log + screenshot.

Status: **PASSED** on CLK-LX3 (Sersa).

## Pre-purge evidence (irreversible step)

- [x] Snapshot recorded in `evidence/prod-phantom-venta-rows-2026-08-14.md` before DELETE.
