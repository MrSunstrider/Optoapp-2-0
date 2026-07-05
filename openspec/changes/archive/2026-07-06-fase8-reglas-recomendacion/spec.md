# Delta Spec: Fase 8 — 6 Reglas de Recomendación

## Domain: indicadores-negocio (Modified)

### Extensions to existing domain models

#### Requirement: MargenCategoria gains gastosMes field

The `AnalisisMensual` domain model SHALL add:

```kotlin
val gastosMes: Double = 0.0
```

And the `fromJson` parser SHALL read the key `gastos_mes` from the RPC response JSONB. This is required for R6 evaluation (gastosMes / ventasMes > 0.4).

**Rationale**: The Supabase RPC `rpc_analisis_mensual` already returns `gastos_mes` in its JSONB output (line 1027 of the architecture document). The domain model simply did not parse it. No RPC changes needed.

##### Scenario: RPC response with gastos_mes is parsed correctly

- GIVEN an RPC JSONB response with `"gastos_mes": 3900.0`
- WHEN `AnalisisMensual.fromJson(json)` is called
- THEN `gastosMes == 3900.0`

##### Scenario: RPC response without gastos_mes defaults to 0

- GIVEN an RPC JSONB response missing the `gastosMes` key (e.g., old cached response)
- WHEN `AnalisisMensual.fromJson(json)` is called
- THEN `gastosMes == 0.0` (no crash)

### ADDED Requirements

#### Requirement: R1 — Recomendacion domain model + RecomendacionTipo enum

The system SHALL create the following domain models in a new file `domain/Recomendacion.kt`:

```kotlin
data class Recomendacion(
    val id: String,                   // SHA-256 hash of tipo + titulo for dedup + feedback
    val tipo: RecomendacionTipo,
    val titulo: String,               // single plain-language phrase
    val detalle: String,              // 2-3 sentences with concrete names and amounts
    val impactoEstimado: String?,     // "Impacto estimado: +S/ 320 este mes"
    val prioridad: Prioridad,         // ALTA, MEDIA, BAJA
    val accion: String?,              // what to do, step by step
    val datosAccion: DatosAccion?     // structured data for action buttons
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

##### Scenario: Recomendacion can be constructed with all fields

- GIVEN all field values for a recommendation
- WHEN `Recomendacion(...)` is constructed
- THEN the instance has the correct `tipo`, `prioridad`, `titulo`, `detalle`, `impactoEstimado`, `accion`, and `datosAccion`
- AND `id` is a non-empty String

##### Scenario: DatosAccion defaults to null collections

- GIVEN `DatosAccion()` is constructed with no arguments
- THEN `pacienteIds`, `productoIds`, and `montoTotal` are all null

#### Requirement: R2 — GenerarRecomendacionesUseCase orchestrates 6 rules

The system SHALL create `domain/GenerarRecomendacionesUseCase.kt` with Hilt `@Inject` constructor:

```kotlin
class GenerarRecomendacionesUseCase @Inject constructor(
    private val obtenerAnalisisMensual: ObtenerAnalisisMensualUseCase,
    private val obtenerDeudores: ObtenerDeudoresUseCase,
    private val configuracionFinancieraDao: ConfiguracionFinancieraDao
) {
    suspend operator fun invoke(opticaId: String, mes: LocalDate): Resource<List<Recomendacion>>
}
```

The `invoke` method SHALL:
1. Call `obtenerAnalisisMensual(opticaId, mes)` to get `AnalisisMensual`
2. Call `obtenerDeudores(opticaId)` to get `List<Deudor>`
3. Read `ConfiguracionFinancieraEntity` from DAO
4. Evaluate each of the 6 rules as private functions
5. Collect all matching recommendations into a list
6. Sort by priority (ALTA first, MEDIA second, BAJA third)
7. Cap at 5 items, keeping highest-priority ones
8. Return `Resource.Success<List<Recomendacion>>`

##### Scenario: All 6 rules fire with matching data

- GIVEN an optica with debtors >= threshold, low-margin category, stagnant stock, high-margin category, falling sales, and high expenses
- WHEN `invoke("o1", LocalDate.of(2026, 7, 1))` is called
- THEN a list of up to 5 `Recomendacion` items is returned
- AND all 6 tipos are present in the result set (before capping)
- AND the list is sorted with ALTA before MEDIA

##### Scenario: No rules fire with clean data

- GIVEN an optica with no debtors, healthy margins, active stock, stable sales, low expenses
- WHEN `invoke("o1", LocalDate.of(2026, 7, 1))` is called
- THEN `Resource.Success(emptyList())` is returned

##### Scenario: Error in one dependency propagates as Resource.Error

- GIVEN `obtenerAnalisisMensual` returns `Resource.Error`
- WHEN `invoke("o1", mes)` is called
- THEN `Resource.Error` is returned with the original error message
- AND no rules are evaluated

#### Requirement: R3 — Rule 1: Cobrar (ALTA)

The rule SHALL generate a `COBRAR` recommendation when:
- `deudaTotal > config.deudaTotalAlertaMonto` (default S/ 3,000) OR any debtor has `diasDeuda > config.deudaViejaAlertaDias` (default 30 days)

Priority: **ALTA**

The `detalle` SHALL list up to 3 debtors with name, amount, and days. The `accion` SHALL include phone numbers. The `datosAccion.pacienteIds` SHALL list affected patient IDs.

##### Scenario: Debt total exceeds threshold generates COBRAR

- GIVEN deudores list totals S/ 4,200 and `deudaTotalAlertaMonto` = 3,000
- WHEN the COBRAR rule is evaluated
- THEN a `Recomendacion(tipo=COBRAR, prioridad=ALTA)` is returned
- AND `detalle` contains the total "S/ 4,200"

##### Scenario: Old debt but under total threshold also generates COBRAR

- GIVEN deuda total = S/ 1,500 (under 3,000) but one debtor has 45 days deuda (> 30)
- WHEN the COBRAR rule is evaluated
- THEN a `Recomendacion(tipo=COBRAR, prioridad=ALTA)` is returned

##### Scenario: No debtors returns no COBRAR recommendation

- GIVEN deudores list is empty
- WHEN the COBRAR rule is evaluated
- THEN no COBRAR recommendation is generated

#### Requirement: R4 — Rule 2: Mejorar Precio (ALTA)

The rule SHALL generate a `MEJORAR_PRECIO` recommendation for each category in `margenPorCategoria` where:
- `margenPct < 10.0` (low margin) AND `ventas >= config.minVentasParaRecomendar` (monetary threshold — per user decision, S/. threshold as proxy for unit count)

**Note on `minVentasParaRecomendar`**: Per user confirmation, the integer `minVentasParaRecomendar` (default 5) SHALL represent a monetary amount in S/. as a proxy. No RPC changes are needed. The field name stays as-is; its effective unit is S/. for this rule.

Priority: **ALTA**

The `detalle` SHALL name the category, current margin %, current price, and suggest a target price that would achieve at least 25% margin. The `impactoEstimado` SHALL calculate the monthly upside.

##### Scenario: Low-margin category above threshold generates MEJORAR_PRECIO

- GIVEN a category "Monturas Económicas" with `ventas = 960.0` (>= 5), `margenPct = 8.3`
- WHEN the MEJORAR_PRECIO rule is evaluated
- THEN a `Recomendacion(tipo=MEJORAR_PRECIO, prioridad=ALTA)` is returned
- AND `detalle` contains the category name and current margin

##### Scenario: Low-margin category below monetary threshold is skipped

- GIVEN a category "Accesorios" with `ventas = 2.0` (< 5) and `margenPct = 5.0`
- WHEN the MEJORAR_PRECIO rule is evaluated
- THEN no MEJORAR_PRECIO recommendation is generated for this category

##### Scenario: Healthy-margin category is skipped

- GIVEN a category with `margenPct = 30.0`
- WHEN the MEJORAR_PRECIO rule is evaluated
- THEN no MEJORAR_PRECIO recommendation is generated

#### Requirement: R5 — Rule 3: Liquidar Stock (MEDIA)

The rule SHALL generate a `LIQUIDAR_STOCK` recommendation when any item in `stockEstancado` has `diasSinVenta > config.stockEstancadoAlertaDias` (default 180 days).

Priority: **MEDIA**

The `detalle` SHALL list affected models with costo and days. The `impactoEstimado` SHALL calculate recoverable amount at 80% of cost (20% discount). The `datosAccion.productoIds` SHALL list affected montura IDs.

##### Scenario: Stagnant stock above threshold generates LIQUIDAR_STOCK

- GIVEN stockEstancado has 2 items with `diasSinVenta = 210` and 1 item with `diasSinVenta = 45`
- WHEN the LIQUIDAR_STOCK rule is evaluated
- THEN a `Recomendacion(tipo=LIQUIDAR_STOCK, prioridad=MEDIA)` is returned
- AND `detalle` mentions exactly 2 stagnant items (the 45-day item is excluded)

##### Scenario: No stagnant stock returns nothing

- GIVEN stockEstancado is empty
- WHEN the LIQUIDAR_STOCK rule is evaluated
- THEN no LIQUIDAR_STOCK recommendation is generated

#### Requirement: R6 — Rule 4: Vender Más de (MEDIA)

The rule SHALL generate a `VENDER_MAS_DE` recommendation for each category in `margenPorCategoria` where:
- `margenPct > 35.0` (high margin) AND the category's contribution to total margin > 25% AND `ventas >= config.minVentasParaRecomendar` (monetary threshold)

Priority: **MEDIA**

The `detalle` SHALL name the category, margin %, and contribution to total profit. The `accion` SHALL suggest a concrete sales action.

##### Scenario: High-margin star category generates VENDER_MAS_DE

- GIVEN a category "Lentes Progresivos" with `margenPct = 45.0`, `ventas = 4800.0`, contributing 35% of total margins
- WHEN the VENDER_MAS_DE rule is evaluated
- THEN a `Recomendacion(tipo=VENDER_MAS_DE, prioridad=MEDIA)` is returned
- AND `detalle` contains "45%" and the category name

##### Scenario: High-margin but low contribution is skipped

- GIVEN a category with `margenPct = 50.0` but contributing only 2% of total margin
- WHEN the VENDER_MAS_DE rule is evaluated
- THEN no VENDER_MAS_DE recommendation is generated for this category

##### Scenario: High-margin below monetary threshold is skipped

- GIVEN a category with `margenPct = 50.0`, `ventas = 3.0` (< 5), high contribution
- WHEN the VENDER_MAS_DE rule is evaluated
- THEN no VENDER_MAS_DE recommendation is generated for this category

#### Requirement: R7 — Rule 5: Alerta Caída (ALTA)

The rule SHALL generate an `ALERTA_CAIDA` recommendation when:
- `analisis.variacionVentasPct < -config.caidaVentasAlertaPct` (default -10%)

Priority: **ALTA**

The `detalle` SHALL show current vs previous month amounts and the percentage drop. When `variacionVentasPct` is null (no previous month data), the rule SHALL NOT fire.

##### Scenario: Significant sales drop generates ALERTA_CAIDA

- GIVEN `variacionVentasPct = -15.0` and `caidaVentasAlertaPct = 10.0`
- WHEN the ALERTA_CAIDA rule is evaluated
- THEN a `Recomendacion(tipo=ALERTA_CAIDA, prioridad=ALTA)` is returned
- AND `detalle` mentions "15% menos"

##### Scenario: Mild drop below threshold is skipped

- GIVEN `variacionVentasPct = -3.0` and `caidaVentasAlertaPct = 10.0`
- WHEN the ALERTA_CAIDA rule is evaluated
- THEN no ALERTA_CAIDA recommendation is generated

##### Scenario: No previous month data is skipped

- GIVEN `variacionVentasPct = null`
- WHEN the ALERTA_CAIDA rule is evaluated
- THEN no ALERTA_CAIDA recommendation is generated

#### Requirement: R8 — Rule 6: Reducir Gasto (MEDIA)

The rule SHALL generate a `REDUCIR_GASTO` recommendation when:
- `analisis.gastosMes / analisis.ventasMes > 0.4` (gastos > 40% of ventas)

**Note on threshold**: The 40% threshold is hardcoded per the plan. There is no `gastosAlertaVentasPct` column in `ConfiguracionFinancieraEntity` — no schema changes are needed.

Priority: **MEDIA**

The `detalle` SHALL show the expense ratio, the actual amounts (gastosMes / ventasMes), and the reduction opportunity. The `impactoEstimado` SHALL calculate the gain from a 10% expense reduction.

##### Scenario: High expense ratio generates REDUCIR_GASTO

- GIVEN `gastosMes = 3900.0` and `ventasMes = 9400.0` (ratio 41.5%)
- WHEN the REDUCIR_GASTO rule is evaluated
- THEN a `Recomendacion(tipo=REDUCIR_GASTO, prioridad=MEDIA)` is returned
- AND `detalle` mentions the expense ratio "41.5%"

##### Scenario: Healthy expense ratio is skipped

- GIVEN `gastosMes = 1000.0` and `ventasMes = 10000.0` (ratio 10%)
- WHEN the REDUCIR_GASTO rule is evaluated
- THEN no REDUCIR_GASTO recommendation is generated

##### Scenario: Zero ventasMes does not crash

- GIVEN `ventasMes = 0.0`
- WHEN the REDUCIR_GASTO rule is evaluated
- THEN the division is avoided (guard clause)
- AND no REDUCIR_GASTO recommendation is generated

#### Requirement: R9 — Priorization and capping

The system SHALL sort all generated recommendations by priority: ALTA first, MEDIA second, BAJA third. Within the same priority level, the order SHALL be: COBRAR, ALERTA_CAIDA, MEJORAR_PRECIO, LIQUIDAR_STOCK, VENDER_MAS_DE, REDUCIR_GASTO.

The result list SHALL be capped at a maximum of 5 items. If 6 or more recommendations are generated, the lowest-priority items SHALL be dropped. If there are more than 5 ALTA+MEDIA recommendations, the lowest-priority MEDIA items SHALL be trimmed.

##### Scenario: More than 5 recommendations are capped at 5

- GIVEN all 6 rules fire (6 recommendations)
- WHEN prioritization runs
- THEN the returned list has exactly 5 items
- AND the dropped item is MEDIA priority (never an ALTA)

##### Scenario: Priority order is correct

- GIVEN 2 ALTA and 1 MEDIA recommendation
- WHEN prioritization runs
- THEN ALTA items appear before MEDIA items

#### Requirement: R10 — FeedbackRecomendacionEntity Room entity + DAO

The system SHALL create a Room entity for local feedback storage (offline-first):

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

The system SHALL also create `FeedbackRecomendacionDao`:

```kotlin
@Dao
interface FeedbackRecomendacionDao {
    @Upsert
    suspend fun upsert(feedback: FeedbackRecomendacionEntity)

    @Query("SELECT * FROM feedback_recomendaciones WHERE opticaId = :opticaId ORDER BY fecha DESC")
    suspend fun getByOpticaId(opticaId: String): List<FeedbackRecomendacionEntity>
}
```

The database version SHALL be bumped from 33 to 34 with migration `MIGRATION_33_34`:
```sql
CREATE TABLE IF NOT EXISTS feedback_recomendaciones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    recomendacionId TEXT NOT NULL,
    opticaId TEXT NOT NULL,
    fueUtil INTEGER NOT NULL,
    fecha INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS index_feedback_recomendacionId_opticaId
    ON feedback_recomendaciones(recomendacionId, opticaId);
```

##### Scenario: Feedback is persisted locally

- GIVEN a `FeedbackRecomendacionEntity` with `recomendacionId = "abc123"`, `fueUtil = true`
- WHEN `feedbackDao.upsert(feedback)` is called
- THEN the entity is stored in Room
- AND `getByOpticaId("o1")` returns it

##### Scenario: Migration preserves all existing data

- GIVEN a device at database version 33 with 50 existing rows across all tables
- WHEN `MIGRATION_33_34` runs
- THEN all 50 rows are preserved (no data loss)
- AND the `feedback_recomendaciones` table exists with the correct schema

#### Requirement: R11 — FeedbackRecomendacionUseCase

The system SHALL create `domain/FeedbackRecomendacionUseCase.kt`:

```kotlin
class FeedbackRecomendacionUseCase @Inject constructor(
    private val feedbackRecomendacionDao: FeedbackRecomendacionDao
) {
    suspend fun marcarUtil(recomendacionId: String, opticaId: String)
    suspend fun marcarNoUtil(recomendacionId: String, opticaId: String)
}
```

- `marcarUtil` SHALL upsert a `FeedbackRecomendacionEntity` with `fueUtil = true`
- `marcarNoUtil` SHALL upsert a `FeedbackRecomendacionEntity` with `fueUtil = false`
- The method SHALL be idempotent: calling it twice with the same `recomendacionId` updates the existing row (no duplicates)

##### Scenario: marcarUtil stores positive feedback

- GIVEN a recommendation with id "abc123"
- WHEN `feedbackUseCase.marcarUtil("abc123", "o1")` is called
- THEN a `FeedbackRecomendacionEntity` with `fueUtil = true` is stored

##### Scenario: Same recomendacionId updates existing feedback

- GIVEN feedback for "abc123" exists with `fueUtil = false`
- WHEN `feedbackUseCase.marcarUtil("abc123", "o1")` is called
- THEN the existing row is updated to `fueUtil = true` (no duplicate rows)
