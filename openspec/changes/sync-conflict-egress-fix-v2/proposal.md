# Proposal: Sync Conflict Egress Fix v2

**Change**: `sync-conflict-egress-fix-v2`
**Artifact store**: hybrid · **TDD mode**: STRICT

## Intent

The Android app carries 981 persistent sync conflicts and burned a Supabase egress spike of 900 MB/day vs a 32 MB baseline. Three code-level root causes (RC-1, RC-2, RC-3) keep multiplying egress and prevent stale conflicts from clearing even though the underlying Supabase trigger is already fixed (`20260615000000_fix_updated_at_trigger_no_overwrite.sql`). Left alone, every sync cycle re-downloads full tables and the phantom conflict backlog never drains, risking egress overage cost and an unusable conflict UI.

## Scope

### In Scope
- **RC-1** — `ConflictHelper.fetchRemoteUpdatedAt` (lines 127–132): add `.in("id", ids)` filter so only the IDs under check are fetched, not the whole optica table. O(N) → O(k).
- **RC-2** — `PostSaveSyncScheduler.scheduleHistorialSync` (line 123) and `scheduleFinanzasSync` (line 147): remove the needless `syncPacientesUseCase!!(opticaId)` cascade so each scheduler syncs only its own module.
- **RC-3** — `ConflictHelper.filterConflicts`: when an entity goes to the `safe` list (`local >= remote`) and a conflict record exists, call `conflictDao.resolveConflict(entity.id, opticaId)` to auto-clear the stale record on the next sync cycle.

### Out of Scope
- New Supabase migration — trigger already fixed; no schema change.
- One-off purge of the 981 stale records — RC-3 self-heals them on the next sync; `acceptAllCloud()` remains the manual fallback.
- Conflict-resolution UI/UX, `resolveKeepMine`, and `sync_entity_state` durability — owned by `sync-conflict-rootcause`.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `android-sync-conflict`: conflict detection MUST fetch remote timestamps by ID only; per-module schedulers MUST NOT cascade a pacientes sync; conflicts MUST auto-clear when local is newer or equal.

## Approach

Two-file change (~80 lines incl. tests), single PR. RED→GREEN→REFACTOR: write/extend tests first.
1. `ConflictHelperTest` — assert ID-filtered query (RC-1) and auto-clear of safe entities (RC-3).
2. `PostSaveSyncSchedulerTest` — assert no pacientes cascade on historial/finanzas saves (RC-2).
3. Implement the three fixes to green.

**Decision — keep `downloadAfterUpload = true` in `performSilentSync`:** KEEP it. Once RC-1 makes downloads ID-scoped and RC-2 removes the cascade, the per-sync download cost collapses, so the marginal extra download is cheap. It guarantees the local entity converges to the server-confirmed `updated_at`, which is what prevents conflicts from regenerating. Removing it would trade a small egress saving for timestamp drift and resurrected phantom conflicts — the exact failure we are fixing. Revisit only if post-fix egress telemetry still shows the download as a hotspot.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `optoapp/.../domain/sync/ConflictHelper.kt` | Modified | RC-1 ID filter + RC-3 auto-clear |
| `optoapp/.../sync/PostSaveSyncScheduler.kt` | Modified | RC-2 remove pacientes cascade |
| `optoapp/.../test/.../ConflictHelperTest.kt` | New/Modified | RC-1 + RC-3 cases |
| `optoapp/.../test/.../PostSaveSyncSchedulerTest.kt` | Modified | RC-2 cases |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| RC-3 auto-clears a conflict the user still needed to review | Low | Only clears when `local >= remote` (no real divergence); resolution path unchanged |
| RC-2 removal drops a sync someone relied on | Low | Each module already syncs itself; tests assert no behavior loss |
| RC-1 `.in` filter misbehaves on empty `ids` | Low | Guard empty list; covered by test |

## Rollback Plan

Single PR — revert the merge commit. The two files return to prior behavior with no schema/data side effects (no migration involved). Stale records, if any remain, are still clearable via `acceptAllCloud()`.

## Dependencies

- Supabase trigger migration `20260615000000_...` already applied (prerequisite met).

## Success Criteria

- [ ] Per silent sync no longer triggers full-table downloads (RC-1) or a pacientes cascade on historial/finanzas saves (RC-2).
- [ ] Daily Supabase egress returns toward the ~32 MB baseline.
- [ ] The 981 stale conflict records drain to ~0 over subsequent sync cycles (RC-3).
- [ ] All new/updated tests pass; no conflict regenerates after a clean sync cycle.
