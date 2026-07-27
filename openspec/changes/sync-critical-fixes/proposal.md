# Proposal: Sync Critical Fixes

## Intent

Fix 4 critical/high bugs in OptoApp's offline-first sync that cause crashes, data loss, and silent failures. These share the same code areas (Room entities, DAOs, ConflictHelper, upload paths) and should ship together.

## Scope

### In Scope
- S2: Make EvaluacionEntity columns nullable where Supabase allows NULL (avoids crash on download)
- C2: Add `OnConflictStrategy.REPLACE` to all `@Insert` DAOs used in download paths (avoids crash on re-download)
- H6: Wrap `markSynced()` inside `database.withTransaction` on upload paths (avoids re-upload loop)
- All-chunk: Return empty map instead of throwing `RuntimeException` in `ConflictHelper.fetchRemoteUpdatedAt()` (avoids sync module crash)

### Out of Scope
- C3 (FK cascade on partial upload) — partially mitigated, requires deeper architectural change
- Silent 300s timeout — needs separate investigation
- Supabase schema or RLS changes
- New entity types or sync flow changes

## Capabilities

### New Capabilities
None — all changes are bugfixes, no new feature capabilities.

### Modified Capabilities
None — no spec-level behavior changes. Internal correctness fixes only.

## Approach

| Issue | Fix | File(s) |
|-------|-----|---------|
| S2 | Mark EvaluacionEntity fields `?` where Supabase returns NULL (e.g., `String?` for `String = ""` defaults). Room SQLite doesn't enforce NOT NULL — cursor reads null → crash. | `data/evaluacion/EvaluacionEntity.kt` |
| C2 | Change `@Insert` to `@Insert(onConflict = OnConflictStrategy.REPLACE)` in all DAOs called from download paths. | `data/gastooperativo/GastoOperativoDao.kt`, `data/ordencompra/OrdenCompraDao.kt`, `data/ordencompra/OrdenCompraItemDao.kt`, `data/regalodispensacion/RegaloDispensacionDao.kt`, `data/inventariofisico/InventarioFisicoDao.kt` |
| H6 | Replace `syncStateTracker.markSynced(...)` calls in upload paths (UploadSyncCoordinator, SyncPacientesUseCase, SyncHistorialUseCase, SyncInventarioUseCase, etc.) with `syncStateTracker.markSyncedAtomic(...)` or explicit `database.withTransaction { block(); markSynced() }`. | `domain/UploadSyncCoordinator.kt`, `domain/SyncPacientesUseCase.kt`, `domain/SyncHistorialUseCase.kt`, `domain/SyncInventarioUseCase.kt`, `domain/SyncProveedoresUseCase.kt`, `domain/SyncOrdenesCompraUseCase.kt`, `domain/SyncFinanzasMerge.kt`, `domain/SyncInventarioFisicoUseCase.kt` |
| All-chunk | Change `fetchRemoteUpdatedAt` to return `emptyMap()` instead of throwing `RuntimeException` when all chunks fail. | `domain/sync/ConflictHelper.kt` |

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `data/evaluacion/EvaluacionEntity.kt` | Modified | ~20 columns made nullable (String?, Int? where Supabase allows NULL) |
| `data/*/dao/*Dao.kt` | Modified | 5-6 DAOs: add `OnConflictStrategy.REPLACE` to `@Insert` |
| `domain/sync/ConflictHelper.kt` | Modified | Remove throw in `fetchRemoteUpdatedAt`, return empty map |
| `domain/UploadSyncCoordinator.kt` | Modified | Wrap per-entity markSynced inside withTransaction |
| `domain/SyncPacientesUseCase.kt` | Modified | Same transactional fix |
| `domain/SyncHistorialUseCase.kt` | Modified | Same transactional fix |
| `domain/SyncInventarioUseCase.kt` | Modified | Same transactional fix |
| `domain/SyncProveedoresUseCase.kt` | Modified | Same transactional fix |
| `domain/SyncOrdenesCompraUseCase.kt` | Modified | Same transactional fix |
| `domain/SyncFinanzasMerge.kt` | Modified | Same transactional fix |
| `domain/SyncInventarioFisicoUseCase.kt` | Modified | Same transactional fix |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Nullable EvaluacionEntity breaks consumers expecting non-null | Medium | Empty string defaults on null via `?: ""` getter or Elvis at call sites |
| withTransaction wrapping changes timing of markSynced relative to batch tracking | Low | Batch-level markSynced stays outside the per-entity transaction; only per-entity markers move inside |
| Missing a DAO with plain `@Insert` in download paths | Medium | Audit all 24+ DAOs systematically; grep for `@Insert` (no REPLACE) in files referenced by download use cases |

## Rollback Plan

Revert per-commit: each fix is isolated. Revert in reverse order (All-chunk → H6 → C2 → S2). No schema migration needed — SQLite column nullability changes are backward-compatible at storage level.

## Dependencies

None. All changes are client-side only.

## Success Criteria

- [ ] Download sync no longer crashes on EvaluacionEntity with NULL columns from Supabase
- [ ] Re-download with existing PKs no longer throws `SQLiteConstraintException`
- [ ] Upload with `markSynced` failure no longer causes re-upload loop (entity marked synced only after both DB write + state update succeed within transaction)
- [ ] Network outage during conflict detection returns empty safe list instead of crashing sync module
- [ ] All unit tests pass (`./gradlew :optoapp:testDebugUnitTest --stacktrace`)
