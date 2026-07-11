# Change Proposal: fix-analisis-financiero-bugs

## Intent

Fix two bugs in the "Análisis Financiero" screen (`AnalisisNegocioScreen`) that degrade the user experience: (1) gastos del mes CRUD items not appearing, and (2) recommendation thumbs up/down buttons providing zero visual feedback.

This is a **bugfix**, not a new feature. Scope is tight, changes are small, and the primary fix (Bug 2) has a clearly identified root cause.

---

## Scope

### In Scope

- **Bug 2 (PRIORITY)**: `feedbacksEnviados` is updated in `AnalisisNegocioViewModel` state after a successful recommendation feedback, but `RecomendacionCard` never reads it → user clicks and sees no reaction. Wire `feedbacksEnviados` into the UI to show confirmation state.
- **Bug 1**: Gastos del mes CRUD — add integration test coverage for `GastosViewModel` (save → `allGastos` emission → UI state) since the exploration found no code defect. The bug is likely environmental (empty Room data), so test coverage helps confirm correctness and catch regressions.

### Out of Scope

- No Supabase schema or RLS changes (neither bug affects the server)
- No Room entity, DAO, or migration changes
- No sync coordinator or edge function changes
- No new UI components beyond visual state within existing `RecomendacionCard`
- No `GastosViewModel` lifecycle or scoping refactors (exploration found no evidence of divergent ViewModel instances causing the empty-list issue)

---

## Approach

### Bug 2 — Wire `feedbacksEnviados` into RecomendacionCard

**Root cause** (confirmed by exploration): `AnalisisNegocioViewModel.onFeedback()` successfully saves data via `FeedbackRecomendacionUseCase` and updates `_uiState.value.feedbacksEnviados`, but `RecomendacionCard` never reads that state. The buttons remain identical and clickable → user perceives "clicking does nothing."

**Fix** (all changes in `AnalisisNegcioScreen.kt`):

1. **Pass `feedbacksEnviados` to `RecomendacionCard`** — add `feedbacksEnviados: Map<String, Boolean>` parameter to the composable function signature.
2. **Check if feedback was already sent** — if `feedbacksEnviados.contains(rec.id)`:
   - Disable both buttons (material `enabled = false` or use disabled styling)
   - Show a confirmation indicator: green checkmark icon + "Valoración enviada" text replacing the button row, or an inline confirmation message within the card
   - The `fueUtil` boolean from the map determines which thumb icon was clicked (optional for UX — the confirmation itself is the key feedback)
3. **Error feedback**: The existing error banner at the screen top is too far from the recommendation buttons. Keep the global error state but also consider adding a local Snackbar or inline error near the specific recommendation card that failed.

**Design decision**: Show a confirmation message (text + checkmark icon) **and** disable/re-style the buttons after feedback is sent. This is the simplest effective approach — the user sees immediate visual confirmation that their tap registered, and cannot accidentally double-submit.

**No ViewModel changes needed** — `feedbacksEnviados` already exists in `AnalisisNegocioUiState` and is correctly populated.

### Bug 1 — Add GastosViewModel Test Coverage

**Finding** (from exploration): The CRUD code is correct — `combinedClickable`, month filter, Room Flow, and save/delete flows all work as designed. The most likely cause is empty Room data for the user's `opticaId`.

**Fix**:

1. **Add `GastosViewModelTest` coverage for the save flow**:
   - Test: `save(nuevoGasto)` emits the new expense through `allGastos` and `uiState` shows it
   - Test: `delete(gasto)` removes the expense and `allGastos` reflects the change
   - Test: error handling in `save()` sets `uiState.error` (currently only `delete()` error is tested)

2. **Add diagnostic logging** in `GastosViewModel.init` to log the `opticaId` value and the count of loaded gastos at startup. This helps future investigation without spamming production logs.

**No code changes to the CRUD logic itself** — the code is correct. The tests prove correctness and catch regressions.

---

## Capabilities

This change touches two existing capability areas from `openspec/specs/`:

| Capability | Spec File | Relation |
|-----------|-----------|----------|
| `analisis-negocio` | `openspec/specs/analisis-negocio/spec.md` | Defines `GastoOperativoEntity`, `GastoOperativoDao`, Room migration, ViewModel state (`GastosUiState`, `AnalisisNegocioUiState`). Bug 1 test coverage extends `GastosViewModel` testing within this capability. |
| `recomendaciones` | `openspec/specs/recomendaciones/spec.md` | Defines `Recomendacion` domain model, `FeedbackRecomendacionUseCase`, `FeedbackRecomendacionEntity`/DAO. Bug 2 UI fix lives in `RecomendacionCard` rendering within this capability. |

No new capability is needed — both bugs are UI integration issues within existing capabilities.

---

## Delta Specifications Needed

### Bug 2 Delta Specs (in `openspec/specs/recomendaciones/`)

- **REQ-FEEDBACK-UI-1**: After a user clicks "Útil" or "No me sirve" on a recommendation card, the UI MUST provide immediate visual feedback confirming the action was registered.
- **REQ-FEEDBACK-UI-2**: A recommendation card that has already received feedback MUST disable its feedback buttons to prevent double-submission.
- **REQ-FEEDBACK-UI-3**: The send-feedback error path MUST produce a visible error indication near the recommendation card (Snackbar or inline text), not only at the screen-level error banner.

### Bug 1 Delta Specs (in `openspec/specs/analisis-negocio/`)

- **REQ-GASTOS-TEST-1**: `GastosViewModel` SHALL emit the newly saved expense through `allGastos` after `save()` completes.
- **REQ-GASTOS-TEST-2**: `GastosViewModel` SHALL remove the deleted expense from `allGastos` after `delete()` completes.
- **REQ-GASTOS-TEST-3**: `GastosViewModel.save()` SHALL set `uiState.error` when the repository write fails.

---

## Risks

| Risk | Mitigation |
|------|------------|
| Bug 1 is purely environmental (no data in Room) — test coverage alone doesn't fix the user's experience | Tests prove the code works. Supplement with diagnostic logging in `GastosViewModel.init`. If the bug persists on device, the investigation shifts to data population (requires separate change). |
| The two `GastosViewModel` instances (one per route) drift in production | Exploration found the code is correct — Room is shared, so data is consistent. Only intervene if tests pass but device behavior diverges. |
| Bug 2 fix (button disable state) might confuse users who want to change their feedback | Minimum viable fix: disabled buttons + confirmation text. Future enhancement: allow toggling feedback (not in scope). |

---

## Supabase Schema / RLS Impact

**None.** Neither bug requires changes to:
- PostgreSQL migrations or seed data
- Row-Level Security policies
- Edge Functions or RPCs
- Auth configuration

Both fixes are entirely in the Android client layer (Compose UI + ViewModel tests).

---

## Rollback Plan

### Bug 2 (UI feedback for recomendaciones)

Rollback is trivial: revert the changes in `AnalisisNegocioScreen.kt` that:
1. Add `feedbacksEnviados` parameter to `RecomendacionCard`
2. Add conditional rendering (confirmation state) inside `RecomendacionCard`

No data is affected — the ViewModel state `feedbacksEnviados` continues to work. Only the visual feedback disappears, returning to the pre-fix behavior.

**Reversion scope**: Single file (`AnalisisNegocioScreen.kt`) — git revert of 2 function signatures and ~20 lines of conditional rendering.

### Bug 1 (Test coverage for GastosViewModel)

Rollback is revert-only: remove the new test methods from `GastosViewModelTest.kt` and the diagnostic log lines from `GastosViewModel.kt`.

**Reversion scope**: Two files — test methods removed, 1-2 log lines removed. No production behavior changes.

---

## Proposal Decisions

1. **Bug 2 fix priority**: HIGH. Root cause is clear. Fix is small (~20 lines).
2. **Bug 1 scope**: LOW. Code is correct. Add tests + diagnostics only.
3. **No ViewModel unification for Bug 1**: Not justified by evidence. The two `GastosViewModel` instances share Room correctly.
4. **No Snackbar for Bug 2 error path**: The global error banner at screen top already shows feedback errors. Adding a Snackbar is a nice-to-have but increases scope. Keep it simple — wire `feedbacksEnviados` into the card.
5. **Confirm button behavior for feedback**: Disable buttons + show inline confirmation text. This prevents double-submission and gives clear visual feedback.
