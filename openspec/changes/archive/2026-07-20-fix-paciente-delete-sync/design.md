# Design: Fix Paciente Delete & Sync Integrity

## Technical Approach

Six isolated fixes for data corruption, sync integrity, and authorization gaps in the Pacientes module. F1 code already exists in `SyncPacientesUseCase.download()` — only RED→GREEN tests needed. F2–F6 require small code changes validated by new tests. All fixes target the Room↔Supabase sync pipeline and the ViewModel authorization layer. Strict TDD: tests written RED first, then code fixed to GREEN.

## Architecture Decisions

| # | Option | Tradeoff | Decision |
|---|--------|----------|----------|
| F2 | `val canonical` → `var canonical` | Mutable var is less FP-idiomatic but reflects the accumulating-merge reality | Use `var canonical`, reassign after each `mergePacienteData` call inside the transaction |
| F4 | Add `catch (e: CancellationException) { throw e }` before `IOException` in Phase 1 inner try | Requires two catch additions (inner + outer try blocks) | Add rethrow in BOTH blocks: inner delete-loop catch (line 189) and outer pending-deletion query catch (line 199) |
| F5 | Abort upload on double network failure | Over-conservative if Supabase is genuinely empty | In `upload()`, when `effectiveRemoteMap == null` (batch failed) AND per-entity fallback also yields no remote timestamps for checkable entities, log prominent warning and return 0 (abort). The guard checks: if `conflictSafe.size == deduplicated.size` while `checkableCount > 0`, double-failure is likely |
| F6 | `AuthorizationGuard.requireRole(role, setOf("admin", "gerente"), "guardar paciente")` at top of `savePaciente` | Fail-fast, same pattern as `deletePacienteGuarded`; blocks all non-admin/gerente saves | Use `IllegalArgumentException` thrown by `require()` inside the guard, matching the delete pattern exactly |

## Data Flow

```
F1 download() Phase 1             F1 download() Phase 2
sync_entity_state ──retry──► Supabase DELETE        conflictDao + pendingDeletions
        │                          │                        │
        ├─success► clearEntityState│                        └─► skipIds set
        └─fail───► preserve tombstone                      │
                                                      forEach remote ──► if id in skipIds? skip : upsert

F5 upload() double-failure guard
fetchAllRemotePacientes ──empty──► effectiveRemoteMap=null ──► filterConflicts (per-entity fallback)
        │                                                              │
        └─(double-fail check)◄── if checkableCount>0 AND all passed ──┘
                │
                └─► markError + return 0 (abort)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `data/PacienteRepository.kt:174` | Modify (F2) | `val canonical` → `var canonical`; reassign after `mergePacienteData` |
| `domain/SyncPacientesUseCase.kt:189,199` | Modify (F4) | Add `catch (e: CancellationException) { throw e }` in inner+outer Phase 1 blocks |
| `domain/SyncPacientesUseCase.kt:133-141` | Modify (F5) | Post-`filterConflicts` check: if `effectiveRemoteMap==null` and all checkable entities passed, abort |
| `viewmodel/PacienteViewModel.kt:157` | Modify (F6) | Add `AuthorizationGuard.requireRole(role, setOf("admin", "gerente"), "guardar paciente")` before duplicate check |
| `test/.../SyncPacientesUseCaseDownloadGuardTest.kt` | Modify (F1,F4,F5) | Add Phase 1 retry test, skipIds guard test, CancellationException propagation test, double-failure abort test |
| `test/.../PacienteRepositoryTest.kt` | Modify (F2,F3) | Add 3-duplicate HO merge test; add updatedAt stamping test |
| `test/.../PacienteViewModelTest.kt` | Modify (F3,F6) | Add savePaciente role authorization tests (admin passes, vendedor denied) |

## Testing Strategy

| Fix | Layer | Test file | Scenario |
|-----|-------|-----------|----------|
| F1 | Unit (mock) | `SyncPacientesUseCaseDownloadGuardTest` | Tombstone in sync_entity_state → Phase 1 retries DELETE → Phase 2 skipIds prevents re-insert |
| F2 | Integration (Room) | `PacienteRepositoryTest` | 3 pacientes same HO: `resolveDuplicatePacientesByHistoria` merges all fields, deletes duplicates |
| F3 | Unit | `PacienteRepositoryTest` | `OptoRepository.insertPaciente` stamps `updatedAt` non-null on save |
| F4 | Unit (mock) | `SyncPacientesUseCaseDownloadGuardTest` | Cancel coroutine scope during Phase 1; verify `CancellationException` rethrown, not swallowed |
| F5 | Unit (mock) | `SyncPacientesUseCaseDownloadGuardTest` | Mock `fetchAllRemotePacientes` returns empty + `fetchRemoteUpdatedAt` returns empty; verify upload returns 0 |
| F6 | Unit (mock) | `PacienteViewModelTest` | Admin role → save succeeds; vendedor role → `IllegalArgumentException("Unauthorized")` thrown |

F3 stamping verification: since `savePaciente` delegates to `OptoRepository.insertPaciente` which already stamps `updatedAt`, the test proves `@Upsert` persists the stamped value by reading back via `getPacienteById`.

## Migration / Rollout

No data migration required. Each fix is independently revertible via commit. F2 mutability change is scoped to a local variable within a transaction.

## Open Questions

None.
