# Apply Progress: av-ambliopia-ambidirectional

**Mode**: Strict TDD  
**Work unit**: Unit 2 — OptoWeb (feature-branch-chain) — COMPLETE  
**Updated**: 2026-09-26  
**Commits**: none (orchestrator commits later)

## Completed (Unit 1 — Android)

- [x] 1.1 RED `parseAvToLogMar` tests
- [x] 1.2 GREEN `parseAvToLogMar` implementation
- [x] 1.3 REGRESSION Snellen characterization
- [x] 2.1 RED `computeOtrosAuto` amblyopia tests
- [x] 2.2 GREEN wire `computeOtrosAuto` → `parseAvToLogMar`
- [x] 4.1 Android focused verify

## Completed (Unit 2 — OptoWeb)

- [x] 3.1 RED Web Vitest cases (`6/6` num/den, `avToLogMar` matrix, mixed `autoAmbliopiaValue`, unparseable → null)
- [x] 3.2 GREEN Web helpers (`snellenToLogMar` num/den + `avToLogMar` + wire)
- [x] 4.2 Web focused + schema untouched
- [x] 4.3 Work-unit closeout (all tasks marked)

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `DiagnosticoCalculatorTest.kt` | Unit | ✅ baseline DiagnosticoCalculatorTest + HelperTest green | ✅ Written (compile fail: unresolved `parseAvToLogMar`) | N/A (RED-only task) | ✅ 7 cases (blank, Snellen, decimal, reject, incomplete, equivalence, 6/6) | ➖ tests only |
| 1.2 | same | Unit | N/A (follows 1.1) | ✅ from 1.1 | ✅ DiagnosticoCalculatorTest green | ✅ forced real logic via reject/decimal/Snellen paths | ✅ minimal pure fn; `parseSnellenToLogMar` untouched |
| 1.3 | `DiagnosticoCalculatorTest` + `EvaluacionViewModelCatchRefactorTest` | Unit | ✅ prior Snellen green | ➖ approval (no new behavior) | ✅ both suites green; no Snellen contract edits | ➖ N/A | ➖ none |
| 2.1 | `EvaluacionDiagnosticoHelperTest.kt` | Unit | ✅ HelperTest transpose suite green | ✅ 6 failing amblyopia cases (preserve cases already green) | N/A (RED-only task) | ✅ mixed / decimal / threshold / preserve / auto-off | ➖ tests only |
| 2.2 | same | Unit | N/A (follows 2.1) | ✅ from 2.1 | ✅ HelperTest green after one-line parser swap | ✅ all amblyopia scenarios | ➖ none needed |
| 4.1 | all three classes | Unit | N/A | N/A | ✅ focused verify BUILD SUCCESSFUL | N/A | N/A |
| 3.1 | `optoweb/.../evaluacion-constants.test.ts` | Unit | ✅ 53/53 baseline green | ✅ 7 failing (6/6 hardcode + missing `avToLogMar` + mixed auto) | N/A (RED-only task) | ✅ Snellen num/den, decimal matrix, reject, equivalence, mixed auto, null preserve | ➖ tests only |
| 3.2 | same | Unit | N/A (follows 3.1) | ✅ from 3.1 | ✅ 61/61 Vitest pass | ✅ all Web parity scenarios | ➖ none needed (minimal Android-parity helpers) |
| 4.2 | Vitest focused | Unit | N/A | N/A | ✅ 61 passed; form untouched; no `supabase/migrations/` dirty | N/A | N/A |
| 4.3 | tasks.md | N/A | N/A | N/A | ✅ all checkboxes `[x]` | N/A | N/A |

### Test Summary

- **Unit 1 new tests**: 7 (`parseAvToLogMar`) + 9 (`computeOtrosAuto`) = 16
- **Unit 2 new/extended tests**: +8 cases (1 snellen num/den + 5 avToLogMar + 2 autoAmbliopiaValue) → file 53 → 61
- **Focused suites passing**: Android Unit 1: 72 tests; Web Unit 2: 61 tests, 0 failures
- **Layers used**: Unit only
- **Approval tests**: Snellen characterization left unchanged (1.3); existing Web `20/x` snellen cases remain green
- **Pure functions created**: Android `parseAvToLogMar`; Web `avToLogMar` (+ `snellenToLogMar` fix)

## Work Unit Evidence (Unit 1)

| Evidence | Value |
|----------|-------|
| Focused test command | `./gradlew :optoapp:testDebugUnitTest --tests "com.example.optoapp.viewmodel.diagnostico.DiagnosticoCalculatorTest" --tests "com.example.optoapp.viewmodel.EvaluacionDiagnosticoHelperTest" --tests "com.example.optoapp.viewmodel.EvaluacionViewModelCatchRefactorTest" --stacktrace` |
| Exact result | BUILD SUCCESSFUL — 44 + 15 + 13 = 72 tests, 0 failures |
| Runtime harness | N/A — pure unit helpers; no device/UI boundary |
| Rollback boundary | Revert Android changes restoring `computeOtrosAuto` → `parseSnellenToLogMar` and removing `parseAvToLogMar` + Unit 1 tests |

## Work Unit Evidence (Unit 2)

| Evidence | Value |
|----------|-------|
| Focused test command | `cd ../optoweb && npm test -- src/lib/__tests__/evaluacion-constants.test.ts` |
| Exact result | Vitest 61 passed (1 file), 0 failures (after GREEN) |
| Runtime harness | N/A — pure Vitest helpers; form already wired via `autoAmbliopiaValue` |
| Rollback boundary | Revert Web helper commit restoring `20/den` hardcode and Snellen-only `autoAmbliopiaValue`; remove `avToLogMar` + Unit 2 Vitest cases |
| Schema / form | No `supabase/migrations/` changes; `nueva-evaluacion-form.tsx` untouched |

## Workload / PR Boundary

- Mode: feature-branch-chain (Unit 2 OptoWeb only in this batch)
- Boundary: Web helpers + Vitest; Android left as Unit 1 completed slice
- Next: `sdd-verify` for full change verification

## Deviations from Design

None — implementation matches design.

## Issues Found

None.
