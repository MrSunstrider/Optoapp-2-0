# Design: Fix Inventory Double Stock Writer

## Technical Approach

One writer for a sale's stock effect: the **local dispensación save** (`DispensacionStockHelper`
→ Room `montura_movimientos` + `monturas.stockActual`). Sync becomes pure transport —
`SyncInventarioUseCase` upserts the movement rows and the montura snapshot, and
`UploadSyncCoordinator` stops calling `rpc_adjust_montura_stock` entirely.

The RPC is not deleted from the database: `optoapp-web` shares this schema and may call it.
It is hardened instead so that no caller can double-apply or cross tenants.

## Why not "make the RPC idempotent and keep calling it"

Idempotency alone leaves two writers of one fact with two different `tipo` values
(`SALIDA_VENTA` vs `venta`). `idx_movimientos_conflict (referencia_id, tipo, montura_id)`
therefore never dedupes them — it only blocks the *replay* of the second writer. The phantom
ledger, the transient double decrement, and the analytics blind spot
(`rpc_analisis_mensual` reads `SALIDA_VENTA` only) all survive. Removing the second writer
is the only fix that collapses the class.

## Sequencing constraint (load-bearing)

Purging the remote `venta` rows removes exactly what a naive idempotency check matches on. A
device still running the pre-fix build would then find nothing, treat its replay as a brand-new
sale and decrement `stock_actual` for real — the 23505 that used to abort it is gone. Measured
against production in a rolled-back probe: `stock 3 → 2` plus a re-created phantom.

So the migration hardens **before** it purges, in one transaction, and the hardening makes the
sale's `tipo` an alias rather than part of its identity:

```
1. Remote: harden RPC (sale aliases collapse) → then purge phantom rows
2. Client: stop calling the RPC + Room 44→45 purges local phantoms
3. Install on CLK-LX3, sync, confirm zero 23505 and zero `venta` on both sides
```

With `venta` and `SALIDA_VENTA` treated as one fact per `(referencia_id, montura_id, optica_id)`,
an old build's replay is a no-op whether or not the phantom row still exists. Fleet order stops
being load-bearing, which is the only version of this fix that does not depend on operator luck.

## Sale aliasing, and why it does not over-match

The identity lookup is tenant-scoped and matches either the exact `tipo` or, when the call is a
sale, any sale alias. A movement type that is not a sale — `AJUSTE`, `ENTRADA` — still applies
normally even when it shares a `referencia_id` with a sale, which the tests pin explicitly.

## Components

| Component | Change |
|-----------|--------|
| `UploadSyncCoordinator.uploadDispensaciones` | Delete the post-chunk stock RPC loop; keep the dispensaciones upsert and `markSynced` order |
| `UploadSyncCoordinator` companion | Delete `buildAdjustStockParams`, `parseAdjustStockOk`, `parseAdjustStockNewStock`, `parseAdjustStockError` (dead once the caller is gone) |
| `UploadSyncCoordinatorTest` | Delete the 9 helper tests that pin the deleted API; add the single-writer invariant test |
| `OptoDatabaseMigrations` | `MIGRATION_44_45`: scoped DELETE of local phantom `venta` rows |
| `OptoDatabase` | `version = 45`, companion re-export, `addMigrations(..., MIGRATION_44_45)` |
| `MonturaInventoryCoordinator.syncStockFromMovimientos` | Deterministic ordering (`fecha`, then `updatedAt`, then `id`) |
| `supabase/migrations/*_inventory_single_writer_purge_and_guard.sql` | Remote phantom purge + hardened RPC |
| `supabase/tests/test_inventory_stock_single_writer.sql` | Executable RED→GREEN assertions |

## Purge predicate (identical on both sides)

A row is a phantom **only** if all hold:

- `tipo = 'venta'`
- `nota = 'venta_dispensacion'`
- a `SALIDA_VENTA` row exists with the same `(referencia_id, montura_id, optica_id)`

Measured on production: 24 matches, **0** `venta` rows outside the predicate. A legitimate
future `venta` row (from some other caller) with no `SALIDA_VENTA` twin is never touched.

Room uses camelCase columns, so the local predicate reads `referenciaId` / `monturaId` /
`opticaId` — snake_case would silently match nothing.

## Hardened `rpc_adjust_montura_stock`

Signature, return shape (`jsonb`), `SECURITY DEFINER` and `search_path` are preserved.

1. **Tenant guard** — `app_private.has_optica_role(auth.uid(), p_optica_id, ARRAY['admin','gerente','especialista','asesor','asesora','ventas'])`, mirroring the
   `montura_movimientos_insert` RLS policy that `SECURITY DEFINER` would otherwise bypass.
   Denied → `{ok:false, error:'forbidden'}`.
2. **Lock** — `SELECT stock_actual FROM monturas WHERE id=… AND optica_id=… FOR UPDATE`;
   missing → `{ok:false, error:'not_found'}`.
3. **Idempotency** — tenant-scoped on `(referencia_id, montura_id, optica_id)` plus the type,
   with `venta`/`SALIDA_VENTA` collapsed into one sale. A match returns
   `{ok:true, idempotent:true, new_stock:<current>, recorded_tipo:…}` and touches nothing.
4. **Insufficiency before write** — `v_old + p_delta < 0` → `{ok:false, error:'insufficient'}`,
   no mutation (the old body mutated then compensated).
5. **Apply** — `INSERT … ON CONFLICT DO NOTHING` first, then update stock only if the insert
   landed. `FOR UPDATE` already serializes concurrent RPC calls per montura, so a zero
   row-count means the fact was recorded by a path that does not take that lock (the
   inventario movement upsert). That ledger row stands and stock must not stack on top, so
   the call returns `idempotent` rather than raising — raising is exactly what produced the
   23505 flood.

`idx_movimientos_conflict` stays. It is the guard that kept stock from collapsing.

## Data-integrity notes

`monturas.stock_actual` is **not** rewritten. It already agrees with the `SALIDA_VENTA`
ledger (montura `16f5f6f4`: ledger 22→18, `stock_actual` 18) because the inventario monturas
upsert overwrote the RPC's decrements. The purge deletes ledger rows only; no stock column
changes, verified by a before/after count in the SQL test.

## Test Strategy

Strict TDD, pure JUnit + MockK, no Robolectric.

| Layer | RED | GREEN |
|-------|-----|-------|
| Single writer | Reflection test asserts `UploadSyncCoordinator` (and companion) expose no `*AdjustStock*` member — fails while helpers exist | Passes after deletion |
| Room 44→45 | `Migration44To45Test` — unresolved `MIGRATION_44_45` | Scoped DELETE with camelCase columns, registered in `OptoDatabase` |
| Reconstruction | Test with two movements on the same `fecha` — nondeterministic `lastOrNull()` | Deterministic `sortedWith(fecha, updatedAt, id)` |
| Remote | SQL asserts: guard present, idempotent replay, insufficiency, 0 phantoms remain, `SALIDA_VENTA` count unchanged, stock unchanged | After migration |

The reflection test is the durable regression guard: it encodes "finanzas upload must not
adjust stock" as a compile/run assertion that survives the deletion, instead of a mock
expectation on a call site that no longer exists.

## Accepted limits (raised in GGA review, deliberately not "fixed")

**Same-date ties with no timestamp.** `latestStockByMontura` orders by `fecha`, then
`updatedAt`, then `id`. When two movements share a `fecha` and both predate the `updatedAt`
column (added in Room 22→23 with no back-fill), the `id` tiebreak is a random UUID and carries
no causal signal. Ordering by the movement chain does not rescue this: a `10→9` sale and a
`9→10` adjustment are each the other's successor, so the chain is a cycle precisely in the
ambiguous case. The change makes the outcome *deterministic across runs*, which the previous
`sortedBy { fecha }` + `lastOrNull()` over DAO row order was not; it does not claim to recover
an ordering the data never recorded. New movements are stamped at write time, so the window is
limited to legacy rows.

**`nota IS NULL` phantoms survive the purge.** The predicate uses equality, so a `venta` row
with a null note would not match. Production has 24 `venta` rows and all 24 match the
predicate, with zero outside it, so there is nothing to catch. Widening the predicate would
trade a measured-empty risk for a real one: deleting `venta` rows the second writer never
wrote.

## Rollback Boundaries

Client PR revert restores the old noise, no data loss. Room 44→45 is forward-only but
deleted rows are reconstructible from the surviving `SALIDA_VENTA`. The remote purge is
irreversible — the 24 rows are snapshotted into `evidence/` before the DELETE. The RPC is
`CREATE OR REPLACE`; restoring the prior body reverts the hardening.

## RDD Status

**Disabled/unmanaged** — invariants, evidence, budget and rollback recorded as principles;
no receipt authority or kill switch exists in this repo.
