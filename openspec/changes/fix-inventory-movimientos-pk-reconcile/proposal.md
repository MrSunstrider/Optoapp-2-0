# Proposal: Fix inventory movement PK reconcile on sync

## Intent

Stop `sync:inventario` failing with SQLSTATE `23505` on `idx_movimientos_conflict`.
A movement whose composite key already exists remotely must adopt the remote primary key
locally and must not POST a new UUID.

## Evidence

Logcat (2026-08-25): `Error sincronizando inventario: duplicate key value violates unique constraint "idx_movimientos_conflict"` on `POST .../montura_movimientos` with `Prefer: resolution=merge-duplicates` (PK only).

`ConflictHelper.detectConflictMovimientos` treated same-key / same-`stockNuevo` rows as safe even when `id` differed. `uploadMovimientos` then upserted the local UUID.

## Scope

### In Scope
- Partition safe movimientos into upload vs local ID reconcile
- Adopt remote `id` in Room when the composite key matches and stock matches
- Upsert remaining rows with `onConflict = referencia_id,tipo,montura_id`
- Unit tests for partition and upload skip/upload/conflict paths

### Out of Scope
- Dropping `idx_movimientos_conflict`
- Swallowing 23505
- Schema / RLS changes
- Dirty-row filtering of the inventario snapshot

## Capabilities

### Modified Capabilities
- `sync`: inventario upload reconciles movement identity before transport

## Approach

Same pattern as `SyncInventarioFisicoUseCase.uploadDetalles`: one remote fetch, composite-key lookup, adopt remote PK locally, upload only new facts.

## Causal Invariant

`(referencia_id, tipo, montura_id)` names one movement fact. A second local UUID for that fact is the same fact, not a new row.

## Affected Areas

`domain/sync/ConflictHelper.kt`, `domain/SyncInventarioUseCase.kt`, unit tests.
