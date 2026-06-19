# Tasks: Fix CierreCaja Crash and Related Cash-Close Bugs

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~180–230 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | All six bug fixes + tests | PR 1 | Independent fixes; single PR well under 400-line budget |

---

## Phase 1: Atomic Infrastructure Fix — DAO conflict strategy (Bug #6)

- [x] 1.1 **[FAIL]** Add unit/integration test `ArqueoCajaDaoTest` in `src/test/java/com/example/optoapp/data/arqueo/ArqueoCajaDaoTest.kt`: insert arqueo, insert again with same PK, assert row count = 1 and values updated. Run `./gradlew testDebugUnitTest` → expect RED (ABORT throws exception).
- [x] 1.2 **[FIX]** In `src/main/java/com/example/optoapp/data/arqueo/ArqueoCajaDao.kt`: change `OnConflictStrategy.ABORT` → `OnConflictStrategy.REPLACE` on `insertArqueo`.
- [x] 1.3 **[PASS]** Run `./gradlew testDebugUnitTest` → GREEN on `ArqueoCajaDaoTest`.

---

## Phase 2: ViewModel Unit Fixes — Key Casing + Coroutine Leak (Bugs #4, #5)

- [x] 2.1 **[FAIL]** In `src/test/java/com/example/optoapp/viewmodel/ArqueoCajaViewModelTest.kt`: add test `totalesUseTitleCaseKeys` — seed fake repo with `Pago(metodoPago="Efectivo", monto=500.0)`, call `cerrarDia()`, assert `totales["Efectivo"] == 500.0` and `totales["efectivo"] == null`. Run `./gradlew testDebugUnitTest` → RED.
- [x] 2.2 **[FIX]** In `src/main/java/com/example/optoapp/viewmodel/ArqueoCajaViewModel.kt`: change `cerrarDia()` map lookup keys from `"efectivo"`, `"tarjeta"`, `"transferencia"`, `"movil"` to `"Efectivo"`, `"Tarjeta"`, `"Transferencia"`, `"Móvil"`.
- [x] 2.3 **[PASS]** Run `./gradlew testDebugUnitTest` → GREEN on `totalesUseTitleCaseKeys`.
- [x] 2.4 **[FAIL]** In `src/test/java/com/example/optoapp/viewmodel/CierreCajaViewModelTest.kt`: add test `observeArqueoForDateCancelsPreviousCollector` — use `StandardTestDispatcher`, call `observeArqueoForDate(date1)` then `observeArqueoForDate(date2)`, emit from date1 flow after switch, assert date1 emissions are NOT received. Run `./gradlew testDebugUnitTest` → RED.
- [x] 2.5 **[FIX]** In `src/main/java/com/example/optoapp/viewmodel/CierreCajaViewModel.kt`: replace manual Job-per-call pattern in `observeArqueoForDate` with a `MutableStateFlow<Pair<LocalDate, String>?>` driven via `flatMapLatest { getArqueoByFecha(...) }.onEach { ... }.launchIn(viewModelScope)` initialized in `init {}`.
- [x] 2.6 **[PASS]** Run `./gradlew testDebugUnitTest` → GREEN on `observeArqueoForDateCancelsPreviousCollector`.

---

## Phase 3: DI Restructure — Remove runBlocking from DatabaseModule (Bug #3)

- [x] 3.1 **[FAIL]** In `src/test/java/com/example/optoapp/viewmodel/ArqueoCajaViewModelTest.kt`: add test `userIdResolvedAsyncWithoutBlocking` — construct `ArqueoCajaViewModel` with `FakeSessionManager` returning `"user@test.com"` from `userEmail` flow; advance `TestCoroutineScope`; assert `viewModel.currentUserId == "user@test.com"` after coroutine runs. Run `./gradlew testDebugUnitTest` → RED (constructor still accepts `@CurrentUserId String`).
- [x] 3.2 **[FIX]** In `src/main/java/com/example/optoapp/viewmodel/ArqueoCajaViewModel.kt`: remove `@CurrentUserId` constructor param; inject `SessionManager` instead; add `private var currentUserId: String = ""`; in `init { viewModelScope.launch { currentUserId = sessionManager.userEmail.first() } }`.
- [x] 3.3 **[FIX]** In `src/main/java/com/example/optoapp/di/DatabaseModule.kt`: delete `provideCurrentUserId` function (L247–253); remove `runBlocking` and `first` imports if unused after deletion.
- [x] 3.4 **[FIX]** In `src/main/java/com/example/optoapp/di/CurrentUserId.kt`: delete file (qualifier annotation no longer has any consumer).
- [x] 3.5 **[PASS]** Run `./gradlew testDebugUnitTest` → GREEN on `userIdResolvedAsyncWithoutBlocking`. Run `./gradlew testDebugUnitTest` full suite → no regressions.

---

## Phase 4: Screen Fixes — Import Correction + LazyColumn Crash (Bugs #2, #1)

- [x] 4.1 **[FIX]** In `src/main/java/com/example/optoapp/ui/screens/CierreCajaScreen.kt`: fix L26 — change `import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` (or equivalent wrong import) to `import androidx.hilt.navigation.compose.hiltViewModel`.
- [x] 4.2 **[FIX]** In the same file: replace `LazyColumn { items(uiState.pagos) { pago -> TransactionItem(pago) } }` with `Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { uiState.pagos.forEach { pago -> TransactionItem(pago) } }`; remove unused `LazyColumn`, `items` imports.

---

## Phase 5: Regression Verification

- [x] 5.1 Run `./gradlew testDebugUnitTest` — full unit suite GREEN with all new tests passing.
- [x] 5.2 Run `./gradlew :optoapp:assembleDebug` — BUILD SUCCESSFUL, no compilation errors.
- [ ] 5.3 Manual smoke: launch app → navigate to CierreCaja → verify screen renders → register payment → close day → re-close same day (assert update, no crash). [Requires device/emulator — out of scope for automated apply]

## Spec references
- REQ-1 → Phase 4 (4.2)
- REQ-2 → Phase 4 (4.1)
- REQ-3 → Phase 3
- REQ-4 → Phase 2 (2.1–2.3)
- REQ-5 → Phase 2 (2.4–2.6)
- REQ-6 → Phase 1

## Files touched
- src/main/java/com/example/optoapp/ui/screens/CierreCajaScreen.kt ✅
- src/main/java/com/example/optoapp/viewmodel/ArqueoCajaViewModel.kt ✅
- src/main/java/com/example/optoapp/viewmodel/CierreCajaViewModel.kt ✅
- src/main/java/com/example/optoapp/data/arqueo/ArqueoCajaDao.kt ✅
- src/main/java/com/example/optoapp/di/DatabaseModule.kt ✅
- src/main/java/com/example/optoapp/di/CurrentUserId.kt (DELETED) ✅
- src/test/java/com/example/optoapp/viewmodel/ArqueoCajaViewModelTest.kt ✅
- src/test/java/com/example/optoapp/viewmodel/CierreCajaViewModelTest.kt ✅
- src/test/java/com/example/optoapp/data/arqueo/ArqueoCajaDaoTest.kt (NEW) ✅
