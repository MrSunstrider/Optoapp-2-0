# Proposal: Limpieza Reportes Financieros

## Intent

Three defects in `ReportesViewModel` + `ReportesScreen`: **(A)** `totalCobrado` builds an unused `dispMap` from `allDispensaciones`; **(B)** "Semanal" derives its week from `fechaDiario` but the UI only renders the date-picker button for "Diario"; **(D)** `totalCobrado` and `cobrosPeriodo` fetch every pago with `getPagosByDateRangeForOptica(MIN, MAX, …)` and filter in-memory, though the DAO already accepts a real range. User confirmed **C** (pagos sin `dispensacionId` counted as cobro) is correct, unchanged.

## Scope

### In Scope
- **A. Dead code** — drop unused `dispMap`; remove `allDispensaciones` from `totalCobrado`'s `combine`.
- **B. Semanal date picker** — render the existing date-picker button when `periodo == "Semanal"`. No ViewModel state change.
- **D. Push period to SQL** — compute `(start, end)` per period and pass to `getPagosByDateRangeForOptica`; add `private fun rangoPagos(...)` in the ViewModel.
- New `ReportesViewModelTest` (Robolectric + in-memory Room) for A, B, C regression, D

### Out of Scope
- **C.** Pagos sin `dispensacionId` counted as cobro — confirmed, unchanged.
- PDF generator, new filters. No Supabase schema, RLS, sync, or DB migration.

## Capabilities

### New Capabilities
- None — refactor + UX gap-fill.

### Modified Capabilities
- None — `ReportesScreen` has no existing spec.

## Approach

- **A.** Drop dispensaciones combine-source; remove dead local.
- **B.** Extract `PeriodDateButton(visible = "Diario" || "Semanal", …)` composable.
- **D.** Call `rangoPagos()` inside each `flatMapLatest`. `cobrosPeriodo` keeps `getAllDispensacionesForOptica` for classification.
- TDD per `openspec/config.yaml`; fake `OptoRepository` records call args.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `optoapp/.../viewmodel/ReportesViewModel.kt` | Modified | Remove dead `dispMap` + dispensaciones source; add `rangoPagos()`; pass real range to pagos DAO |
| `optoapp/.../ui/screens/ReportesScreen.kt` | Modified | Show date-picker for "Semanal"; extract shared composable |
| `optoapp/src/test/.../viewmodel/ReportesViewModelTest.kt` | New | Robolectric tests for A, B, C, D |
| `optoapp/.../data/pago/PagoDao.kt` | Unchanged | `getPagosByDateRangeForOptica(start, end, opticaId)` already supports the optimization |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Off-by-one in week/month boundaries | Med | Per-period tests (Sunday week start, Jan 31 vs Feb 1) |
| Hidden consumer of `allDispensaciones` in `totalCobrado` | Low | Test: `totalCobrado` unchanged when only dispensaciones change |

## Rollback Plan

Revert commits to `ReportesViewModel.kt`, `ReportesScreen.kt`, and the new test file. Pure code-level revert; no schema, no data, no sync impact.

## Dependencies

- `OptoRepository.getPagosByDateRangeForOptica` accepts a date range (existing).
- Robolectric + `Room.inMemoryDatabaseBuilder` (used elsewhere).

## Success Criteria

- [ ] `totalCobrado` does not depend on `allDispensaciones`.
- [ ] `ReportesScreen` shows date-picker for "Semanal"; date change updates the week.
- [ ] `getPagosByDateRangeForOptica` receives `(start, end)` matching the period.
- [ ] Pagos sin `dispensacionId` still count as cobro (C regression).
- [ ] `./gradlew :optoapp:testDebugUnitTest --stacktrace` passes with new `ReportesViewModelTest`.
