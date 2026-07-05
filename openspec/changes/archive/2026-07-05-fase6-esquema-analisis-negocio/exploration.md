# Exploration — Fase 6: Esquema de datos para análisis de negocio (Parte B)

**Change:** `fase6-esquema-analisis-negocio`
**Date:** 2026-07-05
**Phase:** Explore (SDD)

---

## 1. Current state of each relevant table/entity

### Supabase — existing tables

| Table | Exists? | RLS? | Notes |
|-------|---------|------|-------|
| `ventas` | ✅ Creada (Fase 1) | ✅ `ventas_select/insert/update/delete` | Tiene `costo_unitario_snapshot` pero NO `categoria_producto_id` |
| `monturas` | ✅ Existente pre-Fase 1 | ✅ `monturas_select/insert/update/delete` | Tiene `costo` (numeric) y `stock_actual` (integer) — necesarios para `resumen_diario.inventario_valor` |
| `pagos` | ✅ Existente | ✅ | Referenciado por `recalcular_resumen_diario()` |
| `opticas` | ✅ Existente | ✅ | FK para todas las nuevas tablas |
| `categorias_producto` | ❌ No existe | N/A | Nueva tabla seed |
| `gastos_operativos` | ❌ No existe | N/A | Tabla completamente nueva |
| `margen_por_categoria` | ❌ No existe | N/A | Nueva tabla materializada |
| `resumen_diario` | ❌ No existe | N/A | Nueva tabla de agregados diarios |
| `costos_productos` | ❌ No existe | N/A | Nueva tabla con histórico |
| `configuracion_financiera` | ❌ No existe | N/A | Nueva tabla 1:1 con `opticas` |
| `feedback_recomendaciones` | ❌ No existe | N/A | Nueva tabla opcional |

### Room (Android) — existing entities

| Entity | Exists? | Package | Notes |
|--------|---------|---------|-------|
| `Venta` | ✅ | `data.venta` | NO tiene `categoriaProductoId`. Tiene campo `ot` (Room-only, no en Supabase). |
| `Montura` | ✅ | `data.Entities` (compilado) | Tiene `costo` y `stockActual` |
| `VentaDao` | ✅ | `data.venta` | `@Upsert`, `suspend fun`, `Flow<List<Venta>>` |
| `CategoriaProducto` | ❌ No existe | — | Nueva entidad |
| `GastoOperativo` | ❌ No existe | — | Nueva entidad |
| `MargenPorCategoria` | ❌ No existe | — | Nueva entidad |
| `ResumenDiario` | ❌ No existe | — | Nueva entidad |
| `CostoProducto` | ❌ No existe | — | Nueva entidad |
| `ConfiguracionFinanciera` | ❌ No existe | — | Nueva entidad |

### Room — current version

- **DB version:** 31 (v31 en `OptoDatabase.kt`)
- **Last migration:** `MIGRATION_30_31` (added `ot` column to `ventas`)
- **Next version:** v32 (for Fase 6 additions)

---

## 2. Affected files and areas

### Supabase migrations (new file)

| # | Migration | Type | Description |
|---|-----------|------|-------------|
| 1 | `20260705000000_create_categorias_producto.sql` | CREATE | Tabla `categorias_producto` + seed data |
| 2 | `20260705000001_add_categoria_producto_id_to_ventas.sql` | ALTER | `ALTER TABLE ventas ADD COLUMN categoria_producto_id TEXT REFERENCES categorias_producto(id)` |
| 3 | `20260705000002_create_gastos_operativos.sql` | CREATE | Tabla `gastos_operativos` con CHECK ampliado |
| 4 | `20260705000003_create_margen_por_categoria.sql` | CREATE | Tabla `margen_por_categoria` + RLS |
| 5 | `20260705000004_create_resumen_diario.sql` | CREATE | Tabla `resumen_diario` + RLS |
| 6 | `20260705000005_create_costos_productos.sql` | CREATE | Tabla `costos_productos` + RLS |
| 7 | `20260705000006_create_configuracion_financiera.sql` | CREATE | Tabla `configuracion_financiera` + RLS |
| 8 | `20260705000007_create_feedback_recomendaciones.sql` | CREATE | Tabla `feedback_recomendaciones` + RLS |
| 9 | `20260705000008_rpc_recalcular_resumen_diario.sql` | CREATE | Función PL/pgSQL `recalcular_resumen_diario()` |

### Android / Room — new files needed

| File | Type | Package |
|------|------|---------|
| `CategoriaProductoEntity.kt` | Room Entity | `data.categoriaproducto` |
| `CategoriaProductoDao.kt` | DAO | `data.categoriaproducto` |
| `GastoOperativoEntity.kt` | Room Entity | `data.gastooperativo` |
| `GastoOperativoDao.kt` | DAO | `data.gastooperativo` |
| `ResumenDiarioEntity.kt` | Room Entity | `data.resumendiario` |
| `ResumenDiarioDao.kt` | DAO | `data.resumendiario` |
| `ConfiguracionFinancieraEntity.kt` | Room Entity | `data.configuracionfinanciera` |
| `ConfiguracionFinancieraDao.kt` | DAO | `data.configuracionfinanciera` |

**Nota:** `CategoriaProducto`, `ConfiguracionFinanciera`, y `GastoOperativo` son tablas de configuración maestra que NO requieren sincronización bidireccional (son semilla fija o solo escritas por el dueño). Por lo tanto, NO necesitan `UploadSyncCoordinator` / `DownloadSyncCoordinator` ni entrada en `SyncFinanzasUseCase`.

`ResumenDiario` es una tabla de solo lectura desde el lado Android (se calcula en Supabase vía `recalcular_resumen_diario()`). NO se sincroniza bidireccionalmente — solo se descarga.

### Android / Room — files modified

| File | Change |
|------|--------|
| `data/venta/Venta.kt` | Agregar `categoriaProductoId: String? = null` |
| `data/OptoDatabase.kt` | Agregar nuevas entidades al array `entities[]`, nuevos DAOs abstractos, nueva migration v31→v32 |
| `data/OptoDatabaseMigrations.kt` | Agregar `MIGRATION_31_32` con CREATE TABLEs y ALTER TABLE ventas |
| `data/OptoRepository.kt` | Agregar métodos para nuevas entidades (DAO passthrough + sync scheduling) |
| `di/DatabaseModule.kt` | Agregar `@Provides` para nuevos DAOs, inyectar en `OptoRepository` |

### Domain — sync pipeline

| Archivo | Cambio |
|---------|--------|
| `domain/DownloadSyncCoordinator.kt` | Agregar `downloadResumenDiario()` |
| `domain/SyncFinanzasUseCase.kt` | Agregar descarga de `resumen_diario` en el flujo de download |
| `domain/SyncFinanzasDto.kt` | Agregar `ResumenDiarioRemota`, `ConfiguracionFinancieraRemota` DTOs |

### NOT affected (no changes needed)

- `UploadSyncCoordinator.kt` — ninguna de las nuevas tablas requiere upload bidireccional
- `SyncSnapshotCoordinator.kt` — no se agrega nueva entidad al snapshot
- `PostSaveSyncScheduler.kt` — no se necesitan nuevos schedulers
- `MonturaEntity.kt` — `monturas.costo` y `monturas.stock_actual` ya existen, se leen directamente

---

## 3. Migration strategy

### Supabase migrations (applied via `supabase db push`)

All new tables are independent (no FK cycles). Recommended order:

```
1. categorias_producto          ← seed data incluido
2. ALTER TABLE ventas ADD COLUMN categoria_producto_id
3. gastos_operativos
4. costos_productos             ← depende de categorias_producto
5. configuracion_financiera     ← 1:1 con opticas, sin FK explícita
6. margen_por_categoria         ← depende de categorias_producto
7. resumen_diario               ← independiente
8. feedback_recomendaciones     ← independiente
9. rpc_recalcular_resumen_diario
```

### Room migration (v31 → v32)

```kotlin
val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // New standalone tables
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS categorias_producto (
                id TEXT NOT NULL PRIMARY KEY,
                nombre TEXT NOT NULL,
                familia TEXT NOT NULL,
                orden INTEGER NOT NULL DEFAULT 0
            )
        """)
        
        // Seed data for categorias_producto (same as Supabase)
        db.execSQL("INSERT OR IGNORE INTO categorias_producto ...")
        
        // ALTER ventas to add categoriaProductoId
        db.execSQL("ALTER TABLE ventas ADD COLUMN categoriaProductoId TEXT DEFAULT NULL")
        
        // gastos_operativos table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS gastos_operativos (
                id TEXT NOT NULL PRIMARY KEY,
                ...
            )
        """)
        
        // resumen_diario table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS resumen_diario (
                id TEXT NOT NULL PRIMARY KEY,
                ...
            )
        """)
        
        // configuracion_financiera table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS configuracion_financiera (
                opticaId TEXT NOT NULL PRIMARY KEY,
                ...
            )
        """)
    }
}
```

**Riesgo:** La semilla de `categorias_producto` debe ser idempotente (`INSERT OR IGNORE`) para no duplicar datos si hay rollback y re-ejecución.

---

## 4. Risks and gotchas

### 🔴 Critical

1. **`gastos_operativos` no existe en Supabase ni en Room** — hay que crearla desde cero. El plan asume que ya existe una tabla original con CHECK 'fijo'/'variable', pero no hay tal tabla. La migración debe ser CREATE TABLE, no ALTER TABLE.

2. **`Venta.kt` tiene campo `ot` que NO existe en Supabase** — Es un Room-only field. Al agregar `categoriaProductoId` a Venta, hay que decidir si también debe ser Room-only (sin contraparte en Supabase) o si se mapea al DTO remoto.

3. **`MargenPorCategoria` y `CostosProductos` no se sincronizan en el pipeline actual** — Son tablas de solo lectura/maestra que no siguen el patrón de las entidades transaccionales (Dispensacion, Pago, etc.). No deben agregarse al `UploadSyncCoordinator`.

### 🟡 High

4. **El seed de `categorias_producto` debe ser idéntico en Supabase y Room** — Si divergen, la app clasificará ventas con IDs que no existen en el backend.

5. **`resumen_diario` usa `SUM(monto_total)` y `SUM(costo_unitario_snapshot)`** — Si `costo_unitario_snapshot` es NULL en alguna venta, el cálculo daría NULL. La función `recalcular_resumen_diario()` usa `COALESCE(SUM(...), 0)`, lo cual es correcto.

6. **`recalcular_resumen_diario()` asume que `pagos.fecha` existe como columna** — Verificar que la tabla `pagos` realmente tiene columna `fecha`. Si está como `fecha` en Supabase, funciona.

7. **`configuracion_financiera` usa `optica_id` como PK TEXT** — No tiene FK explícita a `opticas(id)`. Esto es intencional (la optica puede no existir aún al crear la config), pero hay que asegurar consistencia en la app.

### 🟢 Medium

8. **`VentaRemota` DTO en `SyncFinanzasDto.kt` no mapea `ot` ni `categoria_producto_id`** — Al agregar `categoria_producto_id` a Supabase, el DTO remoto debe actualizarse.

9. **Las nuevas entidades no aparecen en la UI** (Fase 7) — Eso es correcto; esta fase solo cubre esquema de datos. Las pantallas de análisis vienen en Fase 7+.

### 📝 Design decisions confirmed by the plan

| Decisión | Impacto |
|----------|---------|
| Costos de lentes: carga manual por el dueño | `costos_productos.costo_unitario` se escribe manualmente. No hay integración automática con laboratorios. |
| Gastos fijos: el dueño carga mensualmente | `gastos_operativos` reemplaza el modelo anterior. CHECK ampliado a categorías realistas (`alquiler`, `servicios`, `personal`, etc.). |
| Histórico de inventario: solo hacia adelante | No hay migración de datos históricos. `monturas.stock_actual` y `monturas.costo` se leen en el momento del primer cálculo de `resumen_diario`. |

---

## 5. Readiness assessment

**✅ Ready for Fase 6 — with the following observations:**

### What's already in place

- ✅ `VentaDao` pattern exists → DAOs for new entities follow exactly the same pattern
- ✅ DI wiring pattern clear (`DatabaseModule` → provides daos → injects `OptoRepository`)
- ✅ Sync pipeline well understood (download-only for read tables, bidirectional for transactional tables)
- ✅ Room migration pattern well established (sequential `MIGRATION_*` in `OptoDatabaseMigrations.kt`)

### What needs attention before implementation

1. **`gastos_operativos` is a full CREATE TABLE, not an ALTER** — the plan text (section 6.4) reads "ALTER TABLE... DROP CONSTRAINT" but the table doesn't exist. Implementation must be CREATE TABLE.
2. **Room migration script must include seed data** for `categorias_producto` (idempotent INSERT).
3. **`Venta` entity + DTO must be updated** to include `categoriaProductoId`.
4. **Naming convention check:** Room uses `camelCase` (e.g. `categoriaProductoId`). Supabase uses `snake_case` (`categoria_producto_id`). The existing `VentaRemota` DTO handles this via `@SerialName`. New DTOs must follow the same convention.

### Recommended implementation order

1. Supabase migrations (all 9)
2. Room v31→v32 migration
3. New Room entities + DAOs
4. Update `Venta` entity + `VentaDao` (add `categoriaProductoId`)
5. Hilt DI: `DatabaseModule`
6. Update `OptoRepository` (passthrough + sync scheduling for new entities)
7. Update `DownloadSyncCoordinator` + `SyncFinanzasUseCase` (download `resumen_diario`)
8. Update `SyncFinanzasDto` (new `ResumenDiarioRemota`, etc.)
9. Tests

---

*End of exploration artifact.*
