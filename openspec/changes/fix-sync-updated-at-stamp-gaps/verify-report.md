# Verify Report: fix-sync-updated-at-stamp-gaps

**Date:** 2026-08-31

## Gaps closed

| Area | Fix |
|------|-----|
| monturas stock adjust | `adjustStock` sets `updatedAt`; coordinator passes `Instant.now()` |
| monturas legacy rows | Migration 48→49 backfill null `updatedAt` |
| proveedores insert | `ProveedorRepository.insert` stamps `updatedAt` |

## Not affected / already OK

- Finanzas/historial user writes via `OptoRepository` (stamp on insert/update)
- inventario_fisico, dispensacion_items, gastos (no `updated_at` on upload DTO)
- montura_movimientos (prior change 47→48)

## Deferred

- `BackupRestoreCoordinator` restore path without stamps — separate change if needed

## Tests

Targeted + full `testDebugUnitTest` GREEN.
