# Business Analysis Data Schema Specification

## Overview

Foundational data schema for business analysis — product categorization, margin tracking, daily summaries, operative expenses, and financial configuration. This is the infrastructure layer that enables business indicators, recommendations, and UI (Fases 7–10).

The spec covers Supabase migrations, Room entities + DAOs, DI wiring, repository passthroughs, and download sync for read-only tables. No UI changes are included.

---

## Requirements

### R1: Supabase `categorias_producto` Table

The system SHALL create table `public.categorias_producto` with the following schema:

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `TEXT` | `PRIMARY KEY` — stable string identifier (e.g. `'lente_progresivo'`) |
| `nombre` | `TEXT` | `NOT NULL` — display name (e.g. `"Lentes Progresivos"`) |
| `familia` | `TEXT` | `NOT NULL`, `CHECK (familia IN ('lente', 'montura', 'servicio'))` |
| `orden` | `INTEGER` | `NOT NULL DEFAULT 0` — for UI ordering |

#### R1.1: Seed Data

The migration SHALL insert exactly 9 rows idempotently (`ON CONFLICT (id) DO NOTHING`):

| id | nombre | familia | orden |
|----|--------|---------|-------|
| `lente_progresivo` | Lentes Progresivos | lente | 1 |
| `lente_monofocal` | Lentes Monofocales | lente | 2 |
| `lente_bifocal` | Lentes Bifocales | lente | 3 |
| `lente_otro` | Otros Lentes | lente | 9 |
| `montura_premium` | Monturas Premium | montura | 4 |
| `montura_estandar` | Monturas Estándar | montura | 5 |
| `montura_economica` | Monturas Económicas | montura | 6 |
| `servicio_extra` | Servicios Extra | servicio | 7 |
| `servicio_garantia` | Garantías Extendidas | servicio | 8 |

#### R1.2: RLS on `categorias_producto`

RLS SHALL be enabled. Three policies SHALL be created:

| Policy | Command | Using / With Check |
|--------|---------|--------------------|
| `categorias_producto_select` | SELECT | `USING (true)` — all authenticated users can read the master list |
| `categorias_producto_insert` | INSERT | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin'])` |
| `categorias_producto_delete` | DELETE | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin'])` |

Since `categorias_producto` has no `optica_id` column (it's global), the SELECT policy SHALL allow `USING (true)`.

---

### R2: ALTER `ventas` — Add `categoria_producto_id`

The system SHALL `ALTER TABLE public.ventas ADD COLUMN categoria_producto_id TEXT REFERENCES public.categorias_producto(id)`.

- The column SHALL be nullable (existing rows get NULL).
- No default value SHALL be set.
- The column SHALL be indexed: `CREATE INDEX index_ventas_categoria_producto_id ON ventas(categoria_producto_id)`.

---

### R3: Supabase `gastos_operativos` Table

The system SHALL CREATE TABLE `public.gastos_operativos`:

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `UUID` | `PRIMARY KEY DEFAULT gen_random_uuid()` |
| `optica_id` | `TEXT` | `NOT NULL` |
| `categoria` | `TEXT` | `NOT NULL`, `CHECK (categoria IN ('alquiler', 'servicios', 'personal', 'proveedores', 'insumos', 'marketing', 'impuestos', 'otro'))` |
| `descripcion` | `TEXT` | `NOT NULL` |
| `monto` | `NUMERIC` | `NOT NULL` |
| `fecha` | `DATE` | `NOT NULL` — the date this expense was incurred |
| `fecha_programada` | `DATE` | nullable — for recurring/scheduled expenses |
| `nota` | `TEXT` | nullable |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT NOW()` |

An index SHALL be created: `CREATE INDEX idx_gastos_operativos_optica_fecha ON gastos_operativos(optica_id, fecha)`.

#### R3.1: RLS on `gastos_operativos`

| Policy | Command | Using / With Check |
|--------|---------|--------------------|
| `gastos_select` | SELECT | `app_private.is_optica_member(auth.uid(), optica_id)` |
| `gastos_insert` | INSERT | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente'])` |
| `gastos_update` | UPDATE | Same as insert (USING + WITH CHECK) |
| `gastos_delete` | DELETE | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente'])` |

---

### R4: Supabase `margen_por_categoria` Table

The system SHALL CREATE TABLE `public.margen_por_categoria`:

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `UUID` | `PRIMARY KEY DEFAULT gen_random_uuid()` |
| `optica_id` | `TEXT` | `NOT NULL` |
| `categoria_producto_id` | `TEXT` | `NOT NULL`, FK to `categorias_producto(id)` |
| `periodo` | `TEXT` | `NOT NULL` — e.g. `'2026-07'`, `'2026-Q3'`, `'2026'` |
| `tipo_periodo` | `TEXT` | `NOT NULL`, `CHECK IN ('mensual', 'trimestral', 'anual')` |
| `ventas_totales` | `NUMERIC` | `NOT NULL` |
| `costo_total` | `NUMERIC` | `NOT NULL` |
| `cantidad_ventas` | `INTEGER` | `NOT NULL` |
| `margen_bruto` | `NUMERIC` | `NOT NULL` — `ventas_totales - costo_total` |
| `margen_porcentaje` | `NUMERIC` | `NOT NULL` — `(margen_bruto / ventas_totales) * 100` |
| `ticket_promedio` | `NUMERIC` | `NOT NULL` — `ventas_totales / cantidad_ventas` |
| `calculado_en` | `TIMESTAMPTZ` | `NOT NULL DEFAULT NOW()` |

**UNIQUE constraint**: `UNIQUE (optica_id, categoria_producto_id, periodo, tipo_periodo)`

**Index**: `CREATE INDEX idx_margen_cat_opt_per ON margen_por_categoria(optica_id, periodo)`

#### R4.1: RLS on `margen_por_categoria`

| Policy | Command | Using / With Check |
|--------|---------|--------------------|
| `margen_cat_select` | SELECT | `app_private.is_optica_member(auth.uid(), optica_id)` |

Server-calculated table; no INSERT/UPDATE/DELETE policies needed (write happens via RPC or direct admin SQL).

#### R4.2: No Room entity for `margen_por_categoria`

This table is server-side only. No Room entity, DAO, or sync wiring SHALL be created for it. It is populated by `recalcular_resumen_diario()` or dedicated server-side recalculation in future phases.

---

### R5: Supabase `resumen_diario` Table

The system SHALL CREATE TABLE `public.resumen_diario`:

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `UUID` | `PRIMARY KEY DEFAULT gen_random_uuid()` |
| `optica_id` | `TEXT` | `NOT NULL` |
| `fecha` | `DATE` | `NOT NULL` |
| `ventas_cantidad` | `INTEGER` | `NOT NULL DEFAULT 0` |
| `ventas_monto_total` | `NUMERIC` | `NOT NULL DEFAULT 0` |
| `ventas_costo_total` | `NUMERIC` | `NOT NULL DEFAULT 0` |
| `cobros_cantidad` | `INTEGER` | `NOT NULL DEFAULT 0` |
| `cobros_monto_total` | `NUMERIC` | `NOT NULL DEFAULT 0` |
| `saldo_pendiente_total` | `NUMERIC` | `NOT NULL DEFAULT 0` |
| `saldo_pendiente_cantidad` | `INTEGER` | `NOT NULL DEFAULT 0` |
| `inventario_valor` | `NUMERIC` | nullable |
| `inventario_unidades` | `INTEGER` | nullable |
| `calculado_en` | `TIMESTAMPTZ` | `NOT NULL DEFAULT NOW()` |

**UNIQUE constraint**: `UNIQUE (optica_id, fecha)`

**Index**: `CREATE INDEX idx_resumen_diario_opt_fecha ON resumen_diario(optica_id, fecha)`

#### R5.1: RLS on `resumen_diario`

| Policy | Command | Using / With Check |
|--------|---------|--------------------|
| `resumen_diario_select` | SELECT | `app_private.is_optica_member(auth.uid(), optica_id)` |

Server-calculated table; no INSERT/UPDATE/DELETE policies needed.

#### R5.2: No upload sync for `resumen_diario`

`resumen_diario` is read-only from Android. The app SHALL download it from Supabase but SHALL NOT upload it. No DAO upsert methods that trigger upload sync SHALL be created for this entity.

---

### R6: Supabase `costos_productos` Table

System SHALL REPLACE `costos_productos` with matrix schema:

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `UUID` | `PRIMARY KEY` |
| `optica_id` | `TEXT` | `NOT NULL` |
| `material` | `TEXT` | `NOT NULL` |
| `tipo_lente` | `TEXT` | `NOT NULL` |
| `stock_o_fabricacion` | `TEXT` | `NOT NULL`, `CHECK IN ('stock','fabricacion','montura')` |
| `tratamiento` | `TEXT` | nullable |
| `serie` | `INTEGER` | nullable — 1/2/3 or null for fixed-price |
| `costo_unitario` | `NUMERIC` | `NOT NULL` |
| `laboratorio_id` | `TEXT` | nullable |
| `vigente_desde` | `DATE` | `NOT NULL` |
| `vigente_hasta` | `DATE` | nullable |

Index: `CREATE INDEX idx_costos_productos_lookup ON costos_productos(optica_id, material, tipo_lente, stock_o_fabricacion, serie) WHERE vigente_hasta IS NULL`.

(Previously: flat schema with categoria_producto_id, producto_descripcion, costo_unitario)

#### R6.1: RLS on `costos_productos`

| Policy | Command | Using / With Check |
|--------|---------|--------------------|
| `costos_productos_select` | SELECT | `app_private.is_optica_member(auth.uid(), optica_id)` |
| `costos_productos_insert` | INSERT | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente'])` |
| `costos_productos_update` | UPDATE | Same as insert |
| `costos_productos_delete` | DELETE | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin'])` |

#### R6.2: Room Entity for `costos_productos`

`CostoProductoEntity` SHALL exist for offline access. DAO SHALL provide lookup queries by block and series. Entity SHALL participate in download AND upload sync. (Previously: no Room entity — server-side only.)

- GIVEN migration applied
- WHEN table inspected
- THEN `costos_productos` has matrix columns
- AND SELECT policy allows optica members
- AND INSERT/UPDATE allow admin/gerente

---

### R7: Supabase `configuracion_financiera` Table

The system SHALL CREATE TABLE `public.configuracion_financiera`:

| Column | Type | Constraints |
|--------|------|-------------|
| `optica_id` | `TEXT` | `PRIMARY KEY` — 1:1 with `opticas(id)` |
| `margen_neto_objetivo` | `NUMERIC` | `DEFAULT 15.0` — target net margin percentage |
| `ticket_promedio_objetivo` | `NUMERIC` | nullable — target average ticket |
| `caida_ventas_alerta_pct` | `NUMERIC` | `DEFAULT 10.0` — alert if sales drop this % |
| `deuda_vieja_alerta_dias` | `INTEGER` | `DEFAULT 30` — debt older than this triggers alert |
| `deuda_total_alerta_monto` | `NUMERIC` | `DEFAULT 3000.0` — total debt over this triggers alert |
| `stock_estancado_alerta_dias` | `INTEGER` | `DEFAULT 180` — slow-moving stock threshold |
| `stock_bajo_alerta_unidades` | `INTEGER` | `DEFAULT 2` — low stock alert threshold |
| `min_ventas_para_recomendar` | `INTEGER` | `DEFAULT 5` — minimum sales for recommendations |
| `frecuencia_recalculo_dias` | `INTEGER` | `DEFAULT 1` — recalculation frequency |

#### R7.1: RLS on `configuracion_financiera`

| Policy | Command | Using / With Check |
|--------|---------|--------------------|
| `config_fin_select` | SELECT | `app_private.is_optica_member(auth.uid(), optica_id)` |
| `config_fin_insert` | INSERT | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente'])` |
| `config_fin_update` | UPDATE | Same as insert |
| `config_fin_delete` | DELETE | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin'])` |

#### R7.2: Read-only from Android

`ConfiguracionFinanciera` is managed via the web companion. The Android app SHALL download it but SHALL NOT upload or provide local insert/update UI for it. Room entity and DAO SHALL provide read-only access (SELECT queries only, no upsert/insert/update).

---

### R8: Supabase `feedback_recomendaciones` Table

The system SHALL CREATE TABLE `public.feedback_recomendaciones`:

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `UUID` | `PRIMARY KEY DEFAULT gen_random_uuid()` |
| `optica_id` | `TEXT` | `NOT NULL` |
| `recomendacion_id` | `TEXT` | `NOT NULL` — hash/slug identifying the recommendation message |
| `fue_util` | `BOOLEAN` | `NOT NULL` |
| `fecha` | `TIMESTAMPTZ` | `DEFAULT NOW()` |

#### R8.1: RLS on `feedback_recomendaciones`

| Policy | Command | Using / With Check |
|--------|---------|--------------------|
| `feedback_select` | SELECT | `app_private.is_optica_member(auth.uid(), optica_id)` |
| `feedback_insert` | INSERT | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas'])` |

No update/delete policies (feedback is append-only).

#### R8.2: No Room entity for `feedback_recomendaciones`

Used only from the web companion and future Edge Functions. No Room entity SHALL be created.

---

### R9: Supabase RPC `recalcular_resumen_diario`

The system SHALL create a PostgreSQL function `public.recalcular_resumen_diario(p_optica_id TEXT, p_fecha DATE) RETURNS void` that idempotently upserts a row into `resumen_diario`.

#### R9.1: Calculation Logic

The function SHALL:

1. **Sales aggregation**: Query `public.ventas` WHERE `optica_id = p_optica_id AND fecha = p_fecha` to compute:
   - `ventas_cantidad` = `COALESCE(COUNT(*), 0)`
   - `ventas_monto_total` = `COALESCE(SUM(monto_total), 0)`
   - `ventas_costo_total` = For each venta, JOIN `dispensaciones ON dispensaciones.venta_id = ventas.id` then JOIN `dispensacion_items ON dispensacion_items.dispensacion_id = dispensaciones.id`. Sum `COALESCE(dispensacion_items.costo_real_od, 0) + COALESCE(dispensacion_items.costo_real_oi, 0) + COALESCE(dispensacion_items.costo_real_montura, 0) + COALESCE(dispensacion_items.costo_real_biselado, 0) + COALESCE(dispensacion_items.costo_real_lc, 0)`. For ventas with no matching `dispensacion_items` (e.g., `servicio_extra`), fall back to `COALESCE(ventas.costo_unitario_snapshot, 0)`.

(Previously: `ventas_costo_total` summed `costo_unitario_snapshot` directly from `ventas` without JOIN to `dispensacion_items`.)

2. **Payments aggregation**: Query `public.pagos` WHERE `optica_id = p_optica_id AND fecha = p_fecha` to compute:
   - `cobros_cantidad` = `COALESCE(COUNT(*), 0)`
   - `cobros_monto_total` = `COALESCE(SUM(monto), 0)`

3. **Pending balance**: Query `public.ventas` LEFT JOIN aggregated `public.pagos` by `venta_id`:
   - `saldo_pendiente_cantidad` = COUNT of ventas where `monto_total - COALESCE(total_pagado, 0) > 0.005`
   - `saldo_pendiente_total` = SUM of the same difference

4. **Inventory snapshot**: Query `public.monturas` WHERE `optica_id = p_optica_id`:
   - `inventario_valor` = `COALESCE(SUM(costo * stock_actual), 0)`
   - `inventario_unidades` = `COALESCE(SUM(stock_actual), 0)`

5. **Idempotent upsert**: `INSERT INTO resumen_diario (...) VALUES (...) ON CONFLICT (optica_id, fecha) DO UPDATE SET ...` updating all computed fields plus `calculado_en = now()`.

#### R9.2: RPC Security

The function SHALL be defined as `SECURITY INVOKER` so it respects RLS policies of the calling user.

#### R9.3: Null-safe Inventory

If `monturas.costo` is NULL for any row, `COALESCE(SUM(costo * stock_actual), 0)` would return 0 for that row's contribution — this is acceptable. The function SHALL NOT fail on NULL cost values.

#### Scenario: costo_real_* from dispensacion_items is used when items exist

- GIVEN a venta on 2026-07-05 with linked dispensacion_items having `costo_real_od = 25.00` and `costo_real_montura = 80.00`
- WHEN `recalcular_resumen_diario('o1', '2026-07-05')` is called
- THEN `ventas_costo_total` includes `105.00` (25 + 80) for that venta, NOT `costo_unitario_snapshot`

#### Scenario: costo_unitario_snapshot fallback for servicio_extra venta

- GIVEN a venta on 2026-07-05 with `categoria_producto_id = 'servicio_extra'` and no linked dispensacion_items, with `costo_unitario_snapshot = 15.00`
- WHEN `recalcular_resumen_diario('o1', '2026-07-05')` is called
- THEN `ventas_costo_total` includes `15.00` from the fallback column

#### Scenario: Mixed ventas — some with items, some without

- GIVEN a mix of dispensacion-linked ventas and servicio_extra ventas on the same fecha
- WHEN `recalcular_resumen_diario('o1', '2026-07-05')` is called
- THEN each venta's cost is summed using its correct source (items or fallback)
- AND the total `ventas_costo_total` is the correct aggregate

---

### R10: Room `Venta` Entity — Add `categoriaProductoId`

The existing `Venta` entity in `data/venta/Venta.kt` SHALL add a new field:

```kotlin
val categoriaProductoId: String? = null
```

- The field SHALL be nullable (existing rows have NULL).
- Room column name SHALL be `categoriaProductoId` (camelCase per project convention).
- The DAO, constructor, and all existing usages SHALL continue to compile without changes (default null handles backward compatibility).

---

### R11: Room `CategoriaProductoEntity`

A new Room entity SHALL exist at `data/categoriaproducto/CategoriaProductoEntity.kt`:

| Column | Type | Room annotation |
|--------|------|-----------------|
| `id` | `String` | `@PrimaryKey` |
| `nombre` | `String` | — |
| `familia` | `String` | — |
| `orden` | `Int` | `DEFAULT 0` |

Table name: `categorias_producto`.

#### R11.1: `CategoriaProductoDao`

| Method | Return | Description |
|--------|--------|-------------|
| `getAll()` | `suspend fun`: `List<CategoriaProductoEntity>` | Read all categories ordered by `orden` |
| `getById(id)` | `suspend fun`: `CategoriaProductoEntity?` | Single lookup |

No upsert/insert/delete methods — this is a fixed seed table, read-only from app code.

#### R11.2: Seed Data in Room Migration

The Room migration v31→v32 SHALL insert the same 9 seed rows idempotently using `INSERT OR IGNORE INTO categorias_producto(id, nombre, familia, orden) VALUES (...)`.

---

### R12: Room `GastoOperativoEntity`

A new Room entity SHALL exist at `data/gastooperativo/GastoOperativoEntity.kt`:

| Column | Type | Room annotation |
|--------|------|-----------------|
| `id` | `String` | `@PrimaryKey` |
| `opticaId` | `String` | — |
| `categoria` | `String` | — |
| `descripcion` | `String` | — |
| `monto` | `Double` | — |
| `fecha` | `LocalDate` | (TypeConverter handles TEXT ↔ LocalDate) |
| `fechaProgramada` | `LocalDate?` | nullable |
| `nota` | `String?` | nullable |
| `createdAt` | `String?` | nullable |

Table name: `gastos_operativos`.

Index: `CREATE INDEX index_gastos_operativos_opticaId ON gastos_operativos(opticaId)`.

#### R12.1: `GastoOperativoDao`

| Method | Return | Description |
|--------|--------|-------------|
| `getByOptica(opticaId)` | `Flow<List<GastoOperativoEntity>>` | Reactive — all expenses for optica |
| `getByOpticaAndDateRange(opticaId, start, end)` | `Flow<List<GastoOperativoEntity>>` | Filtered by date range |
| `getById(id)` | `suspend fun`: `GastoOperativoEntity?` | Single lookup |
| `upsert(entity)` | `suspend fun` | `@Upsert` — insert or replace |
| `deleteById(id)` | `suspend fun` | Delete single expense |

---

### R13: Room `ResumenDiarioEntity`

A new Room entity SHALL exist at `data/resumendiario/ResumenDiarioEntity.kt`:

| Column | Type | Room annotation |
|--------|------|-----------------|
| `id` | `String` | `@PrimaryKey` |
| `opticaId` | `String` | — |
| `fecha` | `LocalDate` | — |
| `ventasCantidad` | `Int` | `DEFAULT 0` |
| `ventasMontoTotal` | `Double` | `DEFAULT 0.0` |
| `ventasCostoTotal` | `Double` | `DEFAULT 0.0` |
| `cobrosCantidad` | `Int` | `DEFAULT 0` |
| `cobrosMontoTotal` | `Double` | `DEFAULT 0.0` |
| `saldoPendienteTotal` | `Double` | `DEFAULT 0.0` |
| `saldoPendienteCantidad` | `Int` | `DEFAULT 0` |
| `inventarioValor` | `Double?` | nullable |
| `inventarioUnidades` | `Int?` | nullable |
| `calculadoEn` | `String?` | nullable — ISO timestamp as String |

Table name: `resumen_diario`.

Index: `CREATE UNIQUE INDEX index_resumen_diario_opticaId_fecha ON resumen_diario(opticaId, fecha)`.

#### R13.1: `ResumenDiarioDao`

| Method | Return | Description |
|--------|--------|-------------|
| `getByOpticaAndDateRange(opticaId, start, end)` | `Flow<List<ResumenDiarioEntity>>` | Reactive — ordered by fecha DESC |
| `getByOpticaAndFecha(opticaId, fecha)` | `suspend fun`: `ResumenDiarioEntity?` | Single day lookup |
| `getAllByOptica(opticaId)` | `suspend fun`: `List<ResumenDiarioEntity>` | Snapshot for sync reconciliation |
| `upsert(entity)` | `suspend fun` | `@Upsert` — called from DownloadSyncCoordinator |
| `deleteAll()` | `suspend fun` | Clear local cache |

**No upload sync.** The `upsert` method exists only for download-side persistence.

---

### R14: Room `ConfiguracionFinancieraEntity`

A new Room entity SHALL exist at `data/configuracionfinanciera/ConfiguracionFinancieraEntity.kt`:

| Column | Type | Room annotation |
|--------|------|-----------------|
| `opticaId` | `String` | `@PrimaryKey` |
| `margenNetoObjetivo` | `Double?` | nullable |
| `ticketPromedioObjetivo` | `Double?` | nullable |
| `caidaVentasAlertaPct` | `Double?` | nullable |
| `deudaViejaAlertaDias` | `Int?` | nullable |
| `deudaTotalAlertaMonto` | `Double?` | nullable |
| `stockEstancadoAlertaDias` | `Int?` | nullable |
| `stockBajoAlertaUnidades` | `Int?` | nullable |
| `minVentasParaRecomendar` | `Int?` | nullable |
| `frecuenciaRecalculoDias` | `Int?` | nullable |

Table name: `configuracion_financiera`.

#### R14.1: `ConfiguracionFinancieraDao`

| Method | Return | Description |
|--------|--------|-------------|
| `getByOptica(opticaId)` | `suspend fun`: `ConfiguracionFinancieraEntity?` | Single-row lookup |
| `upsert(entity)` | `suspend fun` | `@Upsert` — called from download sync only |

Read-only from app code. No custom upsert path for user-initiated saves.

---

### R15: Room Migration v31→v32

A migration `MIGRATION_31_32` SHALL exist in `OptoDatabaseMigrations.kt`:

1. **CREATE TABLE `categorias_producto`**
2. **Seed data** (idempotent — 9 INSERT OR IGNORE statements)
3. **ALTER TABLE `ventas` ADD COLUMN categoriaProductoId**
4. **CREATE TABLE `gastos_operativos`**
5. **CREATE TABLE `resumen_diario`**
6. **CREATE TABLE `configuracion_financiera`**
7. **CREATE INDEX statements**

#### R15.1: OptoDatabase Version and Registration

- Bump `version = 32` in `@Database` annotation.
- Add 4 entity classes to `entities` array.
- Add 4 abstract DAO methods.
- Register `MIGRATION_31_32` in `.addMigrations()`.
- Add companion re-export.

#### R15.2: Data Preservation

The migration SHALL use `CREATE TABLE IF NOT EXISTS` and `ALTER TABLE ... ADD COLUMN` syntax that preserves all existing data.

---

### R16: Hilt DI — DatabaseModule

`DatabaseModule` SHALL provide `@Provides` methods for `CategoriaProductoDao`, `GastoOperativoDao`, `ResumenDiarioDao`, and `ConfiguracionFinancieraDao`.

---

### R17: OptoRepository — Passthrough Methods

`OptoRepository` SHALL add passthrough methods:

| Method | Delegates To | Sync Triggered? |
|--------|-------------|-----------------|
| `suspend fun upsertGastoOperativo(entity)` | `gastoOperativoDao.upsert(entity)` | **Yes** → `PostSaveSyncScheduler.scheduleFinanzasSync()` |
| `suspend fun upsertGastoOperativoFromRemote(entity)` | `gastoOperativoDao.upsert(entity)` | **No** |
| `suspend fun upsertResumenDiarioFromRemote(entity)` | `resumenDiarioDao.upsert(entity)` | **No** |
| `suspend fun upsertConfiguracionFinancieraFromRemote(entity)` | `configuracionFinancieraDao.upsert(entity)` | **No** |
| `suspend fun getCategoriasProducto()` | `categoriaProductoDao.getAll()` | N/A |

---

### R18: SyncFinanzasDto — New Remote DTOs

- `ResumenDiarioRemota` — `@Serializable` DTO with `@SerialName` mappings and `toEntity()`.
- `ConfiguracionFinancieraRemota` — same pattern.
- `GastoOperativoRemota` — with `toEntity()` and `toRemoto()` for upload path.
- `VentaRemota` — add `@SerialName("categoria_producto_id") val categoriaProductoId: String? = null`.
- `FinanzasSyncResult` — add `uploadedGastosOperativos: Int = 0`, `downloadedResumenesDiarios: Int = 0`, `downloadedConfiguracionesFinancieras: Int = 0`.

---

### R19: DownloadSyncCoordinator — New Download Methods

- `downloadResumenDiario(opticaId: String): Int` — queries `resumen_diario`, persists via repository.
- `downloadConfiguracionFinanciera(opticaId: String): Int` — queries `configuracion_financiera` with `maybeSingle()`.

---

### R20: SyncFinanzasUseCase — Download Flow Integration

Both new downloads SHALL be called in the `if (downloadAfterUpload)` block. The `FinanzasSyncResult` SHALL include all new counters.

---

### R21: Sync Order Guarantee

The download order SHALL be: arqueo_caja → dispensaciones → dispensacion_items → servicios_extra → ventas → **resumen_diario** → **configuracion_financiera** → pagos

---

### R22: Upload Sync for New Entities

`UploadSyncCoordinator` SHALL include `uploadGastosOperativos()` following existing patterns. No upload methods for `resumen_diario`, `configuracion_financiera`, or other read-only tables.

---

## Scenarios

### Scenario: Supabase categorias_producto table creation
```
GIVEN the Supabase project is at the latest migration
 WHEN migration is applied
 THEN a table `public.categorias_producto` exists
  AND it has columns id, nombre, familia, orden
  AND the CHECK constraint on familia permits only 'lente', 'montura', 'servicio'
  AND 9 rows are seeded with correct id, nombre, familia, orden values
  AND re-running the migration does not duplicate seed rows (ON CONFLICT DO NOTHING)
```

### Scenario: RLS policies applied to categorias_producto
```
GIVEN the categorias_producto table exists with RLS enabled
 WHEN the migration is applied
 THEN SELECT policy allows all authenticated users (USING true)
  AND INSERT/DELETE policies allow only admin
```

### Scenario: ventas.categoria_producto_id column added
```
GIVEN the ventas table exists
 WHEN migration is applied
 THEN ventas has column categoria_producto_id (TEXT, nullable, FK to categorias_producto)
  AND an index exists on ventas(categoria_producto_id)
  AND existing rows have NULL in the new column
```

### Scenario: Supabase gastos_operativos table creation
```
GIVEN the Supabase project has no gastos_operativos table
 WHEN migration is applied
 THEN a table `public.gastos_operativos` exists
  AND it has columns id, optica_id, categoria, descripcion, monto, fecha, fecha_programada, nota, created_at
  AND the CHECK constraint on categoria permits only:
   'alquiler', 'servicios', 'personal', 'proveedores', 'insumos', 'marketing', 'impuestos', 'otro'
  AND an index exists on (optica_id, fecha)
```

### Scenario: RLS policies on gastos_operativos
```
GIVEN gastos_operativos table exists with RLS enabled
 WHEN the migration is applied
 THEN a SELECT policy exists allowing any optica member
  AND INSERT/UPDATE policies exist for admin and gerente
  AND a DELETE policy exists for admin only
```

### Scenario: Supabase resumen_diario table creation
```
GIVEN the Supabase project is at the latest migration
 WHEN migration is applied
 THEN a table `public.resumen_diario` exists
  AND it has UNIQUE constraint on (optica_id, fecha)
  AND an index exists on (optica_id, fecha)
  AND SELECT policy allows any optica member
```

### Scenario: Supabase costos_productos table creation
```
GIVEN the Supabase project is at the latest migration
 WHEN migration is applied
 THEN a table `public.costos_productos` exists
  AND a partial index exists on (optica_id, categoria_producto_id) WHERE vigente_hasta IS NULL
  AND SELECT policy allows any optica member
  AND INSERT/UPDATE policies allow admin and gerente
```

### Scenario: Supabase configuracion_financiera table creation
```
GIVEN the Supabase project is at the latest migration
 WHEN migration is applied
 THEN a table `public.configuracion_financiera` exists
  AND it has optica_id as PRIMARY KEY
  AND SELECT policy allows any optica member
  AND INSERT/UPDATE policies allow admin and gerente
```

### Scenario: recalcular_resumen_diario function exists
```
GIVEN the migration has been applied
 WHEN `recalcular_resumen_diario('o1', '2026-07-05')` is called
 THEN resumen_diario gets a row for optica_id='o1' and fecha='2026-07-05'
  AND calling the function twice does not duplicate the row
  AND calling the function after new ventas updates the existing row
```

### Scenario: Room migration v31→v32 runs on existing DB
```
GIVEN a device has OptoDatabase at version 31 with existing data
 WHEN MIGRATION_31_32 runs
 THEN 4 new tables are created
  AND ventas table gains column categoriaProductoId (TEXT, nullable)
  AND all existing data in pre-v31 tables is preserved
  AND categorias_producto has 9 seed rows
```

### Scenario: Room CategoriaProductoDao queries
```
GIVEN CategoriaProductoDao is available in the database
 WHEN getAll() is called
 THEN a List<CategoriaProductoEntity> with 9 rows is returned, ordered by orden ASC
 WHEN getById('lente_progresivo') is called
 THEN the matching entity is returned
 WHEN getById('non_existent') is called
 THEN null is returned
```

### Scenario: Room GastoOperativoDao CRUD
```
GIVEN GastoOperativoDao is available in the database
 WHEN upsert() is called with a new entity THEN it is inserted
 WHEN getByOptica() is called THEN a Flow emits the list
 WHEN upsert() is called with an existing ID THEN the row is updated
 WHEN deleteById() is called THEN the row is removed
```

### Scenario: Room ResumenDiarioDao — download-only read
```
GIVEN ResumenDiarioDao is available with seeded rows
 WHEN getByOpticaAndDateRange() is called
 THEN a Flow<List<ResumenDiarioEntity>> is returned, ordered by fecha DESC
 WHEN upsert() is called with a remote row
 THEN it is inserted or replaced without triggering any sync scheduler
```

### Scenario: Room ConfiguracionFinancieraDao — read-only
```
GIVEN ConfiguracionFinancieraDao is available
 WHEN getByOptica(opticaId) is called
 THEN the single-row entity is returned, or null if not configured
```

### Scenario: Hilt DI provides new DAOs
```
GIVEN the Hilt component graph is initialized
 WHEN DatabaseModule is processed
 THEN @Provides methods exist for all 4 new DAOs
```

### Scenario: DownloadSyncCoordinator downloads resumen_diario
```
GIVEN a sync cycle runs via SyncFinanzasUseCase
 WHEN downloadAfterUpload = true
 THEN downloadResumenDiario() is called
  AND each downloaded row is persisted via repository
```

### Scenario: DownloadSyncCoordinator downloads configuracion_financiera
```
GIVEN a sync cycle runs via SyncFinanzasUseCase
 WHEN downloadAfterUpload = true
 THEN downloadConfiguracionFinanciera() is called
  AND if a row exists, it is persisted via repository
```

---

### R23: Supabase RPC `rpc_analisis_mensual`

The system SHALL create `public.rpc_analisis_mensual(p_optica_id TEXT, p_mes DATE) RETURNS jsonb LANGUAGE plpgsql SECURITY INVOKER STABLE`.

The function SHALL compute CORE financial indicators for the given month by reading from `resumen_diario` and `gastos_operativos`.

| Indicator | Key | Source |
|-----------|-----|--------|
| Monthly sales | `ventas_mes` | `resumen_diario.ventas_monto_total` SUM |
| Monthly collections | `cobros_mes` | `resumen_diario.cobros_monto_total` SUM |
| Monthly cost | `costo_mes` | `resumen_diario.ventas_costo_total` SUM |
| Monthly expenses | `gastos_mes` | `gastos_operativos.monto` SUM |
| Pending balance | `saldo_pendiente` | Latest `resumen_diario.saldo_pendiente_total` |
| Net margin % | `margen_neto_pct` | `(ventas - costos - gastos) / ventas * 100` |
| Average ticket | `ticket_promedio` | `ventas_mes / cantidad_ventas` |
| Sales count | `cantidad_ventas` | `resumen_diario.ventas_cantidad` SUM |
| Previous month sales | `ventas_mes_anterior` | `resumen_diario.ventas_monto_total` SUM for previous month |
| Sales variation % | `variacion_ventas_pct` | `(ventas - anterior) / anterior * 100` |
| Historical months | `meses_historicos` | `COUNT(DISTINCT DATE_TRUNC('month', fecha))` from `resumen_diario` for `p_optica_id` |

(Previously: 10 indicators, no `meses_historicos`.)

The function SHALL ALSO compute inline:

- **`margen_por_categoria`** (JSONB array): revenue per category from `dispensaciones` + `servicios_extra`, mapping `tipo_lente + material_lente` to `categoria_producto_id` via CASE. Each row SHALL include `categoria`, `ventas`, `costos=0`, `margen_pct=null` (cost entry deferred to Slice 2).

- **`stock_estancado`** (JSONB array): unsold products from `monturas` LEFT JOIN `montura_movimientos (tipo='SALIDA_VENTA')` and `dispensaciones.montura_id`. `dias_sin_venta` SHALL be `CURRENT_DATE - MAX(fecha)` for sold, 999 for never-sold. `ultima_venta` SHALL be the real date or null. The low-stock filter (`stock_actual <= stock_minimo`) SHALL be removed.

(Previously: not computed by this RPC; `stock_estancado` used low-stock filter with hardcoded 999.)

#### R23.1: RPC Security

`REVOKE EXECUTE ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) FROM public, anon; GRANT EXECUTE ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) TO authenticated, service_role;`

#### Scenario: margen_por_categoria returns real revenue from inline computation

- GIVEN dispensaciones with `tipo_lente='monofocal', material_lente='resina_stock'` for July 2026
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN `margen_por_categoria` contains a row with non-zero `ventas` for the mapped `categoria_producto_id`
- AND the mapping from `(tipo_lente, material_lente)` to `categoria` follows the CASE expression

#### Scenario: stock_estancado shows computed dias_sin_venta for sold monturas

- GIVEN a montura with a SALIDA_VENTA movimiento on 2026-03-15
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN that montura appears in `stock_estancado` with real `diasSinVenta` and `ultimaVenta = "2026-03-15"`

#### Scenario: never-sold montura shows 999 days and null date

- GIVEN a montura with no SALIDA_VENTA and no `dispensaciones.montura_id` reference
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN that montura has `diasSinVenta = 999` and `ultimaVenta = null`

#### Scenario: no sales data returns zero rows in margen_por_categoria

- GIVEN an optica has zero dispensaciones and zero servicios_extra for July 2026
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN `margen_por_categoria` contains 9 rows (one per `categorias_producto`) with `ventas = 0, costos = 0, margen_pct = null`

#### Scenario: meses_historicos returns correct count

- GIVEN `resumen_diario` has rows for 5 distinct months (2026-03 through 2026-07) for optica 'o1'
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN the returned JSON includes `meses_historicos = 5`

#### Scenario: meses_historicos counts only months with data

- GIVEN `resumen_diario` has zero rows for optica 'o1' (never synced or calculated)
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN `meses_historicos = 0`

---

### R24: Supabase RPC `rpc_deudores`

The system SHALL create `public.rpc_deudores(p_optica_id TEXT) RETURNS TABLE(paciente_nombre TEXT, paciente_telefono TEXT, venta_id TEXT, venta_fecha DATE, monto_total NUMERIC, total_pagado NUMERIC, saldo NUMERIC, dias_deuda INTEGER) LANGUAGE sql SECURITY INVOKER STABLE`.

JOIN `ventas` LEFT JOIN `pagos` LEFT JOIN `pacientes`, HAVING `saldo > 0.005`, ORDER BY `dias_deuda DESC`.

#### R24.1: RPC Security

`REVOKE EXECUTE ON FUNCTION public.rpc_deudores(TEXT) FROM public, anon; GRANT EXECUTE ON FUNCTION public.rpc_deudores(TEXT) TO authenticated, service_role;`

---

### R25: Update `rpc_count_pendientes` to Query `ventas`

The system SHALL rewrite `rpc_count_pendientes` to query `public.ventas` instead of old `dispensaciones` + `servicios_extra`. The function signature and return type SHALL remain unchanged (RETURNS jsonb).

- Overdue deliveries: `ventas` WHERE `estado = 'Pendiente' AND fecha < CURRENT_DATE`
- Unpaid balance: `ventas` LEFT JOIN aggregated `pagos` WHERE `monto_total - COALESCE(total_pagado, 0) > 0.005` AND `estado IS DISTINCT FROM 'Anulado'`

---

### R26: Deprecate `rpc_resumen_financiero` and `rpc_saldo_pendiente`

The system SHALL add deprecation comments to both functions using `COMMENT ON FUNCTION ... IS 'DEPRECATED: Use rpc_analisis_mensual instead. This function remains for backward compatibility.'`.

The functions SHALL NOT be dropped — they remain callable for backward compatibility.

---

### R27: GRANT EXECUTE on `recalcular_resumen_diario` (Fix from Fase 6)

The system SHALL execute `GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario(TEXT, DATE) TO authenticated, service_role` and `REVOKE EXECUTE FROM public, anon`. This grant was missing from the Fase 6 migration, causing permission errors on client-side calls.

---

### R28: Room `ResumenDiarioDao` — Monthly Aggregation Query

Add to R13.1 the following method:

| Method | Return | Description |
|--------|--------|-------------|
| `getByOpticaAndMonth(opticaId, yearMonth)` | `suspend fun`: `List<ResumenDiarioEntity>` | Filter by `strftime('%Y-%m', fecha) = yearMonth`, ordered by `fecha ASC` |

This enables offline calculation of monthly aggregates from local Room data when Supabase RPC is unreachable.

---

### R29: Room Migration v32→v33 — Add `ventaId` to `Pago`

A migration `MIGRATION_32_33` SHALL exist in `OptoDatabaseMigrations.kt`:

1. `ALTER TABLE pagos ADD COLUMN ventaId TEXT`
2. `CREATE INDEX IF NOT EXISTS index_pagos_ventaId ON pagos(ventaId)`

The `Pago` entity SHALL add `val ventaId: String? = null` with `@SerialName("ventaId")`. The field SHALL be nullable with default null — existing constructors continue to compile.

Database version SHALL be bumped from 32 to 33. The migration SHALL be registered in `.addMigrations()` and re-exported in the `OptoDatabase` companion object.

---

### R30: Android Category Values Match DB CHECK Constraint

The `GastosViewModel.categorias` list MUST contain exactly the 8 values permitted by the PostgreSQL CHECK constraint on `gastos_operativos.categoria`: `alquiler`, `servicios`, `personal`, `proveedores`, `insumos`, `marketing`, `impuestos`, `otro`. No other value SHALL be present in the list. The default `categoria` in `GastosUiState` MUST be `"alquiler"`.

#### Scenario: ViewModel category list is DB-compliant

```
GIVEN the GastosViewModel.categorias list is defined
 WHEN the ViewModel is first initialized
 THEN the list contains exactly ["alquiler", "servicios", "personal", "proveedores", "insumos", "marketing", "impuestos", "otro"]
  AND every value in the list is a member of the DB CHECK constraint set
```

#### Scenario: Default categoria is a valid DB value

```
GIVEN a new GastosUiState instance is created with default values
 WHEN the default categoria field is read
 THEN it equals "alquiler"
  AND it is a member of the DB CHECK constraint set
```

### R31: Sync Upload Succeeds With Valid Category

The `UploadSyncCoordinator.uploadGastosOperativos()` flow MUST succeed when a `GastoOperativoEntity` has a `categoria` value that matches the DB CHECK constraint. The failure mode for entities with non-compliant categories MUST NOT change — they SHALL continue to fail with a CHECK constraint violation, same as before this change.

#### Scenario: Valid category uploads successfully

```
GIVEN a GastoOperativoEntity with categoria = "alquiler" is pending upload
 WHEN UploadSyncCoordinator.uploadGastosOperativos() processes it
 THEN the Supabase INSERT completes without CHECK constraint error
  AND the entity is marked as synced
```

#### Scenario: Old invalid category still fails (no regression)

```
GIVEN a GastoOperativoEntity saved with the old categoria value "Local" is pending upload
 WHEN UploadSyncCoordinator.uploadGastosOperativos() processes it
 THEN the Supabase INSERT fails with a CHECK constraint violation
  AND the failure mode is identical to the current (pre-fix) behavior
```

---

### R32: ProyeccionCaja — mesesHistoricos Field

The Android domain model `ProyeccionCaja` (in `domain/`) SHALL gain a field `mesesHistoricos: Int` with default value `0`.

The field SHALL be populated from the `meses_historicos` value returned by `rpc_analisis_mensual`. The RPC response JSONB deserializer SHALL map `"meses_historicos"` to `ProyeccionCaja.mesesHistoricos`.

#### Scenario: RPC response with meses_historicos

- GIVEN `rpc_analisis_mensual` returns `{"meses_historicos": 5, ...}`
- WHEN the response is deserialized to `ProyeccionCaja`
- THEN `proyeccionCaja.mesesHistoricos == 5`

#### Scenario: Default when missing from response

- GIVEN `rpc_analisis_mensual` returns a JSON without the `meses_historicos` key (backward compatibility)
- WHEN the response is deserialized to `ProyeccionCaja`
- THEN `proyeccionCaja.mesesHistoricos == 0`

---

### R33: ProyeccionCard — Data-Depth Warning

The `ProyeccionCard` composable SHALL display a warning banner when `mesesHistoricos < 3`.

The warning SHALL contain user-facing text indicating that projections are based on limited data (fewer than 3 months). When `mesesHistoricos >= 3`, no warning SHALL be displayed. The decision SHALL be driven by the `ProyeccionCaja.mesesHistoricos` value — no separate RPC or query is needed.

#### Scenario: Warning shown for insufficient data

- GIVEN `ProyeccionCaja.mesesHistoricos == 1` (only 1 month of data)
- WHEN `ProyeccionCard` renders
- THEN a warning banner is visible with text referencing limited data depth

#### Scenario: No warning when data is sufficient

- GIVEN `ProyeccionCaja.mesesHistoricos == 5` (5 months of data)
- WHEN `ProyeccionCard` renders
- THEN no warning banner is shown

#### Scenario: Edge case — exactly 2 months

- GIVEN `ProyeccionCaja.mesesHistoricos == 2`
- WHEN `ProyeccionCard` renders
- THEN a warning banner is visible (2 < 3)

#### Scenario: Edge case — exactly 3 months

- GIVEN `ProyeccionCaja.mesesHistoricos == 3`
- WHEN `ProyeccionCard` renders
- THEN no warning banner is shown (3 >= 3)

---

## Delta Requirements (fix-error-mi-negocio)

### REQ-1: Error Deduplication

The error accumulator used in the ViewModel MUST produce at most one occurrence of each distinct error message, regardless of how many sources produce the same text.

#### Scenario: Same error from multiple paths appears once

```
GIVEN two data sources both fail with the identical error message "No se pudieron cargar los datos"
 WHEN the ViewModel collects errors from both sources
 THEN the resulting error set contains exactly one occurrence of that message
  AND the error set size equals the count of distinct error messages
```

#### Scenario: Distinct error messages are preserved

```
GIVEN three data sources each fail with different messages
 WHEN the ViewModel collects all errors
 THEN the resulting error set contains all three distinct messages
  AND no message is lost
```

**Test type**: unit (ViewModel with fake failing use cases)

---

### REQ-2: User-Facing Error Messages Are Static

Every domain-layer use case that produces a user-facing error message MUST return a static, user-friendly string. Raw exception text, JSON bodies, and `e.localizedMessage` MUST NOT appear in the error message visible to the UI.

The full exception (message + stack trace) MUST be logged via `Log.e` to Logcat for diagnostics.

#### Scenario: Supabase RPC failure shows static message

```
GIVEN the Supabase RPC `rpc_analisis_mensual` returns HTTP 400 with verbose JSON body
 WHEN `ObtenerAnalisisMensualUseCase` catches the exception
 THEN the returned `Resource.Error` message is static text
  AND the message does NOT contain "400", "JSON", "localizedMessage", or any raw exception text
  AND the full exception is written to Logcat via `Log.e`
```

#### Scenario: Network failure in deudores shows static message

```
GIVEN the Supabase RPC `rpc_deudores` throws a network exception
 WHEN `ObtenerDeudoresUseCase` catches the exception
 THEN the returned `Resource.Error` message is static text
  AND the full exception is written to Logcat via `Log.e`
```

**Test type**: unit (use case with a fake failing Supabase client + Logcat spy/verification)

---

### REQ-3: `LocalDate.parse` Is Null-Safe for Nullable RPC Fields

The `ObtenerDeudoresUseCase` MUST handle a null or missing `venta_fecha` field in the `rpc_deudores` response without throwing `DateTimeParseException`. When `venta_fecha` is null or empty, the corresponding `Deudor.ventaFecha` field MUST be set to `LocalDate.MIN` and the processing of the remaining rows MUST continue.

#### Scenario: Null venta_fecha does not crash

```
GIVEN the `rpc_deudores` response includes a row with `venta_fecha = null`
 WHEN `ObtenerDeudoresUseCase` parses that row
 THEN the row is included in the result list
  AND `Deudor.ventaFecha` is set to `LocalDate.MIN` for that row
  AND the remaining rows are successfully parsed without exception
```

#### Scenario: Empty venta_fecha does not crash

```
GIVEN the `rpc_deudores` response includes a row with `venta_fecha = ""` (empty string)
 WHEN `ObtenerDeudoresUseCase` parses that row
 THEN `Deudor.ventaFecha` is set to `LocalDate.MIN` for that row
  AND the remaining rows are successfully parsed without exception
```

#### Scenario: Valid venta_fecha parses normally

```
GIVEN the `rpc_deudores` response includes a row with `venta_fecha = "2026-07-01"`
 WHEN `ObtenerDeudoresUseCase` parses that row
 THEN `Deudor.ventaFecha` equals `LocalDate.of(2026, 7, 1)`
```

**Test type**: unit (use case with a fake PreloadedRpcResponse returning controlled data)

---

### REQ-4: `CancellationException` MUST Be Rethrown

The ViewModel data-loading coroutine MUST rethrow `CancellationException` instead of catching it in a generic handler. When the coroutine scope is cancelled (e.g., user navigates away), the error card MUST NOT display a spurious "Error inesperado" message, and the coroutine hierarchy MUST be properly cancelled.

#### Scenario: Navigation away does not show spurious error

```
GIVEN the AnalisisNegocioScreen is visible and loading data
 WHEN the user navigates away before data loading completes
 THEN no "Error inesperado" or similar generic error text appears in the UI
  AND the coroutine hierarchy completes cancellation without raising unhandled exceptions
```

#### Scenario: runCatching does not swallow CancellationException

```
GIVEN the ViewModel wraps the data-loading block in error handling
 WHEN a `CancellationException` is thrown inside that block
 THEN the exception is rethrown and propagates up
  AND it is NOT caught by any generic `catch (e: Exception)` or `.onFailure` handler
```

**Test type**: unit (ViewModel with `TestCoroutineDispatcher`, cancelled scope, verify no error state emitted)

---

### REQ-5: `GenerarRecomendacionesUseCase` Accepts Pre-Fetched Data

The use case MUST accept `AnalisisMensual` and `List<Deudor>` data objects as input parameters. It MUST NOT call `ObtenerAnalisisMensualUseCase` or `ObtenerDeudoresUseCase` internally. The business logic (evaluating and generating recommendations) MUST remain identical.

#### Scenario: Pre-fetched data produces same recommendations

```
GIVEN an AnalisisMensual object, a List<Deudor>, and a ConfiguracionFinancieraEntity
 WHEN `GenerarRecomendacionesUseCase.invoke(analisis, deudores, opticaId)` is called
 THEN a `Resource<List<Recomendacion>>` is returned
  AND the recommendations are computed using only the provided data (no additional RPC calls)
```

#### Scenario: Null/empty pre-fetched data returns error gracefully

```
GIVEN `analisis` is null AND `deudores` is null
 WHEN `GenerarRecomendacionesUseCase.invoke(null, null, opticaId)` is called
 THEN a `Resource.Error` is returned
  AND no RPC calls are made
```

**Test type**: unit (use case with injected data, verify with a mock that no RPC calls occur)

---

### REQ-6: ViewModel Orchestration — Data Passing

The ViewModel MUST orchestrate two parallel data calls (`obtenerAnalisisMensual`, `obtenerDeudores`), then pass their results to `generarRecomendaciones`. Errors from all three sources MUST be collected into a unique set.

#### Scenario: All calls succeed

```
GIVEN both `obtenerAnalisisMensual` and `obtenerDeudores` return `Resource.Success`
 WHEN the ViewModel loads data
 THEN `generarRecomendaciones` is called with the fetched `analisis` and `deudores` data
  AND `uiState.analisis` is set to the fetched analisis data
  AND `uiState.deudores` is set to the fetched deudores data
  AND `uiState.error` is null
```

#### Scenario: One call fails, recommendations still computed

```
GIVEN `obtenerAnalisisMensual` returns `Resource.Success` with valid data
  AND `obtenerDeudores` returns `Resource.Error`
 WHEN the ViewModel loads data
 THEN `generarRecomendaciones` is called with `analisis = null` and `deudores = null`
  AND `uiState.deudores` is an empty list
  AND `uiState.error` includes the deudores error message
  AND `uiState.analisis` is set to the fetched analisis data
```

#### Scenario: Both calls fail

```
GIVEN both `obtenerAnalisisMensual` and `obtenerDeudores` return `Resource.Error`
 WHEN the ViewModel loads data
 THEN `uiState.error` contains both error messages (once each)
  AND `uiState.analisis` and `uiState.deudores` are null/empty
  AND recommendations are not computed (or return error)
```

**Test type**: unit (ViewModel with controlled fake use cases, verify state emissions)

---

### REQ-7: No Redundant RPC Calls from ViewModel

Two views affected by the same RPC response MUST NOT make duplicate calls to that RPC. If `generarRecomendaciones` depends on the same data as the primary data calls, the ViewModel MUST pass that data instead of letting the use case re-fetch it.

This requirement amends the implicit behavior of the existing `analisis-negocio` Android flow. The base spec remains unchanged in its data schema and RPC contracts.

#### Scenario: rpc_analisis_mensual is called at most once per load

```
GIVEN the ViewModel initiates a data load
 WHEN all data loading completes
 THEN `rpc_analisis_mensual` or its local equivalent is called exactly once
  AND `rpc_deudores` is called exactly once
```

**Test type**: integration or unit (count Supabase client invocations via mock)

---

## Delta Scenarios (Fase 7)

### Scenario: rpc_analisis_mensual returns CORE indicators
```
GIVEN an optica has resumen_diario data and gastos for July 2026
 WHEN rpc_analisis_mensual('o1', '2026-07-01') is called
 THEN a JSONB object is returned with all 10 CORE keys as non-null values
  AND ventas_mes matches monthly SUM from resumen_diario
```

### Scenario: rpc_analisis_mensual handles empty month
```
GIVEN an optica has no data for a month
 WHEN rpc_analisis_mensual('o1', '2026-08-01') is called
 THEN all numeric values return 0
  AND margen_neto_pct returns 0 (not error)
```

### Scenario: rpc_deudores returns debtors by aging
```
GIVEN 3 ventas with partial payments, oldest 60 days ago
 WHEN rpc_deudores('o1') is called
 THEN 3 rows returned ordered by dias_deuda DESC
  AND each has saldo > 0
```

### Scenario: rpc_deudores returns empty for no debtors
```
GIVEN all ventas fully paid
 WHEN rpc_deudores('o1') is called
 THEN empty result set returned
```

### Scenario: rpc_count_pendientes counts from ventas
```
GIVEN 5 ventas with pending balance
 WHEN rpc_count_pendientes('o1') is called
 THEN count matches pending from ventas table
```

### Scenario: Deprecated RPCs still execute
```
GIVEN deprecation migration applied
 WHEN old RPC is called
 THEN it executes normally (no error)
  AND function body has deprecation comment
```

### Scenario: recalcular_resumen_diario GRANT fix
```
GIVEN recalcular_resumen_diario exists
 WHEN an authenticated user calls it
 THEN the function executes without permission error
```

### Scenario: ResumenDiarioDao monthly filter
```
GIVEN 30 daily rows for July 2026 in Room
 WHEN getByOpticaAndMonth('o1', '2026-07') is called
 THEN 30 rows returned for client-side aggregation
```

### Scenario: MIGRATION_32_33 preserves existing Pago data
```
GIVEN a device has OptoDatabase at version 32 with 50 Pago rows
 WHEN MIGRATION_32_33 runs
 THEN all 50 rows preserved
  AND ventaId column exists with NULL for existing rows
  AND index_pagos_ventaId exists
```

---

## Out of Scope

- Fase 7: Business indicator calculations and UI screens
- Fase 8: Recommendation engine
- Fase 9: Financial configuration screens
- `MargenPorCategoria` Room entity (server-side only)
- `FeedbackRecomendaciones` Room entity (web-only)
- `FeedbackRecomendaciones` Room entity (web-only)
- Upload sync for `resumen_diario` and `configuracion_financiera`
- Snapshot coordinator changes (`SyncSnapshotCoordinator`)
- RPC for pre-calculating `margen_por_categoria`
- UI for assigning `categoria_producto_id` at venta creation time
