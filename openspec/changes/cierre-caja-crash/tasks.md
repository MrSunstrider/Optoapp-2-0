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

- [ ] 1.1 **[FAIL]** Add unit/integration test `ArqueoCajaDaoReplaceTest` in `src/androidTest/java/com/example/optoapp/data/ArqueoCajaDaoReplaceTest.kt`: insert arqueo, insert again with same PK, assert row count = 1 and values updated. Run `./gradlew connectedAndroidTest` → expect RED (ABORT throws exception).
- [ ] 1.2 **[FIX]** In `src/main/java/com/example/optoapp/data/arqueo/ArqueoCajaDao.kt`: change `OnConflictStrategy.ABORT` → `OnConflictStrategy.REPLACE` on `insertArqueo`.
- [ ] 1.3 **[PASS]** Run `./gradlew connectedAndroidTest` → GREEN on `ArqueoCajaDaoReplaceTest`.

---

## Phase 2: ViewModel Unit Fixes — Key Casing + Coroutine Leak (Bugs #4, #5)

- [ ] 2.1 **[FAIL]** In `src/test/java/com/example/optoapp/viewmodel/ArqueoCajaViewModelTest.kt`: add test `totalesUseTitleCaseKeys` — seed fake repo with `Pago(metodoPago="Efectivo", monto=500.0)`, call `cerrarDia()`, assert `totales["Efectivo"] == 500.0` and `totales["efectivo"] == null`. Run `./gradlew testDebugUnitTest` → RED.
- [ ] 2.2 **[FIX]** In `src/main/java/com/example/optoapp/viewmodel/ArqueoCajaViewModel.kt`: change `cerrarDia()` map lookup keys from `"efectivo"`, `"tarjeta"`, `"transferencia"`, `"movil"` to `"Efectivo"`, `"Tarjeta"`, `"Transferencia"`, `"Móvil"`.
- [ ] 2.3 **[PASS]** Run `./gradlew testDebugUnitTest` → GREEN on `totalesUseTitleCaseKeys`.
- [ ] 2.4 **[FAIL]** In `src/test/java/com/example/optoapp/viewmodel/CierreCajaViewModelTest.kt`: add test `observeArqueoForDateCancelsPreviousCollector` — use `StandardTestDispatcher`, call `observeArqueoForDate(date1)` then `observeArqueoForDate(date2)`, emit from date1 flow after switch, assert date1 emissions are NOT received. Run `./gradlew testDebugUnitTest` → RED.
- [ ] 2.5 **[FIX]** In `src/main/java/com/example/optoapp/viewmodel/CierreCajaViewModel.kt`: replace manual Job-per-call pattern in `observeArqueoForDate` with a `MutableStateFlow<Pair<LocalDate, String>?>` driven via `flatMapLatest { getArqueoByFecha(...) }.onEach { ... }.launchIn(viewModelScope)` initialized in `init {}`.
- [ ] 2.6 **[PASS]** Run `./gradlew testDebugUnitTest` → GREEN on `observeArqueoForDateCancelsPreviousCollector`.

---

## Phase 3: DI Restructure — Remove runBlocking from DatabaseModule (Bug #3)

- [ ] 3.1 **[FAIL]** In `src/test/java/com/example/optoapp/viewmodel/ArqueoCajaViewModelTest.kt`: add test `userIdResolvedAsyncWithoutBlocking` — construct `ArqueoCajaViewModel` with `FakeSessionManager` returning `"user@test.com"` from `userEmail` flow; advance `TestCoroutineScope`; assert `viewModel.currentUserId == "user@test.com"` after coroutine runs. Run `./gradlew testDebugUnitTest` → RED (constructor still accepts `@CurrentUserId String`).
- [ ] 3.2 **[FIX]** In `src/main/java/com/example/optoapp/viewmodel/ArqueoCajaViewModel.kt`: remove `@CurrentUserId` constructor param; inject `SessionManager` instead; add `private var currentUserId: String = ""`; in `init { viewModelScope.launch { currentUserId = sessionManager.userEmail.first() } }`.
- [ ] 3.3 **[FIX]** In `src/main/java/com/example/optoapp/di/DatabaseModule.kt`: delete `provideCurrentUserId` function (L247–253); remove `runBlocking` and `first` imports if unused after deletion.
- [ ] 3.4 **[FIX]** In `src/main/java/com/example/optoapp/di/CurrentUserId.kt`: delete file (qualifier annotation no longer has any consumer).
- [ ] 3.5 **[PASS]** Run `./gradlew testDebugUnitTest` → GREEN on `userIdResolvedAsyncWithoutBlocking`. Run `./gradlew testDebugUnitTest` full suite → no regressions.

---

## Phase 4: Screen Fixes — Import Correction + LazyColumn Crash (Bugs #2, #1)

- [ ] 4.1 **[FAIL]** Add Compose UI test `CierreCajaScreenRendersWithoutCrash` in `src/androidTest/java/com/example/optoapp/ui/CierreCajaScreenTest.kt`: set up a NavHost with `CierreCajaScreen`, provide ≥1 pago in fake state, `composeTestRule.waitForIdle()`, assert no `IllegalStateException`. Run `./gradlew connectedAndroidTest` → RED (LazyColumn inside verticalScroll crashes).
- [ ] 4.2 **[FIX]** In `src/main/java/com/example/optoapp/ui/screens/CierreCajaScreen.kt`: fix L26 — change `import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` (or equivalent wrong import) to `import androidx.hilt.navigation.compose.hiltViewModel`.
- [ ] 4.3 **[FIX]** In the same file: replace `LazyColumn { items(uiState.pagos) { pago -> TransactionItem(pago) } }` with `Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { uiState.pagos.forEach { pago -> TransactionItem(pago) } }`; remove unused `LazyColumn`, `items` imports.
- [ ] 4.4 **[PASS]** Run `./gradlew connectedAndroidTest` → GREEN on `CierreCajaScreenRendersWithoutCrash`.

---

## Phase 5: Regression Verification

- [ ] 5.1 Run `./gradlew testDebugUnitTest` — full unit suite GREEN with all new tests passing.
- [ ] 5.2 Run `./gradlew connectedAndroidTest` — all instrumented tests GREEN including `ArqueoCajaDaoReplaceTest` and `CierreCajaScreenRendersWithoutCrash`.
- [ ] 5.3 Manual smoke: launch app → navigate to CierreCaja → verify screen renders → register payment → close day → re-close same day (assert update, no crash).
