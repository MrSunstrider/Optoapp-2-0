# Proposal: Persist Rate Limiter to Supabase

## Intent

The in-memory `Map` in `web/src/lib/rate-limit.ts` resets on every serverless cold start (Vercel), rendering PIN brute-force protection ineffective in production. We need a persistent store that survives across invocations.

## Scope

### In Scope
- Replace in-memory `Map` with Supabase DB-backed rate limiter (`pin_attempts` table)
- Keep `checkRateLimit(key: string)` API signature unchanged
- Add per-IP and per-userId composite tracking
- TTL-based expiration with cleanup strategy
- Migration for `pin_attempts` table with RLS policy
- Update `web/src/lib/rate-limit.test.ts` to mock Supabase calls

### Out of Scope
- Android rate limiting (server-side only)
- Client-side throttling UI changes
- Distributed Redis or external cache services

## Capabilities

### New Capabilities
- `api-rate-limiting`: DB-backed attempt tracking with TTL expiration for serverless environments

### Modified Capabilities
None

## Approach

Replace the `Map` with `supabase.from('pin_attempts').upsert()` / `.select()`. Use a composite key of `ip_hash` + `user_id` (or a single `limit_key` column). Expiration handled via `expires_at` timestamp; cleanup via a lightweight nightly `pg_cron` job or `DELETE` on read for stale rows (read-repair). The function stays synchronous-looking by using `await` internally — callers already run in async server contexts (Server Actions / Route Handlers). Constants (`WINDOW_MS`, `MAX_ATTEMPTS`) remain unchanged.

Alternative considered: Redis/Upstash — rejected because the project already uses Supabase as its single persistence layer; adding another service adds ops cost and a new failure mode.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `web/src/lib/rate-limit.ts` | Modified | DB-backed implementation, same signature |
| `web/src/lib/rate-limit.test.ts` | Modified | Mock Supabase client; assert DB interactions |
| `supabase/migrations/` | New | `pin_attempts` table, indexes, RLS policy |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Supabase latency adds ~20-50ms per PIN check | Med | Add `LIMIT 1` index on `limit_key`; benchmark in verify phase |
| Migration fails on conflict with existing table | Low | Use idempotent `CREATE TABLE IF NOT EXISTS` |
| Test flakiness from async DB mocking | Low | Use Vitest `vi.mock` for `@supabase/ssr`; mock clock still works |

## Rollback Plan

1. Revert `rate-limit.ts` to the previous `Map` implementation (single commit revert).
2. Drop the `pin_attempts` table via a new migration if needed.
3. No schema changes affect other tables; rollback is isolated.

## Dependencies

- Supabase project with `pg_cron` enabled (standard on Supabase Postgres 17) or a lightweight scheduled edge function for cleanup.

## Success Criteria

- [ ] `checkRateLimit` returns identical `{ allowed, remaining }` shape for all existing test scenarios
- [ ] Attempts persist across simulated cold starts (two separate Supabase client instances)
- [ ] Stale rows older than `WINDOW_MS` are not counted
- [ ] `npm test` passes with mocked Supabase client
- [ ] Migration applies cleanly to existing Supabase project
