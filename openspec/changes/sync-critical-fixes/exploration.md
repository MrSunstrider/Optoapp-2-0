## Exploration: Sync Critical Fixes

### Current State
OptoApp uses offline-first architecture with Room local DB syncing to Supabase PostgreSQL. Two recent fixes (c8c6768 batch queries, 9aac3fe telemetry) improved conflict detection and error visibility. sync-resilience-improvements is fully delivered. sync-conflict-servicios code is deployed but tests missing.

### Affected Areas
- `optoapp/src/main/java/com/example/optoapp/data/sync/` — ConflictHelper, SyncOrchestrator, upload/download coordinators
- `optoapp/src/main/java/com/example/optoapp/data/local/` — Room entities (EvaluacionEntity, DAOs)
- `optoapp/src/main/java/com/example/optoapp/data/repository/OptoRepository.kt` — upsert methods
- `optoapp/src/main/java/com/example/optoapp/data/local/dao/` — All DAOs with @Insert methods used in download paths

### Issues Found (ordered by severity)

#### S2 — Evaluaciones nullable mismatch (CRITICAL)
Room EvaluacionEntity has 90+ columns as NOT NULL, but Supabase accepts NULL. If Supabase returns a row with NULL in any column, Room crashes with CursorIndexOutOfBoundsException or null assigned to non-null type.

#### C2 — Download not idempotent (CRITICAL)
DAO methods using @Insert without OnConflictStrategy.REPLACE crash on duplicate PK during re-download (SQLiteConstraintException). Affects all *FromRemote download paths.

#### C3 — FK cascade on partial upload (CRITICAL, partially mitigated)
safeUpload returns partial count via UploadPartialException. Then uploadPagos runs referencing local dispensaciones that don't exist on server → FK error from Supabase. Errors propagate but pay cycle is wasted.

#### H6 — SyncStateTracker not transactional on upload (HIGH)
markSynced runs AFTER successful chunk uploads outside database.withTransaction. If markSynced fails, entity is uploaded but marked as not-synced → re-uploaded next cycle. Download path already wraps both in withTransaction.

#### All-chunk failure crash (HIGH)
ConflictHelper.selectRemoteRows throws RuntimeException("All chunk queries failed for $tableName") when ALL chunk queries fail. Callers in filterConflicts don't catch this → crashes the sync module.

#### Silent 300s timeout (HIGH)
executeSilentModules times out silently if a single module hangs (logs to AppLogger, returns onModuleResult("_timeout", ...)). No user-visible error.

### Recommendation
Fix in priority order: S2 → C2 → H6 → all-chunk failure handling. These share the same code area and can be delivered as one change.

### Ready for Proposal
Yes
