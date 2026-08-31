# Proposal: Close remaining updatedAt stamp gaps (monturas + proveedores)

## Intent

Prevent inventario/proveedores upload failures from null `updated_at` after montura_movimientos fix.

## In scope

- `MonturaDao.adjustStock` sets `updatedAt` on successful stock change
- Migration 48→49 backfill `monturas.updatedAt IS NULL`
- `ProveedorRepository.insert` stamps `updatedAt`

## Out of scope

- Backup restore stamping
- Supabase schema changes
