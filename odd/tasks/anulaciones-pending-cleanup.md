# Anulaciones pending cleanup

## Objective

Close the remaining technical debt from the Anulaciones feature (context: `odd/tasks/anulaciones-server-guards.md`): one duplicated constant, one overlapping test, one silent residual-credit block, and stale notes after a claim OT renumber.

## Problem

1. `CierreCajaViewModel` declares its own `TIPO_REVERSO` although the single domain constant exists in `CancelLedgerUseCases.kt`.
2. `InformacionFinancieraViewModelTest` asserts the ordinary zero total twice (`save rejects montoTotal zero or less` and the newer ordinary-zero test).
3. `DiscardLosingClaimUseCase.transferResidualCredit` silently waits when the pagos download marker is not set because a pago is quarantined or pending local deletion; the user never learns the credit transfer is pending.
4. `renumberCollidingClaimOts` renumbers a claim replacement's OT but free-text notes that embed the old replacement OT keep the stale number.

## Scope

- Branch `fix/anulaciones-pending-cleanup` from `main` (`d1d60f7c`, release 1.16.16). One work-unit commit per task, tests with the behavior.
- Android client only. No `optoapp-web`, no Supabase migrations, no production database, no `openspec/changes/_jd-android-test-cluster/`.

## Constraints

- TDD: strict. Source: Engram `sdd-init/optoapp` (`strict_tdd: true`). Runner: `./gradlew :optoapp:testDebugUnitTest`.
- JUnit 4 + MockK + `runTest` (never `runBlocking`); real Room only for DAO/transaction tests.
- No WHAT comments; English artifacts (user-facing strings follow existing Spanish UI strings); conventional commits; no AI attribution; never `--no-verify`; resolve every GGA observation.
- Route: inline for T1/T2 (single-file or mechanical); T3/T4 inline after reading the two coordinator files (exploration bounded to the claim flow).

## Tasks

- [ ] T1 `CierreCajaViewModel` uses the domain `TIPO_REVERSO`; duplicate removed. Refactor, no RED; guarded by existing CierreCaja tests.
- [ ] T2 Consolidate `InformacionFinancieraViewModelTest` zero/negative total tests so each behavior is asserted once.
- [ ] T3 User-visible notice (`reclamo_descartado`, no `quarantine:` prefix) when a `reclamo_credito` marker is pending and the pagos download was incomplete because of a local quarantine/pending deletion; idempotent per original; cleared when the transfer runs.
- [ ] T4 `renumberCollidingClaimOts` also rewrites notes of the replacement's own rows that embed the old replacement OT, in the same transaction; original's OT untouched.

## Acceptance criteria

- Single `TIPO_REVERSO` definition in the codebase.
- Each zero/negative-total behavior asserted exactly once; no behavior change.
- Quarantine/pending deletion + pending credit marker -> exactly one Spanish notice per original; none for network failures; removed after the transfer.
- After a renumber, only the replacement's rows with the exact old OT token carry the new OT; the original's rows are unchanged.
- Full `./gradlew :optoapp:testDebugUnitTest --rerun` green; GGA clean.

## Delivery

Branch pushed, PR to `main` titled `fix(anulaciones): close pending technical debt`. Not merged by the agent.

## Progress

- Branch created from `main` at `d1d60f7c`.
