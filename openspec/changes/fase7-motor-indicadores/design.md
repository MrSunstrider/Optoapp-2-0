# Design: Fase 7 — Motor de 8 indicadores en lenguaje de negocio

## Technical Approach

RPC-first aggregation with Room fallback: 2 new Supabase SQL functions compute the 8 indicators server-side; Android UseCases call them when online and fall back to local Room aggregation when offline. Room `resumen_diario` serves as the cache layer for monthly indicators (1–4). Indicators 7–8 (stock estancado, valor inventario) are always calculated locally from `monturas` + `montura_movimientos` since that data is synced.

## Architecture Decisions

### Decision: Single vs. split RPC for 8 indicators

| Option | Tradeoff |
|--------|----------|
| Single RPC returning JSONB | One call, atomic result. All indicators or nothing. Simpler client cache. |
| Split RPCs per indicator group | More granular caching, but N round-trips + complex client orchestration. |

**Choice**: Single `rpc_analisis_mensual` returning JSONB with all 8 indicator keys. Complements `rpc_deudores` (separate — it's a table, not a summary).

### Decision: Offline fallback scope

| Option | Tradeoff |
|--------|----------|
| Full local calculation for all 8 indicators | Requires Room entities for `costos_productos` + `margen_por_categoria` (server-only) |
| Fallback only for indicators 1–4 from `resumen_diario` | Simpler: `resumen_diario` already has ventas/cobros/costo columns. Indicators 5–8 return limited/zero data offline. |

**Choice**: Offline fallback for 1–4 only (from `resumen_diario`). Indicators 5–8 return empty/0 with an `esOffline` flag. This avoids creating Room entities for `costos_productos` and `margen_por_categoria` which are server-only per spec.

### Decision: Deprecation strategy for old RPCs

**Choice**: Comment-only deprecation. No DROP. No ALTER. All old RPCs remain fully functional. This avoids breaking existing call sites that haven't been migrated yet.

## Data Flow

```
┌─ Usuario ─────────────────────────────────┐
│  BIViewModel / BIScreen (Fase 9)          │
│       │                                    │
│       ▼ invoke(opticaId, mes)              │
│  ┌──────────────────────────────┐          │
│  │  ObtenerAnalisisMensualUseCase         │
│  │  try { rpc() } catch { fallback() }    │
│  └──────┬───────────────────────┘          │
│         │                                  │
│  ┌──────▼────────┐    ┌─────────────────┐  │
│  │ supabase.rpc  │    │ ResumenDiarioDao│  │
│  │ (online)      │    │ (offline, 1-4)  │  │
│  └──────┬────────┘    └────────┬────────┘  │
│         │                      │           │
│  ┌──────▼──────────────────────▼────────┐  │
│  │      AnalisisMensual (domain)        │  │
│  └─────────────────────────────────────┘  │
└───────────────────────────────────────────┘

Supabase:
  rpc_analisis_mensual ──→ resumen_diario + gastos_operativos + monturas
  rpc_deudores ──→ ventas + pagos + pacientes
```

## File Changes

### Supabase — New migrations (5 files)

| File | Action | Description |
|------|--------|-------------|
| `supabase/migrations/20260706000000_fase7_rpc_analisis_mensual.sql` | Create | `rpc_analisis_mensual(p_optica_id,p_mes) RETURNS jsonb` — 8 indicators + GRANT |
| `supabase/migrations/20260706000001_fase7_rpc_deudores.sql` | Create | `rpc_deudores(p_optica_id) RETURNS TABLE(...)` + GRANT |
| `supabase/migrations/20260706000002_fase7_fix_grant_recalcular.sql` | Create | `GRANT EXECUTE ON FUNCTION recalcular_resumen_diario TO authenticated` |
| `supabase/migrations/20260706000003_fase7_update_rpc_count_pendientes.sql` | Create | Rewrite `rpc_count_pendientes` to read from `ventas` instead of old tables |
| `supabase/migrations/20260706000004_fase7_deprecations.sql` | Create | `COMMENT ON FUNCTION` for `rpc_resumen_financiero` and `rpc_saldo_pendiente` |

### Android — New files (4 files)

| File | Action | Description |
|------|--------|-------------|
| `optoapp/.../domain/AnalisisMensual.kt` | Create | Domain model with all 8 indicator fields + `esOffline` flag + mapper from RPC JsonElement |
| `optoapp/.../domain/Deudor.kt` | Create | Domain model: nombre, telefono, ventaId, fecha, montoTotal, totalPagado, saldo, diasDeuda |
| `optoapp/.../domain/ObtenerAnalisisMensualUseCase.kt` | Create | Calls RPC online, falls back to Room offline |
| `optoapp/.../domain/ObtenerDeudoresUseCase.kt` | Create | Calls RPC online, falls back to local Room JOIN offline |

### Android — Modified files (4 files)

| File | Action | Description |
|------|--------|-------------|
| `optoapp/.../data/dispensacion/DispensacionEntity.kt` | Modify | Add `ventaId: String? = null` + `@SerialName("ventaId")` to `Pago` data class |
| `optoapp/.../data/resumendiario/ResumenDiarioDao.kt` | Modify | Add `getByOpticaAndMonth(opticaId, yearMonth): List<ResumenDiarioEntity>` |
| `optoapp/.../data/OptoDatabase.kt` | Modify | Version 33, add `MIGRATION_32_33` re-export, register in `.addMigrations()` |
| `optoapp/.../data/OptoDatabaseMigrations.kt` | Modify | Add `MIGRATION_32_33`: ALTER TABLE + CREATE INDEX |

### Not modified (by design decision)
- `OptoRepository` — not touched. Analytical UseCases call DAOs directly, not through the god repository.
- `BIViewModel` / `BIScreen` — not touched. UI integration is Fase 9.
- No Room entities created for `costos_productos` or `margen_por_categoria` (server-only).

## Interfaces / Contracts

### Domain models

```kotlin
data class AnalisisMensual(
    val ventasMes: Double,
    val cobrosMes: Double,
    val margenNetoPct: Double,
    val margenPorCategoria: List<MargenCategoria>,
    val deudores: DeudoresResumen,
    val proyeccionCaja: ProyeccionCaja?,
    val stockEstancado: StockEstancado,
    val valorInventario: Double,
    val ventasMesAnterior: Double,
    val variacionVentasPct: Double?,
    val esOffline: Boolean = false  // true when data is from Room fallback (limited set)
)

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

### UseCase contracts

```kotlin
class ObtenerAnalisisMensualUseCase @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val resumenDiarioDao: ResumenDiarioDao
) {
    suspend operator fun invoke(opticaId: String, mes: LocalDate): Resource<AnalisisMensual>
}

class ObtenerDeudoresUseCase @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val ventaDao: VentaDao,
    private val pagoDao: PagoDao
) {
    suspend operator fun invoke(opticaId: String): Resource<List<Deudor>>
}
```

### ResumenDiarioDao addition

```kotlin
@Query("""
    SELECT * FROM resumen_diario 
    WHERE opticaId = :opticaId 
      AND strftime('%Y-%m', fecha) = :yearMonth 
    ORDER BY fecha ASC
""")
suspend fun getByOpticaAndMonth(opticaId: String, yearMonth: String): List<ResumenDiarioEntity>
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Supabase RPC | `rpc_analisis_mensual` returns correct JSONB for populated/empty month | Execute in Supabase migration test or manual SQL validation |
| Supabase RPC | `rpc_deudores` returns ordered debtors | Execute with known fixture data |
| Unit (UseCase) | `ObtenerAnalisisMensualUseCase` — online RPC path | Mock `SupabaseClient.postgrest.rpc()`, verify JsonElement mapping |
| Unit (UseCase) | `ObtenerAnalisisMensualUseCase` — offline fallback | Mock RPC to throw IOException, verify `ResumenDiarioDao` fallback |
| Unit (UseCase) | `ObtenerDeudoresUseCase` — same dual path | Same pattern |
| Unit (Domain) | `AnalisisMensual` JSON mapper | Test edge cases: null fields, 0 values, missing keys |
| Room | MIGRATION_32_33 preserves existing Pago rows | In-memory Room test: create v32 schema, insert rows, run migration, verify |
| Room | `getByOpticaAndMonth` returns correct rows | Direct DAO test with known fixtures |

## Migration / Rollout

**Supabase**: All 5 migration files are additive or idempotent (`CREATE OR REPLACE`, `IF NOT EXISTS`, `GRANT` is idempotent). Apply in timestamp order. No data migration.

**Room**: `ALTER TABLE ADD COLUMN` is an instant operation (no data rewrite). Index creation is also instant. Rollback: downgrade to version 32, remove `MIGRATION_32_33`, drop new code — no data loss.

## Open Questions

- None. All decisions are resolved by the spec and exploration analysis.
