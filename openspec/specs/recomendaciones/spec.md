# Recommendation Engine Specification

## Purpose

Android-side engine that generates business recommendations from financial indicators. Evaluates 6 rules in-memory against existing data sources (AnalisisMensual, Deudores, ConfiguracionFinanciera), prioritizes results, and caps at 5 items. Local-only evaluation with zero network calls at rule time.

## Requirements

### R1: Recomendacion Domain Model

The system SHALL provide `domain/Recomendacion.kt` with:

- `Recomendacion` data class: id, tipo, titulo, detalle, impactoEstimado, prioridad, accion, datosAccion
- `RecomendacionTipo` enum: COBRAR, MEJORAR_PRECIO, LIQUIDAR_STOCK, VENDER_MAS_DE, REDUCIR_GASTO, ALERTA_CAIDA
- `Prioridad` enum: ALTA (ordinal 0), MEDIA (1), BAJA (2)
- `DatosAccion` data class: pacienteIds, productoIds, montoTotal — all nullable
- `id` format: `"${tipo.name}::${titulo}"` (deterministic, human-readable)

#### Scenario: Recomendacion can be constructed with all fields
- GIVEN all field values for a recommendation
- WHEN `Recomendacion(...)` is constructed
- THEN the instance has the correct tipo, prioridad, titulo, detalle, impactoEstimado, accion, and datosAccion

#### Scenario: DatosAccion defaults to null collections
- GIVEN `DatosAccion()` is constructed with no arguments
- THEN pacienteIds, productoIds, and montoTotal are all null

### R2: GenerarRecomendacionesUseCase

A Hilt UseCase `open class` with `@Inject` constructor accepting `ObtenerAnalisisMensualUseCase`, `ObtenerDeudoresUseCase`, and `ConfiguracionFinancieraDao`.

`suspend operator fun invoke(opticaId: String, mes: LocalDate): Resource<List<Recomendacion>>`

PIPELINE: fetch inputs → guard errors → evaluate 6 rules → collect non-null → sort by priority ordinal → take(5).

#### Scenario: All 6 rules fire
- GIVEN data that triggers all 6 rules
- WHEN invoke is called
- THEN up to 5 Recomendacion items returned, sorted ALTA before MEDIA

#### Scenario: No rules fire
- GIVEN no rule conditions are met
- WHEN invoke is called
- THEN `Resource.Success(emptyList())`

#### Scenario: Error propagation
- GIVEN any dependency returns Resource.Error
- WHEN invoke is called
- THEN Resource.Error returned with original message, no rules evaluated

### R3: Rule 1 — Cobrar (ALTA)

Fires when `deudaTotal > deudaTotalAlertaMonto` OR any debtor `diasDeuda > deudaViejaAlertaDias`.

Thresholds from `ConfiguracionFinancieraEntity`. Priority: ALTA.

#### Scenario: Total exceeds threshold
- GIVEN deudores total S/ 4,200 and threshold 3,000
- THEN `Recomendacion(tipo=COBRAR, prioridad=ALTA)` with detalle containing "S/ 4,200"

#### Scenario: Old debt triggers even under total threshold
- GIVEN total S/ 1,500 but one debtor at 45 days (> 30)
- THEN COBRAR recommendation returned

#### Scenario: No debtors
- GIVEN empty deudores list
- THEN no COBRAR recommendation (null)

### R4: Rule 2 — Mejorar Precio (ALTA)

Fires per category where `margenPct < 10` AND `ventas >= minVentasParaRecomendar` (monetary threshold in S/.). Priority: ALTA.

#### Scenario: Low margin above threshold
- GIVEN category with ventas 960.0, margenPct 8.3
- THEN `Recomendacion(tipo=MEJORAR_PRECIO, prioridad=ALTA)` with category name in detalle

#### Scenario: Below monetary threshold
- GIVEN category with ventas 2.0 (< 5), margenPct 5.0
- THEN null

#### Scenario: Healthy margin
- GIVEN category with margenPct 30.0
- THEN null

### R5: Rule 3 — Liquidar Stock (MEDIA)

Fires when any item `diasSinVenta > stockEstancadoAlertaDias` (default 180). Priority: MEDIA.

#### Scenario: Items exceed threshold
- GIVEN 2 items at 210 days, threshold 180
- THEN `Recomendacion(tipo=LIQUIDAR_STOCK, prioridad=MEDIA)` listing only those 2 items

#### Scenario: No stagnant items
- GIVEN empty stockEstancado
- THEN null

### R6: Rule 4 — Vender Más de (MEDIA)

Fires per category where `margenPct > 35`, contribution > 25%, `ventas >= minVentasParaRecomendar`. Hardcoded thresholds: 35%, 25%. Priority: MEDIA.

#### Scenario: Star category
- GIVEN category with margenPct 45%, contribution 35%, ventas 4800.0
- THEN `Recomendacion(tipo=VENDER_MAS_DE, prioridad=MEDIA)` with "45%" in detalle

#### Scenario: High margin low contribution
- GIVEN margenPct 50% but contributes 2%
- THEN null

#### Scenario: Below monetary threshold
- GIVEN margenPct 50%, ventas 3.0 (< 5)
- THEN null

### R7: Rule 5 — Alerta Caída (ALTA)

Fires when `variacionVentasPct < -caidaVentasAlertaPct` (default -10%). Priority: ALTA.

#### Scenario: Significant drop
- GIVEN variacionVentasPct = -15%, threshold 10%
- THEN `Recomendacion(tipo=ALERTA_CAIDA, prioridad=ALTA)` with percentage in detalle

#### Scenario: Mild drop
- GIVEN variacionVentasPct = -3%
- THEN null

#### Scenario: Null variation (no previous month)
- GIVEN variacionVentasPct = null
- THEN null

### R8: Rule 6 — Reducir Gasto (MEDIA)

Fires when `gastosMes / ventasMes > 0.4` (hardcoded 40% threshold). Priority: MEDIA.

#### Scenario: High expense ratio
- GIVEN gastosMes 3900.0, ventasMes 9400.0 (ratio 41.5%)
- THEN `Recomendacion(tipo=REDUCIR_GASTO, prioridad=MEDIA)` with ratio in detalle

#### Scenario: Healthy ratio
- GIVEN gastosMes 1000.0, ventasMes 10000.0 (ratio 10%)
- THEN null

#### Scenario: Zero ventasMes
- GIVEN ventasMes = 0.0
- THEN null (no division crash)

### R9: Prioritization and Capping

Sort by priority ordinal (ALTA=0, MEDIA=1, BAJA=2). Within same priority: COBRAR, ALERTA_CAIDA, MEJORAR_PRECIO, LIQUIDAR_STOCK, VENDER_MAS_DE, REDUCIR_GASTO. Cap at 5 items.

#### Scenario: More than 5 capped
- GIVEN 6 rules fire
- THEN exactly 5 items returned, lowest priority dropped

#### Scenario: Priority order
- GIVEN 2 ALTA + 1 MEDIA
- THEN ALTA items before MEDIA items

### R10: FeedbackRecomendacionEntity + DAO + Migration

Room entity `feedback_recomendaciones` with: id (autoGenerate), recomendacionId, opticaId, fueUtil (Boolean), fecha (Long). Index on (recomendacionId, opticaId). DAO with `@Upsert` and `getByOpticaId` (ordered fecha DESC).

Database version 34 with `MIGRATION_33_34`: CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS.

#### Scenario: Feedback persisted
- GIVEN feedback entity with fueUtil = true
- WHEN upsert called
- THEN getByOpticaId returns it

#### Scenario: Migration preserves data
- GIVEN v33 database with existing data
- WHEN MIGRATION_33_34 runs
- THEN all existing rows preserved, new table and index exist

### R11: FeedbackRecomendacionUseCase

`marcarUtil(id, opticaId)` upserts with fueUtil=true. `marcarNoUtil(id, opticaId)` upserts with fueUtil=false. Idempotent via DAO `@Upsert`.

#### Scenario: marcarUtil stores positive feedback
- GIVEN recommendation id "abc123"
- WHEN marcarUtil called
- THEN entity stored with fueUtil = true

#### Scenario: Same id updates existing feedback
- GIVEN existing feedback with fueUtil = false
- WHEN marcarUtil called with same id
- THEN row updated to fueUtil = true (no duplicate)

## Out of Scope

- Fase 9 UI screens for recommendation display and feedback buttons
- Sync of feedback_recomendaciones to Supabase (local-only for now)
- RPC changes or Supabase migrations (all evaluation is local)
