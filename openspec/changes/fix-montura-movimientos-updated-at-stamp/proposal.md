# Proposal: Stamp updatedAt on montura_movimientos Room save

## Intent

Eliminate 23502 failures on inventario upload caused by explicit `updated_at: null` in PostgREST upsert. Complete REQ-B1 of `sync-conflict-rootcause` for movement entities.

## Causal invariant

Every `MonturaMovimiento` persisted locally for upload MUST have non-null `updatedAt` before `toRemoto()`.

## In scope

- Stamp in `MonturaInventoryCoordinator.insertMonturaMovimiento()`
- Route `registrarSalida()` through stamped insert path
- Room migration 47→48 backfill legacy null rows
- TDD tests: coordinator, migration, upload batch

## Out of scope

- Supabase BEFORE INSERT trigger
- `toRemoto()` Instant.now() fallback (REQ-B2 drift)
- Per call-site stamps

## Rollback

Revert coordinator + migration; backfilled timestamps are harmless.

## Budget

~340 authored lines (< 400 RDD gate). Single PR. No remote DB migration.
