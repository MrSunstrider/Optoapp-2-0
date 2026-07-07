# Tasks: Fix Gastos Categoria Constraint Mismatch

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~65 (3 files modified + 1 new test file) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |
| Chain strategy | size-exception |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: RED — Write Failing Tests

- [x] 1.1 Add test for `GastosUiState()` default `categoria` equals `"alquiler"` in a new test file or `GastosRecurrentesTest.kt` (fails while line 22 still has `"Local"`)
- [x] 1.2 Add test for `GastosViewModel.categorias` containing exactly the 8 DB CHECK values (fails while line 97 still has old labels)

## Phase 2: GREEN — Implement Production Code

- [x] 2.1 Replace `GastosViewModel.kt` line 97: swap `categorias` list values with DB CHECK constraint values
- [x] 2.2 Replace `GastosViewModel.kt` line 22: change `GastosUiState` default `categoria` from `"Local"` to `"alquiler"`

## Phase 3: REFACTOR — Update Test Fixtures

- [x] 3.1 Update `GastosRecurrentesTest.kt` — replace `"Local"`→`"alquiler"`, `"Planilla"`→`"personal"`, `"Reparacion"`→`"servicios"` in test fixtures and assertions
- [x] 3.2 Update `OptoRepositoryFinanzasTest.kt` — replace `"Alquiler"`→`"alquiler"`, `"Servicios"`→`"servicios"`, `"Sueldos"`→`"personal"`, `"Marketing"`→`"marketing"`, `"Temporal"`→`"otro"`, `"Internet"`→`"servicios"` in test fixtures

## Phase 4: VERIFY — Confirm All Green

- [x] 4.1 Run `./gradlew :optoapp:testDebugUnitTest --stacktrace` — all tests pass
- [x] 4.2 Run `./gradlew :optoapp:assembleDebug` — compilation succeeds
