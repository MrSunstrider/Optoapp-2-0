# Design: Servicios Extra en Ventas

## Technical Approach

Fold `ServicioExtra` revenue into the existing reactive `combine` chains of three ViewModels (`ReportesViewModel`, `CierreCajaViewModel`, `BIViewModel`) by adding `repository.getAllServiciosForOptica(opticaId)` as an additional Flow source. No repository changes are required — the method already exists and returns `Flow<List<ServicioExtra>>`. Pago classification is extended to consult both `dispensacionId` and `servicioExtraId` via a `when` expression. The `ServicioExtra` entity gains an optional `fechaEntrega` mirror of `DispensacionOptica.fechaEntrega`, with paired Room `28→29` and Supabase migrations adding a nullable column. TDD red→green per `openspec/config.yaml` (`tdd: true`): failing tests first, then implementation. See delta specs `reportes-financieros/spec.md` and `servicio-extra/spec.md`.

## Architecture Decisions

### Decision 1: How servicios enter each Combine chain

**Choice**: Per-ViewModel compose strategy that respects each chain's existing shape.

| ViewModel | Current sources | New source added | Strategy |
|-----------|-----------------|------------------|----------|
| `ReportesViewModel.totalVendido`/`totalPagado` | `allDispensaciones` StateFlow (already filtered) | new `allServiciosDelPeriodo` StateFlow mirroring `allDispensaciones` (filter by `dentroDelPeriodo`) | `combine(allDispensaciones, allServiciosDelPeriodo)` → sum both lists |
| `ReportesViewModel.cobrosPeriodo` | `(pagos, todasDisp)` inside flatMapLatest | `repository.getAllServiciosForOptica` + build `servMap` | `combine(pagos, todasDisp, todasServ)` (3-arg Kotlin combine) |
| `CierreCajaViewModel.observePagos` | `(pagos, dispensaciones)` 2-arg combine | `repository.getAllServiciosForOptica` (third source) | 3-arg combine; build `servMap` for classification + `serviciosExtraHoy` filter |
| `BIViewModel.observeStats` | nested `combine` of 4 + 2 outer | fold servicios into the inner 4-arg `combine` (becomes 5-arg) | Kotlin `combine` supports up to 5 typed args — fits exactly |

**Alternatives considered**: A shared "DailySalesUseCase" (exploration Approach B) — deferred per proposal `Out of Scope`. Rejected to keep the change localized and review-scoped.
**Rationale**: Each chain has a different arity and filter shape; reusing existing `combine` patterns keeps the diff minimal and matches the codebase's reactive style. `allServiciosDelPeriodo` mirrors `allDispensaciones` so both maps share the same period-filter semantics.

### Decision 2: Dual-ID payment classification

**Choice**: A `when` expression consulting `dispensacionId` first, then `servicioExtraId`, with the existing `else` branch preserved.

```kotlin
// CierreCajaViewModel (fecha == null preserves old behavior; Reportes uses dentroDelPeriodo)
val dispFecha = pago.dispensacionId?.let { dispMap[it]?.fecha }
val servFecha = pago.servicioExtraId?.let { servMap[it]?.fecha }
when {
    dispFecha != null && dispFecha == fecha      -> ventasHoy      // dispensación wins
    servFecha != null && servFecha == fecha      -> ventasHoy
    dispFecha != null && dispFecha < fecha      -> cobrosAtrasados
    servFecha != null && servFecha < fecha       -> cobrosAtrasados
    else                                         -> ventasHoy      // orphan preserved
}
```

For `ReportesViewModel.cobrosPeriodo`, the dual-ID `when` mirrors the spec's " dispensación date wins when both IDs resolve" rule:

```kotlin
val dispFecha = pago.dispensacionId?.let { dispMap[it]?.fecha }
when {
    dispFecha != null && dentroDelPeriodo(dispFecha, p, a, fd, now) -> 0.0
    dispFecha == null && pago.servicioExtraId?.let { servMap[it]?.fecha }
        ?.let { dentroDelPeriodo(it, p, a, fd, now) } == true       -> 0.0
    else                                                            -> pago.monto
}
```

**Rationale**: Spec scenario "Pago with both IDs falls back to dispensación date" requires dispensación precedence. Preserving the orphan `else` branch keeps historical behavior (proposal risk: "orphan classification drift — Low").

### Decision 3: Room MIGRATION_28_29

**Choice**: Follow the `MIGRATION_27_28` pattern exactly:

```kotlin
val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE servicios_extra ADD COLUMN fecha_entrega TEXT")
    }
}
```

- `OptoDatabase.kt`: bump `version = 28` → `29`; add `val MIGRATION_28_29 get() = com.example.optoapp.data.MIGRATION_28_29`; append it to the `addMigrations(...)` list.
- `ServicioExtra` data class: add `@SerialName("fechaEntrega") @Serializable(with = LocalDateSerializer::class) val fechaEntrega: LocalDate? = null` last (preserves Kotlin default arg ordering for existing callers).

**Rationale**: Nullable `TEXT` matches `DispensacionOptica.fechaEntrega` (MIGRATION_23_24 used `ALTER TABLE dispensaciones ADD COLUMN fechaEntrega TEXT`). No DEFAULT preserves Room entity annotation parity (gotcha from `MIGRATION_26_27`). LocalDate serializer handles ISO-8601 string conversion.

### Decision 4: Supabase migration

**Choice**: New file `supabase/migrations/20260630000000_servicios_extra_fecha_entrega.sql`:

```sql
ALTER TABLE public.servicios_extra ADD COLUMN fecha_entrega DATE;
```

**Rationale**: Nullable `DATE` — back-fills existing rows to `NULL`, no data loss (spec scenario "Column added as nullable DATE"). Timestamp `20260630000000` follows the existing `YYYYMMDDhhmmss` convention (`20260424024500_servicios_extra_ot_placeholder_alignment.sql`).

## Data Flow

```
ReportesViewModel
─────────────────
sessionManager.opticaId ─┐
_periodo/_anio/_fechaDiario ─┐
                         │
getAllDispensacionesForOptica ─┐
                              combine → allDispensaciones (filtered)
getAllServiciosForOptica ─┐
                         combine → allServiciosDelPeriodo (filtered, NEW)
                              │
totalVendido = Σ disp.montoTotal + Σ serv.montoTotal
totalPagado  = Σ disp.montoPagado + Σ serv.aCuenta
cobrosPeriodo: combine(pagos, todasDisp, todasServ) → dual-ID when

CierreCajaViewModel
───────────────────
(fecha, opticaId) → flatMapLatest →
  combine(pagos, dispensaciones, servicios) →
  dispMap + servMap → dual-ID when →
  totalVentasHoy (disp) + totalServiciosExtra (NEW) + totalGeneral + saldoPendiente

BIViewModel
───────────
(periodo, opticaId) → flatMapLatest →
  combine(evaluacionesActual, evaluacionesAnterior, dispensaciones, pagos,
          servicios) → proyectada = Σ disp.montoTotal + Σ serv.montoTotal
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `optoapp/src/main/java/com/example/optoapp/viewmodel/ReportesViewModel.kt` | Modify | Add `allServiciosDelPeriodo` StateFlow; `totalVendido`/`totalPagado` combine disp+serv; `cobrosPeriodo` 3-arg combine with `servMap` + dual-ID when |
| `optoapp/src/main/java/com/example/optoapp/viewmodel/CierreCajaViewModel.kt` | Modify | `CierreCajaUiState` += `serviciosExtraHoy`, `totalServiciosExtra`, `totalGeneral`; `CierreCajaResult` += same; `observePagos` 3-arg combine; dual-ID when; `saldoPendiente = totalGeneral - ventasHoy` |
| `optoapp/src/main/java/com/example/optoapp/viewmodel/BIViewModel.kt` | Modify | `BiCoreFlows` += `servicios: List<ServicioExtra>`; inner `combine` 4→5 args; `recaudacionProyectada += servicios.sumOf { it.montoTotal }` |
| `optoapp/src/main/java/com/example/optoapp/data/dispensacion/DispensacionEntity.kt` | Modify | `ServicioExtra` += `fechaEntrega: LocalDate? = null` (with `LocalDateSerializer`) |
| `optoapp/src/main/java/com/example/optoapp/data/OptoDatabase.kt` | Modify | `version = 29`; add `MIGRATION_28_29` re-export; append to `addMigrations(...)` |
| `optoapp/src/main/java/com/example/optoapp/data/OptoDatabaseMigrations.kt` | Modify | Add `MIGRATION_28_29` |
| `supabase/migrations/20260630000000_servicios_extra_fecha_entrega.sql` | Create | `ALTER TABLE public.servicios_extra ADD COLUMN fecha_entrega DATE;` |
| `optoapp/src/test/java/com/example/optoapp/data/ServicioExtraMigration28To29Test.kt` | Create | RED test mirroring `Migration27To28Test`: chain sequentiality, column add + row preservation, fresh insert |
| `optoapp/src/test/java/com/example/optoapp/viewmodel/ReportesViewModelDiarioTest.kt` | Modify | Add servicio-extra fixtures; assert `totalVendido`/`totalPagado`/`cobrosPeriodo` include servicios |
| `optoapp/src/test/java/com/example/optoapp/viewmodel/ReportesViewModelOtrosPeriodosTest.kt` | Modify | Service-extra fixtures for Semanal/Mensual/Anual periods |
| `optoapp/src/test/java/com/example/optoapp/viewmodel/CierreCajaViewModelTest.kt` | Modify | Assert new `CierreCajaUiState` fields; dual-ID classification scenarios |
| `optoapp/src/test/java/com/example/optoapp/viewmodel/BIViewModelTest.kt` | Create | First-ever `BIViewModelTest`: `recaudacionProyectada` includes servicios + existing fields characterization |

## Interfaces / Contracts

```kotlin
// CierreCajaUiState new fields
data class CierreCajaUiState(
    val fecha: LocalDate = DateUtils.today(),
    val pagos: List<Pago> = emptyList(),
    val totalVentasHoy: Double = 0.0,
    val serviciosExtraHoy: List<ServicioExtra> = emptyList(),  // NEW
    val totalServiciosExtra: Double = 0.0,                    // NEW
    val totalGeneral: Double = 0.0,                           // NEW = totalVentasHoy + totalServiciosExtra
    val ventasHoy: Double = 0.0,
    val cobrosAtrasados: Double = 0.0,
    val saldoPendiente: Double = 0.0,                         // FIXED = totalGeneral - ventasHoy
    val isLoading: Boolean = false,
    val arqueoForFecha: ArqueoCaja? = null
)

// ServicioExtra new field (last position — backward-compat default)
@SerialName("fechaEntrega")
@Serializable(with = LocalDateSerializer::class)
val fechaEntrega: LocalDate? = null
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (ViewModel) | `ReportesViewModel`: totalVendido/totalPagado include servicios; `cobrosPeriodo` dual-ID classification per 6 spec scenarios | Mockk `getAllServiciosForOptica` + `getAllDispensacionesForOptica`; emit fixture flows; assert StateFlow values after `advanceUntilIdle`; pattern from `ReportesViewModelDiarioTest`. |
| Unit (ViewModel) | `CierreCajaViewModel`: desglose fields, `saldoPendiente`, dual-ID `ventasHoy`/`cobrosAtrasados` per 4 spec scenarios | Mirror existing `CierreCajaViewModelTest` setup (flowOf fixtures, `getPagosByDateRangeForOptica` mocked) |
| Unit (ViewModel) | `BIViewModel`: `recaudacionProyectada` = Σ disp + Σ serv; period-change re-emission | New `BIViewModelTest` file (none exists); reuse mockk patterns |
| Unit (Migration) | `MIGRATION_28_29`: column add, row preservation (N rows), fresh insert with `fecha_entrega`, chain sequentiality 6→29, re-export equality | Robolectric `Migration27To28Test` pattern: `FrameworkSQLiteOpenHelperFactory` v28→v29; insert `'2026-07-01'` text; assert equality |
| Integration | None — ViewModels already integration-tested via Repository; no new DB query paths | n/a |
| E2E | Cierre de Caja UI shows desglose | Manual smoke (out of scope for design; UI reads new StateFlow fields) |

## Migration / Rollout

**Room (local SQLite)**:
1. Bump `OptoDatabase.version` 28→29.
2. Register `MIGRATION_28_29` in `addMigrations(...)`.
3. `ALTER TABLE servicios_extra ADD COLUMN fecha_entrega TEXT` (nullable, no DEFAULT — matches Room entity annotations; existing rows back-fill to `NULL`).
4. Migration test (`ServicioExtraMigration28To29Test`) red before implementation, green after.
5. Rollback: revert `OptoDatabase.version` to 28 and remove `MIGRATION_28_29`; users who already migrated will recreate from scratch (destructive — document in release notes).

**Supabase (remote Postgres)**:
1. New migration file `20260630000000_servicios_extra_fecha_entrega.sql`.
2. CI `supabase-ci.yml` runs `supabase db lint` and `supabase db diff --linked` on PRs touching `supabase/migrations/`.
3. Column nullable → no app downtime; `supabase-kt` serializer reads `fechaEntrega` as null until the Android app post-backs.
4. Rollback: `ALTER TABLE public.servicios_extra DROP COLUMN IF EXISTS fecha_entrega` if not yet in production release.

**Phasing**: shipping Room + Supabase + ViewModel in one PR is safe — nullable column + additive reads. Sync layer (out of scope) will pick up `fechaEntrega` in a future change.

## Open Questions

- [ ] None.