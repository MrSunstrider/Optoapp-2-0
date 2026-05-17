# Design: Persist Rate Limiter to Supabase

## Technical Approach

Single PostgreSQL RPC (`check_rate_limit`) replaces the in-memory `Map`. The RPC does atomic SELECT-or-INSERT-or-UPDATE in one round-trip. The TS function becomes a thin async wrapper: `await createClient().rpc(...)`. Callers in async server contexts only need `await`.

Already implemented — this design documents the deployed architecture.

## Architecture Decisions

| Decision | Options | Choice | Why |
|----------|---------|--------|-----|
| DB interaction | RPC vs SELECT+INSERT | **RPC** | One round-trip meets <50ms budget. Avoids two-trip SELECT-then-UPDATE pattern. |
| Client injection | Accept client param vs internal creation | **Internal** via `createClient()` | Spec mandates callers need only `await` — no new params. Follows existing server-client pattern. |
| Function privileges | SECURITY DEFINER vs INVOKER | **SECURITY DEFINER** with `GRANT TO authenticated, service_role` | Table RLS denies direct access. RPC bypasses RLS as owner. Matches project convention for server-side functions. |
| Cleanup | pg_cron vs read-repair only | **Both** — WHERE filter + pg_cron hourly | RPC's `WHERE window_start > v_window_ago` filters expired rows (read-repair). pg_cron deletes rows older than 24h hourly. |
| Index | Composite vs single-column | **`(limit_key, window_start)`** | Covers the RPC query: `WHERE limit_key = $1 ORDER BY window_start DESC LIMIT 1`. |
| Atomicity | SERIALIZABLE vs eventual accuracy | **Accept eventual accuracy** | True concurrent attempts for the same key are extremely rare in a PIN brute-force context. Adding `FOR UPDATE` causes lock contention without meaningful security benefit. |

## Data Flow

```
Route Handler                         Supabase DB
     │
     ├─ await checkRateLimit("pin-verify:abc")
     │        │
     │        ├─ await createClient()
     │        └─ supabase.rpc("check_rate_limit", {
     │              p_limit_key, p_window_ms, p_max_attempts })
     │              ────────────────────────►
     │                                       ├─ SELECT ... WHERE limit_key=$1
     │                                       │    AND window_start > now()-window
     │                                       │    ORDER BY window_start DESC LIMIT 1
     │                                       ├─ NOT FOUND → INSERT (attempts=1)
     │                                       ├─ attempts>=max → return blocked
     │                                       ├─ ELSE → UPDATE attempts++
     │                                       └─ RETURN jsonb {allowed, remaining}
     │              ◄────────────────────────
     └─ if (!rl.allowed) → 429
```

## Migration: `pin_attempts` Table

**File**: `supabase/migrations/20260511000000_pin_attempts_rate_limit.sql` (already deployed)

| Column | Type | Default |
|--------|------|---------|
| `id` | `uuid` PK | `gen_random_uuid()` |
| `limit_key` | `text NOT NULL` | — |
| `attempts` | `integer NOT NULL` | `1` |
| `window_start` | `timestamptz NOT NULL` | `now()` |
| `created_at` | `timestamptz NOT NULL` | `now()` |

**Index**: `idx_pin_attempts_key_window` ON `(limit_key, window_start)`

**RLS**: `service_role_full_access` — `FOR ALL TO service_role`. Authenticated users access through SECURITY DEFINER RPC only.

**RPC signature**: `check_rate_limit(p_limit_key text, p_window_ms integer, p_max_attempts integer) RETURNS jsonb`

**pg_cron**: `cleanup-pin-attempts` runs hourly, deletes rows where `window_start < now() - interval '24 hours'`

## Implementation: `web/src/lib/rate-limit.ts`

```ts
import { createClient } from "@/lib/supabase/server";

const WINDOW_MS = 60_000;
const MAX_ATTEMPTS = 5;

export async function checkRateLimit(
  key: string
): Promise<{ allowed: boolean; remaining: number }> {
  const supabase = await createClient();
  const { data, error } = await supabase.rpc("check_rate_limit", {
    p_limit_key: key,
    p_window_ms: WINDOW_MS,
    p_max_attempts: MAX_ATTEMPTS,
  });
  if (error) throw error;
  return data as unknown as { allowed: boolean; remaining: number };
}
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `supabase/migrations/20260511000000_pin_attempts_rate_limit.sql` | Create | Table, index, RLS, RPC, pg_cron job |
| `web/src/lib/rate-limit.ts` | Modify | `async` wrapper calling RPC; `Map` removed |
| `web/src/app/api/auth/verify-pin/route.ts` | Modify | Line 19: added `await` before `checkRateLimit` |
| `web/src/app/api/config/change-pin/route.ts` | Modify | Line 18: added `await` before `checkRateLimit` |
| `web/src/lib/rate-limit.test.ts` | Modify | Mock `createClient`; stateful RPC mock with fake timers |

## Testing Strategy

| Layer | What | How |
|-------|------|-----|
| Unit | `checkRateLimit` TS wrapper | Mock `createClient` → return `{ rpc: mockRpc }`. Assert correct params forwarded to `.rpc()`. |
| Integration | 8 scenarios (window expiry, blocking, concurrency, etc.) | Stateful in-memory mock RPC with `vi.useFakeTimers()` |
| DB | `check_rate_limit` PL/pgSQL | `SELECT check_rate_limit(...)` in Supabase SQL editor |

Mock structure: `createClient` returns `{ rpc: vi.fn() }` with a Map-based state machine replicating the RPC logic. Fake timers still work because the mock reads `Date.now()`.

## Migration Rollout

Two PR-able work units (~180 lines total, low risk):

1. **PR #1 — Migration** (~66 lines SQL): Table, index, RLS, RPC, pg_cron
2. **PR #2 — Implementation** (~120 lines TS): New rate-limit.ts, `await` on 2 routes, updated tests

**Rollback**: Revert `rate-limit.ts` to previous in-memory `Map`. Drop `pin_attempts` and RPC via migration if needed. Isolated change — no other tables affected.

## Open Questions

- [ ] Is `pg_cron` enabled on production? If not, `cron.schedule`/`cron.unschedule` calls fail — wrap in a DO block or remove and use read-repair-only cleanup.
- [ ] Should we add `SELECT ... FOR UPDATE` to the RPC for strict atomicity under high-concurrency scenarios? Current cost/benefit analysis says no for a PIN rate limiter.
