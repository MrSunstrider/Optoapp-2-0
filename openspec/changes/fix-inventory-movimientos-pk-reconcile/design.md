# Design: inventory movement PK reconcile

## Root cause

PostgREST upsert defaults to PK `id`. Unique index `idx_movimientos_conflict (referencia_id, tipo, montura_id)` rejects a second UUID for the same fact. Room `@Insert(REPLACE)` on that unique index regenerates local IDs when a dispensación is edited.

## Flow

```
local snapshot → distinctBy composite key
             → filterConflictMovimientos (one remote fetch)
             → partition: upload | reconcile locally
             → adopt remote.id in Room
             → upsert only new/same-id rows with onConflict composite
```

## Seams

- `ConflictHelper.partitionMovimientosForUpload` — pure, unit-tested
- `filterConflictMovimientos` returns `MovimientoUploadPlan` (safeIds + remoteByKey + conflictedIds)
- `SyncInventarioUseCase.upsertMovimientosBatch` / `runInTransaction` — overridable in tests

## Rollback

Revert the two production files. `idx_movimientos_conflict` is unchanged.
