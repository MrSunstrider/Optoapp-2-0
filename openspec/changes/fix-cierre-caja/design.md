# Design: Fix Cierre de Caja — Payment Balance and Data Correctness

## Technical Approach

Six changes across three layers preserving Clean Architecture: DAO layer (add date-filtered ServicioExtra query), presentation layer (ViewModel formula + Screen per-item + ResumenCard format), test layer (Room in-memory + multi-optica). Core fix replaces the `totalGeneral - ventasHoy` aggregate with per-entity subtraction (`montoTotal - montoPagado` / `montoTotal - aCuenta`), which accounts for historical payments that the old formula ignored.

## Architecture Decisions

| Decision | Option A (chosen) | Option B (rejected) | Rationale |
|----------|------------------|---------------------|-----------|
| saldoPendiente source | Entity-tracked `montoPagado`/`aCuenta` | Filter today's pagos from ViewModel | Entity fields track cumulative payments across all dates; ViewModel pagos are date-filtered and miss historical payments. |
| DAO granularity | Date-filtered `getByDateRange*` + batch ID lookup for pago-linked items | Keep `getAll*ForOptica` | ~20x fewer rows for typical optica (1 day vs all-time). Pago-linked items outside date range still need batched lookup. |
| Per-item "Pagado" | `disp.montoPagado` / `serv.aCuenta` | Filter `uiState.pagos` by entity ID | Entity fields are the canonical payment total; uiState pagos only contain today's payments. |
| Future-date handling | Explicit `Log.w` + separate counter | Silent fallthrough to `ventasHoy` | Data integrity: future-dated links indicate sync/timestamp error and must not distort today's numbers. |
| Test framework | Room in-memory + mockk for session | Pure mockk | Room in-memory catches SQL errors, multi-optica isolation, and validates DAO queries correctly. Mandatory for multi-optica leakage test. |

## Data Flow

```
User selects date
      │
      ▼
CierreCajaViewModel.observePagos()
      │
      ├── combine(fecha, opticaId, refreshTrigger)
      │       │
      │       ▼
      │   flatMapLatest
      │       │
      │       ├── getPagosByDateRangeForOptica(start, end, opticaId)  → today's pagos
      │       ├── getDispensacionesByDateRangeForOptica(start, end, opticaId) → date-filtered dispensaciones
      │       └── getServiciosByDateRangeForOptica(start, end, opticaId) [NEW] → date-filtered servicios
      │
      ▼
Combine lambda:
  1. Extract pago-linked IDs (dispensacionId, servicioExtraId)
  2. Merge date-filtered + pago-linked entities (deduped)
  3. Classify pagos: ventasHoy / cobrosAtrasados / future-warning
  4. saldoPendiente = Σ(montoTotal - montoPagado) + Σ(montoTotal - aCuenta)  ← KEY FIX
  5. dispOtMap for OT labels
  6. Emit CierreCajaUiState
      │
      ▼
CierreCajaScreen (Compose)
  - Total ventas: uiState.totalGeneral (%.2f format)
  - ResumenCard x3: %.2f format [FIXED from %.0f]
  - Per-item: disp.montoPagado / serv.aCuenta [FIXED from filtering uiState.pagos]
  - Per-item saldo: montoTotal - pagado
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `data/servicio/ServicioExtraDao.kt` | Modify | Add `getServiciosByDateRangeForOptica(start, end, opticaId): Flow<List<ServicioExtra>>` |
| `data/DispensacionRepository.kt` | Modify | Add delegation for new ServicioExtra date-range query |
| `data/OptoRepository.kt` | Modify | Add delegation for new ServicioExtra date-range query |
| `viewmodel/CierreCajaViewModel.kt` | Modify | Replace `getAll*ForOptica` with date-filtered; new saldoPendiente formula; future-date branch; batch pago-linked fetch |
| `ui/screens/CierreCajaScreen.kt` | Modify | Per-item "Pagado" from entity fields; per-item "Saldo" from entity fields |
| `ui/components/cierre-caja/ResumenCard.kt` | Modify | `"%.0f"` → `"%.2f"` |
| `CierreCajaViewModelTest.kt` | Rewrite | Room in-memory DB, multi-optica isolation, corrected assertions |

### Key Code Changes

**New DAO query** (`ServicioExtraDao`):
```sql
SELECT * FROM servicios_extra
WHERE fecha >= :start AND fecha <= :end AND opticaId = :opticaId
```

**New saldoPendiente** (ViewModel, inside combine lambda):
```kotlin
val saldoPendiente = dispensacionesHoy.sumOf { it.montoTotal - it.montoPagado } +
                     serviciosExtraHoy.sumOf { it.montoTotal - it.aCuenta }
```

**Future-date branch** (ViewModel, pagos.forEach):
```kotlin
pago.dispensacionId?.let { id -> dispMap[id]?.fecha }?.let { dispFecha ->
    if (dispFecha > fecha) { Log.w(TAG, "Future-dated disp"); futureCounter += pago.monto }
}
```

**Per-item in Screen**: `totalPagado = disp.montoPagado` (was: `pagos.filter { it.dispensacionId == disp.id }.sumOf { it.monto }`)

## Sequence: Reactive Flow

```
setFecha(newDate) → _uiState.update { it.copy(fecha = newDate) }
  → combine(fecha, opticaId, trigger).distinctUntilChanged()
  → flatMapLatest cancels previous flow, launches new combine
  → 3 DAO Flow queries execute in parallel
  → combine lambda classifies, computes, emits
  → Screen collectAsState() recomposes
```

## Error States

| State | Behavior |
|-------|----------|
| Empty data (no pagos/disp/serv) | All totals = 0.0, show "Sin movimientos este día" |
| Loading | `LinearProgressIndicator` visible, `isLoading = true` |
| Permission denied | `canView = false` → "Acceso restringido" card, no data queries |
| Future-dated links | `Log.w` per pago, excluded from `ventasHoy` and `cobrosAtrasados` |

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| DAO | New `getServiciosByDateRangeForOptica` | Room in-memory, insert servicios with different dates/opticaIds, assert correct rows |
| ViewModel | saldoPendiente with historical payments | `$300 disp, $200 paid yesterday + $100 today` → saldoPendiente = $0. Room in-memory: insert disp + 2 pagos, run ViewModel, assert |
| ViewModel | Multi-optica isolation | Room in-memory: insert data for optica-A and optica-B, run ViewModel for optica-A, assert zero optica-B items in uiState |
| ViewModel | Future-date classification | Pago linked to disp with fecha > selectedDate → ventasHoy excludes it, Log.w called |
| ViewModel | serv.aCuenta in saldoPendiente | ServicioExtra montoTotal=150, aCuenta=75 → contributes 75 to saldoPendiente |
| Screen | ResumenCard format | Snapshot test or manual verification: "s/. 150.00" not "s/. 150" |

## Migration Path

No schema changes, no data migration, no RLS changes. Entity fields (`montoPagado`, `aCuenta`) already exist. Existing tests must be rewritten (mockk → Room in-memory) and their assertions updated to match corrected formulas. Per-file revert possible via git revert.

## Open Questions

None. All decisions confirmed by proposal and adversarial review findings.
