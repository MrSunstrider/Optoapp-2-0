# Tasks: fix-analisis-financiero-bugs

## Phase 1: Bug 2 — RED (failing UI tests)

1. Create `RecomendacionCardFeedbackTest` with Compose Test + Robolectric rendering `RecomendacionCard` with controlled state.
2. **1.1**: Write failing test for REQ-FEEDBACK-UI-1: render card with `feedbacksEnviados = mapOf("r1" to true)` — assert confirmation text "Gracias por tu valoración" + checkmark icon visible; assert buttons not displayed.
3. **1.2**: Write failing test for REQ-FEEDBACK-UI-2: render card with feedbacksEnviados containing rec.id — assert both thumb buttons are disabled (no click action triggers onFeedback).
4. **1.3**: Write failing test for REQ-FEEDBACK-UI-3: render card with `feedbackErrorRecId = "r1"` — assert inline error "No se pudo enviar tu valoración" visible; assert buttons remain enabled.

## Phase 2: Bug 2 — GREEN (implement fix)

5. **2.1**: Add `feedbackErrorRecId: String? = null` to `AnalisisNegocioUiState` in `AnalisisNegocioViewModel.kt`.
6. **2.2**: Update `onFeedback()` error handling in `AnalisisNegocioViewModel.kt`: set `feedbackErrorRecId` on failure, clear on retry success.
7. **2.3**: Add `feedbacksEnviados: Map<String, Boolean>` and `feedbackErrorRecId: String?` params to `RecomendacionCard` composable in `AnalisisNegocioScreen.kt`.
8. **2.4**: Implement three visual states in `RecomendacionCard`: default (buttons visible), success (checkmark + "Gracias por tu valoración"), error (inline red text, buttons enabled).
9. **2.5**: Verify Phase 1 tests pass in green.

## Phase 3: Bug 1 — RED (failing ViewModel tests)

10. **3.1**: Add in-memory Room DB test setup to `GastosViewModelTest.kt` (real `OptoDatabase` + real `OptoRepository`).
11. **3.2**: Write failing test for REQ-GASTOS-TEST-1: `save()` emits new expense through `allGastos` with correct `fecha` — data integrity for monthly filter.
12. **3.3**: Write failing test for REQ-GASTOS-TEST-3: `save()` sets `uiState.error` when repository write fails (use MockK for error case).

## Phase 4: Bug 1 — GREEN (implement tests + diagnostics)

13. **4.1**: Add `TAG` companion, `import android.util.Log`, and `Log.d(TAG, ...)` diagnostic in `GastosViewModel.kt` init when `allGastos` first emits empty.
14. **4.2**: Implement test for REQ-GASTOS-TEST-3 diagnostic: verify Log.d contains "GastosViewModel", `opticaId`, and "0 gastos" on empty DB init.
15. **4.3**: Verify Phase 3 tests pass in green.

## Phase 5: Verify

16. **5.1**: Run `./gradlew :optoapp:testDebugUnitTest --stacktrace` — all tests pass.
17. **5.2**: Run `./gradlew :optoapp:assembleDebug` — build succeeds.

---

## Workload Forecast

**Estimated delta**: ~80-100 lines across 4 files.
**Delivery**: single PR, no chaining needed.

```
GUARD: EXPECTED CHANGES
AnalisisNegocioScreen.kt: ~25 lines  (RecomendacionCard UI states + params)
AnalisisNegocioViewModel.kt: ~8 lines  (feedbackErrorRecId + error handling)
GastosViewModel.kt: ~3 lines  (Log.d diagnostic + TAG)
GastosViewModelTest.kt: ~50 lines  (test setup + 4 new tests)
TOTAL: ~86 lines — well under 400-line threshold for single PR.
GUARD: OK
```
