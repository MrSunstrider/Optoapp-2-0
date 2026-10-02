# Anulaciones backlog fixes (before tracker merge)

## Objective

Close the four backlog items left open by the Anulaciones chain audit, with real root-cause fixes and tests, before pushing the tracker and its chained PRs.

## Problem and why

The read-only audit of `feat/anulaciones-wu6c-verify-fixes` (8d5b4021) found:

1. Double restock: protected only because every caller wraps `restockOnce` in a transaction. On its own, `restockOnce` is check-then-act, adjusts stock before inserting the movimiento, and `MonturaMovimientoDao.insertMovimiento` uses `REPLACE`, so a key collision silently replaces a row. `CancelServicioExtraUseCase` has no real-Room overlap test.
2. Silent cancellations: `ServiciosViewModel.confirmAnular` ignores the `LifecycleOutcome` (an `AlreadyTerminal` result closes the dialog and navigates back without a message; a test locks that in). `DispensacionViewModel.anularDispensacion` treats `AlreadyTerminal("Anulado")` as a silent success.
3. Hard delete sync: `OptoRepository.deleteDispensacion` tombstones and schedules sync, but no test proves it.
4. Claim refund method: `ReclamarDispensacionUseCase` persists a `Reembolso` with whatever `metodoReembolso` it receives, including blank. `reclamoPreview` accepts `Infinity` and shows a refund for any positive excess, while the use case refunds only above `MONEY_EPSILON`; `canConfirmReclamo` ignores the method.

## Scope

- Branch `feat/anulaciones-wu7-backlog-fixes`, stacked on `feat/anulaciones-wu6c-verify-fixes` (the last link of the feature-branch chain).
- Android only (`optoapp/`). No Supabase migration, no OptoWeb.

## Out of scope (recorded, not fixed here)

- Offline double-claim across two devices (needs a server unique index on `reclamo_origen_id`, a G1 schema decision).
- Remote `ON DELETE CASCADE` of pagos when a hard delete reaches the server with pagos that were never downloaded locally.

## Constraints

- Strict TDD: observed RED, then GREEN, then REFACTOR. Runner: `./gradlew :optoapp:testDebugUnitTest` (source: `sdd-init/optoapp`, `strict_tdd: true`).
- JUnit 4 + MockK; Robolectric only for real-Room transaction tests.
- No WHAT comments. Conventional commits, no AI attribution, never `--no-verify`.

## Tasks

- [x] T1 Make restock idempotent on its own: `restockOnce` atomic (no double stock adjustment even outside a caller transaction) and movimiento inserts no longer silently `REPLACE` on the restock path. Real-Room overlap test for `CancelServicioExtraUseCase`. Route: delegated writer.
- [x] T2 Surface lifecycle outcomes: `confirmAnular` and `anularDispensacion` report `AlreadyTerminal` to the user; replace the "silent no-op" test. Route: delegated writer.
- [x] T3 Prove hard-delete sync: repository test that `deleteDispensacion` records the tombstone and schedules sync. Route: delegated writer.
- [x] T4 Validate the claim refund method in the use case, and align `reclamoPreview` / `canConfirmReclamo` with the use case (finite total, `MONEY_EPSILON`, non-blank method when a refund is shown). Route: delegated writer.
- [x] T6 Resolve review findings on T1/T3: a failed restock inside a caller transaction must fail the caller instead of rolling it back silently; an ignored claim must be backed by an existing row; standalone rollback and true concurrency tests for the helper; real `SyncStateTracker` in the hard-delete tests; schedule inventory sync only after the restock commits. Route: delegated writer.
- [ ] T5 Verify: full unit suite, GGA, native RDD on the commits, and a read-only re-audit of the four items. Route: parent.

Route evidence: 4+ files across use cases, DAO, ViewModels, UI and tests, so the writer trigger fires; one delegated writer.

## Acceptance criteria

- Each task has a test that failed before its fix and passes after it.
- `testDebugUnitTest` is green, GGA passes, RDD is approved or its findings are resolved.
- The re-audit reports all four items FIXED.

## Delivery

One work-unit commit per task on `feat/anulaciones-wu7-backlog-fixes`. The chained PR targets `feat/anulaciones-wu6c-verify-fixes`.

## Progress

- Branch created at 8d5b4021.
- T1 done in `5874e3f8` (`fix(inventario): make montura restock atomic and idempotent`).
  - Approach: `restockOnce` runs in its own transaction through an injected `DatabaseTransactionRunner` (joins a caller transaction when present), claims the AJUSTE movimiento first with a new `insertMovimientoIfAbsent` (`IGNORE` on the unique `(referenciaId, tipo, monturaId)` index) and adjusts stock only when the claim wins. A failed adjustment after a won claim throws so the claim rolls back. The shared `insertMovimiento` keeps `REPLACE` because sync download relies on it (`MonturaMovimientoDaoTest.insertMovimiento_duplicateSecondaryUniqueKey_replacesRowWithoutThrowing`). The now-unused `hasMovimiento` / `countByKey` were removed.
  - RED: `DispensacionStockHelperRoomTest.duplicateRestockArrivingMidRestock_adjustsStockOnceAndKeepsFirstMovimiento` failed with `expected:<[Success(true), Success(false)]> but was:<[Success(true)]>` (both the outer and the interleaved duplicate restocked).
  - GREEN: `DispensacionStockHelperRoomTest` (3), `DispensacionStockHelperTest` (15), `CancelServicioExtraTransactionTest` (1), `AnularDispensacionTransactionTest` (16), `ReclamoTransactionTest` (18), `MonturaMovimientoDaoTest` (10), `CancelLedgerUseCasesTest` (12): all passing.
  - `CancelServicioExtraTransactionTest.overlappingInvocations_applyExactlyOnce` passed on first run (characterization: the estado check already serializes overlapping cancels; RED not applicable).

- T2 done in `64cc1b18` (`fix(anulaciones): report already-terminal outcomes to the user`).
  - `ServiciosViewModel` exposes `infoMessage` / `clearInfoMessage`. On `AlreadyTerminal` it closes the dialog, shows "Este servicio ya fue anulado", skips `onComplete` (so `NuevoServicioScreen` no longer pops back before the snackbar shows) and reloads the open form when it is that servicio, so the screen turns read-only. Both `ServiciosExtraScreen` and `NuevoServicioScreen` show the message in their snackbar.
  - `DispensacionViewModel.anularDispensacion` sets `infoMessage = "Esta orden ya fue anulada"` on `AlreadyTerminal("Anulado")` and still completes (closes the dialog and reloads the order); `NuevaDispensacionScreen` already renders `infoMessage`. `Reclamada` keeps its error.
  - RED: `ServiciosViewModelDeleteTest` "informs the user and stays on screen" (`expected:<Este servicio ya fue anulado> but was:<null>`), "reloads the open form so it turns read-only" (`expected:<[Anulado]> but was:<[Pendiente]>`); `DispensacionViewModelAnulacionTest` "informs the user and completes to refresh the order" (`expected:<Esta orden ya fue anulada> but was:<null>`).
  - GREEN: all `ServiciosViewModel*` and `DispensacionViewModel*` suites passing (ServiciosViewModelDeleteTest 13, DispensacionViewModelAnulacionTest 6).

- T3 done in `36ad0e86` (`test(sync): cover dispensacion hard-delete tombstone and sync`).
  - `OptoRepositoryFinanzasTest.deleteDispensacion_removesRowRecordsTombstoneAndSchedulesFinanzasSync` and `deleteDispensacion_tombstoneFailure_rollsBackDeleteAndSkipsSync` (real Room).
  - RED not applicable: both passed on first run because the behavior already exists (characterization tests). No code was broken to fake RED.
  - GREEN: `OptoRepositoryFinanzasTest` 10/10 passing.
  - GGA first rejected the file for pre-existing issues, all fixed in this commit: an `assertNotNull(value) { msg }` that could never fail (now `assertNotNull(msg, value)`), `runBlocking` replaced by `runTest`, a stale "RED phase" class comment, and a WHAT comment (ordering moved into the test name `getGastosOperativos_returnsNewestFechaFirst`).

- T4 done in `e10c2316` (`fix(reclamos): require refund method and align claim preview with ledger`).
  - `ReclamarDispensacionUseCase` computes the refund once, right after the ledger snapshot and before any write, and rejects a blank `metodoReembolso` with `IllegalArgumentException("Selecciona el método de reembolso.")` when a refund exists; `transferCredit` receives that same value (no second computation) and a trimmed method.
  - `MONEY_EPSILON` is now `internal` in the domain and reused by `reclamoPreview` (no duplicated literal). The preview rejects non-finite totals (`Infinity`, `1e309`, `NaN`), shows the refund row and method picker only above `MONEY_EPSILON`, and `canConfirmReclamo` takes the method and requires it whenever a refund is shown. The method field is marked as required/error when blank.
  - RED: `ReclamarDispensacionUseCaseTest.blankRefundMethodWithExcess_isRejectedBeforeAnyWrite` (no exception thrown, writes happened); `ReclamoPreviewTest` "non finite new total" (`Infinity expected null, but was:<Infinity>`), "excess within the ledger epsilon" (`expected:<0.0> but was:<0.0029999…>`), "claim with a shown refund requires a refund method" (confirm allowed with blank method).
  - GREEN: `ReclamarDispensacionUseCaseTest` 7, `ReclamoPreviewTest` 12, `ReclamoTransactionTest` 18, `DispensacionViewModelReclamoTest` 14: all passing.

- T6 restock contract done (`fix(inventario): propagate nested restock failures and verify ignored claims`).
  - `DatabaseTransactionRunner.isInTransaction()` (Room `inTransaction()`). `restockOnce` reads it before entering; a write failure after the claim (failed adjustment, or an `IGNORE`d claim with no row found by the new `MonturaMovimientoDao.findByKey` / `MonturaInventoryCoordinator.findMovimientoByKey`) throws inside the transaction so the claim rolls back, is rethrown when nested and returned as `Result.failure` when standalone. Pre-claim validation failures stay `Result.failure` values (nothing written, no nested failure).
  - Caller audit: the only production caller is `restockOrThrow` (`CancelLedgerUseCases.kt`, used by `CancelServicioExtraUseCase` and `AnularDispensacionUseCase` inside `repository.withTransaction`); it already threw on failure, so the propagated `IllegalStateException` keeps its semantics and now fails the caller instead of a silent rollback.
  - Finding 6 done: `insertMonturaMovimientoIfAbsent` no longer schedules and the restock uses the new `adjustMonturaStockLocal`; `restockOnce` schedules inventory sync once, after its transaction returns `success(true)`. When nested this is still before the caller's commit; the callers already schedule after their own commit.
  - RED: `DispensacionStockHelperRoomTest.adjustFailureInsideCallerTransaction_failsTheCallerAndRollsBackItsWrites` (`caller transaction must fail loudly, got null`); `DispensacionStockHelperTest.restockOnce_ignoredClaimWithoutExistingRow_failsInsteadOfReportingAlreadyRestocked` (`expected:<No se pudo registrar el movimiento de reposición> but was:<null>`).
  - Characterization (passed on first run, no code broken to fake RED): `adjustFailureAfterClaim_standalone_returnsFailureAndRollsBackClaim`, `concurrentRestocksForSameKey_adjustStockExactlyOnce` (two `async(Dispatchers.IO)` restocks, outcomes `{true, false}`, stock +1, one movimiento), `MonturaMovimientoDaoTest.findByKey_matchesOnlyExactReferenciaTipoAndMontura` (query scaffold added before the test). Scheduling and nested-rethrow mock assertions were added after the fix as regression coverage.
  - GREEN: `DispensacionStockHelperRoomTest` 6, `DispensacionStockHelperTest` 17, `MonturaMovimientoDaoTest` 11, `AnularDispensacionTransactionTest` 16, `ReclamoTransactionTest` 18, `CancelServicioExtraTransactionTest` 1, `CancelLedgerUseCasesTest` 12, `MonturaInventoryCoordinator*` 8: all passing.

## Next step

T5 (parent): full suite, GGA, native RDD on the commits, read-only re-audit.
