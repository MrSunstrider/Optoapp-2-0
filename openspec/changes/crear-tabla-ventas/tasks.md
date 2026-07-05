# Tasks: Crear tabla canónica ventas — Fase 1

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~550–650 |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: Supabase; PR 2: Room; PR 3: Android wiring + tests |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | PR | Notes |
|------|------|----|-------|
| 1 | 3 Supabase migrations (DDL, backfill, triggers) | PR 1 | Standalone |
| 2 | Room entity + DAO + MIGRATION_29_30 + tests | PR 2 | Depends on PR 1 |
| 3 | Repository + ViewModels + Sync + DTO + tests | PR 3 | Depends on PR 2 |

## Phase 1: Supabase Migrations

- [ ] 1.1 `NNNN_create_ventas_table.sql` — DDL, CHECK origen IN ('dispensacion','servicio_extra'), CHECK estado IN ('Pendiente','Entregado','Anulado'), 2 indexes, RLS + 4 policies
- [ ] 1.2 `NNNN_backfill_ventas.sql` — INSERT ventas ON CONFLICT DO NOTHING from both source tables, ALTER pagos ADD venta_id + index, UPDATE pagos.venta_id
- [ ] 1.3 `NNNN_triggers_ventas.sql` — 2 fn + 2 AFTER INSERT OR UPDATE triggers on dispensaciones and servicios_extra

## Phase 2: Room Data Layer

- [ ] 2.1 [RED] VentaDao test: Room in-memory DB, verify upsert/getById/getByOpticaAndDateRange/getAllByOptica/deleteAll
- [ ] 2.2 [RED] MIGRATION_29_30 test: build v29 DB, migrate, assert ventas table + indexes exist, existing data preserved
- [ ] 2.3 [GREEN] Venta.kt entity — @Entity(tableName="ventas"), 12 cols, indices on opticaId and (origen, origenId)
- [ ] 2.4 [GREEN] VentaDao.kt — getByOpticaAndDateRange (Flow, ORDER BY fecha DESC), getById, upsert (@Upsert), getAllByOptica, deleteAll
- [ ] 2.5 [GREEN] MIGRATION_29_30 in OptoDatabaseMigrations.kt — CREATE TABLE ventas (camelCase cols), CREATE INDEX opticaId + (origen, origenId)
- [ ] 2.6 [GREEN] OptoDatabase.kt — version=30, add Venta::class, abstract ventaDao(), re-export MIGRATION_29_30, add to builder

## Phase 3: Repository + ViewModels

- [x] 3.1 [RED] OptoRepository test: upsertVenta delegates to DAO, upsertVentaFromRemote skips timestamp + scheduler
- [x] 3.2 [RED] DispensacionViewModel test: saveDispensacion upserts Venta with "v_disp_" prefix BEFORE scheduleFinanzasSync
- [x] 3.3 [RED] ServiciosViewModel test: saveServicio upserts Venta with "v_serv_" prefix BEFORE scheduleFinanzasSync
- [x] 3.4 [GREEN] OptoRepository — add upsertVenta(venta) + upsertVentaFromRemote(venta) following remote-bypass pattern
- [x] 3.5 [GREEN] DispensacionViewModel.saveDispensacion() — construct Venta after persistence, upsert before sync scheduler
- [x] 3.6 [GREEN] ServiciosViewModel.saveServicio() — construct Venta after persistence, upsert before sync scheduler

## Phase 4: Sync Integration

- [x] 4.1 [RED] VentaRemota DTO test: toEntity() maps snake→camelCase correctly
- [x] 4.2 [RED] DownloadSyncCoordinator test: downloadVentas fetches + persists via upsertVentaFromRemote
- [x] 4.3 [RED] SyncFinanzasUseCase test: downloadVentas called between servicios and pagos, result includes downloadedVentas
- [x] 4.4 [GREEN] SyncFinanzasDto.kt — add VentaRemota (@Serializable), FinanzasSyncResult.downloadedVentas field
- [x] 4.5 [GREEN] DownloadSyncCoordinator.kt — TABLE_VENTAS const, downloadVentas(opticaId): Int following downloadPagos pattern
- [x] 4.6 [GREEN] SyncFinanzasUseCase.invoke() — wire downloadVentas() between downloadServicios() and downloadPagos()
