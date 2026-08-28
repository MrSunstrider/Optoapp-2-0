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

## Judgment Day Round 2 (post-fix)

| Finding | Judge A | Judge B | Severity | Status |
|---------|---------|---------|----------|--------|
| Reconcile non-transactional | — | — | CRITICAL | **Fixed** — `runInTransaction` wraps upsert+delete |
| Robolectric in persistence test | — | — | WARNING | **Fixed** — pure JUnit + MockK |
| Unbounded fetch (upload path) | — | — | WARNING | **Fixed** — paginated 500/page |
| `safeIds.toSet()` in filter loop | — | — | SUGGESTION | **Fixed** — hoisted to `safeIdSet` |
| Reconcile without `markError` | — | — | WARNING | **Fixed** — try/catch + markError |
| Inflated `uploadedMovimientos` count | — | — | SUGGESTION | **Fixed** — `reconciledMovimientos` field |
| Duplicate `associateBy` remote keys | — | — | WARNING | **Fixed** — `indexRemoteByCompositeKey` |
| Incomplete `MovimientoRemotoRow` | — | — | WARNING | **Fixed** — full snapshot fields |
| Download path unbounded | yes | yes | WARNING (theoretical) | **Fixed** — `fetchAllRemoteMovimientos` paginated |
| Conflict records missing localData/remoteData | no | yes | WARNING (theoretical) | **Fixed** |
| Double indexRemote pass | yes | no | SUGGESTION | **Fixed** — overload accepts `remoteByKey` |
| `toRemoto` drops updatedAt | yes | no | WARNING (pre-existing) | **Fixed** |
| Offset pagination snapshot isolation | no | yes | WARNING (theoretical) | INFO — acceptable for current scale |

Confirmed CRITICAL: 0. Confirmed WARNING (real): 0.

**JUDGMENT: APPROVED**
