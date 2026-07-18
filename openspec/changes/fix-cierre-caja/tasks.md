# Tasks: Fix Cierre de Caja — Payment Balance and Data Correctness

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~370 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |
| Chain strategy | size-exception |

Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Medium

## Phase 1: Data Layer — DAO + Repository

- [ ] 1.1 Add `getDispensacionesByDateRangeForOptica(start, end, opticaId)` to `DispensacionDao`
  - Files: `data/dispensacion/DispensacionDao.kt`
  - Deps: none | AC: Query returns only rows within date range for given opticaId
- [ ] 1.2 Add `getServiciosByDateRangeForOptica(start, end, opticaId)` to `ServicioExtraDao`
  - Files: `data/servicio/ServicioExtraDao.kt`
  - Deps: none | AC: Query returns only rows within date range for given opticaId
- [ ] 1.3 Expose new queries through `DispensacionRepository`
  - Files: `data/DispensacionRepository.kt`
  - Deps: 1.2 | AC: Repository delegates to new ServicioExtraDao method
- [ ] 1.4 Expose new queries through `OptoRepository`
  - Files: `data/OptoRepository.kt`
  - Deps: 1.1, 1.2 | AC: OptoRepository calls repository-layer methods for both new queries

## Phase 2: UI Components

- [ ] 2.1 Fix `ResumenCard` format string: `"%.0f"` → `"%,.2f"` with locale-aware formatting
  - Files: `ui/components/cierre-caja/ResumenCard.kt`
  - Deps: none | AC: Amounts display as "S/150.00" not "S/150"

## Phase 3: Core Logic — ViewModel

- [ ] 3.1 Replace `getAllDispensacionesForOptica`/`getAllServiciosExtraForOptica` with date-filtered DAO queries
  - Files: `viewmodel/CierreCajaViewModel.kt`
  - Deps: 1.4 | AC: ViewModel queries only date-range rows instead of full table
- [ ] 3.2 Fix `saldoPendiente`: compute as `Σ(montoTotal - montoPagado) + Σ(montoTotal - aCuenta)` per entity
  - Files: `viewmodel/CierreCajaViewModel.kt`
  - Deps: 3.1 | AC: S/300 disp paid S/200 yesterday + S/100 today → saldoPendiente = S/0
- [ ] 3.3 Add future-date classification: `Log.w` + exclude from `ventasHoy` when linked entity has future fecha
  - Files: `viewmodel/CierreCajaViewModel.kt`
  - Deps: 3.1 | AC: Future-dated payment excluded from ventasHoy, Log.w emitted with entity ID

## Phase 4: Presentation — Screen

- [ ] 4.1 Update per-item display: "Pagado" shows `disp.montoPagado`/`serv.aCuenta`, "Saldo" = `montoTotal - pagado`
  - Files: `ui/screens/CierreCajaScreen.kt`
  - Deps: 3.2 | AC: Each item shows cumulative payment from entity field, not filtered uiState.pagos

## Phase 5: Test Suite

- [ ] 5.1 Rewrite `CierreCajaViewModelTest` with Room in-memory DB, mockk for session, covering all spec scenarios
  - Files: `CierreCajaViewModelTest.kt`
  - Deps: 1.1, 1.2, 2.1, 3.3, 4.1 | AC: All spec scenarios pass; multi-optica leakage assertion passes; `./gradlew :optoapp:testDebugUnitTest --stacktrace` green
