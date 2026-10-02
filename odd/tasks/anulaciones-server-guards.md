# Anulaciones server guards (offline double claim, remote hard delete)

## Objective

Make the server the authority for two multi-device races the Android client cannot see offline, and make the client resolve each server refusal cleanly instead of stalling sync or losing data.

## Problem and why

1. **Offline double claim.** Two devices can claim the same delivered order offline. Each creates a replacement (`reclamo_origen_id = original.id`), reverses the original's credits and transfers them. Today:
   - `reclamo_origen_id` has no unique index in production, so nothing on the server stops a second replacement.
   - Both devices usually pick the same OT `<base>-R1`. Upload reconciles by OT (`UploadSyncCoordinator`, `remoteIdByOt`), adopts the winner's id and overwrites the winner's replacement, so a server index alone would never fire.
   - A 23505 is not isolatable (`FinanzasUploadValidator`), so it stalls the whole finanzas module on every sync. The loser's `SALIDA_VENTA` movimientos still upload through inventario.
2. **Remote hard delete with pagos never downloaded.** `EliminarDispensacionUseCase` checks only local pagos and movimientos. In production, `pagos.dispensacion_id`, `dispensacion_items` and `regalos_dispensacion` are `ON DELETE CASCADE`, so a tombstone that reaches the server deletes remote pagos the device never saw. A refused remote delete (if we add a guard) is retried forever and its tombstone hides the row from download.

Production check (read-only, 2026-10-01): 0 rows with `reclamo_origen_id`, so a unique index applies cleanly. `dispensaciones` has no delete trigger; `pacientes` has `trg_guard_pacientes_delete`, and `dispensaciones_paciente_id_fkey` is `ON DELETE CASCADE`.

## Decisions (recommended options, user preference: take the recommended option)

- **Claim winner:** the first claim to reach the server wins. The losing device discards its whole local claim and downloads the winner's; the user sees one informational message.
- **Hard delete guard:** the server refuses a *direct* delete of a dispensacion that has pagos or montura movimientos. Cascades from a paciente delete stay allowed (paciente deletes keep their existing guardrails). On refusal, the client drops the tombstone so the next download restores the order with its pagos, and tells the user.

## Scope

- Branch `feat/anulaciones-wu8-server-guards`, stacked on `feat/anulaciones-wu7-backlog-fixes`.
- One new Supabase migration (sorted after `20261001042950`), written and linted locally only. Applying it to production is G1 and needs GGA plus explicit user authorization.
- Android client changes in `optoapp/`. No OptoWeb.

## Constraints

- Strict TDD: observed RED, then GREEN, then REFACTOR. Runner `./gradlew :optoapp:testDebugUnitTest` (source `sdd-init/optoapp`, `strict_tdd: true`).
- Migration conventions: `supabase/migrations/README.md` (idempotent, transaction-safe, banner header, `SET search_path`, `REVOKE EXECUTE`).
- No WHAT comments; conventional commits; no AI attribution; never `--no-verify`.

## Tasks

- [ ] T1 Migration: partial unique index on `dispensaciones(reclamo_origen_id)`; `BEFORE DELETE` guard on `dispensaciones` refusing direct deletes with pagos or montura movimientos (mirroring the local trace check), allowing paciente cascades; stable error code/token. Route: delegated writer. Status: committed; runtime proof (lint + SQL test) pending, no Docker locally.
- [x] T2 Claim conflict on upload: detect a local replacement whose original already has a different remote replacement (OT reconciliation and the new index), quarantine it instead of adopting the remote id; make the claim-index and `pagos_reversa_pago_id_uidx` violations isolatable; keep the loser's movimientos from uploading. Route: delegated writer.
- [ ] T3 Discard the losing claim locally in one transaction (replacement, its pagos and items, unsynced Reversos on the original, consumed frames restocked idempotently, original estado restored), then let download bring the winner; one informational message. Route: delegated writer.
- [ ] T4 Refused remote delete: `DeletionSyncHelper` recognises the guard refusal, clears the tombstone so download restores the order, and records a user-visible message without the `quarantine:` prefix; transient errors keep today's retry. Route: delegated writer.
- [ ] T5 Verify: full suite, `supabase db lint` on the migration, GGA, native RDD, read-only re-audit. Route: parent.

Route evidence: the sync map spans 10+ files (upload, validator, merge, download, deletion, inventario, DTOs, migrations), so the mapping and writer triggers fire.

## Acceptance criteria

- A second replacement for the same original never reaches the server, and the losing device converges to the winner's claim with correct ledger and stock.
- A server-refused delete restores the order locally with its pagos; sync of other rows continues.
- Each behavior has a test that failed first; full suite green; GGA and RDD pass.

## Delivery

One work-unit commit per task on `feat/anulaciones-wu8-server-guards`; the chained PR targets `feat/anulaciones-wu7-backlog-fixes`.

## Progress

- Branch created at `f98a5d54`. Sync map recorded (Explore agent afb1734b).
- TDD: strict (source Engram `sdd-init/optoapp`), runner `./gradlew :optoapp:testDebugUnitTest`.
- T1 written: `supabase/migrations/20261002034600_anulacion_server_guards.sql` (global partial unique index `dispensaciones_reclamo_origen_uidx`; `guard_dispensaciones_delete` BEFORE DELETE trigger, SECURITY DEFINER so the trace check fails closed regardless of caller RLS, refusal `P0001` + message token `dispensacion_has_trace`; paciente cascades detected by the parent paciente no longer being visible). Behavior test `supabase/tests/test_anulacion_server_guards.sql` (rollback-only, G1-G5). Runtime proof pending: Docker is not installed on this machine, so neither `supabase db lint` nor the SQL test could run (RED/GREEN not observed for SQL). Must run in CI / a local stack before G1. Commit `95e51a2b` (GGA passed; hook skipped migration lint: Docker not available).
- T2 done. `UploadSyncCoordinator.detectClaimConflicts` (lookup now carries `reclamo_origen_id`): a local replacement never adopts another row's id; if its (remapped) original already has a different remote replacement it is quarantined `quarantine:reclamo_duplicate:<localOrigenId>`; if its OT belongs to an unrelated remote order, `quarantine:reclamo_ot_conflict:<remoteId>` (no auto-discard). The local original is held back from upload while either conflict is unresolved, so the loser's estado/motivo/fecha never overwrite the winner's. `FinanzasUploadValidator` makes only `dispensaciones_reclamo_origen_uidx` / `pagos_reversa_pago_id_uidx` 23505 isolatable (`poisonReason`), mapped to `reclamo_duplicate` / `quarantine:reverso_duplicate:<reversaPagoId>`. Pagos pre-gate quarantines a local Reverso whose target already has a different remote Reverso. Items, regalos (finanzas) and montura movimientos (`referenciaId == id` or `id:*`, inventario) of a `reclamo_duplicate` replacement are skipped; its transfer pagos keep the existing parent gate. RED: 7 UploadSyncCoordinatorTest + 1 SyncInventarioUseCaseUploadTest failed for the expected reasons (adoption/overwrite, 23505 rethrown, loser rows uploaded); characterization (passed first): own replacement already remote, unrelated 23505 still propagates, transfer pagos parent gate. Two fixture/expectation corrections in tests after first GREEN attempt (key-equal Abono fixture; original held back on OT conflict). GREEN: focused 71/71; full `testDebugUnitTest` BUILD SUCCESSFUL.

## Next step

T3.
