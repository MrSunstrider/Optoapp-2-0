# Verify: fix-inventory-movimientos-pk-reconcile

Date: 2026-08-27

## Commands

| Command | Result |
|---------|--------|
| Targeted partition + upload tests (after Round 1 fixes) | BUILD SUCCESSFUL |
| `ConflictHelperMovimientoPersistenceTest` (incl. fail-closed fetch) | BUILD SUCCESSFUL |
| Full `:optoapp:testDebugUnitTest` | 2215 completed, 3 failed in `DispensacionViewModelCreateSaveTest` / `DeleteTest` (Dispatchers.Main leftover; not this change) |

## FR mapping

| Requirement | Test |
|-------------|------|
| Local UUID differs, same stock → no POST, adopt remote id, delete old PK | `uploadMovimientos_skipsRemoteUpsert_whenCompositeKeyExistsWithDifferentId` |
| Remote fetch fails → no upload, `Resource.Error` | `uploadMovimientos_failsClosed_whenRemoteFetchFails` |
| No remote match → upsert | `uploadMovimientos_uploadsWhenNoRemoteMatch` |
| Stock differs → no upload | `uploadMovimientos_doesNotUploadConflictedMovimientos` |
| Partition pure logic | `MovimientoUploadPartitionTest` (3 cases) |
| Persistence + fail-closed fetch | `ConflictHelperMovimientoPersistenceTest` |

## Manual (device)

Edit a dispensación with tienda montura → sync → logcat must not contain `idx_movimientos_conflict` / `23505`.

## GGA Round 1 (equivalent, live)

R1–R4 BLOCKED on: (1) reconcile without deleting old local PK; (2) fetch fail-open + composite `onConflict` rewriting remote `id`.

Fixes: `deleteMonturaMovimiento`, `remoteFetchSucceeded=false` aborts upload, composite `onConflict` removed.

## RDD

`rdd_mode`: unmanaged. Causal invariant: one movement fact per `(referencia_id, tipo, montura_id)`. Rollback: revert the listed production files.

## Judgment Day Round 2

| Finding | Judge A | Judge B | Severity | Status |
|---------|---------|---------|----------|--------|
| PostgREST fetch truncates ~1000 | no | yes | WARNING (theoretical) | Suspect → INFO (prod ~37 rows; pre-existing fetch shape) |
| Reconcile skips metadata upload | no | yes | WARNING (theoretical) | Suspect → INFO (ledger fact is stockNuevo; download is LWW) |
| markSynced after reconcile skipped if upload throws | yes | no | WARNING (theoretical) | Suspect → INFO (recovers next cycle; monitoring only) |
| Dead `local.id != remoteId` guard | yes | no | SUGGESTION | Suspect → INFO |

Confirmed CRITICAL: 0. Confirmed WARNING (real): 0.

**JUDGMENT: APPROVED**
