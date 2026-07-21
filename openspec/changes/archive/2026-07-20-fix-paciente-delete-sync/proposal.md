# Proposal: Fix Paciente Delete & Sync Integrity

## Intent

Six bugs found in a 3-round Judgment Day review of the Pacientes module cause data corruption (patient resurrection after partial delete, data loss on 3+ duplicate merge), sync integrity failure (CancellationException swallowed, silent conflict bypass on double network failure), and incomplete authorization (`savePaciente` lacks role check, `@Upsert` bypasses `updatedAt` stamping). Some fixes already in code but lack TDD tests — this change formalizes all fixes with proper specs and RED→GREEN coverage.

## Scope

### In Scope
- **F1**: Patient resurrection: formalize Phase 1 retry + `skipIds` guard with TDD tests (3-step scenario: local delete → remote fail → sync avoids re-insert)
- **F2**: 3+ duplicate merge: fix `val canonical` → `var canonical` in `resolveDuplicatePacientesByHistoria` + tests for 3+ rows
- **F3**: `updatedAt` stamping: ensure `savePaciente` → `OptoRepository.insertPaciente` stamps `updatedAt`, add tests proving `@Upsert` preserves timestamp
- **F4**: CancellationException in Phase 1: add `catch (e: CancellationException) { throw e }` in inner try/catch of `download()` Phase 1
- **F5**: Silent conflict bypass: if per-entity `fetchRemoteUpdatedAt` also fails, do NOT treat all entities as safe; add conflict-protection test for double-failure edge case
- **F6**: Role authorization for `savePaciente`: add `AuthorizationGuard.requireRole` matching `deletePacienteGuarded` pattern

### Out of Scope
- Sync generalization to other entity types (e.g., evaluaciones, dispensaciones)
- Three-way merge enhancements (FR-07–FR-12 from sync-conflict spec)
- PIN/2FA auth changes

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- **sync-state-tracking**: Add requirement for automatic pending-delete retry during download (Phase 1) + `skipIds` guard against resurrection (F1)
- **sync-conflict**: Update conflict detection to handle `fetchRemoteUpdatedAt` failure (F5); add test scenarios for 3+ duplicate merge (F2)
- **sync-state-tracking** (extended): `savePaciente` SHALL require `AuthorizationGuard.requireRole(role, ["admin", "gerente"])` (F6)

## Approach

| Fix | Type | Approach |
|-----|------|----------|
| F1 | Test + code formalization | Tests verify download() Phase 1 retries pending deletes + `skipIds` prevents re-insert. Code already in place — write failing tests first (RED) then confirm (GREEN) |
| F2 | Bug fix + tests | Change `val canonical` → `var canonical` in `PacienteRepository.resolveDuplicatePacientesByHistoria`; `mergePacienteData` already returns a new instance, assign back to `canonical`. Tests for 3+ entries same HO |
| F3 | Bug fix + tests | `OptoRepository.insertPaciente` already stamps `updatedAt` — add tests confirming `@Upsert` stores the stamped value and conflictHelper picks it up |
| F4 | Bug fix | Add `catch (e: CancellationException) { throw e }` to inner Phase 1 delete loop before `catch (e: IOException)` |
| F5 | Bug fix + tests | In `filterConflicts`, when `fetchRemoteUpdatedAt` returns empty AND local entities have timestamps, `fetchAllRemotePacientes` also returns empty — guard path already exists (`effectiveRemoteMap = null` fallback). Write tests for double-failure scenario |
| F6 | Implementation | Add `AuthorizationGuard.requireRole` at top of `savePaciente`, fail-fast with same error pattern as `deletePacienteGuarded` |

## Affected Areas

| File | Impact | Fixes |
|------|--------|-------|
| `domain/SyncPacientesUseCase.kt` | Modified | F1 (already patched), F4, F5 |
| `data/PacienteRepository.kt` | Modified | F2 |
| `data/OptoRepository.kt` | Already fixed | F3 (stamping exists) |
| `viewmodel/PacienteViewModel.kt` | Modified | F6 |
| `domain/sync/ConflictHelper.kt` | Unchanged (guard exists) | F5 (tests only) |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| F2 regression on existing HO merge | Low | Wrap in transaction, existing tests + new 3+ scenario |
| F6 blocks legitimate save if role enum changes | Low | Role check matches existing `deletePacienteGuarded` exactly |
| Sync timing change from F4 rethrow | Low | `invoke()` already handles CancellationException properly |

## Rollback Plan

Revert individual commits per fix. Each fix is self-contained and independently revertible. F1–F6 can be rolled back separately without affecting other functionality.

## Dependencies

- Existing test infrastructure (Robolectric, Room in-memory DB, mock Supabase client)
- No new external dependencies

## Success Criteria

- [ ] F1: Test proves pending-delete paciente is NOT re-inserted on download after remote delete failure
- [ ] F2: 3+ duplicate pacientes with same HO merge without data loss (all fields preserved)
- [ ] F3: `@Upsert` via `savePaciente` stores non-null `updatedAt` timestamp
- [ ] F4: CancellationException in Phase 1 propagates (not swallowed)
- [ ] F5: Double network failure during upload does NOT silently bypass conflict detection
- [ ] F6: `savePaciente` blocks unauthorized roles with `IllegalArgumentException`
- [ ] All tests pass: `./gradlew :optoapp:testDebugUnitTest --stacktrace`
