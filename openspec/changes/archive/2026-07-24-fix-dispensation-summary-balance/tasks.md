# Tasks: Fix Dispensation Summary Balance

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~80 (4 fix lines + test files) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: RED — Write Failing Tests

- [x] 1.1 Create `DispensacionViewModelSaldoTest.kt`: insert a Pago with `tipo = "Abono"` (monto=100) and another with `tipo = "Anulación"` (monto=-100) for same `dispensacionId`; assert `pagosSumByDispensacion` maps to 100.0 (not 0.0)
- [x] 1.2 Add test to same file: insert Pago with `tipo = "Abono"` (monto=100) and `tipo = "Anulación"` (monto=-100) for same `servicioExtraId`; assert `aCuentaSumByServicio` maps to 100.0 (not 0.0)
- [x] 1.3 Add tests to `ReportesViewModelTest` (or new `ReportesViewModelSaldoTest.kt`): same two scenarios for `ReportesViewModel.pagosSumByDispensacion` and `ReportesViewModel.aCuentaSumByServicio`
- [x] 1.4 Run tests and confirm they fail (RED) — `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Phase 2: GREEN — Apply Fix

- [x] 2.1 In `DispensacionViewModel.kt:143`: replace `.filter { it.dispensacionId != null }` with `.filter { it.tipo != "Anulación" && it.dispensacionId != null }` in `pagosSumByDispensacion`
- [x] 2.2 In `DispensacionViewModel.kt:154`: replace `.filter { it.servicioExtraId != null }` with `.filter { it.tipo != "Anulación" && it.servicioExtraId != null }` in `aCuentaSumByServicio`
- [x] 2.3 In `ReportesViewModel.kt:254`: same replacement for `pagosSumByDispensacion`
- [x] 2.4 In `ReportesViewModel.kt:265`: same replacement for `aCuentaSumByServicio`
- [x] 2.5 Run tests and confirm all pass (GREEN) — `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Phase 3: REFACTOR — Clean Up

- [x] 3.1 Remove the misleading comment "Anulaciones (negative monto) are INCLUDED so they net out correctly" from both ViewModels (lines 137, 247)
- [x] 3.2 Verify no other flows miss this filter — grep for `.filter { it.dispensacionId` and `.filter { it.servicioExtraId` in `viewmodel/` to confirm all match the pattern
- [x] 3.3 Run full test suite one final time: `./gradlew :optoapp:testDebugUnitTest --stacktrace`
