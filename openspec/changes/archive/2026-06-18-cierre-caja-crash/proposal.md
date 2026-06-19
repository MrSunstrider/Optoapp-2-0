# Proposal: Fix CierreCaja Crash and Related Cash-Close Bugs

## Intent

`CierreCajaScreen` crashes (`IllegalStateException`) the moment it opens with any payment data in the database, making the daily cash-close (arqueo) workflow unusable for any active practice. The same screen path also harbors five additional defects that corrupt totals, leak coroutines, risk ANR on cold start, and swallow duplicate-close errors. This change fixes the crash and all related bugs in one pass so the cash-close flow is reliable end to end.

## Scope

### In Scope
- Bug #1 (CRITICAL): replace nested `LazyColumn` inside scrolling `Column` with `Column` + `forEach` in `CierreCajaScreen.kt`.
- Bug #2 (HIGH): correct `hiltViewModel` import to `androidx.hilt.navigation.compose`.
- Bug #3 (HIGH): remove main-thread `runBlocking` DataStore read; resolve user email async in ViewModel `init {}`.
- Bug #4 (HIGH): normalize payment-method key lookup so title-case `metodoPago` values map correctly.
- Bug #5 (MEDIUM): cancel prior `observeArqueoForDate` collector before re-subscribing (`flatMapLatest`/`launchIn` + `Job`).
- Bug #6 (MEDIUM): make `insertArqueo` idempotent (`OnConflictStrategy.REPLACE`) or surface an error state.

### Out of Scope
- No Room schema migration or DB version bump.
- No UI/visual redesign of the cash-close screen.
- No changes to the Next.js web app or shared spec model.
- No new arqueo features (reporting, export, multi-currency).

## Capabilities

### New Capabilities
None

### Modified Capabilities
None — this is a defect-fix change with no spec-level requirement changes.

## Approach

Treat all six defects as one cohesive bugfix on the cash-close vertical slice. Start with the crash (Bug #1) to restore basic usability, then correctness (Bugs #2–#4), then lifecycle/robustness (Bugs #5–#6). Each fix is local to its file; no cross-cutting refactor. Inject `SessionManager` into the ViewModel to remove the DI-time blocking read, keeping the rest of the DI graph intact.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/screens/CierreCajaScreen.kt` | Modified | Fix crash (nested scroll) + Hilt import |
| `di/DatabaseModule.kt` | Modified | Remove main-thread `runBlocking` DataStore read |
| `viewmodel/ArqueoCajaViewModel.kt` | Modified | Normalize method keys; surface insert errors |
| `viewmodel/CierreCajaViewModel.kt` | Modified | Cancel/replace arqueo collector; inject email async |
| `data/arqueo/ArqueoCajaDao.kt` | Modified | Idempotent insert (REPLACE) |

## Business Rules

- Arqueo must be **idempotent**: re-closing the same date updates the existing record, never throws.
- Payment totals must be computed using the **exact title-case method names** stored on `Pago.metodoPago`.
- No silent failures: a duplicate-close or insert error must update record or surface to the user, never be swallowed.

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `REPLACE` overwrites a prior same-date arqueo unintentionally | Med | Confirm same-date re-close is the intended idempotent behavior; otherwise wrap in try/catch + error state |
| Async email init introduces a transient empty state | Low | Guard UI on email-loaded state; default to loading |
| `flatMapLatest` change alters emission timing | Low | Verify single active collector via test |

## Rollback Plan

Each fix is an isolated edit to one of five files. Revert the change commit (or per-file edits) to restore prior behavior. No migrations or persisted state changes, so rollback is a pure code revert with no data cleanup.

## Dependencies

- Hilt navigation-compose artifact must be present for the corrected import (Bug #2).

## Success Criteria

- [ ] CierreCaja opens without crashing for any non-empty payment dataset.
- [ ] Arqueo totals per method (Efectivo/Tarjeta/Transferencia/Móvil) are correct, never 0.0 when payments exist.
- [ ] Re-closing the same date updates the record instead of failing silently.
- [ ] No main-thread blocking on cold start; no duplicated arqueo collectors.
