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
- [ ] T2 Surface lifecycle outcomes: `confirmAnular` and `anularDispensacion` report `AlreadyTerminal` to the user; replace the "silent no-op" test. Route: delegated writer.
- [ ] T3 Prove hard-delete sync: repository test that `deleteDispensacion` records the tombstone and schedules sync. Route: delegated writer.
- [ ] T4 Validate the claim refund method in the use case, and align `reclamoPreview` / `canConfirmReclamo` with the use case (finite total, `MONEY_EPSILON`, non-blank method when a refund is shown). Route: delegated writer.
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
- T1 done (commit: see `git log`, subject `fix(inventario): make montura restock atomic and idempotent`).
  - Approach: `restockOnce` runs in its own transaction through an injected `DatabaseTransactionRunner` (joins a caller transaction when present), claims the AJUSTE movimiento first with a new `insertMovimientoIfAbsent` (`IGNORE` on the unique `(referenciaId, tipo, monturaId)` index) and adjusts stock only when the claim wins. A failed adjustment after a won claim throws so the claim rolls back. The shared `insertMovimiento` keeps `REPLACE` because sync download relies on it (`MonturaMovimientoDaoTest.insertMovimiento_duplicateSecondaryUniqueKey_replacesRowWithoutThrowing`). The now-unused `hasMovimiento` / `countByKey` were removed.
  - RED: `DispensacionStockHelperRoomTest.duplicateRestockArrivingMidRestock_adjustsStockOnceAndKeepsFirstMovimiento` failed with `expected:<[Success(true), Success(false)]> but was:<[Success(true)]>` (both the outer and the interleaved duplicate restocked).
  - GREEN: `DispensacionStockHelperRoomTest` (3), `DispensacionStockHelperTest` (15), `CancelServicioExtraTransactionTest` (1), `AnularDispensacionTransactionTest` (16), `ReclamoTransactionTest` (18), `MonturaMovimientoDaoTest` (10), `CancelLedgerUseCasesTest` (12): all passing.
  - `CancelServicioExtraTransactionTest.overlappingInvocations_applyExactlyOnce` passed on first run (characterization: the estado check already serializes overlapping cancels; RED not applicable).

## Next step

T2.
