# Delta Spec: Fase 6 — Esquema de datos para análisis de negocio

## Overview

Create the foundational data schema for business analysis — product categorization, margin tracking, daily summaries, operative expenses, and financial configuration. This is the infrastructure layer (Parte B) that enables Fases 7–10 (indicators, recommendations, and UI).

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
| `categorias_producto_select` | SELECT | `app_private.is_optica_member(auth.uid(), optica_id)` — note: this is a multi-tenant seed table, all members can read |
| `categorias_producto_insert` | INSERT | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin'])` |
| `categorias_producto_delete` | DELETE | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin'])` |

Since `categorias_producto` has no `optica_id` column (it's global), the SELECT policy SHALL allow `USING (true)` — all authenticated users can read the master list. INSERT/DELETE SHALL be admin-only.

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

**There is no existing `gastos_operativos` table to ALTER.** This is a full CREATE TABLE. The plan text from `propuesta-ingresos-analisis-financiero.md` section 6.4 describing an ALTER with `DROP CONSTRAINT` is obsolete — that constraint does not exist in the current schema. The implementation SHALL ignore the ALTER described in the original plan and create the table fresh with the CHECK constraint above.

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

The system SHALL CREATE TABLE `public.costos_productos`:

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `UUID` | `PRIMARY KEY DEFAULT gen_random_uuid()` |
| `optica_id` | `TEXT` | `NOT NULL` |
| `categoria_producto_id` | `TEXT` | `NOT NULL`, FK to `categorias_producto(id)` |
| `producto_descripcion` | `TEXT` | nullable — free-text description, e.g. `"Tokai 1.67 Progresivo"` |
| `costo_unitario` | `NUMERIC` | `NOT NULL` |
| `vigente_desde` | `DATE` | `NOT NULL DEFAULT CURRENT_DATE` |
| `vigente_hasta` | `DATE` | nullable — NULL means currently active |
| `fecha_actualizacion` | `TIMESTAMPTZ` | `DEFAULT NOW()` |

**Partial index** (currently active costs only):
```sql
CREATE INDEX idx_costos_vigentes ON public.costos_productos (optica_id, categoria_producto_id)
    WHERE vigente_hasta IS NULL;
```

#### R6.1: RLS on `costos_productos`

| Policy | Command | Using / With Check |
|--------|---------|--------------------|
| `costos_productos_select` | SELECT | `app_private.is_optica_member(auth.uid(), optica_id)` |
| `costos_productos_insert` | INSERT | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente'])` |
| `costos_productos_update` | UPDATE | Same as insert |
| `costos_productos_delete` | DELETE | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin'])` |

#### R6.2: No Room entity for `costos_productos`

This table is server-side master data managed via web companion. No Room entity, DAO, or sync wiring SHALL be created for it.

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
   - `ventas_costo_total` = `COALESCE(SUM(costo_unitario_snapshot), 0)` — NULL-safe via COALESCE

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

A new Room entity SHALL be created at `data/categoriaproducto/CategoriaProductoEntity.kt`:

| Column | Type | Room annotation |
|--------|------|-----------------|
| `id` | `String` | `@PrimaryKey` |
| `nombre` | `String` | — |
| `familia` | `String` | — |
| `orden` | `Int` | `DEFAULT 0` |

Table name: `categorias_producto`.

#### R11.1: `CategoriaProductoDao`

A new DAO SHALL be created at `data/categoriaproducto/CategoriaProductoDao.kt`:

| Method | Return | Description |
|--------|--------|-------------|
| `getAll()` | `suspend fun`: `List<CategoriaProductoEntity>` | Read all categories ordered by `orden` |
| `getById(id)` | `suspend fun`: `CategoriaProductoEntity?` | Single lookup |

No upsert/insert/delete methods — this is a fixed seed table, read-only from app code.

#### R11.2: Seed Data in Room Migration

The Room migration v31→v32 SHALL insert the same 9 seed rows idempotently using `INSERT OR IGNORE INTO categorias_producto(id, nombre, familia, orden) VALUES (...)`.

---

### R12: Room `GastoOperativoEntity`

A new Room entity SHALL be created at `data/gastooperativo/GastoOperativoEntity.kt`:

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

A new Room entity SHALL be created at `data/resumendiario/ResumenDiarioEntity.kt`:

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

**No upload sync.** The `upsert` method exists only for download-side persistence. No `upsertResumenDiario()` method in `OptoRepository` SHALL trigger `PostSaveSyncScheduler`.

---

### R14: Room `ConfiguracionFinancieraEntity`

A new Room entity SHALL be created at `data/configuracionfinanciera/ConfiguracionFinancieraEntity.kt`:

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

A new migration `MIGRATION_31_32` SHALL be added to `OptoDatabaseMigrations.kt`:

1. **CREATE TABLE `categorias_producto`**:
```sql
CREATE TABLE IF NOT EXISTS categorias_producto (
    id TEXT NOT NULL PRIMARY KEY,
    nombre TEXT NOT NULL,
    familia TEXT NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0
)
```

2. **Seed data** (idempotent — 9 INSERT OR IGNORE statements).

3. **ALTER TABLE `ventas`**:
```sql
ALTER TABLE ventas ADD COLUMN categoriaProductoId TEXT DEFAULT NULL
```

4. **CREATE TABLE `gastos_operativos`**:
```sql
CREATE TABLE IF NOT EXISTS gastos_operativos (
    id TEXT NOT NULL PRIMARY KEY,
    opticaId TEXT NOT NULL,
    categoria TEXT NOT NULL,
    descripcion TEXT NOT NULL,
    monto REAL NOT NULL,
    fecha TEXT NOT NULL,
    fechaProgramada TEXT,
    nota TEXT,
    createdAt TEXT
)
```

5. **CREATE TABLE `resumen_diario`**:
```sql
CREATE TABLE IF NOT EXISTS resumen_diario (
    id TEXT NOT NULL PRIMARY KEY,
    opticaId TEXT NOT NULL,
    fecha TEXT NOT NULL,
    ventasCantidad INTEGER NOT NULL DEFAULT 0,
    ventasMontoTotal REAL NOT NULL DEFAULT 0.0,
    ventasCostoTotal REAL NOT NULL DEFAULT 0.0,
    cobrosCantidad INTEGER NOT NULL DEFAULT 0,
    cobrosMontoTotal REAL NOT NULL DEFAULT 0.0,
    saldoPendienteTotal REAL NOT NULL DEFAULT 0.0,
    saldoPendienteCantidad INTEGER NOT NULL DEFAULT 0,
    inventarioValor REAL,
    inventarioUnidades INTEGER,
    calculadoEn TEXT
)
```

6. **CREATE TABLE `configuracion_financiera`**:
```sql
CREATE TABLE IF NOT EXISTS configuracion_financiera (
    opticaId TEXT NOT NULL PRIMARY KEY,
    margenNetoObjetivo REAL,
    ticketPromedioObjetivo REAL,
    caidaVentasAlertaPct REAL,
    deudaViejaAlertaDias INTEGER,
    deudaTotalAlertaMonto REAL,
    stockEstancadoAlertaDias INTEGER,
    stockBajoAlertaUnidades INTEGER,
    minVentasParaRecomendar INTEGER,
    frecuenciaRecalculoDias INTEGER
)
```

7. **CREATE INDEX statements**:
   - `CREATE INDEX IF NOT EXISTS index_gastos_operativos_opticaId ON gastos_operativos(opticaId)`
   - `CREATE UNIQUE INDEX IF NOT EXISTS index_resumen_diario_opticaId_fecha ON resumen_diario(opticaId, fecha)`

#### R15.1: OptoDatabase Version and Registration

- Bump `version = 32` in `@Database` annotation.
- Add `CategoriaProductoEntity::class`, `GastoOperativoEntity::class`, `ResumenDiarioEntity::class`, `ConfiguracionFinancieraEntity::class` to the `entities` array.
- Add `abstract fun categoriaProductoDao(): CategoriaProductoDao`
- Add `abstract fun gastoOperativoDao(): GastoOperativoDao`
- Add `abstract fun resumenDiarioDao(): ResumenDiarioDao`
- Add `abstract fun configuracionFinancieraDao(): ConfiguracionFinancieraDao`
- Register `MIGRATION_31_32` in the `.addMigrations()` chain.
- Add `val MIGRATION_31_32 get() = com.example.optoapp.data.MIGRATION_31_32` companion re-export.

#### R15.2: Data Preservation

The migration SHALL use `CREATE TABLE IF NOT EXISTS` and `ALTER TABLE ... ADD COLUMN` syntax that preserves all existing data in tables `pacientes`, `evaluaciones`, `dispensaciones`, `servicios_extra`, `pagos`, `ventas`, `monturas`, `montura_movimientos`, `sync_entity_state`, `conflict_records`, `proveedores`, `categorias_montura`, `montura_proveedor`, `ordenes_compra`, `orden_compra_items`, `inventario_fisico`, `inventario_fisico_detalle`, `arqueo_caja`, and all other pre-existing tables.

---

### R16: Hilt DI — DatabaseModule

`DatabaseModule` SHALL add the following `@Provides` methods:

```kotlin
@Provides
fun provideCategoriaProductoDao(database: OptoDatabase): CategoriaProductoDao = database.categoriaProductoDao()

@Provides
fun provideGastoOperativoDao(database: OptoDatabase): GastoOperativoDao = database.gastoOperativoDao()

@Provides
fun provideResumenDiarioDao(database: OptoDatabase): ResumenDiarioDao = database.resumenDiarioDao()

@Provides
fun provideConfiguracionFinancieraDao(database: OptoDatabase): ConfiguracionFinancieraDao = database.configuracionFinancieraDao()
```

The `OptoRepository` provider SHALL be updated to accept and pass these new DAOs if they are needed by the repository delegation layer. Alternatively, the repository SHALL access them directly via the database instance (following the existing pattern where `database.ventaDao()` is used in the `provideOptoRepository` method).

---

### R17: OptoRepository — Passthrough Methods

`OptoRepository` SHALL add passthrough methods for the new entities following the existing pattern:

| Method | Delegates To | Sync Triggered? |
|--------|-------------|-----------------|
| `suspend fun upsertGastoOperativo(entity)` | `gastoOperativoDao.upsert(entity)` | **Yes** → `PostSaveSyncScheduler.scheduleFinanzasSync()` |
| `suspend fun upsertGastoOperativoFromRemote(entity)` | `gastoOperativoDao.upsert(entity)` | **No** — remote timestamp-preserving path |
| `suspend fun upsertResumenDiarioFromRemote(entity)` | `resumenDiarioDao.upsert(entity)` | **No** — read-only download |
| `suspend fun upsertConfiguracionFinancieraFromRemote(entity)` | `configuracionFinancieraDao.upsert(entity)` | **No** — read-only download |
| `suspend fun getCategoriasProducto()` | `categoriaProductoDao.getAll()` | N/A (read-only seed) |

---

### R18: SyncFinanzasDto — New Remote DTOs

Two new DTOs SHALL be added to `SyncFinanzasDto.kt`:

#### R18.1: `ResumenDiarioRemota`

```kotlin
@Serializable
data class ResumenDiarioRemota(
    val id: String,
    @SerialName("optica_id") val opticaId: String,
    val fecha: String,
    @SerialName("ventas_cantidad") val ventasCantidad: Int = 0,
    @SerialName("ventas_monto_total") val ventasMontoTotal: Double = 0.0,
    @SerialName("ventas_costo_total") val ventasCostoTotal: Double = 0.0,
    @SerialName("cobros_cantidad") val cobrosCantidad: Int = 0,
    @SerialName("cobros_monto_total") val cobrosMontoTotal: Double = 0.0,
    @SerialName("saldo_pendiente_total") val saldoPendienteTotal: Double = 0.0,
    @SerialName("saldo_pendiente_cantidad") val saldoPendienteCantidad: Int = 0,
    @SerialName("inventario_valor") val inventarioValor: Double? = null,
    @SerialName("inventario_unidades") val inventarioUnidades: Int? = null,
    @SerialName("calculado_en") val calculadoEn: String? = null
) {
    fun toEntity() = ResumenDiarioEntity(...)
}
```

#### R18.2: `ConfiguracionFinancieraRemota`

```kotlin
@Serializable
data class ConfiguracionFinancieraRemota(
    @SerialName("optica_id") val opticaId: String,
    @SerialName("margen_neto_objetivo") val margenNetoObjetivo: Double? = null,
    @SerialName("ticket_promedio_objetivo") val ticketPromedioObjetivo: Double? = null,
    // ... remaining fields with @SerialName mappings
) {
    fun toEntity() = ConfiguracionFinancieraEntity(...)
}
```

#### R18.3: `VentaRemota` — Add `categoriaProductoId`

The existing `VentaRemota` DTO SHALL add:
```kotlin
@SerialName("categoria_producto_id") val categoriaProductoId: String? = null
```

And the `toEntity()` method SHALL pass it through to `Venta.categoriaProductoId`.

#### R18.4: `FinanzasSyncResult` — Add New Counters

```kotlin
val downloadedResumenesDiarios: Int = 0,
val downloadedConfiguracionesFinancieras: Int = 0
```

---

### R19: DownloadSyncCoordinator — New Download Methods

`DownloadSyncCoordinator` SHALL add:

#### R19.1: `downloadResumenDiario(opticaId: String): Int`

Following the exact pattern of `downloadVentas()`:
- Query `supabase.postgrest["resumen_diario"].select { filter { eq("optica_id", opticaId) } }`
- Decode as `List<ResumenDiarioRemota>`
- For each: call `repository.upsertResumenDiarioFromRemote(r.toEntity())`
- Mark sync state via `syncStateTracker.markSynced()`
- Return count of downloaded rows

#### R19.2: `downloadConfiguracionFinanciera(opticaId: String): Int`

- Query `supabase.postgrest["configuracion_financiera"].select { filter { eq("optica_id", opticaId) }.maybeSingle() }`
- Decode as `ConfiguracionFinancieraRemota?` (0 or 1 row)
- If present: call `repository.upsertConfiguracionFinancieraFromRemote(r.toEntity())`
- Return 1 if found, 0 otherwise

#### R19.3: Table Constants

Add to the companion object:
```kotlin
private const val TABLE_RESUMEN_DIARIO = "resumen_diario"
private const val TABLE_CONFIG_FINANCIERA = "configuracion_financiera"
```

---

### R20: SyncFinanzasUseCase — Download Flow Integration

`SyncFinanzasUseCase.invoke()` SHALL add both new downloads to the `if (downloadAfterUpload)` block:

```kotlin
// After downloadVentas() and before downloadPagos():
val resumenDown = downloadSyncCoordinator.downloadResumenDiario(opticaId)
Log.d(TAG, "Finanzas: download resumen_diario=$resumenDown")
val configDown = downloadSyncCoordinator.downloadConfiguracionFinanciera(opticaId)
Log.d(TAG, "Finanzas: download configuracion_financiera=$configDown")
```

The `FinanzasSyncResult` construction SHALL include both counters.

---

### R21: Sync Order Guarantee

The download order SHALL be:
1. arqueo_caja → 2. dispensaciones → 3. dispensacion_items → 4. servicios_extra → 5. ventas → **6. resumen_diario** → **7. configuracion_financiera** → 8. pagos

`resumen_diario` and `configuracion_financiera` are independent tables with no FK dependencies on other synced entities; however, placing them after `ventas` is semantically correct since they aggregate ventas data.

---

### R22: No Upload Sync for New Entities

`UploadSyncCoordinator` SHALL NOT add upload methods for `categorias_producto`, `gastos_operativos`, `resumen_diario`, `configuracion_financiera`, or any other new table. Rationale:

- `categorias_producto`: fixed seed, server-authoritative
- `gastos_operativos`: CRUD from Android → upload sync IS needed for this entity (see R17 — it uses the standard local-upsert + scheduleFinanzasSync pattern). This WILL be implemented in Fase 6.
- `resumen_diario`: read-only download, server-calculated
- `configuracion_financiera`: read-only from Android, managed via web

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
  AND it has columns id, optica_id, fecha, ventas_cantidad, ventas_monto_total, ventas_costo_total,
   cobros_cantidad, cobros_monto_total, saldo_pendiente_total, saldo_pendiente_cantidad,
   inventario_valor, inventario_unidades, calculado_en
  AND UNIQUE constraint exists on (optica_id, fecha)
  AND an index exists on (optica_id, fecha)
  AND SELECT policy allows any optica member
```

### Scenario: Supabase costos_productos table creation

```
GIVEN the Supabase project is at the latest migration
 WHEN migration is applied
 THEN a table `public.costos_productos` exists
  AND it has columns id, optica_id, categoria_producto_id, producto_descripcion,
   costo_unitario, vigente_desde, vigente_hasta, fecha_actualizacion
  AND a partial index exists on (optica_id, categoria_producto_id) WHERE vigente_hasta IS NULL
  AND SELECT policy allows any optica member
  AND INSERT/UPDATE policies allow admin and gerente
```

### Scenario: Supabase configuracion_financiera table creation

```
GIVEN the Supabase project is at the latest migration
 WHEN migration is applied
 THEN a table `public.configuracion_financiera` exists
  AND it has optica_id as PRIMARY KEY (TEXT)
  AND it has columns margen_neto_objetivo (DEFAULT 15.0), caida_ventas_alerta_pct (DEFAULT 10.0),
   deuda_vieja_alerta_dias (DEFAULT 30), deuda_total_alerta_monto (DEFAULT 3000.0),
   stock_estancado_alerta_dias (DEFAULT 180), stock_bajo_alerta_unidades (DEFAULT 2),
   min_ventas_para_recomendar (DEFAULT 5), frecuencia_recalculo_dias (DEFAULT 1)
  AND SELECT policy allows any optica member
  AND INSERT/UPDATE policies allow admin and gerente
```

### Scenario: recalcular_resumen_diario function exists

```
GIVEN the migration has been applied
 WHEN `recalcular_resumen_diario('o1', '2026-07-05')` is called
 THEN resumen_diario gets a row for optica_id='o1' and fecha='2026-07-05'
  AND ventas_cantidad matches COUNT(*) from ventas for that optica and date
  AND ventas_monto_total matches SUM(monto_total)
  AND ventas_costo_total matches COALESCE(SUM(costo_unitario_snapshot), 0)
  AND calling the function twice does not duplicate the row
  AND calling the function after new ventas updates the existing row (idempotent upsert)
```

### Scenario: recalcular_resumen_diario with NULL costo_unitario_snapshot

```
GIVEN ventas exist for optica_id='o1' on '2026-07-05' where some rows have NULL costo_unitario_snapshot
 WHEN `recalcular_resumen_diario('o1', '2026-07-05')` is called
 THEN ventas_costo_total is the COALESCE(SUM(...), 0) — not NULL
  AND the function does not fail
```

### Scenario: recalcular_resumen_diario pending balance calculation

```
GIVEN ventas exist with pagos where some ventas are partially paid
 WHEN `recalcular_resumen_diario('o1', '2026-07-05')` is called
 THEN saldo_pendiente_total is the SUM of (v.monto_total - SUM(p.monto)) for ventas with pending balance > 0.005
  AND saldo_pendiente_cantidad is COUNT of such ventas
```

### Scenario: Room migration v31→v32 runs on existing DB

```
GIVEN a device has OptoDatabase at version 31 with existing data
 WHEN MIGRATION_31_32 runs
 THEN 4 new tables are created: categorias_producto, gastos_operativos, resumen_diario, configuracion_financiera
  AND ventas table gains column categoriaProductoId (TEXT, nullable)
  AND all existing data in pre-v31 tables is preserved
  AND categorias_producto has 9 seed rows
  AND re-running the migration does not duplicate seed rows (INSERT OR IGNORE)
```

### Scenario: Room CategoriaProductoDao queries

```
GIVEN CategoriaProductoDao is available in the database
 WHEN getAll() is called
 THEN a List<CategoriaProductoEntity> with 9 rows is returned, ordered by orden ASC
 WHEN getById('lente_progresivo') is called
 THEN the matching entity is returned with nombre='Lentes Progresivos'
 WHEN getById('non_existent') is called
 THEN null is returned
```

### Scenario: Room GastoOperativoDao CRUD

```
GIVEN GastoOperativoDao is available in the database
 WHEN upsert() is called with a new entity
 THEN it is inserted
 WHEN getByOptica() is called
 THEN a Flow emits the list of expenses for that optica
 WHEN getByOpticaAndDateRange() is called
 THEN only expenses within the date range are returned
 WHEN upsert() is called with an existing ID
 THEN the row is updated
 WHEN deleteById() is called
 THEN the row is removed
```

### Scenario: Room ResumenDiarioDao — download-only read

```
GIVEN ResumenDiarioDao is available with seeded rows
 WHEN getByOpticaAndDateRange(opticaId, start, end) is called
 THEN a Flow<List<ResumenDiarioEntity>> is returned, ordered by fecha DESC
 WHEN getByOpticaAndFecha(opticaId, fecha) is called
 THEN a single matching entity is returned
 WHEN getAllByOptica(opticaId) is called
 THEN all rows for that optica are returned (snapshot for sync reconciliation)
 WHEN upsert() is called with a remote row
 THEN it is inserted or replaced without triggering any sync scheduler
```

### Scenario: Room ConfiguracionFinancieraDao — read-only

```
GIVEN ConfiguracionFinancieraDao is available
 WHEN getByOptica(opticaId) is called
 THEN the single-row entity is returned, or null if not configured
 WHEN upsert() is called with a remote entity
 THEN the row is inserted or replaced (download-only path)
```

### Scenario: Hilt DI provides new DAOs

```
GIVEN the Hilt component graph is initialized
 WHEN DatabaseModule is processed
 THEN @Provides methods exist for CategoriaProductoDao, GastoOperativoDao, ResumenDiarioDao, ConfiguracionFinancieraDao
  AND all four DAOs can be injected into dependents
```

### Scenario: OptoRepository passthrough methods

```
GIVEN OptoRepository is instantiated
 WHEN upsertGastoOperativo(entity) is called
 THEN it calls gastoOperativoDao.upsert(entity)
  AND PostSaveSyncScheduler.scheduleFinanzasSync() is scheduled
 WHEN upsertGastoOperativoFromRemote(entity) is called
 THEN it calls gastoOperativoDao.upsert(entity)
  AND no sync scheduler is triggered (remote timestamp preserved)
 WHEN upsertResumenDiarioFromRemote(entity) is called
 THEN it calls resumenDiarioDao.upsert(entity)
  AND no sync scheduler is triggered
```

### Scenario: DownloadSyncCoordinator downloads resumen_diario

```
GIVEN a sync cycle runs via SyncFinanzasUseCase
 WHEN downloadAfterUpload = true
 THEN downloadResumenDiario() is called
  AND it queries supabase.postgrest["resumen_diario"] for the optica
  AND each downloaded row is persisted via repository.upsertResumenDiarioFromRemote()
  AND the count of downloaded rows is returned
```

### Scenario: DownloadSyncCoordinator downloads configuracion_financiera

```
GIVEN a sync cycle runs via SyncFinanzasUseCase
 WHEN downloadAfterUpload = true
 THEN downloadConfiguracionFinanciera() is called
  AND it queries supabase.postgrest["configuracion_financiera"] for the optica (maybeSingle)
  AND if a row exists, it is persisted via repository.upsertConfiguracionFinancieraFromRemote()
  AND 1 is returned if a row was downloaded, 0 otherwise
```

### Scenario: SyncFinanzasUseCase includes new counters

```
GIVEN a sync cycle completes with download
 WHEN SyncFinanzasUseCase constructs FinanzasSyncResult
 THEN the result contains downloadedResumenesDiarios: Int
  AND the result contains downloadedConfiguracionesFinancieras: Int
  AND both fields default to 0
```

### Scenario: VentaRemota includes categoria_producto_id

```
GIVEN VentaRemota is used to decode a remote venta row
 WHEN the remote row has categoria_producto_id = 'lente_progresivo'
 THEN r.categoriaProductoId is 'lente_progresivo'
  AND venta.categoriaProductoId is 'lente_progresivo' after toEntity()
 WHEN the remote row has no categoria_producto_id (null)
 THEN r.categoriaProductoId is null
  AND venta.categoriaProductoId is null after toEntity()
```

---

## Out of Scope

- Fase 7: Business indicator calculations and UI screens
- Fase 8: Recommendation engine
- Fase 9: Financial configuration screens
- `MargenPorCategoria` Room entity (server-side only)
- `CostoProducto` Room entity (server-side only)
- `FeedbackRecomendaciones` Room entity (web-only)
- Upload sync for `resumen_diario` and `configuracion_financiera` (read-only on Android)
- Snapshot coordinator changes (`SyncSnapshotCoordinator`)
- RPC for pre-calculating `margen_por_categoria` (deferred to Fase 7+)
- UI for assigning `categoria_producto_id` at venta creation time (deferred to Fase 7+)
