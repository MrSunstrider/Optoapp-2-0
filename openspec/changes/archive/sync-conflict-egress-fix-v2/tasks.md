# Tasks: Sync Conflict Egress Fix v2

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~80–120 (2 prod files + 2 test files) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |
| Chain strategy | size-exception |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | All RC-1 + RC-2 + RC-3 fixes | PR 1 | Single atomic PR; ~80–120 lines well within budget |

---

## Phase 1: RED — Write Failing Tests (Strict TDD)

> All 8 test methods MUST be written and confirmed failing BEFORE any production code changes.
> Test runner: `./gradlew testDebugUnitTest`

- [x] 1.1 **[NEW FILE]** Create `ConflictHelperTest.kt` at `optoapp/src/test/java/com/example/optoapp/domain/sync/ConflictHelperTest.kt`. Scaffold the test class with `ConflictHelper(supabase, tracker, dao)` construction; mock `ConflictDao` with MockK relaxed; define a `FakeConflictHelper` inner subclass that overrides `selectRemoteRows` and captures the received `ids` argument.
- [x] 1.2 **[RC-1 RED]** Add `fetchRemoteUpdatedAt_returnsEmptyMap_whenIdsEmpty`: call `fetchRemoteUpdatedAt(tableName, opticaId, emptyList())`; assert result `== emptyMap()`; assert `selectRemoteRows` was NOT called. Run test — must be RED (method does not yet exist).
- [x] 1.3 **[RC-1 RED]** Add `fetchRemoteUpdatedAt_usesInFilter_whenIdsNonEmpty`: override `selectRemoteRows` to capture `ids`; invoke with `[id1, id2]`; assert captured ids `== listOf(id1, id2)`. Run — RED.
- [x] 1.4 **[RC-1 RED]** Add `fetchRemoteUpdatedAt_returnsOnlyRequestedIds`: `selectRemoteRows` fake returns rows for `id1`, `id2`, `id3`; call with `[id1, id2]`; assert result keys `== setOf(id1, id2)` and `id3` absent. Run — RED.
- [x] 1.5 **[RC-3 RED]** Add `filterConflicts_callsResolveConflict_forSafeEntityWithRecord`: entity `e1` local > remote; stub `conflictDao.resolveConflict` to succeed; run `filterConflicts`; `verify(exactly = 1) { conflictDao.resolveConflict(e1.id, opticaId) }`. Run — RED.
- [x] 1.6 **[RC-3 RED]** Add `filterConflicts_doesNotCallResolveConflict_forConflictedEntity`: entity `e2` local < remote; run `filterConflicts`; `verify(exactly = 0) { conflictDao.resolveConflict(e2.id, any()) }`. Run — RED.
- [x] 1.7 **[RC-3 RED]** Add `filterConflicts_resolveConflict_isIdempotentWhenNoRecord`: entity `e3` local > remote; `conflictDao.resolveConflict` is a no-op relaxed mock (no record); run `filterConflicts`; assert no exception thrown and method was called once. Run — RED.
- [x] 1.8 **[RC-2 RED]** In existing `PostSaveSyncSchedulerTest.kt` add `scheduleHistorialSync_doesNotInvokeSyncPacientes`: mock `syncPacientesUseCase`; call `scheduleHistorialSync(opticaId)`; `verify(exactly = 0) { syncPacientesUseCase(any()) }`. Run — RED (currently line 123 fires the cascade).
- [x] 1.9 **[RC-2 RED]** Add `scheduleFinanzasSync_doesNotInvokeSyncPacientes`: same pattern for `scheduleFinanzasSync`; `verify(exactly = 0) { syncPacientesUseCase(any()) }`. Run — RED (line 147 cascade).

> Gate: all 8 test methods are present and failing. Do NOT proceed to Phase 2 until confirmed RED.

---

## Phase 2: GREEN — Implement Production Fixes

> Implement the minimal code to make every RED test pass. No gold-plating.

- [x] 2.1 **[RC-1 — ConflictHelper.kt]** Extract `@VisibleForTesting internal open suspend fun selectRemoteRows(tableName: String, opticaId: String, ids: List<String>): List<RemoteTimestamp>` with real postgrest body: `supabase.postgrest[tableName].select { filter { eq("optica_id", opticaId); isIn("id", ids) } }.decodeList()`. Note: `isIn` confirmed in postgrest-kt 3.6.0 bytecode. Seam is `internal open` (not `protected open`) because `protected` cannot expose `internal` type — Kotlin compiler rejects it.
- [x] 2.2 **[RC-1 — ConflictHelper.kt]** Rewrite `fetchRemoteUpdatedAt` to: (a) return `emptyMap()` immediately when `ids.isEmpty()`; (b) otherwise call `selectRemoteRows(tableName, opticaId, ids)` and build the result map directly from returned rows. Mark method `internal`. Drop the in-memory `idSet` post-filter — server-side `isIn` is trusted.
- [x] 2.3 **[RC-3 — ConflictHelper.kt]** In `filterConflicts`, inside both safe branches (null-check early return and `isLocalNewerOrEqual` true branch), add: `conflictDao.resolveConflict(entity.id, opticaId)` unconditionally after `safe.add(entity)`.
- [x] 2.4 **[RC-2 — PostSaveSyncScheduler.kt]** Deleted `syncPacientesUseCase!!(opticaId)` from `scheduleHistorialSync` (was line ~123). Deleted `syncPacientesUseCase!!(opticaId)` from `scheduleFinanzasSync` (was line ~147). No other structural change.

---

## Phase 3: VERIFY — Confirm GREEN + Regression Check

- [x] 3.1 Run `./gradlew testDebugUnitTest` — all 8 new test methods pass GREEN. 877 total tests, 0 failures.
- [x] 3.2 No pre-existing tests regressed (zero new failures in the full suite).
- [x] 3.3 `fetchRemoteUpdatedAt` is `internal` (not public) in `ConflictHelper.kt`.
- [x] 3.4 `selectRemoteRows` is `internal open` and annotated `@VisibleForTesting`.
- [x] 3.5 `scheduleHistorialSync` and `scheduleFinanzasSync` contain no reference to `syncPacientesUseCase` after the edit.

---

## Phase 4: Cleanup

- [x] 4.1 Dead `idSet`-related code removed from `fetchRemoteUpdatedAt` — it's now a clean delegate to `selectRemoteRows`.
- [x] 4.2 Unused imports removed from `ConflictHelper.kt` (old `@kotlinx.serialization.SerialName` inline ref replaced by top-level import).
- [x] 4.3 Confirm build compiles cleanly: `./gradlew assembleDebug`.

---

## Spec → Task Traceability

| Spec Requirement | Tasks |
|------------------|-------|
| RC-1: ID-scoped fetch (empty guard) | 1.2, 1.3, 1.4, 2.1, 2.2 |
| RC-2: scheduler isolation | 1.8, 1.9, 2.4 |
| RC-3: auto-clear safe entities | 1.5, 1.6, 1.7, 2.3 |
| All GREEN + no regression | 3.1–3.5 |
| Dead code cleanup | 4.1–4.3 |

## Files Changed

| File | Action | Phase |
|------|--------|-------|
| `optoapp/src/main/java/com/example/optoapp/domain/sync/ConflictHelper.kt` | Modify (RC-1 seam + RC-3 resolveConflict) | 2.1–2.3 |
| `optoapp/src/main/java/com/example/optoapp/sync/PostSaveSyncScheduler.kt` | Modify (RC-2 remove 2 lines) | 2.4 |
| `optoapp/src/test/java/com/example/optoapp/domain/sync/ConflictHelperTest.kt` | Modified (6 new test methods for RC-1 + RC-3 added to existing file) | 1.1–1.7 |
| `optoapp/src/test/java/com/example/optoapp/sync/PostSaveSyncSchedulerTest.kt` | Modify (2 new test methods for RC-2) | 1.8–1.9 |
