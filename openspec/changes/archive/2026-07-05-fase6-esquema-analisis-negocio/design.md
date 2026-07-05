# Design: Fase 6 — Esquema de datos para análisis de negocio

## Technical Approach

Single Supabase migration file with all DDL + RLS + seed data + RPC in dependency order. 4 new Room entities (read-only seed, read-only download, read-only config, and CRUD local). Room v31→v32 manual migration matching existing `MIGRATION_*` pattern. Download-only sync for `resumen_diario` and `configuracion_financiera`; no upload sync for any new table in this phase.

## Architecture Decisions

### Decision: Single Supabase migration vs. multiple files

| Option | Tradeoff | Decision |
|--------|----------|----------|
| 9 separate `.sql` files | Cleaner isolation, easier rollback per table | **Single file** — all dependencies are local (categorias_producto FK for ventas + costos + margen), no cross-file ordering risk, matches existing project convention (see MIGRATION_30_31 pattern) |

### Decision: Room migration — manual vs. AutoMigration

| Option | Tradeoff | Decision |
|--------|----------|----------|
| `AutoMigration` with `spec` | Less boilerplate, but cannot run seed INSERTs | **Manual `Migration(31, 32)`** — must run `INSERT OR IGNORE` seed data and `ALTER TABLE ventas ADD COLUMN`, which AutoMigration doesn't support. Consistent with all 24 existing migrations. |

### Decision: Room entities — which tables get one

| Supabase table | Room entity? | Rationale |
|----------------|-------------|-----------|
| `categorias_producto` | ✅ **CategoriaProductoEntity** | Seed data needed offline for category lookup when creating/editing ventas |
| `gastos_operativos` | ✅ **GastoOperativoEntity** | User creates expenses from Android — local CRUD + sync upload (deferred) |
| `resumen_diario` | ✅ **ResumenDiarioEntity** | Downloaded daily summaries for offline dashboard |
| `configuracion_financiera` | ✅ **ConfiguracionFinancieraEntity** | Read-only download for indicators (Fase 7+) |
| `margen_por_categoria` | ❌ Server-only | Pre-calculated server-side, queried via RPC in Fase 7 |
| `costos_productos` | ❌ Server-only | Web-managed master data |
| `feedback_recomendaciones` | ❌ Web-only | Append-only feedback from recommendation engine |

### Decision: `Venta.categoriaProductoId` — Room-only or mapped to Supabase DTO

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Room-only field (like `ot`) | Never synced, set locally only | **Mapped** — column exists in Supabase (`categoria_producto_id`), so VentaRemota gets `@SerialName("categoria_producto_id")` and the download/upload pipeline carries it |
| Mapped + synced | Single source of truth for category per venta | Both Room entity and remote DTO carry it; existing ventas get NULL |

### Decision: `gastos_operativos` upload sync in Fase 6

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Upload method in `UploadSyncCoordinator` | Expense changes propagate to server immediately | **Deferred** — `upsertGastoOperativo()` schedules full finanzas sync (uploading dispensaciones/pagos/etc.) but no dedicated upload method for gastos_operativos. Local-only until write UI + upload sync are added together. |
| No upload | Data only on device, lost on reinstall | Consistent with "no upload for new tables" out-of-scope from proposal. |

### Decision: Type converters

No new TypeConverters needed. `LocalDate` ↔ `String` is already handled by existing `Converters` class. `Double?` and `Int?` are natively supported by Room.

## Data Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        Supabase                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐   │
│  │categorias_   │    │  ventas      │    │  gastos_         │   │
│  │producto      │◄───│(categoria_   │    │  operativos      │   │
│  │(seed)        │ FK │ producto_id) │    │  (manual CRUD)   │   │
│  └──────────────┘    └──────┬───────┘    └──────────────────┘   │
│                             │                                    │
│              ┌──────────────┴──────────────┐                    │
│              │  recalcular_resumen_diario() │                    │
│              │  (RPC, on-demand)           │                    │
│              └──────────────┬──────────────┘                    │
│                             ▼                                    │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐   │
│  │resumen_diario│    │margen_por_   │    │configuracion_    │   │
│  │(download)    │    │categoria     │    │financiera        │   │
│  └──────────────┘    └──────────────┘    │(download)        │   │
│                                          └──────────────────┘   │
└──────────────────┬──────────────────────────────────────────────┘
                   │ download (SyncFinanzasUseCase)
                   ▼
┌─────────────────────────────────────────────────────────────────┐
│  Room (Android local DB)                                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌───────────────┐  │
│  │CategoriaProducto│  │GastoOperativo   │  │ResumenDiario │  │
│  │Entity (seed RO) │  │Entity (CRUD)    │  │Entity (DL RO)│  │
│  └─────────────────┘  └─────────────────┘  └───────────────┘  │
│  ┌──────────────────┐  ┌─────────────────┐                     │
│  │Configuracion     │  │Venta (modified) │                     │
│  │FinancieraEntity  │  │+ categoriaPdtoId│                     │
│  │(DL RO)           │  └─────────────────┘                     │
│  └──────────────────┘                                          │
└─────────────────────────────────────────────────────────────────┘
```

**Sync download order (in `SyncFinanzasUseCase`):** arqueo_caja → dispensaciones → dispensacion_items → servicios_extra → ventas → **resumen_diario** → **configuracion_financiera** → pagos

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `supabase/migrations/20260705000000_fase6_esquema_analisis.sql` | **Create** | Single migration: 8 CREATE TABLE + ALTER ventas + RLS + seed + RPC function |
| `data/categoriaproducto/CategoriaProductoEntity.kt` | **Create** | Room entity: `id`, `nombre`, `familia`, `orden` |
| `data/categoriaproducto/CategoriaProductoDao.kt` | **Create** | DAO: `getAll()` (suspend, ordered by `orden`), `getById()` |
| `data/gastooperativo/GastoOperativoEntity.kt` | **Create** | Room entity: `id`, `opticaId`, `categoria`, `descripcion`, `monto`, `fecha`, `fechaProgramada?`, `nota?`, `createdAt?` |
| `data/gastooperativo/GastoOperativoDao.kt` | **Create** | DAO: `getByOptica(Flow)`, `getByOpticaAndDateRange(Flow)`, `getById`, `upsert(@Upsert)`, `deleteById` |
| `data/resumendiario/ResumenDiarioEntity.kt` | **Create** | Room entity matching `resumen_diario` columns |
| `data/resumendiario/ResumenDiarioDao.kt` | **Create** | DAO: `getByOpticaAndDateRange(Flow)`, `getByOpticaAndFecha`, `getAllByOptica`, `upsert(@Upsert)`, `deleteAll` |
| `data/configuracionfinanciera/ConfiguracionFinancieraEntity.kt` | **Create** | Room entity: 1:1 with `optica_id` |
| `data/configuracionfinanciera/ConfiguracionFinancieraDao.kt` | **Create** | DAO: `getByOptica`, `upsert(@Upsert)` |
| `data/venta/Venta.kt` | **Modify** | Add `val categoriaProductoId: String? = null` |
| `data/OptoDatabase.kt` | **Modify** | Bump version to 32; add 4 entities to array; add 4 abstract DAO methods; add `MIGRATION_31_32` re-export and to chain |
| `data/OptoDatabaseMigrations.kt` | **Modify** | Add `MIGRATION_31_32` with CREATE TABLE IF NOT EXISTS for 4 tables, ALTER TABLE ventas, INSERT OR IGNORE seed |
| `di/DatabaseModule.kt` | **Modify** | Add 4 `@Provides` for new DAOs; inject new DAOs into `provideOptoRepository` |
| `data/OptoRepository.kt` | **Modify** | Add private DAO fields, passthrough methods: `upsertGastoOperativo`, `upsertGastoOperativoFromRemote`, `upsertResumenDiarioFromRemote`, `upsertConfiguracionFinancieraFromRemote`, `getCategoriasProducto` |
| `domain/SyncFinanzasDto.kt` | **Modify** | Add `ResumenDiarioRemota`, `ConfiguracionFinancieraRemota` DTOs with `toEntity()`; add `categoriaProductoId` to `VentaRemota`; add counters to `FinanzasSyncResult` |
| `domain/DownloadSyncCoordinator.kt` | **Modify** | Add `downloadResumenDiario()`, `downloadConfiguracionFinanciera()`; add table constants |
| `domain/SyncFinanzasUseCase.kt` | **Modify** | Add both downloads to `if (downloadAfterUpload)` block; include counters in result |

## Interfaces / Contracts

### CategoriaProductoDao (read-only seed — no upsert)

```kotlin
@Dao
interface CategoriaProductoDao {
    @Query("SELECT * FROM categorias_producto ORDER BY orden ASC")
    suspend fun getAll(): List<CategoriaProductoEntity>

    @Query("SELECT * FROM categorias_producto WHERE id = :id")
    suspend fun getById(id: String): CategoriaProductoEntity?
}
```

### GastoOperativoDao (local CRUD — follows VentaDao pattern)

```kotlin
@Dao
interface GastoOperativoDao {
    @Query("SELECT * FROM gastos_operativos WHERE opticaId = :opticaId ORDER BY fecha DESC")
    fun getByOptica(opticaId: String): Flow<List<GastoOperativoEntity>>

    @Query("SELECT * FROM gastos_operativos WHERE opticaId = :opticaId AND fecha >= :start AND fecha <= :end ORDER BY fecha DESC")
    fun getByOpticaAndDateRange(opticaId: String, start: LocalDate, end: LocalDate): Flow<List<GastoOperativoEntity>>

    @Query("SELECT * FROM gastos_operativos WHERE id = :id")
    suspend fun getById(id: String): GastoOperativoEntity?

    @Upsert
    suspend fun upsert(entity: GastoOperativoEntity)

    @Query("DELETE FROM gastos_operativos WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

### ResumenDiarioDao (download-only — upsert exists for sync persistence only)

```kotlin
@Dao
interface ResumenDiarioDao {
    @Query("SELECT * FROM resumen_diario WHERE opticaId = :opticaId AND fecha >= :start AND fecha <= :end ORDER BY fecha DESC")
    fun getByOpticaAndDateRange(opticaId: String, start: LocalDate, end: LocalDate): Flow<List<ResumenDiarioEntity>>

    @Query("SELECT * FROM resumen_diario WHERE opticaId = :opticaId AND fecha = :fecha")
    suspend fun getByOpticaAndFecha(opticaId: String, fecha: LocalDate): ResumenDiarioEntity?

    @Query("SELECT * FROM resumen_diario WHERE opticaId = :opticaId")
    suspend fun getAllByOptica(opticaId: String): List<ResumenDiarioEntity>

    @Upsert
    suspend fun upsert(entity: ResumenDiarioEntity)

    @Query("DELETE FROM resumen_diario")
    suspend fun deleteAll()
}
```

### ConfiguracionFinancieraDao (download-only)

```kotlin
@Dao
interface ConfiguracionFinancieraDao {
    @Query("SELECT * FROM configuracion_financiera WHERE opticaId = :opticaId")
    suspend fun getByOptica(opticaId: String): ConfiguracionFinancieraEntity?

    @Upsert
    suspend fun upsert(entity: ConfiguracionFinancieraEntity)
}
```

### Room migration — MIGRATION_31_32 key details

- **Seed data**: 9 `INSERT OR IGNORE INTO categorias_producto(id, nombre, familia, orden) VALUES(...)` statements — one per row
- **Venta alter**: `ALTER TABLE ventas ADD COLUMN categoriaProductoId TEXT DEFAULT NULL` (DEFAULT NULL avoids NOT NULL violations on existing rows)
- **New tables**: All use `CREATE TABLE IF NOT EXISTS` for idempotency
- **Indices**: `CREATE INDEX IF NOT EXISTS index_gastos_operativos_opticaId ON gastos_operativos(opticaId)` and `CREATE UNIQUE INDEX IF NOT EXISTS index_resumen_diario_opticaId_fecha ON resumen_diario(opticaId, fecha)`
- **Room column types**: `TEXT` for `String`/`LocalDate`, `REAL` for `Double`, `INTEGER` for `Int`, nullable columns omit `NOT NULL`

### OptoRepository passthrough pattern

Two paths for upsert (mirrors existing `upsertVenta`/`upsertArqueoFromRemote` pattern):

```kotlin
// Local-write path (user-initiated) — stamps timestamp + schedules sync
suspend fun upsertGastoOperativo(entity: GastoOperativoEntity) {
    val stamped = entity.copy(createdAt = Instant.now().toString())
    gastoOperativoDao.upsert(stamped)
    postSaveSyncScheduler.get().scheduleFinanzasSync(stamped.opticaId)
}

// Remote-write path (download) — preserves server timestamp, no sync trigger
suspend fun upsertResumenDiarioFromRemote(entity: ResumenDiarioEntity) {
    resumenDiarioDao.upsert(entity)
}
```

### SyncFinanzasDto — new DTOs

`ResumenDiarioRemota` and `ConfiguracionFinancieraRemota` follow the exact `VentaRemota` pattern: `@Serializable` with `@SerialName` for snake_case mapping, `toEntity()` method. `FinanzasSyncResult` gains `downloadedResumenesDiarios: Int = 0` and `downloadedConfiguracionesFinancieras: Int = 0`.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (DAO) | CategoriaProductoDao queries | Room in-memory DB, verify 9 seed rows, ordering, null for missing ID |
| Unit (DAO) | GastoOperativoDao CRUD | Insert → query (Flow emission) → update → delete, verify reactive Flow |
| Unit (DAO) | ResumenDiarioDao queries | Upsert from remote, query by date range, verify fecha DESC ordering |
| Unit (DAO) | ConfiguracionFinancieraDao | Single row upsert + getByOptica |
| Unit (DAO) | VentaDao — categoriaProductoId | Insert Venta with new field, verify persistence and null default |
| Unit (Migration) | MIGRATION_31_32 | Create v31 DB, run migration, assert all 4 tables exist with correct columns, assert seed rows, assert ventas has new column, assert existing data preserved |
| Integration | DownloadSyncCoordinator | Mock supabase client, verify `downloadResumenDiario()` calls `repository.upsertResumenDiarioFromRemote()` for each row |
| Integration | OptoRepository passthroughs | Verify `upsertGastoOperativo` triggers `scheduleFinanzasSync`, remote path does not |

## Migration / Rollout

1. **Supabase**: Apply single migration file via `supabase db push`. Creates all tables, adds column, seeds data, creates RPC. Existing data preserved.
2. **Android**: Ship next release with v32 Room DB. Users with v31 get `MIGRATION_31_32` on first launch. **Rollback**: Remove migration, drop to v31, re-deploy.
3. **Seed consistency**: Both Supabase migration and Room migration hardcode the same 9-row INSERT list. Code review must verify they match byte-for-byte.

## Open Questions

- [ ] `gastos_operativos` upload: `upsertGastoOperativo()` calls `scheduleFinanzasSync()` (per R17), but spec R22 explicitly bans new upload methods. The cycle uploads other entities but not gastos_operativos itself. Confirm: is upload for gastos_operativos deferred to Fase 7+?
- [ ] Room seed diverging from Supabase seed: the 9 rows are duplicated across two codebases. Consider extracting to a shared constant file (e.g., `CategoriaProductoSeed`) to guarantee they stay in sync.
