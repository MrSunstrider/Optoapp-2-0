# Change: add-costos-lc

## Intent
Create dedicated `costos_lc` table for contact lens costs, replacing the hack where LC used `costos_productos` with `stock_o_fabricacion='lente_contacto'`.

## Why
Contact lenses have their own domain (cosmetico vs graduado, materials, modalities) that doesn't fit the lens cost matrix model. A dedicated table with its own lookup logic is cleaner.

## Approach
Follow the `costos_biselado` pattern: own entity, own DAO, own sync upload/download, own Supabase table with RLS.

## Files
- NEW: `data/costolc/CostoLcEntity.kt` — Room entity
- NEW: `data/costolc/CostoLcDao.kt` — lookup + upsertAll + flow query
- MODIFY: `data/OptoDatabase.kt` — add entity + DAO
- MODIFY: `di/DatabaseModule.kt` — provide DAO
- MODIFY: `domain/SyncFinanzasDto.kt` — add CostoLcDto
- MODIFY: `domain/SyncFinanzasUseCase.kt` — add upload/download
- NEW: `supabase/migrations/20260717020002_create_costos_lc.sql` — table + RLS

## Rollback
Drop migration + git checkout. No existing data in costos_productos with lente_contacto (0 rows).

## Strict TDD
Tests before implementation. No Robolectric.
