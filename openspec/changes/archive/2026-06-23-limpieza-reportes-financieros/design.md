# Design: Limpieza Reportes Financieros

## Technical Approach

Small refactor in `ReportesViewModel` (drop one combine argument, add one private helper, restructure two flows to push the date range into the DAO) plus a one-condition change in `ReportesScreen`. Reuse existing MockK + `UnconfinedTestDispatcher` + `runTest` test infrastructure. Maps to proposal A, B, D; C is unchanged.

## Architecture Decisions

| # | Decision | Choice | Rationale |
|---|----------|--------|-----------|
| 1 | A — remove dispensaciones from `totalCobrado` | Drop `allDispensaciones` from the `combine` args + the `dispensaciones` parameter; drop dead `dispMap`; keep `dentroDelPeriodo` filter as safety net | `totalCobrado` is `pago.monto` only; spec mandates dispensaciones-independence. YAGNI. |
| 2 | B — Semanal date-picker | Inline extend `if (periodo == "Diario")` to `if (periodo == "Diario" \|\| periodo == "Semanal")` | Minimum change that satisfies the spec scenario. Reuses shared `fechaDiario` / `datePickerState` / `showDatePicker` with zero behavior drift. (Proposal suggests composable extraction — see Open Q #2.) |
| 3 | D — push period into DAO | New `private fun periodDateRange(p, a, fd, now): Pair<LocalDate, LocalDate>`; use `flatMapLatest` over `combine(_periodo, _anio, _fechaDiario)` to recreate the pagos Flow when period changes | `combine` cannot swap its source Flow. `flatMapLatest` is the established pattern in this file (see `allDispensaciones`). |
| 4 | D — `cobrosPeriodo` dispensaciones source | Keep `getAllDispensacionesForOptica(opticaId)` unfiltered | Classification needs dispensaciones OUTSIDE the period (a pago in-period for an old dispensación = "cobro de períodos anteriores"). |
| 5 | Semanal week start | Monday (`startOfWeek = fd − (dayOfWeek.value − 1)`, `endOfWeek = startOfWeek + 6`) | Matches existing `dentroDelPeriodo`, tests, and user's design formula. See Open Q #1. |

## Data Flow

```
sessionManager.opticaId
  └─ flatMapLatest { opticaId ─────────────────────────────────┐
       combine(_periodo, _anio, _fechaDiario)                 │
         └─ flatMapLatest { (p, a, fd) ────────────────────────┤
              val (start, end) = periodDateRange(p,a,fd,now)   │
              repository.getPagosByDateRangeForOptica(start,   │
                end, opticaId)                                │
                └─ map { pagos ->                              │
                     pagos.filter { dentroDelPeriodo(…) }     │  // safety net
                          .sumOf { it.monto }                 │
                   }                                          │
            }                                                 │
     }                                                        │
```

`cobrosPeriodo` mirrors this but keeps `getAllDispensacionesForOptica` unfiltered and uses the dispensación date to classify each pago.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `optoapp/.../viewmodel/ReportesViewModel.kt` | Modify | Add `periodDateRange` helper. Rewrite `totalCobrado` (drop `allDispensaciones` + `dispMap`) and `cobrosPeriodo` (push pagos range) to use `flatMapLatest` over `combine(_periodo, _anio, _fechaDiario)`. Keep `dentroDelPeriodo` filter in both. |
| `optoapp/.../ui/screens/ReportesScreen.kt` | Modify | Extend `if (periodo == "Diario")` → `if (periodo == "Diario" \|\| periodo == "Semanal")` (~line 126). No other changes — state, dialog, formatting, `Modifier.weight(1f)` are already correct. |
| `optoapp/.../viewmodel/ReportesViewModelDiarioTest.kt` | Modify | Add `verify { repository.getPagosByDateRangeForOptica(eq(today), eq(today), eq(opticaId)) }`; add test that mutates `allDispensaciones` and asserts `totalCobrado` constant (A regression). |
| `optoapp/.../viewmodel/ReportesViewModelOtrosPeriodosTest.kt` | Modify | Add `verify` per period asserting the captured range. |
| `PagoDao.kt`, `DispensacionRepository.kt`, `OptoRepository.kt` | Unchanged | `getPagosByDateRangeForOptica(start, end, opticaId)` already supports the optimization. |

## Interfaces / Contracts

```kotlin
private fun periodDateRange(
    p: String, a: String, fd: LocalDate, now: LocalDate
): Pair<LocalDate, LocalDate> = when (p) {
    "Diario"   -> fd to fd
    "Semanal"  -> {
        val startOfWeek = fd.minusDays((fd.dayOfWeek.value - 1).toLong())
        startOfWeek to startOfWeek.plusDays(6)   // Mon–Sun inclusive
    }
    "Este mes" -> now.withDayOfMonth(1) to now.withDayOfMonth(now.lengthOfMonth())
    "Este año" -> now.withDayOfYear(1)   to now.withDayOfYear(now.lengthOfYear())
    "Anual"    -> LocalDate.of(a.toInt(), 1, 1) to LocalDate.of(a.toInt(), 12, 31)
    else       -> LocalDate.MIN to LocalDate.MAX  // "Todo"
}
```

DAO query unchanged: `WHERE fecha >= :start AND fecha <= :end AND opticaId = :opticaId` — both bounds inclusive, per spec.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (MockK) | `periodDateRange` boundaries | `verify` per period in existing tests; no new files |
| Unit | A regression: `totalCobrado` invariant when only `allDispensaciones` changes | New test in `DiarioTest`: emit twice from the dispensaciones Flow, assert `totalCobrado` constant |
| Unit | C regression: `dispensacionId == null` → counted in `cobrosPeriodo` | Already covered; keep and add range `verify` |
| CI gate | `./gradlew :optoapp:testDebugUnitTest --stacktrace` | Unchanged; runs all three ViewModel test classes |

No new test infrastructure, no Robolectric needed — pure-MockK captures the DAO args.

## Migration / Rollout

No migration. Pure code refactor — no schema, no data, no sync impact. Rollback = revert the two files + test updates.

## Open Questions

- [ ] **Semanal week start**: Spec line 46 says "Sunday start"; existing `dentroDelPeriodo`, existing `ReportesViewModelOtrosPeriodosTest` (`currentMonday` + "Mon through Sun" test name), and the user's design formula all use **Monday start**. Design follows user/existing behavior. Confirm with PO that Monday is correct, then patch the spec text in a follow-up.
- [ ] **Composalable extraction (B)**: Proposal suggests extracting `PeriodDateButton`. Design inlines the change. If literal alignment with the proposal is required, extraction is a small follow-up; current inline change is lower risk and easier to revert.
