# Tasks: Deferred Tier 3 Paciente Tech Debt

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~170 + tests (~250 total) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: Group A — Quick Wins (Items 15, 18, 21)

- [x] **1.1** [TEST] `SyncPacientesUseCaseTest`: `toRemoto()` produces JSON array, `toEntity()` decodes JSON + CSV fallback
- [x] **1.2** `SyncPacientesUseCase`: `toRemoto()` → `Json.encodeToString()`, `toEntity()` → `Json.decodeFromString` + CSV fallback
- [x] **1.3** `SyncHistorialUseCase`: line 97 `joinToString` → `Json.encodeToString()` — No test needed (trivial, same pattern)
- [x] **1.4** [REFACTOR] Add `TODO` comment to remove CSV fallback after full migration. Verify all tests green.
- [x] **1.5** [TEST] `PacienteRepositoryTest`: insert `HO-2026-XXXX` rows → `getMaxHistoriaNum` returns correct MAX
- [x] **1.6** `PacienteDao`: add `getMaxHistoriaNum(opticaId, year)` → `MAX(CAST(SUBSTR(historiaOptometrica, 9) AS INTEGER))` query
- [x] **1.7** `PacienteRepository`: replace in-memory MAX loop with `dao.getMaxHistoriaNum()` call
- [x] **1.8** [REFACTOR] Remove unused DAO query `getHistoriasOptometricasByOptica` if no other callers. Verify tests green.
- [x] **1.9** [TEST] `GastosViewModelTest`: emitted gastos are sorted by `fecha` descending
- [x] **1.10** `GastosViewModel`: add `sortedByDescending { it.fecha }` in state derivation
- [x] **1.11** `GastosScreen`: remove inline `sortedByDescending` from `items()` call
- [x] **1.12** [TEST] `MonturasViewModelTest`: derived state respects `sortBy` field matching pre-change behavior
- [x] **1.13** `MonturasViewModel`: absorb sort/filter logic as derived `StateFlow`
- [x] **1.14** `MonturasScreen`: remove inline sort/filter, consume from VM state
- [x] **1.15** [REFACTOR] Verify all Group A tests pass

## Phase 2: Group B — UI Polish (Items 19, 20, 22)

- [x] **2.1** [TEST] New `EmptyStateTest` (Compose): renders icon, title, optional subtitle and action button
- [x] **2.2** Create `ui/components/common/EmptyState.kt` with `@Composable fun EmptyState(...)`
- [x] **2.3** `DetallePacienteScreen` tab composables: replaced `EmptyListMessage` with `EmptyState` in EvaluacionesList, DispensacionesList, ServiciosExtraList
- [x] **2.4** [REFACTOR] Verify Compose UI tests pass
- [x] **2.5** `PacienteInfoHeader`: added `FlowRow` of `SuggestionChip` displaying `ultimasEtiquetas` when non-empty — No test needed (display-only, Compose preview)
- [x] **2.6** [TEST] `DetallePacienteScreenErrorTest`: structural verification of timeout/error/retry mechanism (for existing pattern, no Compose rendering test possible due to Hilt dependency; structural tests verify concept)
- [x] **2.7** `DetallePacienteScreen`: replaced infinite `CircularProgressIndicator` with `LaunchedEffect` timeout (5s) + error/retry state. `retryTrigger` key restarts load on retry. Error state shows icon, message, and retry button.
- [x] **2.8** [REFACTOR] Full `./gradlew :optoapp:testDebugUnitTest --stacktrace` green — all 38+ tests pass, zero failures.
