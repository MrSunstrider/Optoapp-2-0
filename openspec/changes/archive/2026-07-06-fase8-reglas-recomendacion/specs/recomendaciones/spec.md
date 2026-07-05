# Delta Spec: recomendaciones — Fase 8 Reglas de Recomendación

## ADDED Requirements

### Requirement: R1 — Recomendacion domain model + RecomendacionTipo enum

The system SHALL create `domain/Recomendacion.kt` with:

```kotlin
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

`id` format: `"${tipo.name}::${titulo}"` — deterministic and human-readable.

#### Scenario: Recomendacion can be constructed with all fields

- GIVEN all field values for a recommendation
- WHEN `Recomendacion(...)` is constructed
- THEN the instance has the correct `tipo`, `prioridad`, `titulo`, `detalle`, `impactoEstimado`, `accion`, and `datosAccion`

#### Scenario: DatosAccion defaults to null collections

- GIVEN `DatosAccion()` is constructed with no arguments
- THEN `pacienteIds`, `productoIds`, and `montoTotal` are all null

### Requirement: R2 — GenerarRecomendacionesUseCase orchestrates 6 rules

The system SHALL create `domain/GenerarRecomendacionesUseCase.kt` with Hilt `@Inject` constructor:

```kotlin
open class GenerarRecomendacionesUseCase @Inject constructor(
    private val obtenerAnalisisMensual: ObtenerAnalisisMensualUseCase,
    private val obtenerDeudores: ObtenerDeudoresUseCase,
    private val configuracionFinancieraDao: ConfiguracionFinancieraDao
) {
    suspend operator fun invoke(opticaId: String, mes: LocalDate): Resource<List<Recomendacion>>
}
```

The `invoke` method SHALL:
1. Fetch inputs: analisis, deudores, config
2. Guard: propagate errors
3. Evaluate 6 rules as `private fun`
4. Sort by priority ordinal (ALTA=0, MEDIA=1, BAJA=2)
5. Cap at 5 items
6. Return `Resource.Success`

#### Scenario: All 6 rules fire with matching data

- GIVEN data that triggers all 6 rules
- WHEN `invoke("o1", mes)` is called
- THEN list of up to 5 `Recomendacion` items returned, sorted ALTA before MEDIA

#### Scenario: No rules fire with clean data

- GIVEN no rule conditions are met
- WHEN `invoke("o1", mes)` is called
- THEN `Resource.Success(emptyList())`

#### Scenario: Error in dependency propagates

- GIVEN any dependency returns `Resource.Error`
- WHEN `invoke("o1", mes)` is called
- THEN `Resource.Error` returned with original message

### Requirement: R3 — Rule 1: Cobrar (ALTA)

Generates `COBRAR` when deudaTotal > deudaTotalAlertaMonto OR any debtor `diasDeuda > deudaViejaAlertaDias`.

#### Scenario: Debt total exceeds threshold

- GIVEN deudores total S/ 4,200 and threshold 3,000
- THEN `Recomendacion(tipo=COBRAR, prioridad=ALTA)` with detalle containing "S/ 4,200"

#### Scenario: Old debt but under total threshold

- GIVEN total S/ 1,500 (under threshold) but 45-day-old debt (> 30)
- THEN `Recomendacion(tipo=COBRAR, prioridad=ALTA)`

#### Scenario: No debtors

- GIVEN empty deudores list
- THEN no COBRAR recommendation (null)

### Requirement: R4 — Rule 2: Mejorar Precio (ALTA)

Generates `MEJORAR_PRECIO` per category where `margenPct < 10` AND `ventas >= minVentasParaRecomendar` (monetary threshold S/.).

#### Scenario: Low-margin above monetary threshold

- GIVEN category with ventas 960.0 (>= 5), margenPct 8.3
- THEN `Recomendacion(tipo=MEJORAR_PRECIO, prioridad=ALTA)`

#### Scenario: Below monetary threshold

- GIVEN category with ventas 2.0 (< 5), margenPct 5.0
- THEN null

#### Scenario: Healthy margin

- GIVEN category with margenPct 30.0
- THEN null

### Requirement: R5 — Rule 3: Liquidar Stock (MEDIA)

Generates `LIQUIDAR_STOCK` when any item `diasSinVenta > stockEstancadoAlertaDias` (default 180).

#### Scenario: Items exceed days threshold

- GIVEN 2 items at 210 days, 1 at 45 days, threshold 180
- THEN `Recomendacion(tipo=LIQUIDAR_STOCK, prioridad=MEDIA)` with 2 items in detalle

#### Scenario: No stagnant items

- GIVEN empty stockEstancado
- THEN null

### Requirement: R6 — Rule 4: Vender Más de (MEDIA)

Generates `VENDER_MAS_DE` per category where `margenPct > 35`, contribution > 25%, `ventas >= minVentasParaRecomendar`.

#### Scenario: Star category

- GIVEN category with margenPct 45%, contribution 35%, ventas 4800.0
- THEN `Recomendacion(tipo=VENDER_MAS_DE, prioridad=MEDIA)` with "45%" in detalle

#### Scenario: High margin low contribution

- GIVEN margenPct 50% but contributes 2%
- THEN null

#### Scenario: Below monetary threshold

- GIVEN margenPct 50%, ventas 3.0 (< 5)
- THEN null

### Requirement: R7 — Rule 5: Alerta Caída (ALTA)

Generates `ALERTA_CAIDA` when `variacionVentasPct < -caidaVentasAlertaPct` (default -10%).

#### Scenario: Significant drop

- GIVEN variacionVentasPct = -15%, threshold 10%
- THEN `Recomendacion(tipo=ALERTA_CAIDA, prioridad=ALTA)` with "15%" in detalle

#### Scenario: Mild drop

- GIVEN variacionVentasPct = -3%
- THEN null

#### Scenario: Null variation

- GIVEN variacionVentasPct = null
- THEN null

### Requirement: R8 — Rule 6: Reducir Gasto (MEDIA)

Generates `REDUCIR_GASTO` when `gastosMes / ventasMes > 0.4` (hardcoded 40% threshold).

#### Scenario: High expense ratio

- GIVEN gastosMes 3900.0, ventasMes 9400.0 (ratio 41.5%)
- THEN `Recomendacion(tipo=REDUCIR_GASTO, prioridad=MEDIA)` with ratio in detalle

#### Scenario: Healthy ratio

- GIVEN gastosMes 1000.0, ventasMes 10000.0 (ratio 10%)
- THEN null

#### Scenario: Zero ventasMes

- GIVEN ventasMes = 0.0
- THEN no division crash, returns null

### Requirement: R9 — Priorization and capping

Sort by priority (ALTA > MEDIA > BAJA). Cap at 5 items. Within same priority: COBRAR, ALERTA_CAIDA, MEJORAR_PRECIO, LIQUIDAR_STOCK, VENDER_MAS_DE, REDUCIR_GASTO.

#### Scenario: More than 5 capped at 5

- GIVEN 6 rules fire
- THEN exactly 5 items returned, dropped item is lowest priority

#### Scenario: Priority order correct

- GIVEN 2 ALTA + 1 MEDIA
- THEN ALTA items before MEDIA items

### Requirement: R10 — FeedbackRecomendacionEntity Room entity + DAO

Room entity `feedback_recomendaciones` table:
- id (autoGenerate), recomendacionId, opticaId, fueUtil (Boolean), fecha (Long)
- Index on (recomendacionId, opticaId)
- DAO: `@Upsert suspend fun upsert()`, `@Query getByOpticaId` ordered fecha DESC

Database version bumped 33→34 with MIGRATION_33_34 (CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS).

#### Scenario: Feedback persisted locally

- GIVEN FeedbackRecomendacionEntity with fueUtil = true
- WHEN upsert is called
- THEN getByOpticaId returns it

#### Scenario: Migration preserves data

- GIVEN v33 database with existing data
- WHEN MIGRATION_33_34 runs
- THEN all existing rows preserved, new table exists

### Requirement: R11 — FeedbackRecomendacionUseCase

`marcarUtil(recomendacionId, opticaId)` upserts with fueUtil=true.
`marcarNoUtil(recomendacionId, opticaId)` upserts with fueUtil=false.
Idempotent: calling twice updates existing row.

#### Scenario: marcarUtil stores positive feedback

- GIVEN recommendation id "abc123"
- WHEN marcarUtil is called
- THEN entity stored with fueUtil = true

#### Scenario: Same id updates existing feedback

- GIVEN existing feedback with fueUtil = false
- WHEN marcarUtil is called with same id
- THEN row updated to fueUtil = true (no duplicate)
