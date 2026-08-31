# Exploration: updated_at null upload gaps (post montura_movimientos fix)

## Already fixed

- `montura_movimientos`: stamp in `insertMonturaMovimiento`, migration 47→48 backfill.

## Same 23502 pattern (Supabase NOT NULL + explicit null in upsert)

| Entity | Write gap | Server `updated_at` |
|--------|-----------|-------------------|
| monturas | `adjustStock` / `syncStockFromMovimientos` change stock without touching `updatedAt`; legacy rows pre-audit column | NOT NULL |
| proveedores | `ProveedorRepository.insert` no stamp | nullable (DEFAULT) — stamp for REQ-B1 / future hardening |

## Not affected

- inventario_fisico / detalle: no `updated_at` on Supabase
- dispensacion_items: no `updated_at` column
- gastos_operativos: uses `created_at` (already stamped on insert)
- pacientes/evaluaciones/dispensaciones/pagos/servicios: `OptoRepository` stamps on user writes

## Out of scope (separate change)

- `BackupRestoreCoordinator` inserts finanzas/historial without stamp — needs per-entity audit
