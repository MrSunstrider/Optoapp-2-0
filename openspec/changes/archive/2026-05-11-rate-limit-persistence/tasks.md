# Tasks: Persist Rate Limiter to Supabase

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~215 (60 SQL + 155 TS) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR (or PR #1 Migration → PR #2 Implementation if preferred) |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: stacked-to-main
400-line budget risk: Low

## Phase 1: Database Migration

- [x] 1.1 Create `supabase/migrations/20260511000000_pin_attempts_rate_limit.sql` — `pin_attempts` table, composite index `(limit_key, window_start)`, RLS + service_role policy, `check_rate_limit` PL/pgSQL RPC function (atomic SELECT-or-INSERT-or-UPDATE), pg_cron cleanup job (hourly, delete >24h)

## Phase 2: Core Implementation

- [x] 2.1 Rewrite `web/src/lib/rate-limit.ts` — replace in-memory `Map` with async wrapper calling `supabase.rpc("check_rate_limit", { p_limit_key, p_window_ms, p_max_attempts })`; remove `attempts` Map and sync logic

## Phase 3: Integration

- [x] 3.1 Update `web/src/app/api/auth/verify-pin/route.ts:19` — add `await` before `checkRateLimit("pin-verify:" + user.id)`
- [x] 3.2 Update `web/src/app/api/config/change-pin/route.ts:18` — add `await` before `checkRateLimit("pin-change:" + user.id)`

## Phase 4: Testing

- [x] 4.1 Rewrite `web/src/lib/rate-limit.test.ts` — mock `@/lib/supabase/server` with stateful in-memory RPC mock; add `await` to all 8 existing test calls; preserve fake timer window-expiry scenarios
- [x] 4.2 Verify: run `npm test` in `web/` — all tests pass; run migration against Supabase SQL editor and manual RPC calls

## Implementation Order

Phase 1 first (migration) — everything depends on the DB schema and RPC. Then Phase 2 (core logic), Phase 3 (callers), Phase 4 (tests + verification). All changes are ~215 lines, well under the 400-line review budget.

## Verification Criteria per Task

| Task | Criteria |
|------|----------|
| 1.1 | Migration applied idempotently; `SELECT check_rate_limit(...)` returns correct `{allowed, remaining}` |
| 2.1 | `checkRateLimit(key)` is async; calls `supabase.rpc` with correct params; throws on error |
| 3.1 | Route handler awaits `checkRateLimit`; 429 response still works |
| 3.2 | Same as 3.1 for change-pin route |
| 4.1 | All 8 scenarios pass; mock simulates RPC correctly with window expiry; fake timers intact |
| 4.2 | `npm test` green; DB migration verified in Supabase |
