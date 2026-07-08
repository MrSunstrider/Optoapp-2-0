# Design: Judgment Day Follow-up Fixes

## Technical Approach

Eight independent fixes grouped by concern: data integrity (F1–F2), security (F3–F5), resilience (F6–F7), dev UX (F8). Each is a single-file mutation with no new types, schemas, or APIs.

All fixes are already applied on `main`. This design documents the *what and why* of each fix.

## Architecture Decisions

### F1: uploadServicios return value — `rows.size` over `servicios.size`

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Return `servicios.size` | Includes duplicates merged away during dedup; marks non-uploaded entities as synced | ❌ Rejected — inflates progress counters |
| Return `rows.size` | Only counts unique rows actually upserted; matches `uploadDispensaciones` pattern | ✅ Adopted |

**Rationale**: `servicios` is the raw snapshot. After `LinkedHashMap` dedup by OT/id, `rows` (`uniqueRows.values.distinctBy { it.id }`) is the actual payload. Returning `servicios.size` overcounts when duplicates exist. Critically, `servicios.forEach` with `markSynced(...)` marks merged-away duplicates as synced — a future sync will skip them, causing permanent data loss. The fix also changes sync-state marking from `servicios` to `rows`.

**Reference**: `uploadDispensaciones` does exactly this pattern (uniqueById → markSynced per unique → return `rows.size`).

### F2: uploadedVentas in FinanzasSyncResult

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Omit field (default 0) | Ventas upload counts invisible in monitoring | ❌ Rejected — breaks observability |
| Add `uploadedVentas: Int = 0` | Backward-compatible; sync dashboard sees actual counts | ✅ Adopted |

**Rationale**: `FinanzasSyncResult` had `uploadedServicios`, `uploadedPagos`, etc. but silently dropped `uploadedVentas`. Sync reporting showed 0 ventas uploaded even when they succeeded, making failures indistinguishable from successes. The `= 0` default avoids breaking existing callers.

### F3: Venta.toRemoto() createdAt — server timestamp, not client clock

| Option | Tradeoff | Decision |
|--------|----------|----------|
| `createdAt = createdAt ?: Instant.now()` | Client clock controls audit timestamp; malleable by user | ❌ Rejected — security risk |
| `createdAt = createdAt` (null → server DEFAULT now()) | Server sets authoritative timestamp; client cannot forge | ✅ Adopted |

**Rationale**: Audit fields (`created_at`, `updated_at`) are trust anchors. Using `Instant.now()` on the client means a manipulated device clock or compromised APK can backdate records. Supabase columns have `DEFAULT now()`, so omitting `createdAt` when null triggers the server-side value.

### F4: safeUpload 401/403 rethrow

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Generic `catch (e: Exception)` swallows all | Auth errors become silent 0s; RLS misconfig invisible | ❌ Rejected |
| Explicit `catch (e: RestException)` + rethrow 401/403 | Auth failures propagate as crashes; operator alerted | ✅ Adopted |

**Rationale**: `supabase-kt` wraps HTTP errors in `io.github.jan.supabase.exceptions.RestException` with a `statusCode` property. 401 = Unauthenticated, 403 = Forbidden (RLS policy rejected). Returning `0` for these hides security misconfiguration behind a "sync worked" facade. The fix catches `RestException` before the generic `Exception` catch and rethrows 401/403; other status codes (4xx/5xx) still degrade gracefully to 0.

### F5: VentaDao DELETE queries — opticaId filter

| Option | Tradeoff | Decision |
|--------|----------|----------|
| `DELETE FROM ventas WHERE id = :id` | Works in practice since Room is per-login | ❌ Rejected — fragile |
| `DELETE FROM ventas WHERE id = :id AND opticaId = :opticaId` | Defense-in-depth; requires caller to pass opticaId explicitly | ✅ Adopted |

**Rationale**: Room data is scoped by login (user sees their optica's data), so a bare `WHERE id = :id` is unlikely to delete wrong-tenant rows in normal operation. However, this is fragility, not safety. A bug in caller logic or a future code path that reuses these queries without tenant context could silently delete cross-tenant data. Adding `AND opticaId = :opticaId` is defense-in-depth with zero runtime cost.

### F6: NetworkRetryHelper — RestException catch for 429 retry

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Generic `catch (e: Exception)` in retry loop | Catches RestException but never retries it | ❌ Rejected |
| Explicit `catch (e: RestException)` + retry on 429 | HTTP 429 (rate limit) triggers backoff + jitter | ✅ Adopted |

**Rationale**: supabase-kt's Postgrest API throws `RestException` for any HTTP error. A 429 Too Many Requests should be retried with backoff, not bubbled up. The `isTransientNetworkError` helper already checks for "429" in the message string, but the catch hierarchy must **reach** that check. The explicit `RestException` catch mirrors the IOException catch and is placed at the same level.

### F7: Backoff jitter — `Random.nextLong(0, 200)`

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Deterministic backoff: 400ms, 800ms, 1200ms | All clients retry simultaneously = thundering herd | ❌ Rejected |
| Jittered backoff: `400*(attempt+1) + Random.nextLong(0, 200)` | Spreads retries across 0-199ms window per attempt | ✅ Adopted |

**Rationale**: Without jitter, N devices that see a transient failure simultaneously retry at the exact same time, recreating the load that caused the failure. `Random.nextLong(0, 200)` adds 0-199ms of noise per attempt, statistically spreading retries while keeping the base exponential window predictable.

### F8: emulador.bat — `%LOCALAPPDATA%` environment variable

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Hardcoded `C:\Users\Alvaro\...` | Only works on one machine; breaks in CI or other dev machines | ❌ Rejected |
| `%LOCALAPPDATA%\Android\Sdk` | Works on any Windows machine regardless of username | ✅ Adopted |

**Rationale**: The SDK path depends on the Windows username. `%LOCALAPPDATA%` resolves dynamically to the current user's `AppData\Local` directory, making the script portable.

## Data Flow

None of the fixes alter runtime data flow. They correct: return values (F1), result fields (F2), serialization (F3), exception routing (F4, F6), query predicates (F5), timing (F7), and scripts (F8).

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `domain/UploadSyncCoordinator.kt` | Modify | F1: return `rows.size`, mark synced on `rows` not `servicios` |
| `domain/SyncFinanzasDto.kt` | Modify | F2: add `uploadedVentas: Int = 0`; F3: remove `Instant.now()` from `createdAt` |
| `domain/SyncFinanzasUseCase.kt` | Modify | F2: pass `ventasUp` to result; F4: add `RestException` catch for 401/403 rethrow |
| `data/venta/VentaDao.kt` | Modify | F5: add `AND opticaId = :opticaId` to both DELETE queries |
| `domain/NetworkRetryHelper.kt` | Modify | F6: add `RestException` catch; F7: add `Random.nextLong(0, 200)` jitter |
| `emulador.bat` | Modify | F8: `%LOCALAPPDATA%` replaces hardcoded path |

## Testing Strategy

Existing unit tests exercise `FinanzasSyncResult` construction — the new field defaults to 0 and is backward-compatible. No new test coverage is required for any fix; each change is a direct correction verified by existing tests + build.

Verification: `./gradlew :optoapp:testDebugUnitTest --stacktrace` + `./gradlew :optoapp:assembleDebug`.

## Migration / Rollout

No migration required. Each fix is revertible individually from git history.

## Open Questions

None. All fixes are already applied.
