# Tasks: Fase 8 — 6 Reglas de Recomendación

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~380–450 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Delivery strategy | single-PR |
| TDD mode | Strict — RED before GREEN within each phase |

## Phase 1: Domain Models + Model Extensions

### Task 1.1 [TDD] — Add gastosMes to AnalisisMensual

- **ID**: `F8-GASTOS-MES`
- **Phase**: 1 — Domain Models
- **Dependencies**: None
- **Files**:
  - `optoapp/src/main/java/com/example/optoapp/domain/AnalisisMensual.kt` (MODIFY)
- **Tests**: `optoapp/src/test/java/com/example/optoapp/domain/AnalisisMensualMapperTest.kt` (MODIFY — expand existing)
- **Description**:
  - Write **RED** test: `fromJson_withGastosMes_parsesCorrectly()` — construct a `JsonElement` with `"gastos_mes": 3900.0`, assert `gastosMes == 3900.0`
  - Write **RED** test: `fromJson_withoutGastosMes_defaultsToZero()` — construct a `JsonElement` without `gastos_mes` key, assert `gastosMes == 0.0`
  - Implement **GREEN**:
    1. Add `val gastosMes: Double = 0.0` to `AnalisisMensual` data class constructor
    2. In `fromJson`, assign `gastosMes = obj.optDouble("gastos_mes")`
- **Acceptance Criteria**:
  - `AnalisisMensual.gastosMes` defaults to 0.0
  - `fromJson` parses `"gastos_mes"` from RPC JSONB
  - Missing key produces 0.0, not crash
  - All existing `AnalisisMensual` tests continue passing

### Task 1.2 [TDD] — Create Recomendacion domain model, enums, and DatosAccion

- **ID**: `F8-DOMAIN-MODELS`
- **Phase**: 1 — Domain Models
- **Dependencies**: None
- **Files**:
  - `optoapp/src/main/java/com/example/optoapp/domain/Recomendacion.kt` (CREATE)
- **Tests**: `optoapp/src/test/java/com/example/optoapp/domain/RecomendacionTest.kt` (CREATE)
- **Description**:
  - Write **RED** test: construct `Recomendacion` with all fields, verify each field value
  - Write **RED** test: `Recomendacion` with different `tipo` values compiles correctly
  - Write **RED** test: `Prioridad.ALTA` ordinal is 0 (for sorting)
  - Write **RED** test: `DatosAccion()` all-defaults is null for all fields
  - Write **RED** test: `DatosAccion(pacienteIds = listOf("a", "b"))` preserves values
  - Implement **GREEN**: create `Recomendacion.kt` with:
    ```kotlin
    package com.example.optoapp.domain

    data class Recomendacion(
        val id: String,
        val tipo: RecomendacionTipo,
        val titulo: String,
        val detalle: String,
        val impactoEstimado: String?,
        val prioridad: Prioridad,
        val accion: String?,
        val datosAccion: DatosAccion?
    )

    enum class RecomendacionTipo {
        COBRAR, MEJORAR_PRECIO, LIQUIDAR_STOCK,
        VENDER_MAS_DE, REDUCIR_GASTO, ALERTA_CAIDA
    }

    enum class Prioridad { ALTA, MEDIA, BAJA }

    data class DatosAccion(
        val pacienteIds: List<String>? = null,
        val productoIds: List<String>? = null,
        val montoTotal: Double? = null
    )
    ```
- **Acceptance Criteria**:
  - All 4 models compile and are constructable
  - `Prioridad.ALTA.ordinal == 0`, `MEDIA == 1`, `BAJA == 2` (enables comparator by ordinal)
  - Tests verify field assignment and null defaults
  - All models are in `domain/` package with no Android dependencies

### Task 1.3 [TDD] — Add getByOpticaIdOnce suspend function to ConfiguracionFinancieraDao

- **ID**: `F8-CONFIG-DAO`
- **Phase**: 1 — Domain Models
- **Dependencies**: None
- **Files**:
  - `optoapp/src/main/java/com/example/optoapp/data/configuracionfinanciera/ConfiguracionFinancieraDao.kt` (MODIFY)
- **Tests**: `optoapp/src/test/java/com/example/optoapp/data/configuracionfinanciera/ConfiguracionFinancieraDaoTest.kt` (MODIFY)
- **Description**:
  - Write **RED** test: add `getByOpticaIdOnce_returnsCorrectConfig()` to the existing DAO test — insert a config, call `getByOpticaIdOnce("o1")`, assert the returned entity matches
  - Write **RED** test: `getByOpticaIdOnce_returnsNullForMissing()` — call with unknown opticaId, assert null
  - Implement **GREEN**: add to `ConfiguracionFinancieraDao`:
    ```kotlin
    @Query("SELECT * FROM configuracion_financiera WHERE opticaId = :opticaId")
    suspend fun getByOpticaIdOnce(opticaId: String): ConfiguracionFinancieraEntity?
    ```
  - Existing `getByOpticaId` Flow method remains unchanged
- **Acceptance Criteria**:
  - New suspend function returns config for existing opticaId
  - Returns null for non-existent opticaId
  - Existing Flow-based method still works
  - All existing DAO tests pass

## Phase 2: Core Engine — GenerarRecomendacionesUseCase

### Task 2.1 [TDD] — Implement 6 rule functions (pure logic, private)

- **ID**: `F8-RULES`
- **Phase**: 2 — Core Engine
- **Dependencies**: Task 1.1 (gastosMes), Task 1.2 (Recomendacion model), Task 1.3 (config DAO)
- **Files**:
  - `optoapp/src/main/java/com/example/optoapp/domain/GenerarRecomendacionesUseCase.kt` (CREATE — rule functions only)
- **Tests**: `optoapp/src/test/java/com/example/optoapp/domain/GenerarRecomendacionesUseCaseTest.kt` (CREATE — rule-level tests)
- **Description**:
  - Write **RED** tests for each rule function independently (pure function tests, no mocking needed):

    **R1 — evaluarCobrar** (3 tests):
    - `cobrar_whenDeudaTotalExceedsThreshold_returnsRecomendacion()` — create deudores list with total S/ 4,200, config with `deudaTotalAlertaMonto = 3000`, assert `Recomendacion` returned with `tipo=COBRAR`, `prioridad=ALTA`, `detalle` contains "S/ 4,200"
    - `cobrar_whenOldDebtorExists_returnsRecomendacion()` — total S/ 1,500 (under threshold), but one debtor with 45 days > 30, assert recommendation returned
    - `cobrar_whenNoDebtors_returnsNull()` — empty list, assert null

    **R2 — evaluarMejorarPrecio** (3 tests):
    - `mejorarPrecio_whenLowMarginAboveThreshold_returnsRecomendacion()` — create category with `ventas = 960.0` (>= 5), `margenPct = 8.3`, assert `tipo=MEJORAR_PRECIO`, `detalle` contains category name
    - `mejorarPrecio_whenBelowMonetaryThreshold_returnsNull()` — `ventas = 2.0` (< 5), any margin, assert null
    - `mejorarPrecio_whenHealthyMargin_returnsNull()` — `margenPct = 30.0`, assert null

    **R3 — evaluarLiquidarStock** (3 tests):
    - `liquidarStock_whenItemsExceedDiasThreshold_returnsRecomendacion()` — 2 items with 210 días, 1 item with 45 días, config with `stockEstancadoAlertaDias = 180`, assert `tipo=LIQUIDAR_STOCK`, only 2 items in detalle
    - `liquidarStock_whenAllItemsBelowThreshold_returnsNull()` — 2 items with 30 and 90 días, assert null
    - `liquidarStock_whenEmptyList_returnsNull()` — empty list, assert null

    **R4 — evaluarVenderMasDe** (3 tests):
    - `venderMasDe_whenHighMarginHighContribution_returnsRecomendacion()` — category with `margenPct = 45.0`, contribution 35%, `ventas = 4800.0` (>= 5), assert `tipo=VENDER_MAS_DE`, `detalle` contains "45%"
    - `venderMasDe_whenHighMarginButLowContribution_returnsNull()` — `margenPct = 50.0` but contributes 2%, assert null
    - `venderMasDe_whenBelowMonetaryThreshold_returnsNull()` — `ventas = 3.0` (< 5), assert null

    **R5 — evaluarAlertaCaida** (3 tests):
    - `alertaCaida_whenDropExceedsThreshold_returnsRecomendacion()` — `variacionVentasPct = -15.0`, config with `caidaVentasAlertaPct = 10.0`, assert `tipo=ALERTA_CAIDA`, `detalle` contains "15%"
    - `alertaCaida_whenDropBelowThreshold_returnsNull()` — `variacionVentasPct = -3.0`, assert null
    - `alertaCaida_whenNullVariation_returnsNull()` — `variacionVentasPct = null`, assert null

    **R6 — evaluarReducirGasto** (3 tests):
    - `reducirGasto_whenRatioExceeds40Percent_returnsRecomendacion()` — `gastosMes = 3900.0`, `ventasMes = 9400.0`, assert `tipo=REDUCIR_GASTO`, `detalle` contains ratio info
    - `reducirGasto_whenRatioBelow40Percent_returnsNull()` — `gastosMes = 1000.0`, `ventasMes = 10000.0`, assert null
    - `reducirGasto_whenVentasMesIsZero_returnsNull()` — `ventasMes = 0.0`, assert no division crash, return null

  - Implement **GREEN**: create `GenerarRecomendacionesUseCase.kt` with:
    - `open class` (for test mocking, per project pattern)
    - `@Inject constructor` with all 3 dependencies (but rule functions don't use them directly yet — they receive parsed data)
    - 6 `private fun` rule methods, each returning `Recomendacion?`
    - Each rule creates its own `Recomendacion` with the `id = tipo.name + "::" + titulo` format
- **Acceptance Criteria**:
  - All 18 tests pass (6 rules × 3 scenarios each)
  - Each rule function is a pure function — all inputs passed as parameters, no IO or state
  - Proper null handling for edge cases (empty lists, zero divisions, null values)
  - `id` is deterministic: same inputs → same `id`

### Task 2.2 [TDD] — Implement orchestrator: invoke, prioritization, capping

- **ID**: `F8-ORCHESTRATOR`
- **Phase**: 2 — Core Engine
- **Dependencies**: Task 2.1 (rule functions exist)
- **Files**:
  - `optoapp/src/main/java/com/example/optoapp/domain/GenerarRecomendacionesUseCase.kt` (MODIFY — add `invoke` method with orchestration logic)
- **Tests**: Add to existing test file from 2.1
- **Description**:
  - Write **RED** tests with MockK:

    **Orchestration path** (3 tests):
    - `invoke_whenAllRulesFire_returnsCappedList()` — mock all 3 dependencies to return data that triggers all 6 rules. Assert the result has at most 5 items. Assert the dropped item is the lowest-priority MEDIA.
    - `invoke_whenNoRulesFire_returnsEmptyList()` — mock dependencies to return data where no rule triggers. Assert `Resource.Success(emptyList())`.
    - `invoke_whenAnalisisError_returnsError()` — mock `obtenerAnalisisMensual` to return `Resource.Error("sin conexion")`. Assert `Resource.Error` with same message.

    **Prioritization** (2 tests):
    - `invoke_prioridadOrderAltaBeforeMedia()` — mock data that triggers 2 ALTA rules and 1 MEDIA. Assert first 2 items have `prioridad=ALTA`, third is MEDIA.
    - `invoke_cappingKeepsHighestPriority()` — mock data that triggers 6 rules. Assert all 5 returned items have higher priority than the dropped one. If there are exactly 5 ALTA+MEDIA, all 5 are returned (no BAJA items).

    **Config DAO integration** (1 test):
    - `invoke_readsConfigFromDao()` — verify that `configuracionFinancieraDao.getByOpticaIdOnce()` is called with the correct opticaId.

  - Implement **GREEN**: add to `GenerarRecomendacionesUseCase`:
    ```kotlin
    suspend operator fun invoke(opticaId: String, mes: LocalDate): Resource<List<Recomendacion>> {
        // 1. Fetch inputs
        val analisisResult = obtenerAnalisisMensual(opticaId, mes)
        if (analisisResult is Resource.Error) return analisisResult

        val deudoresResult = obtenerDeudores(opticaId)
        if (deudoresResult is Resource.Error) return deudoresResult

        val config = configuracionFinancieraDao.getByOpticaIdOnce(opticaId)
            ?: return Resource.Error("Configuracion financiera no encontrada")

        val analisis = (analisisResult as Resource.Success).data
        val deudores = (deudoresResult as Resource.Success).data

        // 2. Evaluate rules
        val recommendations = listOfNotNull(
            evaluarCobrar(deudores, config),
            evaluarMejorarPrecio(analisis.margenPorCategoria, config),
            evaluarLiquidarStock(analisis.stockEstancado, config),
            evaluarVenderMasDe(analisis.margenPorCategoria, config),
            evaluarAlertaCaida(analisis, config),
            evaluarReducirGasto(analisis)
        )

        // 3. Sort by priority ordinal (ALTA=0, MEDIA=1, BAJA=2)
        val sorted = recommendations.sortedBy { it.prioridad.ordinal }

        // 4. Cap at 5
        return Resource.Success(sorted.take(5))
    }
    ```
- **Acceptance Criteria**:
  - Orchestrator calls all 3 dependencies in correct order
  - Error from any dependency propagates as `Resource.Error`
  - Prioritization works: ALTA > MEDIA > BAJA by ordinal
  - Capping works: max 5 items returned
  - At least 5 items: keep highest priority, trim lowest MEDIA or BAJA
  - Empty result: `Resource.Success(emptyList())` (not error)
  - All 6 pure rule functions from Task 2.1 remain unchanged

## Phase 3: Feedback Layer

### Task 3.1 [TDD] — Create FeedbackRecomendacionEntity + DAO

- **ID**: `F8-FEEDBACK-ENTITY`
- **Phase**: 3 — Feedback
- **Dependencies**: None (standalone Room entity)
- **Files**:
  - `optoapp/src/main/java/com/example/optoapp/data/feedbackrecomendacion/FeedbackRecomendacionEntity.kt` (CREATE)
  - `optoapp/src/main/java/com/example/optoapp/data/feedbackrecomendacion/FeedbackRecomendacionDao.kt` (CREATE)
- **Tests**: `optoapp/src/test/java/com/example/optoapp/data/FeedbackRecomendacionDaoTest.kt` (CREATE)
- **Description**:
  - Create directory `data/feedbackrecomendacion/` under main source
  - Write **RED** tests for DAO:
    - `upsert_insertsNewFeedback()` — create entity, upsert, assert `getByOpticaId` returns it
    - `upsert_updatesExistingFeedback()` — insert twice with same `recomendacionId` but different `fueUtil`, assert only 1 row exists with latest value
    - `getByOpticaId_returnsAllForOptica()` — insert 2 feedbacks for optica "o1" and 1 for "o2", assert "o1" returns exactly 2, ordered by fecha DESC
  - Implement **GREEN**:
    ```kotlin
    @Entity(
        tableName = "feedback_recomendaciones",
        indices = [Index(value = ["recomendacionId", "opticaId"])]
    )
    data class FeedbackRecomendacionEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val recomendacionId: String,
        val opticaId: String,
        val fueUtil: Boolean,
        val fecha: Long = System.currentTimeMillis()
    )
    ```
    ```kotlin
    @Dao
    interface FeedbackRecomendacionDao {
        @Upsert
        suspend fun upsert(feedback: FeedbackRecomendacionEntity)

        @Query("SELECT * FROM feedback_recomendaciones WHERE opticaId = :opticaId ORDER BY fecha DESC")
        suspend fun getByOpticaId(opticaId: String): List<FeedbackRecomendacionEntity>
    }
    ```
- **Acceptance Criteria**:
  - Entity stores `recomendacionId`, `opticaId`, `fueUtil`, `fecha`
  - DAO upsert is idempotent (same composite key updates, doesn't duplicate)
  - DAO query returns by opticaId, ordered newest first
  - All DAO tests pass with in-memory Room

### Task 3.2 [TDD] — Room migration v33→v34

- **ID**: `F8-MIGRATION-33-34`
- **Phase**: 3 — Feedback
- **Dependencies**: Task 3.1 (entity + DAO exist)
- **Files**:
  - `optoapp/src/main/java/com/example/optoapp/data/OptoDatabase.kt` (MODIFY — version 33→34, add entity, export migration)
  - `optoapp/src/main/java/com/example/optoapp/data/OptoDatabaseMigrations.kt` (MODIFY — add MIGRATION_33_34)
- **Tests**: `optoapp/src/test/java/com/example/optoapp/data/MIGRATION_33_34_Test.kt` (CREATE)
- **Description**:
  - Write **RED** test:
    - Create in-memory DB at version 33 with existing tables (`configuracion_financiera`, `ventas`, etc.)
    - Insert 2 rows in `configuracion_financiera` and 3 in `ventas`
    - Run `MIGRATION_33_34`
    - Assert all 5 existing rows preserved
    - Assert `feedback_recomendaciones` table exists
    - Assert inserting into `feedback_recomendaciones` works after migration
  - Implement **GREEN**:
    - In `OptoDatabaseMigrations.kt`:
      ```kotlin
      val MIGRATION_33_34 = object : Migration(33, 34) {
          override fun migrate(db: SupportSQLiteDatabase) {
              db.execSQL("""
                  CREATE TABLE IF NOT EXISTS feedback_recomendaciones (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      recomendacionId TEXT NOT NULL,
                      opticaId TEXT NOT NULL,
                      fueUtil INTEGER NOT NULL,
                      fecha INTEGER NOT NULL
                  )
              """)
              db.execSQL("""
                  CREATE INDEX IF NOT EXISTS index_feedback_recomendacionId_opticaId
                      ON feedback_recomendaciones(recomendacionId, opticaId)
              """)
          }
      }
      ```
    - In `OptoDatabase.kt`:
      - Bump `version = 34`
      - Add `FeedbackRecomendacionEntity::class` to `entities = [...]`
      - Re-export `MIGRATION_33_34` from companion object
      - Register in `.addMigrations(MIGRATION_33_34)`
- **Acceptance Criteria**:
  - Database version is 34
  - All v33 data preserved after migration
  - `feedback_recomendaciones` table + index created
  - New feedback entity can be inserted and queried post-migration
  - Migration is idempotent (`CREATE TABLE IF NOT EXISTS`)

### Task 3.3 [TDD] — Create FeedbackRecomendacionUseCase

- **ID**: `F8-FEEDBACK-USECASE`
- **Phase**: 3 — Feedback
- **Dependencies**: Task 3.1 (DAO exists)
- **Files**:
  - `optoapp/src/main/java/com/example/optoapp/domain/FeedbackRecomendacionUseCase.kt` (CREATE)
- **Tests**: `optoapp/src/test/java/com/example/optoapp/domain/FeedbackRecomendacionUseCaseTest.kt` (CREATE)
- **Description**:
  - Write **RED** tests with MockK:
    - `marcarUtil_callsDaoWithFueUtilTrue()` — mock DAO, call `marcarUtil("abc", "o1")`, verify `dao.upsert()` was called with entity having `recomendacionId="abc"`, `opticaId="o1"`, `fueUtil=true`
    - `marcarNoUtil_callsDaoWithFueUtilFalse()` — same pattern, verify `fueUtil=false`
    - `marcarUtil_twice_isIdempotent()` — call twice, verify `dao.upsert()` called twice but only 1 row exists (DAO handles dedup, but verify the UseCase doesn't add extra logic violating idempotence)
  - Implement **GREEN**:
    ```kotlin
    package com.example.optoapp.domain

    import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionDao
    import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionEntity
    import javax.inject.Inject

    open class FeedbackRecomendacionUseCase @Inject constructor(
        private val feedbackRecomendacionDao: FeedbackRecomendacionDao
    ) {
        suspend fun marcarUtil(recomendacionId: String, opticaId: String) {
            feedbackRecomendacionDao.upsert(
                FeedbackRecomendacionEntity(
                    recomendacionId = recomendacionId,
                    opticaId = opticaId,
                    fueUtil = true
                )
            )
        }

        suspend fun marcarNoUtil(recomendacionId: String, opticaId: String) {
            feedbackRecomendacionDao.upsert(
                FeedbackRecomendacionEntity(
                    recomendacionId = recomendacionId,
                    opticaId = opticaId,
                    fueUtil = false
                )
            )
        }
    }
    ```
- **Acceptance Criteria**:
  - `marcarUtil` upserts entity with `fueUtil = true`
  - `marcarNoUtil` upserts entity with `fueUtil = false`
  - Both methods are `suspend`
  - UseCase is `open class` with `@Inject constructor`
  - `id` auto-generated (0 default, Room assigns on insert)

## Phase 4: Compilation Guard + Full Test Suite

### Task 4.1 — Compilation guard and full test run

- **ID**: `F8-COMPILATION-GUARD`
- **Phase**: 4 — Tests
- **Dependencies**: All Phase 1 + Phase 2 + Phase 3 tasks
- **Description**:
  - Verify the project compiles: `./gradlew :optoapp:assembleDebug`
  - Verify all tests pass: `./gradlew :optoapp:testDebugUnitTest --stacktrace`
  - Run JaCoCo: `./gradlew :optoapp:jacocoTestReport` (coverage threshold: 5%)
- **Acceptance Criteria**:
  - `:optoapp:assembleDebug` succeeds (zero errors)
  - `:optoapp:testDebugUnitTest` passes all tests (both new and existing)
  - `:optoapp:jacocoTestReport` reports ≥ 5% instruction coverage
  - No existing tests broken by the changes

## Phase Dependencies Summary

```
F8-GASTOS-MES        ── standalone ───────────────────────────────────┐
F8-DOMAIN-MODELS     ── standalone ───────────────────────────────────┤
F8-CONFIG-DAO        ── standalone ───────────────────────────────────┤
                                                                      │
F8-RULES             ── depends-on: F8-GASTOS-MES, F8-DOMAIN-MODELS,  │
                                     F8-CONFIG-DAO ───────────────────┤
F8-ORCHESTRATOR      ── depends-on: F8-RULES ─────────────────────────┤
                                                                      │
F8-FEEDBACK-ENTITY   ── standalone ───────────────────────────────────┤
F8-MIGRATION-33-34   ── depends-on: F8-FEEDBACK-ENTITY ───────────────┤
F8-FEEDBACK-USECASE  ── depends-on: F8-FEEDBACK-ENTITY ───────────────┤
                                                                      │
F8-COMPILATION-GUARD ── depends-on: ALL ──────────────────────────────┘
```

## Delivery Order

1. **F8-GASTOS-MES** + **F8-DOMAIN-MODELS** + **F8-CONFIG-DAO** + **F8-FEEDBACK-ENTITY** (parallel — all standalone)
2. **F8-RULES** (after Phase 1 domain models)
3. **F8-ORCHESTRATOR** (after rules)
4. **F8-MIGRATION-33-34** + **F8-FEEDBACK-USECASE** (parallel, after feedback entity)
5. **F8-COMPILATION-GUARD** (last — verifies everything together)

## Notes for Apply

- `GenerarRecomendacionesUseCase` must be `open class` (per project pattern for MockK mocking in tests)
- Rule functions are `private fun` inside the UseCase — they are NOT accessible from tests directly. Test them by setting up the mock dependencies to return specific data and calling `invoke()`, then assert the resulting `Recomendacion` list. The pure-function approach means: if mock A returns data X and mock B returns data Y, then rule N produces (or doesn't produce) a specific recommendation. This makes the tests both readable and robust.
- Follow the existing `CancellationException` rethrow pattern from Fase 7 UseCases
- The `id` field uses the raw string `"${tipo.name}::${titulo}"` — no hashing. This is deterministic and unique enough for feedback lookup.
- `ConfiguracionFinancieraDao.getByOpticaIdOnce` — add a suspend `@Query` method (not `Flow`)
- Gastos threshold for R6 is hardcoded at 0.4 (40%). This is a business constant, not from config. Define as `private const val GASTOS_VENTAS_RATIO_ALERTA = 0.4` in the UseCase companion object.
- Migration script uses `CREATE TABLE IF NOT EXISTS` for idempotency. New migration timestamp for Supabase side: none needed (no SQL changes).
