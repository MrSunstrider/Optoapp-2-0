## Exploration: fix-analisis-financiero-bugs

### Current State

The **Análisis Financiero** screen (`estadisticas_bi` route) is implemented via `AnalisisNegocioScreen.kt` which uses three ViewModels:
- `AnalisisNegocioViewModel` — loads monthly analysis, deudores, and recommendations via use cases
- `GastosViewModel` — manages gastos operativos CRUD, reads from Room via `allGastos` StateFlow
- `AuthViewModel` — provides the user's optica role

A **GastosScreen** exists at the `gastos` route with its own `GastosViewModel` instance. Both ViewModel instances are scoped to their respective `NavBackStackEntry`, so they are DIFFERENT instances but share the same Room database.

The gastos CRUD (add/edit/delete) was implemented in commit `db97c8b`. The recommendation feedback infrastructure (DAO, Entity, UseCase) was built previously but the UI feedback loop (visual response to thumbs up/down) was documented as not yet implemented in `PARTE-B-COMPLETA.md`.

---

### Bug 1: CRUD en gastos del mes — Root Cause Analysis

**Finding: The CRUD code is correctly implemented and SHOULD work if there is data in the Room database. No blocking code defect was found.**

The investigation traced the full data flow:

#### Data Flow: SessionManager → Room → ViewModel → UI

1. **`SessionManager.opticaId`** (`SessionManager.kt:71`): Returns `_opticaIdFlow`, a `MutableStateFlow<String>` initialized in the constructor with `getSecureOpticaId()` (reads from `EncryptedSharedPreferences`, fallback `LEGACY_OPTICA_ID`). **Always has a value — does not hang.**

2. **`GastosViewModel.init`** (`GastosViewModel.kt:47-52`):
   ```kotlin
   sessionManager.opticaId.flatMapLatest { repository.getGastosOperativos(it) }
       .collect { _allGastos.value = autoGenerarSiFalta(gastos) }
   ```
   - `flatMapLatest` subscribes to `repository.getGastosOperativos(opticaId)` once
   - Room Flow **always emits** (empty list if no data)
   - `autoGenerarSiFalta` may upsert recurrent templates, which triggers a Room re-emission that stabilizes after one iteration
   - No infinite loop — the auto-generation check prevents re-creating already-existing entries

3. **Repository → Room** (`OptoRepository.kt:217-218`): `getGastosOperativos` delegates to `GastoOperativoDao.getByOpticaId(opticaId)` which is a standard Room `@Query` returning `Flow<List<GastoOperativoEntity>>`.

4. **Month filter** (`AnalisisNegocioScreen.kt:194-196`):
   ```kotlin
   gastos.filter { it.fecha.month == mesActual.month && it.fecha.year == mesActual.year }
   ```
   - `it.fecha` is `LocalDate`, `mesActual` is `LocalDate`
   - `.month` returns `java.time.Month` enum — **`==` comparison on enum singletons is correct**
   - `.year` returns `Int` — **correct**
   - **The month filter is correct.**

5. **UI rendering** (`AnalisisNegocioScreen.kt:218-240`): Items are rendered with `combinedClickable(onClick = { ... editGasto(g) }, onLongClick = { deleteTarget = g.id })`. The `@OptIn(ExperimentalFoundationApi::class)` is present. **The UI code is correct.**

6. **Save/Delete** (`GastosViewModel.kt:129-170`): Both `save()` and `delete()` write to Room, which triggers Room to re-emit. The `_allGastos` collect block picks up the change. **The save/delete flow is correct.**

#### Verified: No blocking issue found

All paths were traced and are correct:
- ✅ `sessionManager.opticaId` emits immediately (MutableStateFlow with initial value)
- ✅ `flatMapLatest` subscribes and Room Flow emits on table changes
- ✅ Month filter compares `java.time.Month` enum values (equals works for enums)
- ✅ `combinedClickable` has the required `ExperimentalFoundationApi` opt-in
- ✅ Dialog state management (`showDialog`, `deleteTarget`) is correct
- ✅ `autoGenerarSiFalta` stabilizes after one iteration (no infinite recursion)

#### Possible explanations (not code defects):

1. **No gastos in Room DB for this opticaId**: The "Agregar" button always shows, but the list section only renders when `gastosMes.isNotEmpty()`. If the user hasn't added any gastos yet, or gastos were added under a different `opticaId`, the list will be empty.

2. **ViewModel lifecycle (GastosScreen vs AnalisisNegocioScreen)**: Both screens create separate `GastosViewModel` instances via `hiltViewModel()`. They share Room data, so this should NOT cause a discrepancy. However, if the `AnalisisNegocioScreen`'s `GastosViewModel` is created before the user's session is saved (opticaId defaults to `LEGACY_OPTICA_ID`), the Room query may return different results. Once `saveSession()` updates `_opticaIdFlow`, the `flatMapLatest` picks up the new value.

3. **No test coverage for the CRUD integration**: `GastosViewModelTest.kt` only tests error handling for `delete()`. No test verifies that `save()` → Room → `_allGastos` → UI state flow works end-to-end.

---

### Bug 2: Recomendaciones thumbs no responden — Root Cause Analysis

**Finding: The clicks DO work, data IS saved, but the UI provides ZERO visual feedback. The `feedbacksEnviados` state is updated but NEVER consumed by the UI.**

#### Investigation of each potential root cause:

1. **Does `sessionManager.opticaId.first()` hang?** (`AnalisisNegocioViewModel.kt:76`)
   - `opticaId` is `_opticaIdFlow` — a `MutableStateFlow<String>` initialized in the constructor
   - `MutableStateFlow` always has a value — `first()` returns immediately
   - Confirmed in tests: `every { sessionManager.opticaId } returns flowOf(opticaId)`
   - **ROOT CAUSE DISMISSED — does NOT hang**

2. **Does `FeedbackRecomendacionDao.upsert()` work?**
   - `FeedbackRecomendacionDao.kt:9-10`: `@Upsert suspend fun upsert(feedback: FeedbackRecomendacionEntity)`
   - `FeedbackRecomendacionEntity` has `@PrimaryKey val id: String = UUID.randomUUID().toString()` — each upsert creates a new row (auto-generated UUID)
   - **This works correctly** (verified by DAO test `FeedbackRecomendacionDaoTest.kt`)

3. **Does `FeedbackRecomendacionUseCase` work?**
   - `FeedbackRecomendacionUseCase.kt:7-28`: Calls `dao.upsert()` with the correct entity
   - **Verified by use case test** `FeedbackRecomendacionUseCaseTest.kt`

4. **Are exceptions silently eaten?**
   - `AnalisisNegocioViewModel.kt:85-88`: The catch block sets `error = "No se pudo enviar tu valoracion"` and logs the exception. If `error` were displayed by the UI, the user WOULD see it. But the error state is only shown at the top of the screen (`AnalisisNegocioScreen.kt:107-122`) as a full-width card that appears when `uiState.error != null`. This does NOT display as a Snackbar or inline near the recommendation buttons, so the user may not notice it.

5. **`feedbacksEnviados` is NEVER read by the UI** — **THIS IS THE ROOT CAUSE**
   - `AnalisisNegocioViewModel.kt:82-84`: After successful feedback:
     ```kotlin
     _uiState.value = _uiState.value.copy(
         feedbacksEnviados = _uiState.value.feedbacksEnviados + (recomendacionId to fueUtil)
     )
     ```
   - **`feedbacksEnviados` is stored in state but `AnalisisNegocioScreen.kt` never reads it.** It is not passed to `RecomendacionCard`, and there is no conditional rendering that hides or changes the buttons after feedback.
   - The `RecomendacionCard` composable (`AnalisisNegocioScreen.kt:461-547`) always shows both `FilledTonalButton` for "Útil" and "No me sirve" with no state dependency.
   - After the user clicks, the buttons remain visible, unchanged, and clickable → **"clicking does nothing" from the user's perspective.**

#### Summary of Bug 2

| Step | What happens | User sees |
|------|-------------|-----------|
| 1. Clicks "Útil" | `onClick = { onFeedback(true) }` fires | Nothing visually changes |
| 2. `onFeedback(rec.id, true)` | Coroutine launched in viewModelScope | Nothing |
| 3. `sessionManager.opticaId.first()` | Returns immediately (MutableStateFlow) | Nothing |
| 4. `feedbackRecomendacion.marcarUtil(...)` | Calls `dao.upsert(...)` → Room writes | Nothing |
| 5. `feedbacksEnviados` updated | State updated but NOT read by UI | **Nothing — no visual feedback** |
| 6. Error (if any) | `error` set in state | Error banner shown at screen TOP, not near buttons |

The data IS persisted. No crash occurs. But there is zero UI reaction.

---

### Affected Areas

- `optoapp/src/main/java/com/example/optoapp/ui/screens/AnalisisNegocioScreen.kt` — Bug 1: lines 194-196 (month filter), lines 222-225 (`combinedClickable`). Bug 2: lines 160-166 (recommendation list), lines 461-547 (`RecomendacionCard` — no state read for feedbacksEnviados), lines 107-122 (error display — too far from buttons)
- `optoapp/src/main/java/com/example/optoapp/viewmodel/AnalisisNegocioViewModel.kt` — Bug 2: lines 73-90 (`onFeedback` method updates `feedbacksEnviados` but no way to feed it back to UI for conditional rendering)
- `optoapp/src/main/java/com/example/optoapp/viewmodel/GastosViewModel.kt` — Bug 1: potential scoping issue (two instances for two routes), lines 55-68 (`autoGenerarSiFalta` has dead code `val mesInicio`)
- `optoapp/src/main/java/com/example/optoapp/domain/FeedbackRecomendacionUseCase.kt` — No changes needed (works correctly)
- `optoapp/src/main/java/com/example/optoapp/data/feedbackrecomendacion/FeedbackRecomendacionEntity.kt` — No changes needed (works correctly)
- `optoapp/src/main/java/com/example/optoapp/data/feedbackrecomendacion/FeedbackRecomendacionDao.kt` — No changes needed (works correctly)
- `optoapp/src/test/java/com/example/optoapp/viewmodel/GastosViewModelTest.kt` — Only tests delete error handling, no tests for save→UI flow
- `optoapp/src/test/java/com/example/optoapp/viewmodel/AnalisisNegocioViewModelTest.kt` — Tests `onFeedback` via coVerify but doesn't verify UI state changes propagate

---

### Approaches

#### Bug 1: CRUD en gastos del mes

1. **Add integration test coverage, then investigate environment** — write a `GastosViewModelTest` that verifies `save()` → `allGastos` emission → UI state update, then test on a real device to confirm data is actually in Room
   - Pros: Low risk, confirms whether it's a code bug or environment issue
   - Cons: Doesn't fix anything if it IS a code bug; the test might pass locally even if there's a timing/scoping issue on device
   - Effort: Low

2. **Unify GastosViewModel into a shared scope** — change both `AnalisisNegocioScreen` and `GastosScreen` to use the same `GastosViewModel` instance via a shared `hiltViewModel()` scoped to the activity or a nav graph scope
   - Pros: Eliminates any potential misalignment between two ViewModel instances
   - Cons: Changes ViewModel lifecycle semantics; increases risk of keeping stale data in memory
   - Effort: Medium

3. **Verify the `allGastos` flow initialization is robust** — add explicit logging and error handling in the `init` block to catch silent failures, and verify the `opticaId` value is correct for the actual session
   - Pros: Helps diagnose if users actually have data issues
   - Cons: Doesn't fix the root cause if it's environmental
   - Effort: Low

**Recommended approach for Bug 1**: Approach 1 (add test coverage first), then if tests pass and bug persists on device, investigate whether the two ViewModel instances are actually diverging. The code appears correct — the most likely explanation is empty Room data for the user's session.

#### Bug 2: Recomendaciones thumbs no responden

1. **Wire `feedbacksEnviados` into the UI** — pass `feedbacksEnviados` to `RecomendacionCard`, conditionally disable/hide buttons when feedback was already sent for that `rec.id`, and show a confirmation state (e.g., checkmark icon, green outline, or Snackbar)
   - Pros: Fixes the actual root cause — user sees their action had an effect
   - Cons: Requires changes to both screen and card composable
   - Effort: Low-Medium

2. **Remove the `feedbacksEnviados` approach, call `refresh()` after feedback** — instead of tracking `feedbacksEnviados`, reload recommendations after sending feedback so the UI reflects the new state (the recommendation could disappear or change)
   - Pros: Cleaner UX — the recommendation is immediately removed or updated
   - Cons: Recomendaciones may not have logic to skip already-feedbacked items; requires backend changes
   - Effort: Medium

3. **Add a Snackbar on action** — simply show a Snackbar "¡Gracias por tu valoración!" after the feedback is saved, without changing the recommendation list
   - Pros: Minimal code change, immediate user feedback
   - Cons: Doesn't prevent double-feedback; less polished UX
   - Effort: Low

**Recommended approach for Bug 2**: Approach 1 — wire `feedbacksEnviados` into `RecomendacionCard` with visual confirmation. Combined with a Snackbar for the error case. This matches the original design intent (feedback infrastructure was built for this purpose).

---

### Recommendation

**Focus on Bug 2 first**, since its root cause is clearly identified and the fix is well-scoped:

1. Pass `feedbacksEnviados` from `uiState` to `RecomendacionCard`
2. In `RecomendacionCard`, check if `feedbacksEnviados` contains `rec.id`:
   - If yes: show a confirmation state (green checkmark + "Valoración enviada") and disable both buttons
   - If no: show both buttons as they are today (no change to existing behavior)
3. Move the error display from the screen-level error banner to a Snackbar or inline error near the buttons for better UX

**For Bug 1**, add test coverage first (`GastosViewModelTest` for the full save→allGastos→UI flow), then:
- If tests pass but the bug persists on device, the issue is likely environmental (no data in Room for the session's opticaId)
- Add a `GastosScreen` test that verifies gastos added from GastosScreen appear in AnalisisNegocioScreen's gastos list (proving ViewModel instance sharing works through Room)

---

### Risks

- **Risk 1 (Bug 2 false positive)**: If `sessionManager.opticaId.first()` DOES hang under certain conditions (e.g., encrypted prefs migration failure), the root cause would be different from what we found. Add a timeout or fallback to prevent the coroutine from hanging indefinitely.
- **Risk 2 (Bug 1 misdiagnosis)**: Our static analysis found no code defect for Bug 1. If the actual root cause is a Runtime issue (Room migration, encrypted prefs, etc.), adding test coverage alone won't fix it. The fix may require runtime debugging on an affected device.
- **Risk 3 (ViewModel scoping)**: If the two `GastosViewModel` instances are found to diverge in production, unifying them requires careful scope planning to avoid memory leaks or stale data.
- **Risk 4 (Test coverage gap)**: `AnalisisNegocioScreenTest.kt` has 30 tests but they are all trivial (field existence checks, string comparisons). No test actually exercises the feedback UI flow or the gastos CRUD UI flow. Fixing the bugs should include meaningful integration tests.

---

### Ready for Proposal

Yes — proceed to `sdd-propose`. Tell the user:

- **Bug 1 (CRUD gastos)**: All code paths traced and appear correct. The most likely cause is empty Room data for the user's opticaId, or a subtle environmental issue. We recommend adding test coverage first, then investigating on device if the bug persists. The code is correct — `allGastos` Flow, month filter, `combinedClickable`, and save/delete flows all work as designed.

- **Bug 2 (Recomendaciones thumbs)**: Root cause identified. `feedbacksEnviados` is updated in state after a successful feedback, but the UI never reads it. The user clicks → data saves → state updates → UI doesn't react → "clicking does nothing." The fix is to wire `feedbacksEnviados` into `RecomendacionCard` with visual feedback (disable buttons, show confirmation).
