# Proposal — Fase 6: Esquema de datos para análisis de negocio

## Intent

Create the foundational data schema (Supabase + Room) for business analysis — product categorization, margin tracking, daily summaries, operative expenses, and financial configuration. This is the backend for all remaining financial phases (Fase 7–10 indicators, recommendations, and UI).

## Scope

### In Scope
- 8 Supabase tables + RLS: `categorias_producto`, `gastos_operativos`, `margen_por_categoria`, `resumen_diario`, `costos_productos`, `configuracion_financiera`, `feedback_recomendaciones`, plus ALTER `ventas ADD COLUMN categoria_producto_id`
- Idempotent seed data for `categorias_producto` (9 rows)
- RPC function `recalcular_resumen_diario(optica_id, fecha)` — upsert-based daily aggregation
- 4 Room entities + DAOs: `CategoriaProducto`, `GastoOperativo`, `ResumenDiario`, `ConfiguracionFinanciera`
- Update `Venta` entity (add `categoriaProductoId`)
- Room migration v31→v32
- DI wiring (`DatabaseModule`), `OptoRepository` passthroughs for new entities
- `DownloadSyncCoordinator` + `SyncFinanzasUseCase` — download `resumen_diario`
- `SyncFinanzasDto` — add `ResumenDiarioRemota`, `ConfiguracionFinancieraRemota`

### Out of Scope
- Fase 7: business indicator calculations and UI
- Fase 8: recommendation engine
- Fase 9: financial configuration screens
- `MargenPorCategoria` / `CostoProducto` Room entities (server-side calculated only)
- Upload sync for new tables (deferred until write UI exists)
- Snapshot coordinator changes (`SyncSnapshotCoordinator`)

## Capabilities

### New Capabilities
- `categorizacion-productos`: `categorias_producto` master table + `ventas.categoria_producto_id` — enables per-category margin analysis
- `gastos-operativos`: operational expense tracking with 8 business categories (alquiler, servicios, personal, proveedores, insumos, marketing, impuestos, otro) + RLS
- `resumen-diario`: daily pre-aggregated summary (sales count/amount/cost, payments, pending balance, inventory value) — calculated server-side via RPC, downloaded to Android
- `configuracion-financiera`: per-tenant financial objectives (target margin, ticket, alert thresholds, recalculation frequency) — 1:1 with `opticas`
- `costos-productos`: product unit cost tracking with historical versioning (vigente_desde/vigente_hasta) — server-side master data

### Modified Capabilities
None — pure schema addition, no existing spec-level behavior changes.

## Approach

1. **Supabase migration** — single `.sql` file with 8 CREATE TABLE + 1 ALTER + RLS policies + seed INSERTs + RPC function. Order: `categorias_producto` → ALTER `ventas` → `costos_productos` → `configuracion_financiera` → `gastos_operativos` → `margen_por_categoria` → `resumen_diario` → `feedback_recomendaciones` → RPC
2. **Room v31→v32 migration** — match Supabase schema locally (CREATE TABLE + ALTER + idempotent seed)
3. **Room entities + DAOs** — follow existing `VentaDao` pattern, no bidirectional sync
4. **Venta entity update** — add `categoriaProductoId: String? = null`
5. **DI wiring** — new `@Provides` in `DatabaseModule`, passthroughs in `OptoRepository`
6. **Download sync** — add `ResumenDiario` download to `SyncFinanzasUseCase` + DTOs

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `supabase/migrations/` | **New** | 1 migration (~320 SQL, 8 tables + 1 ALTER + RLS + seed + RPC) |
| `data/venta/Venta.kt` | **Modified** | Add `categoriaProductoId` |
| `data/categoriaproducto/` | **New** | `CategoriaProductoEntity`, `CategoriaProductoDao` |
| `data/gastooperativo/` | **New** | `GastoOperativoEntity`, `GastoOperativoDao` |
| `data/resumendiario/` | **New** | `ResumenDiarioEntity`, `ResumenDiarioDao` |
| `data/configuracionfinanciera/` | **New** | `ConfiguracionFinancieraEntity`, `ConfiguracionFinancieraDao` |
| `data/OptoDatabase.kt` | **Modified** | Register entities, DAOs, migration v32 |
| `data/OptoDatabaseMigrations.kt` | **Modified** | Add `MIGRATION_31_32` |
| `di/DatabaseModule.kt` | **Modified** | `@Provides` for new DAOs |
| `data/OptoRepository.kt` | **Modified** | DAO passthroughs |
| `domain/SyncFinanzasDto.kt` | **Modified** | New remote DTOs |
| `domain/DownloadSyncCoordinator.kt` | **Modified** | `downloadResumenDiario()` |
| `domain/SyncFinanzasUseCase.kt` | **Modified** | ResumenDiario in download flow |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `gastos_operativos` no existe — plan asume ALTER pero necesita CREATE | High | Exploration confirmó CREATE; especificar en spec |
| Seed `categorias_producto` divergente entre Supabase y Room | Med | Misma lista hardcodeada en ambos lados; verificar en code review |
| `categoriaProductoId` en Venta — Room-only o mapeado a Supabase DTO? | Med | Decidir en design (Venta.`ot` es Room-only, seguir mismo patrón o mapear) |

## Rollback Plan

1. **Supabase**: `DROP TABLE` all 8 new tables (CASCADE) + `ALTER TABLE ventas DROP COLUMN categoria_producto_id` + `DROP FUNCTION recalcular_resumen_diario`
2. **Room**: Remove migration v32, downgrade DB version to 31. Run `supabase db reset` on dev branches.

## Dependencies

- Fases 1–2 (`ventas`, `pagos` tables) — ✅ in production
- `monturas` table with `costo`/`stock_actual` — ✅ pre-existing
- Room v31 baseline migration — ✅ exists

## Success Criteria

- [ ] 1 Supabase migration applied cleanly — all 8 tables + ALTER + RLS + seed + RPC
- [ ] RLS policies restrict write to admin; all members can SELECT by `optica_id`
- [ ] `recalcular_resumen_diario()` upserts without duplicates (test 2x same day)
- [ ] Room migration v31→v32 runs without errors on existing production-mirror DB
- [ ] 4 new entities + DAOs compile; `Venta` entity updated with `categoriaProductoId`
- [ ] CI: `./gradlew :optoapp:testDebugUnitTest --stacktrace` + `assembleDebug` ✅
- [ ] Seed `categorias_producto` identical on Supabase and Room (9 rows, same IDs)
