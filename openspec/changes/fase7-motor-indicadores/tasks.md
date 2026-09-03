# Tasks: Fase 7 — Motor de 8 indicadores en lenguaje de negocio

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~480–550 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Delivery strategy | single-PR |
| TDD mode | Strict — RED before GREEN within each phase |

## Phase 1: Supabase Migrations

### Task 1.1 — Create 5 migration files for new RPCs, GRANT fix, update, and deprecations

- **ID**: `F7-SUPABASE`
- **Phase**: 1 — Supabase
- **Dependencies**: None
- **Files**:
  - `supabase/migrations/20260706000000_fase7_rpc_analisis_mensual.sql` (CREATE)
  - `supabase/migrations/20260706000001_fase7_rpc_deudores.sql` (CREATE)
  - `supabase/migrations/20260706000002_fase7_fix_grant_recalcular.sql` (CREATE)
  - `supabase/migrations/20260706000003_fase7_update_rpc_count_pendientes.sql` (CREATE)
  - `supabase/migrations/20260706000004_fase7_deprecations.sql` (CREATE)
- **Tests**: Manual SQL validation (no automated test framework for RPCs in this project)
- **Description**: Five idempotent migration files following the existing `SECURITY INVOKER` + `GRANT` pattern:

  1. **`rpc_analisis_mensual`** — `CREATE OR REPLACE FUNCTION public.rpc_analisis_mensual(p_optica_id TEXT, p_mes DATE) RETURNS jsonb LANGUAGE plpgsql SECURITY INVOKER STABLE`. Computes all 8 indicators + month-over-month comparison from `resumen_diario`, `ventas`, `pagos`, `gastos_operativos`, `monturas`, `montura_movimientos`. Returns JSONB with keys: `ventas_mes`, `ventas_mes_anterior`, `variacion_ventas_pct`, `cobros_mes`, `margen_neto_pct`, `margen_por_categoria` (JSONB array), `deudores` (JSONB: count + total), `proyeccion_caja` (JSONB or null), `stock_estancado` (JSONB array), `valor_inventario`.

  2. **`rpc_deudores`** — `CREATE OR REPLACE FUNCTION public.rpc_deudores(p_optica_id TEXT) RETURNS TABLE(paciente_nombre TEXT, paciente_telefono TEXT, venta_id TEXT, venta_fecha DATE, monto_total NUMERIC, total_pagado NUMERIC, saldo NUMERIC, dias_deuda INTEGER) LANGUAGE sql SECURITY INVOKER STABLE`. JOINs `ventas` + `pagos` + `pacientes`, filters `HAVING saldo > 0.005`, orders by `dias_deuda DESC`.

  3. **GRANT fix** for `recalcular_resumen_diario` — `GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario TO authenticated` (missing from Fase 6).

  4. **Rewrite `rpc_count_pendientes`** — Replace body to read from `ventas` instead of old `dispensaciones` + `servicios_extra`. Keep same signature and return type (JSONB). Use `CREATE OR REPLACE FUNCTION`.

  5. **Deprecation comments** — `COMMENT ON FUNCTION public.rpc_resumen_financiero IS 'DEPRECATED: Use rpc_analisis_mensual instead'` + same for `rpc_saldo_pendiente`. No DROP.

- **Acceptance Criteria**:
  - `rpc_analisis_mensual('o1', '2026-07-01')` returns JSONB with all 8 indicator keys as numeric values (0 for empty months, no errors)
  - `rpc_deudores('o1')` returns rows ordered by `dias_deuda DESC` with only positive-saldos (> 0.005)
  - `recalcular_resumen_diario` callable by `authenticated` role (GRANT fix validated)
  - `rpc_count_pendientes('o1')` returns count matching `ventas` pending balance query
  - `rpc_resumen_financiero` and `rpc_saldo_pendiente` still execute normally with deprecation comment visible in `\df+`
  - Each migration is idempotent (rerunnable)
  - Timestamps follow the `20260706*` sequence (after Fase 6 at `20260705*`)

## Phase 2: Room Data Layer

### Task 2.1 [TDD] — Add ventaId to Pago entity

- **ID**: `F7-PAGO-VENTAID`
- **Phase**: 2 — Room Data Layer
- **Dependencies**: None (compilation-only change, no DB migration yet)
- **Files**:
  - `optoapp/src/main/java/com/example/optoapp/data/dispensacion/DispensacionEntity.kt` (MODIFY)
- **Tests**: `optoapp/src/test/java/com/example/optoapp/data/PagoEntityTest.kt` (CREATE)
- **Description**:
  - Add `val ventaId: String? = null` + `@SerialName("ventaId")` to the `Pago` data class
  - Field is nullable with default null — existing constructors continue compiling
  - No DB migration yet (schema change and migration are separate tasks)
  - Write test first: verify `Pago(ventaId = "abc").ventaId == "abc"` and `Pago().ventaId == null`
- **Acceptance Criteria**:
  - `Pago` data class has a `ventaId: String?` field (nullable, default null)
  - `@SerialName("ventaId")` annotation present for kotlinx.serialization compatibility
  - Existing code that constructs `Pago(...)` without `ventaId` compiles without changes
  - Test: default value is null
  - Test: explicit value is preserved

### Task 2.2 [TDD] — Room migration v32→v33 (MIGRATION_32_33)

- **ID**: `F7-MIGRATION-32-33`
- **Phase**: 2 — Room Data Layer
- **Dependencies**: Task 2.1 (Pago.ventaId field must exist in entity)
- **Files**:
  - `optoapp/src/main/java/com/example/optoapp/data/OptoDatabaseMigrations.kt` (MODIFY)
  - `optoapp/src/main/java/com/example/optoapp/data/OptoDatabase.kt` (MODIFY)
- **Tests**:
  - `optoapp/src/test/java/com/example/optoapp/data/MIGRATION_32_33_Test.kt` (CREATE)
- **Description**:
  - Write **RED** test first: create in-memory DB at version 32, insert 3 `Pago` rows, run `MIGRATION_32_33`, assert rows preserved and `ventaId` column exists with NULL for existing rows
  - Implement **GREEN**: add `MIGRATION_32_33` to `OptoDatabaseMigrations.kt`:
    ```sql
    ALTER TABLE pagos ADD COLUMN ventaId TEXT
    CREATE INDEX IF NOT EXISTS index_pagos_ventaId ON pagos(ventaId)
    ```
  - Bump `OptoDatabase` version from 32 to 33
  - Add `@Database` entity list unchanged (Pago already registered, no new entity)
  - Re-export `MIGRATION_32_33` in `OptoDatabase` companion object
  - Register `MIGRATION_32_33` in `.addMigrations(...)` builder call
- **Acceptance Criteria**:
  - Migration from v32→v33 preserves all existing Pago rows (verify with test)
  - `ventaId` column exists in `pagos` table with NULL for rows migrated from v32
  - Index `index_pagos_ventaId` exists on `pagos(ventaId)`
  - `OptoDatabase.version == 33`
  - All existing tests pass with the new version

### Task 2.3 [TDD] — Add monthly aggregation query to ResumenDiarioDao

- **ID**: `F7-RD-MONTHLY`
- **Phase**: 2 — Room Data Layer
- **Dependencies**: None (can be done in parallel with 2.1/2.2)
- **Files**:
  - `optoapp/src/main/java/com/example/optoapp/data/resumendiario/ResumenDiarioDao.kt` (MODIFY)
  - `optoapp/src/test/java/com/example/optoapp/data/resumendiario/ResumenDiarioDaoTest.kt` (MODIFY)
- **Description**:
  - Write **RED** test first: add `getByOpticaAndMonth_returnsRowsForYearMonth()` to `ResumenDiarioDaoTest`:
    - Insert rows with fechas `2026-07-01` through `2026-07-05` and some in other months
    - Assert that `getByOpticaAndMonth("o1", "2026-07")` returns exactly the 5 July rows
    - Assert that `getByOpticaAndMonth("o1", "2026-06")` returns empty list
  - Implement **GREEN**: add to `ResumenDiarioDao`:
    ```kotlin
    @Query("""
        SELECT * FROM resumen_diario
        WHERE opticaId = :opticaId
          AND strftime('%Y-%m', fecha) = :yearMonth
        ORDER BY fecha ASC
    """)
    suspend fun getByOpticaAndMonth(opticaId: String, yearMonth: String): List<ResumenDiarioEntity>
    ```
  - Verify existing tests still pass
- **Acceptance Criteria**:
  - `getByOpticaAndMonth` returns only rows matching the year-month pattern
  - Returns empty list when no rows match
  - Results ordered by `fecha ASC` (oldest first, for SUM calculation)
  - Existing `getByOpticaId` Flow query remains unchanged
  - All existing DAO tests pass

## Phase 3: Domain Models + UseCases

### Task 3.1 — Create AnalisisMensual and Deudor domain models

- **ID**: `F7-DOMAIN-MODELS`
- **Phase**: 3 — Domain Layer
- **Dependencies**: None (pure Kotlin data classes, no Room/Supabase dependency)
- **Files**:
  - `optoapp/src/main/java/com/example/optoapp/domain/AnalisisMensual.kt` (CREATE)
  - `optoapp/src/main/java/com/example/optoapp/domain/Deudor.kt` (CREATE)
- **Tests**:
  - `optoapp/src/test/java/com/example/optoapp/domain/AnalisisMensualMapperTest.kt` (CREATE)
- **Description**:

  **AnalisisMensual.kt** — domain model with all 8 indicators:
  ```kotlin
  package com.example.optoapp.domain

  data class AnalisisMensual(
      val ventasMes: Double,
      val cobrosMes: Double,
      val margenNetoPct: Double,
      val margenPorCategoria: List<MargenCategoria>,
      val deudores: DeudoresResumen,
      val proyeccionCaja: ProyeccionCaja?,
      val stockEstancado: List<StockEstancadoItem>,
      val valorInventario: Double,
      val ventasMesAnterior: Double,
      val variacionVentasPct: Double?,
      val esOffline: Boolean = false
  )

  data class MargenCategoria(
      val categoria: String,
      val ventas: Double,
      val costos: Double,
      val margenPct: Double?
  )

  data class DeudoresResumen(
      val cantidad: Int,
      val saldoTotal: Double
  )

  data class ProyeccionCaja(
      val ingresosEsperados: Double,
      val egresosProgramados: Double,
      val saldoNeto: Double
  )

  data class StockEstancadoItem(
      val monturaId: String,
      val sku: String,
      val modelo: String,
      val costo: Double,
      val stockActual: Int,
      val ultimaVenta: String?,
      val diasSinVenta: Int
  )
  ```

  **Deudor.kt**:
  ```kotlin
  package com.example.optoapp.domain

  data class Deudor(
      val pacienteNombre: String,
      val pacienteTelefono: String,
      val ventaId: String,
      val ventaFecha: LocalDate,
      val montoTotal: Double,
      val totalPagado: Double,
      val saldo: Double,
      val diasDeuda: Int
  )
  ```

  **Mapper** from RPC JSON to domain models:
  - Companion object `AnalisisMensual.fromJson(json: JsonElement): AnalisisMensual` — parse JSONB response, handle nulls (treat missing keys as 0)
  - Write test: `fromJson()` with full response, empty response, partial/missing keys
  - Write test: `fromJson()` with null `variacionVentasPct` (when no previous month data)

- **Acceptance Criteria**:
  - `AnalisisMensual` contains all 8 indicator fields + auxiliary models
  - `fromJson()` maps Supabase JSONB response correctly (numeric values, nested JSONB arrays for `margen_por_categoria`, `stock_estancado`)
  - Missing keys produce 0 or null (not crash)
  - `esOffline` defaults to `false`
  - `Deudor` has all 8 fields matching the RPC TABLE return columns
  - All mapper tests pass with edge cases (nulls, empty arrays, zero values)

### Task 3.2 [TDD] — ObtenerAnalisisMensualUseCase

- **ID**: `F7-USECASE-ANALISIS`
- **Phase**: 3 — Domain Layer
- **Dependencies**: Task 2.3 (ResumenDiarioDao.getByOpticaAndMonth), Task 3.1 (AnalisisMensual model + mapper)
- **Files**:
  - `optoapp/src/main/java/com/example/optoapp/domain/ObtenerAnalisisMensualUseCase.kt` (CREATE)
- **Tests**:
  - `optoapp/src/test/java/com/example/optoapp/domain/ObtenerAnalisisMensualUseCaseTest.kt` (CREATE)
- **Description**:
  - Write **RED** test first with MockK:
    - **Online path**: mock `SupabaseClient.postgrest.rpc("rpc_analisis_mensual")` to return a `JsonElement`. Verify `invoke()` returns `Resource.Success<AnalisisMensual>` with mapping applied. Verify RPC was called with correct params.
    - **Offline path**: mock RPC call to throw `IOException`. Mock `ResumenDiarioDao.getByOpticaAndMonth()` to return 2 rows. Verify fallback returns `Resource.Success` with `esOffline=true` and indicators 1-4 computed from Room data (SUM of ventasMontoTotal, cobrosMontoTotal, etc.). Verify indicators 5-8 are 0/empty.
    - **Unexpected error path**: mock RPC to throw `RuntimeException`. Verify `Resource.Error` is returned.
  - Implement **GREEN**:
    ```kotlin
    class ObtenerAnalisisMensualUseCase @Inject constructor(
        private val supabaseClient: SupabaseClient,
        private val resumenDiarioDao: ResumenDiarioDao
    ) {
        suspend operator fun invoke(opticaId: String, mes: LocalDate): Resource<AnalisisMensual> {
            return try {
                val response = supabaseClient.postgrest.rpc("rpc_analisis_mensual") {
                    param("p_optica_id", opticaId)
                    param("p_mes", mes.toString())
                }
                val analisis = AnalisisMensual.fromJson(response)
                Resource.Success(analisis)
            } catch (e: IOException) {
                fallbackToRoom(opticaId, mes)
            }
        }

        private suspend fun fallbackToRoom(opticaId: String, mes: LocalDate): Resource<AnalisisMensual> {
            val yearMonth = "${mes.year}-${mes.monthValue.toString().padStart(2, '0')}"
            val rows = resumenDiarioDao.getByOpticaAndMonth(opticaId, yearMonth)
            return Resource.Success(AnalisisMensual(
                ventasMes = rows.sumOf { it.ventasMontoTotal },
                cobrosMes = rows.sumOf { it.cobrosMontoTotal },
                margenNetoPct = 0.0,
                margenPorCategoria = emptyList(),
                deudores = DeudoresResumen(0, 0.0),
                proyeccionCaja = null,
                stockEstancado = emptyList(),
                valorInventario = rows.lastOrNull()?.inventarioValor ?: 0.0,
                ventasMesAnterior = 0.0,
                variacionVentasPct = null,
                esOffline = true
            ))
        }
    }
    ```
- **Acceptance Criteria**:
  - Online: calls `rpc_analisis_mensual` with `p_optica_id` and `p_mes` params, maps JSONB to `AnalisisMensual`
  - Offline: catches `IOException`, falls back to `resumenDiarioDao.getByOpticaAndMonth`, returns limited data with `esOffline=true`
  - Unexpected error: returns `Resource.Error` (not crash)
  - Hilt `@Inject` constructor annotated

### Task 3.3 [TDD] — ObtenerDeudoresUseCase

- **ID**: `F7-USECASE-DEUDORES`
- **Phase**: 3 — Domain Layer
- **Dependencies**: Task 3.1 (Deudor model), Task 2.1 (Pago.ventaId for local JOIN)
- **Files**:
  - `optoapp/src/main/java/com/example/optoapp/domain/ObtenerDeudoresUseCase.kt` (CREATE)
- **Tests**:
  - `optoapp/src/test/java/com/example/optoapp/domain/ObtenerDeudoresUseCaseTest.kt` (CREATE)
- **Description**:
  - Write **RED** test first with MockK:
    - **Online path**: mock `supabase.postgrest.rpc("rpc_deudores")` returning 3 debtor rows. Verify `invoke()` returns `Resource.Success<List<Deudor>>` with correct mapping.
    - **Offline path**: mock RPC to throw `IOException`. Mock `VentaDao.getAllVentasByOptica()` and `PagoDao.getPagosListByOptica()`. Verify fallback performs client-side JOIN and returns debtors sorted by oldest first.
    - **Empty case**: mock RPC returning empty set. Verify empty list returned.
  - Implement **GREEN**:
    ```kotlin
    class ObtenerDeudoresUseCase @Inject constructor(
        private val supabaseClient: SupabaseClient,
        private val ventaDao: VentaDao,
        private val pagoDao: PagoDao
    ) {
        suspend operator fun invoke(opticaId: String): Resource<List<Deudor>> {
            return try {
                val response = supabaseClient.postgrest.rpc("rpc_deudores") {
                    param("p_optica_id", opticaId)
                }
                val deudores = response // map JsonArray to List<Deudor>
                Resource.Success(deudores)
            } catch (e: IOException) {
                fallbackToRoom(opticaId)
            }
        }

        private suspend fun fallbackToRoom(opticaId: String): Resource<List<Deudor>> {
            val ventas = ventaDao.getAllVentasByOptica(opticaId)
            val pagos = pagoDao.getPagosListByOptica(opticaId)
            // Client-side GROUP BY: compute totalPagado per ventaId from pagos
            // Filter HAVING saldo > 0.005, order by fecha ASC
            // Require pacientes loaded from pacienteDao for nombres
            // Return List<Deudor>
        }
    }
    ```
  - NOTE: Offline fallback for Deudores requires patient names. Either inject `PacienteDao` or limit offline deudores to basic info. Decision per design: offline fallback returns debtors from local cache with available data. If paciente names unavailable locally, return empty nombre + telefono.
- **Acceptance Criteria**:
  - Online: calls `rpc_deudores` and maps response to `List<Deudor>` with all fields
  - Offline: catches `IOException`, performs local JOIN from Room data
  - Debtors ordered by oldest first (ASC `ventaFecha` or `diasDeuda` DESC)
  - Only debtors with `saldo > 0.005` included
  - Empty debtors list handled (returns empty list, not error)
  - Hilt `@Inject` constructor annotated

## Phase 4: Remaining Tests — Edge Cases & Integration

### Task 4.1 — DAO integration test for getByOpticaAndMonth with real migration

- **ID**: `F7-TEST-DAO-INTEGRATION`
- **Phase**: 4 — Tests
- **Dependencies**: Task 2.3 (DAO query), Task 2.2 (migration)
- **Files**:
  - `optoapp/src/test/java/com/example/optoapp/data/resumendiario/ResumenDiarioDaoTest.kt` (MODIFY)
- **Description**:
  - Add test to `ResumenDiarioDaoTest`:
    - `getByOpticaAndMonth_correctlySumsVentas()` — insert 10 rows for July 2026 with varying `ventasMontoTotal` values. Call `getByOpticaAndMonth("o1", "2026-07")`. Assert the returned list has 10 rows. Compute `SUM(ventasMontoTotal)` client-side and verify against expected total.
    - `getByOpticaAndMonth_excludesOtherMonths()` — insert rows for June, July, August. Query for July-only. Assert exactly July rows returned.
    - `getByOpticaAndMonth_returnsEmptyForNoData()` — query a month with no rows. Assert empty list.
  - These tests run against the actual Room DB (in-memory) and validate the DAO works end-to-end with the real schema (v33).
- **Acceptance Criteria**:
  - All DAO tests pass with the final v33 schema
  - SUM aggregation computed correctly client-side from returned rows
  - Month filtering works correctly via `strftime` pattern matching
  - Empty month returns empty list (not crash)

### Task 4.2 — Stale context check and compilation guard

- **ID**: `F7-COMPILATION-GUARD`
- **Phase**: 4 — Tests
- **Dependencies**: All Phase 2 + Phase 3 tasks
- **Files**: All modified files
- **Description**:
  - Verify the project compiles: `./gradlew :optoapp:assembleDebug`
  - Verify all existing tests pass: `./gradlew :optoapp:testDebugUnitTest --stacktrace`
  - Run JaCoCo to confirm no coverage regression below 30% threshold: `./gradlew :optoapp:jacocoCoverageVerification`
  - Check that no existing call sites for `Pago(...)` lack the new `ventaId` parameter (compiler-enforced since it has a default)
  - Verify no imports broken by the entity change
- **Acceptance Criteria**:
  - `:optoapp:assembleDebug` succeeds with zero errors
  - `:optoapp:testDebugUnitTest` passes all tests (both new and existing)
  - `:optoapp:jacocoTestReport` reports ≥5% instruction coverage (no regression)

## Phase Dependencies Summary

```
F7-SUPABASE          ── standalone ──────────────────────────────────┐
F7-PAGO-VENTAID      ── standalone ─────────────────────────────┐     │
F7-MIGRATION-32-33   ── depends-on: F7-PAGO-VENTAID ───────┐   │     │
F7-RD-MONTHLY        ── standalone ─────────────────────┐   │   │     │
F7-DOMAIN-MODELS     ── standalone ───────────────┐     │   │   │     │
F7-USECASE-ANALISIS  ── depends-on: F7-RD-MONTHLY  │     │   │   │   │
                       depends-on: F7-DOMAIN-MODELS │ ────┤   │   │   │
F7-USECASE-DEUDORES  ── depends-on: F7-PAGO-VENTAID       │   │   │   │
                       depends-on: F7-DOMAIN-MODELS        │   │   │   │
F7-TEST-DAO-INTEGRATION ── depends-on: F7-RD-MONTHLY      │   │   │   │
                          depends-on: F7-MIGRATION-32-33   │   │   │   │
F7-COMPILATION-GUARD ── depends-on: ALL ───────────────────┴───┴───┴───┘
```

## Delivery Order

1. **F7-SUPABASE** (standalone, can go first)
2. **F7-PAGO-VENTAID** + **F7-RD-MONTHLY** + **F7-DOMAIN-MODELS** (parallelizable)
3. **F7-MIGRATION-32-33** (after PAGO-VENTAID)
4. **F7-USECASE-ANALISIS** (after RD-MONTHLY + DOMAIN-MODELS)
5. **F7-USECASE-DEUDORES** (after PAGO-VENTAID + DOMAIN-MODELS)
6. **F7-TEST-DAO-INTEGRATION** (after RD-MONTHLY + MIGRATION-32-33)
7. **F7-COMPILATION-GUARD** (last — verifies everything together)

## Notes for Apply

- Follow the `GRANT` + `REVOKE` pattern from existing migrations (e.g., `20260513000000_rpc_financial_aggregates.sql`): always `REVOKE EXECUTE FROM public, anon` then `GRANT EXECUTE TO authenticated, service_role`
- Migration timestamps start at `20260706` (the day after Fase 6's `20260705`)
- MIGRATION_32_33 must be registered in BOTH `companion object` (re-export) AND the `.addMigrations(...)` builder call — see `OptoDatabase.kt` lines 81–115 for the pattern
- The `Pago` entity lives in `DispensacionEntity.kt` alongside `DispensacionOptica`, `ServicioExtra`, `Montura`, `MonturaMovimiento`
- All new domain models and UseCases go under `optoapp/src/main/java/com/example/optoapp/domain/`
- IOExceptions trigger fallback; `CancellationException` must be rethrown (per existing project pattern)
- No changes to `OptoRepository`, `BIViewModel`, or `BIScreen` — those belong to Fase 9
