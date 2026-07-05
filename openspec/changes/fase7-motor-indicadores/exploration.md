# Exploration: Fase 7 — Motor de 8 indicadores en lenguaje de negocio

## Current State

### Supabase: Existing RPC Functions

| RPC | File | Status | Notes |
|-----|------|--------|-------|
| `rpc_resumen_financiero` | `20260514000000_rpc_resumen_financiero.sql` | ✅ Updated to `ventas` | Returns `ingresos_cobrados`, `ventas_emitidas`, `saldo_pendiente`, `ticket_promedio`. Needs deprecation per plan — replaced by `rpc_analisis_mensual` |
| `rpc_saldo_pendiente` | `20260513000000_rpc_financial_aggregates.sql` | ⚠️ Still uses old tables | Reads from `dispensaciones.monto_pagado` and `servicios_extra.a_cuenta`. Needs deprecation |
| `rpc_cierre_caja_resumen` | `20260514000001_rpc_cierre_caja_resumen.sql` | ✅ No changes needed | Only reads from `pagos` |
| `rpc_count_pendientes` | `20260513000000_rpc_financial_aggregates.sql` | ⚠️ Needs update | Still uses `dispensaciones` + `servicios_extra` directly, should use `ventas` |
| `rpc_pacientes_con_saldo` | Same file | ⚠️ Needs deprecation | Uses old tables |
| `rpc_pacientes_con_entrega_pendiente` | Same file | ⚠️ Needs deprecation | Uses old tables |
| `rpc_analisis_mensual` | ❌ Does not exist | **Create** | New function returning JSONB for 8 indicators |
| `rpc_deudores` | ❌ Does not exist | **Create** | Returns TABLE for deudores |
| `recalcular_resumen_diario` | `20260705000000_fase6_esquema_analisis.sql` | ⚠️ Missing GRANT | **Missing**: `GRANT EXECUTE TO authenticated` — client-side calls will fail |

#### RPC Pattern
All existing RPCs are defined as **SQL migrations** (not Edge Functions). They use:
- `SECURITY INVOKER` — runs with caller's privileges, RLS enforced
- `SET search_path = public`
- `GRANT EXECUTE ON FUNCTION ... TO authenticated, service_role`
- `REVOKE EXECUTE ON FUNCTION ... FROM public, anon`

### Supabase: Schema Status (Fase 6 complete)

| Table | Supabase | Room (Android) | Notes |
|-------|----------|----------------|-------|
| `ventas` | ✅ Created + triggers + backfill | ✅ `Venta` entity + `VentaDao` | Has `categoriaProductoId` field |
| `resumen_diario` | ✅ Created + RLS | ✅ `ResumenDiarioEntity` + `ResumenDiarioDao` | DAO has only: `getByOpticaId`, `upsert`, `deleteAll` |
| `gastos_operativos` | ✅ Created + RLS | ✅ `GastoOperativoEntity` + `GastoOperativoDao` | Categories: alquiler, servicios, personal, proveedores, insumos, marketing, impuestos, otro |
| `categorias_producto` | ✅ Created + RLS | ✅ `CategoriaProductoEntity` + `CategoriaProductoDao` | 9 seed categories |
| `configuracion_financiera` | ✅ Created + RLS | ✅ `ConfiguracionFinancieraEntity` + `ConfiguracionFinancieraDao` | 1:1 with opticas |
| `costos_productos` | ✅ Created + RLS | ❌ **Missing Room entity** | No `CostoProductoEntity`, no DAO |
| `margen_por_categoria` | ✅ Created + RLS | ❌ **Missing Room entity** | No `MargenPorCategoriaEntity`, no DAO |
| `feedback_recomendaciones` | ✅ Created + RLS | ❌ **Missing Room entity** | Not needed until Fase 8 |

### Android: Pago entity

The `Pago` Room entity (`DispensacionEntity.kt:89`) does **NOT** have a `ventaId` field yet. It still uses `dispensacionId` and `servicioExtraId` as dual references. The plan requires queries against `pagos.venta_id` for deudores and saldo calculations.

### Android: UseCase Patterns

All existing UseCases follow the same pattern:

```kotlin
open class SomeUseCase @Inject constructor(
    private val dependency: SomeDependency
) {
    suspend operator fun invoke(opticaId: String, ...): Resource<ResultType> {
        return try {
            // work
            Resource.Success(result)
        } catch (e: IOException) {
            Resource.Error(e.message ?: "Error")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error inesperado")
        }
    }
}
```

Key characteristics:
- **Hilt `@Inject` constructor** — no `@Singleton` needed unless shared state
- **`suspend operator fun invoke(...)`** — single entry point
- **`Resource<T>` return** — sealed class (`Success`, `Error`)
- **No direct RPC calls** — they delegate to Repositories or DAOs
- RPC calls happen at the **DataSource layer** (e.g., `MembershipDataSource` calls `supabase.postgrest.rpc(...)`)

### Android: Existing BI Dashboard

**BIViewModel** (`viewmodel/BIViewModel.kt`):
- `@HiltViewModel` with `@Inject constructor(repository, sessionManager, ventaDao)`
- Uses reactive `combine` + `flatMapLatest` pattern
- Current indicators: examenes (actual/anterior), recaudacion (proyectada/cobrada), topProductos, entregas pendientes/completadas, monturas stock bajo, inventario stats
- **None of the 8 business indicators from Fase 7** exist yet — the BI dashboard was written before the plan

**BIScreen** (`ui/screens/BIScreen.kt`):
- "Panel de Estadísticas" — displays KPICards, BarChart, DonutChart
- Connected in `MainDrawerScreen.kt` route `estadisticas_bi`
- Drawer label: "Estadísticas (BI)" — in `DrawerSections.kt` and `MainDrawerContent.kt`

**BIViewModelTest** (`viewmodel/BIViewModelTest.kt`):
- Uses MockK, tests current KPI calculations
- ~240 lines of test code

**ReportesViewModel** (`viewmodel/ReportesViewModel.kt`):
- Also uses `ventaDao` and `OptoRepository`
- Has a fallback: if `ventas` table is empty, derives Venta from `dispensaciones` + `servicios_extra`
- Calculates: `totalVendido`, `totalPagado`, `totalCobrado`, `cobrosPeriodo`

### Room DAOs — Aggregation Capabilities

**ResumenDiarioDao** — current queries:
- `getByOpticaId(opticaId): Flow<List<ResumenDiarioEntity>>`
- `upsert(resumen)`
- `deleteAll()`
- **Missing**: aggregation queries: SUM by month, date range queries for monthly indicators

**VentaDao** — current queries:
- `getVentasByOpticaAndDateRange(opticaId, start, end): Flow<List<Venta>>`
- `getAllVentasByOptica(opticaId): List<Venta>`
- Basic CRUD

### Sync Architecture

`SyncFinanzasUseCase` already downloads all Fase 6 data:
- `downloadVentas()`, `downloadResumenDiario()`, `downloadConfiguracionFinanciera()`
- These are called in the Finanzas sync pipeline

---

## Affected Areas

### Supabase (Migrations)
- `supabase/migrations/new_rpc_analisis_mensual.sql` — **CREATE** (new)
- `supabase/migrations/new_rpc_deudores.sql` — **CREATE** (new)
- `supabase/migrations/new_grant_recalcular.sql` — **MODIFY** (missing GRANT)
- `supabase/migrations/deprecate_rpc_resumen_financiero.sql` — **MODIFY** (add deprecation notice)
- `supabase/migrations/deprecate_rpc_saldo_pendiente.sql` — **MODIFY** (add deprecation + new function if needed)
- `supabase/migrations/update_rpc_count_pendientes.sql` — **MODIFY** (read from `ventas`)

### Android — New Files
- `domain/ObtenerAnalisisMensualUseCase.kt` — new UseCase
- `domain/ObtenerDeudoresUseCase.kt` — new UseCase
- `data/margenporcategoria/MargenPorCategoriaEntity.kt` — new Room entity
- `data/margenporcategoria/MargenPorCategoriaDao.kt` — new Room DAO
- `data/costoproducto/CostoProductoEntity.kt` — new Room entity
- `data/costoproducto/CostoProductoDao.kt` — new Room DAO

### Android — Modified Files
- `data/dispensacion/DispensacionEntity.kt` (Pago) — add `ventaId` field
- `data/pago/PagoDao.kt` — add `ventaId` queries (if needed)
- `data/resumendiario/ResumenDiarioDao.kt` — add monthly aggregation queries
- `data/resumendiario/ResumenDiarioEntity.kt` — may need updates
- `data/OptoDatabase.kt` — add new entities to entity list + abstract DAOs
- `data/OptoDatabaseMigrations.kt` — add migration for new Room tables + Pago schema change
- `data/OptoRepository.kt` — add new DAO references (margen, costo)
- `viewmodel/BIViewModel.kt` — integrate 8 business indicators
- `ui/screens/BIScreen.kt` — redesign for business language
- `viewmodel/BIViewModelTest.kt` — update tests

---

## Approaches

### 1. Direct Room Query (No RPC) — All indicators calculated in Android from local Room data

- **Description**: Skip Supabase RPCs entirely for the 8 indicators. Use Room DAO aggregation queries (SUM, GROUP BY) combined with Kotlin flow operations to calculate all indicators client-side.
- **Pros**:
  - Works fully offline without Supabase RPC dependency
  - Simpler to test (Room in-memory DB)
  - No GRANT/permission issues
  - Consistent with existing pattern (BIViewModel calculates locally)
- **Cons**:
  - Complex aggregation queries (deudores, proyección 30 días) harder in Room/SQLite
  - Must download all `ventas`, `pagos`, `monturas` data to calculate — more bandwidth
  - Proyección 30 días (indicator 6) requires trend analysis that's harder in Android
- **Effort**: Medium-High

### 2. Supabase RPC with Local Fallback — Recommended (per plan)

- **Description**: Create SQL RPCs (`rpc_analisis_mensual`, `rpc_deudores`) that do the heavy aggregation server-side. Android UseCases fetch from RPC when online, fall back to local Room aggregation when offline. Local Room `resumen_diario` provides a cached version of the monthly aggregates.
- **Pros**:
  - Matches the plan's architecture exactly
  - Server-side aggregation is more efficient (PostgreSQL window functions, CTEs)
  - Less data transferred to client (only aggregated results)
  - Room `resumen_diario` serves as cache for monthly indicators
- **Cons**:
  - More moving parts (RPC → Supabase client → UseCase)
  - Offline experience requires careful fallback to Room queries
  - Missing GRANT must be fixed
- **Effort**: Medium (matches plan)

### 3. Hybrid — RPC for heavy indicators, Room for simple ones

- **Description**: Use `rpc_analisis_mensual` for indicators 1-4 (monthly aggregation) and `rpc_deudores` for indicator 5 (calculates in Room from local `ventas` + `pagos`). Indicators 6-8 (stock, inventory, projection) use Room queries against `monturas` and `gastos_operativos` locally.
- **Pros**: Pragmatic balance; stock/inventory data is always local
- **Cons**: Two different fetching strategies to maintain; inconsistent error handling
- **Effort**: Medium

---

## Recommendation

**Approach 2 (RPC + Local Fallback per plan)**, with these specifics:

1. **Create `rpc_analisis_mensual`** on Supabase as per plan design (reads from `resumen_diario` + `gastos_operativos`)
2. **Create `rpc_deudores`** on Supabase as per plan design (JOIN `ventas` + `pagos` + `pacientes`)
3. **Fix missing GRANT** for `recalcular_resumen_diario` so the Android app can call it when the user opens the analytics screen
4. **Android UseCases** (`ObtenerAnalisisMensualUseCase`, `ObtenerDeudoresUseCase`) call Supabase RPC when online, fall back to Room queries when offline
5. Use `resumen_diario` local table as the **cache layer** for monthly indicators
6. Indicators 7 (stock estancado) and 8 (inventario) can use direct Room/MonturaDao queries since that data is always synced locally

This keeps the architecture aligned with the plan, maintains offline-first capability, and leverages PostgreSQL's aggregation strength.

---

## Risks

1. **Pago entity lacks `ventaId`** — The Android Pago entity doesn't have `ventaId` yet. Must add it for deudores queries and ledger consistency. This requires a Room migration + database version bump.
2. **Missing GRANT on `recalcular_resumen_diario`** — Without it, calling it from Android will fail with permission error. Must add in a new migration.
3. **No Room entities for `margen_por_categoria`** — If Android needs this for indicator 4 ("Lo que más te deja"), the entity must be created. However, the indicator can also be calculated server-side only and consumed via RPC.
4. **BIViewModel refactor risk** — Current BIViewModel has a complex reactive pipeline. Adding 8 indicators to the same flow-based architecture may increase complexity. Consider splitting into smaller ViewModels or UseCases.
5. **No existing `ObtenerAnalisisMensualUseCase`** — The pattern exists for sync UseCases, but not for analytical/business-query UseCases. The team needs to establish the pattern for these.
6. **`rpc_analisis_mensual` and `rpc_deudores` not yet defined** — Must be created as new SQL migrations, following the existing RPC pattern (SECURITY INVOKER, GRANT appropriate).

---

## Ready for Proposal

**Yes**. The codebase investigation is complete. Key gaps are well-understood:

1. Three new SQL functions needed (analisis_mensual, deudores, fix grant)
2. Two new Android UseCases needed
3. Two new Room entities needed (MergenPorCategoria, CostoProducto) — only if Android needs offline access to these
4. Pago entity needs `ventaId` field
5. BIViewModel + BIScreen needs redesign for 8 business indicators
6. MargenPorCategoria and CostoProducto Room entities may be optional if indicators use RPC only

The orchestrator should proceed to proposal phase with these gaps and the recommendation above.
