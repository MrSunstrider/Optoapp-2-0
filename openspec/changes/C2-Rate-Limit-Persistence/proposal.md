# Proposal: C2-Rate-Limit-Persistence

## Intent

The committed `web/src/lib/rate-limit.ts` uses an in-memory `Map` that resets on every serverless cold start (Vercel), rendering PIN brute-force protection ineffective. We need persistent cross-invocation state with minimal latency.

## Scope

### In Scope
- Replace in-memory `Map` with a Supabase DB-backed rate limiter
- Keep `checkRateLimit(key: string)` API and return shape
- Support per-IP and per-userId tracking via caller-composed keys
- TTL-based auto-expiration with cleanup
- Migration for `pin_attempts` table, RLS, and atomic RPC

### Out of Scope
- Android rate limiting
- Client-side throttling UI
- External caching services (evaluated and rejected)

## Capabilities

### New Capabilities
- `api-rate-limiting`: DB-backed attempt tracking with TTL expiration for serverless

### Modified Capabilities
None

## Approach

Use a Supabase `pin_attempts` table with an atomic PL/pgSQL RPC (`check_rate_limit`) that SELECTs-or-INSERTs-or-UPDATEs in one round-trip. The TS layer becomes a thin async wrapper calling `supabase.rpc()`. Callers in async server contexts only need to `await` the result.

| Option | Persistence | TTL | Latency | Ops Cost |
|--------|-------------|-----|---------|----------|
| Supabase DB + RPC | Yes | pg_cron + read-repair | ~20-50ms | Zero |
| Upstash Redis | Yes | Built-in EXPIRE | ~5-15ms | New service + cost |
| Vercel KV | Yes | Built-in EXPIRE | ~10-30ms | New service + cost |

**Recommendation**: Supabase DB + RPC. Zero new dependencies, single round-trip, leverages existing RLS and migration workflow. Redis/Vercel KV add infrastructure cost and a new failure mode for marginal latency gains on a non-hot path.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `web/src/lib/rate-limit.ts` | Modified | Async RPC wrapper; no local state |
| `web/src/lib/rate-limit.test.ts` | Modified | Mock `createClient` RPC; preserve fake timers |
| `web/src/app/api/auth/verify-pin/route.ts` | Modified | Add `await` to `checkRateLimit` call |
| `web/src/app/api/config/change-pin/route.ts` | Modified | Add `await` to `checkRateLimit` call |
| `supabase/migrations/` | New | `pin_attempts` table, composite index, RLS, RPC, pg_cron cleanup |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|-------------|
| Supabase latency adds ~20-50ms per check | Med | Composite index on `(limit_key, window_start)`; benchmark in verify |
| `pg_cron` unavailable on project | Low | Fallback to read-repair filter; optional edge-function cleanup |
| Migration conflicts with existing table | Low | Idempotent `CREATE TABLE IF NOT EXISTS` |

## Rollback Plan

1. Revert `rate-limit.ts` to previous in-memory `Map` implementation.
2. Drop `pin_attempts` table and `check_rate_limit` function via a new migration if needed.
3. Isolated change — no other tables affected.

## Dependencies

- Supabase project with `pg_cron` enabled (standard) or read-repair-only fallback.

## Success Criteria

- [ ] `checkRateLimit` returns identical `{ allowed, remaining }` shape for all test scenarios
- [ ] Attempts persist across simulated cold starts
- [ ] Stale rows older than `WINDOW_MS` are not counted
- [ ] `npm test` passes with mocked Supabase client
- [ ] Migration applies cleanly to existing Supabase project
