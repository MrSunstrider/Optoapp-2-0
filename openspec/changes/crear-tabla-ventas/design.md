# Design: Crear tabla canónica ventas (ledger financiero) — Fase 1

## Technical Approach

Add a canonical `ventas` table as a read-only (download-only) ledger, delegating write authority to PostgreSQL triggers on `dispensaciones` and `servicios_extra`. Android creates local venta rows on every save (offline-first) and downloads server-authoritative rows during sync. No venta upload path exists — triggers handle dedup server-side. The `ventas` download slot inserts between `servicios` and `pagos` in `SyncFinanzasUseCase`.

## Architecture Decisions

| Decision | Option A (chosen) | Option B (rejected) | Rationale |
|----------|-------------------|---------------------|-----------|
| **Upload** | `UploadSyncCoordinator` has no `uploadVentas()` | Upload from Android with idempotent upsert | Triggers on dispensaciones/servicios_extra guarantee correctness at DB level. Dual-source upload would introduce race conditions. |
| **Local upsert** | ViewModels upsert venta locally BEFORE sync scheduler | Defer to sync download only | Offline-first: users must see their own ventas immediately after save, even with no network. |
| **Download order** | `ventas` after `servicios`, before `pagos` | After `pagos` | Enforces FK/dependency consistency: pagos.venta_id references ventas rows that must exist first. |
| **pagos.venta_id coexistence** | ADD COLUMN, keep dispensacion_id + servicio_extra_id | Drop old columns immediately | Safe incremental migration. Phase 2+ will transition references; no data lost. |
| **Venta entity package** | `data/venta/Venta.kt` (new package) | `data/dispensacion/Venta.kt` | Clean separation. Ventas is its own domain concept, not a sub-concept of dispensaciones. |

## Data Flow

### Sequence: Offline Save (DispensacionViewModel)

```
User taps Save
  │
  ▼
DispensacionViewModel.saveDispensacion()
  ├─ validate
  ├─ repository.insertDispensacion(disp)    ← persisted locally
  ├─ repository.insertDispensacionItem(...)
  ├─ repository.insertPago(pago)
  ├─ repository.upsertVenta(venta)          ← NEW: offline-first venta (R13)
  └─ postSaveSyncScheduler.scheduleFinanzasSync()
```

### Sequence: Sync Download (SyncFinanzasUseCase)

```
SyncFinanzasUseCase.invoke(opticaId)
  │
  ├─ deletionSyncHelper.pushPendingDeletions()
  ├─ uploadDispensaciones() → uploadDispensacionItems() → uploadServicios() → uploadPagos()
  │
  ▼ downloadAfterUpload=true
  ├─ downloadArqueos()
  ├─ downloadDispensaciones()
  ├─ downloadDispensacionItems()
  ├─ downloadServicios()
  ├─ downloadVentas(opticaId)                ← NEW: before pagos (R15)
  │     └─ repository.upsertVentaFromRemote()  ← timestamp-preserving path (R12)
  └─ downloadPagos(opticaId)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `supabase/migrations/NNN_create_ventas_table.sql` | Create | DDL: ventas table, CHECK constraints, indexes, RLS policies |
| `supabase/migrations/NNN_backfill_ventas.sql` | Create | Backfill ventas rows + ALTER pagos ADD venta_id + backfill pagos.venta_id |
| `supabase/migrations/NNN_triggers_ventas.sql` | Create | AFTER INSERT/UPDATE triggers on dispensaciones + servicios_extra |
| `optoapp/.../data/venta/Venta.kt` | Create | Room entity: `@Entity(tableName = "ventas")` with 12 columns + indices |
| `optoapp/.../data/venta/VentaDao.kt` | Create | DAO: getByOpticaAndDateRange, getById, upsert, getAllByOptica, deleteAll |
| `optoapp/.../data/OptoDatabaseMigrations.kt` | Modify | Add `MIGRATION_29_30` (CREATE TABLE ventas + indices) |
| `optoapp/.../data/OptoDatabase.kt` | Modify | Version 30, add Venta::class, ventaDao(), re-export MIGRATION_29_30 |
| `optoapp/.../data/OptoRepository.kt` | Modify | Add `upsertVenta(venta)` and `upsertVentaFromRemote(venta)` |
| `optoapp/.../viewmodel/DispensacionViewModel.kt` | Modify | Upsert venta after dispensacion persisted, before scheduleFinanzasSync |
| `optoapp/.../viewmodel/ServiciosViewModel.kt` | Modify | Upsert venta after servicio persisted, before scheduleFinanzasSync |
| `optoapp/.../domain/DownloadSyncCoordinator.kt` | Modify | Add `downloadVentas(opticaId): Int` method |
| `optoapp/.../domain/SyncFinanzasDto.kt` | Modify | Add `VentaRemota` data class + `toEntity()`, add `downloadedVentas` to `FinanzasSyncResult` |
| `optoapp/.../domain/SyncFinanzasUseCase.kt` | Modify | Wire `downloadVentas()` between servicios and pagos |

## Room Schema (Venta entity)

```kotlin
@Entity(
    tableName = "ventas",
    indices = [
        Index("opticaId"),
        Index("origen", "origenId")
    ]
)
data class Venta(
    @PrimaryKey val id: String,               // "v_disp_<id>" or "v_serv_<id>"
    val opticaId: String,
    val origen: String,                        // "dispensacion" | "servicio_extra"
    val origenId: String,
    val pacienteId: String,
    val fecha: LocalDate,
    val fechaEntrega: LocalDate? = null,
    val montoTotal: Double,
    val costoUnitarioSnapshot: Double? = null,
    val estado: String,                        // "Pendiente" | "Entregado" | "Anulado"
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val updatedBy: String? = null
)
```

## Supabase Migrations (3 files, sequential)

1. **Migration 1** (`XXX_create_ventas_table.sql`): `CREATE TABLE public.ventas` with all columns, CHECK constraints on `origen` and `estado`, RLS enabled, 4 policies (select/insert/update/delete), composite indexes.
2. **Migration 2** (`XXX_backfill_ventas.sql`): `INSERT INTO ventas ... ON CONFLICT DO NOTHING` from dispensaciones (mapping `estado_entrega→estado`) + servicios_extra (using `COALESCE(paciente_id, '')`). Then `ALTER TABLE pagos ADD COLUMN venta_id TEXT`, `CREATE INDEX`, and `UPDATE pagos SET venta_id = 'v_disp_' || dispensacion_id` / `'v_serv_' || servicio_extra_id`.
3. **Migration 3** (`XXX_triggers_ventas.sql`): `CREATE FUNCTION fn_upsert_venta_from_dispensacion()` + `CREATE TRIGGER trg_dispensacion_to_venta AFTER INSERT OR UPDATE ON public.dispensaciones`. Same pattern for servicios_extra. Function uses `ON CONFLICT (id) DO UPDATE SET monto_total=EXCLUDED.monto_total, ...`.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Room migration | MIGRATION_29_30 creates ventas table, preserves existing data | Robolectric: build v29 DB, run migration, query sqlite_master, verify schema |
| VentaDao | Upsert, getById, getByOpticaAndDateRange, getAllByOptica, deleteAll | Room in-memory DB with allowMainThreadQueries |
| DispensacionViewModel | saveDispensacion upserts venta locally | Unit test with fake OptoRepository verifying venta upsert call order |
| ServiciosViewModel | saveServicio upserts venta locally | Same pattern as DispensacionViewModel test |
| DownloadSyncCoordinator | downloadVentas fetches and persists correctly | Mock supabase client, verify repository.upsertVentaFromRemote calls |
| SyncFinanzasUseCase | downloadVentas called between servicios and pagos | Integration test capturing call order of download methods |
| SyncFinanzasDto | VentaRemota.toEntity() maps snake_case→camelCase correctly | Unit test with known JSON fixture |

## Migration / Rollout

**Supabase**: 3 additive migrations run sequentially. No data loss — source tables untouched. Rollback: drop triggers, drop ventas table, drop pagos.venta_id column.

**Android**: MIGRATION_29_30 is additive (CREATE TABLE). Rollback: revert version to 29, remove entity/DAO from OptoDatabase. Existing installs on v29 get an empty ventas table on upgrade — populated during next sync download.
