# Design: Fase 8 — 6 Reglas de Recomendación

## Technical Approach

Pure local evaluation engine on Android. No new Supabase RPCs. The `GenerarRecomendacionesUseCase` consumes three existing inputs (Fase 7 outputs + config) and runs 6 rule functions in-memory. All rules are pure Kotlin with zero Android framework dependencies — the UseCase itself has `@Inject` for Hilt but the rule evaluation logic is stateless and testable without mocking.

### Why not server-side?

| Aspect | Local evaluation | Server-side RPC |
|--------|-----------------|-----------------|
| Latency | Zero (in-process) | 1 N RT to Supabase |
| Offline | Works fully offline | Requires connectivity |
| Complexity | 6 private functions | New RPC + GRANT + versioning |
| Testability | Pure Kotlin, no mocking needed for rule logic | Integration test against Supabase |

**Choice**: 100% local. All data is already available from Fase 7's UseCases plus `ConfiguracionFinancieraDao`.

## Architecture Decision: Rule evaluation pipeline

```
invoke(opticaId, mes)
    │
    ├─ 1. Fetch inputs (parallel-friendly, sequential for simplicity)
    │   ├─ analisis = obtenerAnalisisMensual(opticaId, mes)
    │   └─ deudores = obtenerDeudores(opticaId)
    │   └─ config = configDao.getByOpticaId(opticaId)
    │
    ├─ 2. Guard: if any input is Error, propagate
    │
    ├─ 3. Evaluate each rule → List<Recomendacion>
    │   ├─ evaluarCobrar(deudores, config)           // R3
    │   ├─ evaluarMejorarPrecio(margenPorCategoria, config) // R4
    │   ├─ evaluarLiquidarStock(stockEstancado, config)     // R5
    │   ├─ evaluarVenderMasDe(margenPorCategoria, config)   // R6
    │   ├─ evaluarAlertaCaida(analisis, config)             // R7
    │   └─ evaluarReducirGasto(analisis)                    // R8
    │
    ├─ 4. Sort by priority (ALTA > MEDIA > BAJA)
    │
    └─ 5. Cap at 5 items → Resource.Success(result)
```

Each rule function:
- Receives only the data it needs (no whole `AnalisisMensual` unless needed)
- Returns `Recomendacion?` (null = rule didn't fire)
- Is a `private fun` (not `suspend`) — pure computation, no IO

## Architecture Decision: Rule thresholds and config

| Rule | Threshold | Source |
|------|-----------|--------|
| R1 — COBRAR | `deudaTotal > deudaTotalAlertaMonto` OR any `diasDeuda > deudaViejaAlertaDias` | `ConfiguracionFinancieraEntity` |
| R2 — MEJORAR_PRECIO | `margenPct < 10` AND `ventas >= minVentasParaRecomendar` | Hardcoded 10% + config |
| R3 — LIQUIDAR_STOCK | `diasSinVenta > stockEstancadoAlertaDias` | `ConfiguracionFinancieraEntity` |
| R4 — VENDER_MAS_DE | `margenPct > 35` AND contribution > 25% AND `ventas >= minVentasParaRecomendar` | Hardcoded 35%/25% + config |
| R5 — ALERTA_CAIDA | `variacionVentasPct < -caidaVentasAlertaPct` | `ConfiguracionFinancieraEntity` |
| R6 — REDUCIR_GASTO | `gastosMes / ventasMes > 0.4` | Hardcoded 40% |

Hardcoded thresholds (10%, 35%, 25%, 40%) match the original plan and are not user-configurable. If configurability is needed, adding new columns to `configuracion_financiera` is straightforward but out of scope for this phase.

## Architecture Decision: minVentasParaRecomendar as monetary proxy

Per user confirmation: R2 and R4 use the integer `minVentasParaRecomendar` (default 5) as a **monetary amount in S/.**. The comparison is `categoria.ventas >= minVentasParaRecomendar`, where `ventas` is already a `Double` in S/.

This means:
- Default threshold: a category needs at least S/ 5 in sales to trigger a recommendation about it
- The field name in config remains `minVentasParaRecomendar` (no schema change)
- The effective semantic is "minimum sales amount to consider a category significant enough to recommend about"

## Architecture Decision: Feedback — Room local only (offline-first)

Per user confirmation: `FeedbackRecomendacion` is stored in a local Room table `feedback_recomendaciones` with sync planned for a future phase. The entity uses `@PrimaryKey(autoGenerate = true)` and indices on `(recomendacionId, opticaId)` for efficient lookups.

Sync approach (future): the `feedback_recomendaciones` table will follow the same pattern as other Room entities — an `UploadSyncCoordinator` batch includes new feedback rows, upserts them to the Supabase `feedback_recomendaciones` table, and marks them as synced.

## Architecture Decision: No OptoRepository involvement

Following the pattern established by `ObtenerAnalisisMensualUseCase` and `ObtenerDeudoresUseCase` in Fase 7, analytical UseCases inject DAOs directly instead of going through the god `OptoRepository`. `GenerarRecomendacionesUseCase` does the same: it injects `ConfiguracionFinancieraDao` directly, not through `OptoRepository`.

## File Inventory

### New files (6)

| File | Type | Description |
|------|------|-------------|
| `domain/Recomendacion.kt` | Data classes | `Recomendacion`, `RecomendacionTipo`, `Prioridad`, `DatosAccion` — pure Kotlin, ~50 lines |
| `domain/GenerarRecomendacionesUseCase.kt` | UseCase | Engine with 6 private rule functions + orchestration, @Inject, ~250 lines |
| `domain/FeedbackRecomendacionUseCase.kt` | UseCase | Save 👍/👎 feedback, @Inject, ~40 lines |
| `data/feedbackrecomendacion/FeedbackRecomendacionEntity.kt` | Room entity | Local feedback table, ~25 lines |
| `data/feedbackrecomendacion/FeedbackRecomendacionDao.kt` | Room DAO | Upsert + query by opticaId, ~20 lines |
| `data/feedbackrecomendacion/package-info` (optional) | — | Directory marker if needed |

### Modified files (3)

| File | Change | Lines |
|------|--------|-------|
| `domain/AnalisisMensual.kt` | Add `gastosMes: Double = 0.0` field + `gastos_mes` parsing in `fromJson` | +3 |
| `data/OptoDatabase.kt` | Bump version 33→34, add `FeedbackRecomendacionEntity` to entity list, export `MIGRATION_33_34` | +5 |
| `data/OptoDatabaseMigrations.kt` | Add `MIGRATION_33_34`: CREATE TABLE + CREATE INDEX | +15 |

### Test files (new, 2)

| File | Description |
|------|-------------|
| `test/domain/GenerarRecomendacionesUseCaseTest.kt` | One test per rule + prioritization + edge cases, ~300 lines |
| `test/domain/FeedbackRecomendacionUseCaseTest.kt` | Feedback save + idempotence, ~60 lines |

### Not modified (by design decision)

- `ConfiguracionFinancieraEntity` — all needed columns already exist
- `ConfiguracionFinancieraDao` — but note: current `getByOpticaId` returns `Flow<ConfiguracionFinancieraEntity?>`. The UseCase needs a one-shot value. Either add a `suspend fun getByOpticaIdOnce(opticaId: String): ConfiguracionFinancieraEntity?` to the DAO, or call `.first()` in the UseCase. **Decision**: add `getByOpticaIdOnce` suspend function to DAO (cleaner separation — domain layer shouldn't depend on Flow).
- `OptoRepository` — not touched
- No Supabase migrations — all evaluation is local
- No new RPCs

## Data Flow Diagram

```
┌─ Usuario ──────────────────────────────────────────────────────────┐
│  BIViewModel / BIScreen (Fase 9)                                   │
│       │                                                             │
│       ▼ invoke(opticaId, mes)                                       │
│  ┌────────────────────────────────────────────────────────────┐     │
│  │  GenerarRecomendacionesUseCase                             │     │
│  │                                                            │     │
│  │  ┌──────────────────┐  ┌──────────────┐  ┌────────────┐   │     │
│  │  │ ObtenerAnalisis  │  │ Obtener     │  │ Config    │   │     │
│  │  │ MensualUseCase   │  │ DeudoresUC  │  │ Dao       │   │     │
│  │  └───────┬──────────┘  └──────┬───────┘  └─────┬──────┘   │     │
│  │          │                    │                 │          │     │
│  │  ┌───────▼────────────────────▼─────────────────▼────────┐ │     │
│  │  │             6 rule functions (pure Kotlin)            │ │     │
│  │  │  cobrar │ mejorarPrecio │ liquidarStock │ venderMas  │ │     │
│  │  │  alertaCaida │ reducirGasto                          │ │     │
│  │  └───────────────────────────┬──────────────────────────┘ │     │
│  │                              │                             │     │
│  │  ┌───────────────────────────▼──────────────────────────┐  │     │
│  │  │  Sort(ALTA > MEDIA > BAJA) → take(5)                │  │     │
│  │  └───────────────────────────┬──────────────────────────┘  │     │
│  │                              │                             │     │
│  │  ┌───────────────────────────▼──────────────────────────┐  │     │
│  │  │  Resource<List<Recomendacion>>                       │  │     │
│  │  └──────────────────────────────────────────────────────┘  │     │
│  └────────────────────────────────────────────────────────────┘     │
│                                                                      │
│  Feedback (separate flow):                                           │
│  ┌────────────────────────────────────────────┐                     │
│  │  FeedbackRecomendacionUseCase              │                     │
│  │  marcarUtil / marcarNoUtil                  │                     │
│  │       │                                     │                     │
│  │  ┌────▼─────────────────────────┐           │                     │
│  │  │ FeedbackRecomendacionDao    │           │                     │
│  │  │ (Room local, offline-first) │           │                     │
│  │  └─────────────────────────────┘           │                     │
│  └────────────────────────────────────────────┘                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Interfaces / Contracts

### Recomendacion domain model

```kotlin
data class Recomendacion(
    val id: String,                   // hash(tipo + titulo)
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

### GenerarRecomendacionesUseCase

```kotlin
class GenerarRecomendacionesUseCase @Inject constructor(
    private val obtenerAnalisisMensual: ObtenerAnalisisMensualUseCase,
    private val obtenerDeudores: ObtenerDeudoresUseCase,
    private val configuracionFinancieraDao: ConfiguracionFinancieraDao
) {
    suspend operator fun invoke(opticaId: String, mes: LocalDate): Resource<List<Recomendacion>>
}
```

### Private rule function signatures

```kotlin
private fun evaluarCobrar(
    deudores: List<Deudor>,
    config: ConfiguracionFinancieraEntity
): Recomendacion?

private fun evaluarMejorarPrecio(
    categorias: List<MargenCategoria>,
    config: ConfiguracionFinancieraEntity
): Recomendacion?

private fun evaluarLiquidarStock(
    stockEstancado: List<StockEstancadoItem>,
    config: ConfiguracionFinancieraEntity
): Recomendacion?

private fun evaluarVenderMasDe(
    categorias: List<MargenCategoria>,
    config: ConfiguracionFinancieraEntity
): Recomendacion?

private fun evaluarAlertaCaida(
    analisis: AnalisisMensual,
    config: ConfiguracionFinancieraEntity
): Recomendacion?

private fun evaluarReducirGasto(
    analisis: AnalisisMensual
): Recomendacion?
```

### FeedbackRecomendacionUseCase

```kotlin
class FeedbackRecomendacionUseCase @Inject constructor(
    private val feedbackRecomendacionDao: FeedbackRecomendacionDao
) {
    suspend fun marcarUtil(recomendacionId: String, opticaId: String)
    suspend fun marcarNoUtil(recomendacionId: String, opticaId: String)
}
```

## Recomendacion ID generation

The `id` field SHALL be a deterministic hash of `tipo.name + titulo` to:
1. Enable idempotent feedback (same recommendation always has the same ID)
2. Allow dedup if the same recommendation is generated in different sessions
3. Keep feedback meaningful across re-evaluations

Implementation: `id = tipo.name + "::" + titulo` (no hashing needed for dedup — the raw string is unique and human-readable). The hash approach was considered but adds complexity with zero benefit at this stage since the string is already stable and deterministic.

## Database Migration: v33→v34

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

The migration uses `CREATE TABLE IF NOT EXISTS` for idempotency. No data migration is needed — this is a new table.

Room entity list in `OptoDatabase` SHALL add `FeedbackRecomendacionEntity::class`.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (rule) | R1 — COBRAR: fires when debt > threshold OR old debt exists | Mock deudores + config, assert Recomendacion? returned |
| Unit (rule) | R2 — MEJORAR_PRECIO: fires when margin < 10% and ventas >= min | Mock categorias + config |
| Unit (rule) | R3 — LIQUIDAR_STOCK: fires when diasSinVenta > threshold | Mock stockEstancado + config |
| Unit (rule) | R4 — VENDER_MAS_DE: fires when margin > 35% and contribution > 25% | Mock categorias + config |
| Unit (rule) | R5 — ALERTA_CAIDA: fires when variacion < -threshold | Mock analisis + config |
| Unit (rule) | R6 — REDUCIR_GASTO: fires when gastos/ventas > 0.4 | Mock analisis |
| Unit (rule) | Edge cases: null/empty inputs, zero divisions, boundary values | Per rule |
| Unit (orchestrator) | All 6 rules fire → prioritization → capping to 5 | Mock all three dependencies |
| Unit (orchestrator) | Error path: one dependency returns Error → immediate Error return | Mock ObtenerAnalisis to return Error |
| Unit (feedback) | marcarUtil / marcarNoUtil store correct values | Mock FeedbackRecomendacionDao |
| Room | MIGRATION_33_34 creates table, preserves v33 data | In-memory Room test |
| Compilation | Full assembleDebug passes | Gradle |

Key testing principle: each rule function is a **pure function** — given inputs, return `Recomendacion?`. This means tests don't need Hilt or Robolectric for the rule logic itself. Only the orchestrator path (dependency injection, error propagation) needs MockK.

## Open Questions

- **ConfiguracionFinancieraDao.getByOpticaIdOnce**: Add a suspend one-shot getter to the DAO, or use `.first()` on the Flow? Decision: add a suspend function to keep the domain layer clean of Flow dependency.
- **Sync for feedback**: Room-only in this phase. Future sync via existing `UploadSyncCoordinator` pattern.
