# Design: Persist Rate Limiter to Supabase

## Technical Approach

Replace the in-memory `Map` with a single PostgreSQL RPC call (`check_rate_limit`). The RPC does atomic SELECT-or-INSERT-or-UPDATE in one round-trip. The TS function becomes an async wrapper: `await createClient().rpc(...)`. Callers only need to add `await`.

## Architecture Decisions

| Decision | Options | Choice | Rationale |
|----------|---------|--------|-----------|
| DB interaction | RPC vs SELECT+INSERT/UPDATE vs UPSERT | **RPC function** | Single round-trip meets spec budget (<50ms). UPSERT requires unique constraint on `limit_key` which conflicts with multiple expired rows. SELECT+UPDATE is 2 trips. |
| Client injection | Accept client as param vs internal creation | **Internal** via `createClient()` | Spec scenario explicitly states callers need only `await` addition — no new param. Server client already works in route handler context. Duplicate `cookies()` call is negligible. |
| Function privileges | SECURITY DEFINER vs INVOKER | **SECURITY DEFINER** | Table RLS denies authenticated direct access. The RPC function runs as owner (bypasses RLS). Follows project pattern: `GRANT EXECUTE TO authenticated, service_role`. |
| Cleanup strategy | pg_cron vs read-repair vs both | **Read-repair (WHERE filter) + pg_cron** | The RPC filters expired rows in its WHERE clause (read-repair). A pg_cron job deletes rows older than 24h as a belt-and-suspenders measure. No stale data survives queries; periodic job keeps table lean. |
| Index | `(limit_key, window_start)` vs `(limit_key)` only | **Composite `(limit_key, window_start)`** | The RPC queries with `WHERE limit_key = $1 ORDER BY window_start DESC LIMIT 1`. Composite index covers the full query. |

## Data Flow

```
Route Handler                         DB (Supabase)
     │                                     │
     ├─ await checkRateLimit("pin-verify:abc")
     │        │                            │
     │        ├─ await createClient()      │
     │        ├─ supabase.rpc(             │
     │        │    'check_rate_limit',     │
     │        │    { p_limit_key,          │
     │        │      p_window_ms,          │
     │        │      p_max_attempts }      │
     │        │  ) ──────────────────────► │
     │        │                            ├─ SELECT ... ORDER BY window_start DESC LIMIT 1
     │        │                            ├─ IF not found OR expired → INSERT (attempts=1)
     │        │                            ├─ IF attempts >= max → return blocked
     │        │                            ├─ ELSE → UPDATE attempts++
     │        │                            ├─ RETURN jsonb {allowed, remaining}
     │        │  ◄─────────────────────────┤
     │        └─ return result             │
     └─ use { allowed, remaining }         │
```

## Migration SQL

**File**: `supabase/migrations/20260511000000_pin_attempts_rate_limit.sql`

```sql
-- Persist rate-limit attempts across serverless cold starts.

CREATE TABLE IF NOT EXISTS public.pin_attempts (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    limit_key text NOT NULL,
    attempts integer NOT NULL DEFAULT 1,
    window_start timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pin_attempts_key_window
    ON public.pin_attempts (limit_key, window_start);

ALTER TABLE public.pin_attempts ENABLE ROW LEVEL SECURITY;

CREATE POLICY "service_role_full_access" ON public.pin_attempts
    FOR ALL TO service_role USING (true) WITH CHECK (true);

-- RPC: single round-trip, atomic rate-limit check
CREATE OR REPLACE FUNCTION public.check_rate_limit(
    p_limit_key text,
    p_window_ms integer,
    p_max_attempts integer
) RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_row record;
    v_now timestamptz := now();
    v_window_ago timestamptz := v_now - (p_window_ms || ' milliseconds')::interval;
BEGIN
    SELECT * INTO v_row
    FROM pin_attempts
    WHERE limit_key = p_limit_key
      AND window_start > v_window_ago
    ORDER BY window_start DESC
    LIMIT 1;

    IF NOT FOUND THEN
        INSERT INTO pin_attempts (limit_key, attempts, window_start)
        VALUES (p_limit_key, 1, v_now);
        RETURN jsonb_build_object('allowed', true, 'remaining', p_max_attempts - 1);
    END IF;

    IF v_row.attempts >= p_max_attempts THEN
        RETURN jsonb_build_object('allowed', false, 'remaining', 0);
    END IF;

    UPDATE pin_attempts SET attempts = v_row.attempts + 1 WHERE id = v_row.id;
    RETURN jsonb_build_object('allowed', true, 'remaining', p_max_attempts - v_row.attempts - 1);
END;
$$;

REVOKE EXECUTE ON FUNCTION public.check_rate_limit(text, integer, integer) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.check_rate_limit(text, integer, integer) TO authenticated, service_role;

-- pg_cron cleanup (hourly, removes rows older than 24h)
SELECT cron.schedule(
    'cleanup-pin-attempts',
    '0 * * * *',
    $$ DELETE FROM public.pin_attempts WHERE window_start < now() - interval '24 hours' $$
);
```

## Implementation: `rate-limit.ts`

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
| `supabase/migrations/20260511000000_pin_attempts_rate_limit.sql` | **Create** | Table, index, RLS, RPC function, pg_cron job |
| `web/src/lib/rate-limit.ts` | **Modify** | `async` wrapper calling RPC; `Map` removed |
| `web/src/app/api/auth/verify-pin/route.ts` | **Modify** | Add `await` before `checkRateLimit` |
| `web/src/app/api/config/change-pin/route.ts` | **Modify** | Add `await` before `checkRateLimit` |
| `web/src/lib/rate-limit.test.ts` | **Modify** | Mock `@/lib/supabase/server`; add `await` to all calls; replace fake timers with inline time simulation in mock |

## Caller Analysis

**2 production callers** (both in `async function POST` route handlers):

| Caller | Current | Change |
|--------|---------|--------|
| `verify-pin/route.ts:19` | `const rl = checkRateLimit("pin-verify:" + user.id)` | `const rl = await checkRateLimit("pin-verify:" + user.id)` |
| `change-pin/route.ts:18` | `const rl = checkRateLimit("pin-change:" + user.id)` | `const rl = await checkRateLimit("pin-change:" + user.id)` |

Plus `rate-limit.test.ts` (8 test cases, all sync calls → add `await`).

## Test Strategy

**Approach**: Mock `@/lib/supabase/server` so `createClient()` returns a fake with `.rpc()` that delegates to an in-memory Map simulating the same RPC logic. This keeps the 8 existing test scenarios valid.

**Mock structure** (`rate-limit.test.ts`):

```ts
vi.mock("@/lib/supabase/server", () => ({
  createClient: vi.fn(),
}));

function makeMockRpc() {
  const store = new Map<string, { attempts: number; windowStart: number }>();
  return vi.fn(async (_fn: string, params: { p_limit_key: string; p_window_ms: number; p_max_attempts: number }) => {
    const { p_limit_key, p_window_ms, p_max_attempts } = params;
    const now = Date.now();
    const entry = store.get(p_limit_key);

    if (!entry || now > entry.windowStart + p_window_ms) {
      store.set(p_limit_key, { attempts: 1, windowStart: now });
      return { data: { allowed: true, remaining: p_max_attempts - 1 }, error: null };
    }
    if (entry.attempts >= p_max_attempts) {
      return { data: { allowed: false, remaining: 0 }, error: null };
    }
    entry.attempts++;
    return { data: { allowed: true, remaining: p_max_attempts - entry.attempts }, error: null };
  });
}
```

**Fake timers**: `vi.useFakeTimers()` + `vi.setSystemTime()` still work because the mock reads `Date.now()`, not DB timestamps. Window expiry tests (`advanceTimersByTime`) remain valid.

| Layer | What | How |
|-------|------|-----|
| Unit | `checkRateLimit` TS wrapper | Mock `createClient`, assert `.rpc()` call params and return value |
| Integration | 8 existing scenarios (window expiry, concurrency, etc.) | Stateful mock RPC with fake timers |
| DB | `check_rate_limit` PL/pgSQL | Manual `SELECT check_rate_limit(...)` in Supabase SQL editor |

## Work Units (PR-able chunks)

**PR #1 — Migration** (~60 lines SQL): `pin_attempts` table, index, RLS, RPC function, pg_cron job. Deploy separately; verify in Supabase dashboard.

**PR #2 — Implementation + Callers + Tests** (~120 lines TS): New `rate-limit.ts`, updated route handlers (2 lines each), rewritten test file with mock. All changes depend on migration being deployed.

> **400-line budget**: Low risk (total ~180 lines across both PRs). No chaining needed.

## Open Questions

- [ ] Is `pg_cron` enabled on the production Supabase project? If not, the `cron.schedule` call will fail — remove it from migration and use a separate Edge Function for cleanup.
