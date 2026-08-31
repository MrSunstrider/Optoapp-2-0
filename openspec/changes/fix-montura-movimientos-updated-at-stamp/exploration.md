# Exploration: montura_movimientos updated_at null on upload

## Symptom

PostgreSQL `23502`: `null value in column "updated_at" of relation "montura_movimientos" violates not-null constraint` during `sync:inventario` upload.

Receipt: diagnóstico 2026-08-29 12:06:53, óptica `25af5a92-4a2d-4e7a-957f-61bec87a07d8`.

## Causal chain

1. Android creates `MonturaMovimiento` without `updatedAt` (nullable in Room).
2. `SyncInventarioUseCase.toRemoto()` passes `updatedAt = null` in JSON upsert.
3. Supabase column is NOT NULL; DEFAULT does not apply when client sends explicit null.
4. Audit trigger is BEFORE UPDATE only — no INSERT fallback.

## Write paths without stamp

- `MonturaInventoryCoordinator.insertMonturaMovimiento()` — direct DAO insert
- `MonturaInventoryCoordinator.registrarSalida()` — bypasses coordinator insert
- Call sites: DispensacionStockHelper, MonturasViewModel, OrdenCompraRepository (all route through insertMonturaMovimiento except registrarSalida)

## Why error cleared without code fix

PK reconcile by `(referenciaId, tipo, monturaId)` skips POST when remote row exists; `markSynced` clears batch error.

## Related

- IMPROVEMENT-PLAN S7
- sync-conflict-rootcause REQ-B1
- fix-inventory-movimientos-pk-reconcile (masks 23502 when remote match exists)
