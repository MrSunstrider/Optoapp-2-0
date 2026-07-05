# Proposal: Crear tabla canónica ventas (ledger financiero) y migración de pagos — Fase 1

## Intent

Income calculations today require separate queries against `dispensaciones` and `servicios_extra`, with `pagos` referencing two foreign tables via weak text columns (`dispensacion_id` / `servicio_extra_id`). Every new income type adds another UNION branch. Create a canonical `ventas` table as the single source of truth for all income, with triggers to keep it in sync, and add `pagos.venta_id` to replace the dual-reference pattern.

## Scope

### In Scope

1. Supabase: `CREATE TABLE ventas` + RLS + indexes
2. Supabase: `ALTER TABLE pagos ADD COLUMN venta_id` + backfill
3. Supabase: Backfill ventas from existing dispensaciones and servicios_extra
4. Supabase: Triggers `trg_dispensacion_to_venta` / `trg_servicio_to_venta` (AFTER INSERT OR UPDATE, upsert)
5. Room: `Venta` entity + `VentaDao` (getByOpticaAndDateRange, getById, upsert, getAllByOptica, deleteAll)
6. Room: MIGRATION_29_30 (CREATE TABLE ventas)
7. Android: Local upsert of ventas in `DispensacionViewModel.saveDispensacion()` and `ServiciosViewModel.saveServicio()`
8. Android: `UploadSyncCoordinator` downloads ventas before pagos (NOT uploaded — trigger handles server-side)

### Out of Scope

- Fases 2–10 (Cierre de Caja consuming ventas, Reportes, BI, análisis financiero, UI, etc.)
- Removing `pagos.dispensacion_id` / `pagos.servicio_extra_id` (coexistence maintained)
- Cancelled/anulada states (none exist in source tables)

## Capabilities

### New Capabilities
- `ventas-ledger`: canonical income table, triggered sync from dispensaciones/servicios_extra, Room entity and DAO, offline-first upsert on save with download-only sync.

### Modified Capabilities
- None — Fase 1 is infrastructure only. No spec-level behavior changes.

## Approach

1. Write 3 Supabase migrations: (a) CREATE TABLE ventas + RLS + indexes, (b) backfill ventas from both source tables + pagos.venta_id, (c) triggers on dispensaciones/servicios_extra.
2. Write Venta entity + VentaDao in Room. Add to OptoDatabase entities list, create MIGRATION_29_30.
3. Modify DispensacionViewModel and ServiciosViewModel to upsert into ventas table on every save.
4. Modify UploadSyncDownloader to fetch ventas in the download batch (inserted before pagos in sync order).
5. Strict TDD: write failing tests before implementation (DAO, ViewModel upsert, migration).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `supabase/migrations/` | New (3 files) | ventas DDL + RLS, backfill, triggers |
| `optoapp/data/venta/Venta.kt` | New | Room entity |
| `optoapp/data/venta/VentaDao.kt` | New | DAO with 5 queries |
| `optoapp/data/Migration29to30.kt` | New | Room migration |
| `optoapp/data/OptoDatabase.kt` | Modified | Add Venta entity, bump version, register MIGRATION_29_30 |
| `optoapp/viewmodel/DispensacionViewModel.kt` | Modified | Upsert ventas on saveDispensacion() |
| `optoapp/viewmodel/ServiciosViewModel.kt` | Modified | Upsert ventas on saveServicio() |
| `optoapp/domain/UploadSyncCoordinator.kt` | Modified | Download ventas before pagos |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Trigger conflicts with Android upload of origen entity | Low | Trigger is idempotent (ON CONFLICT DO UPDATE) |
| Backfill timeout on large datasets | Med | DO NOTHING makes re-runs safe; run in transaction with temp table if needed |
| Stale Room DB without ventas (pre-existing installs) | Low | MIGRATION_29_30 handles it |

## Rollback Plan

**Supabase**: Drop triggers, drop ventas table, drop pagos.venta_id column. Data in dispensaciones/servicios_extra/pagos is untouched. **Additive** — no data lost on revert.

**Android**: Revert OptoDatabase version to 29 and remove Venta entity/DAO/migration. Remove upsert calls from ViewModels. Remove download step. All rollback via reverting the commit(s).

## Dependencies

- Supabase project access (migrations applied sequentially)
- Room MIGRATION_29_30 depends on current version = 29

## Success Criteria

- [ ] `ventas` table exists in Supabase with correct schema, RLS, indexes
- [ ] Backfill populates ventas rows for ALL existing dispensaciones and servicios_extra
- [ ] All existing pagos have `venta_id` populated after backfill
- [ ] INSERT/UPDATE on dispensaciones auto-upserts a corresponding venta row
- [ ] INSERT/UPDATE on servicios_extra auto-upserts a corresponding venta row
- [ ] Room MIGRATION_29_30 creates ventas table and preserves existing data
- [ ] DispensacionViewModel.saveDispensacion() upserts into ventas locally
- [ ] ServiciosViewModel.saveServicio() upserts into ventas locally
- [ ] UploadSyncCoordinator downloads ventas before pagos
- [ ] All tests pass (`./gradlew :optoapp:testDebugUnitTest --stacktrace`)
