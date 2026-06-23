# Tasks: Limpieza Reportes Financieros

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~140 (80 impl, 60 tests) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Delivery strategy | ask-on-risk |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

## Phase 1: Tests (TDD — write failing tests first)

- [x] 1.1 **T3.1** — Add regression test in `ReportesViewModelDiarioTest.kt`: emit twice from `allDispensaciones`, assert `totalCobrado` is constant. Verifies REQ-A1 (dispensaciones-independence).
- [x] 1.2 **T3.2** — Skipped: Compose UI testing not feasible with current test infrastructure. ViewModel behavior verified via ViewModel-level tests.
- [x] 1.3 **T3.3** — Add `verify { repository.getPagosByDateRangeForOptica(eq(today), eq(today), eq(opticaId)) }` in `ReportesViewModelDiarioTest.kt` for Diario period. Verifies REQ-D1 (exact range passed to DAO).
- [x] 1.4 **T3.4–T3.8** — Add `verify` assertions in `ReportesViewModelOtrosPeriodosTest.kt` for each period: Semanal (week range), Este mes (month range), Este año (year range), Anual (selected year range), Todo (MIN, MAX). All use `advanceUntilIdle()` before verification.
- [x] 1.5 Run tests — all new tests should fail against current code (DAO called with `any(), any()`).

## Phase 2: Core Implementation

- [x] 2.1 **T-01** — Add `private fun periodDateRange(p: String, a: String, fd: LocalDate, now: LocalDate): Pair<LocalDate, LocalDate>` to `ReportesViewModel.kt`. Implements spec line 46: Diario → `(fd, fd)`, Semanal → Mon/Sun, Este mes → first/last of month, Este año → Jan 1/Dec 31, Anual → Jan 1/Dec 31 of year `a`, Todo → `(LocalDate.MIN, LocalDate.MAX)`.
- [x] 2.2 **T-02 (REQ-A1 + REQ-D1)** — Rewrite `totalCobrado`: remove `allDispensaciones` from combine args, remove `dispensaciones` parameter, remove unused `dispMap`. Replace `LocalDate.MIN, LocalDate.MAX` with `periodDateRange(p, a, fd, now)`. Switch from `combine` to `flatMapLatest` over `combine(_periodo, _anio, _fechaDiario)`. Keep `dentroDelPeriodo` safety-net filter.
- [x] 2.3 **T-03 (REQ-D1)** — Rewrite `cobrosPeriodo`: replace `LocalDate.MIN, LocalDate.MAX` with `periodDateRange(p, a, fd, now)`. Switch from `combine` to `flatMapLatest` over `combine(_periodo, _anio, _fechaDiario)`. Keep `getAllDispensacionesForOptica` unfiltered (needed for classification). Keep `dentroDelPeriodo` safety-net filter.
- [x] 2.4 **T-04 (REQ-B1)** — In `ReportesScreen.kt` line 126: extend `if (periodo == "Diario")` to `if (periodo == "Diario" || periodo == "Semanal")`. No other changes — state, dialog, `fechaDiario`, DateUtils are already shared.

## Phase 3: Verification

- [x] 3.1 Run `./gradlew :optoapp:testDebugUnitTest --stacktrace` — all tests must pass including new T3.1–T3.9.
- [x] 3.2 T3.9 (empty data → zero) is already covered by existing test `empty data across all periods returns zero` in `ReportesViewModelOtrosPeriodosTest.kt` — confirm it still passes.

## Files to Modify

| File | Changes |
|------|---------|
| `optoapp/src/main/java/com/example/optoapp/viewmodel/ReportesViewModel.kt` | Add `periodDateRange` helper; refactor `totalCobrado` and `cobrosPeriodo` to use `flatMapLatest` + real date range; drop `allDispensaciones` from `totalCobrado` combine |
| `optoapp/src/main/java/com/example/optoapp/ui/screens/ReportesScreen.kt` | Extend date-picker visibility condition to include `periodo == "Semanal"` |
| `optoapp/src/test/java/com/example/optoapp/viewmodel/ReportesViewModelDiarioTest.kt` | Add T3.1 (A regression), T3.2 (Semanal picker), T3.3 (Diario exact range) |
| `optoapp/src/test/java/com/example/optoapp/viewmodel/ReportesViewModelOtrosPeriodosTest.kt` | Add T3.4–T3.8 (period range verification per period) |

## Dependencies

- T-01 must be completed before T-02 and T-03 (helper used by both).
- T-02 and T-03 are independent of each other (separate flows).
- T-04 (UI) is independent of all ViewModel changes — can run in parallel.

## Implementation Order

1. Tests first (Phase 1) — TDD red/green cycle; tests fail with `any(), any()` before refactor.
2. T-01 (helper) — foundational for the refactor.
3. T-02 (totalCobrado) and T-03 (cobrosPeriodo) — independent, can be done in either order.
4. T-04 (UI) — independent, can run in parallel with ViewModel work.
5. Phase 3 verification — single `./gradlew` run.

## Success Criteria

- `totalCobrado` does not subscribe to `allDispensaciones`.
- DAO receives `(fd, fd)` for Diario, week range for Semanal, month range for Este mes, year range for Este año, selected-year range for Anual, `(MIN, MAX)` for Todo.
- Date picker button renders for both `Diario` and `Semanal`.
- All tests pass including T3.1–T3.9.
