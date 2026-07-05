# Crear Tabla Ventas — Delta Specification

## Overview

Create a canonical `ventas` ledger table as the single source of truth for all income, with PostgreSQL triggers to keep it in sync from `dispensaciones` and `servicios_extra`, add `pagos.venta_id` to replace the dual-reference pattern, and wire the Android app for offline-first local venta creation with download-only sync.

**Phase**: Fase 1 of 10-phase financial module — infrastructure only. No spec-level behavior changes for existing screens.

---

## Requirements

### R1: Supabase `ventas` Table

The database SHALL contain a table `public.ventas` with the following columns:

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `TEXT` | `PRIMARY KEY` |
| `optica_id` | `TEXT` | `NOT NULL` |
| `origen` | `TEXT` | `NOT NULL`, `CHECK (origen IN ('dispensacion', 'servicio_extra'))` |
| `origen_id` | `TEXT` | `NOT NULL` |
| `paciente_id` | `TEXT` | `NOT NULL` |
| `fecha` | `DATE` | `NOT NULL` |
| `fecha_entrega` | `DATE` | nullable |
| `monto_total` | `NUMERIC` | `NOT NULL` |
| `costo_unitario_snapshot` | `NUMERIC` | nullable — best-effort cost capture at time of sale |
| `estado` | `TEXT` | `NOT NULL`, `CHECK (estado IN ('Pendiente', 'Entregado', 'Anulado'))` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT NOW()` |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT NOW()` |
| `updated_by` | `UUID` | nullable |

### R2: ID Convention

Every `ventas` row SHALL use a deterministic ID:

- Dispensación: `'v_disp_' || dispensacion.id`
- Servicio extra: `'v_serv_' || servicio_extra.id`

The trigger upsert SHALL use `ON CONFLICT (id) DO UPDATE` (idempotent).

### R3: Indexes on `ventas`

The following indexes SHALL be created:

1. `index_ventas_optica_id_fecha` ON `ventas(optica_id, fecha)` — for date-range queries by optica.
2. `index_ventas_origen_origen_id` ON `ventas(origen, origen_id)` — for lookups by source entity.

### R4: RLS Policies on `ventas`

RLS SHALL be enabled on `public.ventas`. Four policies SHALL be created, matching the existing pattern:

| Policy | Command | Using / With Check |
|--------|---------|--------------------|
| `ventas_select` | SELECT | `app_private.is_optica_member(auth.uid(), optica_id)` |
| `ventas_insert` | INSERT | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas'])` |
| `ventas_update` | UPDATE | Same as INSERT (using + with check) |
| `ventas_delete` | DELETE | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente'])` |

### R5: `pagos.venta_id` Column

An `ALTER TABLE public.pagos ADD COLUMN venta_id TEXT` SHALL be applied. An index `index_pagos_venta_id` ON `pagos(venta_id)` SHALL be created. The existing `dispensacion_id` and `servicio_extra_id` columns SHALL remain in place (coexistence).

### R6: Backfill Ventas

A one-time backfill migration SHALL:

1. Insert a row into `ventas` for every existing row in `dispensaciones` (where `estado_entrega` maps to `estado`).
2. Insert a row into `ventas` for every existing row in `servicios_extra` (where `estado` maps to `ventas.estado`).
3. Use `ON CONFLICT (id) DO NOTHING` for safe re-runs.
4. Set `pagos.venta_id` = `'v_disp_' || pagos.dispensacion_id` WHERE `dispensacion_id IS NOT NULL`.
5. Set `pagos.venta_id` = `'v_serv_' || pagos.servicio_extra_id` WHERE `servicio_extra_id IS NOT NULL` and `dispensacion_id IS NULL`.

### R7: Trigger on `dispensaciones`

A trigger `trg_dispensacion_to_venta` SHALL be defined:

- **Event**: `AFTER INSERT OR UPDATE` ON `public.dispensaciones` FOR EACH ROW.
- **Function**: Upserts into `ventas` with:
  - `id` = `'v_disp_' || NEW.id`
  - `optica_id` = NEW.optica_id
  - `origen` = `'dispensacion'`
  - `origen_id` = NEW.id
  - `paciente_id` = NEW.paciente_id
  - `fecha` = NEW.fecha
  - `fecha_entrega` = NEW.fecha_entrega
  - `monto_total` = NEW.monto_total
  - `costo_unitario_snapshot` = NULL (best-effort in future phases)
  - `estado` = `'Entregado'` WHEN NEW.estado_entrega = `'Entregado'`, ELSE `'Pendiente'`
  - `updated_by` = NEW.updated_by
- On conflict, SHALL update `monto_total`, `fecha_entrega`, `estado`, `updated_at`, `updated_by`.

### R8: Trigger on `servicios_extra`

A trigger `trg_servicio_to_venta` SHALL be defined:

- **Event**: `AFTER INSERT OR UPDATE` ON `public.servicios_extra` FOR EACH ROW.
- **Function**: Upserts into `ventas` with analogous mapping, `origen` = `'servicio_extra'`.
- `paciente_id` SHALL use `COALESCE(NEW.paciente_id, '')` (servicios_extra allows NULL paciente_id).
- `estado` SHALL be `'Entregado'` WHEN NEW.estado = `'Entregado'`, ELSE `'Pendiente'`.

### R9: Room `Venta` Entity

A new Room entity `Venta` SHALL be created at `optoapp/src/main/java/com/example/optoapp/data/venta/Venta.kt`:

- Table name: `ventas`.
- Indices: `opticaId`, composite `(origen, origenId)`.
- Columns (camelCase per project convention): `id` (PK), `opticaId`, `origen`, `origenId`, `pacienteId`, `fecha` (LocalDate), `fechaEntrega` (nullable LocalDate), `montoTotal` (Double), `costoUnitarioSnapshot` (nullable Double), `estado`, `createdAt` (nullable String), `updatedAt` (nullable String), `updatedBy` (nullable String).

### R10: Room `VentaDao`

A new DAO interface SHALL be created at `optoapp/src/main/java/com/example/optoapp/data/venta/VentaDao.kt` with:

| Method | Return | Description |
|--------|--------|-------------|
| `getByOpticaAndDateRange(opticaId, start, end)` | `Flow<List<Venta>>` | Reactive query filtered by optica + date range |
| `getById(id)` | `suspend Venta?` | Single record lookup |
| `upsert(venta)` | `suspend` | `@Upsert` — insert or replace |
| `getAllByOptica(opticaId)` | `suspend List<Venta>` | Snapshot for sync reconciliation |
| `deleteAll()` | `suspend` | Clear local cache |

### R11: Room Migration 29→30

A new migration `MIGRATION_29_30` SHALL be added to `OptoDatabaseMigrations.kt`:

- `CREATE TABLE IF NOT EXISTS ventas (...)` — schema matching Venta entity with camelCase columns and REAL for numeric fields.
- `CREATE INDEX index_ventas_opticaId ON ventas(opticaId)`.
- `CREATE INDEX index_ventas_origen_origenId ON ventas(origen, origenId)`.

The `OptoDatabase` SHALL:
- Bump `version = 30`.
- Add `Venta::class` to the `entities` array.
- Add abstract `ventaDao(): VentaDao`.
- Register `MIGRATION_29_30` (re-export and builder chain).

### R12: `OptoRepository` — Local Upsert and Remote Path

`OptoRepository` SHALL add two methods:

1. `suspend fun upsertVenta(venta: Venta)` — delegates to `ventaDao.upsert(venta)`, used by ViewModel local saves.
2. `suspend fun upsertVentaFromRemote(venta: Venta)` — delegates to the same DAO upsert, **NO** timestamp override, **NO** scheduler call (follows the existing remote-bypass pattern in OptoRepository lines 166–182).

### R13: DispensacionViewModel — Local Venta Upsert

`DispensacionViewModel.saveDispensacion()` SHALL create and upsert a `Venta` row **immediately after** the `DispensacionOptica` is persisted and **before** `postSaveSyncScheduler.scheduleFinanzasSync()` is called.

The venta SHALL be constructed with:
- `id` = `"v_disp_$finalId"`
- `origen` = `"dispensacion"`
- `origenId` = `finalId`
- `pacienteId`, `fecha`, `fechaEntrega`, `montoTotal` — from the saved `DispensacionOptica`
- `costoUnitarioSnapshot` = `null`
- `estado` = `"Entregado"` if `s.estadoEntrega == "Entregado"`, else `"Pendiente"`

This SHALL happen regardless of online/offline state.

### R14: ServiciosViewModel — Local Venta Upsert

`ServiciosViewModel.saveServicio()` SHALL create and upsert a `Venta` row **immediately after** the `ServicioExtra` is persisted and **before** `postSaveSyncScheduler.scheduleFinanzasSync()` is called.

The venta SHALL be constructed with:
- `id` = `"v_serv_$finalId"`
- `origen` = `"servicio_extra"`
- `origenId` = `finalId`
- `pacienteId` = `state.pacienteId?.takeIf { !it.isBlank() } ?: ""`
- `fecha` = `state.fecha`
- `fechaEntrega` = `null`
- `montoTotal` = `montoParsed`
- `costoUnitarioSnapshot` = `null`
- `estado` = `"Entregado"` if `state.estado == "Entregado"`, else `"Pendiente"`

This SHALL happen regardless of online/offline state.

### R15: DownloadSyncCoordinator — Ventas Download

`DownloadSyncCoordinator` SHALL:

1. Add constant `TABLE_VENTAS = "ventas"`.
2. Add method `suspend fun downloadVentas(opticaId: String): Int` following the exact pattern of `downloadPagos()`.
3. Add a `VentaRemota` data class in `SyncFinanzasDto.kt` with `@SerialName` mappings (snake_case → camelCase), a `toEntity()` method, and appropriate defaults.
4. Be called in `SyncFinanzasUseCase.invoke()` **between** `downloadServicios()` and `downloadPagos()`.

The downloaded ventas SHALL be persisted via `repository.upsertVentaFromRemote()` (timestamp-preserving path).

### R16: UploadSyncCoordinator — No Ventas Upload

`UploadSyncCoordinator` SHALL NOT have a `uploadVentas()` method. Ventas are server-authoritative: the PostgreSQL triggers handle creation and updates on the server. The app only downloads ventas.

### R17: FinanzasSyncResult — Ventas Field

`FinanzasSyncResult` SHALL gain a new field: `val downloadedVentas: Int = 0`, logged and tracked in `SyncFinanzasUseCase`.

---

## Scenarios

---

### Scenario: Supabase ventas table creation

```
GIVEN the Supabase project is at the latest migration
 WHEN migration `XXXX_create_ventas_table.sql` is applied
 THEN a table `public.ventas` exists
  AND it has columns id, optica_id, origen, origen_id, paciente_id, fecha, fecha_entrega, monto_total, costo_unitario_snapshot, estado, created_at, updated_at, updated_by
  AND the CHECK constraint on origen permits only 'dispensacion' and 'servicio_extra'
  AND the CHECK constraint on estado permits only 'Pendiente', 'Entregado', and 'Anulado'
  AND indexes exist on (optica_id, fecha) and (origen, origen_id)
```

### Scenario: RLS policies applied to ventas

```
GIVEN the ventas table exists with RLS enabled
 WHEN the migration is applied
 THEN a SELECT policy exists allowing any optica member (is_optica_member)
  AND an INSERT policy exists for write roles (admin, gerente, especialista, asesor, asesora, ventas)
  AND an UPDATE policy exists for write roles with both USING and WITH CHECK
  AND a DELETE policy exists for admin and gerente only
```

### Scenario: pagos.venta_id column added

```
GIVEN the pagos table exists
 WHEN migration `XXXX_backfill_ventas.sql` is applied
 THEN pagos has column venta_id (TEXT, nullable)
  AND an index exists on pagos(venta_id)
  AND pagos.dispensacion_id and pagos.servicio_extra_id still exist
```

### Scenario: Backfill from dispensaciones

```
GIVEN existing rows in public.dispensaciones with known id, optica_id, paciente_id, fecha, monto_total, estado_entrega, updated_at
 WHEN the backfill migration runs
 THEN for each dispensacion, a row is inserted into ventas with:
   - id = 'v_disp_' || dispensacion.id
   - origen = 'dispensacion'
   - estado = 'Entregado' if estado_entrega = 'Entregado' else 'Pendiente'
  AND re-running the migration does not fail (idempotent via ON CONFLICT DO NOTHING)
```

### Scenario: Backfill from servicios_extra

```
GIVEN existing rows in public.servicios_extra with known id, optica_id, paciente_id (nullable), fecha, monto_total, estado
 WHEN the backfill migration runs
 THEN for each servicio, a row is inserted into ventas with:
   - id = 'v_serv_' || servicio.id
   - origen = 'servicio_extra'
   - paciente_id = COALESCE(servicio.paciente_id, '')
   - estado = 'Entregado' if servicio.estado = 'Entregado' else 'Pendiente'
  AND re-running the migration does not fail
```

### Scenario: Backfill pagos.venta_id

```
GIVEN ventas have been backfilled from dispensaciones and servicios_extra
 WHEN the backfill migration runs the UPDATE pagos step
 THEN for every pago with dispensacion_id IS NOT NULL: venta_id = 'v_disp_' || dispensacion_id
  AND for every pago with servicio_extra_id IS NOT NULL AND dispensacion_id IS NULL: venta_id = 'v_serv_' || servicio_extra_id
  AND pagos with neither reference remain with venta_id = NULL
```

### Scenario: Trigger on INSERT to dispensaciones

```
GIVEN a new dispensacion is INSERTED into public.dispensaciones
 WHEN the AFTER INSERT trigger fires
 THEN a corresponding venta row is upserted with:
   - id = 'v_disp_' || NEW.id
   - origen = 'dispensacion'
   - estado matching estado_entrega
   - monto_total matching NEW.monto_total
```

### Scenario: Trigger on UPDATE to dispensaciones

```
GIVEN an existing dispensacion is UPDATED (e.g., estado_entrega changes to 'Entregado')
 WHEN the AFTER UPDATE trigger fires
 THEN the corresponding venta row is updated:
   - estado = 'Entregado'
   - updated_at = NOW()
   - monto_total reflects the new value if changed
```

### Scenario: Trigger on INSERT to servicios_extra

```
GIVEN a new servicio_extra is INSERTED
 WHEN the AFTER INSERT trigger fires
 THEN a corresponding venta row is upserted with:
   - id = 'v_serv_' || NEW.id
   - origen = 'servicio_extra'
   - paciente_id = COALESCE(NEW.paciente_id, '')
```

### Scenario: Trigger on UPDATE to servicios_extra

```
GIVEN an existing servicio_extra is UPDATED (e.g., monto_total changes)
 WHEN the AFTER UPDATE trigger fires
 THEN the corresponding venta row's monto_total and updated_at are updated
```

### Scenario: Room Venta entity exists with correct schema

```
GIVEN the Room OptoDatabase version is 29
 WHEN MIGRATION_29_30 runs
 THEN a SQLite table `ventas` is created with camelCase columns
  AND indexes on (opticaId) and (origen, origenId) are created
  AND the Venta entity maps to tableName = "ventas"
  AND VentaDao exposes getByOpticaAndDateRange, getById, upsert, getAllByOptica, deleteAll
```

### Scenario: OptoDatabase version bump and entity registration

```
GIVEN the existing OptoDatabase (version 29, entities list without Venta)
 WHEN the change is applied
 THEN version = 30
  AND Venta::class is in the entities array
  AND abstract fun ventaDao(): VentaDao is declared
  AND MIGRATION_29_30 is registered in the builder chain
  AND OptoDatabase.MIGRATION_29_30 re-exports the migration
```

### Scenario: DispensacionViewModel upserts venta on local save

```
GIVEN a user saves a dispensacion offline via DispensacionViewModel.saveDispensacion()
 WHEN the method completes successfully
 THEN a Venta row is upserted into the local Room DB with id = 'v_disp_' || dispensacion.id
  AND estado matches the dispensacion's estadoEntrega
  AND the venta upsert happens BEFORE postSaveSyncScheduler.scheduleFinanzasSync() is called
```

### Scenario: ServiciosViewModel upserts venta on local save

```
GIVEN a user saves a servicio extra offline via ServiciosViewModel.saveServicio()
 WHEN the method completes successfully
 THEN a Venta row is upserted into the local Room DB with id = 'v_serv_' || servicio.id
  AND estado matches the servicio's estado
  AND the venta upsert happens BEFORE postSaveSyncScheduler.scheduleFinanzasSync() is called
```

### Scenario: DownloadSyncCoordinator downloads ventas before pagos

```
GIVEN a sync cycle runs via SyncFinanzasUseCase
 WHEN downloadAfterUpload = true
 THEN downloadSyncCoordinator.downloadVentas() is called AFTER downloadServicios()
  AND BEFORE downloadPagos()
  AND the downloaded ventas are persisted via repository.upsertVentaFromRemote()
  AND each venta's server timestamp is preserved (no Instant.now() override)
  AND the sync scheduler is NOT called for downloaded ventas
```

### Scenario: UploadSyncCoordinator does NOT upload ventas

```
GIVEN UploadSyncCoordinator handles sync uploads
 WHEN any sync cycle runs
 THEN the UploadSyncCoordinator SHALL NOT contain an uploadVentas() method
  AND no ventas data is uploaded to Supabase from the app
```

### Scenario: FinanzasSyncResult includes downloadedVentas

```
GIVEN a sync cycle completes
 WHEN SyncFinanzasUseCase constructs FinanzasSyncResult
 THEN the result contains downloadedVentas: Int
  AND this field defaults to 0
```

### Scenario: Room migration preserves existing data

```
GIVEN a device has OptoDatabase at version 29 with existing data in all tables
 WHEN MIGRATION_29_30 runs
 THEN the ventas table is created as a new table
  AND all existing data in pacientes, dispensaciones, servicios_extra, pagos, monturas, etc. is preserved
```

### Scenario: VentaDao queries

```
GIVEN VentaDao is available in the database
 WHEN getByOpticaAndDateRange is called with opticaId, start, end
 THEN a Flow<List<Venta>> is returned with ventas in that range, ordered by fecha DESC
 WHEN getById is called with an existing id
 THEN the matching Venta is returned
 WHEN upsert is called with a new Venta
 THEN it is inserted
 WHEN upsert is called with an existing Venta (same id)
 THEN it is replaced
 WHEN getAllByOptica is called
 THEN all ventas for that optica are returned
 WHEN deleteAll is called
 THEN all ventas are removed from the local DB
```

---

## Out of Scope (carried from proposal)

- Fases 2–10 (Cierre de Caja consuming ventas, Reportes, BI, análisis financiero, UI, etc.)
- Removing `pagos.dispensacion_id` / `pagos.servicio_extra_id` (coexistence maintained).
- Cancelled/anulada states (none exist in source tables).
- UI changes — Fase 1 is purely data infrastructure.
