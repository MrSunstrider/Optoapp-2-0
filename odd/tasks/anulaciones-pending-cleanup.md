# Anulaciones pending cleanup

## Objective

Close the remaining technical debt from the Anulaciones feature (context: `odd/tasks/anulaciones-server-guards.md`): one duplicated constant, one overlapping test, one silent residual-credit block, and stale notes after a claim OT renumber.

## Problem

1. `CierreCajaViewModel` declared its own `TIPO_REVERSO` although the single domain constant exists in `CancelLedgerUseCases.kt`.
2. `InformacionFinancieraViewModelTest` asserted the ordinary zero total twice (`save rejects montoTotal zero or less` and the newer ordinary-zero test), and the negative total for an ordinary dispensacion was never asserted.
3. `DiscardLosingClaimUseCase.transferResidualCredit` silently waits when the pagos download marker is not set because a pago is quarantined or pending local deletion; the user never learns the credit transfer is pending.
4. `renumberCollidingClaimOts` renumbers a claim replacement's OT but free-text notes that embed the old replacement OT kept the stale number.

## Scope

- Branch `fix/anulaciones-pending-cleanup` from `main` (`d1d60f7c`, release 1.16.16). One work-unit commit per task, tests with the behavior.
- Android client only. No `optoapp-web`, no Supabase migrations, no production database, no `openspec/changes/_jd-android-test-cluster/`.

## Constraints

- TDD: strict. Source: Engram `sdd-init/optoapp` (`strict_tdd: true`). Runner: `./gradlew :optoapp:testDebugUnitTest`.
- JUnit 4 + MockK + `runTest` (never `runBlocking`); real Room only for DAO/transaction tests.
- No WHAT comments; English artifacts (user-facing strings follow existing Spanish UI strings); conventional commits; no AI attribution; never `--no-verify`; resolve every GGA observation.
- Route: inline for all four tasks (each touches a bounded set of files already read in full: route evidence = 3-6 files per task, mapping done by direct reads of the claim flow).

## Tasks

- [x] T1 `CierreCajaViewModel` uses the domain `TIPO_REVERSO`; duplicate removed. Refactor, no RED; guarded by the existing CierreCaja tests.
- [x] T2 Consolidate `InformacionFinancieraViewModelTest` zero/negative total tests so each behavior is asserted once.
- [x] T3 User-visible notice (`reclamo_descartado`, no `quarantine:` prefix) when a `reclamo_credito` marker is pending and the pagos download was incomplete because of a local quarantine/pending deletion; idempotent per original; cleared when the transfer runs.
- [x] T4 `renumberCollidingClaimOts` also rewrites the notes of the replacement's own rows that embed the old replacement OT, in the same transaction; the original's OT is untouched.

## Acceptance criteria

- Single `TIPO_REVERSO` definition in the codebase.
- Each zero/negative-total behavior asserted exactly once; no behavior change.
- Quarantine/pending deletion + pending credit marker -> exactly one Spanish notice per original; none for network failures; removed after the transfer.
- After a renumber, only the replacement's rows with the exact old OT token carry the new OT; the original's rows are unchanged.
- Full `./gradlew :optoapp:testDebugUnitTest --rerun` green; GGA clean.

## Delivery

Branch pushed, PR to `main` titled `fix(anulaciones): close pending technical debt`. Not merged by the agent. Forecast: well under 400 authored changed lines of production code (tests excluded); single PR.

## Progress

- Branch created from `main` at `d1d60f7c`.
- T1 (`32f510a0`): the duplicate private const was removed and `com.example.optoapp.domain.TIPO_REVERSO` imported (same module, `internal`). Refactor; no RED by design. Guarded by `*CierreCaja*` tests: BUILD SUCCESSFUL. GGA passed.
- T2 (`86bbefe2`): removed `save rejects montoTotal zero or less` (it only asserted the ordinary zero, already covered with the stronger "writes nothing" assertions) and added `save rejects a negative total for an ordinary dispensacion and writes nothing` (the "or less" half that was never asserted for ordinary orders; characterization, passed first). Coverage kept: ordinary zero, ordinary negative, claim zero accepted, claim negative rejected. `InformacionFinancieraViewModelTest` green. GGA passed.
- T3 (`e26441ef`): `DownloadSyncCoordinator.persistRemoteRows` marks `download_<entity>_blocked`/`batch` only when a row was skipped for a local quarantine or pending deletion (cleared when the fetch starts, never set for network or persist failures). `DiscardLosingClaimUseCase.transferResidualCredit` posts one `reclamo_descartado` notice per pending `reclamo_credito` original (entity id `<origenId>:credito_pendiente`, message "El crédito del reclamo de la OT X queda pendiente hasta resolver los pagos en espera.", upsert so it is replaced, not duplicated) when the pagos batch is incomplete and blocked locally; it is cleared after the transfer is attempted on a complete download. RED (5 failures, expected reasons): `pagosHeldByALocalQuarantine_tellTheUserTheCreditIsPendingOnce`, `pendingCreditNotice_isClearedWhenTheTransferRuns` (real Room), `pagoSkippedForALocalQuarantine_marksTheBatchBlockedLocally`, `pagoSkippedForAPendingLocalDeletion_marksTheBatchBlockedLocally`, `pagosFetch_clearsAStaleLocalBlockBeforeDownloading`. Characterization (passed first): network failure posts no notice, no pending credit posts no notice, persist failure / full success do not mark blocked, notice cleared when the original is gone. GREEN: `DiscardLosingClaimUseCaseTest`, `DownloadSyncCoordinatorTest`, `SyncFinanzasUseCase*` all pass. GGA passed, no observations.
- T4 (`868e89b3`): finding: the claim flow embeds the replacement OT in exactly one place, the frame sale movimiento note ("Venta por reclamo de OT <replacement.ot>", `referenciaId == replacement.id`). Every pago note the claim flow writes ("Crédito/Ajuste de crédito/Reembolso por reclamo de OT", discard transfer "Crédito por reclamo de OT") embeds the ORIGINAL OT, which a renumber never changes, so pagos are deliberately not rewritten. `renumberCollidingClaimOts` now runs the OT update and `rewriteClaimSaleNotes` in one `runInTransaction`; only movimientos of that replacement (`id` or `id:*`) are touched, only the exact old OT token (`replaceOtToken`, boundary-aware so `-R1` never matches inside `-R12` or `X...-R1`), with a fresh `updatedAt` so a sale already uploaded under the old OT is upserted again by the next inventario sync. RED (4 failures, expected reasons): 2 `ReplaceOtTokenTest` against a no-op skeleton, `renumbering a claim replacement rewrites the old OT only in its own movimiento notes`, `the OT update and the movimiento note rewrite share one transaction`. Characterization (passed first): keeping the OT rewrites nothing, token helper leaves the original OT / longer tokens alone. GREEN: `UploadSyncCoordinatorTest`, `ReplaceOtTokenTest`, `SyncFinanzasUseCase*`. GGA first flagged pre-existing issues in `UploadSyncCoordinatorTest` (two tautological `Instant` tests that exercised no production code, `// expected` comments, an `acceptable` catch that could pass without reaching the merge, repeated 8-argument anonymous coordinators, positional args, fully qualified types, duplicated retry stubs); all resolved in the same commit (tautological tests deleted, merge-after-upsert test rewritten as a failure and a success case, one `TestUploadCoordinator` base class, `stubRetryPassThrough` reused). Final GGA pass with no observations.
- Full suite `./gradlew :optoapp:testDebugUnitTest --rerun` at `868e89b3`: BUILD SUCCESSFUL, 2698 tests, 0 failures, 0 errors, 3 skipped.
- Pending: none.

## Next step

Open the PR to `main`; the user decides the merge.
